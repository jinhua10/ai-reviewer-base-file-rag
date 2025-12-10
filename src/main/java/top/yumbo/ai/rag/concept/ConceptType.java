package top.yumbo.ai.rag.concept;

/**
 * 概念类型枚举 (Concept Type Enum)
 *
 * 定义系统中支持的概念类型
 * (Defines concept types supported by the system)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public enum ConceptType {

    /**
     * 文档级概念 (Document level)
     */
    DOCUMENT("文档", "Document"),

    /**
     * 章节级概念 (Section level)
     */
    SECTION("章节", "Section"),

    /**
     * 技术概念 (Technical concept)
     */
    TECHNICAL("技术", "Technical"),

    /**
     * 业务概念 (Business concept)
     */
    BUSINESS("业务", "Business"),

    /**
     * 算法概念 (Algorithm concept)
     */
    ALGORITHM("算法", "Algorithm"),

    /**
     * 架构概念 (Architecture concept)
     */
    ARCHITECTURE("架构", "Architecture"),

    /**
     * API 概念 (API concept)
     */
    API("接口", "API"),

    /**
     * 数据结构 (Data structure)
     */
    DATA_STRUCTURE("数据结构", "Data Structure"),

    /**
     * 设计模式 (Design pattern)
     */
    DESIGN_PATTERN("设计模式", "Design Pattern"),

    /**
     * 框架/库 (Framework/Library)
     */
    FRAMEWORK("框架", "Framework"),

    /**
     * 工具 (Tool)
     */
    TOOL("工具", "Tool"),

    /**
     * 通用概念 (General concept)
     */
    GENERAL("通用", "General"),

    /**
     * 其他 (Other)
     */
    OTHER("其他", "Other");

    private final String chineseName;
    private final String englishName;

    ConceptType(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    /**
     * 获取中文名称 (Get Chinese name)
     */
    public String getChineseName() {
        return chineseName;
    }

    /**
     * 获取英文名称 (Get English name)
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * 根据字符串获取概念类型 (Get concept type by string)
     */
    public static ConceptType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return GENERAL;
        }

        String upperType = type.toUpperCase();
        try {
            return ConceptType.valueOf(upperType);
        } catch (IllegalArgumentException e) {
            // 尝试匹配中文名称 (Try to match Chinese name)
            for (ConceptType ct : ConceptType.values()) {
                if (ct.chineseName.equals(type)) {
                    return ct;
                }
            }
            return GENERAL;
        }
    }
}

