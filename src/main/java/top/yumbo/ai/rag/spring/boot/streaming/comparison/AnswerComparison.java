package top.yumbo.ai.rag.spring.boot.streaming.comparison;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 答案对比记录
 * (Answer Comparison Record)
 *
 * 用于记录 HOPE 答案和 LLM 答案的对比结果
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Data
@Builder
public class AnswerComparison {

    /**
     * 对比记录ID
     */
    private String comparisonId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 原始问题
     */
    private String question;

    /**
     * HOPE 答案
     */
    private String hopeAnswer;

    /**
     * HOPE 答案置信度
     */
    private Double hopeConfidence;

    /**
     * HOPE 答案来源
     */
    private String hopeSource;

    /**
     * LLM 答案
     */
    private String llmAnswer;

    /**
     * 用户选择（hope/llm/both/neither）
     */
    private UserChoice userChoice;

    /**
     * 用户评论
     */
    private String userComment;

    /**
     * 差异分析结果
     */
    private String differenceAnalysis;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 反馈时间
     */
    private LocalDateTime feedbackAt;

    /**
     * 是否已处理（触发投票等）
     */
    private boolean processed;

    /**
     * 用户选择枚举
     */
    public enum UserChoice {
        HOPE_BETTER("HOPE答案更好"),
        LLM_BETTER("LLM答案更好"),
        BOTH_GOOD("两者都好"),
        NEITHER_GOOD("都不够好");

        private final String description;

        UserChoice(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取选择的答案
     */
    public String getChosenAnswer() {
        if (userChoice == null) {
            return null;
        }

        switch (userChoice) {
            case HOPE_BETTER:
                return hopeAnswer;
            case LLM_BETTER:
                return llmAnswer;
            case BOTH_GOOD:
                return hopeAnswer + "\n\n" + llmAnswer;
            default:
                return null;
        }
    }

    /**
     * 是否需要差异分析
     */
    public boolean needsDifferenceAnalysis() {
        return userChoice == UserChoice.HOPE_BETTER
            || userChoice == UserChoice.LLM_BETTER
            || userChoice == UserChoice.NEITHER_GOOD;
    }
}

