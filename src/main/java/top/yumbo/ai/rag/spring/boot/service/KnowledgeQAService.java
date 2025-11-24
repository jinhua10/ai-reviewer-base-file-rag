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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;

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
    private final LLMClient llmClient;

    private LocalFileRAG rag;
    private LocalEmbeddingEngine embeddingEngine;
    private SimpleVectorIndexEngine vectorIndexEngine;
    private top.yumbo.ai.rag.optimization.SmartContextBuilder contextBuilder;

    public KnowledgeQAService(KnowledgeQAProperties properties,
                              KnowledgeBaseService knowledgeBaseService,
                              HybridSearchService hybridSearchService,
                              LLMClient llmClient) {
        this.properties = properties;
        this.knowledgeBaseService = knowledgeBaseService;
        this.hybridSearchService = hybridSearchService;
        this.llmClient = llmClient;
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

        // 初始化智能上下文构建器
        contextBuilder = SmartContextBuilder.builder()
            .maxContextLength(properties.getLlm().getMaxContextLength())
            .maxDocLength(properties.getLlm().getMaxDocLength())
            .build();

        log.info("   ✅ 智能上下文构建器已初始化");
        log.info("      - 最大上下文: {} 字符", properties.getLlm().getMaxContextLength());
        log.info("      - 最大文档长度: {} 字符", properties.getLlm().getMaxDocLength());

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

            // 步骤2: 构建智能上下文
            String context = contextBuilder.buildSmartContext(question, documents);
            log.info("Context stats: {}", contextBuilder.getContextStats(context));

            // 步骤3: 构建 Prompt
            String prompt = buildPrompt(question, context);

            // 步骤4: 调用 LLM 生成答案
            String answer = llmClient.generate(prompt);

            // 步骤5: 提取文档来源
            List<String> sources = documents.stream()
                .map(Document::getTitle)
                .distinct()
                .toList();

            long totalTime = System.currentTimeMillis() - startTime;

            // 显示结果
            log.info("\n💡 回答:");
            log.info(answer);
            log.info("\n📚 数据来源 (共{}个文档):", sources.size());
            sources.forEach(source -> log.info("   - {}", source));
            log.info("\n⏱️  响应时间: {}ms", totalTime);
            log.info("=".repeat(80));

            return new AIAnswer(answer, sources, totalTime);

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
     * 构建 LLM Prompt
     */
    private String buildPrompt(String question, String context) {
        return String.format("""
            你是一个专业的知识助手。请基于以下文档内容回答用户问题。
            
            # 相关文档
            %s
            
            # 用户问题
            %s
            
            # 回答要求
            1. 必须基于文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，明确告知用户
            3. 回答要清晰、准确、有条理
            4. 可以引用文档名称作为信息来源
            5. 保持专业友好的语气
            
            # 请提供你的回答：
            """, context, question);
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
}
