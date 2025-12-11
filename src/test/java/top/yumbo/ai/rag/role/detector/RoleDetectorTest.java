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
}

