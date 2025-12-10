package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 问答归档配置（QA archive configuration）
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge.qa.qa-archive")
public class QAArchiveProperties {

    /**
     * 是否启用归档 (Whether to enable archiving)
     * 控制是否启用问答归档功能
     * (Controls whether to enable Q&A archiving functionality)
     */
    private boolean enabled = true;

    /**
     * 归档策略 (Archive strategy)
     * - auto: 自动归档所有符合条件的问答 (Auto: Archive all Q&A that meet conditions)
     * - feedback-based: 基于用户反馈归档（推荐）(Feedback-based: Archive based on user feedback (recommended))
     * - manual: 手动审核后归档 (Manual: Archive after manual review)
     */
    private String strategy = "feedback-based";

    /**
     * 最低评分阈值（feedback-based 模式）(Minimum rating threshold (feedback-based mode))
     * 归档所需的最低评分
     * (Minimum rating required for archiving)
     */
    private int minRating = 4;

    /**
     * 问题最短长度（字符数）(Minimum question length (character count))
     * 问题文本的最小长度限制
     * (Minimum length limit for question text)
     */
    private int minQuestionLength = 5;

    /**
     * 回答最短长度（字符数）(Minimum answer length (character count))
     * 回答文本的最小长度限制
     * (Minimum length limit for answer text)
     */
    private int minAnswerLength = 20;

    /**
     * 归档路径 (Archive path)
     * 存储备档问答的目录路径
     * (Directory path for storing archived Q&A)
     */
    private String archivePath = "./data/rag";

    /**
     * 是否自动索引归档的文档 (Whether to automatically index archived documents)
     * 控制是否自动为归档文档创建索引
     * (Controls whether to automatically create indexes for archived documents)
     */
    private boolean autoIndex = true;

    /**
     * 临时文档过期天数（自动清理）(Temporary document expiry days (auto-cleanup))
     * 临时文档自动清理前的天数
     * (Number of days before temporary documents are automatically cleaned up)
     */
    private int tempDocumentExpiryDays = 30;
}
