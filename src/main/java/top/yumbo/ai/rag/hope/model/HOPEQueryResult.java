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
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HOPEQueryResult {

    /**
     * 直接回答（如果有）
     * (Direct answer if available)
     */
    private String answer;

    /**
     * 来源层: permanent / ordinary / high_frequency
     * (Source layer: permanent / ordinary / high_frequency)
     */
    private String sourceLayer;

    /**
     * 置信度 (0-1)
     * (Confidence score (0-1))
     */
    private double confidence;

    /**
     * 是否需要调用 LLM
     * (Whether LLM call is needed)
     */
    private boolean needsLLM;

    /**
     * 匹配的技能模板（如果有）
     * (Matched skill template if available)
     */
    private SkillTemplate skillTemplate;

    /**
     * 匹配的确定性知识（如果有）
     * (Matched factual knowledge if available)
     */
    private FactualKnowledge factualKnowledge;

    /**
     * 增强上下文列表
     * (Enhanced context list)
     */
    @Builder.Default
    private List<String> contexts = new ArrayList<>();

    /**
     * 相似问答（来自中频层）
     * (Similar Q&A from ordinary layer)
     */
    @Builder.Default
    private List<SimilarQA> similarQAs = new ArrayList<>();

    /**
     * 会话上下文（来自高频层）
     * (Session context from high-frequency layer)
     */
    private SessionContext sessionContext;

    /**
     * 处理耗时（毫秒）
     * (Processing time in milliseconds)
     */
    private long processingTimeMs;

    /**
     * 可以直接回答
     * (Can answer directly)
     * 
     * @return 是否可以直接回答 (Whether can answer directly)
     */
    public boolean canDirectAnswer() {
        return !needsLLM && answer != null && !answer.isEmpty() && confidence >= 0.9;
    }

    /**
     * 有技能模板可用
     * (Has skill template available)
     * 
     * @return 是否有技能模板可用 (Whether has skill template available)
     */
    public boolean hasSkillTemplate() {
        return skillTemplate != null && skillTemplate.isEnabled();
    }

    /**
     * 有相似问答参考
     * (Has similar Q&A for reference)
     * 
     * @return 是否有相似问答参考 (Whether has similar Q&A for reference)
     */
    public boolean hasSimilarReference() {
        return similarQAs != null && !similarQAs.isEmpty();
    }

    /**
     * 相似问答数据
     * (Similar Q&A data)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarQA {
        /**
         * 问题
         * (Question)
         */
        private String question;
        
        /**
         * 答案
         * (Answer)
         */
        private String answer;
        
        /**
         * 相似度
         * (Similarity)
         */
        private double similarity;
        
        /**
         * 评分
         * (Rating)
         */
        private int rating;
    }

    /**
     * 会话上下文数据
     * (Session context data)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionContext {
        /**
         * 会话ID
         * (Session ID)
         */
        private String sessionId;
        
        /**
         * 当前话题
         * (Current topic)
         */
        private String currentTopic;
        
        /**
         * 对话历史
         * (Conversation history)
         */
        private List<ConversationTurn> history;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConversationTurn {
            /**
             * 角色
             * (Role)
             */
            private String role;
            
            /**
             * 内容
             * (Content)
             */
            private String content;
        }
    }
}

