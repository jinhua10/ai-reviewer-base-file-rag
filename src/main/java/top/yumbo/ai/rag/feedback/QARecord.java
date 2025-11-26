package top.yumbo.ai.rag.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答记录
 * 记录每次用户提问的详细信息
 *
 * @author AI Reviewer Team
 * @since 2025-11-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QARecord {

    /**
     * 记录ID（唯一标识）
     */
    private String id;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI 回答
     */
    private String answer;

    /**
     * 提问时间
     */
    private LocalDateTime timestamp;

    /**
     * 检索命中的文档列表（所有检索到的文档）
     */
    private List<String> retrievedDocuments;

    /**
     * 实际使用的文档列表（纳入回答的文档）
     */
    private List<String> usedDocuments;

    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;

    /**
     * 整体反馈评分
     * null: 未反馈
     * 1: 差
     * 2: 一般
     * 3: 好
     * 4: 很好
     * 5: 优秀
     */
    private Integer overallRating;

    /**
     * 整体反馈内容
     */
    private String overallFeedback;

    /**
     * 文档反馈列表
     */
    private List<DocumentFeedback> documentFeedbacks;

    /**
     * 是否已应用到相关性优化
     */
    private boolean appliedToOptimization;

    /**
     * 管理员审核状态
     * PENDING: 待审核
     * APPROVED: 已批准
     * REJECTED: 已拒绝
     */
    private ReviewStatus reviewStatus;

    /**
     * 单个文档的反馈
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentFeedback {
        /**
         * 文档名称
         */
        private String documentName;

        /**
         * 反馈类型
         * LIKE: 点赞（这个文档很有帮助）
         * DISLIKE: 踩（这个文档没有帮助/不相关）
         */
        private FeedbackType feedbackType;

        /**
         * 反馈原因（可选）
         */
        private String reason;

        /**
         * 反馈时间
         */
        private LocalDateTime feedbackTime;
    }

    /**
     * 反馈类型
     */
    public enum FeedbackType {
        LIKE,      // 点赞
        DISLIKE    // 踩
    }

    /**
     * 审核状态
     */
    public enum ReviewStatus {
        PENDING,   // 待审核
        APPROVED,  // 已批准
        REJECTED   // 已拒绝
    }
}

