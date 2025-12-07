package top.yumbo.ai.rag.hope;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HOPE 三层记忆架构配置
 * (HOPE Three-Layer Memory Architecture Configuration)
 *
 * 参考 Google HOPE 论文设计
 * (Designed based on Google HOPE paper)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge.qa.hope")
public class HOPEConfig {

    /**
     * 是否启用 HOPE 架构
     * (Whether to enable HOPE architecture)
     */
    private boolean enabled = true;

    /**
     * 低频层配置 (Permanent Layer Config)
     */
    private PermanentConfig permanent = new PermanentConfig();

    /**
     * 中频层配置 (Ordinary Layer Config)
     */
    private OrdinaryConfig ordinary = new OrdinaryConfig();

    /**
     * 高频层配置 (High-frequency Layer Config)
     */
    private HighFrequencyConfig highFrequency = new HighFrequencyConfig();

    /**
     * 响应策略配置 (Response Strategy Config)
     */
    private StrategyConfig strategy = new StrategyConfig();

    /**
     * 低频层配置 - 技能知识库
     * (Permanent Layer - Skill Knowledge Base)
     */
    @Data
    public static class PermanentConfig {
        /**
         * 存储路径
         */
        private String storagePath = "./data/hope/permanent";

        /**
         * 晋升到低频层的最小访问次数
         */
        private int promotionMinAccessCount = 10;

        /**
         * 晋升到低频层的最小平均评分
         */
        private double promotionMinAvgRating = 4.5;

        /**
         * 直接回答的最小置信度
         */
        private double directAnswerConfidence = 0.9;
    }

    /**
     * 中频层配置 - 近期知识
     * (Ordinary Layer - Recent Knowledge)
     */
    @Data
    public static class OrdinaryConfig {
        /**
         * 存储路径
         */
        private String storagePath = "./data/hope/ordinary";

        /**
         * 保留时间（天）
         */
        private int retentionDays = 30;

        /**
         * 相似度阈值（高于此值可直接使用历史答案）
         */
        private double similarityThreshold = 0.95;

        /**
         * 作为参考的相似度阈值
         */
        private double referenceThreshold = 0.7;

        /**
         * 最大存储条目数
         */
        private int maxEntries = 10000;
    }

    /**
     * 高频层配置 - 实时上下文
     * (High-frequency Layer - Real-time Context)
     */
    @Data
    public static class HighFrequencyConfig {
        /**
         * 存储方式: memory / redis
         */
        private String storage = "memory";

        /**
         * 会话超时（分钟）
         */
        private int sessionTimeoutMinutes = 30;

        /**
         * 最大会话数
         */
        private int maxSessions = 1000;

        /**
         * 每个会话最大历史记录数
         */
        private int maxHistoryPerSession = 20;
    }

    /**
     * 响应策略配置
     * (Response Strategy Config)
     */
    @Data
    public static class StrategyConfig {
        /**
         * 直接回答的置信度阈值
         */
        private double directAnswerConfidence = 0.9;

        /**
         * 是否启用技能模板
         */
        private boolean enableSkillTemplates = true;

        /**
         * 是否启用问题分类
         */
        private boolean enableQuestionClassification = true;
    }
}

