package top.yumbo.ai.rag.spring.boot.model.document;

/**
 * 片段类型枚举
 *
 * 定义不同文档类型的分析单位
 */
public enum SegmentType {
    // ==================== PPT ====================
    /** 幻灯片 */
    SLIDE("幻灯片", "slide"),

    // ==================== PDF/Word ====================
    /** 页面 */
    PAGE("页面", "page"),

    /** 章节 */
    CHAPTER("章节", "chapter"),

    /** 小节 */
    SECTION("小节", "section"),

    /** 段落 */
    PARAGRAPH("段落", "paragraph"),

    // ==================== Markdown ====================
    /** 标题块 */
    HEADING_BLOCK("标题块", "heading"),

    // ==================== 代码 ====================
    /** 文件 */
    FILE("文件", "file"),

    /** 类 */
    CLASS("类", "class"),

    /** 函数/方法 */
    FUNCTION("函数", "function"),

    /** 模块 */
    MODULE("模块", "module"),

    // ==================== 通用 ====================
    /** 文本块（固定长度分割） */
    TEXT_CHUNK("文本块", "chunk"),

    /** 表格 */
    TABLE("表格", "table"),

    /** 图片 */
    IMAGE("图片", "image"),

    /** 列表 */
    LIST("列表", "list");

    private final String displayName;
    private final String code;

    SegmentType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取枚举值
     */
    public static SegmentType fromCode(String code) {
        for (SegmentType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return TEXT_CHUNK; // 默认返回文本块
    }
}

