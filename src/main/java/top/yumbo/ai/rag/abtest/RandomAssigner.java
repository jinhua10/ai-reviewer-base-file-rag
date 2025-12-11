package top.yumbo.ai.rag.abtest;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 随机分组器 (Random Assigner)
 * 使用一致性哈希将用户随机分配到A/B测试组
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class RandomAssigner {

    private static final Logger logger = LoggerFactory.getLogger(RandomAssigner.class);

    /**
     * 用户分组缓存 (User Assignment Cache)
     * Key: experimentId:userId, Value: variantId
     */
    private final Map<String, String> assignmentCache;

    public RandomAssigner() {
        this.assignmentCache = new ConcurrentHashMap<>();
    }

    /**
     * 分配用户到组 (Assign User to Group)
     * 使用一致性哈希保证同一用户总是分配到同一组
     */
    public Assignment assignToGroup(ABTestExperiment experiment, String userId) {
        String cacheKey = experiment.getExperimentId() + ":" + userId;

        // 检查缓存 (Check cache)
        String cachedVariantId = assignmentCache.get(cacheKey);
        if (cachedVariantId != null) {
            logger.debug(I18N.get("abtest.assign.cache_hit"), userId, cachedVariantId);
            return new Assignment(experiment.getExperimentId(), userId, cachedVariantId);
        }

        // 使用一致性哈希分配 (Assign using consistent hashing)
        String variantId = consistentHash(userId, experiment.getVariants());

        // 缓存结果 (Cache result)
        assignmentCache.put(cacheKey, variantId);

        logger.info(I18N.get("abtest.assign.assigned"), userId, variantId);
        return new Assignment(experiment.getExperimentId(), userId, variantId);
    }

    /**
     * 一致性哈希 (Consistent Hashing)
     */
    private String consistentHash(String userId, List<Variant> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("No variants available");
        }

        // 计算哈希值 (Calculate hash)
        int hash = Math.abs(userId.hashCode());

        // 根据分配比例选择变体 (Select variant based on allocation)
        double random = (hash % 10000) / 10000.0;
        double cumulative = 0.0;

        for (Variant variant : variants) {
            cumulative += variant.getAllocation();
            if (random <= cumulative) {
                return variant.getVariantId();
            }
        }

        // 默认返回第一个 (Default to first)
        return variants.get(0).getVariantId();
    }

    /**
     * 获取用户分组 (Get User Assignment)
     */
    public String getAssignment(String experimentId, String userId) {
        String cacheKey = experimentId + ":" + userId;
        return assignmentCache.get(cacheKey);
    }

    /**
     * 按角色分层 (Stratify by Role)
     * 确保每个角色在各组中均匀分布
     */
    public Map<String, List<Assignment>> stratifyByRole(ABTestExperiment experiment,
                                                         Map<String, String> userRoles) {
        Map<String, List<Assignment>> stratified = new HashMap<>();

        for (Map.Entry<String, String> entry : userRoles.entrySet()) {
            String userId = entry.getKey();
            String role = entry.getValue();

            Assignment assignment = assignToGroup(experiment, userId);
            stratified.computeIfAbsent(role, k -> new ArrayList<>()).add(assignment);
        }

        logger.info(I18N.get("abtest.assign.stratified"), stratified.size());
        return stratified;
    }
}

/**
 * 分配记录 (Assignment Record)
 */
@Data
class Assignment {
    private String experimentId;
    private String userId;
    private String variantId;
    private LocalDateTime assignedAt;

    public Assignment(String experimentId, String userId, String variantId) {
        this.experimentId = experimentId;
        this.userId = userId;
        this.variantId = variantId;
        this.assignedAt = java.time.LocalDateTime.now();
    }

}

