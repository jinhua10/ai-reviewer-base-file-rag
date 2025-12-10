package top.yumbo.ai.rag.ppl;

import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;
import top.yumbo.ai.rag.ppl.config.RerankConfig;

import java.util.List;
import java.util.Map;

/**
 * PPL 服务统一接口 (PPL Service Unified Interface)
 *
 * 支持三种实现 (Supports three implementations):
 * - ONNX: Hugging Face ONNX Runtime（本地嵌入式）(Local embedded inference)
 * - Ollama: 本地 LLM 服务器 (Local LLM server)
 * - OpenAI: 云端 API 服务 (Cloud API service)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public interface PPLService {

    /**
     * 计算文本的困惑度（Perplexity）(Calculate text perplexity)
     * 
     * 困惑度是评估语言模型预测文本概率的指标，值越低表示文本越符合语言模型的预期
     * (Perplexity is a metric for evaluating the probability of a language model predicting text, 
     * lower values indicate the text is more expected by the language model)
     *
     * @param text 待计算的文本 (Text to calculate)
     * @return 困惑度值（越低表示越符合语言模型的预期）(Perplexity value, lower indicates more expected by the language model)
     * @throws PPLException 计算失败时抛出 (Thrown when calculation fails)
     */
    double calculatePerplexity(String text) throws PPLException;

    /**
     * 批量计算困惑度（可选优化，默认实现为逐个计算）(Batch calculate perplexity - optional optimization, default implementation is individual calculation)
     *
     * @param texts 待计算的文本列表 (List of texts to calculate)
     * @return 文本到困惑度的映射 (Mapping from text to perplexity)
     */
    default Map<String, Double> batchCalculatePerplexity(List<String> texts) {
        Map<String, Double> results = new java.util.HashMap<>();
        for (String text : texts) {
            try {
                results.put(text, calculatePerplexity(text));
            } catch (PPLException e) {
                // 失败时返回最大值 (Return maximum value on failure)
                results.put(text, Double.MAX_VALUE);
            }
        }
        return results;
    }

    /**
     * 基于 PPL 的文档切分 (PPL-based document chunking)
     *
     * 原理：当文本的困惑度突然升高时，表明主题发生了转换，此时进行切分
     * (Principle: When the perplexity of text suddenly increases, it indicates a topic transition, at which point chunking is performed)
     *
     * @param content 待切分的文档内容 (Document content to be chunked)
     * @param query 查询问题（可选，用于上下文相关的切分）(Query question (optional, for context-related chunking))
     * @param config 切分配置 (Chunking configuration)
     * @return 切分后的文档块列表 (List of document chunks after chunking)
     * @throws PPLException 切分失败时抛出 (Thrown when chunking fails)
     */
    List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException;

    /**
     * 基于 PPL 的文档重排序 (PPL-based document reranking)
     *
     * 原理：计算"问题+文档"的困惑度，困惑度越低表示该文档越相关
     * (Principle: Calculate the perplexity of "question+document", lower perplexity indicates the document is more relevant)
     *
     * @param question 查询问题 (Query question)
     * @param candidates 候选文档列表 (Candidate document list)
     * @param config 重排序配置 (Reranking configuration)
     * @return 重排序后的文档列表 (Reranked document list)
     * @throws PPLException 重排序失败时抛出 (Thrown when reranking fails)
     */
    List<Document> rerank(String question, List<Document> candidates, RerankConfig config) throws PPLException;

    /**
     * 获取服务提供商类型 (Get service provider type)
     *
     * @return 提供商类型 (Provider type)
     */
    PPLProviderType getProviderType();

    /**
     * 健康检查 (Health check)
     *
     * @return true 表示服务正常，false 表示服务异常 (true indicates service is normal, false indicates service is abnormal)
     */
    boolean isHealthy();

    /**
     * 获取性能指标 (Get performance metrics)
     *
     * @return 性能指标对象 (Performance metrics object)
     */
    PPLMetrics getMetrics();

    /**
     * 预热服务（可选，用于提前加载模型）(Warm up service (optional, used for pre-loading models))
     */
    default void warmup() {
        // 默认不做任何操作 (Default do nothing)
    }

    /**
     * 关闭服务，释放资源 (Close service and release resources)
     */
    default void close() {
        // 默认不做任何操作 (Default do nothing)
    }
}

