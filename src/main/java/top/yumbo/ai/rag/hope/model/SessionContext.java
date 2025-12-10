package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文 - 存储在高频层的实时会话信息
 * (Session Context - Real-time session info stored in high-frequency layer)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {

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
    @Builder.Default
    private List<ConversationTurn> history = new ArrayList<>();

    /**
     * 临时定义/更正
     * (Temporary definitions/corrections)
     */
    @Builder.Default
    private List<TempDefinition> tempDefinitions = new ArrayList<>();

    /**
     * 用户偏好（本会话）
     * (User preference for this session)
     */
    private UserPreference preference;

    /**
     * 创建时间
     * (Created time)
     */
    private LocalDateTime createdAt;

    /**
     * 最后活跃时间
     * (Last active time)
     */
    private LocalDateTime lastActiveAt;

    /**
     * 对话轮次
     * (Conversation turn)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationTurn {
        /**
         * 角色 user / assistant
         * (Role: user / assistant)
         */
        private String role;
        /**
         * 内容
         * (Content)
         */
        private String content;
        /**
         * 时间戳
         * (Timestamp)
         */
        private LocalDateTime timestamp;
        /**
         * 该轮对话的话题（可选）
         * (Topic for this conversation turn (optional))
         */
        private String topic;
    }

    /**
     * 临时定义
     * (Temporary definition)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempDefinition {
        /**
         * 术语
         * (Term)
         */
        private String term;
        /**
         * 定义
         * (Definition)
         */
        private String definition;
        /**
         * 创建时间
         * (Creation time)
         */
        private LocalDateTime createdAt;
        /**
         * 仅本会话有效
         * (Valid for this session only)
         */
        private boolean sessionOnly;
    }

    /**
     * 用户偏好
     * (User preference)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreference {
        /**
         * 响应风格（详细/简洁）
         * (Response style: detailed/concise)
         */
        private String responseStyle;
        /**
         * 首选语言（中文/英文）
         * (Preferred language: Chinese/English)
         */
        private String preferredLanguage;
        /**
         * 常见话题
         * (Common topics)
         */
        private List<String> commonTopics;
    }

    /**
     * 添加对话轮次
     * (Add conversation turn)
     * 
     * @param role 角色（user/assistant） (Role: user/assistant)
     * @param content 内容 (Content)
     */
    public void addTurn(String role, String content) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(ConversationTurn.builder()
            .role(role)
            .content(content)
            .timestamp(LocalDateTime.now())
            .topic(currentTopic)
            .build());
        lastActiveAt = LocalDateTime.now();
    }

    /**
     * 添加临时定义
     * (Add temporary definition)
     * 
     * @param term 术语 (Term)
     * @param definition 定义 (Definition)
     */
    public void addTempDefinition(String term, String definition) {
        if (tempDefinitions == null) {
            tempDefinitions = new ArrayList<>();
        }
        tempDefinitions.add(TempDefinition.builder()
            .term(term)
            .definition(definition)
            .createdAt(LocalDateTime.now())
            .sessionOnly(true)
            .build());
    }

    /**
     * 获取最近 N 轮对话
     * (Get recent N conversation turns)
     * 
     * @param n 轮次数 (Number of turns)
     * @return 最近的对话列表 (List of recent conversations)
     */
    public List<ConversationTurn> getRecentHistory(int n) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        int start = Math.max(0, history.size() - n);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    /**
     * 构建对话摘要
     */
    public String buildContextSummary() {
        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();

        // 添加当前话题
        if (currentTopic != null && !currentTopic.isEmpty()) {
            summary.append(I18N.get("model.current_topic_label")).append(currentTopic).append("\n\n");
        }

        // 添加临时定义
        if (tempDefinitions != null && !tempDefinitions.isEmpty()) {
            summary.append(I18N.get("model.session_definitions_label"));
            for (TempDefinition def : tempDefinitions) {
                summary.append("- ").append(def.getTerm())
                       .append(": ").append(def.getDefinition()).append("\n");
            }
            summary.append("\n");
        }

        // 添加最近对话
        List<ConversationTurn> recent = getRecentHistory(5);
        if (!recent.isEmpty()) {
            summary.append(I18N.get("model.recent_conversations_label"));
            for (ConversationTurn turn : recent) {
                String roleLabel = "user".equals(turn.getRole()) ? 
                    I18N.get("model.user_label") : I18N.get("model.assistant_label");
                // 截断过长的内容
                String content = turn.getContent();
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                summary.append(roleLabel).append(": ").append(content).append("\n");
            }
        }

        return summary.toString();
    }

    /**
     * 检查是否过期
     * (Check if expired)
     * 
     * @param timeoutMinutes 超时分钟数 (Timeout minutes)
     * @return 是否过期 (Whether expired)
     */
    public boolean isExpired(int timeoutMinutes) {
        if (lastActiveAt == null) {
            return true;
        }
        return lastActiveAt.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * 获取对话轮数
     * (Get conversation turn count)
     * 
     * @return 对话轮数 (Conversation turn count)
     */
    public int getTurnCount() {
        return history != null ? history.size() : 0;
    }
}

