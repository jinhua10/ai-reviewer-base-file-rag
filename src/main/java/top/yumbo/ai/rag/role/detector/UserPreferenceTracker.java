package top.yumbo.ai.rag.role.detector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户偏好追踪器 (User Preference Tracker)
 *
 * 追踪和学习用户的角色偏好
 * (Tracks and learns user's role preferences)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class UserPreferenceTracker {

    /**
     * 用户历史记录 (User history)
     * Key: userId, Value: UserPreference
     */
    private final Map<String, UserPreference> userPreferences = new ConcurrentHashMap<>();

    /**
     * 最大历史记录数 (Max history size)
     */
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * 记录用户选择 (Record user choice)
     *
     * @param userId 用户ID (User ID)
     * @param roleId 角色ID (Role ID)
     * @param question 问题 (Question)
     */
    public void recordChoice(String userId, String roleId, String question) {
        if (userId == null || roleId == null) {
            return;
        }

        UserPreference preference = userPreferences.computeIfAbsent(
                userId, k -> new UserPreference(userId));

        preference.addHistory(roleId, question);

        log.debug(I18N.get("detector.preference.recorded", userId, roleId));
    }

    /**
     * 获取用户偏好角色 (Get user preferred roles)
     *
     * @param userId 用户ID (User ID)
     * @return 角色偏好列表 (Role preference list)
     */
    public List<RolePreference> getUserPreferences(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        UserPreference preference = userPreferences.get(userId);
        if (preference == null) {
            return Collections.emptyList();
        }

        return preference.getTopRoles(5);
    }

    /**
     * 预测用户偏好角色 (Predict user preferred role)
     *
     * @param userId 用户ID (User ID)
     * @param question 当前问题 (Current question)
     * @return 预测的角色匹配结果 (Predicted role match result)
     */
    public Optional<RoleMatchResult> predictRole(String userId, String question) {
        List<RolePreference> preferences = getUserPreferences(userId);

        if (preferences.isEmpty()) {
            log.debug(I18N.get("detector.preference.no.history", userId));
            return Optional.empty();
        }

        // 获取最常用的角色 (Get most frequently used role)
        RolePreference topPreference = preferences.getFirst();

        // 计算置信度 (Calculate confidence)
        double confidence = calculateConfidence(topPreference.getCount(),
                                               preferences.stream()
                                                       .mapToInt(RolePreference::getCount)
                                                       .sum());

        RoleMatchResult result = RoleMatchResult.builder()
                .roleId(topPreference.getRoleId())
                .score(topPreference.getCount())
                .confidence(confidence)
                .method("preference")
                .reason("基于用户历史偏好 (Based on user history preference)")
                .extraInfo(String.format("历史使用次数: %d, 当前问题: %s (Historical usage: %d, Current question: %s)",
                                        topPreference.getCount(), question, topPreference.getCount(), question))
                .build();

        log.info(I18N.get("detector.preference.predicted", userId, topPreference.getRoleId()));
        return Optional.of(result);
    }

    /**
     * 计算置信度 (Calculate confidence)
     *
     * @param roleCount 角色使用次数 (Role usage count)
     * @param totalCount 总使用次数 (Total usage count)
     * @return 置信度 (Confidence)
     */
    private double calculateConfidence(int roleCount, int totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }

        double ratio = (double) roleCount / totalCount;

        // 考虑历史记录数量 (Consider history count)
        double historyFactor = Math.min(1.0, totalCount / 20.0);

        return ratio * historyFactor;
    }

    /**
     * 获取统计信息 (Get statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userPreferences.size());
        stats.put("totalRecords", userPreferences.values().stream()
                .mapToInt(p -> p.getHistory().size())
                .sum());

        return stats;
    }

    // ==================== 内部类 (Inner Classes) ====================

    /**
     * 用户偏好 (User Preference)
     */
    private static class UserPreference {
        private final List<HistoryRecord> history = new ArrayList<>();
        private final Map<String, Integer> roleCount = new HashMap<>();

        @SuppressWarnings("unused")
        public UserPreference(String userId) {
            // userId 用于构造函数，但不需要存储
        }

        @SuppressWarnings("unused")
        public void addHistory(String roleId, String question) {
            // 添加历史记录 (Add history record)
            history.add(new HistoryRecord(roleId));

            // 更新计数 (Update count)
            roleCount.merge(roleId, 1, Integer::sum);

            // 限制历史记录大小 (Limit history size)
            if (history.size() > MAX_HISTORY_SIZE) {
                HistoryRecord removed = history.removeFirst();
                // 减少计数 (Decrease count)
                roleCount.computeIfPresent(removed.roleId, (k, v) -> v > 1 ? v - 1 : null);
            }
        }

        public List<HistoryRecord> getHistory() {
            return new ArrayList<>(history);
        }

        public List<RolePreference> getTopRoles(int limit) {
            return roleCount.entrySet().stream()
                    .map(e -> new RolePreference(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparingInt(RolePreference::getCount).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * 历史记录 (History Record)
     */
    private static class HistoryRecord {
        private final String roleId;

        public HistoryRecord(String roleId) {
            this.roleId = roleId;
        }
    }

    /**
     * 角色偏好 (Role Preference)
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RolePreference {
        private String roleId;
        private int count;
    }
}

