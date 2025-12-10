package top.yumbo.ai.rag.spring.boot.abtest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A/B 测试服务
 * (A/B Testing Service)
 *
 * 用于冲突概念的随机展示和用户反应统计
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Service
public class ABTestService {

    // A/B 测试实验存储
    private final Map<String, ABTestExperiment> experiments = new ConcurrentHashMap<>();

    // 用户分组记录
    private final Map<String, UserGroup> userGroups = new ConcurrentHashMap<>();

    /**
     * 创建 A/B 测试实验
     * (Create A/B test experiment)
     *
     * @param experimentId 实验ID
     * @param question 问题
     * @param variantA 变体A（概念A）
     * @param variantB 变体B（概念B）
     * @return 实验对象
     */
    public ABTestExperiment createExperiment(
            String experimentId,
            String question,
            Variant variantA,
            Variant variantB) {

        ABTestExperiment experiment = new ABTestExperiment();
        experiment.setExperimentId(experimentId);
        experiment.setQuestion(question);
        experiment.setVariantA(variantA);
        experiment.setVariantB(variantB);
        experiment.setCreatedAt(LocalDateTime.now());
        experiment.setActive(true);

        experiments.put(experimentId, experiment);

        log.info(I18N.get("abtest.log.experiment_created", experimentId, question));

        return experiment;
    }

    /**
     * 为用户分配变体
     * (Assign variant to user)
     *
     * @param experimentId 实验ID
     * @param userId 用户ID
     * @return 分配的变体（A或B）
     */
    public Variant assignVariant(String experimentId, String userId) {
        ABTestExperiment experiment = experiments.get(experimentId);
        if (experiment == null || !experiment.isActive()) {
            throw new IllegalArgumentException("实验不存在或已停止: " + experimentId);
        }

        // 检查用户是否已分组
        String groupKey = experimentId + ":" + userId;
        UserGroup existingGroup = userGroups.get(groupKey);
        if (existingGroup != null) {
            log.debug(I18N.get("abtest.log.user_in_group", userId, existingGroup.getGroupName()));
            return existingGroup.getGroupName().equals("A") ?
                experiment.getVariantA() : experiment.getVariantB();
        }

        // 随机分组（50% / 50%）
        boolean assignToA = ThreadLocalRandom.current().nextBoolean();
        String groupName = assignToA ? "A" : "B";
        Variant assignedVariant = assignToA ?
            experiment.getVariantA() : experiment.getVariantB();

        // 记录分组
        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(userId);
        userGroup.setExperimentId(experimentId);
        userGroup.setGroupName(groupName);
        userGroup.setAssignedAt(LocalDateTime.now());
        userGroups.put(groupKey, userGroup);

        // 更新实验统计
        if (assignToA) {
            experiment.getGroupACount().incrementAndGet();
        } else {
            experiment.getGroupBCount().incrementAndGet();
        }

        log.info("用户已分组: userId={}, experimentId={}, group={}",
            userId, experimentId, groupName);

        return assignedVariant;
    }

    /**
     * 记录用户反馈
     * (Record user feedback)
     *
     * @param experimentId 实验ID
     * @param userId 用户ID
     * @param satisfied 是否满意
     */
    public void recordFeedback(String experimentId, String userId, boolean satisfied) {
        ABTestExperiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            log.warn("实验不存在: {}", experimentId);
            return;
        }

        String groupKey = experimentId + ":" + userId;
        UserGroup userGroup = userGroups.get(groupKey);
        if (userGroup == null) {
            log.warn("用户未分组: userId={}, experimentId={}", userId, experimentId);
            return;
        }

        // 记录反馈
        userGroup.setFeedback(satisfied);
        userGroup.setFeedbackAt(LocalDateTime.now());

        // 更新实验统计
        if ("A".equals(userGroup.getGroupName())) {
            experiment.getGroupAFeedbackCount().incrementAndGet();
            if (satisfied) {
                experiment.getGroupASatisfiedCount().incrementAndGet();
            }
        } else {
            experiment.getGroupBFeedbackCount().incrementAndGet();
            if (satisfied) {
                experiment.getGroupBSatisfiedCount().incrementAndGet();
            }
        }

