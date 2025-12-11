package top.yumbo.ai.rag.role;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色知识提取器 (Role Knowledge Extractor)
 *
 * 核心创新 (Core Innovation):
 * 从个人本地数据中提取角色强相关的知识
 *
 * 洞察 (Insight):
 * - 开发者的本地数据 → 开发者角色知识
 * - 架构师的本地数据 → 架构师角色知识
 * - 测试的本地数据 → 测试角色知识
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class RoleKnowledgeExtractor {

    /**
     * 角色特征词典 (Role feature dictionary)
     * Key: roleId, Value: 特征关键词列表
     */
    private final Map<String, Set<String>> roleFeatures = new HashMap<>();

    /**
     * 最小质量分数 (Minimum quality score)
     */
    private double minQualityScore = 0.7;

    // ========== 初始化 (Initialization) ==========

    public RoleKnowledgeExtractor() {
        initializeRoleFeatures();
        log.info(I18N.get("role.extractor.initialized"));
    }

    /**
     * 初始化角色特征 (Initialize role features)
     */
    private void initializeRoleFeatures() {
        // 开发者特征 (Developer features)
        roleFeatures.put("developer", Set.of(
            "代码", "编程", "算法", "数据结构", "设计模式",
            "code", "programming", "algorithm", "bug", "debug",
            "Java", "Python", "JavaScript", "API", "框架"
        ));

        // 架构师特征 (Architect features)
        roleFeatures.put("architect", Set.of(
            "架构", "设计", "系统", "性能", "扩展性",
            "architecture", "design", "system", "scalability",
            "微服务", "分布式", "高可用", "技术选型"
        ));

        // 测试特征 (Tester features)
        roleFeatures.put("tester", Set.of(
            "测试", "用例", "自动化", "质量", "缺陷",
            "test", "testing", "QA", "automation", "quality",
            "单元测试", "集成测试", "性能测试"
        ));

        // 产品经理特征 (Product Manager features)
        roleFeatures.put("product_manager", Set.of(
            "需求", "功能", "用户", "体验", "迭代",
            "requirement", "feature", "user", "UX", "story",
            "产品设计", "原型", "需求分析"
        ));

        log.debug(I18N.get("role.extractor.features_loaded"), roleFeatures.size());
    }

    // ========== 知识提取 (Knowledge Extraction) ==========

    /**
     * 从用户数据中提取角色知识 (Extract role knowledge from user data)
     *
     * @param userId 用户ID (User ID)
     * @param userData 用户数据 (User data)
     * @return 角色知识映射 (Role knowledge map)
     */
    public Map<String, List<RoleKnowledge>> extract(String userId, List<UserKnowledge> userData) {
        try {
            log.info(I18N.get("role.extractor.extracting"), userId, userData.size());

            Map<String, List<RoleKnowledge>> roleKnowledgeMap = new HashMap<>();

            // 1. 遍历用户数据 (Iterate user data)
            for (UserKnowledge uk : userData) {
                // 2. 质量过滤 (Quality filter)
                if (uk.getQualityScore() < minQualityScore) {
                    continue;
                }

                // 3. 角色分类 (Role classification)
                String detectedRole = detectRole(uk);

                if (detectedRole != null) {
                    // 4. 创建角色知识 (Create role knowledge)
                    RoleKnowledge rk = new RoleKnowledge();
                    rk.setId(UUID.randomUUID().toString());
                    rk.setRoleId(detectedRole);
                    rk.setSourceUserId(userId);
                    rk.setQuestion(uk.getQuestion());
                    rk.setAnswer(uk.getAnswer());
                    rk.setQualityScore(uk.getQualityScore());
                    rk.setExtractTime(LocalDateTime.now());
                    rk.setRelevanceScore(calculateRelevance(uk, detectedRole));

                    // 5. 添加到映射 (Add to map)
                    roleKnowledgeMap.computeIfAbsent(detectedRole, k -> new ArrayList<>()).add(rk);
                }
            }

            log.info(I18N.get("role.extractor.extracted"),
                roleKnowledgeMap.values().stream().mapToInt(List::size).sum());

            return roleKnowledgeMap;

        } catch (Exception e) {
            log.error(I18N.get("role.extractor.extract_failed"), e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 检测知识所属角色 (Detect knowledge role)
     */
    private String detectRole(UserKnowledge knowledge) {
        String content = knowledge.getQuestion() + " " + knowledge.getAnswer();
        content = content.toLowerCase();

        Map<String, Integer> roleScores = new HashMap<>();

        // 计算每个角色的匹配分数 (Calculate match score for each role)
        for (var entry : roleFeatures.entrySet()) {
            String roleId = entry.getKey();
            Set<String> features = entry.getValue();

            int matchCount = 0;
            for (String feature : features) {
                if (content.contains(feature.toLowerCase())) {
                    matchCount++;
                }
            }

            if (matchCount > 0) {
                roleScores.put(roleId, matchCount);
            }
        }

        // 返回得分最高的角色 (Return role with highest score)
        if (roleScores.isEmpty()) {
            return null;
        }

        return roleScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * 计算相关性分数 (Calculate relevance score)
     */
    private double calculateRelevance(UserKnowledge knowledge, String roleId) {
        String content = knowledge.getQuestion() + " " + knowledge.getAnswer();
        content = content.toLowerCase();

        Set<String> features = roleFeatures.get(roleId);
        if (features == null) {
            return 0.5;
        }

        int matchCount = 0;
        for (String feature : features) {
            if (content.contains(feature.toLowerCase())) {
                matchCount++;
            }
        }

        // 相关性 = 匹配数 / 特征总数 (Relevance = match count / total features)
        return Math.min(1.0, (double) matchCount / Math.max(features.size() * 0.3, 1));
    }

    // ========== 批量提取 (Batch Extraction) ==========

    /**
     * 从多个用户数据中批量提取角色知识 (Batch extract from multiple users)
     *
     * @param usersData 多用户数据映射 (Multi-user data map)
     * @return 角色知识映射 (Role knowledge map)
     */
    public Map<String, List<RoleKnowledge>> batchExtract(Map<String, List<UserKnowledge>> usersData) {
        log.info(I18N.get("role.extractor.batch_extracting"), usersData.size());

        Map<String, List<RoleKnowledge>> aggregated = new HashMap<>();

        for (var entry : usersData.entrySet()) {
            String userId = entry.getKey();
            List<UserKnowledge> userData = entry.getValue();

            // 提取单个用户的角色知识 (Extract role knowledge for single user)
            Map<String, List<RoleKnowledge>> userRoleKnowledge = extract(userId, userData);

            // 聚合到总映射 (Aggregate to total map)
            for (var roleEntry : userRoleKnowledge.entrySet()) {
                String roleId = roleEntry.getKey();
                List<RoleKnowledge> knowledge = roleEntry.getValue();

                aggregated.computeIfAbsent(roleId, k -> new ArrayList<>()).addAll(knowledge);
            }
        }

        log.info(I18N.get("role.extractor.batch_extracted"),
            aggregated.values().stream().mapToInt(List::size).sum());

        return aggregated;
    }

    // ========== 统计分析 (Statistics) ==========

    /**
     * 获取提取统计 (Get extraction statistics)
     */
    public ExtractionStats getStats(Map<String, List<RoleKnowledge>> roleKnowledgeMap) {
        ExtractionStats stats = new ExtractionStats();

        for (var entry : roleKnowledgeMap.entrySet()) {
            String roleId = entry.getKey();
            List<RoleKnowledge> knowledge = entry.getValue();

            RoleStats roleStats = new RoleStats();
            roleStats.setRoleId(roleId);
            roleStats.setKnowledgeCount(knowledge.size());
            roleStats.setAvgQualityScore(
                knowledge.stream()
                    .mapToDouble(RoleKnowledge::getQualityScore)
                    .average()
                    .orElse(0.0)
            );
            roleStats.setAvgRelevanceScore(
                knowledge.stream()
                    .mapToDouble(RoleKnowledge::getRelevanceScore)
                    .average()
                    .orElse(0.0)
            );

            stats.getRoleStats().put(roleId, roleStats);
        }

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 用户知识 (User Knowledge)
     */
    @Data
    public static class UserKnowledge {
        private String id;
        private String userId;
        private String question;
        private String answer;
        private double qualityScore;
        private LocalDateTime createTime;
    }

    /**
     * 角色知识 (Role Knowledge)
     */
    @Data
    public static class RoleKnowledge {
        private String id;
        private String roleId;              // 角色ID
        private String sourceUserId;        // 来源用户ID
        private String question;
        private String answer;
        private double qualityScore;        // 质量分数
        private double relevanceScore;      // 角色相关性分数
        private LocalDateTime extractTime;  // 提取时间
    }

    /**
     * 提取统计 (Extraction Statistics)
     */
    @Data
    public static class ExtractionStats {
        private Map<String, RoleStats> roleStats = new HashMap<>();
    }

    /**
     * 角色统计 (Role Statistics)
     */
    @Data
    public static class RoleStats {
        private String roleId;
        private int knowledgeCount;
        private double avgQualityScore;
        private double avgRelevanceScore;
    }
}

