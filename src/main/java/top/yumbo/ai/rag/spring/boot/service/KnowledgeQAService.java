package top.yumbo.ai.rag.spring.boot.service;

import ai.onnxruntime.OrtException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.feedback.QARecordService;
import top.yumbo.ai.rag.image.ImageInfo;
import top.yumbo.ai.rag.image.ImageStorageService;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.ppl.PPLServiceFacade;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.optimization.SmartContextBuilder;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchContext;
import top.yumbo.ai.rag.spring.boot.strategy.search.SearchStrategyDispatcher;

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
    private final ChunkStorageService chunkStorageService;
    private final ImageStorageService imageStorageService;
    private final QARecordService qaRecordService;
    private final SimilarQAService similarQAService;
    private final PPLServiceFacade pplServiceFacade;  // PPL 服务门面
    private final PPLConfig pplConfig;  // PPL 配置
    private final SearchStrategyDispatcher searchStrategyDispatcher;  // 检索策略调度器

    private LocalFileRAG rag;
    private LocalEmbeddingEngine embeddingEngine;
    private SimpleVectorIndexEngine vectorIndexEngine;
    private top.yumbo.ai.rag.optimization.SmartContextBuilder contextBuilder;

    /**
     * -- GETTER --
     *  检查是否正在索引
     */
    // 索引状态标记
    @Getter
    private volatile boolean isIndexing = false;

    public KnowledgeQAService(KnowledgeQAProperties properties,
                              KnowledgeBaseService knowledgeBaseService,
                              HybridSearchService hybridSearchService,
                              SearchSessionService sessionService,
                              SearchConfigService configService,
                              LLMClient llmClient,
                              ChunkStorageService chunkStorageService,
                              ImageStorageService imageStorageService,
                              QARecordService qaRecordService,
                              SimilarQAService similarQAService,
                              PPLServiceFacade pplServiceFacade,
                              PPLConfig pplConfig,
                              @Autowired(required = false)
                              SearchStrategyDispatcher searchStrategyDispatcher) {
        this.properties = properties;
        this.knowledgeBaseService = knowledgeBaseService;
        this.hybridSearchService = hybridSearchService;
        this.sessionService = sessionService;
        this.configService = configService;
        this.llmClient = llmClient;
        this.chunkStorageService = chunkStorageService;
        this.imageStorageService = imageStorageService;
        this.qaRecordService = qaRecordService;
        this.similarQAService = similarQAService;
        this.pplServiceFacade = pplServiceFacade;  // 初始化 PPL 服务
        this.pplConfig = pplConfig;  // 初始化 PPL 配置
        this.searchStrategyDispatcher = searchStrategyDispatcher;  // 初始化检索策略调度器
    }

    /**
     * 初始化问答系统
     */
    @PostConstruct
    public void initialize() {
        log.info(I18N.get("log.kqa.sep"));
        log.info(I18N.get("log.kqa.init_start"));
        log.info(I18N.get("log.kqa.sep"));

        try {
            // 1. 初始化知识库
            initializeKnowledgeBase();

            // 2. 初始化向量检索
            initializeVectorSearch();

            // 3. 初始化LLM客户端
            initializeLLMClient();

            // 4. 创建问答系统
            createQASystem();

            log.info(I18N.get("log.kqa.sep"));
            log.info(I18N.get("log.kqa.init_done"));
            log.info(I18N.get("log.kqa.sep"));

        } catch (Exception e) {
            log.error(I18N.get("log.kqa.init_failed"), e);
            throw new RuntimeException(I18N.get("log.kqa.init_failed"), e);
        }
    }

    /**
     * 初始化知识库
     */
    private void initializeKnowledgeBase() {
        log.info(I18N.get("log.kqa.step", 1, I18N.get("log.kqa.init_kb")));

        String storagePath = properties.getKnowledgeBase().getStoragePath();
        String sourcePath = properties.getKnowledgeBase().getSourcePath();
        boolean rebuildOnStartup = properties.getKnowledgeBase().isRebuildOnStartup();

        log.info(I18N.get("log.kqa.storage_path", storagePath));
        log.info(I18N.get("log.kqa.source_path", sourcePath));

        BuildResult buildResult;
        if (rebuildOnStartup) {
            log.info(I18N.get("log.kqa.rebuild_mode"));
            buildResult = buildKnowledgeBaseWithRebuild(sourcePath, storagePath);
        } else {
            log.info(I18N.get("log.kqa.incremental_mode"));
            buildResult = buildKnowledgeBaseIncremental(sourcePath, storagePath);
        }

        log.info(I18N.get("knowledge_qa_service.log.build_complete"));
        log.info(I18N.get("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(I18N.get("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(I18N.get("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(I18N.get("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // 连接到知识库 / Connect to knowledge base
        rag = LocalFileRAG.builder()
                .storagePath(storagePath)
                .enableCache(properties.getKnowledgeBase().isEnableCache())
                .build();

        var stats = rag.getStatistics();
        log.info(I18N.get("knowledge_qa_service.log.kb_ready"));
        log.info(I18N.get("knowledge_qa_service.log.document_count", stats.getDocumentCount()));
        log.info(I18N.get("knowledge_qa_service.log.index_count", stats.getIndexedDocumentCount()));
    }

    private BuildResult buildKnowledgeBaseWithRebuild(String sourcePath, String storagePath) {
        BuildResult buildResult = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);
        if (buildResult.getError() != null) {
            throw new RuntimeException(I18N.get("log.kqa.build_failed", buildResult.getError()));
        }

        log.info(I18N.get("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(I18N.get("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(I18N.get("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(I18N.get("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // RAG 实例将在 initializeKnowledgeBase() 方法末尾统一创建 / RAG instance will be created at the end of initializeKnowledgeBase() method
        return buildResult;
    }

    private BuildResult buildKnowledgeBaseIncremental(String sourcePath, String storagePath) {
        BuildResult buildResult = knowledgeBaseService.buildKnowledgeBaseWithIncrementalIndex(sourcePath, storagePath);
        if (buildResult.getError() != null) {
            throw new RuntimeException(I18N.get("log.kqa.build_failed", buildResult.getError()));
        }

        log.info(I18N.get("knowledge_qa_service.log.build_complete"));
        log.info(I18N.get("knowledge_qa_service.log.total_files", buildResult.getTotalFiles()));
        log.info(I18N.get("knowledge_qa_service.log.processed_files", buildResult.getSuccessCount()));
        log.info(I18N.get("knowledge_qa_service.log.failed_files", buildResult.getFailedCount()));
        log.info(I18N.get("knowledge_qa_service.log.total_documents", buildResult.getTotalDocuments()));

        // RAG 实例将在 initializeKnowledgeBase() 方法末尾统一创建 / RAG instance will be created at the end of initializeKnowledgeBase() method
        return buildResult;
    }

    /**
     * 初始化向量检索 / Initialize vector search
     */
    private void initializeVectorSearch() {
        if (!properties.getVectorSearch().isEnabled()) {
            log.info(I18N.get("knowledge_qa_service.log.vector_disabled"));
            return;
        }

        log.info(I18N.get("knowledge_qa_service.log.init_vector_engine", ""));

        try {
            // 初始化嵌入引擎 / Initialize embedding engine
            embeddingEngine = new LocalEmbeddingEngine(properties.getVectorSearch().getModel().getPath());

            log.info(I18N.get("knowledge_qa_service.log.vector_engine_loaded", embeddingEngine.getModelName()));
            log.info(I18N.get("knowledge_qa_service.log.vector_model", embeddingEngine.getModelName()));
            log.info(I18N.get("knowledge_qa_service.log.vector_dimension", embeddingEngine.getEmbeddingDim()));

            // 加载向量索引 / Load vector index
            String indexPath = properties.getVectorSearch().getIndexPath();
            vectorIndexEngine = new SimpleVectorIndexEngine(
                    indexPath,
                    embeddingEngine.getEmbeddingDim()
            );

            log.info(I18N.get("knowledge_qa_service.log.vector_index_loaded", vectorIndexEngine.size()));
            log.info(I18N.get("knowledge_qa_service.log.vector_index_path", indexPath));
            log.info(I18N.get("knowledge_qa_service.log.vector_count", vectorIndexEngine.size()));

        } catch (OrtException | IOException e) {
            log.error(I18N.get("log.kb.vector_init_failed"), e);
            log.warn(I18N.get("knowledge_qa_service.log.vector_init_failed_hint"));
            log.warn(I18N.get("knowledge_qa_service.log.vector_init_model_hint"));
            log.warn(I18N.get("knowledge_qa_service.log.vector_init_solution"));
            log.warn(I18N.get("knowledge_qa_service.log.vector_init_solution_1"));
            log.warn(I18N.get("knowledge_qa_service.log.vector_init_solution_2"));
            log.warn(I18N.get("knowledge_qa_service.model_download_hint"));
            log.warn(I18N.get("knowledge_qa_service.model_doc_hint"));
            embeddingEngine = null;
            vectorIndexEngine = null;
            // 不抛出异常，允许系统继续运行（只使用文本搜索）
            // (Don't throw exception, allow system to continue running with text search only)
        }
    }

    /**
     * 初始化LLM客户端 / Initialize LLM client
     */
    private void initializeLLMClient() {
        log.info(I18N.get("knowledge_qa_service.log.init_llm"));

        String provider = properties.getLlm().getProvider();
        log.info(I18N.get("knowledge_qa_service.log.llm_provider", provider));
        log.info(I18N.get("knowledge_qa_service.log.llm_client_type", llmClient.getClass().getSimpleName()));

        log.info(I18N.get("knowledge_qa_service.log.llm_client_ready"));
    }

    /**
     * 创建问答系统 / Create QA system
     */
    private void createQASystem() {
        log.info(I18N.get("knowledge_qa_service.log.create_qa_system"));

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

        log.info(I18N.get("knowledge_qa_service.log.smart_context_initialized",
            properties.getLlm().getMaxContextLength(), properties.getLlm().getMaxDocLength()));
        log.info(I18N.get("knowledge_qa_service.log.chunking_strategy", strategy, strategy.getDescription()));
        log.info(I18N.get("knowledge_qa_service.log.chunk_size_chars", properties.getLlm().getChunking().getChunkSize()));
        log.info(I18N.get("knowledge_qa_service.log.chunk_overlap_chars", properties.getLlm().getChunking().getChunkOverlap()));

        if (strategy == top.yumbo.ai.rag.chunking.ChunkingStrategy.AI_SEMANTIC
            && properties.getLlm().getChunking().getAiChunking().isEnabled()) {
            log.info(I18N.get("knowledge_qa_service.log.ai_chunking_enabled",
                properties.getLlm().getChunking().getAiChunking().getModel()));
        }

        if (embeddingEngine != null && vectorIndexEngine != null) {
            log.info(I18N.get("knowledge_qa_service.log.using_vector_enhancement"));
        } else {
            log.info(I18N.get("knowledge_qa_service.log.using_keyword_mode"));
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
            throw new IllegalStateException(I18N.get("log.kqa.system_not_initialized"));
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(I18N.get("knowledge_qa_service.question_separator"));
            log.info(I18N.get("knowledge_qa_service.question_prompt", question));
            log.info(I18N.get("knowledge_qa_service.separator"));

            // 步骤0: 搜索相似问题（在检索文档之前）
            // (Step 0: Search for similar questions before retrieving documents)
            List<SimilarQAService.SimilarQA> similarQuestions = null;
            try {
                // 从配置获取相似问题参数 (Get similar QA params from config)
                int minScore = properties.getSimilarQa().getMinScore();
                int limit = properties.getSimilarQa().getLimit();
                similarQuestions = similarQAService.findSimilar(question, minScore, limit);
                if (!similarQuestions.isEmpty()) {
                    log.info(I18N.get("knowledge_qa_service.similar_found", similarQuestions.size()));
                }
            } catch (Exception e) {
                log.warn(I18N.get("knowledge_qa_service.similar_question_failed", e.getMessage()));
            }

            // 步骤1: 检索相关文档 / Step 1: Retrieve relevant documents
            List<top.yumbo.ai.rag.model.Document> documents;

            // 优先使用策略调度器（如果可用）/ Prefer strategy dispatcher if available
            if (searchStrategyDispatcher != null && !searchStrategyDispatcher.getAllStrategies().isEmpty()) {
                documents = searchWithStrategyDispatcher(question);
                log.info(I18N.get("knowledge_qa_service.using_strategy_dispatcher"));
            } else if (embeddingEngine != null && vectorIndexEngine != null) {
                // 使用混合检索 / Use hybrid search
                documents = hybridSearchService.hybridSearch(question, rag, embeddingEngine, vectorIndexEngine);
                log.info(I18N.get("knowledge_qa_service.using_hybrid_search"));
            } else {
                // 使用纯关键词检索 / Use pure keyword search
                documents = hybridSearchService.keywordSearch(question, rag);
                log.info(I18N.get("knowledge_qa_service.using_keyword_search"));
            }

            // 步骤1.5: PPL Rerank（如果启用）(Step 1.5: PPL Rerank if enabled)
            if (pplServiceFacade != null && pplConfig != null && pplConfig.getReranking() != null &&
                pplConfig.getReranking().isEnabled() && !documents.isEmpty()) {
                try {
                    log.info(I18N.get("log.ppl.rerank_start", documents.size()));
                    long rerankStart = System.currentTimeMillis();

                    // PPLServiceFacade.rerank 需要 2 个参数: question, candidates
                    // (config 会自动从 pplConfig 中获取)
                    documents = pplServiceFacade.rerank(question, documents);

                    long rerankTime = System.currentTimeMillis() - rerankStart;
                    log.info(I18N.get("log.ppl.rerank_completed", rerankTime));
                } catch (Exception e) {
                    log.warn(I18N.get("log.ppl.rerank_failed", e.getMessage()));
                }
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

                log.info(I18N.get("knowledge_qa_service.create_session",
                    sessionId, totalDocs, documents.size(), firstBatch.getRemainingDocuments()));
            }

            if (totalDocs > docsPerQuery) {
                log.warn(I18N.get("knowledge_qa_service.too_many_docs_retrieved",
                        totalDocs, docsPerQuery));

                log.info(I18N.get("knowledge_qa_service.remaining_docs_unprocessed", remainingDocs.size()));
            } else {
                log.info(I18N.get("knowledge_qa_service.retrieved_all", totalDocs));
            }

            // 步骤2: 构建智能上下文 / Step 2: Build smart context
            // 设置当前文档ID（用于保存切分块）/ Set current document ID (for saving chunks)
            if (!documents.isEmpty() && contextBuilder != null) {
                String firstDocTitle = documents.get(0).getTitle();
                contextBuilder.setCurrentDocumentId(firstDocTitle);
            }

            String context = contextBuilder.buildSmartContext(question, documents);
            log.info(I18N.get("knowledge_qa_service.context_stats", contextBuilder.getContextStats(context)));

            // 步骤3: 收集可用的图片信息
            List<top.yumbo.ai.rag.image.ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<top.yumbo.ai.rag.image.ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        // 从配置获取每文档最大图片数 (Get max images per doc from config)
                        int maxImagesPerDoc = properties.getImageProcessing().getMaxImagesPerDoc();

                        imageContext.append(I18N.get("knowledge_qa_service.available_images", doc.getTitle()));
                        for (int i = 0; i < Math.min(docImages.size(), maxImagesPerDoc); i++) {
                            top.yumbo.ai.rag.image.ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : I18N.get("knowledge_qa_service.related_image");
                            // 格式：图片 1：描述（来源：URL）![描述](URL)
                            // (Format: Image 1: description (source: URL) ![desc](URL))
                            imageContext.append("\n  ")
                                       .append(I18N.get("knowledge_qa_service.image_item",
                                           i + 1, imgDesc, img.getUrl()))
                                       .append(" ")
                                       .append("![").append(imgDesc).append("](").append(img.getUrl()).append(")");
                        }
                        if (docImages.size() > maxImagesPerDoc) {
                            imageContext.append("\n  ").append(I18N.get("knowledge_qa_service.more_images", docImages.size() - maxImagesPerDoc));
                        }
                        imageContext.append("\n");
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("knowledge_qa_service.image_not_found", doc.getTitle()), e);
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
                log.info(I18N.get("knowledge_qa_service.images_in_context", allImages.size()));
            }

            log.info(I18N.get("knowledge_qa_service.using_docs", usedDocTitles.size()));
            if (hasMoreDocs) {
                log.info(I18N.get("knowledge_qa_service.remaining_docs", remainingDocs.size()));
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
                        log.info(I18N.get("knowledge_qa_service.found_chunks_images", chunks.size(), images.size()));
                    } catch (Exception e) {
                        log.warn(I18N.get("knowledge_qa_service.load_chunk_failed"), e);
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果 / Display results
            log.info(I18N.get("knowledge_qa_service.answer_label"));
            log.info(answer);
            log.info(I18N.get("knowledge_qa_service.sources_label", sources.size()));
             sources.forEach(source -> log.info("   - {}", source));
            log.info(I18N.get("knowledge_qa_service.response_time", totalTime));
             log.info(I18N.get("knowledge_qa_service.separator"));

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
                    I18N.get("knowledge_qa_service.error_processing", e.getMessage()),
                    List.of(),
                    totalTime
            );
        }
    }

    /**
     * 带上下文的问答（供策略层调用）
     * (QA with context - for strategy layer)
     *
     * @param prompt 提示词/问题
     * @param context 上下文内容（可以为空）
     * @return 答案字符串
     */
    public String askWithContext(String prompt, String context) {
        if (llmClient == null) {
            throw new IllegalStateException(I18N.get("log.kqa.system_not_initialized"));
        }

        try {
            String fullPrompt;
            if (context != null && !context.trim().isEmpty()) {
                fullPrompt = prompt + "\n\n" + I18N.get("knowledge_qa_service.log.context_info") + "\n" + context;
            } else {
                fullPrompt = prompt;
            }

            log.debug(I18N.get("knowledge_qa_service.log.llm_call", fullPrompt.length()));
            return llmClient.generate(fullPrompt);

        } catch (Exception e) {
            log.error(I18N.get("knowledge_qa_service.log.llm_call_failed"), e);
            throw new RuntimeException(I18N.get("knowledge_qa_service.log.llm_call_error", e.getMessage()), e);
        }
    }

    /**
     * 直接问答（不使用知识库检索）
     * (Direct QA - without knowledge base retrieval)
     *
     * 用于单文档分析场景，直接将文档内容作为上下文发送给 LLM
     * (Used for single document analysis, directly sends document content as context to LLM)
     *
     * @param prompt 完整的提示词（包含文档内容）(Complete prompt including document content)
     * @return AI 回答 (AI Answer)
     */
    public AIAnswer askDirectly(String prompt) {
        if (llmClient == null) {
            throw new IllegalStateException(I18N.get("log.kqa.system_not_initialized"));
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(I18N.get("knowledge_qa_service.log.direct_qa_mode"));
            log.debug(I18N.get("knowledge_qa_service.log.prompt_length", prompt.length()));

            // 直接调用 LLM (Directly call LLM)
            String answer = llmClient.generate(prompt);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info(I18N.get("knowledge_qa_service.log.direct_qa_complete", totalTime));

            return new AIAnswer(
                answer,
                List.of(), // 无引用来源 (No reference sources)
                totalTime
            );

        } catch (Exception e) {
            log.error(I18N.get("knowledge_qa_service.log.direct_qa_failed"), e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                I18N.get("knowledge_qa_service.log.direct_qa_error", e.getMessage()),
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
            throw new IllegalStateException(I18N.get("log.kqa.system_not_initialized"));
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info(I18N.get("knowledge_qa_service.question_separator"));
            log.info(I18N.get("knowledge_qa_service.question_label", question, sessionId));
            log.info(I18N.get("knowledge_qa_service.separator"));

            // 从会话获取当前批次的文档 / Get current batch of documents from session
            SearchSessionService.SessionDocuments sessionDocs =
                sessionService.getCurrentDocuments(sessionId);

            List<Document> documents = sessionDocs.getDocuments();

            log.info(I18N.get("knowledge_qa_service.using_session_docs",
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
            log.info(I18N.get("knowledge_qa_service.context_stats", contextBuilder.getContextStats(context)));

            // 步骤3: 收集可用的图片信息
            List<ImageInfo> allImages = new ArrayList<>();
            StringBuilder imageContext = new StringBuilder();

            for (top.yumbo.ai.rag.model.Document doc : documents) {
                try {
                    List<ImageInfo> docImages =
                        imageStorageService.listImages(doc.getTitle());

                    if (!docImages.isEmpty()) {
                        allImages.addAll(docImages);

                        imageContext.append(I18N.get("knowledge_qa_service.available_images", doc.getTitle()));
                        for (int i = 0; i < Math.min(docImages.size(), 5); i++) {
                            ImageInfo img = docImages.get(i);
                            String imgDesc = img.getDescription() != null && !img.getDescription().isEmpty()
                                ? img.getDescription()
                                : I18N.get("knowledge_qa_service.related_image");
                            // 格式：图片 1：描述（来源：URL）![描述](URL)
                            imageContext.append("\n  ")
                                       .append(I18N.get("knowledge_qa_service.image_item",
                                           i + 1, imgDesc, img.getUrl()))
                                       .append(" ")
                                       .append("![").append(imgDesc).append("](").append(img.getUrl()).append(")");
                        }
                        if (docImages.size() > 5) {
                            imageContext.append("\n  ").append(I18N.get("knowledge_qa_service.more_images", docImages.size() - 5));
                        }
                        imageContext.append("\n");
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("knowledge_qa_service.image_not_found", doc.getTitle()), e);
                }
            }

            // 步骤4: 构建增强的 Prompt
            List<String> usedDocTitles = documents.stream()
                .map(Document::getTitle)
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
                log.info(I18N.get("knowledge_qa_service.images_in_context", allImages.size()));
            }

            log.info(I18N.get("knowledge_qa_service.using_docs", usedDocTitles.size()));
            if (hasMoreDocs) {
                log.info(I18N.get("knowledge_qa_service.remaining_docs", remainingDocsCount));
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
                        log.info(I18N.get("knowledge_qa_service.found_chunks_images", chunks.size(), images.size()));
                    } catch (Exception e) {
                        log.warn(I18N.get("knowledge_qa_service.load_chunk_failed"), e);
                    }
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果 / Display results
            log.info(I18N.get("knowledge_qa_service.answer_label"));
            log.info(answer);
            log.info(I18N.get("knowledge_qa_service.sources_label", sources.size()));
             sources.forEach(source -> log.info("   - {}", source));
            log.info(I18N.get("knowledge_qa_service.response_time", totalTime));
             log.info(I18N.get("knowledge_qa_service.separator"));

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
            log.error(I18N.get("knowledge_qa_service.qa_with_session_failed"), e);
            long totalTime = System.currentTimeMillis() - startTime;
            return new AIAnswer(
                    I18N.get("knowledge_qa_service.error_processing", e.getMessage()),
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
        // 从配置中获取提示词模板（From configuration get prompt template）
        String template = properties.getLlm().getPromptTemplate();

        // 构建增强内容（Build enhanced content）
        StringBuilder enhancement = new StringBuilder();

        // 【重要】图片使用指南必须放在最前面，在基础提示词之前（Image guide must be at the very beginning, before base prompt）
        StringBuilder imageGuide = new StringBuilder();
        if (hasImages && !imageContext.isEmpty()) {
            imageGuide.append(I18N.get("knowledge_qa_service.important_notice"));
            imageGuide.append("\n").append(I18N.get("knowledge_qa_service.image_guide_1"));
            imageGuide.append("\n").append(I18N.get("knowledge_qa_service.image_guide_2"));
            imageGuide.append("\n").append(I18N.get("knowledge_qa_service.image_guide_3"));
            imageGuide.append("\n").append(I18N.get("knowledge_qa_service.image_guide_4"));
            imageGuide.append("\n\n");
        }

        // 添加文档使用说明（Add document usage instructions）
        if (!usedDocuments.isEmpty()) {
            enhancement.append("\n\n").append(I18N.get("knowledge_qa_service.referenced_docs"));
            for (int i = 0; i < usedDocuments.size(); i++) {
                enhancement.append("\n").append(I18N.get("knowledge_qa_service.doc_item", i + 1, usedDocuments.get(i)));
            }
        }

        // 如果有更多未处理的文档，提示用户（If there are more unprocessed documents, prompt the user）
        if (hasMoreDocs && remainingCount > 0) {
            enhancement.append("\n\n").append(I18N.get("knowledge_qa_service.more_docs_notice",
                usedDocuments.size(), remainingCount));
        }

        // 添加图片信息（在问题和上下文之后）（Add image information after question and context）
        if (hasImages && !imageContext.isEmpty()) {
            enhancement.append("\n\n").append(imageContext);
        }

        // 【关键】构建最终 Prompt：图片指南 → 基础模板 → 文档说明 → 图片列表
        // (Critical) Build final prompt: Image guide → Base template → Doc instructions → Image list
        return imageGuide.toString() +
               template.replace("{question}", question)
                      .replace("{context}", context) +
               enhancement.toString();
    }

    /**
     * 获取知识库统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        if (rag == null) {
            throw new IllegalStateException(I18N.get("log.kqa.kb_not_initialized"));
        }
        return rag.getStatistics();
    }

    /**
     * 获取增强的统计信息（包含文件系统扫描）/ Get enhanced statistics (including file system scan)
     * 返回实时的文件系统文档数量和已索引的文档数量 / Return real-time file system document count and indexed document count
     */
    public EnhancedStatistics getEnhancedStatistics() {
        if (rag == null) {
            throw new IllegalStateException(I18N.get("log.kqa.kb_not_initialized"));
        }

        // 获取基础统计信息 / Get basic statistics
        LocalFileRAG.Statistics basicStats = rag.getStatistics();

        // 扫描文件系统获取实际文件数量
        long fileSystemDocCount = scanFileSystemDocuments();

        // 获取存储引擎中的唯一文档数（不含分块）
        long uniqueDocsCount = basicStats.getDocumentCount();

        // 获取索引引擎中的总块数（含分块）
        long totalIndexedChunks = basicStats.getIndexedDocumentCount();

        // 构建增强的统计信息
        EnhancedStatistics stats = new EnhancedStatistics();
        stats.setDocumentCount(fileSystemDocCount);  // 文件系统中的原始文件数
        stats.setUniqueDocumentsIndexed(uniqueDocsCount); // 已索引的唯一文档数（存储引擎）
        stats.setIndexedChunksCount(totalIndexedChunks);  // 索引的总块数（索引引擎）

        // 计算未索引的文档数：文件系统文件数 - 存储引擎中的唯一文档数
        stats.setUnindexedCount(Math.max(0, fileSystemDocCount - uniqueDocsCount));

        // 计算索引完成度：基于唯一文档数而非块数
        stats.setIndexProgress(fileSystemDocCount > 0 ?
            (int) Math.round((double) Math.min(uniqueDocsCount, fileSystemDocCount) / fileSystemDocCount * 100) : 100);

        // 为了兼容性，设置 indexedDocumentCount 为唯一文档数
        stats.setIndexedDocumentCount(uniqueDocsCount);

        log.debug(I18N.get("knowledge_qa_service.debug_enhanced_stats_v2",
            fileSystemDocCount, uniqueDocsCount, totalIndexedChunks,
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
            if (sourcePath.startsWith(I18N.get("knowledge_qa_service.classpath_prefix"))) {
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
                log.warn(I18N.get("log.kqa.docs_dir_missing", documentsPath.toString()));
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

                log.debug(I18N.get("log.kqa.scanned_files_count", count));
                return count;
            }

        } catch (Exception e) {
            log.error(I18N.get("log.kqa.scan_failed"), e);
            // 出错时返回基础统计的数量
            return rag.getStatistics().getDocumentCount();
        }
    }

    /**
     * 增强的统计信息类
     */
    @lombok.Data
    public static class EnhancedStatistics {
        private long documentCount;          // 文件系统中的文档数量（原始文件数）
        private long indexedDocumentCount;   // 已索引的文档块数量（包含分块）
        private long unindexedCount;         // 未索引的文档数量
        private int indexProgress;           // 索引完成度百分比
        private long indexedChunksCount;     // 索引的文档块总数（用于显示）
        private long uniqueDocumentsIndexed; // 已索引的唯一文档数（不含分块）
    }

    /**
     * 重建知识库
     */
    public synchronized BuildResult rebuildKnowledgeBase() {
        log.info(I18N.get("knowledge_qa_service.rebuild_start"));

        // 设置索引状态为进行中
        isIndexing = true;

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁 / Close existing RAG instance and release index lock
            if (rag != null) {
                log.info(I18N.get("knowledge_qa_service.close_existing_kb"));
                try {
                    rag.close();
                    log.info(I18N.get("knowledge_qa_service.kb_closed"));
                } catch (Exception e) {
                    log.warn(I18N.get("knowledge_qa_service.close_kb_warning", e.getMessage()));
                }
                rag = null;
            }

            // 2. 重建知识库
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            // 强制重建 / Force rebuild
            var result = knowledgeBaseService.buildKnowledgeBase(sourcePath, storagePath, true);

            if (result.getError() != null) {
                log.error(I18N.get("log.kqa.rebuild_failed", result.getError()));
                throw new RuntimeException(I18N.get("log.kqa.build_failed", result.getError()));
            }

            log.info(I18N.get("log.kqa.rebuild_complete"));
            log.info(I18N.get("knowledge_qa_service.success_files", result.getSuccessCount()));
            log.info(I18N.get("knowledge_qa_service.failed_files", result.getFailedCount()));
            log.info(I18N.get("knowledge_qa_service.total_documents", result.getTotalDocuments()));

            // 3. 重新初始化知识库实例 / Reinitialize knowledge base instance
            log.info(I18N.get("log.kqa.reinit_kb"));
            initializeKnowledgeBase();
            log.info(I18N.get("log.kqa.reinit_complete"));

            return result;

        } catch (Exception e) {
            log.error(I18N.get("log.kqa.rebuild_error"), e);

            // 尝试恢复知识库实例 / Try to recover knowledge base instance
            try {
                if (rag == null) {
                    log.info(I18N.get("log.kqa.recover_kb"));
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error(I18N.get("log.kqa.recover_failed"), ex);
            }

            throw new RuntimeException(I18N.get("log.kqa.build_failed", e.getMessage()), e);
        } finally {
            // 无论成功或失败，都重置索引状态
            isIndexing = false;
        }
    }

    /**
     * 增量索引知识库 / Incremental index knowledge base
     * 只处理新增和修改的文档，性能更优
     */
    public synchronized BuildResult incrementalIndexKnowledgeBase() {
        log.info(I18N.get("knowledge_qa_service.incremental_index_start"));

        // 设置索引状态为进行中
        isIndexing = true;

        try {
            // 1. 关闭现有的 RAG 实例，释放索引锁
            if (rag != null) {
                log.info(I18N.get("knowledge_qa_service.closing_existing_kb"));
                try {
                    rag.close();
                    log.info(I18N.get("knowledge_qa_service.existing_kb_closed"));
                } catch (Exception e) {
                    log.warn(I18N.get("knowledge_qa_service.close_kb_warning", e.getMessage()));
                }
                rag = null;
            }

            // 2. 执行增量索引
            String storagePath = properties.getKnowledgeBase().getStoragePath();
            String sourcePath = properties.getKnowledgeBase().getSourcePath();

            var result = knowledgeBaseService.incrementalIndex(sourcePath, storagePath);

            if (result.getError() != null) {
                log.error(I18N.get("log.kqa.incremental_failed", result.getError()));
                throw new RuntimeException(I18N.get("log.kqa.build_failed", result.getError()));
            }

            log.info(I18N.get("log.kqa.incremental_complete"));
            log.info(I18N.get("knowledge_qa_service.success_files", result.getSuccessCount()));
            log.info(I18N.get("knowledge_qa_service.failed_files", result.getFailedCount()));
            log.info(I18N.get("knowledge_qa_service.total_documents", result.getTotalDocuments()));

            // 3. 重新初始化知识库实例 / Reinitialize knowledge base instance
            log.info(I18N.get("log.kqa.reinit_kb"));
            initializeKnowledgeBase();
            log.info(I18N.get("log.kqa.reinit_complete"));

            return result;

        } catch (Exception e) {
            log.error(I18N.get("log.kqa.incremental_error"), e);

            // 尝试恢复知识库实例 / Try to recover knowledge base instance
            try {
                if (rag == null) {
                    log.info(I18N.get("log.kqa.recover_kb"));
                    initializeKnowledgeBase();
                }
            } catch (Exception ex) {
                log.error(I18N.get("log.kqa.recover_failed"), ex);
            }

            throw new RuntimeException(I18N.get("log.kqa.build_failed", e.getMessage()), e);
        } finally {
            // 无论成功或失败，都重置索引状态
            isIndexing = false;
        }
    }

    /**
     * 搜索文档 / Search documents
     */
    public List<Document> searchDocuments(String query, int limit) {
        if (rag == null) {
            throw new IllegalStateException(I18N.get("log.kqa.kb_not_initialized"));
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
        log.info(I18N.get("knowledge_qa_service.destroy_start"));

        if (embeddingEngine != null) {
            embeddingEngine.close();
            log.info(I18N.get("knowledge_qa_service.vector_engine_closed"));
        }

        if (rag != null) {
            rag.close();
            log.info(I18N.get("knowledge_qa_service.kb_closed_safe"));
        }

        log.info(I18N.get("knowledge_qa_service.system_closed"));
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
            log.debug(I18N.get("knowledge_qa_service.log.record_saved", recordId));
            return recordId;
        } catch (Exception e) {
            log.warn(I18N.get("knowledge_qa_service.save_qa_failed", e));
            return null;
        }
    }

    /**
     * 使用检索策略调度器执行检索
     * (Execute search using strategy dispatcher)
     *
     * @param question 查询问题 (Query question)
     * @return 检索到的文档列表 (Retrieved document list)
     */
    private List<top.yumbo.ai.rag.model.Document> searchWithStrategyDispatcher(String question) {
        // 构建检索上下文 (Build search context)
        SearchContext.SearchParameters params = new SearchContext.SearchParameters();
        params.setLuceneTopK(configService.getLuceneTopK());
        params.setVectorTopK(configService.getVectorTopK());
        params.setHybridTopK(configService.getHybridTopK());
        params.setMinScoreThreshold(configService.getMinScoreThreshold());
        // 从 properties 获取权重配置 (Get weight config from properties)
        params.setLuceneWeight(properties.getVectorSearch().getLuceneWeight());
        params.setVectorWeight(properties.getVectorSearch().getVectorWeight());
        params.setSimilarityThreshold(properties.getVectorSearch().getSimilarityThreshold());

        // 提取关键词 (Extract keywords)
        String keywords = hybridSearchService.extractKeywords(question);

        SearchContext context =
            SearchContext.builder()
                .question(question)
                .expandedQuestion(question)
                .keywords(keywords)
                .rag(rag)
                .embeddingEngine(embeddingEngine)
                .vectorIndexEngine(vectorIndexEngine)
                .parameters(params)
                .build();

        // 使用策略调度器执行检索 (Execute search using strategy dispatcher)
        return searchStrategyDispatcher.search(context);
    }
}
