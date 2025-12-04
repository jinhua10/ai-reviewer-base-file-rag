package top.yumbo.ai.rag.ppl;

import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;
import top.yumbo.ai.rag.ppl.config.RerankConfig;

import java.util.List;
import java.util.Map;

/**
 * PPL 服务统一接口
 *
 * 支持三种实现：
 * - ONNX: Hugging Face ONNX Runtime（本地嵌入式）
 * - Ollama: 本地 LLM 服务器
 * - OpenAI: 云端 API 服务
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public interface PPLService {

    /**
     * 计算文本的困惑度（Perplexity）
     *
     * @param text 待计算的文本
     * @return 困惑度值（越低表示越符合语言模型的预期）
     * @throws PPLException 计算失败时抛出
     */
    double calculatePerplexity(String text) throws PPLException;

    /**
     * 批量计算困惑度（可选优化，默认实现为逐个计算）
     *
     * @param texts 待计算的文本列表
     * @return 文本到困惑度的映射
     */
    default Map<String, Double> batchCalculatePerplexity(List<String> texts) {
        Map<String, Double> results = new java.util.HashMap<>();
        for (String text : texts) {
            try {
                results.put(text, calculatePerplexity(text));
            } catch (PPLException e) {
                // 失败时返回最大值
                results.put(text, Double.MAX_VALUE);
            }
        }
        return results;
    }

    /**
     * 基于 PPL 的文档切分
     *
     * 原理：当文本的困惑度突然升高时，表明主题发生了转换，此时进行切分
     *
     * @param content 待切分的文档内容
     * @param query 查询问题（可选，用于上下文相关的切分）
     * @param config 切分配置
     * @return 切分后的文档块列表
     * @throws PPLException 切分失败时抛出
     */
    List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException;

    /**
     * 基于 PPL 的文档重排序
     *
     * 原理：计算"问题+文档"的困惑度，困惑度越低表示该文档越相关
     *
     * @param question 查询问题
     * @param candidates 候选文档列表
     * @param config 重排序配置
     * @return 重排序后的文档列表
     * @throws PPLException 重排序失败时抛出
     */
    List<Document> rerank(String question, List<Document> candidates, RerankConfig config) throws PPLException;

    /**
     * 获取服务提供商类型
     *
     * @return 提供商类型
     */
    PPLProviderType getProviderType();

    /**
     * 健康检查
     *
     * @return true 表示服务正常，false 表示服务异常
     */
    boolean isHealthy();

    /**
     * 获取性能指标
     *
     * @return 性能指标对象
     */
    PPLMetrics getMetrics();

    /**
     * 预热服务（可选，用于提前加载模型）
     */
    default void warmup() {
        // 默认不做任何操作
    }

    /**
     * 关闭服务，释放资源
     */
    default void close() {
        // 默认不做任何操作
    }
}

