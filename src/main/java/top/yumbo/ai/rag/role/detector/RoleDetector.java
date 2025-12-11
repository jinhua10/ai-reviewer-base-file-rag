package top.yumbo.ai.rag.role.detector;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色检测器 (Role Detector)
 *
 * 综合多种方法检测问题所属角色
 * (Detects role using multiple methods)
 *
 * 检测策略 (Detection Strategy):
 * 1. 关键词匹配 (Keyword matching)
 * 2. AI 智能分析 (AI analysis)
 * 3. 用户历史偏好 (User preference)
 * 4. 加权融合决策 (Weighted fusion)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class RoleDetector {

    /**
     * -- SETTER --
     *  设置 RoleManager (仅用于测试)
     *  (Set RoleManager for testing only)
     */
    @Setter
    @Autowired
    private RoleManager roleManager;

    /**
     * -- SETTER --
     *  设置 KeywordMatcher (仅用于测试)
     *  (Set KeywordMatcher for testing only)
     */
    @Setter
    @Autowired
    private KeywordMatcher keywordMatcher;

    /**
     * -- SETTER --
     *  设置 AIRoleAnalyzer (仅用于测试)
     *  (Set AIRoleAnalyzer for testing only)
     */
    @Setter
    @Autowired
    private AIRoleAnalyzer aiRoleAnalyzer;

    /**
     * -- SETTER --
     *  设置 UserPreferenceTracker (仅用于测试)
     *  (Set UserPreferenceTracker for testing only)
     */
    @Setter
    @Autowired
    private UserPreferenceTracker preferenceTracker;

    @Value("${rag.role.detector.keyword.weight:0.3}")
    private double keywordWeight;

    @Value("${rag.role.detector.ai.weight:0.5}")
    private double aiWeight;

    @Value("${rag.role.detector.preference.weight:0.2}")
    private double preferenceWeight;

    /**
     * 检测角色 (Detect role)
     *
     * @param question 用户问题 (User question)
     * @param userId 用户ID (User ID, optional)
     * @return 角色检测结果 (Role detection result)
     */
    public RoleDetectionResult detect(String question, String userId) {
        if (question == null || question.trim().isEmpty()) {
            log.warn(I18N.get("detector.question.empty"));
            return RoleDetectionResult.createDefault(roleManager.getDefaultRole());
        }

        log.info(I18N.get("detector.detecting", question));

        // 获取所有启用的角色 (Get all enabled roles)
        List<Role> roles = roleManager.getEnabledRoles();

        if (roles.isEmpty()) {
            log.warn(I18N.get("detector.roles.empty"));
            return RoleDetectionResult.createDefault(roleManager.getDefaultRole());
        }

        // 1. 关键词匹配 (Keyword matching)
        List<RoleMatchResult> keywordResults = keywordMatcher.match(question, roles);

        // 2. AI 分析 (AI analysis)
        List<RoleMatchResult> aiResults = aiRoleAnalyzer.analyze(question, roles);

        // 3. 用户偏好 (User preference)
        Optional<RoleMatchResult> preferenceResult = Optional.empty();
        if (userId != null) {
            preferenceResult = preferenceTracker.predictRole(userId, question);
        }

        // 4. 融合结果 (Fuse results)
        RoleDetectionResult result = fuseResults(question, userId,
                keywordResults, aiResults, preferenceResult, roles);

        // 5. 记录用户选择（用于学习） (Record choice for learning)
        if (userId != null && result.getSelectedRole() != null) {
            preferenceTracker.recordChoice(userId, result.getSelectedRole().getId(), question);
        }

        log.info(I18N.get("detector.detected", result.getSelectedRole().getName(),
                         result.getConfidence()));

        return result;
    }

    /**
     * 融合多个检测结果 (Fuse multiple detection results)
     *
     * @param question 问题 (Question)
     * @param userId 用户ID (User ID)
     * @param keywordResults 关键词匹配结果 (Keyword results)
     * @param aiResults AI 分析结果 (AI results)
     * @param preferenceResult 用户偏好结果 (Preference result)
     * @param allRoles 所有角色 (All roles)
     * @return 融合后的检测结果 (Fused detection result)
     */
    private RoleDetectionResult fuseResults(String question, String userId,
                                            List<RoleMatchResult> keywordResults,
                                            List<RoleMatchResult> aiResults,
                                            Optional<RoleMatchResult> preferenceResult,
                                            List<Role> allRoles) {
        // 创建角色分数映射 (Create role score map)
        Map<String, Double> roleScores = new HashMap<>();
        Map<String, List<RoleMatchResult>> roleDetails = new HashMap<>();

        // 累加关键词匹配分数 (Add keyword match scores)
        for (RoleMatchResult result : keywordResults) {
            double score = result.getScore() * keywordWeight;
            roleScores.merge(result.getRoleId(), score, Double::sum);
            roleDetails.computeIfAbsent(result.getRoleId(), k -> new ArrayList<>()).add(result);
        }

        // 累加 AI 分析分数 (Add AI analysis scores)
        for (RoleMatchResult result : aiResults) {
            double score = result.getScore() * aiWeight;
            roleScores.merge(result.getRoleId(), score, Double::sum);
            roleDetails.computeIfAbsent(result.getRoleId(), k -> new ArrayList<>()).add(result);
        }

        // 累加用户偏好分数 (Add preference scores)
        if (preferenceResult.isPresent()) {
            RoleMatchResult result = preferenceResult.get();
            double score = result.getScore() * preferenceWeight;
            roleScores.merge(result.getRoleId(), score, Double::sum);
            roleDetails.computeIfAbsent(result.getRoleId(), k -> new ArrayList<>()).add(result);
        }

        // 选择分数最高的角色 (Select role with highest score)
        Optional<Map.Entry<String, Double>> topEntry = roleScores.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topEntry.isEmpty()) {
            // 没有匹配结果，使用默认角色 (No match, use default role)
            return RoleDetectionResult.createDefault(roleManager.getDefaultRole());
        }

        String topRoleId = topEntry.get().getKey();
        double topScore = topEntry.get().getValue();

        // 获取角色对象 (Get role object)
        Role selectedRole = roleManager.getRole(topRoleId);

        // 计算置信度 (Calculate confidence)
        double maxPossibleScore = keywordWeight * 10 + aiWeight * 10 + preferenceWeight * 10;
        double confidence = Math.min(1.0, topScore / maxPossibleScore);

        // 收集所有匹配结果 (Collect all match results)
        List<RoleMatchResult> allResults = new ArrayList<>();
        allResults.addAll(keywordResults);
        allResults.addAll(aiResults);
        preferenceResult.ifPresent(allResults::add);

        return RoleDetectionResult.builder()
                .question(question)
                .userId(userId)
                .selectedRole(selectedRole)
                .confidence(confidence)
                .finalScore(topScore)
                .allCandidates(convertToResults(roleScores, allRoles))
                .detectionMethods(List.of("keyword", "ai", "preference"))
                .methodResults(roleDetails.get(topRoleId))
                .timestamp(new Date())
                .build();
    }

    /**
     * 转换分数为结果列表 (Convert scores to result list)
     *
     * @param roleScores 角色分数映射 (Role scores map)
     * @param allRoles 所有角色 (All roles)
     * @return 结果列表 (Result list)
     */
    private List<RoleMatchResult> convertToResults(Map<String, Double> roleScores, List<Role> allRoles) {
        Map<String, Role> roleMap = allRoles.stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        return roleScores.entrySet().stream()
                .map(e -> {
                    Role role = roleMap.get(e.getKey());
                    return RoleMatchResult.builder()
                            .roleId(e.getKey())
                            .roleName(role != null ? role.getName() : e.getKey())
                            .score(e.getValue())
                            .confidence(Math.min(1.0, e.getValue() / 10.0))
                            .method("hybrid")
                            .build();
                })
                .sorted(Comparator.comparingDouble(RoleMatchResult::getScore).reversed())
                .toList();
    }

    /**
     * 快速检测（仅使用关键词） (Quick detect using keyword only)
     *
     * @param question 用户问题 (User question)
     * @return 角色检测结果 (Role detection result)
     */
    public RoleDetectionResult quickDetect(String question) {
        List<Role> roles = roleManager.getEnabledRoles();
        List<RoleMatchResult> results = keywordMatcher.match(question, roles);

        if (results.isEmpty()) {
            return RoleDetectionResult.createDefault(roleManager.getDefaultRole());
        }

        RoleMatchResult top = results.get(0);
        Role selectedRole = roleManager.getRole(top.getRoleId());

        return RoleDetectionResult.builder()
                .question(question)
                .selectedRole(selectedRole)
                .confidence(top.getConfidence())
                .finalScore(top.getScore())
                .allCandidates(results)
                .detectionMethods(List.of("keyword"))
                .methodResults(List.of(top))
                .timestamp(new Date())
                .build();
    }

    /**
     * 获取检测器统计信息 (Get detector statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("keywordWeight", keywordWeight);
        stats.put("aiWeight", aiWeight);
        stats.put("preferenceWeight", preferenceWeight);
        stats.put("aiAvailable", aiRoleAnalyzer.isAvailable());
        stats.putAll(preferenceTracker.getStatistics());

        return stats;
    }


}
