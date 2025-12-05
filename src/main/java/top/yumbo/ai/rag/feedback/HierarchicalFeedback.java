package top.yumbo.ai.rag.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分层反馈记录（Hierarchical Feedback Record）
 *
 * 支持文档级、段落级、句子级的精细反馈
 *
 * 📈 优化说明（2025-12-05）：
 * 分层反馈机制可减少 2-3 次反馈交互
 * 详见: md/20251205140000-RAG系统收敛性分析.md
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchicalFeedback {

    /**
     * 反馈ID
     */
    private String id;

    /**
     * 关联的问答记录ID
     */
    private String qaRecordId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 反馈层级
     */
    private FeedbackLevel level;

    /**
     * 文档级反馈
     */
    private DocumentLevelFeedback documentFeedback;

    /**
     * 段落级反馈列表
     */
    private List<ParagraphFeedback> paragraphFeedbacks;

    /**
     * 句子级反馈列表（高亮标记）
     */
    private List<SentenceFeedback> sentenceFeedbacks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 反馈层级枚举
     */
    public enum FeedbackLevel {
        DOCUMENT,   // 文档级（粗粒度）
        PARAGRAPH,  // 段落级（中粒度）
        SENTENCE    // 句子级（细粒度）
    }

    /**
     * 文档级反馈
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentLevelFeedback {
        /**
         * 评分 1-5
         */
        private Integer rating;

        /**
         * 相关性评估
         */
        private RelevanceLevel relevance;

        /**
         * 反馈评论
         */
        private String comment;

        /**
         * 建议标签
         */
        private List<String> tags;
    }

    /**
     * 段落级反馈
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphFeedback {
        /**
         * 段落索引（从0开始）
         */
        private int paragraphIndex;

        /**
         * 段落内容摘要（前100字）
         */
        private String contentPreview;

        /**
         * 段落起始字符位置
         */
        private int startOffset;

        /**
         * 段落结束字符位置
         */
        private int endOffset;

        /**
         * 是否有帮助
         */
        private boolean helpful;

        /**
         * 相关性评分 1-5
         */
        private Integer relevanceScore;

        /**
         * 反馈类型
         */
        private ParagraphFeedbackType feedbackType;

        /**
         * 用户评论
         */
        private String comment;
    }

    /**
     * 句子级反馈（高亮标记）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentenceFeedback {
        /**
         * 句子索引
         */
        private int sentenceIndex;

        /**
         * 句子内容
         */
        private String content;

        /**
         * 起始字符位置
         */
        private int startOffset;

        /**
         * 结束字符位置
         */
        private int endOffset;

        /**
         * 高亮类型
         */
        private HighlightType highlightType;

        /**
         * 用户标注
         */
        private String annotation;

        /**
         * 是否是关键信息
         */
        private boolean keyInformation;
    }

    /**
     * 相关性级别
     */
    public enum RelevanceLevel {
        HIGHLY_RELEVANT,    // 高度相关
        RELEVANT,           // 相关
        PARTIALLY_RELEVANT, // 部分相关
        NOT_RELEVANT,       // 不相关
        MISLEADING          // 误导性
    }

    /**
     * 段落反馈类型
     */
    public enum ParagraphFeedbackType {
        KEY_POINT,          // 关键要点
        SUPPORTING_DETAIL,  // 支撑细节
        BACKGROUND,         // 背景信息
        IRRELEVANT,         // 不相关
        WRONG_INFO,         // 错误信息
        OUTDATED            // 过时信息
    }

    /**
     * 高亮类型
     */
    public enum HighlightType {
        ANSWER,             // 直接答案
        KEY_FACT,           // 关键事实
        IMPORTANT,          // 重要信息
        EXAMPLE,            // 示例
        DEFINITION,         // 定义
        WRONG,              // 错误
        UNCERTAIN           // 不确定
    }
}

