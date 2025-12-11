package top.yumbo.ai.rag.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 反馈实体 (Feedback Entity)
 *
 * 记录用户对问答结果的反馈信息
 * (Records user feedback on QA results)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    /**
     * 反馈ID (Feedback ID)
     */
    private String id;

    /**
     * 问答会话ID (QA session ID)
     */
    private String sessionId;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 问题 (Question)
     */
    private String question;

    /**
     * 答案 (Answer)
     */
    private String answer;

    /**
     * 反馈类型 (Feedback type)
     * explicit: 显式反馈 (thumbs up/down)
     * implicit: 隐式反馈 (behavior)
     */
    private FeedbackType type;

    /**
     * 反馈来源 (Feedback source)
     * user: 用户主动
     * system: 系统推断
     */
    private FeedbackSource source;

    /**
     * 反馈值 (Feedback value)
     * 显式: 1(好), -1(差), 0(中立)
     * 隐式: 0.0-1.0 (置信度)
     */
    private double value;

    /**
     * 反馈标签 (Feedback tags)
     * helpful, accurate, irrelevant, outdated, etc.
     */
    private String[] tags;

    /**
     * 反馈评论 (Feedback comment)
     */
    private String comment;

    /**
     * 行为数据 (Behavior data)
     * 隐式反馈的行为信号
     */
    @Builder.Default
    private Map<String, Object> behaviorData = new HashMap<>();

    /**
     * 创建时间 (Create time)
     */
    @Builder.Default
    private Date createTime = new Date();

    /**
     * 处理状态 (Processing status)
     */
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.PENDING;

    /**
     * 处理时间 (Processing time)
     */
    private Date processedTime;

    /**
     * 是否正面反馈 (Is positive feedback)
     *
     * @return 是否正面 (Whether positive)
     */
    public boolean isPositive() {
        return value > 0;
    }

    /**
     * 是否负面反馈 (Is negative feedback)
     *
     * @return 是否负面 (Whether negative)
     */
    public boolean isNegative() {
        return value < 0;
    }

    /**
     * 是否显式反馈 (Is explicit feedback)
     *
     * @return 是否显式 (Whether explicit)
     */
    public boolean isExplicit() {
        return type == FeedbackType.EXPLICIT;
    }

    /**
     * 是否隐式反馈 (Is implicit feedback)
     *
     * @return 是否隐式 (Whether implicit)
     */
    public boolean isImplicit() {
        return type == FeedbackType.IMPLICIT;
    }

    /**
     * 添加行为数据 (Add behavior data)
     *
     * @param key 键 (Key)
     * @param value 值 (Value)
     */
    public void addBehaviorData(String key, Object value) {
        if (behaviorData == null) {
            behaviorData = new HashMap<>();
        }
        behaviorData.put(key, value);
    }

    /**
     * 反馈类型 (Feedback Type)
     */
    public enum FeedbackType {
        /**
         * 显式反馈 (Explicit feedback)
         * 用户主动点击按钮
         */
        EXPLICIT,

        /**
         * 隐式反馈 (Implicit feedback)
         * 系统根据行为推断
         */
        IMPLICIT
    }

    /**
     * 反馈来源 (Feedback Source)
     */
    public enum FeedbackSource {
        /**
         * 用户主动 (User initiated)
         */
        USER,

        /**
         * 系统推断 (System inferred)
         */
        SYSTEM
    }

    /**
     * 处理状态 (Processing Status)
     */
    public enum ProcessingStatus {
        /**
         * 待处理 (Pending)
         */
        PENDING,

        /**
         * 处理中 (Processing)
         */
        PROCESSING,

        /**
         * 已处理 (Processed)
         */
        PROCESSED,

        /**
         * 处理失败 (Failed)
         */
        FAILED
    }
}

