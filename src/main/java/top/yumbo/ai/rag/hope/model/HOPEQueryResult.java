package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * HOPE 查询结果
 * (HOPE Query Result)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HOPEQueryResult {

    /**
     * 直接回答（如果有）
     */
    private String answer;

    /**
     * 来源层: permanent / ordinary / high_frequency
     */
    private String sourceLayer;

    /**
     * 置信度 (0-1)
     */
    private double confidence;

    /**
     * 是否需要调用 LLM
     */
    private boolean needsLLM;

    /**
     * 匹配的技能模板（如果有）
     */
    private SkillTemplate skillTemplate;

    /**
     * 匹配的确定性知识（如果有）
     */
    private FactualKnowledge factualKnowledge;

    /**
     * 增强上下文列表
     */
    @Builder.Default
    private List<String> contexts = new ArrayList<>();

    /**
     * 相似问答（来自中频层）
     */
    @Builder.Default
    private List<SimilarQA> similarQAs = new ArrayList<>();

    /**
     * 会话上下文（来自高频层）
     */
    private SessionContext sessionContext;

    /**
     * 处理耗时（毫秒）
     */
    private long processingTimeMs;

    /**
     * 可以直接回答
     */
    public boolean canDirectAnswer() {
        return !needsLLM && answer != null && !answer.isEmpty() && confidence >= 0.9;
    }

    /**
     * 有技能模板可用
     */
    public boolean hasSkillTemplate() {
        return skillTemplate != null && skillTemplate.isEnabled();
    }

    /**
     * 有相似问答参考
     */
    public boolean hasSimilarReference() {
        return similarQAs != null && !similarQAs.isEmpty();
    }

    /**
     * 相似问答数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarQA {
        private String question;
        private String answer;
        private double similarity;
        private int rating;
    }

    /**
     * 会话上下文数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionContext {
        private String sessionId;
        private String currentTopic;
        private List<ConversationTurn> history;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConversationTurn {
            private String role;
            private String content;
        }
    }
}

