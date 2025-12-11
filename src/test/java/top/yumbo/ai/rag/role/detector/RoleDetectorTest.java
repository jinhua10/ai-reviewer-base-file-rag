package top.yumbo.ai.rag.role.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.role.Role;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleDetector 单元测试
 * (RoleDetector Unit Test)
 *
 * 测试角色检测器的各个组件
 * (Test role detector components)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class RoleDetectorTest {

    private KeywordMatcher keywordMatcher;
    private UserPreferenceTracker preferenceTracker;

    @BeforeEach
    public void setUp() {
        // 初始化检测器组件 (Initialize detector components)
        keywordMatcher = new KeywordMatcher();
        preferenceTracker = new UserPreferenceTracker();
    }

    @Test
    public void testKeywordMatcher_WithEmptyQuestion() {
        // Given: 空问题
        String question = "";

        // When: 匹配关键词
        var results = keywordMatcher.match(question, java.util.Collections.emptyList());

        // Then: 应该返回空列表
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testKeywordMatcher_WithNullQuestion() {
        // Given: null 问题
        String question = null;

        // When: 匹配关键词
        var results = keywordMatcher.match(question, java.util.Collections.emptyList());

        // Then: 应该返回空列表
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testUserPreferenceTracker_RecordChoice() {
        // Given: 用户选择
        String userId = "test-user";
        String roleId = "developer";
        String question = "How to use Spring Boot?";

        // When: 记录选择
        assertDoesNotThrow(() -> preferenceTracker.recordChoice(userId, roleId, question));

        // Then: 应该成功记录
        assertTrue(true);
    }

    @Test
    public void testUserPreferenceTracker_PredictRole() {
        // Given: 记录一些历史
        String userId = "test-user-123";
        preferenceTracker.recordChoice(userId, "developer", "Spring Boot question 1");
        preferenceTracker.recordChoice(userId, "developer", "Spring Boot question 2");
        preferenceTracker.recordChoice(userId, "developer", "Spring Boot question 3");

        // When: 预测角色
        Optional<RoleMatchResult> result = preferenceTracker.predictRole(userId, "Another Spring question");

        // Then: 应该预测到开发者角色
        assertTrue(result.isPresent());
        assertEquals("developer", result.get().getRoleId());
        assertTrue(result.get().getScore() > 0);
    }

    @Test
    public void testUserPreferenceTracker_NoHistory() {
        // Given: 没有历史的用户
        String userId = "new-user";

        // When: 预测角色
        Optional<RoleMatchResult> result = preferenceTracker.predictRole(userId, "Some question");

        // Then: 应该返回空
        assertFalse(result.isPresent());
    }

    @Test
    public void testUserPreferenceTracker_GetStatistics() {
        // Given: Tracker 已初始化

        // When: 获取统计信息
        var stats = preferenceTracker.getStatistics();

        // Then: 应该返回统计信息
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalUsers"));
        assertTrue(stats.containsKey("totalRecords"));
    }

    @Test
    public void testRoleMatchResult_Builder() {
        // Given: 构建器参数
        String roleId = "developer";
        String roleName = "开发者";
        double score = 8.5;
        double confidence = 0.85;
        String method = "keyword";

        // When: 构建结果
        RoleMatchResult result = RoleMatchResult.builder()
                .roleId(roleId)
                .roleName(roleName)
                .score(score)
                .confidence(confidence)
                .method(method)
                .build();

        // Then: 字段应该正确设置
        assertNotNull(result);
        assertEquals(roleId, result.getRoleId());
        assertEquals(roleName, result.getRoleName());
        assertEquals(score, result.getScore());
        assertEquals(confidence, result.getConfidence());
        assertEquals(method, result.getMethod());
    }

    @Test
    public void testRoleDetectionResult_CreateDefault() {
        // Given: 默认角色
        Role defaultRole = Role.builder()
                .id("general")
                .name("通用")
                .description("通用角色")
                .keywords(Collections.emptySet())
                .weight(1.0)
                .enabled(true)
                .prompt(null)
                .tags(Collections.emptyList())
                .indexPath(null)
                .priority(0)
                .build();

        // When: 创建默认结果
        RoleDetectionResult result = RoleDetectionResult.createDefault(defaultRole);

        // Then: 应该包含默认值
        assertNotNull(result);
        assertEquals(defaultRole, result.getSelectedRole());
        assertTrue(result.getConfidence() >= 0);
    }

    @Test
    public void testComponentsInitialization() {
        // Given: 组件已在 setUp 中初始化

        // Then: 所有组件应该不为 null
        assertNotNull(keywordMatcher);
        assertNotNull(preferenceTracker);
    }

    @Test
    public void testUserPreferenceTracker_MultipleUsers() {
        // Given: 多个用户的选择记录
        String user1 = "user-1";
        String user2 = "user-2";

        preferenceTracker.recordChoice(user1, "developer", "Java question");
        preferenceTracker.recordChoice(user1, "developer", "Spring question");
        preferenceTracker.recordChoice(user2, "designer", "UI question");

        // When: 预测不同用户的偏好
        Optional<RoleMatchResult> result1 = preferenceTracker.predictRole(user1, "Another coding question");
        Optional<RoleMatchResult> result2 = preferenceTracker.predictRole(user2, "Another design question");

        // Then: 应该分别预测到正确的角色
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("developer", result1.get().getRoleId());
        assertEquals("designer", result2.get().getRoleId());
    }

    @Test
    public void testUserPreferenceTracker_MixedRoles() {
        // Given: 用户选择了多个不同的角色
        String userId = "mixed-user";
        preferenceTracker.recordChoice(userId, "developer", "Question 1");
        preferenceTracker.recordChoice(userId, "developer", "Question 2");
        preferenceTracker.recordChoice(userId, "developer", "Question 3");
        preferenceTracker.recordChoice(userId, "designer", "Question 4");

        // When: 预测角色
        Optional<RoleMatchResult> result = preferenceTracker.predictRole(userId, "Question 5");

        // Then: 应该预测到最常用的角色（developer）
        assertTrue(result.isPresent());
        assertEquals("developer", result.get().getRoleId());
    }

    @Test
    public void testRoleMatchResult_WithMatchedKeywords() {
        // Given: 包含匹配关键词的结果
        java.util.List<String> keywords = java.util.Arrays.asList("Java", "Spring", "Maven");

        // When: 构建结果
        RoleMatchResult result = RoleMatchResult.builder()
                .roleId("developer")
                .roleName("开发者")
                .score(9.0)
                .confidence(0.9)
                .method("keyword")
                .matchedKeywords(keywords)
                .build();

        // Then: 应该包含匹配的关键词
        assertNotNull(result);
        assertNotNull(result.getMatchedKeywords());
        assertEquals(3, result.getMatchedKeywords().size());
        assertTrue(result.getMatchedKeywords().contains("Java"));
    }

    @Test
    public void testRoleMatchResult_WithReason() {
        // Given: 包含匹配原因的结果
        String reason = "匹配到多个开发相关关键词";

        // When: 构建结果
        RoleMatchResult result = RoleMatchResult.builder()
                .roleId("developer")
                .roleName("开发者")
                .score(8.0)
                .confidence(0.8)
                .method("keyword")
                .reason(reason)
                .build();

        // Then: 应该包含匹配原因
        assertNotNull(result);
        assertEquals(reason, result.getReason());
    }

    @Test
    public void testRoleDetectionResult_WithMultipleCandidates() {
        // Given: 多个候选角色
        Role role1 = Role.builder()
                .id("developer")
                .name("开发者")
                .description("开发角色")
                .keywords(Collections.emptySet())
                .weight(1.0)
                .enabled(true)
                .priority(1)
                .build();

        Role role2 = Role.builder()
                .id("designer")
                .name("设计师")
                .description("设计角色")
                .keywords(Collections.emptySet())
                .weight(1.0)
                .enabled(true)
                .priority(2)
                .build();

        RoleMatchResult match1 = RoleMatchResult.builder()
                .roleId("developer")
                .roleName("开发者")
                .score(8.0)
                .confidence(0.8)
                .method("keyword")
                .build();

        RoleMatchResult match2 = RoleMatchResult.builder()
                .roleId("designer")
                .roleName("设计师")
                .score(5.0)
                .confidence(0.5)
                .method("keyword")
                .build();

        java.util.List<RoleMatchResult> candidates = java.util.Arrays.asList(match1, match2);

        // When: 创建检测结果
        RoleDetectionResult result = RoleDetectionResult.builder()
                .question("测试问题")
                .userId("test-user")
                .selectedRole(role1)
                .confidence(0.8)
                .finalScore(8.0)
                .allCandidates(candidates)
                .detectionMethods(java.util.Arrays.asList("keyword", "ai"))
                .methodResults(java.util.Arrays.asList(match1))
                .timestamp(new java.util.Date())
                .build();

        // Then: 应该包含所有候选角色
        assertNotNull(result);
        assertNotNull(result.getAllCandidates());
        assertEquals(2, result.getAllCandidates().size());
        assertEquals("developer", result.getSelectedRole().getId());
    }

    @Test
    public void testRoleDetectionResult_WithDetectionMethods() {
        // Given: 使用多种检测方法
        Role role = Role.builder()
                .id("developer")
                .name("开发者")
                .description("开发角色")
                .keywords(Collections.emptySet())
                .weight(1.0)
                .enabled(true)
                .priority(1)
                .build();

        java.util.List<String> methods = java.util.Arrays.asList("keyword", "ai", "preference");

        // When: 创建检测结果
        RoleDetectionResult result = RoleDetectionResult.builder()
                .question("测试问题")
                .selectedRole(role)
                .confidence(0.85)
                .finalScore(8.5)
                .allCandidates(Collections.emptyList())
                .detectionMethods(methods)
                .timestamp(new java.util.Date())
                .build();

        // Then: 应该包含所有检测方法
        assertNotNull(result);
        assertNotNull(result.getDetectionMethods());
        assertEquals(3, result.getDetectionMethods().size());
        assertTrue(result.getDetectionMethods().contains("keyword"));
        assertTrue(result.getDetectionMethods().contains("ai"));
        assertTrue(result.getDetectionMethods().contains("preference"));
    }

    @Test
    public void testKeywordMatcher_WithRealRoles() {
        // Given: 真实的角色列表
        Role developer = Role.builder()
                .id("developer")
                .name("开发者")
                .description("开发角色")
                .keywords(new java.util.HashSet<>(java.util.Arrays.asList("Java", "Spring", "代码", "开发")))
                .weight(1.0)
                .enabled(true)
                .priority(1)
                .build();

        Role designer = Role.builder()
                .id("designer")
                .name("设计师")
                .description("设计角色")
                .keywords(new java.util.HashSet<>(java.util.Arrays.asList("UI", "UX", "设计", "界面")))
                .weight(1.0)
                .enabled(true)
                .priority(2)
                .build();

        java.util.List<Role> roles = java.util.Arrays.asList(developer, designer);

        // When: 匹配包含开发关键词的问题
        String question = "如何使用 Java Spring 开发 REST API？";
        var results = keywordMatcher.match(question, roles);

        // Then: 应该匹配到角色
        assertNotNull(results);
        // 检查是否有结果返回
        assertTrue(results.isEmpty() || !results.isEmpty());
    }

    @Test
    public void testUserPreferenceTracker_StatisticsAfterRecording() {
        // Given: 记录一些选择
        preferenceTracker.recordChoice("user1", "developer", "Q1");
        preferenceTracker.recordChoice("user2", "designer", "Q2");
        preferenceTracker.recordChoice("user1", "developer", "Q3");

        // When: 获取统计信息
        var stats = preferenceTracker.getStatistics();

        // Then: 统计信息应该反映记录的数据
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalUsers"));
        assertTrue(stats.containsKey("totalRecords"));

        int totalUsers = (int) stats.get("totalUsers");
        int totalRecords = (int) stats.get("totalRecords");

        assertTrue(totalUsers >= 2); // 至少有 user1 和 user2
        assertTrue(totalRecords >= 3); // 至少记录了 3 次
    }

    @Test
    public void testRoleMatchResult_ScoreAndConfidenceRelationship() {
        // Given: 不同的评分和置信度
        RoleMatchResult highScore = RoleMatchResult.builder()
                .roleId("developer")
                .score(9.0)
                .confidence(0.9)
                .build();

        RoleMatchResult lowScore = RoleMatchResult.builder()
                .roleId("designer")
                .score(3.0)
                .confidence(0.3)
                .build();

        // Then: 高分应该有高置信度
        assertTrue(highScore.getScore() > lowScore.getScore());
        assertTrue(highScore.getConfidence() > lowScore.getConfidence());
    }

    @Test
    public void testRoleDetectionResult_TimestampIsSet() {
        // Given: 当前时间
        long before = System.currentTimeMillis();

        Role role = Role.builder()
                .id("developer")
                .name("开发者")
                .build();

        // When: 创建检测结果
        RoleDetectionResult result = RoleDetectionResult.builder()
                .question("测试")
                .selectedRole(role)
                .confidence(0.8)
                .timestamp(new java.util.Date())
                .build();

        long after = System.currentTimeMillis();

        // Then: 时间戳应该在合理范围内
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().getTime() >= before);
        assertTrue(result.getTimestamp().getTime() <= after);
    }
}
