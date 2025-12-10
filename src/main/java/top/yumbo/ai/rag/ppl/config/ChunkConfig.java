package top.yumbo.ai.rag.ppl.config;

import lombok.Data;

/**
 * PPL Chunking 配置 (PPL Chunking Configuration)
 * 
 * 用于配置基于困惑度（PPL）的文档切分参数
 * (Used to configure parameters for PPL-based document chunking)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
public class ChunkConfig {

    /**
     * PPL 阈值：当困惑度变化超过此阈值时进行切分
     * PPL threshold: Chunk when perplexity change exceeds this threshold
     * 建议值：15.0 - 25.0 (Recommended values: 15.0 - 25.0)
     * - 较低值（15）：切分更细，块更小 (Lower value (15): finer chunking, smaller chunks)
     * - 较高值（25）：切分更粗，块更大 (Higher value (25): coarser chunking, larger chunks)
     */
    private double pplThreshold = 20.0;

    /**
     * 最大块大小（字符数）- 硬性上限
     * Maximum chunk size (character count) - hard limit
     * 防止单个块过大导致处理困难
     * Prevents individual chunks from being too large and causing processing difficulties
     */
    private int maxChunkSize = 2500;

    /**
     * 目标块大小（字符数）- 软限制
     * Target chunk size (character count) - soft limit
     * 到达此大小后会寻找最近的语义边界切分
     * Will find the nearest semantic boundary to chunk when reaching this size
     */
    private int targetChunkSize = 1500;

    /**
     * 最小块大小（字符数）
     * Minimum chunk size (character count)
     * 防止切分过细导致语义不完整
     * Prevents overly fine chunking that leads to incomplete semantics
     */
    private int minChunkSize = 300;

    /**
     * 块之间的重叠大小（字符数）
     * Overlap size between chunks (character count)
     * 用于保持上下文连贯性
     * Used to maintain context continuity
     */
    private int overlapSize = 150;

    /**
     * 重叠的句子数量
     * Number of overlapping sentences
     * 保留前一块最后 N 个句子作为上下文
     * Keep the last N sentences of the previous chunk as context
     */
    private int overlapSentences = 2;

    /**
     * 是否启用粗分块（两阶段切分）
     * Whether to enable coarse chunking (two-stage chunking)
     * true: 先按 maxChunkSize 粗分，再用 PPL 精细切分（推荐）
     * true: First coarse chunk by maxChunkSize, then fine chunk by PPL (recommended)
     * false: 直接对整个文档进行 PPL 切分
     * false: Directly PPL chunk the entire document
     */
    private boolean enableCoarseChunking = true;

    /**
     * 是否启用语义感知分块
     * Whether to enable semantic-aware chunking
     * true: 在语义边界（段落、章节、列表结束）处切分
     * true: Chunk at semantic boundaries (paragraphs, chapters, list ends)
     * false: 按固定字数切分
     * false: Chunk by fixed character count
     */
    private boolean semanticAware = true;

    /**
     * 是否检测段落边界
     * Whether to detect paragraph boundaries
     */
    private boolean detectParagraph = true;

    /**
     * 是否检测章节标题
     * Whether to detect chapter titles
     */
    private boolean detectChapter = true;

    /**
     * 是否检测列表结束
     * Whether to detect list ends
     */
    private boolean detectListEnd = true;

    /**
     * 是否使用动态 PPL 阈值
     * Whether to use dynamic PPL threshold
     * true: 根据文档 PPL 分布自动调整阈值
     * true: Automatically adjust threshold based on document PPL distribution
     * false: 使用固定阈值
     * false: Use fixed threshold
     */
    private boolean useDynamicThreshold = false;

    /**
     * 验证配置的有效性 (Validate configuration validity)
     * 
     * @throws IllegalArgumentException 当配置无效时抛出 (Thrown when configuration is invalid)
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

