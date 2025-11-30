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
     * 是否启用归档
     */
    private boolean enabled = true;

    /**
     * 归档策略
     * - auto: 自动归档所有符合条件的问答
     * - feedback-based: 基于用户反馈归档（推荐）
     * - manual: 手动审核后归档
     */
    private String strategy = "feedback-based";

    /**
     * 最低评分阈值（feedback-based 模式）
     */
    private int minRating = 4;

    /**
     * 问题最短长度（字符数）
     */
    private int minQuestionLength = 5;

    /**
     * 回答最短长度（字符数）
     */
    private int minAnswerLength = 20;

    /**
     * 归档路径
     */
    private String archivePath = "./data/rag";

    /**
     * 是否自动索引归档的文档
     */
    private boolean autoIndex = true;

    /**
     * 临时文档过期天数（自动清理）
     */
    private int tempDocumentExpiryDays = 30;
}
