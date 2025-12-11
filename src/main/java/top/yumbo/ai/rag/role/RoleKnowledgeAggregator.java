package top.yumbo.ai.rag.role;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.RoleKnowledgeExtractor.RoleKnowledge;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色知识聚合器 (Role Knowledge Aggregator)
 *
 * 核心创新 (Core Innovation):
 * 聚合多个用户的角色知识，形成完整的角色知识库
 *
 * 聚合策略 (Aggregation Strategy):
 * 1. 去重合并 (Deduplication & merging)
 * 2. 质量加权 (Quality weighting)
 * 3. 多数投票 (Majority voting)
 * 4. 自动优化 (Auto optimization)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class RoleKnowledgeAggregator {

    /**
     * 相似度阈值（用于去重） (Similarity threshold for deduplication)
     */
    private double similarityThreshold = 0.85;

    /**
     * 最小用户数（多数投票） (Minimum user count for majority voting)
     */
    private int minUserCount = 3;

    // ========== 初始化 (Initialization) ==========

    public RoleKnowledgeAggregator() {
        log.info(I18N.get("role.aggregator.initialized"));
    }

    // ========== 知识聚合 (Knowledge Aggregation) ==========

    /**
     * 聚合角色知识 (Aggregate role knowledge)
     *
     * @param roleKnowledgeList 角色知识列表 (Role knowledge list)
     * @return 聚合后的知识 (Aggregated knowledge)
     */
    public List<AggregatedKnowledge> aggregate(List<RoleKnowledge> roleKnowledgeList) {
        try {
            log.info(I18N.get("role.aggregator.aggregating"), roleKnowledgeList.size());

            // 1. 按问题分组 (Group by question)
            Map<String, List<RoleKnowledge>> groupedByQuestion = groupByQuestion(roleKnowledgeList);

            // 2. 聚合每组知识 (Aggregate each group)
            List<AggregatedKnowledge> aggregatedList = new ArrayList<>();

            for (var entry : groupedByQuestion.entrySet()) {
                String question = entry.getKey();
                List<RoleKnowledge> group = entry.getValue();

                // 只有多个用户贡献的知识才聚合 (Only aggregate if multiple users)
                if (group.size() >= minUserCount) {
                    AggregatedKnowledge aggregated = aggregateGroup(question, group);
                    aggregatedList.add(aggregated);
                } else {
                    // 单用户知识直接转换 (Single user knowledge, direct conversion)
                    for (RoleKnowledge rk : group) {
                        aggregatedList.add(convertToAggregated(rk));
                    }
                }
            }

            // 3. 按质量排序 (Sort by quality)
            aggregatedList.sort((a, b) ->
                Double.compare(b.getAggregatedScore(), a.getAggregatedScore())
            );

            log.info(I18N.get("role.aggregator.aggregated"),
                roleKnowledgeList.size(), aggregatedList.size());

            return aggregatedList;

        } catch (Exception e) {
            log.error(I18N.get("role.aggregator.aggregate_failed"), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 按问题分组（考虑相似度） (Group by question with similarity)
     */
    private Map<String, List<RoleKnowledge>> groupByQuestion(List<RoleKnowledge> knowledgeList) {
        Map<String, List<RoleKnowledge>> grouped = new HashMap<>();

        for (RoleKnowledge rk : knowledgeList) {
            String question = rk.getQuestion();
            boolean found = false;

            // 查找相似问题 (Find similar question)
            for (String existingQuestion : grouped.keySet()) {
                if (calculateSimilarity(question, existingQuestion) > similarityThreshold) {
                    grouped.get(existingQuestion).add(rk);
                    found = true;
                    break;
                }
            }

            // 没有相似问题，创建新组 (No similar question, create new group)
            if (!found) {
                grouped.put(question, new ArrayList<>(List.of(rk)));
            }
        }

        return grouped;
    }

    /**
     * 聚合一组知识 (Aggregate a group of knowledge)
     */
    private AggregatedKnowledge aggregateGroup(String question, List<RoleKnowledge> group) {
        AggregatedKnowledge aggregated = new AggregatedKnowledge();
        aggregated.setId(UUID.randomUUID().toString());
        aggregated.setQuestion(question);

        // 提取角色ID（假设同组内角色相同）(Extract role ID)
        aggregated.setRoleId(group.get(0).getRoleId());

        // 统计贡献者 (Count contributors)
        Set<String> contributors = group.stream()
            .map(RoleKnowledge::getSourceUserId)
            .collect(Collectors.toSet());
        aggregated.setContributorCount(contributors.size());
        aggregated.setContributorIds(new ArrayList<>(contributors));

        // 聚合答案（选择最高质量的答案） (Aggregate answers - select highest quality)
        RoleKnowledge bestAnswer = group.stream()
            .max(Comparator.comparingDouble(RoleKnowledge::getQualityScore))
            .orElse(group.get(0));
        aggregated.setAnswer(bestAnswer.getAnswer());

        // 计算聚合分数（质量加权平均） (Calculate aggregated score)
        double avgQuality = group.stream()
            .mapToDouble(RoleKnowledge::getQualityScore)
            .average()
            .orElse(0.0);
        double avgRelevance = group.stream()
            .mapToDouble(RoleKnowledge::getRelevanceScore)
            .average()
            .orElse(0.0);

        // 聚合分数 = 平均质量 * 0.6 + 平均相关性 * 0.3 + 贡献者数量加成 * 0.1
        double contributorBonus = Math.min(1.0, contributors.size() / 10.0);
        aggregated.setAggregatedScore(
            avgQuality * 0.6 + avgRelevance * 0.3 + contributorBonus * 0.1
        );

        aggregated.setAggregateTime(LocalDateTime.now());

        return aggregated;
    }

    /**
     * 转换为聚合知识 (Convert to aggregated knowledge)
     */
    private AggregatedKnowledge convertToAggregated(RoleKnowledge rk) {
        AggregatedKnowledge aggregated = new AggregatedKnowledge();
        aggregated.setId(rk.getId());
        aggregated.setRoleId(rk.getRoleId());
        aggregated.setQuestion(rk.getQuestion());
        aggregated.setAnswer(rk.getAnswer());
        aggregated.setContributorCount(1);
        aggregated.setContributorIds(List.of(rk.getSourceUserId()));
        aggregated.setAggregatedScore(rk.getQualityScore());
        aggregated.setAggregateTime(LocalDateTime.now());

        return aggregated;
    }

    /**
     * 计算文本相似度 (Calculate text similarity)
     * 使用简单的 Jaccard 相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        // Jaccard 相似度 = 交集 / 并集
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    // ========== 按角色聚合 (Aggregate by Role) ==========

    /**
     * 按角色聚合知识 (Aggregate knowledge by role)
     *
     * @param roleKnowledgeMap 角色知识映射 (Role knowledge map)
     * @return 按角色分组的聚合知识 (Aggregated knowledge grouped by role)
     */
    public Map<String, List<AggregatedKnowledge>> aggregateByRole(
            Map<String, List<RoleKnowledge>> roleKnowledgeMap) {

        log.info(I18N.get("role.aggregator.aggregating_by_role"), roleKnowledgeMap.size());

        Map<String, List<AggregatedKnowledge>> result = new HashMap<>();

        for (var entry : roleKnowledgeMap.entrySet()) {
            String roleId = entry.getKey();
            List<RoleKnowledge> knowledgeList = entry.getValue();

            // 聚合该角色的所有知识 (Aggregate all knowledge for this role)
            List<AggregatedKnowledge> aggregated = aggregate(knowledgeList);
            result.put(roleId, aggregated);

            log.debug(I18N.get("role.aggregator.role_aggregated"),
                roleId, knowledgeList.size(), aggregated.size());
        }

        return result;
    }

    // ========== 统计分析 (Statistics) ==========

    /**
     * 获取聚合统计 (Get aggregation statistics)
     */
    public AggregationStats getStats(Map<String, List<AggregatedKnowledge>> aggregatedMap) {
        AggregationStats stats = new AggregationStats();
        stats.setTotalRoles(aggregatedMap.size());

        int totalKnowledge = 0;
        int totalContributors = 0;

        for (var entry : aggregatedMap.entrySet()) {
            List<AggregatedKnowledge> knowledgeList = entry.getValue();
            totalKnowledge += knowledgeList.size();

            // 统计贡献者 (Count contributors)
            Set<String> roleContributors = knowledgeList.stream()
                .flatMap(ak -> ak.getContributorIds().stream())
                .collect(Collectors.toSet());
            totalContributors += roleContributors.size();
        }

        stats.setTotalKnowledge(totalKnowledge);
        stats.setAvgKnowledgePerRole(aggregatedMap.isEmpty() ? 0 :
            (double) totalKnowledge / aggregatedMap.size());
        stats.setTotalContributors(totalContributors);

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 聚合知识 (Aggregated Knowledge)
     */
    @Data
    public static class AggregatedKnowledge {
        private String id;
        private String roleId;                  // 角色ID
        private String question;                // 问题
        private String answer;                  // 答案（最佳）
        private double aggregatedScore;         // 聚合分数
        private int contributorCount;           // 贡献者数量
        private List<String> contributorIds;    // 贡献者ID列表
        private LocalDateTime aggregateTime;    // 聚合时间
    }

    /**
     * 聚合统计 (Aggregation Statistics)
     */
    @Data
    public static class AggregationStats {
        private int totalRoles;                 // 总角色数
        private int totalKnowledge;             // 总知识数
        private double avgKnowledgePerRole;     // 平均每个角色的知识数
        private int totalContributors;          // 总贡献者数
    }
}

