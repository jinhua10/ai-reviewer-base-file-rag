package top.yumbo.ai.rag.spring.boot.service;

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.SmartContextBuilder;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

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
import java.util.stream.Collectors;
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
        log.info(LogMessageProvider.getMessage("log.kqa.sep"));
        log.info(LogMessageProvider.getMessage("log.kqa.init_start"));
        log.info(LogMessageProvider.getMessage("log.kqa.sep"));

        try {
            // 1. 初始化知识库
            initializeKnowledgeBase();

            // 2. 初始化向量检索
            initializeVectorSearch();

            // 3. 初始化LLM客户端
            initializeLLMClient();

            // 4. 创建问答系统
            createQASystem();

            log.info(LogMessageProvider.getMessage("log.kqa.sep"));
            log.info(LogMessageProvider.getMessage("log.kqa.init_done"));
            log.info(LogMessageProvider.getMessage("log.kqa.sep"));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kqa.init_failed"), e);
            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.init_failed"), e);
        }
    }

    /**
     * 初始化知识库
     */
    private void initializeKnowledgeBase() {
        log.info(LogMessageProvider.getMessage("log.kqa.step", 1, LogMessageProvider.getMessage("log.kqa.init_kb")));

        String storagePath = properties.getKnowledgeBase().getStoragePath();
        String sourcePath = properties.getKnowledgeBase().getSourcePath();
        boolean rebuildOnStartup = properties.getKnowledgeBase().isRebuildOnStartup();

        log.info(LogMessageProvider.getMessage("log.kqa.storage_path", storagePath));
        log.info(LogMessageProvider.getMessage("log.kqa.source_path", sourcePath));

        BuildResult buildResult;
        if (rebuildOnStartup) {
            log.info(LogMessageProvider.getMessage("log.kqa.rebuild_mode"));
            buildResult = buildKnowledgeBaseWithRebuild(sourcePath, storagePath);
        } else {
            log.info(LogMessageProvider.getMessage("log.kqa.incremental_mode"));
            buildResult = buildKnowledgeBaseIncremental(sourcePath, storagePath);
        }

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.build_complete"));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // 连接到知识库 / Connect to knowledge base
        rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .enableCache(properties.getKnowledgeBase().isEnableCache())
                .build();

        var stats = rag.getStatistics();
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.kb_ready"));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.document_count", stats.getDocumentCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.index_count", stats.getIndexedDocumentCount()));
    }

    private BuildResult buildKnowledgeBaseWithRebuild(String sourcePath, String storagePath) {
        BuildResult buildResult = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);
        if (buildResult.getError() != null) {
            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", buildResult.getError()));
        }

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // RAG 实例将在 initializeKnowledgeBase() 方法末尾统一创建 / RAG instance will be created at the end of initializeKnowledgeBase() method
        return buildResult;
    }

    private BuildResult buildKnowledgeBaseIncremental(String sourcePath, String storagePath) {
        BuildResult buildResult = knowledgeBaseService.buildKnowledgeBaseWithIncrementalIndex(sourcePath, storagePath);
        if (buildResult.getError() != null) {
            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", buildResult.getError()));
        }

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.build_complete"));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // RAG 实例将在 initializeKnowledgeBase() 方法末尾统一创建 / RAG instance will be created at the end of initializeKnowledgeBase() method
        return buildResult;
    }

    /**
     * 初始化向量检索 / Initialize vector search
     */
    private void initializeVectorSearch() {
        if (!properties.getVectorSearch().isEnabled()) {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_disabled"));
            return;
        }

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.init_vector_engine"));

        try {
            // 初始化嵌入引擎 / Initialize embedding engine
            embeddingEngine = new LocalEmbeddingEngine();

            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_engine_loaded"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_model", embeddingEngine.getModelName()));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_dimension", embeddingEngine.getEmbeddingDim()));

            // 加载向量索引 / Load vector index
            String indexPath = properties.getVectorSearch().getIndexPath();
            vectorIndexEngine = new SimpleVectorIndexEngine(
                    indexPath,
                    embeddingEngine.getEmbeddingDim()
            );

            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_index_loaded"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_index_path", indexPath));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.vector_count", vectorIndexEngine.size()));

        } catch (OrtException | IOException e) {
            log.error("❌ Vector search engine initialization failed", e);
            log.warn(LogMessageProvider.getMessage("knowledge_qa_service.model_download_hint"));
            log.warn(LogMessageProvider.getMessage("knowledge_qa_service.model_doc_hint"));
            embeddingEngine = null;
            vectorIndexEngine = null;
        }
    }

    /**
     * 初始化LLM客户端 / Initialize LLM client
     */
    private void initializeLLMClient() {
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.init_llm"));

        String provider = properties.getLlm().getProvider();
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.llm_provider", provider));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.llm_client_type", llmClient.getClass().getSimpleName()));

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.llm_client_ready"));
    }

    /**
     * 创建问答系统 / Create QA system
     */
    private void createQASystem() {
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.create_qa_system"));

        // 获取切分策略配置 / Get chunking strategy configuration
        String strategyName = properties.getLlm().getChunkingStrategy();
        top.yumbo.ai.rag.chunking.ChunkingStrategy strategy =
            top.yumbo.ai.rag.chunking.ChunkingStrategy.fromString(strategyName);

        // 初始化智能上下文构建器（使用新的构造函数，包含存储服务）/ Initialize smart context builder (using new constructor with storage service)
        contextBuilder = new SmartContextBuilder(
            properties.getLlm().getMaxContextLength(),
            properties.getLlm().getMaxDocLength(),
            true, // preserveFullContent（由策略控制，保留兼容性）/ preserveFullContent (controlled by strategy, maintain compatibility)
            properties.getLlm().getChunking(),
            strategy,
            llmClient,
            chunkStorageService  // 传递块存储服务 / Pass chunk storage service
        );

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.smart_context_initialized"));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.max_context_chars", properties.getLlm().getMaxContextLength()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.max_doc_length_chars", properties.getLlm().getMaxDocLength()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.chunking_strategy", strategy, strategy.getDescription()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.chunk_size_chars", properties.getLlm().getChunking().getChunkSize()));
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.chunk_overlap_chars", properties.getLlm().getChunking().getChunkOverlap()));

        if (strategy == top.yumbo.ai.rag.chunking.ChunkingStrategy.AI_SEMANTIC
            && properties.getLlm().getChunking().getAiChunking().isEnabled()) {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.ai_chunking_enabled",
                properties.getLlm().getChunking().getAiChunking().getModel()));
        }

        if (embeddingEngine != null && vectorIndexEngine != null) {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.using_vector_enhancement"));
        } else {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.log.using_keyword_mode"));
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
            throw new IllegalStateException(LogMessageProvider.getMessage("log.kqa.system_not_initialized"));
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.question_separator"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.question_prompt", question));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.separator"));

            // 步骤0: 搜索相似问题（在检索文档之前）/ Step 0: Search for similar questions (before retrieving documents)
            List<SimilarQAService.SimilarQA> similarQuestions = null;
            try {
                similarQuestions = similarQAService.findSimilar(question, 30, 3);  // minScore=30, limit=3
                if (!similarQuestions.isEmpty()) {
                    log.info(LogMessageProvider.getMessage("knowledge_qa_service.similar_found", similarQuestions.size()));
                }
            } catch (Exception e) {
                log.warn(LogMessageProvider.getMessage("knowledge_qa_service.similar_question_failed", e.getMessage()));
            }

            // 步骤1: 检索相关文档 / Step 1: Retrieve relevant documents
            List<top.yumbo.ai.rag.model.Document> documents;

            if (embeddingEngine != null && vectorIndexEngine != null) {
                // 使用混合检索 / Use hybrid search
                documents = hybridSearchService.hybridSearch(question, rag, embeddingEngine, vectorIndexEngine);
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.using_hybrid_search"));
            } else {
                // 使用纯关键词检索 / Use pure keyword search
                documents = hybridSearchService.keywordSearch(question, rag);
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.using_keyword_search"));
            }

            // 根据配置限制文档数量，使用会话管理支持分页引用 / Limit document count according to configuration, use session management to support paginated references
            int docsPerQuery = configService.getDocumentsPerQuery();
            int totalDocs = documents.size();
            boolean hasMoreDocs = false;
            List<top.yumbo.ai.rag.model.Document> remainingDocs = new ArrayList<>();
            String sessionId = null;

            // 创建会话以支持分页引用 / Create session to support paginated references
            if (totalDocs > 0) {
                sessionId = sessionService.createSession(question, documents, docsPerQuery);

                // 获取第一批文档 / Get first batch of documents
                SearchSessionService.SessionDocuments firstBatch =
                    sessionService.getCurrentDocuments(sessionId);
                documents = firstBatch.getDocuments();
                hasMoreDocs = firstBatch.isHasNext();

                log.info(LogMessageProvider.getMessage("knowledge_qa_service.create_session",
                    sessionId, totalDocs, documents.size(), firstBatch.getRemainingDocuments()));
            }

            if (totalDocs > docsPerQuery) {
                log.warn(LogMessageProvider.getMessage("knowledge_qa_service.too_many_docs_retrieved",
                        totalDocs, docsPerQuery));

                log.info(LogMessageProvider.getMessage("knowledge_qa_service.remaining_docs_unprocessed", remainingDocs.size()));
            } else {
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.retrieved_all", totalDocs));
            }

            // 步骤2: 构建智能上下文 / Step 2: Build smart context
            // 设置当前文档ID（用于保存切分块）/ Set current document ID (for saving chunks)
            if (!documents.isEmpty() && contextBuilder != null) {
                String firstDocTitle = documents.get(0).getTitle();
                contextBuilder.setCurrentDocumentId(firstDocTitle);
            }

            String context = contextBuilder.buildSmartContext(question, documents);
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.context_stats", contextBuilder.getContextStats(context)));

            // 步骤3: 收集可用的图片信息
            List<top.yumbo.ai.rag.image.ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        imageContext.append(LogMessageProvider.getMessage("knowledge_qa_service.available_images", doc.getTitle()));
                        for (int i = 0; i < Math.min(docImages.size(), 5); i++) { // 最多列出 5 张图片 / List up to 5 images
                            top.yumbo.ai.rag.image.ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : LogMessageProvider.getMessage("knowledge_qa_service.related_image");
                            imageContext.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_item",
                                i + 1, imgDesc, imgDesc, img.getUrl()));
                        }
                        if (docImages.size() > 5) {
                            imageContext.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.more_images", docImages.size() - 5));
                        }
                    }
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("knowledge_qa_service.image_not_found", doc.getTitle()), e);
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
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.images_in_context", allImages.size()));
            }

            log.info(LogMessageProvider.getMessage("knowledge_qa_service.using_docs", usedDocTitles.size()));
            if (hasMoreDocs) {
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.remaining_docs", remainingDocs.size()));
            }

            // 步骤5: 调用 LLM 生成答案 / Step 5: Call LLM to generate answer
            String answer = llmClient.generate(prompt);


            // 步骤6: 提取文档来源 / Step 6: Extract document sources
            List<String> sources = documents.stream()
                    .map(Document::getTitle)
                    .distinct()
                    .toList();

            // 步骤7: 获取切分块信息 / Step 7: Get chunk information
            List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks = Collections.emptyList();
            List<top.yumbo.ai.rag.image.ImageInfo> images = Collections.emptyList();

            if (!documents.isEmpty()) {
                String firstDocTitle = documents.get(0).getTitle();
                if (chunkStorageService != null && imageStorageService != null) {
                    try {
                        chunks = chunkStorageService.listChunks(firstDocTitle);
                        images = imageStorageService.listImages(firstDocTitle);
                        log.info(LogMessageProvider.getMessage("knowledge_qa_service.found_chunks_images", chunks.size(), images.size()));
                    } catch (Exception e) {
                        log.warn(LogMessageProvider.getMessage("knowledge_qa_service.load_chunk_failed"), e);
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果 / Display results
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.answer_label"));
            log.info(answer);
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.sources_label", sources.size()));
             sources.forEach(source -> log.info("   - {}", source));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.response_time", totalTime));
             log.info(LogMessageProvider.getMessage("knowledge_qa_service.separator"));

            // 保存问答记录（用于反馈和优化）/ Save QA record (for feedback and optimization)
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
            log.error("❌ QA processing failed", e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                    LogMessageProvider.getMessage("knowledge_qa_service.error_processing", e.getMessage()),
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
            throw new IllegalStateException(LogMessageProvider.getMessage("log.kqa.system_not_initialized"));
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.question_separator"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.question_label", question, sessionId));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.separator"));

            // 从会话获取当前批次的文档 / Get current batch of documents from session
            SearchSessionService.SessionDocuments sessionDocs =
                sessionService.getCurrentDocuments(sessionId);

            List<top.yumbo.ai.rag.model.Document> documents = sessionDocs.getDocuments();

            log.info(LogMessageProvider.getMessage("knowledge_qa_service.using_session_docs",
                sessionDocs.getTotalDocuments(),
                sessionDocs.getCurrentPage(),
                sessionDocs.getTotalPages(),
                documents.size()));

            // 获取会话信息 / Get session information
            SearchSessionService.SessionInfo sessionInfo =
                sessionService.getSessionInfo(sessionId);

            // 步骤2: 构建智能上下文 / Step 2: Build smart context
            if (!documents.isEmpty() && contextBuilder != null) {
                String firstDocTitle = documents.get(0).getTitle();
                contextBuilder.setCurrentDocumentId(firstDocTitle);
            }

            String context = contextBuilder.buildSmartContext(question, documents);
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.context_stats", contextBuilder.getContextStats(context)));

            // 步骤3: 收集可用的图片信息
            List<top.yumbo.ai.rag.image.ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        imageContext.append(LogMessageProvider.getMessage("knowledge_qa_service.available_images", doc.getTitle()));
                        for (int i = 0; i < Math.min(docImages.size(), 5); i++) {
                            top.yumbo.ai.rag.image.ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : LogMessageProvider.getMessage("knowledge_qa_service.related_image");
                            imageContext.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_item",
                                i + 1, imgDesc, imgDesc, img.getUrl()));
                        }
                        if (docImages.size() > 5) {
                            imageContext.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.more_images", docImages.size() - 5));
                        }
                    }
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("knowledge_qa_service.image_not_found", doc.getTitle()), e);
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
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.images_in_context", allImages.size()));
            }

            log.info(LogMessageProvider.getMessage("knowledge_qa_service.using_docs", usedDocTitles.size()));
            if (hasMoreDocs) {
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.remaining_docs", remainingDocsCount));
            }

            // 步骤5: 调用 LLM 生成答案 / Step 5: Call LLM to generate answer
            String answer = llmClient.generate(prompt);

            // 步骤6: 提取文档来源 / Step 6: Extract document sources
            List<String> sources = documents.stream()
                    .map(Document::getTitle)
                    .distinct()
                    .toList();

            // 步骤7: 获取切分块信息 / Step 7: Get chunk information
            List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks = Collections.emptyList();
            List<top.yumbo.ai.rag.image.ImageInfo> images = Collections.emptyList();

            if (!documents.isEmpty()) {
                String firstDocTitle = documents.get(0).getTitle();
                if (chunkStorageService != null && imageStorageService != null) {
                    try {
                        chunks = chunkStorageService.listChunks(firstDocTitle);
                        images = imageStorageService.listImages(firstDocTitle);
                        log.info(LogMessageProvider.getMessage("knowledge_qa_service.found_chunks_images", chunks.size(), images.size()));
                    } catch (Exception e) {
                        log.warn(LogMessageProvider.getMessage("knowledge_qa_service.load_chunk_failed"), e);
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果 / Display results
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.answer_label"));
            log.info(answer);
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.sources_label", sources.size()));
             sources.forEach(source -> log.info("   - {}", source));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.response_time", totalTime));
             log.info(LogMessageProvider.getMessage("knowledge_qa_service.separator"));

            // 保存问答记录 / Save QA record
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
            log.error(LogMessageProvider.getMessage("knowledge_qa_service.qa_with_session_failed"), e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                    LogMessageProvider.getMessage("knowledge_qa_service.error_processing", e.getMessage()),
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

        // 添加图片使用指南 / Add image usage guide
        if (hasImages && !imageContext.isEmpty()) {
            enhancement.append(LogMessageProvider.getMessage("knowledge_qa_service.important_notice"));
            enhancement.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_guide_1"));
            enhancement.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_guide_2"));
            enhancement.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_guide_3"));
            enhancement.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.image_guide_4"));
            enhancement.append("\n").append(imageContext);
        }

        // 添加文档使用说明 / Add document usage instructions
        if (!usedDocuments.isEmpty()) {
            enhancement.append(LogMessageProvider.getMessage("knowledge_qa_service.referenced_docs"));
            for (int i = 0; i < usedDocuments.size(); i++) {
                enhancement.append("\n").append(LogMessageProvider.getMessage("knowledge_qa_service.doc_item", i + 1, usedDocuments.get(i)));
            }
        }

        // 如果有更多未处理的文档，提示用户 / If there are more unprocessed documents, prompt the user
        if (hasMoreDocs && remainingCount > 0) {
            enhancement.append(LogMessageProvider.getMessage("knowledge_qa_service.more_docs_notice",
                usedDocuments.size(), remainingCount));
        }

        // 替换占位符 / Replace placeholders
        return template.replace("{question}", question)
                       .replace("{context}", context) +
               enhancement.toString();
    }

    /**
     * 获取知识库统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        if (rag == null) {
            throw new IllegalStateException(LogMessageProvider.getMessage("log.kqa.kb_not_initialized"));
        }
        return rag.getStatistics();
    }

    /**
     * 获取增强的统计信息（包含文件系统扫描）/ Get enhanced statistics (including file system scan)
     * 返回实时的文件系统文档数量和已索引的文档数量 / Return real-time file system document count and indexed document count
     */
    public EnhancedStatistics getEnhancedStatistics() {
        if (rag == null) {
            throw new IllegalStateException(LogMessageProvider.getMessage("log.kqa.kb_not_initialized"));
        }

        // 获取基础统计信息 / Get basic statistics
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

        log.debug(LogMessageProvider.getMessage("knowledge_qa_service.debug_enhanced_stats",
            fileSystemDocCount, basicStats.getIndexedDocumentCount(),
            stats.getUnindexedCount(), stats.getIndexProgress()));

        return stats;
    }

    /**
     * 扫描文件系统统计文档数量 / Scan file system to count documents
     */
    private long scanFileSystemDocuments() {
        try {
            String sourcePath = properties.getKnowledgeBase().getSourcePath();
            Path documentsPath;

            // 处理 classpath 路径 / Handle classpath path
            if (sourcePath.startsWith(LogMessageProvider.getMessage("knowledge_qa_service.classpath_prefix"))) {
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
                log.warn(LogMessageProvider.getMessage("log.kqa.docs_dir_missing", documentsPath.toString()));
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

                log.debug(LogMessageProvider.getMessage("log.kqa.scanned_files_count", count));
                return count;
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kqa.scan_failed"), e);
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
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.rebuild_start"));

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁 / Close existing RAG instance and release index lock
            if (rag != null) {
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.close_existing_kb"));
                try {
                    rag.close();
                    log.info(LogMessageProvider.getMessage("knowledge_qa_service.kb_closed"));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("knowledge_qa_service.close_kb_warning", e.getMessage()));
                }
                rag = null;
            }

            // 2. 重建知识库
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            // 强制重建 / Force rebuild
            var result = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);

            if (result.getError() != null) {
                log.error(LogMessageProvider.getMessage("log.kqa.rebuild_failed", result.getError()));
                throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", result.getError()));
            }

            log.info(LogMessageProvider.getMessage("log.kqa.rebuild_complete"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.success_files", result.getSuccessCount()));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.failed_files", result.getFailedCount()));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.total_documents", result.getTotalDocuments()));

            // 3. 重新初始化知识库实例 / Reinitialize knowledge base instance
            log.info(LogMessageProvider.getMessage("log.kqa.reinit_kb"));
            initializeKnowledgeBase();
            log.info(LogMessageProvider.getMessage("log.kqa.reinit_complete"));

            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kqa.rebuild_error"), e);

            // 尝试恢复知识库实例 / Try to recover knowledge base instance
            try {
                if (rag == null) {
                    log.info(LogMessageProvider.getMessage("log.kqa.recover_kb"));
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error(LogMessageProvider.getMessage("log.kqa.recover_failed"), ex);
            }

            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", e.getMessage()), e);
        }
    }

    /**
     * 增量索引知识库 / Incremental index knowledge base
     * 只处理新增和修改的文档，性能更优
     */
    public synchronized BuildResult incrementalIndexKnowledgeBase() {
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.incremental_index_start"));

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁
            if (rag != null) {
                log.info(LogMessageProvider.getMessage("knowledge_qa_service.closing_existing_kb"));
                try {
                    rag.close();
                    log.info(LogMessageProvider.getMessage("knowledge_qa_service.existing_kb_closed"));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("knowledge_qa_service.close_kb_warning", e.getMessage()));
                }
                rag = null;
            }

            // 2. 执行增量索引
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            var result = knowledgeBaseService.incrementalIndex(sourcePath, storagePath);

            if (result.getError() != null) {
                log.error(LogMessageProvider.getMessage("log.kqa.incremental_failed", result.getError()));
                throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", result.getError()));
            }

            log.info(LogMessageProvider.getMessage("log.kqa.incremental_complete"));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.success_files", result.getSuccessCount()));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.failed_files", result.getFailedCount()));
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.total_documents", result.getTotalDocuments()));

            // 3. 重新初始化知识库实例 / Reinitialize knowledge base instance
            log.info(LogMessageProvider.getMessage("log.kqa.reinit_kb"));
            initializeKnowledgeBase();
            log.info(LogMessageProvider.getMessage("log.kqa.reinit_complete"));

            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.kqa.incremental_error"), e);

            // 尝试恢复知识库实例 / Try to recover knowledge base instance
            try {
                if (rag == null) {
                    log.info(LogMessageProvider.getMessage("log.kqa.recover_kb"));
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error(LogMessageProvider.getMessage("log.kqa.recover_failed"), ex);
            }

            throw new RuntimeException(LogMessageProvider.getMessage("log.kqa.build_failed", e.getMessage()), e);
        }
    }

    /**
     * 搜索文档 / Search documents
     */
    public List<Document> searchDocuments(String query, int limit) {
        if (rag == null) {
            throw new IllegalStateException(LogMessageProvider.getMessage("log.kqa.kb_not_initialized"));
        }

        var result = rag.search(top.yumbo.ai.rag.model.Query.builder()
                .queryText(query)
                .limit(limit)
                .build());

        return result.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .collect(Collectors.toList());
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        log.info(LogMessageProvider.getMessage("knowledge_qa_service.destroy_start"));

        if (embeddingEngine != null) {
            embeddingEngine.close();
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.vector_engine_closed"));
        }

        if (rag != null) {
            rag.close();
            log.info(LogMessageProvider.getMessage("knowledge_qa_service.kb_closed_safe"));
        }

        log.info(LogMessageProvider.getMessage("knowledge_qa_service.system_closed"));
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
            log.debug(LogMessageProvider.getMessage("knowledge_qa_service.log.record_saved"), recordId);
            return recordId;
        } catch (Exception e) {
            log.warn(LogMessageProvider.getMessage("knowledge_qa_service.save_qa_failed"), e);
            return null;
        }
    }
}
