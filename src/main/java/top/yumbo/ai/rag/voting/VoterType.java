package top.yumbo.ai.rag.voting;

/**
 * 投票者类型 (Voter Type)
 *
 * 定义不同类型投票者的权重
 * (Defines weights for different voter types)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public enum VoterType {

    /**
     * 普通用户 (Regular user)
     */
    REGULAR_USER("普通用户", "Regular User", 1.0),

    /**
     * 活跃用户 (Power user)
     */
    POWER_USER("活跃用户", "Power User", 1.5),

    /**
     * 领域专家 (Expert)
     */
    EXPERT("领域专家", "Expert", 3.0),

    /**
     * AI系统 (AI system)
     */
    AI_SYSTEM("AI系统", "AI System", 2.0),

    /**
     * 维护者 (Maintainer)
     */
    MAINTAINER("维护者", "Maintainer", 5.0);

    /**
     * 中文名称 (Chinese name)
     */
    private final String nameCn;

    /**
     * 英文名称 (English name)
     */
    private final String nameEn;

    /**
     * 默认权重 (Default weight)
     */
    private final double defaultWeight;

    VoterType(String nameCn, String nameEn, double defaultWeight) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.defaultWeight = defaultWeight;
    }

    public String getNameCn() {
        return nameCn;
    }

    public String getNameEn() {
        return nameEn;
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }
}

