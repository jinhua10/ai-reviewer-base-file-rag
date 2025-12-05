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
     * 最大块大小（字符数）- 硬性上限
     * 防止单个块过大导致处理困难
     */
    private int maxChunkSize = 2500;

    /**
     * 目标块大小（字符数）- 软限制
     * 到达此大小后会寻找最近的语义边界切分
     */
    private int targetChunkSize = 1500;

    /**
     * 最小块大小（字符数）
     * 防止切分过细导致语义不完整
     */
    private int minChunkSize = 300;

    /**
     * 块之间的重叠大小（字符数）
     * 用于保持上下文连贯性
     */
    private int overlapSize = 150;

    /**
     * 重叠的句子数量
     * 保留前一块最后 N 个句子作为上下文
     */
    private int overlapSentences = 2;

    /**
     * 是否启用粗分块（两阶段切分）
     * true: 先按 maxChunkSize 粗分，再用 PPL 精细切分（推荐）
     * false: 直接对整个文档进行 PPL 切分
     */
    private boolean enableCoarseChunking = true;

    /**
     * 是否启用语义感知分块
     * true: 在语义边界（段落、章节、列表结束）处切分
     * false: 按固定字数切分
     */
    private boolean semanticAware = true;

    /**
     * 是否检测段落边界
     */
    private boolean detectParagraph = true;

    /**
     * 是否检测章节标题
     */
    private boolean detectChapter = true;

    /**
     * 是否检测列表结束
     */
    private boolean detectListEnd = true;

    /**
     * 是否使用动态 PPL 阈值
     * true: 根据文档 PPL 分布自动调整阈值
     * false: 使用固定阈值
     */
    private boolean useDynamicThreshold = false;

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
        if (targetChunkSize <= 0) {
            targetChunkSize = (int) (maxChunkSize * 0.6);
        }
        if (targetChunkSize > maxChunkSize) {
            targetChunkSize = (int) (maxChunkSize * 0.6);
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
        if (overlapSentences < 0) {
            overlapSentences = 0;
        }
    }
}

