package top.yumbo.ai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 反馈系统配置 (Feedback system configuration)
 *
 * 控制反馈是否需要审核以及反馈如何影响检索相关性 (Controls whether feedback requires approval and how feedback affects search relevance)
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "feedback")
public class FeedbackConfig {

    /**
     * 是否需要审核才能生效 (Whether approval is required to take effect)
     * 默认 false - 用户反馈直接生效 (Default false - user feedback takes effect directly)
     */
    private boolean requireApproval = false;

    /**
     * 自动应用反馈到相关性优化 (Automatically apply feedback to relevance optimization)
     * 默认 true - 反馈直接影响文档权重 (Default true - feedback directly affects document weight)
     */
    private boolean autoApply = true;

    /**
     * 点赞权重增量 (Like weight increment)
     * 默认 0.1 - 每次点赞增加 0.1 权重 (Default 0.1 - each like increases weight by 0.1)
     */
    private double likeWeightIncrement = 0.1;

    /**
     * 踩的权重减量 (Dislike weight decrement)
     * 默认 -0.15 - 每次踩减少 0.15 权重 (Default -0.15 - each dislike decreases weight by 0.15)
     */
    private double dislikeWeightDecrement = -0.15;

    /**
     * 最小权重限制 (Minimum weight limit)
     * 默认 0.1 - 即使被踩很多次，最低权重不低于 0.1 (Default 0.1 - minimum weight not below 0.1 even if disliked many times)
     */
    private double minWeight = 0.1;

    /**
     * 最大权重限制 (Maximum weight limit)
     * 默认 2.0 - 即使被点赞很多次，最高权重不超过 2.0 (Default 2.0 - maximum weight not exceeding 2.0 even if liked many times)
     */
    private double maxWeight = 2.0;

    /**
     * 是否启用动态权重调整 (Whether to enable dynamic weighting)
     * 默认 true (Default true)
     */
    private boolean enableDynamicWeighting = true;
}
