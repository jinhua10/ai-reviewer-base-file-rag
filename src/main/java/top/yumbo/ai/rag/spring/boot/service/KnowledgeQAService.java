package top.yumbo.ai.rag.spring.boot.service;

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.SmartContextBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 知识库问答服务
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class KnowledgeQAService {

    private final KnowledgeQAProperties properties;
    private final KnowledgeBaseService knowledgeBaseService;
    private final HybridSearchService hybridSearchService;
    private final SearchSessionService sessionService;
    private final SearchConfigService configService;
    private final LLMClient llmClient;
    private final top.yumbo.ai.rag.chunking.storage.ChunkStorageService chunkStorageService;
    private final top.yumbo.ai.rag.image.ImageStorageService imageStorageService;
    private final top.yumbo.ai.rag.feedback.QARecordService qaRecordService;
    private final SimilarQAService similarQAService;  // 新增

    private LocalFileRAG rag;
    private LocalEmbeddingEngine embeddingEngine;
    private SimpleVectorIndexEngine vectorIndexEngine;
    private top.yumbo.ai.rag.optimization.SmartContextBuilder contextBuilder;

    public KnowledgeQAService(KnowledgeQAProperties properties,
                              KnowledgeBaseService knowledgeBaseService,
                              HybridSearchService hybridSearchService,
                              SearchSessionService sessionService,
                              SearchConfigService configService,
                              LLMClient llmClient,
                              top.yumbo.ai.rag.chunking.storage.ChunkStorageService chunkStorageService,
                              top.yumbo.ai.rag.image.ImageStorageService imageStorageService,
                              top.yumbo.ai.rag.feedback.QARecordService qaRecordService,
                              SimilarQAService similarQAService) {  // 新增
        this.properties = properties;
        this.knowledgeBaseService = knowledgeBaseService;
        this.hybridSearchService = hybridSearchService;
        this.sessionService = sessionService;
        this.configService = configService;
        this.llmClient = llmClient;
        this.chunkStorageService = chunkStorageService;
        this.imageStorageService = imageStorageService;
        this.qaRecordService = qaRecordService;
        this.similarQAService = similarQAService;  // 新增
    }

    /**
     * 初始化问答系统
     */
    @PostConstruct
    public void initialize() {
        log.info("=".repeat(80));
        log.info("📚 知识库问答系统初始化中...");
        log.info("=".repeat(80));

        try {
            // 1. 初始化知识库
            initializeKnowledgeBase();

            // 2. 初始化向量检索
            initializeVectorSearch();

            // 3. 初始化LLM客户端
            initializeLLMClient();

            // 4. 创建问答系统
            createQASystem();

            log.info("=".repeat(80));
            log.info("✅ 知识库问答系统初始化完成！");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("❌ 知识库问答系统初始化失败", e);
            throw new RuntimeException("系统初始化失败", e);
        }
    }

    /**
     * 初始化知识库
     */
    private void initializeKnowledgeBase() {
        log.info("\n🔨 步骤1: 初始化知识库");

        String storagePath = properties.getKnowledgeBase().getStoragePath();
        String sourcePath = properties.getKnowledgeBase().getSourcePath();
        boolean rebuildOnStartup = properties.getKnowledgeBase().isRebuildOnStartup();

        log.info("   - 存储路径: {}", storagePath);
        log.info("   - 文档路径: {}", sourcePath);

        if (rebuildOnStartup) {
            log.info("   - 索引模式: 完全重建（配置要求）");
        } else {
            log.info("   - 索引模式: 增量索引（默认模式）");
        }

        // 检查源路径类型
        if (sourcePath.startsWith("classpath:")) {
            log.info("   - 路径类型: classpath 资源");
        } else {
            log.info("   - 路径类型: 文件系统路径");
        }

        // 构建知识库 - 启动时使用增量索引，除非配置要求重建
        BuildResult buildResult;

        if (rebuildOnStartup) {
            log.info("   🚀 开始完全重建知识库...");
            buildResult = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);
        } else {
            log.info("   🔄 开始增量索引知识库...");
            buildResult = knowledgeBaseService.buildKnowledgeBaseWithIncrementalIndex(sourcePath, storagePath);
        }

        if (buildResult.getError() != null) {
            throw new RuntimeException("知识库构建失败: " + buildResult.getError());
        }

        log.info("   ✅ 知识库构建完成");
        log.info("      - 总文件数: {}", buildResult.getTotalFiles());
        log.info("      - 处理文件: {}", buildResult.getSuccessCount());
        log.info("      - 失败文件: {}", buildResult.getFailedCount());
        log.info("      - 总文档数: {}", buildResult.getTotalDocuments());

        // 连接到知识库
        rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .enableCache(properties.getKnowledgeBase().isEnableCache())
                .build();

        var stats = rag.getStatistics();
        log.info("   ✅ 知识库已就绪");
        log.info("      - 文档数: {}", stats.getDocumentCount());
        log.info("      - 索引数: {}", stats.getIndexedDocumentCount());
    }

    /**
     * 初始化向量检索
     */
    private void initializeVectorSearch() {
        if (!properties.getVectorSearch().isEnabled()) {
            log.info("\n⚠️  向量检索已禁用（配置项: knowledge.qa.vector-search.enabled=false）");
            return;
        }

        log.info("\n🚀 步骤2: 初始化向量检索引擎");

        try {
            // 初始化嵌入引擎
            embeddingEngine = new LocalEmbeddingEngine();

            log.info("   ✅ 向量嵌入引擎已加载");
            log.info("      - 模型: {}", embeddingEngine.getModelName());
            log.info("      - 维度: {}", embeddingEngine.getEmbeddingDim());

            // 加载向量索引
            String indexPath = properties.getVectorSearch().getIndexPath();
            vectorIndexEngine = new SimpleVectorIndexEngine(
                    indexPath,
                    embeddingEngine.getEmbeddingDim()
            );

            log.info("   ✅ 向量索引已加载");
            log.info("      - 索引路径: {}", indexPath);
            log.info("      - 向量数量: {}", vectorIndexEngine.size());

        } catch (OrtException | IOException e) {
            log.error("❌ 向量检索引擎初始化失败", e);
            log.warn("💡 提示：请确保模型文件已下载到 resources/models/ 目录");
            log.warn("      详细说明请查看: 模型下载说明.md");
            embeddingEngine = null;
            vectorIndexEngine = null;
        }
    }

    /**
     * 初始化LLM客户端
     */
    private void initializeLLMClient() {
        log.info("\n🤖 步骤3: 初始化LLM客户端");

        String provider = properties.getLlm().getProvider();
        log.info("   - 提供商: {}", provider);
        log.info("   - 客户端类型: {}", llmClient.getClass().getSimpleName());

        log.info("   ✅ LLM客户端已就绪");
    }

    /**
     * 创建问答系统
     */
    private void createQASystem() {
        log.info("\n📝 步骤4: 创建问答系统");

        // 获取切分策略配置
        String strategyName = properties.getLlm().getChunkingStrategy();
        top.yumbo.ai.rag.chunking.ChunkingStrategy strategy =
            top.yumbo.ai.rag.chunking.ChunkingStrategy.fromString(strategyName);

        // 初始化智能上下文构建器（使用新的构造函数，包含存储服务）
        contextBuilder = new SmartContextBuilder(
            properties.getLlm().getMaxContextLength(),
            properties.getLlm().getMaxDocLength(),
            true, // preserveFullContent（由策略控制，保留兼容性）
            properties.getLlm().getChunking(),
            strategy,
            llmClient,
            chunkStorageService  // 传递块存储服务
        );

        log.info("   ✅ 智能上下文构建器已初始化");
        log.info("      - 最大上下文: {} 字符", properties.getLlm().getMaxContextLength());
        log.info("      - 最大文档长度: {} 字符", properties.getLlm().getMaxDocLength());
        log.info("      - 切分策略: {} ({})", strategy, strategy.getDescription());
        log.info("      - 块大小: {} 字符", properties.getLlm().getChunking().getChunkSize());
        log.info("      - 块重叠: {} 字符", properties.getLlm().getChunking().getChunkOverlap());

        if (strategy == top.yumbo.ai.rag.chunking.ChunkingStrategy.AI_SEMANTIC
            && properties.getLlm().getChunking().getAiChunking().isEnabled()) {
            log.info("      - AI 切分: 启用 (模型: {})",
                properties.getLlm().getChunking().getAiChunking().getModel());
        }

        if (embeddingEngine != null && vectorIndexEngine != null) {
            log.info("   ✅ 使用向量检索增强模式");
        } else {
            log.info("   ✅ 使用关键词检索模式");
        }
    }

    /**
     * 提问
     *
     * @param question 问题
     * @return 回答
     */
    public AIAnswer ask(String question) {
        if (rag == null || llmClient == null) {
            throw new IllegalStateException("问答系统未初始化");
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info("\n" + "=".repeat(80));
            log.info("❓ 问题: {}", question);
            log.info("=".repeat(80));

            // 步骤0: 搜索相似问题（在检索文档之前）
            List<SimilarQAService.SimilarQA> similarQuestions = null;
            try {
                similarQuestions = similarQAService.findSimilar(question, 30, 3);  // minScore=30, limit=3
                if (!similarQuestions.isEmpty()) {
                    log.info("💡 找到 {} 个相似历史问答", similarQuestions.size());
                }
            } catch (Exception e) {
                log.warn("⚠️ 查找相似问题失败: {}", e.getMessage());
            }

            // 步骤1: 检索相关文档
            List<top.yumbo.ai.rag.model.Document> documents;

            if (embeddingEngine != null && vectorIndexEngine != null) {
                // 使用混合检索
                documents = hybridSearchService.hybridSearch(question, rag, embeddingEngine, vectorIndexEngine);
                log.info("✅ 使用混合检索（Lucene + Vector）");
            } else {
                // 使用纯关键词检索
                documents = hybridSearchService.keywordSearch(question, rag);
                log.info("✅ 使用关键词检索");
            }

            // 根据配置限制文档数量，使用会话管理支持分页引用
            int docsPerQuery = configService.getDocumentsPerQuery();
            int totalDocs = documents.size();
            boolean hasMoreDocs = false;
            List<top.yumbo.ai.rag.model.Document> remainingDocs = new ArrayList<>();
            String sessionId = null;

            // 创建会话以支持分页引用
            if (totalDocs > 0) {
                sessionId = sessionService.createSession(question, documents, docsPerQuery);

                // 获取第一批文档
                SearchSessionService.SessionDocuments firstBatch =
                    sessionService.getCurrentDocuments(sessionId);
                documents = firstBatch.getDocuments();
                hasMoreDocs = firstBatch.isHasNext();

                log.info("📝 创建会话: sessionId={}, 总文档数={}, 本次使用={}, 剩余={}",
                    sessionId, totalDocs, documents.size(), firstBatch.getRemainingDocuments());
            }

            if (totalDocs > docsPerQuery) {
                log.warn("⚠️ 检索到 {} 个文档，本次处理前 {} 个（配置: documents-per-query）",
                        totalDocs, docsPerQuery);

                log.info("📋 剩余 {} 个文档未处理，用户可继续提问", remainingDocs.size());
            } else {
                log.info("✅ 检索到 {} 个高相关性文档，全部纳入回答", totalDocs);
            }

            // 步骤2: 构建智能上下文
            // 设置当前文档ID（用于保存切分块）
            if (!documents.isEmpty() && contextBuilder != null) {
                String firstDocTitle = documents.get(0).getTitle();
                contextBuilder.setCurrentDocumentId(firstDocTitle);
            }

            String context = contextBuilder.buildSmartContext(question, documents);
            log.info("Context stats: {}", contextBuilder.getContextStats(context));

            // 步骤3: 收集可用的图片信息
            List<top.yumbo.ai.rag.image.ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        imageContext.append("\n\n【可用图片 - ").append(doc.getTitle()).append("】\n");
                        for (int i = 0; i < Math.min(docImages.size(), 5); i++) { // 最多列出 5 张图片
                            top.yumbo.ai.rag.image.ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : "相关图片";
                            imageContext.append(String.format(
                                "- 图片 %d: %s (引用方式: ![%s](%s))\n",
                                i + 1, imgDesc, imgDesc, img.getUrl()
                            ));
                        }
                        if (docImages.size() > 5) {
                            imageContext.append(String.format("  ... 还有 %d 张图片\n", docImages.size() - 5));
                        }
                    }
                } catch (Exception e) {
                    log.debug("未找到文档图片: {}", doc.getTitle());
                }
            }

            // 步骤4: 构建增强的 Prompt（包含图片信息和文档说明）
            List<String> usedDocTitles = documents.stream()
                    .map(top.yumbo.ai.rag.model.Document::getTitle)
                    .distinct()
                    .toList();

            String prompt = buildEnhancedPrompt(
                question,
                context,
                imageContext.toString(),
                !allImages.isEmpty(),
                usedDocTitles,
                hasMoreDocs,
                remainingDocs.size()
            );

            if (!allImages.isEmpty()) {
                log.info("🖼️ 上下文中包含 {} 张图片信息", allImages.size());
            }

            log.info("📚 本次使用 {} 个文档生成回答", usedDocTitles.size());
            if (hasMoreDocs) {
                log.info("ℹ️ 还有 {} 个相关文档未包含在本次回答中", remainingDocs.size());
            }

            // 步骤5: 调用 LLM 生成答案
            String answer = llmClient.generate(prompt);


            // 步骤6: 提取文档来源
            List<String> sources = documents.stream()
                    .map(Document::getTitle)
                    .distinct()
                    .toList();

            // 步骤7: 获取切分块信息
            List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks = Collections.emptyList();
            List<top.yumbo.ai.rag.image.ImageInfo> images = Collections.emptyList();

            if (!documents.isEmpty()) {
                String firstDocTitle = documents.get(0).getTitle();
                try {
                    chunks = chunkStorageService.listChunks(firstDocTitle);
                    images = imageStorageService.listImages(firstDocTitle);
                    log.info("📦 Found {} chunks and {} images for document", chunks.size(), images.size());
                } catch (Exception e) {
                    log.warn("Failed to load chunks/images info", e);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果
            log.info("\n💡 回答:");
            log.info(answer);
            log.info("\n📚 数据来源 (共{}个文档):", sources.size());
            sources.forEach(source -> log.info("   - {}", source));
            log.info("\n⏱️  响应时间: {}ms", totalTime);
            log.info("=".repeat(80));

            // 保存问答记录（用于反馈和优化）
            String recordId = saveQARecord(question, answer, sources, usedDocTitles, totalTime);

            AIAnswer aiAnswer = new AIAnswer(
                answer,
                sources,
                totalTime,
                chunks,
                images,
                usedDocTitles,      // 本次使用的文档
                totalDocs,          // 检索到的总文档数
                hasMoreDocs         // 是否还有更多文档
            );

            // 设置记录ID，方便后续反馈
            aiAnswer.setRecordId(recordId);

            // 设置相似问题推荐
            if (similarQuestions != null && !similarQuestions.isEmpty()) {
                aiAnswer.setSimilarQuestions(similarQuestions);
            };

            // 设置会话ID，支持分页引用
            aiAnswer.setSessionId(sessionId);

            return aiAnswer;

        } catch (Exception e) {
            log.error("❌ 问答处理失败", e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                    "抱歉，处理您的问题时出现错误：" + e.getMessage(),
                    List.of(),
                    totalTime
            );
        }
    }

    /**
     * 使用会话中的特定批次文档进行问答
     *
     * @param question 问题
     * @param sessionId 会话ID
     * @return 回答
     */
    public AIAnswer askWithSessionDocuments(String question, String sessionId) {
        if (rag == null || llmClient == null) {
            throw new IllegalStateException("问答系统未初始化");
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info("\n" + "=".repeat(80));
            log.info("❓ 问题: {} (使用会话: {})", question, sessionId);
            log.info("=".repeat(80));

            // 从会话获取当前批次的文档
            SearchSessionService.SessionDocuments sessionDocs =
                sessionService.getCurrentDocuments(sessionId);

            List<top.yumbo.ai.rag.model.Document> documents = sessionDocs.getDocuments();

            log.info("📝 使用会话文档: 总{}个, 当前第{}页/{}, 本次使用{}个",
                sessionDocs.getTotalDocuments(),
                sessionDocs.getCurrentPage(),
                sessionDocs.getTotalPages(),
                documents.size());

            // 获取会话信息
            SearchSessionService.SessionInfo sessionInfo =
                sessionService.getSessionInfo(sessionId);

            // 步骤2: 构建智能上下文
            if (!documents.isEmpty() && contextBuilder != null) {
                String firstDocTitle = documents.get(0).getTitle();
                contextBuilder.setCurrentDocumentId(firstDocTitle);
            }

            String context = contextBuilder.buildSmartContext(question, documents);
            log.info("Context stats: {}", contextBuilder.getContextStats(context));

            // 步骤3: 收集可用的图片信息
            List<top.yumbo.ai.rag.image.ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        imageContext.append("\n\n【可用图片 - ").append(doc.getTitle()).append("】\n");
                        for (int i = 0; i < Math.min(docImages.size(), 5); i++) {
                            top.yumbo.ai.rag.image.ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : "相关图片";
                            imageContext.append(String.format(
                                "- 图片 %d: %s (引用方式: ![%s](%s))\n",
                                i + 1, imgDesc, imgDesc, img.getUrl()
                            ));
                        }
                        if (docImages.size() > 5) {
                            imageContext.append(String.format("  ...还有 %d 张图片\n", docImages.size() - 5));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load images for document: {}", doc.getTitle(), e);
                }
            }

            // 步骤4: 构建增强的 Prompt
            List<String> usedDocTitles = documents.stream()
                .map(top.yumbo.ai.rag.model.Document::getTitle)
                .distinct()
                .toList();

            boolean hasMoreDocs = sessionInfo.isHasNext();
            int remainingDocsCount = sessionInfo.getRemainingDocuments();

            String prompt = buildEnhancedPrompt(
                question,
                context,
                imageContext.toString(),
                !allImages.isEmpty(),
                usedDocTitles,
                hasMoreDocs,
                remainingDocsCount
            );

            if (!allImages.isEmpty()) {
                log.info("🖼️ 上下文中包含 {} 张图片信息", allImages.size());
            }

            log.info("📚 本次使用 {} 个文档生成回答", usedDocTitles.size());
            if (hasMoreDocs) {
                log.info("ℹ️ 还有 {} 个相关文档未包含在本次回答中", remainingDocsCount);
            }

            // 步骤5: 调用 LLM 生成答案
            String answer = llmClient.generate(prompt);

            // 步骤6: 提取文档来源
            List<String> sources = documents.stream()
                    .map(Document::getTitle)
                    .distinct()
                    .toList();

            // 步骤7: 获取切分块信息
            List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks = Collections.emptyList();
            List<top.yumbo.ai.rag.image.ImageInfo> images = Collections.emptyList();

            if (!documents.isEmpty()) {
                String firstDocTitle = documents.get(0).getTitle();
                try {
                    chunks = chunkStorageService.listChunks(firstDocTitle);
                    images = imageStorageService.listImages(firstDocTitle);
                    log.info("📦 Found {} chunks and {} images for document", chunks.size(), images.size());
                } catch (Exception e) {
                    log.warn("Failed to load chunks/images info", e);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果
            log.info("\n💡 回答:");
            log.info(answer);
            log.info("\n📚 数据来源 (共{}个文档):", sources.size());
            sources.forEach(source -> log.info("   - {}", source));
            log.info("\n⏱️  响应时间: {}ms", totalTime);
            log.info("=".repeat(80));

            // 保存问答记录
            String recordId = saveQARecord(question, answer, sources, usedDocTitles, totalTime);

            AIAnswer aiAnswer = new AIAnswer(
                answer,
                sources,
                totalTime,
                chunks,
                images,
                usedDocTitles,
                sessionInfo.getTotalDocuments(),
                hasMoreDocs
            );

            aiAnswer.setRecordId(recordId);
            aiAnswer.setSessionId(sessionId);

            return aiAnswer;

        } catch (Exception e) {
            log.error("❌ 使用会话文档问答失败", e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                    "抱歉，处理您的问题时出现错误：" + e.getMessage(),
                    List.of(),
                    totalTime
            );
        }
    }

    /**
     * 构建 LLM Prompt
     */
    private String buildPrompt(String question, String context) {
        // 从配置中获取提示词模板
        String template = properties.getLlm().getPromptTemplate();

        // 替换占位符
        return template
                .replace("{question}", question)
                .replace("{context}", context);
    }

    /**
     * 构建增强的 LLM Prompt（包含图片信息和文档使用说明）
     *
     * @param question 用户问题
     * @param context 文本上下文
     * @param imageContext 图片上下文（图片URL和描述）
     * @param hasImages 是否有可用图片
     * @param usedDocuments 本次使用的文档列表
     * @param hasMoreDocs 是否还有更多文档未处理
     * @param remainingCount 剩余文档数量
     * @return 增强的 Prompt
     */
    private String buildEnhancedPrompt(String question, String context, String imageContext,
                                      boolean hasImages, List<String> usedDocuments,
                                      boolean hasMoreDocs, int remainingCount) {
        // 从配置中获取提示词模板
        String template = properties.getLlm().getPromptTemplate();

        // 构建增强内容
        StringBuilder enhancement = new StringBuilder();

        // 添加图片使用指南
        if (hasImages && !imageContext.isEmpty()) {
            enhancement.append("\n\n**重要提示**：\n");
            enhancement.append("1. 以下是知识库中与问题相关的图片资源，你可以在回答中引用这些图片。\n");
            enhancement.append("2. 如果回答涉及到这些图片的内容（如架构图、流程图、数据图表等），请使用 Markdown 格式引用图片。\n");
            enhancement.append("3. 引用格式已在下方提供，直接复制使用即可。\n");
            enhancement.append("4. 请确保引用的图片 URL 完整且正确。\n");
            enhancement.append(imageContext);
        }

        // 添加文档使用说明
        if (!usedDocuments.isEmpty()) {
            enhancement.append("\n\n**本次参考的文档**：\n");
            for (int i = 0; i < usedDocuments.size(); i++) {
                enhancement.append(String.format("%d. %s\n", i + 1, usedDocuments.get(i)));
            }
        }

        // 如果有更多未处理的文档，提示用户
        if (hasMoreDocs && remainingCount > 0) {
            enhancement.append(String.format(
                "\n\n**提示**：检索到的相关文档较多，本次回答基于前 %d 个最相关的文档。" +
                "还有 %d 个相关文档未包含在本次回答中。" +
                "如果需要查看更多信息，请告知用户可以继续提问相关问题。\n",
                usedDocuments.size(), remainingCount
            ));
        }

        // 替换占位符
        return template.replace("{question}", question)
                       .replace("{context}", context) +
               enhancement.toString();
    }

    /**
     * 获取知识库统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        if (rag == null) {
            throw new IllegalStateException("知识库未初始化");
        }
        return rag.getStatistics();
    }

    /**
     * 获取增强的统计信息（包含文件系统扫描）
     * 返回实时的文件系统文档数量和已索引的文档数量
     */
    public EnhancedStatistics getEnhancedStatistics() {
        if (rag == null) {
            throw new IllegalStateException("知识库未初始化");
        }

        // 获取基础统计信息
        LocalFileRAG.Statistics basicStats = rag.getStatistics();

        // 扫描文件系统获取实际文件数量
        long fileSystemDocCount = scanFileSystemDocuments();

        // 构建增强的统计信息
        EnhancedStatistics stats = new EnhancedStatistics();
        stats.setDocumentCount(fileSystemDocCount);  // 使用文件系统的实际数量
        stats.setIndexedDocumentCount(basicStats.getIndexedDocumentCount());
        stats.setUnindexedCount(fileSystemDocCount - basicStats.getIndexedDocumentCount());
        stats.setIndexProgress(fileSystemDocCount > 0 ?
            (int) Math.round((double) basicStats.getIndexedDocumentCount() / fileSystemDocCount * 100) : 100);

        log.debug("📊 增强统计信息 - 文件系统文档: {}, 已索引: {}, 未索引: {}, 完成度: {}%",
            fileSystemDocCount, basicStats.getIndexedDocumentCount(),
            stats.getUnindexedCount(), stats.getIndexProgress());

        return stats;
    }

    /**
     * 扫描文件系统统计文档数量
     */
    private long scanFileSystemDocuments() {
        try {
            String sourcePath = properties.getKnowledgeBase().getSourcePath();
            Path documentsPath;

            // 处理 classpath 路径
            if (sourcePath.startsWith("classpath:")) {
                String resourcePath = sourcePath.substring("classpath:".length());
                try {
                    var resource = getClass().getClassLoader().getResource(resourcePath);
                    if (resource != null) {
                        Path tempPath = Paths.get(resource.toURI());
                        if (tempPath.toString().contains(".jar!")) {
                            documentsPath = Paths.get("./data/documents");
                        } else {
                            documentsPath = tempPath;
                        }
                    } else {
                        documentsPath = Paths.get("./data/documents");
                    }
                } catch (Exception e) {
                    documentsPath = Paths.get("./data/documents");
                }
            } else {
                documentsPath = Paths.get(sourcePath);
            }

            // 确保目录存在
            if (!Files.exists(documentsPath)) {
                log.warn("文档目录不存在: {}", documentsPath);
                return 0;
            }

            // 支持的文件扩展名
            List<String> supportedExtensions = Arrays.asList(
                "xlsx", "xls", "docx", "doc", "pptx", "ppt", "pdf", "txt", "md", "html", "xml"
            );

            // 扫描并统计文件
            try (Stream<Path> paths = Files.walk(documentsPath, 1)) {
                long count = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        int lastDot = filename.lastIndexOf('.');
                        if (lastDot == -1) return false;
                        String extension = filename.substring(lastDot + 1).toLowerCase();
                        return supportedExtensions.contains(extension);
                    })
                    .count();

                log.debug("📂 扫描文件系统完成，找到 {} 个支持的文档", count);
                return count;
            }

        } catch (Exception e) {
            log.error("扫描文件系统失败", e);
            // 出错时返回基础统计的数量
            return rag.getStatistics().getDocumentCount();
        }
    }

    /**
     * 增强的统计信息类
     */
    @lombok.Data
    public static class EnhancedStatistics {
        private long documentCount;          // 文件系统中的文档数量
        private long indexedDocumentCount;   // 已索引的文档数量
        private long unindexedCount;         // 未索引的文档数量
        private int indexProgress;           // 索引完成度百分比
    }

    /**
     * 重建知识库
     */
    public synchronized BuildResult rebuildKnowledgeBase() {
        log.info("🔄 开始重建知识库...");

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁
            if (rag != null) {
                log.info("📌 关闭现有知识库实例...");
                try {
                    rag.close();
                    log.info("✅ 现有知识库实例已关闭");
                } catch (Exception e) {
                    log.warn("⚠️  关闭现有知识库实例时出现警告: {}", e.getMessage());
                }
                rag = null;
            }

            // 2. 重建知识库
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            // 强制重建
            var result = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);

            if (result.getError() != null) {
                log.error("❌ 知识库重建失败: {}", result.getError());
                throw new RuntimeException("知识库重建失败: " + result.getError());
            }

            log.info("✅ 知识库重建完成！");
            log.info("   - 成功: {} 个文件", result.getSuccessCount());
            log.info("   - 失败: {} 个文件", result.getFailedCount());
            log.info("   - 总文档: {} 个", result.getTotalDocuments());

            // 3. 重新初始化知识库实例
            log.info("🔄 重新初始化知识库实例...");
            initializeKnowledgeBase();
            log.info("✅ 知识库实例重新初始化完成");

            return result;

        } catch (Exception e) {
            log.error("❌ 知识库重建过程出错", e);

            // 尝试恢复知识库实例
            try {
                if (rag == null) {
                    log.info("🔄 尝试恢复知识库实例...");
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error("❌ 恢复知识库实例失败", ex);
            }

            throw new RuntimeException("知识库重建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 增量索引知识库
     * 只处理新增和修改的文档，性能更优
     */
    public synchronized BuildResult incrementalIndexKnowledgeBase() {
        log.info("🔄 开始增量索引知识库...");

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁
            if (rag != null) {
                log.info("📌 关闭现有知识库实例...");
                try {
                    rag.close();
                    log.info("✅ 现有知识库实例已关闭");
                } catch (Exception e) {
                    log.warn("⚠️  关闭现有知识库实例时出现警告: {}", e.getMessage());
                }
                rag = null;
            }

            // 2. 执行增量索引
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            var result = knowledgeBaseService.incrementalIndex(sourcePath, storagePath);

            if (result.getError() != null) {
                log.error("❌ 增量索引失败: {}", result.getError());
                throw new RuntimeException("增量索引失败: " + result.getError());
            }

            log.info("✅ 增量索引完成！");
            log.info("   - 成功: {} 个文件", result.getSuccessCount());
            log.info("   - 失败: {} 个文件", result.getFailedCount());
            log.info("   - 总文档: {} 个", result.getTotalDocuments());

            // 3. 重新初始化知识库实例
            log.info("🔄 重新初始化知识库实例...");
            initializeKnowledgeBase();
            log.info("✅ 知识库实例重新初始化完成");

            return result;

        } catch (Exception e) {
            log.error("❌ 增量索引过程出错", e);

            // 尝试恢复知识库实例
            try {
                if (rag == null) {
                    log.info("🔄 尝试恢复知识库实例...");
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error("❌ 恢复知识库实例失败", ex);
            }

            throw new RuntimeException("增量索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索文档
     */
    public List<Document> searchDocuments(String query, int limit) {
        if (rag == null) {
            throw new IllegalStateException("知识库未初始化");
        }

        var result = rag.search(top.yumbo.ai.rag.model.Query.builder()
                .queryText(query)
                .limit(limit)
                .build());

        return result.getDocuments();
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        log.info("🔄 关闭知识库问答系统...");

        if (embeddingEngine != null) {
            embeddingEngine.close();
            log.info("   ✅ 向量嵌入引擎已关闭");
        }

        if (rag != null) {
            rag.close();
            log.info("   ✅ 知识库已关闭");
        }

        log.info("✅ 知识库问答系统已安全关闭");
    }

    /**
     * 保存问答记录
     */
    private String saveQARecord(String question, String answer,
                               List<String> retrievedDocs, List<String> usedDocs,
                               long responseTimeMs) {
        try {
            top.yumbo.ai.rag.feedback.QARecord record = top.yumbo.ai.rag.feedback.QARecord.builder()
                .question(question)
                .answer(answer)
                .retrievedDocuments(retrievedDocs)
                .usedDocuments(usedDocs)
                .responseTimeMs(responseTimeMs)
                .build();

            String recordId = qaRecordService.saveRecord(record);
            log.debug("📝 问答记录已保存: {}", recordId);
            return recordId;
        } catch (Exception e) {
            log.warn("⚠️ 保存问答记录失败", e);
            return null;
        }
    }
}
