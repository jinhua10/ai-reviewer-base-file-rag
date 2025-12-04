package top.yumbo.ai.rag.ppl.config;

import lombok.Data;

/**
 * PPL Chunking 配置
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class ChunkConfig {

    /**
     * PPL 阈值：当困惑度变化超过此阈值时进行切分
     * 建议值：15.0 - 25.0
     * - 较低值（15）：切分更细，块更小
     * - 较高值（25）：切分更粗，块更大
     */
    private double pplThreshold = 20.0;

    /**
     * 最大块大小（字符数）
     * 防止单个块过大导致处理困难
     */
    private int maxChunkSize = 2000;

    /**
     * 最小块大小（字符数）
     * 防止切分过细导致语义不完整
     */
    private int minChunkSize = 200;

    /**
     * 块之间的重叠大小（字符数）
     * 用于保持上下文连贯性
     */
    private int overlapSize = 100;

    /**
     * 是否启用粗分块（两阶段切分）
     * true: 先按 maxChunkSize 粗分，再用 PPL 精细切分（推荐）
     * false: 直接对整个文档进行 PPL 切分
     */
    private boolean enableCoarseChunking = true;

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (pplThreshold <= 0) {
            throw new IllegalArgumentException("pplThreshold must be positive");
        }
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize must be positive");
        }
        if (minChunkSize <= 0) {
            throw new IllegalArgumentException("minChunkSize must be positive");
        }
        if (minChunkSize > maxChunkSize) {
            throw new IllegalArgumentException("minChunkSize cannot be greater than maxChunkSize");
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("overlapSize cannot be negative");
        }
        if (overlapSize >= minChunkSize) {
            throw new IllegalArgumentException("overlapSize should be less than minChunkSize");
        }
    }
}

