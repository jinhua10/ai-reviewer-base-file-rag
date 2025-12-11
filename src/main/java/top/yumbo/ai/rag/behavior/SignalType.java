package top.yumbo.ai.rag.behavior;

import lombok.Getter;

/**
 * 行为信号类型枚举 (Behavior Signal Type Enum)
 * 定义用户在使用系统时的各种行为信号，用于推断用户对答案的态度
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
@Getter
public enum SignalType {

    // ========== 操作类信号 (Operation Signals) ==========

    /**
     * 复制答案 (Copy Answer)
     * 强正面信号：用户认为答案有价值，想要保存使用
     * 权重：+2.0
     */
    COPY_ANSWER("operation.copy_answer", 2.0, SignalCategory.OPERATION),

    /**
     * 展开详情 (Expand Detail)
     * 正面信号：用户对答案感兴趣，想要了解更多
     * 权重：+1.0
     */
    EXPAND_DETAIL("operation.expand_detail", 1.0, SignalCategory.OPERATION),

    /**
     * 点击参考链接 (Click Reference)
     * 正面信号：用户想要验证或深入了解
     * 权重：+1.0
     */
    CLICK_REFERENCE("operation.click_reference", 1.0, SignalCategory.OPERATION),

    /**
     * 快速下滑 (Scroll Down Quickly)
     * 负面信号：用户对答案不感兴趣，快速跳过
     * 权重：-1.0
     */
    SCROLL_DOWN("operation.scroll_down", -1.0, SignalCategory.OPERATION),

    /**
     * 立即关闭 (Close Immediately)
     * 强负面信号：用户对答案非常不满意，立即离开
     * 权重：-2.0
     */
    CLOSE_IMMEDIATELY("operation.close_immediately", -2.0, SignalCategory.OPERATION),

    // ========== 时间类信号 (Time Signals) ==========

    /**
     * 阅读时间短 (Short Read Time)
     * 负面信号：阅读时间 < 预期时间的30%
     * 权重：-1.0
     */
    READ_TIME_SHORT("time.read_short", -1.0, SignalCategory.TIME),

    /**
     * 正常阅读时间 (Normal Read Time)
     * 中性信号：阅读时间在预期范围内（30%-150%）
     * 权重：0.0
     */
    READ_TIME_NORMAL("time.read_normal", 0.0, SignalCategory.TIME),

    /**
     * 深度阅读 (Long Read Time)
     * 正面信号：阅读时间 > 预期时间的150%
     * 权重：+1.0
     */
    READ_TIME_LONG("time.read_long", 1.0, SignalCategory.TIME),

    /**
     * 返回查看 (Return Visit)
     * 正面信号：用户在一段时间后返回再次查看
     * 权重：+1.5
     */
    RETURN_VISIT("time.return_visit", 1.5, SignalCategory.TIME),

    // ========== 交互类信号 (Interaction Signals) ==========

    /**
     * 追问 (Ask Follow-up)
     * 中性/正面信号：用户想要了解更多，但可能是因为答案不够清晰
     * 权重：0.5（需要根据追问内容调整）
     */
    ASK_FOLLOWUP("interaction.ask_followup", 0.5, SignalCategory.INTERACTION),

    /**
     * 分享答案 (Share Answer)
     * 强正面信号：用户认为答案非常有价值，分享给他人
     * 权重：+2.0
     */
    SHARE_ANSWER("interaction.share_answer", 2.0, SignalCategory.INTERACTION),

    /**
     * 报告错误 (Report Error)
     * 强负面信号：用户发现答案有错误
     * 权重：-2.0
     */
    REPORT_ERROR("interaction.report_error", -2.0, SignalCategory.INTERACTION),

    /**
     * 编辑答案 (Edit Answer)
     * 负面信号：用户认为答案需要修改
     * 权重：-1.0
     */
    EDIT_ANSWER("interaction.edit_answer", -1.0, SignalCategory.INTERACTION),

    // ========== 导航类信号 (Navigation Signals) ==========

    /**
     * 再次搜索 (Search Again)
     * 负面信号：用户对当前答案不满意，重新搜索
     * 权重：-1.5
     */
    SEARCH_AGAIN("navigation.search_again", -1.5, SignalCategory.NAVIGATION),

    /**
     * 查看其他答案 (View Alternative)
     * 负面信号：用户想要查看其他答案进行对比
     * 权重：-1.0
     */
    VIEW_ALTERNATIVE("navigation.view_alternative", -1.0, SignalCategory.NAVIGATION);

    // ========== 枚举属性 (Enum Properties) ==========

    /**
     * 信号标识符 (Signal Identifier)
     */
    private final String identifier;

    /**
     * 基础权重 (Base Weight)
     * 范围：-2.0（强负面）到 +2.0（强正面）
     */
    private final double baseWeight;

    /**
     * 信号分类 (Signal Category)
     */
    private final SignalCategory category;

    /**
     * 构造函数 (Constructor)
     */
    SignalType(String identifier, double baseWeight, SignalCategory category) {
        this.identifier = identifier;
        this.baseWeight = baseWeight;
        this.category = category;
    }

    // ========== Getter 方法 (Getter Methods) ==========

    /**
     * 判断是否为正面信号 (Is Positive Signal)
     */
    public boolean isPositive() {
        return baseWeight > 0;
    }

    /**
     * 判断是否为负面信号 (Is Negative Signal)
     */
    public boolean isNegative() {
        return baseWeight < 0;
    }

    /**
     * 判断是否为中性信号 (Is Neutral Signal)
     */
    public boolean isNeutral() {
        return baseWeight == 0;
    }

    /**
     * 判断是否为强信号 (Is Strong Signal)
     * 强信号的绝对值 >= 1.5
     */
    public boolean isStrong() {
        return Math.abs(baseWeight) >= 1.5;
    }

    /**
     * 根据标识符查找信号类型 (Find by Identifier)
     */
    public static SignalType fromIdentifier(String identifier) {
        for (SignalType type : values()) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown signal identifier: " + identifier);
    }
}

