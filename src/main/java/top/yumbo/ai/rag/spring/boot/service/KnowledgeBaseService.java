package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.DocumentChunker;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 知识库构建服务
 * 支持多种文件格式：Excel, Word, PowerPoint, PDF, TXT等
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    private final KnowledgeQAProperties properties;
    private final TikaDocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final DocumentProcessingOptimizer optimizer;
    private final FileTrackingService fileTrackingService;
    private final top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService;

    public KnowledgeBaseService(KnowledgeQAProperties properties,
                                DocumentProcessingOptimizer optimizer,
                                FileTrackingService fileTrackingService,
                                top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService) {
        this.properties = properties;
        this.optimizer = optimizer;
        this.fileTrackingService = fileTrackingService;
        this.imageExtractionService = imageExtractionService;
        this.documentParser = new TikaDocumentParser();
        this.documentChunker = optimizer.createChunker();
    }

    /**
     * 构建知识库（使用增量索引）
     * 启动时的默认行为：只索引新增和修改的文件
     *
     * @param sourcePath 文档源路径
     * @param storagePath 知识库存储路径
     * @return 构建结果
     */
    public BuildResult buildKnowledgeBaseWithIncrementalIndex(
            String sourcePath, String storagePath) {

        log.info("📂 扫描文档: {}", sourcePath);

        BuildResult result =
            new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 初始化文件追踪
            fileTrackingService.initialize(storagePath);

            // 2. 扫描文件
            List<File> allFiles = scanDocuments(sourcePath);
            result.setTotalFiles(allFiles.size());

            if (allFiles.isEmpty()) {
                log.warn("⚠️  未找到支持的文档文件");
                log.info("💡 提示: 请将文档放到 {} 目录", sourcePath);
                log.info("      支持格式: {}", properties.getDocument().getSupportedFormats());

                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info("✅ 找到 {} 个文档文件", allFiles.size());

            // 3. 打开或创建知识库
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            var stats = rag.getStatistics();
            boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

            if (knowledgeBaseExists) {
                log.info("📚 检测到已有知识库 ({} 个文档)", stats.getDocumentCount());
            } else {
                log.info("📚 首次创建知识库");
            }

            // 4. 筛选需要更新的文件
            List<File> filesToUpdate = new ArrayList<>();
            for (File file : allFiles) {
                if (fileTrackingService.needsUpdate(file)) {
                    filesToUpdate.add(file);
                }
            }

            log.info("📝 需要索引的文件: {} 个", filesToUpdate.size());

            if (filesToUpdate.isEmpty()) {
                log.info("✅ 所有文件都是最新的，无需更新");
                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                // 必须关闭以释放锁
                rag.close();
                return result;
            }

            // 5. 初始化向量检索引擎（如果启用）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info("✅ 向量检索引擎已启用");
                } catch (Exception e) {
                    log.warn("⚠️  向量检索引擎初始化失败，将只使用关键词索引", e);
                }
            }

            // 6. 处理需要更新的文档
            log.info("\n📝 开始处理文档...");

            // 检查是否启用并行处理
            boolean useParallel = properties.getDocument().isParallelProcessing()
                && filesToUpdate.size() > 5;

            if (useParallel) {
                int threads = properties.getDocument().getParallelThreads();
                if (threads == 0) {
                    threads = Runtime.getRuntime().availableProcessors();
                }
                log.info("🚀 使用并行处理模式（{} 个线程）", threads);
            } else {
                log.info("📝 使用串行处理模式");
            }

            int successCount;
            int failedCount;

            optimizer.logMemoryUsage("增量索引开始前");

            if (useParallel) {
                // 并行处理
                var result_counts = processDocumentsInParallel(
                    filesToUpdate, rag, embeddingEngine, vectorIndexEngine);
                successCount = result_counts[0];
                failedCount = result_counts[1];
            } else {
                // 串行处理（原有逻辑）
                successCount = 0;
                failedCount = 0;
                List<Document> batchDocuments = new ArrayList<>();

                for (int i = 0; i < filesToUpdate.size(); i++) {
                    File file = filesToUpdate.get(i);

                    try {
                        // 处理文档
                        List<Document> docs = processDocumentOptimized(
                            file, rag, embeddingEngine, vectorIndexEngine);

                        if (docs != null && !docs.isEmpty()) {
                            batchDocuments.addAll(docs);
                            successCount++;

                            // 标记文件已索引
                            fileTrackingService.markAsIndexed(file);

                            // 估算内存使用
                            long estimatedMemory = docs.stream()
                                .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                                .sum();
                            optimizer.addBatchMemory(estimatedMemory);

                            // 检查是否需要批处理或GC
                            if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                                log.info("📦 批处理: {} 个文档 ({} / {})",
                                    batchDocuments.size(), i + 1, filesToUpdate.size());

                                rag.commit();
                                batchDocuments.clear();
                                optimizer.resetBatchMemory();
                                optimizer.checkAndTriggerGC();
                            }
                        }

                    } catch (Exception e) {
                        log.error("❌ 处理文件失败: {}", file.getName(), e);
                        failedCount++;
                    }

                    // 定期打印进度和内存状态
                    if ((i + 1) % 5 == 0 || i == filesToUpdate.size() - 1) {
                        optimizer.logMemoryUsage(
                            String.format("进度 %d/%d", i + 1, filesToUpdate.size()));
                    }
                }

                // 处理剩余的批次
                if (!batchDocuments.isEmpty()) {
                    log.info("📦 处理最后一批: {} 个文档", batchDocuments.size());
                    rag.commit();
                }
            }

            // 7. 填充构建结果
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            // 8. 关闭资源（包括 RAG 实例）
            // 必须关闭以释放 Lucene 写锁，否则后续实例无法获取锁
            if (embeddingEngine != null) {
                embeddingEngine.close();
            }
            rag.close();

            log.info("\n✅ 增量索引完成！");
            log.info("   - 处理文件: {} / {}", successCount, filesToUpdate.size());
            log.info("   - 失败: {}", failedCount);
            log.info("   - 总文档: {}", result.getTotalDocuments());
            log.info("   - 耗时: {} 秒", result.getBuildTimeMs() / 1000.0);

            return result;

        } catch (Exception e) {
            log.error("❌ 增量索引失败", e);
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * 构建知识库
     *
     * @param sourcePath 文档源路径
     * @param storagePath 知识库存储路径
     * @param rebuild 是否重建
     * @return 构建结果
     */
    public BuildResult buildKnowledgeBase(
            String sourcePath, String storagePath, boolean rebuild) {

        log.info("📂 扫描文档: {}", sourcePath);

        BuildResult result =
            new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 扫描文件
            List<File> files = scanDocuments(sourcePath);
            result.setTotalFiles(files.size());

            if (files.isEmpty()) {
                log.warn("⚠️  未找到支持的文档文件");
                log.info("💡 提示: 请将文档放到 {} 目录", sourcePath);
                log.info("      支持格式: {}", properties.getDocument().getSupportedFormats());

                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info("✅ 找到 {} 个文档文件", files.size());

            // 2. 检查是否需要构建
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            var stats = rag.getStatistics();
            boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

            if (knowledgeBaseExists && !rebuild) {
                log.info("📚 检测到已有知识库 ({} 个文档)", stats.getDocumentCount());
                log.info("✅ 跳过构建，使用已有知识库");

                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);

                rag.close();
                return result;
            }

            if (knowledgeBaseExists && rebuild) {
                log.info("🔄 检测到已有知识库，准备重建...");
                // 清空知识库
                rag.deleteAllDocuments();
                log.info("✓ 已清空旧知识库");

                // 清空文件追踪信息
                fileTrackingService.initialize(storagePath);
                fileTrackingService.clearAll();
                log.info("✓ 已清空文件追踪信息");
            }

            // 3. 处理文档
            log.info("\n📝 开始处理文档...");
            long processStartTime = System.currentTimeMillis();

            // 初始化向量检索引擎（如果启用）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info("✅ 向量检索引擎已启用");
                } catch (Exception e) {
                    log.warn("⚠️  向量检索引擎初始化失败，将只使用关键词索引", e);
                }
            }

            // 检查是否启用并行处理
            boolean useParallel = properties.getDocument().isParallelProcessing()
                && files.size() > 5;

            if (useParallel) {
                int threads = properties.getDocument().getParallelThreads();
                if (threads == 0) {
                    threads = Runtime.getRuntime().availableProcessors();
                }
                log.info("🚀 使用并行处理模式（{} 个线程）", threads);
            } else {
                log.info("📝 使用串行处理模式");
            }

            int successCount;
            int failedCount;

            // 记录初始内存
            optimizer.logMemoryUsage("开始处理前");

            if (useParallel) {
                // 并行处理
                var result_counts = processDocumentsInParallel(
                    files, rag, embeddingEngine, vectorIndexEngine);
                successCount = result_counts[0];
                failedCount = result_counts[1];

                // 标记文件已索引
                if (rebuild) {
                    for (File file : files) {
                        fileTrackingService.markAsIndexed(file);
                    }
                }
            } else {
                // 串行处理（原有逻辑）
                successCount = 0;
                failedCount = 0;
                List<Document> batchDocuments = new ArrayList<>();

                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);

                    try {
                        // 处理文档并收集到批次
                        List<Document> docs = processDocumentOptimized(
                            file, rag, embeddingEngine, vectorIndexEngine);

                        if (docs != null && !docs.isEmpty()) {
                            batchDocuments.addAll(docs);
                            successCount++;

                            // 标记文件已索引（用于增量索引）
                            if (rebuild) {
                                fileTrackingService.markAsIndexed(file);
                            }

                            // 估算内存使用
                            long estimatedMemory = docs.stream()
                                .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                                .sum();
                            optimizer.addBatchMemory(estimatedMemory);

                            // 检查是否需要批处理或GC
                            if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                                log.info("📦 批处理: {} 个文档 ({} / {})",
                                    batchDocuments.size(), i + 1, files.size());

                                rag.commit();
                                batchDocuments.clear();
                                optimizer.resetBatchMemory();
                                optimizer.checkAndTriggerGC();
                            }
                        }

                    } catch (Exception e) {
                        log.error("❌ 处理文件失败: {}", file.getName(), e);
                        failedCount++;
                    }

                    // 定期打印进度和内存状态
                    if ((i + 1) % 5 == 0 || i == files.size() - 1) {
                        optimizer.logMemoryUsage(
                            String.format("进度 %d/%d", i + 1, files.size()));
                    }
                }

                // 处理剩余的批次
                if (!batchDocuments.isEmpty()) {
                    log.info("📦 处理最后一批: {} 个文档", batchDocuments.size());
                    rag.commit();
                }
            }

            long processEndTime = System.currentTimeMillis();

            // 4. 填充构建结果
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(processEndTime - processStartTime);

            // 获取峰值内存使用
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.setPeakMemoryMB(usedMemory / 1024 / 1024);

            // 5. 显示结果
            log.info("\n" + "=".repeat(80));
            log.info("✅ 知识库构建完成");
            log.info("=".repeat(80));
            log.info("   - 成功: {} 个文件", result.getSuccessCount());
            log.info("   - 失败: {} 个文件", result.getFailedCount());
            log.info("   - 总文档: {} 个", result.getTotalDocuments());
            log.info("   - 耗时: {} 秒", String.format("%.2f", result.getBuildTimeMs() / 1000.0));
            log.info("   - 峰值内存: {} MB", result.getPeakMemoryMB());
            log.info("=".repeat(80));

            // 6. 保存文件追踪信息（用于增量索引）
            if (rebuild) {
                fileTrackingService.saveTracking();
                log.info("✓ 已保存文件追踪信息");
            }

            // 7. 优化和提交
            optimizer.commitAndOptimize(rag);

            // 8. 保存向量索引
            optimizer.saveVectorIndex(vectorIndexEngine);

            // 9. 清理资源
            optimizer.closeEmbeddingEngine(embeddingEngine);

            // 10. 最终内存状态
            optimizer.logMemoryUsage("构建完成");

            rag.close();

            return result;

        } catch (Exception e) {
            log.error("❌ 知识库构建失败", e);

            result.setError(e.getMessage());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * 增量索引知识库
     * 只处理新增和修改的文档，大幅提升性能
     *
     * @param sourcePath 文档源路径
     * @param storagePath 知识库存储路径
     * @return 构建结果
     */
    public BuildResult incrementalIndex(
            String sourcePath, String storagePath) {

        log.info("🔄 开始增量索引...");
        log.info("📂 扫描文档: {}", sourcePath);

        BuildResult result =
            new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 初始化文件追踪
            fileTrackingService.initialize(storagePath);

            // 2. 扫描所有文件
            List<File> allFiles = scanDocuments(sourcePath);
            result.setTotalFiles(allFiles.size());

            if (allFiles.isEmpty()) {
                log.warn("⚠️  未找到支持的文档文件");
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info("✅ 找到 {} 个文档文件", allFiles.size());

            // 3. 筛选需要更新的文件
            List<File> filesToUpdate = new ArrayList<>();
            for (File file : allFiles) {
                if (fileTrackingService.needsUpdate(file)) {
                    filesToUpdate.add(file);
                }
            }

            log.info("📝 需要更新的文件: {} 个", filesToUpdate.size());

            if (filesToUpdate.isEmpty()) {
                log.info("✅ 所有文件都是最新的，无需更新");
                LocalFileRAG rag = LocalFileRAG.builder()
                    .storagePath(storagePath)
                    .build();
                var stats = rag.getStatistics();
                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                // 必须关闭以释放锁
                rag.close();
                return result;
            }

            // 4. 打开知识库
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            // 5. 初始化向量检索引擎（如果启用）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info("✅ 向量检索引擎已启用");
                } catch (Exception e) {
                    log.warn("⚠️  向量检索引擎初始化失败，将只使用关键词索引", e);
                }
            }

            // 6. 处理需要更新的文档
            log.info("\n📝 开始处理文档...");
            int successCount = 0;
            int failedCount = 0;
            List<Document> batchDocuments = new ArrayList<>();

            optimizer.logMemoryUsage("增量索引开始前");

            for (int i = 0; i < filesToUpdate.size(); i++) {
                File file = filesToUpdate.get(i);

                try {
                    // 处理文档
                    List<Document> docs = processDocumentOptimized(
                        file, rag, embeddingEngine, vectorIndexEngine);

                    if (docs != null && !docs.isEmpty()) {
                        batchDocuments.addAll(docs);
                        successCount++;

                        // 标记为已索引
                        fileTrackingService.markAsIndexed(file);

                        // 估算内存使用
                        long estimatedMemory = docs.stream()
                            .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                            .sum();
                        optimizer.addBatchMemory(estimatedMemory);

                        // 检查是否需要批处理或GC
                        if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                            log.info("📦 批处理: {} 个文档 ({} / {})",
                                batchDocuments.size(), i + 1, filesToUpdate.size());

                            rag.commit();
                            batchDocuments.clear();
                            optimizer.resetBatchMemory();
                            optimizer.checkAndTriggerGC();
                        }
                    }

                } catch (Exception e) {
                    log.error("❌ 处理文件失败: {}", file.getName(), e);
                    failedCount++;
                }

                // 定期打印进度
                if ((i + 1) % 5 == 0 || i == filesToUpdate.size() - 1) {
                    optimizer.logMemoryUsage(
                        String.format("进度 %d/%d", i + 1, filesToUpdate.size()));
                }
            }

            // 处理剩余的批次
            if (!batchDocuments.isEmpty()) {
                log.info("📦 处理最后一批: {} 个文档", batchDocuments.size());
                rag.commit();
            }

            // 7. 保存文件追踪信息
            fileTrackingService.saveTracking();

            long processEndTime = System.currentTimeMillis();

            // 8. 填充构建结果
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(processEndTime - startTime);

            // 获取峰值内存使用
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.setPeakMemoryMB(usedMemory / 1024 / 1024);

            // 9. 显示结果
            log.info("\n" + "=".repeat(80));
            log.info("✅ 增量索引完成");
            log.info("=".repeat(80));
            log.info("   - 更新文件: {} 个", filesToUpdate.size());
            log.info("   - 成功: {} 个文件", result.getSuccessCount());
            log.info("   - 失败: {} 个文件", result.getFailedCount());
            log.info("   - 总文档: {} 个", result.getTotalDocuments());
            log.info("   - 耗时: {} 秒", String.format("%.2f", result.getBuildTimeMs() / 1000.0));
            log.info("   - 峰值内存: {} MB", result.getPeakMemoryMB());
            log.info("=".repeat(80));

            // 10. 优化和提交
            optimizer.commitAndOptimize(rag);

            // 11. 保存向量索引
            optimizer.saveVectorIndex(vectorIndexEngine);

            // 12. 清理资源
            optimizer.closeEmbeddingEngine(embeddingEngine);

            // 13. 最终内存状态
            optimizer.logMemoryUsage("增量索引完成");

            // 必须关闭 RAG 实例以释放 Lucene 写锁
            rag.close();

            return result;

        } catch (Exception e) {
            log.error("❌ 增量索引失败", e);

            result.setError(e.getMessage());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * 扫描文档文件
     */
    private List<File> scanDocuments(String sourcePath) throws IOException {
        log.info("📂 源路径: {}", sourcePath);

        // 处理 classpath: 前缀
        if (sourcePath.startsWith("classpath:")) {
            return scanClasspathResources(sourcePath.substring("classpath:".length()));
        }

        // 处理普通文件系统路径
        File sourceFile = new File(sourcePath);

        if (!sourceFile.exists()) {
            log.warn("⚠️  路径不存在: {}", sourcePath);
            return Collections.emptyList();
        }

        List<File> files = new ArrayList<>();

        if (sourceFile.isFile()) {
            // 单个文件
            if (isSupportedFile(sourceFile)) {
                files.add(sourceFile);
            }
        } else if (sourceFile.isDirectory()) {
            // 文件夹 - 递归扫描
            try (var stream = Files.walk(Paths.get(sourcePath))) {
                stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isSupportedFile)
                    .forEach(files::add);
            }
        }

        return files;
    }

    /**
     * 扫描 classpath 资源
     */
    private List<File> scanClasspathResources(String resourcePath) throws IOException {
        log.info("📦 扫描 classpath 资源: {}", resourcePath);

        List<File> files = new ArrayList<>();

        try {
            // 获取资源 URL
            var resource = getClass().getClassLoader().getResource(resourcePath);

            if (resource == null) {
                log.warn("⚠️  classpath 资源不存在: {}", resourcePath);
                return files;
            }

            log.info("✓ 找到资源: {}", resource);

            // 转换为 File 对象
            File resourceFile = new File(resource.toURI());

            if (!resourceFile.exists()) {
                log.warn("⚠️  资源文件不存在: {}", resourceFile.getAbsolutePath());
                return files;
            }

            log.info("✓ 资源路径: {}", resourceFile.getAbsolutePath());

            if (resourceFile.isFile()) {
                // 单个文件
                if (isSupportedFile(resourceFile)) {
                    files.add(resourceFile);
                    log.info("✓ 添加文件: {}", resourceFile.getName());
                }
            } else if (resourceFile.isDirectory()) {
                // 目录 - 递归扫描
                log.info("✓ 扫描目录...");
                try (var stream = Files.walk(resourceFile.toPath())) {
                    stream.filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .filter(this::isSupportedFile)
                        .forEach(f -> {
                            files.add(f);
                            log.debug("   - {}", f.getName());
                        });
                }
                log.info("✓ 找到 {} 个支持的文件", files.size());
            }

        } catch (Exception e) {
            log.error("❌ 扫描 classpath 资源失败: {}", resourcePath, e);
            throw new IOException("扫描 classpath 资源失败", e);
        }

        return files;
    }

    /**
     * 判断是否支持的文件格式
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        List<String> supportedFormats = properties.getDocument().getSupportedFormats();

        return supportedFormats.stream()
            .anyMatch(format -> fileName.endsWith("." + format));
    }

    /**
     * 并行处理文档列表
     *
     * @return int[] {successCount, failedCount}
     */
    private int[] processDocumentsInParallel(
            List<File> filesToProcess,
            LocalFileRAG rag,
            LocalEmbeddingEngine embeddingEngine,
            SimpleVectorIndexEngine vectorIndexEngine) {

        int threads = properties.getDocument().getParallelThreads();
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }

        int batchSize = properties.getDocument().getBatchSize();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        int totalFiles = filesToProcess.size();

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            // 分批处理文件
            for (int i = 0; i < totalFiles; i += batchSize) {
                final int batchEnd = Math.min(i + batchSize, totalFiles);
                List<File> batch = filesToProcess.subList(i, batchEnd);

                Future<?> future = executor.submit(() -> {
                    // 每个线程独立的文档列表
                    List<Document> threadDocuments = new ArrayList<>();

                    for (File file : batch) {
                        try {
                            // 处理文档
                            List<Document> docs = processDocumentOptimized(
                                file, rag, embeddingEngine, vectorIndexEngine);

                            if (docs != null && !docs.isEmpty()) {
                                threadDocuments.addAll(docs);
                                successCount.incrementAndGet();

                                // 标记文件已索引
                                fileTrackingService.markAsIndexed(file);
                            }

                        } catch (Exception e) {
                            log.error("❌ 处理文件失败: {}", file.getName(), e);
                            failedCount.incrementAndGet();
                        }

                        // 更新进度
                        int current = processedCount.incrementAndGet();
                        if (current % 10 == 0 || current == totalFiles) {
                            log.info("📊 处理进度: {}/{} ({} 成功, {} 失败)",
                                current, totalFiles,
                                successCount.get(), failedCount.get());

                            optimizer.logMemoryUsage(
                                String.format("并行处理 %d/%d", current, totalFiles));
                        }
                    }

                    // 批次提交（使用 RAG 的同步机制）
                    synchronized (rag) {
                        if (!threadDocuments.isEmpty()) {
                            log.info("📦 提交批次: {} 个文档", threadDocuments.size());
                            rag.commit();
                        }
                    }

                    // 定期触发GC
                    if (processedCount.get() % (batchSize * 3) == 0) {
                        optimizer.checkAndTriggerGC();
                    }
                });

                futures.add(future);
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("❌ 批处理任务失败", e);
                }
            }

        } finally {
            // 关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 最后提交一次
        rag.commit();

        return new int[]{successCount.get(), failedCount.get()};
    }

    /**
     * 处理单个文档（优化版，返回文档列表以支持批处理）
     */
    private List<Document> processDocumentOptimized(File file, LocalFileRAG rag,
                                                     LocalEmbeddingEngine embeddingEngine,
                                                     SimpleVectorIndexEngine vectorIndexEngine) {

        log.info("📄 处理: {} ({} KB)", file.getName(), file.length() / 1024);
        List<Document> createdDocuments = new ArrayList<>();

        try {
            // 1. 检查文件大小
            if (!optimizer.checkFileSize(file.length())) {
                log.warn("   ⚠️  文件过大，跳过: {} MB > {} MB",
                    file.length() / 1024 / 1024,
                    properties.getDocument().getMaxFileSizeMb());
                return createdDocuments;
            }

            // 2. 解析文档内容
            String content = documentParser.parse(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn("   ⚠️  解析内容为空，跳过");
                return createdDocuments;
            }

            log.info("   ✓ 提取 {} 字符", content.length());

            // 2.5 提取图片（如果支持）
            if (imageExtractionService != null && imageExtractionService.supportsDocument(file.getName())) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> images =
                        imageExtractionService.extractAndSaveImages(file, file.getName());

                    if (!images.isEmpty()) {
                        log.info("   🖼️  提取 {} 张图片", images.size());
                    }
                } catch (Exception e) {
                    log.warn("   ⚠️  图片提取失败: {}", e.getMessage());
                    // 不中断文档处理流程
                }
            }

            // 3. 检查内容大小并判断分块策略
            boolean forceChunk = optimizer.needsForceChunking(content.length());
            boolean autoChunk = optimizer.shouldAutoChunk(content.length());

            if (forceChunk) {
                log.warn("   ⚠️  内容过大 ({} MB)，强制分块",
                    content.length() / 1024 / 1024);
            } else if (autoChunk) {
                log.info("   📝 内容较大 ({} KB)，自动分块",
                    content.length() / 1024);
            }

            // 4. 创建文档
            Document document = Document.builder()
                .title(file.getName())
                .content(content)
                .metadata(buildMetadata(file))
                .build();

            // 5. 判断是否需要分块
            List<Document> documentsToIndex;

            if (forceChunk || autoChunk) {
                documentsToIndex = documentChunker.chunk(document);
                log.info("   ✓ 分块: {} 个", documentsToIndex.size());
            } else {
                documentsToIndex = List.of(document);
            }

            // 6. 索引文档
            for (Document doc : documentsToIndex) {
                String docId = rag.index(doc);
                doc.setId(docId);
                createdDocuments.add(doc);

                // 7. 生成向量索引（如果启用）
                if (embeddingEngine != null && vectorIndexEngine != null) {
                    try {
                        float[] vector = embeddingEngine.embed(doc.getContent());
                        vectorIndexEngine.addDocument(docId, vector);
                    } catch (Exception e) {
                        log.debug("向量生成失败: {}", e.getMessage());
                    }
                }
            }

            log.info("   ✅ 索引完成 ({} 个文档)", createdDocuments.size());

            return createdDocuments;

        } catch (Exception e) {
            log.error("   ❌ 处理失败", e);
            throw new RuntimeException("文档处理失败: " + file.getName(), e);
        }
    }

    /**
     * 构建文档元数据
     */
    private Map<String, Object> buildMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("filePath", file.getAbsolutePath());
        metadata.put("fileExtension", getFileExtension(file));
        metadata.put("lastModified", file.lastModified());
        metadata.put("indexTime", System.currentTimeMillis());
        return metadata;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}

