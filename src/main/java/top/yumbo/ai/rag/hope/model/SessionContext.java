package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文 - 存储在高频层的实时会话信息
 * (Session Context - Real-time session info stored in high-frequency layer)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
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
        /** 角色 user / assistant (Role: user / assistant) */
        private String role;
        /** 内容 (Content) */
        private String content;
        private LocalDateTime timestamp;
        private String topic;     // 该轮对话的话题（可选）
    }

    /**
     * 临时定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempDefinition {
        private String term;
        private String definition;
        private LocalDateTime createdAt;
        private boolean sessionOnly;  // 仅本会话有效
    }

    /**
     * 用户偏好
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPreference {
        private String responseStyle;     // 详细/简洁
        private String preferredLanguage; // 中文/英文
        private List<String> commonTopics;
    }

    /**
     * 添加对话轮次
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
            summary.append("当前话题: ").append(currentTopic).append("\n\n");
        }

        // 添加临时定义
        if (tempDefinitions != null && !tempDefinitions.isEmpty()) {
            summary.append("本次会话的定义:\n");
            for (TempDefinition def : tempDefinitions) {
                summary.append("- ").append(def.getTerm())
                       .append(": ").append(def.getDefinition()).append("\n");
            }
            summary.append("\n");
        }

        // 添加最近对话
        List<ConversationTurn> recent = getRecentHistory(5);
        if (!recent.isEmpty()) {
            summary.append("最近对话:\n");
            for (ConversationTurn turn : recent) {
                String roleLabel = "user".equals(turn.getRole()) ? "用户" : "助手";
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
     */
    public boolean isExpired(int timeoutMinutes) {
        if (lastActiveAt == null) {
            return true;
        }
        return lastActiveAt.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * 获取对话轮数
     */
    public int getTurnCount() {
        return history != null ? history.size() : 0;
    }
}

