package top.yumbo.ai.rag.loader;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.Role;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 预加载策略 (Preload Strategy)
 *
 * 决定哪些索引应该被预加载到内存
 * (Determines which indices should be preloaded into memory)
 *
 * 策略 (Strategies):
 * 1. 热门角色优先 (Popular roles first)
 * 2. 最近使用优先 (Recently used first)
 * 3. 高优先级优先 (High priority first)
 * 4. 配置指定预加载 (Configuration specified preload)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
public class PreloadStrategy {

    /**
     * 预加载配置 (Preload configuration)
     */
    private final PreloadConfig config;

    /**
     * 角色使用统计 (Role usage statistics)
     *
     * 使用 ConcurrentHashMap 保证线程安全，因为 recordUsage() 可能在多个线程中被调用
     * (Using ConcurrentHashMap to ensure thread safety, as recordUsage() may be called from multiple threads)
     */
    private final Map<String, RoleUsageStats> usageStats = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 构造函数 (Constructor)
     *
     * @param config 预加载配置 (Preload configuration)
     */
    public PreloadStrategy(PreloadConfig config) {
        this.config = config;
        log.info(I18N.get("loader.preload.strategy_created", config.getMaxPreloadCount()));
    }

    /**
     * 决定应该预加载哪些角色 (Decide which roles should be preloaded)
     *
     * @param allRoles 所有角色 (All roles)
     * @return 应该预加载的角色列表 (List of roles to preload)
     */
    public List<Role> decidePreloadRoles(List<Role> allRoles) {
        if (allRoles == null || allRoles.isEmpty()) {
            log.warn(I18N.get("loader.preload.no_roles"));
            return Collections.emptyList();
        }

        log.info(I18N.get("loader.preload.deciding", allRoles.size()));

        // 1. 过滤已禁用的角色 (Filter disabled roles)
        List<Role> enabledRoles = allRoles.stream()
                .filter(Role::isEnabled)
                .collect(Collectors.toList());

        if (enabledRoles.isEmpty()) {
            log.warn(I18N.get("loader.preload.no_enabled_roles"));
            return Collections.emptyList();
        }

        // 2. 按策略评分 (Score by strategy)
        Map<Role, Double> scores = new HashMap<>();
        for (Role role : enabledRoles) {
            double score = calculateScore(role);
            scores.put(role, score);
        }

        // 3. 排序并选择前N个 (Sort and select top N)
        List<Role> sortedRoles = scores.entrySet().stream()
                .sorted(Map.Entry.<Role, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(config.getMaxPreloadCount())
                .collect(Collectors.toList());

        log.info(I18N.get("loader.preload.decided", sortedRoles.size(),
                sortedRoles.stream().map(Role::getName).collect(Collectors.joining(", "))));

        return sortedRoles;
    }

    /**
     * 计算角色评分 (Calculate role score)
     *
     * @param role 角色 (Role)
     * @return 评分 (Score)
     */
    private double calculateScore(Role role) {
        double score = 0.0;

        // 1. 基础分：优先级 (Base score: priority)
        score += role.getPriority() * config.getPriorityWeight();

        // 2. 使用频率分 (Usage frequency score)
        RoleUsageStats stats = usageStats.get(role.getId());
        if (stats != null) {
            score += stats.getUsageCount() * config.getUsageWeight();
        }

        // 3. 最近使用分 (Recently used score)
        if (stats != null && stats.getLastUsedTime() != null) {
            long hoursSinceLastUse = (System.currentTimeMillis() - stats.getLastUsedTime().getTime()) / (1000 * 60 * 60);
            double recencyScore = Math.max(0, config.getMaxRecencyHours() - hoursSinceLastUse);
            score += recencyScore * config.getRecencyWeight();
        }

        // 4. 配置指定预加载 (Configuration specified preload)
        if (config.getForcePreloadRoles().contains(role.getId())) {
            score += 1000.0; // 强制预加载获得高分 (Force preload gets high score)
        }

        log.debug(I18N.get("loader.preload.score_calculated", role.getName(), score));
        return score;
    }

    /**
     * 记录角色使用 (Record role usage)
     *
     * @param roleId 角色ID (Role ID)
     */
    public void recordUsage(String roleId) {
        usageStats.computeIfAbsent(roleId, k -> new RoleUsageStats())
                .recordUsage();
        log.debug(I18N.get("loader.preload.usage_recorded", roleId));
    }

    /**
     * 获取使用统计 (Get usage statistics)
     *
     * @param roleId 角色ID (Role ID)
     * @return 使用统计 (Usage statistics)
     */
    public RoleUsageStats getUsageStats(String roleId) {
        return usageStats.get(roleId);
    }

    /**
     * 重置统计信息 (Reset statistics)
     */
    public void resetStats() {
        usageStats.clear();
        log.info(I18N.get("loader.preload.stats_reset"));
    }

    /**
     * 角色使用统计 (Role Usage Statistics)
     *
     * 线程安全的统计类，使用 AtomicInteger 保证计数的原子性
     * (Thread-safe statistics class, using AtomicInteger for atomic counting)
     */
    @Data
    public static class RoleUsageStats {
        /**
         * 使用次数 (Usage count)
         * 使用 AtomicInteger 保证线程安全
         * (Using AtomicInteger for thread safety)
         */
        private final java.util.concurrent.atomic.AtomicInteger usageCount = new java.util.concurrent.atomic.AtomicInteger(0);

        /**
         * 最后使用时间 (Last used time)
         * volatile 保证可见性
         * (volatile ensures visibility)
         */
        private volatile Date lastUsedTime;

        /**
         * 记录使用 (Record usage)
         * 线程安全的原子操作
         * (Thread-safe atomic operation)
         */
        public void recordUsage() {
            usageCount.incrementAndGet();
            lastUsedTime = new Date();
        }

        /**
         * 获取使用次数 (Get usage count)
         */
        public int getUsageCount() {
            return usageCount.get();
        }
    }

    /**
     * 预加载配置 (Preload Configuration)
     */
    @Data
    @Builder
    public static class PreloadConfig {
        /**
         * 最大预加载数量 (Maximum preload count)
         */
        @Builder.Default
        private int maxPreloadCount = 3;

        /**
         * 优先级权重 (Priority weight)
         */
        @Builder.Default
        private double priorityWeight = 1.0;

        /**
         * 使用频率权重 (Usage frequency weight)
         */
        @Builder.Default
        private double usageWeight = 2.0;

        /**
         * 最近使用权重 (Recency weight)
         */
        @Builder.Default
        private double recencyWeight = 1.5;

        /**
         * 最大最近使用时长（小时） (Maximum recency hours)
         */
        @Builder.Default
        private int maxRecencyHours = 24;

        /**
         * 强制预加载的角色ID列表 (Force preload role IDs)
         */
        @Builder.Default
        private Set<String> forcePreloadRoles = new HashSet<>();

        /**
         * 创建默认配置 (Create default configuration)
         *
         * @return 默认配置 (Default configuration)
         */
        public static PreloadConfig createDefault() {
            return PreloadConfig.builder().build();
        }
    }
}

