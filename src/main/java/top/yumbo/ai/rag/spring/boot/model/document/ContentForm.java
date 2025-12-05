package top.yumbo.ai.rag.spring.boot.model.document;

/**
 * 内容形式枚举
 *
 * 用于判断备忘录内容是否需要再次处理
 */
public enum ContentForm {
    /** 原始文本 - 需要压缩 */
    RAW_TEXT("原始文本", true),

    /** 结构化要点 - 已精简，可能跳过处理 */
    STRUCTURED_BULLETS("结构化要点", false),

    /** 关键数据 - 数字/日期为主，保持原样 */
    KEY_DATA("关键数据", false),

    /** 代码/公式 - 特殊格式，谨慎处理 */
    CODE_OR_FORMULA("代码/公式", false),

    /** 已聚合摘要 - 上层摘要 */
    AGGREGATED_SUMMARY("已聚合摘要", false);

    private final String displayName;
    private final boolean needsCompression;

    ContentForm(String displayName, boolean needsCompression) {
        this.displayName = displayName;
        this.needsCompression = needsCompression;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 是否需要压缩处理
     */
    public boolean needsCompression() {
        return needsCompression;
    }
}

