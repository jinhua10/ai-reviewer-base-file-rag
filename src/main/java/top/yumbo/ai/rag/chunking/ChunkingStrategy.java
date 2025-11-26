package top.yumbo.ai.rag.chunking;

/**
 * 文档切分策略
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public enum ChunkingStrategy {

    /**
     * 不切分，直接截断
     * 适用场景：文档较小，不需要切分
     * 性能：最好
     * 成本：最低
     * 效果：可能丢失内容
     */
    NONE("不切分"),

    /**
     * 简单切分，按固定长度切分
     * 适用场景：对质量要求不高，追求性能
     * 性能：很好
     * 成本：低
     * 效果：一般（可能在句子中间切断）
     */
    SIMPLE("简单切分"),

    /**
     * 智能关键词切分，优先保留包含关键词的内容
     * 适用场景：平衡效果和成本（推荐）
     * 性能：好
     * 成本：中等
     * 效果：好（保留最相关内容）
     */
    SMART_KEYWORD("智能关键词切分"),

    /**
     * AI 语义切分，使用 AI 模型智能切分
     * 适用场景：对质量要求高，预算充足
     * 性能：较慢
     * 成本：高
     * 效果：最好（语义完整，逻辑连贯）
     */
    AI_SEMANTIC("AI语义切分");

    private final String description;

    ChunkingStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析策略
     */
    public static ChunkingStrategy fromString(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return SMART_KEYWORD; // 默认策略
        }

        try {
            return ChunkingStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SMART_KEYWORD; // 无效值使用默认
        }
    }
}

