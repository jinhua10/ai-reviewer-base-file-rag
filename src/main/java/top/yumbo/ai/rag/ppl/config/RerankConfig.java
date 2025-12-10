package top.yumbo.ai.rag.ppl.config;

import lombok.Data;

/**
 * PPL Rerank 配置 (PPL Rerank Configuration)
 * 
 * 用于配置基于困惑度（PPL）的文档重排序参数
 * (Used to configure parameters for PPL-based document reranking)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class RerankConfig {

    /**
     * 是否启用 PPL Rerank
     * Whether to enable PPL Rerank
     */
    private boolean enabled = false;

    /**
     * PPL 分数的权重（0.0 - 1.0）
     * Weight of PPL score (0.0 - 1.0)
     * 最终分数 = (1 - weight) * 原始分数 + weight * PPL分数
     * Final score = (1 - weight) * original score + weight * PPL score
     *
     * 建议值：0.10 - 0.20 (Recommended values: 0.10 - 0.20)
     * - 较低值（0.10）：主要依赖原始检索分数 (Lower value (0.10): mainly rely on original retrieval score)
     * - 较高值（0.20）：更重视 PPL 流畅度 (Higher value (0.20): give more importance to PPL fluency)
     */
    private double weight = 0.15;

    /**
     * 仅对前 K 个文档进行 PPL Rerank
     * Only PPL Rerank the top K documents
     * 原因：PPL 计算成本较高，只对最有可能相关的文档重排序
     * Reason: PPL calculation is costly, only rerank most likely relevant documents
     *
     * 建议值：3 - 10 (Recommended values: 3 - 10)
     */
    private int topK = 5;

    /**
     * 是否异步执行 Rerank
     * Whether to execute Rerank asynchronously
     * true: 不阻塞主流程，适合对延迟敏感的场景
     * true: Do not block the main process, suitable for latency-sensitive scenarios
     * false: 同步执行，确保返回重排序后的结果
     * false: Execute synchronously, ensure reranked results are returned
     */
    private boolean async = true;

    /**
     * Rerank 超时时间（毫秒）
     * Rerank timeout time (milliseconds)
     * 防止 Rerank 耗时过长影响用户体验
     * Prevent Rerank from taking too long and affecting user experience
     */
    private int timeout = 5000;

    /**
     * 文档内容截断长度（字符数）
     * Document content truncation length (character count)
     * 用于控制 PPL 计算的成本
     * Used to control the cost of PPL calculation
     * 仅使用文档的前 N 个字符参与 PPL 计算
     * Only use the first N characters of the document for PPL calculation
     */
    private int contentTruncateLength = 500;

    /**
     * 验证配置的有效性 (Validate configuration validity)
     * 
     * @throws IllegalArgumentException 当配置无效时抛出 (Thrown when configuration is invalid)
     */
    public void validate() {
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("weight must be between 0.0 and 1.0");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (contentTruncateLength <= 0) {
            throw new IllegalArgumentException("contentTruncateLength must be positive");
        }
    }
}

