package top.yumbo.ai.rag.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答记录（QA Record）
 * 记录每次用户提问的详细信息（Records detailed information for each user question）
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
     * 记录ID（唯一标识）（Record ID (unique identifier)）
     */
    private String id;

    /**
     * 用户问题（User question）
     */
    private String question;

    /**
     * AI 回答（AI answer）
     */
    private String answer;

    /**
     * 提问时间（Question time）
     */
    private LocalDateTime timestamp;

    /**
     * 检索命中的文档列表（所有检索到的文档）（List of retrieved documents (all documents retrieved)）
     */
    private List<String> retrievedDocuments;

    /**
     * 实际使用的文档列表（纳入回答的文档）（List of documents actually used (documents included in the answer)）
     */
    private List<String> usedDocuments;

    /**
     * 响应时间（毫秒）（Response time (milliseconds)）
     */
    private long responseTimeMs;

    /**
     * 整体反馈评分（Overall feedback rating）
     * null: 未反馈（not rated）
     * 1: 差（poor）
     * 2: 一般（fair）
     * 3: 好（good）
     * 4: 很好（very good）
     * 5: 优秀（excellent）
     */
    private Integer overallRating;

    /**
     * 整体反馈内容（Overall feedback content）
     */
    private String overallFeedback;

    /**
     * 文档反馈列表（List of document feedbacks）
     */
    private List<DocumentFeedback> documentFeedbacks;

    /**
     * 是否已应用到相关性优化（Whether applied to relevance optimization）
     */
    private boolean appliedToOptimization;

    /**
     * 管理员审核状态（Admin review status）
     * PENDING: 待审核（pending review）
     * APPROVED: 已批准（approved）
     * REJECTED: 已拒绝（rejected）
     */
    private ReviewStatus reviewStatus;

    /**
     * 单个文档的反馈（Feedback for a single document）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentFeedback {
        /**
         * 文档名称（Document name）
         */
        private String documentName;

        /**
         * 反馈类型（Feedback type）
         * LIKE: 点赞（这个文档很有帮助）（like (this document is helpful)）
         * DISLIKE: 踩（这个文档没有帮助/不相关）（dislike (this document is not helpful/irrelevant)）
         */
        private FeedbackType feedbackType;

        /**
         * 反馈原因（可选）（Feedback reason (optional)）
         */
        private String reason;

        /**
         * 反馈时间（Feedback time）
         */
        private LocalDateTime feedbackTime;
    }

    /**
     * 反馈类型（Feedback type）
     */
    public enum FeedbackType {
        LIKE,      // 点赞（Like）
        NEUTRAL,   // 中性（3星评价）（Neutral (3-star rating)）
        DISLIKE    // 踩（Dislike）
    }

    /**
     * 审核状态（Review status）
     */
    public enum ReviewStatus {
        PENDING,   // 待审核（Pending review）
        APPROVED,  // 已批准（Approved）
        REJECTED   // 已拒绝（Rejected）
    }
}
