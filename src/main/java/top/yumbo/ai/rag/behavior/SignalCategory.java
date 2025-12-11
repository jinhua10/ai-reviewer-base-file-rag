package top.yumbo.ai.rag.behavior;

import lombok.Getter;

/**
 * 信号分类枚举 (Signal Category Enum)
 * 将行为信号按类型分类，便于分析和统计
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
@Getter
public enum SignalCategory {

    /**
     * 操作类信号 (Operation Signals)
     * 包括：复制、展开、点击、滚动等操作行为
     */
    OPERATION("operation"),

    /**
     * 时间类信号 (Time Signals)
     * 包括：阅读时长、返回访问等时间相关行为
     */
    TIME("time"),

    /**
     * 交互类信号 (Interaction Signals)
     * 包括：追问、分享、报错、编辑等交互行为
     */
    INTERACTION("interaction"),

    /**
     * 导航类信号 (Navigation Signals)
     * 包括：再次搜索、查看其他答案等导航行为
     */
    NAVIGATION("navigation");

    private final String identifier;

    SignalCategory(String identifier) {
        this.identifier = identifier;
    }

}

