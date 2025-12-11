package top.yumbo.ai.rag.conflict;

/**
 * 冲突类型 (Conflict Type)
 *
 * 定义不同类型的知识冲突
 * (Defines different types of knowledge conflicts)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public enum ConflictType {

    /**
     * 语义冲突 (Semantic conflict)
     * 同一问题有不同答案
     * (Same question with different answers)
     */
    SEMANTIC("语义冲突", "Semantic conflict", 0.8),

    /**
     * 事实冲突 (Factual conflict)
     * 数据矛盾
     * (Data contradiction)
     */
    FACTUAL("事实冲突", "Factual conflict", 1.0),

    /**
     * 时效冲突 (Temporal conflict)
     * 新旧版本冲突
     * (Old vs new version conflict)
     */
    TEMPORAL("时效冲突", "Temporal conflict", 0.6),

    /**
     * 范围冲突 (Scope conflict)
     * 适用条件不同
     * (Different applicable conditions)
     */
    SCOPE("范围冲突", "Scope conflict", 0.5);

    /**
     * 中文名称 (Chinese name)
     */
    private final String nameCn;

    /**
     * 英文名称 (English name)
     */
    private final String nameEn;

    /**
     * 默认严重度 (Default severity)
     */
    private final double defaultSeverity;

    ConflictType(String nameCn, String nameEn, double defaultSeverity) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.defaultSeverity = defaultSeverity;
    }

    public String getNameCn() {
        return nameCn;
    }

    public String getNameEn() {
        return nameEn;
    }

    public double getDefaultSeverity() {
        return defaultSeverity;
    }
}