        log.info("反馈已记录: userId={}, experimentId={}, group={}, satisfied={}",
            userId, experimentId, userGroup.getGroupName(), satisfied);
    }

    /**
     * 获取实验统计
     * (Get experiment statistics)
     */
    public ExperimentStatistics getStatistics(String experimentId) {
        ABTestExperiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            throw new IllegalArgumentException("实验不存在: " + experimentId);
        }

        ExperimentStatistics stats = new ExperimentStatistics();
        stats.setExperimentId(experimentId);
        stats.setQuestion(experiment.getQuestion());

        // 组A统计
        int groupACount = experiment.getGroupACount().get();
        int groupAFeedback = experiment.getGroupAFeedbackCount().get();
        int groupASatisfied = experiment.getGroupASatisfiedCount().get();

        stats.setGroupACount(groupACount);
        stats.setGroupAFeedbackCount(groupAFeedback);
        stats.setGroupASatisfiedCount(groupASatisfied);
        stats.setGroupASatisfactionRate(groupAFeedback > 0 ?
            (double) groupASatisfied / groupAFeedback : 0.0);

        // 组B统计
        int groupBCount = experiment.getGroupBCount().get();
        int groupBFeedback = experiment.getGroupBFeedbackCount().get();
        int groupBSatisfied = experiment.getGroupBSatisfiedCount().get();

        stats.setGroupBCount(groupBCount);
        stats.setGroupBFeedbackCount(groupBFeedback);
        stats.setGroupBSatisfiedCount(groupBSatisfied);
        stats.setGroupBSatisfactionRate(groupBFeedback > 0 ?
            (double) groupBSatisfied / groupBFeedback : 0.0);

        // 判断赢家
        if (groupAFeedback >= 10 && groupBFeedback >= 10) {
            if (stats.getGroupASatisfactionRate() > stats.getGroupBSatisfactionRate()) {
                stats.setWinner("A");
            } else if (stats.getGroupBSatisfactionRate() > stats.getGroupASatisfactionRate()) {
                stats.setWinner("B");
            } else {
                stats.setWinner("TIE");
            }
        } else {
            stats.setWinner("INSUFFICIENT_DATA");
        }

        return stats;
    }

    /**
     * 自动决策（停止实验并选择赢家）
     * (Auto decide - stop experiment and choose winner)
     *
     * @param experimentId 实验ID
     * @param minSamples 最小样本数（默认30）
     * @param confidenceLevel 置信水平（默认0.05，即95%置信度）
     * @return 决策结果
     */
    public DecisionResult autoDecide(String experimentId, int minSamples, double confidenceLevel) {
        ABTestExperiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            throw new IllegalArgumentException("实验不存在: " + experimentId);
        }

        ExperimentStatistics stats = getStatistics(experimentId);

        DecisionResult result = new DecisionResult();
        result.setExperimentId(experimentId);
        result.setDecisionTime(LocalDateTime.now());

        // 检查是否有足够的样本
        if (stats.getGroupAFeedbackCount() < minSamples ||
            stats.getGroupBFeedbackCount() < minSamples) {
            result.setDecision("CONTINUE");
            result.setReason("样本不足（需要至少 " + minSamples + " 个反馈）");
            return result;
        }

        // 简单决策：选择满意率更高的
        double rateA = stats.getGroupASatisfactionRate();
        double rateB = stats.getGroupBSatisfactionRate();
        double difference = Math.abs(rateA - rateB);

        if (difference < 0.05) {
            // 差异小于5%，认为相似
            result.setDecision("TIE");
            result.setReason("两个变体满意率相近（差异 <5%）");
            result.setChosenVariant(null);
        } else if (rateA > rateB) {
            result.setDecision("CHOOSE_A");
            result.setReason(String.format("变体A满意率更高（%.1f%% vs %.1f%%）",
                rateA * 100, rateB * 100));
            result.setChosenVariant(experiment.getVariantA());
        } else {
            result.setDecision("CHOOSE_B");
            result.setReason(String.format("变体B满意率更高（%.1f%% vs %.1f%%）",
                rateB * 100, rateA * 100));
            result.setChosenVariant(experiment.getVariantB());
        }

        // 停止实验
        experiment.setActive(false);

        log.info("A/B 测试自动决策: experimentId={}, decision={}, reason={}",
            experimentId, result.getDecision(), result.getReason());

        return result;
    }

    /**
     * 获取所有实验
     */
    public List<ABTestExperiment> getAllExperiments() {
        return new ArrayList<>(experiments.values());
    }

    /**
     * 获取活跃实验
     */
    public List<ABTestExperiment> getActiveExperiments() {
        List<ABTestExperiment> active = new ArrayList<>();
        for (ABTestExperiment exp : experiments.values()) {
            if (exp.isActive()) {
                active.add(exp);
            }
        }
        return active;
    }

    /**
     * 停止实验
     */
    public void stopExperiment(String experimentId) {
        ABTestExperiment experiment = experiments.get(experimentId);
        if (experiment != null) {
            experiment.setActive(false);
            log.info("实验已停止: {}", experimentId);
        }
    }

    // ==================== 内部数据类 ====================

    @Data
    public static class ABTestExperiment {
        private String experimentId;
        private String question;
        private Variant variantA;
        private Variant variantB;
        private LocalDateTime createdAt;
        private boolean active;

        // 统计数据
        private AtomicInteger groupACount = new AtomicInteger(0);
        private AtomicInteger groupBCount = new AtomicInteger(0);
        private AtomicInteger groupAFeedbackCount = new AtomicInteger(0);
        private AtomicInteger groupBFeedbackCount = new AtomicInteger(0);
        private AtomicInteger groupASatisfiedCount = new AtomicInteger(0);
        private AtomicInteger groupBSatisfiedCount = new AtomicInteger(0);
    }

    @Data
    @lombok.Builder
    public static class Variant {
        private String variantId;
        private String conceptId;
        private String content;
        private String source;
        private double confidence;
    }

    @Data
    public static class UserGroup {
        private String userId;
        private String experimentId;
        private String groupName; // "A" or "B"
        private LocalDateTime assignedAt;
        private Boolean feedback;
        private LocalDateTime feedbackAt;
    }

    @Data
    public static class ExperimentStatistics {
        private String experimentId;
        private String question;

        private int groupACount;
        private int groupAFeedbackCount;
        private int groupASatisfiedCount;
        private double groupASatisfactionRate;

        private int groupBCount;
        private int groupBFeedbackCount;
        private int groupBSatisfiedCount;
        private double groupBSatisfactionRate;

        private String winner; // "A", "B", "TIE", "INSUFFICIENT_DATA"
    }

    @Data
    public static class DecisionResult {
        private String experimentId;
        private String decision; // "CHOOSE_A", "CHOOSE_B", "TIE", "CONTINUE"
        private String reason;
        private Variant chosenVariant;
        private LocalDateTime decisionTime;
    }
}

