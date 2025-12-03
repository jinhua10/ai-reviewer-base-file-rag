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
import top.yumbo.ai.rag.i18n.LogMessageProvider;

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
 * 知识库构建服务（Knowledge base construction service）
 * 支持多种文件格式：Excel, Word, PowerPoint, PDF, TXT等（Supports multiple file formats: Excel, Word, PowerPoint, PDF, TXT, etc.）
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
    private final SlideContentCacheService slideContentCacheService;

    public KnowledgeBaseService(KnowledgeQAProperties properties,
                                DocumentProcessingOptimizer optimizer,
                                FileTrackingService fileTrackingService,
                                top.yumbo.ai.rag.image.DocumentImageExtractionService imageExtractionService,
                                top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor imageExtractor,
                                SlideContentCacheService slideContentCacheService) {
        this.properties = properties;
        this.optimizer = optimizer;
        this.fileTrackingService = fileTrackingService;
        this.imageExtractionService = imageExtractionService;
        this.slideContentCacheService = slideContentCacheService;

        // 获取批量大小配置
        int visionBatchSize = properties.getImageProcessing().getVisionLlm().getBatch().getSize();

        // 使用注入的 SmartImageExtractor 创建 TikaDocumentParser（Use injected SmartImageExtractor to create TikaDocumentParser）
        this.documentParser = new TikaDocumentParser(
            10 * 1024 * 1024,  // 10MB max content
            true,              // extract image metadata
            true,              // include image placeholders
            visionBatchSize,   // vision batch size from config
            imageExtractor,    // use configured image extractor
            slideContentCacheService // slide cache service
        );
        this.documentChunker = optimizer.createChunker();
    }

    /**
     * 构建知识库（使用增量索引）（Build knowledge base (using incremental indexing)）
     * 启动时的默认行为：只索引新增和修改的文件（Default behavior at startup: only index new and modified files）
     *
     * @param sourcePath 文档源路径（Document source path）
     * @param storagePath 知识库存储路径（Knowledge base storage path）
     * @return 构建结果（Build result）
     */
    public BuildResult buildKnowledgeBaseWithIncrementalIndex(
            String sourcePath, String storagePath) {

        log.info(LogMessageProvider.getMessage("log.kb.scanning", sourcePath));

        BuildResult result = new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 初始化文件追踪（Initialize file tracking）
            fileTrackingService.initialize(storagePath);

            // 1.1 初始化幻灯片缓存服务（Initialize slide cache service）
            if (slideContentCacheService != null) {
                slideContentCacheService.initialize(storagePath);
                log.info("✅ 幻灯片缓存服务已初始化");
            }

            // 2. 扫描文件（Scan files）
            List<File> allFiles = scanDocuments(sourcePath);
            result.setTotalFiles(allFiles.size());

            if (allFiles.isEmpty()) {
                log.warn(LogMessageProvider.getMessage("log.kb.no_documents"));
                log.info(LogMessageProvider.getMessage("log.kb.hint_put_docs", sourcePath));
                log.info(LogMessageProvider.getMessage("log.kb.supported_formats", properties.getDocument().getSupportedFormats()));

                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info(LogMessageProvider.getMessage("log.kb.found_files", allFiles.size()));

            // 3. 打开或创建知识库（Open or create knowledge base）
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            var stats = rag.getStatistics();
            boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

            if (knowledgeBaseExists) {
                log.info(LogMessageProvider.getMessage("log.kb.exists", stats.getDocumentCount()));
            } else {
                log.info(LogMessageProvider.getMessage("log.kb.first_create"));
            }

            // 4. 筛选需要更新的文件（Filter files that need updating）
            List<File> filesToUpdate = new ArrayList<>();
            for (File file : allFiles) {
                if (fileTrackingService.needsUpdate(file)) {
                    filesToUpdate.add(file);
                }
            }

            log.info(LogMessageProvider.getMessage("log.kb.files_to_index", filesToUpdate.size()));

            if (filesToUpdate.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.kb.up_to_date"));
                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                // 必须关闭以释放锁（Must close to release lock）
                rag.close();
                return result;
            }

            // 5. 初始化向量检索引擎（如果启用）（Initialize vector indexing engine if enabled）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info(LogMessageProvider.getMessage("log.kb.vector_enabled"));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.kb.vector_init_failed"), e);
                }
            }

            // 6. 处理需要更新的文档（Process documents that need updating）
            log.info(LogMessageProvider.getMessage("log.kb.processing_start"));

            // 检查是否启用并行处理（Check if parallel processing is enabled）
            boolean useParallel = properties.getDocument().isParallelProcessing()
                && filesToUpdate.size() > 5;

            if (useParallel) {
                int threads = properties.getDocument().getParallelThreads();
                if (threads == 0) {
                    threads = Runtime.getRuntime().availableProcessors();
                }
                log.info(LogMessageProvider.getMessage("log.kb.parallel_mode", threads));
            } else {
                log.info(LogMessageProvider.getMessage("log.kb.serial_mode"));
            }

            int successCount;
            int failedCount;

            optimizer.logMemoryUsage(LogMessageProvider.getMessage("log.kb.gc_before"));

            if (useParallel) {
                // 并行处理（Parallel processing）
                var result_counts = processDocumentsInParallel(
                    filesToUpdate, rag, embeddingEngine, vectorIndexEngine);
                successCount = result_counts[0];
                failedCount = result_counts[1];
            } else {
                // 串行处理（原有逻辑）（Serial processing (original logic)）
                successCount = 0;
                failedCount = 0;
                List<Document> batchDocuments = new ArrayList<>();

                for (int i = 0; i < filesToUpdate.size(); i++) {
                    File file = filesToUpdate.get(i);

                    try {
                        // 处理文档（Process document）
                        List<Document> docs = processDocumentOptimized(
                            file, rag, embeddingEngine, vectorIndexEngine);

                        if (docs != null && !docs.isEmpty()) {
                            batchDocuments.addAll(docs);
                            successCount++;

                            // 标记文件已索引（Mark file as indexed）
                            fileTrackingService.markAsIndexed(file);

                            // 估算内存使用（Estimate memory usage）
                            long estimatedMemory = docs.stream()
                                .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                                .sum();
                            optimizer.addBatchMemory(estimatedMemory);

                            // 检查是否需要批处理或GC（Check if batch processing or GC is needed）
                            if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                                log.info(LogMessageProvider.getMessage("log.kb.batch_processing", batchDocuments.size(), i + 1, filesToUpdate.size()));

                                rag.commit();
                                batchDocuments.clear();
                                optimizer.resetBatchMemory();
                                optimizer.checkAndTriggerGC();
                            }
                        }

                    } catch (Exception e) {
                        log.error(LogMessageProvider.getMessage("log.kb.file_process_failed", file.getName()), e);
                        failedCount++;
                    }

                    // 定期打印进度和内存状态（Print progress and memory status regularly）
                    if ((i + 1) % 5 == 0 || i == filesToUpdate.size() - 1) {
                        optimizer.logMemoryUsage(
                            String.format("%s %d/%d", LogMessageProvider.getMessage("log.kb.progress"), i + 1, filesToUpdate.size()));
                    }
                }

                // 处理剩余的批次（Process remaining batches）
                if (!batchDocuments.isEmpty()) {
                    log.info(LogMessageProvider.getMessage("log.kb.final_batch", batchDocuments.size()));
                    rag.commit();
                }
            }

            // 7. 填充构建结果（Fill build result）
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            // 8. 关闭资源（包括 RAG 实例）（Close resources (including RAG instance)）
            // 必须关闭以释放 Lucene 写锁，否则后续实例无法获取锁（Must close to release Lucene write lock, otherwise subsequent instances cannot acquire lock）
            if (embeddingEngine != null) {
                embeddingEngine.close();
            }
            rag.close();

            log.info(LogMessageProvider.getMessage("log.kb.incremental_done"));
            log.info(LogMessageProvider.getMessage("log.kb.incremental_stats", successCount, filesToUpdate.size(), failedCount, result.getTotalDocuments(), result.getBuildTimeMs() / 1000.0));

            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.incremental_failed"), e);
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * 构建知识库
     *
     * @param sourcePath 文档源路径（Document source path）
     * @param storagePath 知识库存储路径（Knowledge base storage path）
     * @param rebuild 是否重建（Whether to rebuild）
     * @return 构建结果（Build result）
     */
    public BuildResult buildKnowledgeBase(
            String sourcePath, String storagePath, boolean rebuild) {

        log.info(LogMessageProvider.getMessage("log.kb.scanning", sourcePath));

        BuildResult result =
            new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 扫描文件（Scan files）
            List<File> files = scanDocuments(sourcePath);
            result.setTotalFiles(files.size());

            if (files.isEmpty()) {
                log.warn(LogMessageProvider.getMessage("log.kb.no_documents"));
                log.info(LogMessageProvider.getMessage("log.kb.hint_put_docs", sourcePath));
                log.info(LogMessageProvider.getMessage("log.kb.supported_formats", properties.getDocument().getSupportedFormats()));

                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info(LogMessageProvider.getMessage("log.kb.found_files", files.size()));

            // 2. 检查是否需要构建（Check if build is needed）
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            var stats = rag.getStatistics();
            boolean knowledgeBaseExists = stats.getDocumentCount() > 0;

            if (knowledgeBaseExists && !rebuild) {
                log.info(LogMessageProvider.getMessage("log.kb.exists", stats.getDocumentCount()));
                log.info(LogMessageProvider.getMessage("log.kb.skip_build"));

                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);

                rag.close();
                return result;
            }

            if (knowledgeBaseExists && rebuild) {
                log.info(LogMessageProvider.getMessage("log.kb.rebuild_prepare"));
                // 清空知识库（Clear knowledge base）
                rag.deleteAllDocuments();
                log.info(LogMessageProvider.getMessage("log.kb.old_kb_cleared"));

                // 清空文件追踪信息（Clear file tracking information）
                fileTrackingService.initialize(storagePath);
                fileTrackingService.clearAll();
                log.info(LogMessageProvider.getMessage("log.kb.tracking_cleared"));
            }

            // 3. 处理文档（Process documents）
            log.info(LogMessageProvider.getMessage("log.kb.processing_start"));
            long processStartTime = System.currentTimeMillis();

            // 初始化向量检索引擎（如果启用）（Initialize vector indexing engine if enabled）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info(LogMessageProvider.getMessage("log.kb.vector_enabled"));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.kb.vector_init_failed"), e);
                }
            }

            // 检查是否启用并行处理（Check if parallel processing is enabled）
            boolean useParallel = properties.getDocument().isParallelProcessing()
                && files.size() > 5;

            if (useParallel) {
                int threads = properties.getDocument().getParallelThreads();
                if (threads == 0) {
                    threads = Runtime.getRuntime().availableProcessors();
                }
                log.info(LogMessageProvider.getMessage("log.kb.parallel_mode", threads));
            } else {
                log.info(LogMessageProvider.getMessage("log.kb.serial_mode"));
            }

            int successCount;
            int failedCount;

            // 记录初始内存（Record initial memory）
            optimizer.logMemoryUsage(LogMessageProvider.getMessage("log.kb.memory_before"));

            if (useParallel) {
                // 并行处理（Parallel processing）
                var result_counts = processDocumentsInParallel(
                    files, rag, embeddingEngine, vectorIndexEngine);
                successCount = result_counts[0];
                failedCount = result_counts[1];

                // 标记文件已索引（Mark files as indexed）
                if (rebuild) {
                    for (File file : files) {
                        fileTrackingService.markAsIndexed(file);
                    }
                }
            } else {
                // 串行处理（原有逻辑）（Serial processing (original logic)）
                successCount = 0;
                failedCount = 0;
                List<Document> batchDocuments = new ArrayList<>();

                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);

                    try {
                        // 处理文档并收集到批次（Process document and collect to batch）
                        List<Document> docs = processDocumentOptimized(
                            file, rag, embeddingEngine, vectorIndexEngine);

                        if (docs != null && !docs.isEmpty()) {
                            batchDocuments.addAll(docs);
                            successCount++;

                            // 标记文件已索引（用于增量索引）（Mark file as indexed (for incremental indexing)）
                            if (rebuild) {
                                fileTrackingService.markAsIndexed(file);
                            }

                            // 估算内存使用（Estimate memory usage）
                            long estimatedMemory = docs.stream()
                                .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                                .sum();
                            optimizer.addBatchMemory(estimatedMemory);

                            // 检查是否需要批处理或GC（Check if batch processing or GC is needed）
                            if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                                log.info(LogMessageProvider.getMessage("log.kb.batch_processing", batchDocuments.size(), i + 1, files.size()));

                                rag.commit();
                                batchDocuments.clear();
                                optimizer.resetBatchMemory();
                                optimizer.checkAndTriggerGC();
                            }
                        }

                    } catch (Exception e) {
                        log.error(LogMessageProvider.getMessage("log.kb.file_process_failed", file.getName()), e);
                        failedCount++;
                    }

                    // 定期打印进度和内存状态（Print progress and memory status regularly）
                    if ((i + 1) % 5 == 0 || i == files.size() - 1) {
                        optimizer.logMemoryUsage(
                            String.format("%s %d/%d", LogMessageProvider.getMessage("log.kb.progress"), i + 1, files.size()));
                    }
                }

                // 处理剩余的批次（Process remaining batches）
                if (!batchDocuments.isEmpty()) {
                    log.info(LogMessageProvider.getMessage("log.kb.final_batch", batchDocuments.size()));
                    rag.commit();
                }
            }

            long processEndTime = System.currentTimeMillis();

            // 4. 填充构建结果（Fill build result）
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(processEndTime - processStartTime);

            // 获取峰值内存使用（Get peak memory usage）
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.setPeakMemoryMB(usedMemory / 1024 / 1024);

            // 5. 显示结果（Display results）
            log.info(LogMessageProvider.getMessage("log.kb.build_complete"));
            log.info(LogMessageProvider.getMessage("log.kb.build_separator"));
            log.info(LogMessageProvider.getMessage("log.kb.build_success", result.getSuccessCount()));
            log.info(LogMessageProvider.getMessage("log.kb.build_failed", result.getFailedCount()));
            log.info(LogMessageProvider.getMessage("log.kb.build_total", result.getTotalDocuments()));
            log.info(LogMessageProvider.getMessage("log.kb.build_time", String.format("%.2f", result.getBuildTimeMs() / 1000.0)));
            log.info(LogMessageProvider.getMessage("log.kb.build_memory", result.getPeakMemoryMB()));
            log.info(LogMessageProvider.getMessage("log.kb.build_separator"));

            // 6. 保存文件追踪信息（用于增量索引）（Save file tracking information (for incremental indexing)）
            if (rebuild) {
                fileTrackingService.saveTracking();
                log.info(LogMessageProvider.getMessage("log.kb.tracking_saved"));
            }

            // 7. 优化和提交（Optimize and commit）
            optimizer.commitAndOptimize(rag);

            // 8. 保存向量索引（Save vector index）
            optimizer.saveVectorIndex(vectorIndexEngine);

            // 9. 清理资源（Clean up resources）
            optimizer.closeEmbeddingEngine(embeddingEngine);

            // 10. 最终内存状态（Final memory status）
            optimizer.logMemoryUsage(LogMessageProvider.getMessage("log.kb.memory_after"));

            rag.close();

            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.build_failed"), e);

            result.setError(e.getMessage());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * 增量索引知识库
     * 只处理新增和修改的文档，大幅提升性能
     *
     * @param sourcePath 文档源路径（Document source path）
     * @param storagePath 知识库存储路径（Knowledge base storage path）
     * @return 构建结果（Build result）
     */
    public BuildResult incrementalIndex(
            String sourcePath, String storagePath) {

        log.info(LogMessageProvider.getMessage("log.kb.incremental_start"));
        log.info(LogMessageProvider.getMessage("log.kb.scanning", sourcePath));

        BuildResult result =
            new BuildResult();

        long startTime = System.currentTimeMillis();

        try {
            // 1. 初始化文件追踪（Initialize file tracking）
            fileTrackingService.initialize(storagePath);

            // 1.1 初始化幻灯片缓存服务（Initialize slide cache service）
            if (slideContentCacheService != null) {
                slideContentCacheService.initialize(storagePath);
            }

            // 2. 扫描所有文件（Scan all files）
            List<File> allFiles = scanDocuments(sourcePath);
            result.setTotalFiles(allFiles.size());

            if (allFiles.isEmpty()) {
                log.warn(LogMessageProvider.getMessage("log.kb.no_documents"));
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            log.info(LogMessageProvider.getMessage("log.kb.found_files", allFiles.size()));

            // 3. 筛选需要更新的文件（Filter files that need updating）
            List<File> filesToUpdate = new ArrayList<>();
            for (File file : allFiles) {
                if (fileTrackingService.needsUpdate(file)) {
                    filesToUpdate.add(file);
                }
            }

            log.info(LogMessageProvider.getMessage("log.kb.files_to_update", filesToUpdate.size()));

            if (filesToUpdate.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.kb.up_to_date"));
                LocalFileRAG rag = LocalFileRAG.builder()
                    .storagePath(storagePath)
                    .build();
                var stats = rag.getStatistics();
                result.setSuccessCount(0);
                result.setFailedCount(0);
                result.setTotalDocuments((int) stats.getDocumentCount());
                result.setBuildTimeMs(System.currentTimeMillis() - startTime);
                // 必须关闭以释放锁（Must close to release lock）
                rag.close();
                return result;
            }

            // 4. 打开知识库（Open knowledge base）
            LocalFileRAG rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .build();

            // 5. 初始化向量检索引擎（如果启用）（Initialize vector indexing engine if enabled）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                        properties.getVectorSearch().getIndexPath(),
                        embeddingEngine.getEmbeddingDim()
                    );
                    log.info(LogMessageProvider.getMessage("log.kb.vector_enabled"));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.kb.vector_init_failed"), e);
                }
            }

            // 6. 处理需要更新的文档（Process documents that need updating）
            log.info(LogMessageProvider.getMessage("log.kb.processing_start"));
            int successCount = 0;
            int failedCount = 0;
            List<Document> batchDocuments = new ArrayList<>();

            optimizer.logMemoryUsage(LogMessageProvider.getMessage("log.kb.memory_before"));

            for (int i = 0; i < filesToUpdate.size(); i++) {
                File file = filesToUpdate.get(i);

                try {
                    // 处理文档（Process document）
                    List<Document> docs = processDocumentOptimized(
                        file, rag, embeddingEngine, vectorIndexEngine);

                    if (docs != null && !docs.isEmpty()) {
                        batchDocuments.addAll(docs);
                        successCount++;

                        // 标记为已索引（Mark as indexed）
                        fileTrackingService.markAsIndexed(file);

                        // 估算内存使用（Estimate memory usage）
                        long estimatedMemory = docs.stream()
                            .mapToLong(d -> optimizer.estimateMemoryUsage(d.getContent().length()))
                            .sum();
                        optimizer.addBatchMemory(estimatedMemory);

                        // 检查是否需要批处理或GC（Check if batch processing or GC is needed）
                        if (optimizer.shouldBatch(estimatedMemory) || (i + 1) % 10 == 0) {
                            log.info(LogMessageProvider.getMessage("log.kb.batch_processing", batchDocuments.size(), i + 1, filesToUpdate.size()));

                            rag.commit();
                            batchDocuments.clear();
                            optimizer.resetBatchMemory();
                            optimizer.checkAndTriggerGC();
                        }
                    }

                } catch (Exception e) {
                    log.error(LogMessageProvider.getMessage("log.kb.file_process_failed", file.getName()), e);
                    failedCount++;
                }

                // 定期打印进度（Print progress regularly）
                if ((i + 1) % 5 == 0 || i == filesToUpdate.size() - 1) {
                    optimizer.logMemoryUsage(
                        String.format("%s %d/%d", LogMessageProvider.getMessage("log.kb.progress"), i + 1, filesToUpdate.size()));
                }
            }

            // 处理剩余的批次（Process remaining batches）
            if (!batchDocuments.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.kb.final_batch", batchDocuments.size()));
                rag.commit();
            }

            // 7. 保存文件追踪信息（Save file tracking information）
            fileTrackingService.saveTracking();

            long processEndTime = System.currentTimeMillis();

            // 8. 填充构建结果（Fill build result）
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
            result.setTotalDocuments((int) rag.getStatistics().getDocumentCount());
            result.setBuildTimeMs(processEndTime - startTime);

            // 获取峰值内存使用（Get peak memory usage）
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.setPeakMemoryMB(usedMemory / 1024 / 1024);

            // 9. 显示结果（Display results）
            log.info(LogMessageProvider.getMessage("log.kb.incremental_complete"));
            log.info(LogMessageProvider.getMessage("log.kb.build_separator"));
            log.info(LogMessageProvider.getMessage("log.kb.incremental_files", filesToUpdate.size()));
            log.info(LogMessageProvider.getMessage("log.kb.build_success", result.getSuccessCount()));
            log.info(LogMessageProvider.getMessage("log.kb.build_failed", result.getFailedCount()));
            log.info(LogMessageProvider.getMessage("log.kb.build_total", result.getTotalDocuments()));
            log.info(LogMessageProvider.getMessage("log.kb.build_time", String.format("%.2f", result.getBuildTimeMs() / 1000.0)));
            log.info(LogMessageProvider.getMessage("log.kb.build_memory", result.getPeakMemoryMB()));
            log.info(LogMessageProvider.getMessage("log.kb.build_separator"));

            // 10. 优化和提交（Optimize and commit）
            optimizer.commitAndOptimize(rag);

            // 11. 保存向量索引（Save vector index）
            optimizer.saveVectorIndex(vectorIndexEngine);

            // 12. 清理资源（Clean up resources）
            optimizer.closeEmbeddingEngine(embeddingEngine);

            // 13. 最终内存状态（Final memory status）
            optimizer.logMemoryUsage(LogMessageProvider.getMessage("log.kb.memory_after"));

            // 必须关闭 RAG 实例以释放 Lucene 写锁（Must close RAG instance to release Lucene write lock）
            rag.close();

            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.incremental_failed"), e);

            result.setError(e.getMessage());
            result.setBuildTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * 扫描文档文件（Scan document files）
     */
    private List<File> scanDocuments(String sourcePath) throws IOException {
        log.info(LogMessageProvider.getMessage("log.kb.source_path", sourcePath));

        // 处理 classpath: 前缀（Handle classpath: prefix）
        if (sourcePath.startsWith("classpath:")) {
            return scanClasspathResources(sourcePath.substring("classpath:".length()));
        }

        // 处理普通文件系统路径（Handle regular file system paths）
        File sourceFile = new File(sourcePath);

        if (!sourceFile.exists()) {
            log.warn(LogMessageProvider.getMessage("log.kb.path_not_exists", sourcePath));
            return Collections.emptyList();
        }

        List<File> files = new ArrayList<>();

        if (sourceFile.isFile()) {
            // 单个文件（Single file）
            if (isSupportedFile(sourceFile)) {
                files.add(sourceFile);
            }
        } else if (sourceFile.isDirectory()) {
            // 文件夹 - 递归扫描（Folder - recursive scan）
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
     * 扫描 classpath 资源（Scan classpath resources）
     */
    private List<File> scanClasspathResources(String resourcePath) throws IOException {
        log.info(LogMessageProvider.getMessage("log.kb.scan_classpath", resourcePath));

        List<File> files = new ArrayList<>();

        try {
            // 获取资源 URL（Get resource URL）
            var resource = getClass().getClassLoader().getResource(resourcePath);

            if (resource == null) {
                log.warn(LogMessageProvider.getMessage("log.kb.classpath_not_exists", resourcePath));
                return files;
            }

            log.info(LogMessageProvider.getMessage("log.kb.resource_found", resource));

            // 转换为 File 对象（Convert to File object）
            File resourceFile = new File(resource.toURI());

            if (!resourceFile.exists()) {
                log.warn(LogMessageProvider.getMessage("log.kb.resource_file_not_exists", resourceFile.getAbsolutePath()));
                return files;
            }

            log.info(LogMessageProvider.getMessage("log.kb.resource_path", resourceFile.getAbsolutePath()));

            if (resourceFile.isFile()) {
                // 单个文件（Single file）
                if (isSupportedFile(resourceFile)) {
                    files.add(resourceFile);
                    log.info(LogMessageProvider.getMessage("log.kb.add_file", resourceFile.getName()));
                }
            } else if (resourceFile.isDirectory()) {
                // 目录 - 递归扫描（Directory - recursive scan）
                log.info(LogMessageProvider.getMessage("log.kb.scan_directory"));
                try (var stream = Files.walk(resourceFile.toPath())) {
                    stream.filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .filter(this::isSupportedFile)
                        .forEach(f -> {
                            files.add(f);
                            log.debug("   - {}", f.getName());
                        });
                }
                log.info(LogMessageProvider.getMessage("log.kb.files_found", files.size()));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.scan_classpath_failed", resourcePath), e);
            throw new IOException(LogMessageProvider.getMessage("kb_service.error.scan_classpath_failed"), e);
        }

        return files;
    }

    /**
     * 判断是否支持的文件格式 / Check if file format is supported
     */
    private boolean isSupportedFile(File file) {
        String fileName = file.getName().toLowerCase();
        List<String> supportedFormats = properties.getDocument().getSupportedFormats();

        return supportedFormats.stream()
            .anyMatch(format -> fileName.endsWith("." + format));
    }

    /**
     * 并行处理文档列表（Parallel processing of document list）
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

        // 创建线程池（Create thread pool）
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            // 分批处理文件（Process files in batches）
            for (int i = 0; i < totalFiles; i += batchSize) {
                final int batchEnd = Math.min(i + batchSize, totalFiles);
                List<File> batch = filesToProcess.subList(i, batchEnd);

                Future<?> future = executor.submit(() -> {
                    // 每个线程独立的文档列表（Each thread has its own document list）
                    List<Document> threadDocuments = new ArrayList<>();

                    for (File file : batch) {
                        try {
                            // 处理文档（Process document）
                            List<Document> docs = processDocumentOptimized(
                                file, rag, embeddingEngine, vectorIndexEngine);

                            if (docs != null && !docs.isEmpty()) {
                                threadDocuments.addAll(docs);
                                successCount.incrementAndGet();

                                // 标记文件已索引（Mark file as indexed）
                                fileTrackingService.markAsIndexed(file);
                            }

                        } catch (Exception e) {
                            log.error(LogMessageProvider.getMessage("log.kb.file_process_failed", file.getName()), e);
                            failedCount.incrementAndGet();
                        }

                        // 更新进度（Update progress）
                        int current = processedCount.incrementAndGet();
                        if (current % 10 == 0 || current == totalFiles) {
                            log.info(LogMessageProvider.getMessage("log.kb.parallel_progress", current, totalFiles, successCount.get(), failedCount.get()));

                            optimizer.logMemoryUsage(
                                String.format("%s %d/%d", LogMessageProvider.getMessage("log.kb.parallel_memory"), current, totalFiles));
                        }
                    }

                    // 批次提交（使用 RAG 的同步机制）（Batch commit (using RAG's synchronization mechanism)）
                    synchronized (rag) {
                        if (!threadDocuments.isEmpty()) {
                            log.info(LogMessageProvider.getMessage("log.kb.batch_commit", threadDocuments.size()));
                            rag.commit();
                        }
                    }

                    // 定期触发GC（Trigger GC regularly）
                    if (processedCount.get() % (batchSize * 3) == 0) {
                        optimizer.checkAndTriggerGC();
                    }
                });

                futures.add(future);
            }

            // 等待所有任务完成（Wait for all tasks to complete）
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error(LogMessageProvider.getMessage("log.kb.batch_task_failed"), e);
                }
            }

        } finally {
            // 关闭线程池（Shutdown thread pool）
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

        // 最后提交一次（Final commit）
        rag.commit();

        return new int[]{successCount.get(), failedCount.get()};
    }

    /**
     * 处理单个文档（优化版，返回文档列表以支持批处理）（Process single document (optimized version, returns document list to support batch processing)）
     */
    private List<Document> processDocumentOptimized(File file, LocalFileRAG rag,
                                                     LocalEmbeddingEngine embeddingEngine,
                                                     SimpleVectorIndexEngine vectorIndexEngine) {

        log.info(LogMessageProvider.getMessage("log.kb.processing_file", file.getName(), file.length() / 1024));
        List<Document> createdDocuments = new ArrayList<>();

        try {
            // 1. 检查文件大小（Check file size）
            if (!optimizer.checkFileSize(file.length())) {
                log.warn(LogMessageProvider.getMessage("log.kb.file_too_large", file.length() / 1024 / 1024, properties.getDocument().getMaxFileSizeMb()));
                return createdDocuments;
            }

            // 2. 解析文档内容（Parse document content）
            String content = documentParser.parse(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn(LogMessageProvider.getMessage("log.kb.content_empty"));
                return createdDocuments;
            }

            int originalLength = content.length();

            // 2.1 立即截断超大内容，防止后续处理内存溢出（Immediately truncate oversized content to prevent memory overflow）
            // 这是关键：在索引阶段就限制大小，而不是在问答时才处理（This is key: limit size at indexing stage, not at Q&A time）
            int maxContentLength = properties.getDocument().getMaxIndexContentLength();
            if (content.length() > maxContentLength) {
                log.warn(LogMessageProvider.getMessage("log.kb.content_too_large", originalLength, originalLength / 512, maxContentLength));
                content = content.substring(0, maxContentLength);
                log.info(LogMessageProvider.getMessage("log.kb.content_truncated", originalLength - maxContentLength, (originalLength - maxContentLength) * 100 / originalLength));
            }

            log.info(LogMessageProvider.getMessage("log.kb.content_extracted", content.length()));

            // 2.5 提取图片并将图片信息文本化添加到内容中（关键优化）/ Extract images and add image information to content as text (key optimization)
            // 这样图片信息会被索引和向量化，在问答时直接可用，不需要重新处理 / This way image information will be indexed and vectorized, directly available at Q&A time without reprocessing
            if (imageExtractionService != null && imageExtractionService.supportsDocument(file.getName())) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> images =
                        imageExtractionService.extractAndSaveImages(file, file.getName());

                    if (!images.isEmpty()) {
                        log.info(LogMessageProvider.getMessage("log.kb.images_extracted", images.size()));

                        // 将图片信息添加到文档内容中，这样就可以被检索到 / Add image information to document content so it can be retrieved
                        StringBuilder imageText = new StringBuilder();
                        imageText.append(LogMessageProvider.getMessage("kb_service.image.section_title"));

                        for (int i = 0; i < images.size(); i++) {
                            top.yumbo.ai.rag.image.ImageInfo img = images.get(i);
                            imageText.append(LogMessageProvider.getMessage("kb_service.image.image_number", i + 1));
                            imageText.append("\n").append(LogMessageProvider.getMessage("kb_service.image.filename", img.getFilename())).append("\n");
                            imageText.append(LogMessageProvider.getMessage("kb_service.image.url", img.getUrl())).append("\n");

                            if (img.getDescription() != null && !img.getDescription().isEmpty()) {
                                imageText.append(LogMessageProvider.getMessage("kb_service.image.description", img.getDescription())).append("\n");
                            }

                            if (img.getOriginalFilename() != null) {
                                imageText.append(LogMessageProvider.getMessage("kb_service.image.original_file", img.getOriginalFilename())).append("\n");
                            }
                        }

                        imageText.append(LogMessageProvider.getMessage("kb_service.image.section_end"));

                        // 将图片信息添加到内容末尾 / Add image information to the end of content
                        content = content + imageText.toString();

                        log.info(LogMessageProvider.getMessage("log.kb.images_added"));
                    }
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.kb.image_extraction_failed", e.getMessage()));
                    // 不中断文档处理流程 / Do not interrupt document processing flow
                }
            }

            // 3. 检查内容大小并判断分块策略（Check content size and determine chunking strategy）
            boolean forceChunk = optimizer.needsForceChunking(content.length());
            boolean autoChunk = optimizer.shouldAutoChunk(content.length());

            if (forceChunk) {
                log.warn(LogMessageProvider.getMessage("log.kb.force_chunk", content.length() / 1024 / 1024));
            } else if (autoChunk) {
                log.info(LogMessageProvider.getMessage("log.kb.auto_chunk", content.length() / 1024));
            }

            // 4. 创建文档（Create document）
            Document document = Document.builder()
                .title(file.getName())
                .content(content)
                .metadata(buildMetadata(file))
                .build();

            // 5. 判断是否需要分块（Determine if chunking is needed）
            List<Document> documentsToIndex;

            if (forceChunk || autoChunk) {
                documentsToIndex = documentChunker.chunk(document);
                log.info(LogMessageProvider.getMessage("log.kb.chunked", documentsToIndex.size()));
            } else {
                documentsToIndex = List.of(document);
            }

            // 6. 索引文档（Index documents）
            for (Document doc : documentsToIndex) {
                String docId = rag.index(doc);
                doc.setId(docId);
                createdDocuments.add(doc);

                // 7. 生成向量索引（如果启用）（Generate vector index if enabled）
                if (embeddingEngine != null && vectorIndexEngine != null) {
                    try {
                        float[] vector = embeddingEngine.embed(doc.getContent());
                        vectorIndexEngine.addDocument(docId, vector);
                    } catch (Exception e) {
                        log.debug(LogMessageProvider.getMessage("log.kb.vector_generation_failed", e.getMessage()));
                    }
                }
            }

            log.info(LogMessageProvider.getMessage("log.kb.indexing_complete", createdDocuments.size()));

            return createdDocuments;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.processing_failed"), e);
            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.process_failed", file.getName()), e);
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

    /**
     * 增量索引单个文件（用于问答归档）（Incremental index single file (for Q&A archiving)）
     *
     * @param filePath 文件路径（File path）
     */
    public void incrementalIndexFile(Path filePath) {
        try {
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            File file = filePath.toFile();

            if (!file.exists()) {
                log.warn(LogMessageProvider.getMessage("log.kb.file_not_exists", filePath));
                return;
            }

            log.info(LogMessageProvider.getMessage("log.kb.index_single_file", file.getName()));

            // 打开知识库（Open knowledge base）
            LocalFileRAG rag = LocalFileRAG.builder()
                    .storagePath(storagePath)
                    .build();

            // 初始化向量检索引擎（如果启用）（Initialize vector indexing engine if enabled）
            LocalEmbeddingEngine embeddingEngine = null;
            SimpleVectorIndexEngine vectorIndexEngine = null;

            if (properties.getVectorSearch().isEnabled()) {
                try {
                    embeddingEngine = new LocalEmbeddingEngine();
                    vectorIndexEngine = new SimpleVectorIndexEngine(
                            properties.getVectorSearch().getIndexPath(),
                            embeddingEngine.getEmbeddingDim()
                    );
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.kb.vector_init_failed"), e);
                }
            }

            // 处理文档（Process document）
            List<Document> docs = processDocumentOptimized(
                    file, rag, embeddingEngine, vectorIndexEngine);

            if (docs != null && !docs.isEmpty()) {
                rag.commit();
                log.info(LogMessageProvider.getMessage("log.kb.file_indexed", file.getName()));
            }

            // 必须关闭以释放锁（Must close to release lock）
            rag.close();

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kb.index_file_failed", filePath), e);
        }
    }
}
