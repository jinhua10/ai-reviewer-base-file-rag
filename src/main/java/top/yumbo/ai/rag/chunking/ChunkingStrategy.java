package top.yumbo.ai.rag.chunking;

/**
 * 文档切分策略 (Document chunking strategies)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public enum ChunkingStrategy {

    /**
     * 不切分，直接截断 (No chunking - take full content)
     * 适用场景：文档较小，不需要切分 (When documents are small and don't need chunking)
     * 性能：最好 (Best performance)
     * 成本：最低 (Lowest cost)
     * 效果：可能丢失内容 (May lose content relevance)
     */
    NONE("不切分 (No chunking)"),

    /**
     * 简单切分，按固定长度切分 (Simple chunking by fixed length)
     * 适用场景：对质量要求不高，追求性能 (When prioritizing performance over quality)
     * 性能：很好 (Good performance)
     * 成本：低 (Low cost)
     * 效果：一般（可能在句子中间切断） (Average quality - may break sentences)
     */
    SIMPLE("简单切分 (Simple chunking)"),

    /**
     * 智能关键词切分，优先保留包含关键词的内容 (Smart keyword chunking - keep keyword-containing parts)
     * 适用场景：平衡效果和成本（推荐） (Balanced quality and cost - recommended)
     * 性能：好 (Good performance)
     * 成本：中等 (Medium cost)
     * 效果：好（保留最相关内容） (Good quality - keeps most relevant parts)
     */
    SMART_KEYWORD("智能关键词切分 (Smart keyword chunking)"),

    /**
     * AI 语义切分，使用 AI 模型智能切分 (AI semantic chunking using LLMs)
     * 适用场景：对质量要求高，预算充足 (High quality required and sufficient budget)
     * 性能：较慢 (Slower performance)
     * 成本：高 (Higher cost)
     * 效果：最好（语义完整，逻辑连贯） (Best quality - semantic coherence)
     */
    AI_SEMANTIC("AI语义切分 (AI semantic chunking)");

    private final String description;

    ChunkingStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析策略 (Parse strategy from string)
     */
    public static ChunkingStrategy fromString(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return SMART_KEYWORD; // 默认策略 (default strategy)
        }

        try {
            return ChunkingStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SMART_KEYWORD; // 无效值使用默认 (invalid -> default)
        }
    }
}
