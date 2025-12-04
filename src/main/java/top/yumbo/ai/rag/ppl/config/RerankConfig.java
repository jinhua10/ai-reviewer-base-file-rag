package top.yumbo.ai.rag.ppl.config;

import lombok.Data;

/**
 * PPL Rerank 配置
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class RerankConfig {

    /**
     * 是否启用 PPL Rerank
     */
    private boolean enabled = false;

    /**
     * PPL 分数的权重（0.0 - 1.0）
     * 最终分数 = (1 - weight) * 原始分数 + weight * PPL分数
     *
     * 建议值：0.10 - 0.20
     * - 较低值（0.10）：主要依赖原始检索分数
     * - 较高值（0.20）：更重视 PPL 流畅度
     */
    private double weight = 0.15;

    /**
     * 仅对前 K 个文档进行 PPL Rerank
     * 原因：PPL 计算成本较高，只对最有可能相关的文档重排序
     *
     * 建议值：3 - 10
     */
    private int topK = 5;

    /**
     * 是否异步执行 Rerank
     * true: 不阻塞主流程，适合对延迟敏感的场景
     * false: 同步执行，确保返回重排序后的结果
     */
    private boolean async = true;

    /**
     * Rerank 超时时间（毫秒）
     * 防止 Rerank 耗时过长影响用户体验
     */
    private int timeout = 5000;

    /**
     * 文档内容截断长度（字符数）
     * 用于控制 PPL 计算的成本
     * 仅使用文档的前 N 个字符参与 PPL 计算
     */
    private int contentTruncateLength = 500;

    /**
     * 验证配置的有效性
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

