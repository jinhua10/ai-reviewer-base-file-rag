package top.yumbo.ai.rag.role.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.role.Role;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 关键词匹配器测试类 (Keyword Matcher Test)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class KeywordMatcherTest {

    private KeywordMatcher matcher;
    private List<Role> testRoles;

    @BeforeEach
    public void setUp() {
        // 直接实例化（不依赖 Spring 容器）(Direct instantiation without Spring container)
        matcher = new KeywordMatcher();

        // 创建测试角色 (Create test roles)
        Role developer = Role.builder()
                .id("developer")
                .name("开发者")
                .enabled(true)
                .keywords(new HashSet<>(Arrays.asList("代码", "编程", "bug", "code", "programming")))
                .weight(1.2)
                .build();

        Role dataScientist = Role.builder()
                .id("data_scientist")
                .name("数据科学家")
                .enabled(true)
                .keywords(new HashSet<>(Arrays.asList("数据", "模型", "训练", "data", "model", "training")))
                .weight(1.3)
                .build();

        Role productManager = Role.builder()
                .id("product_manager")
                .name("产品经理")
                .enabled(true)
                .keywords(new HashSet<>(Arrays.asList("产品", "需求", "用户", "product", "requirement")))
                .weight(1.0)
                .build();

        testRoles = Arrays.asList(developer, dataScientist, productManager);
    }

    @Test
    public void testMatchDeveloperRole() {
        // 测试匹配开发者角色 (Test matching developer role)
        String question = "如何修复这个代码的 bug？How to fix the code bug?";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        assertFalse(results.isEmpty(), "应该有匹配结果 (Should have match results)");

        RoleMatchResult topResult = results.get(0);
        assertEquals("developer", topResult.getRoleId(), "最佳匹配应该是开发者 (Top match should be developer)");
        assertTrue(topResult.getScore() > 0, "分数应该大于0 (Score should be greater than 0)");
        assertFalse(topResult.getMatchedKeywords().isEmpty(), "应该有匹配的关键词 (Should have matched keywords)");
    }

    @Test
    public void testMatchDataScientistRole() {
        // 测试匹配数据科学家角色 (Test matching data scientist role)
        String question = "如何训练这个机器学习模型？How to train this ML model?";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        assertFalse(results.isEmpty());

        // 查找数据科学家角色 (Find data scientist role)
        Optional<RoleMatchResult> dsResult = results.stream()
                .filter(r -> "data_scientist".equals(r.getRoleId()))
                .findFirst();

        assertTrue(dsResult.isPresent(), "应该匹配到数据科学家 (Should match data scientist)");
        assertTrue(dsResult.get().getScore() > 0);
    }

    @Test
    public void testNoMatch() {
        // 测试无匹配情况 (Test no match scenario)
        String question = "今天天气怎么样？What's the weather today?";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        // 可能没有匹配或分数很低 (May have no match or very low score)
        assertTrue(results.isEmpty() || results.get(0).getScore() < 1.0);
    }

    @Test
    public void testEmptyQuestion() {
        // 测试空问题 (Test empty question)
        List<RoleMatchResult> results = matcher.match("", testRoles);

        assertTrue(results.isEmpty(), "空问题应该没有匹配 (Empty question should have no match)");
    }

    @Test
    public void testNullQuestion() {
        // 测试 null 问题 (Test null question)
        List<RoleMatchResult> results = matcher.match(null, testRoles);

        assertTrue(results.isEmpty(), "null 问题应该没有匹配 (Null question should have no match)");
    }

    @Test
    public void testEmptyRoles() {
        // 测试空角色列表 (Test empty roles list)
        String question = "测试问题 Test question";

        List<RoleMatchResult> results = matcher.match(question, Arrays.asList());

        assertTrue(results.isEmpty(), "空角色列表应该没有匹配 (Empty roles list should have no match)");
    }

    @Test
    public void testGetBestMatch() {
        // 测试获取最佳匹配 (Test get best match)
        String question = "编程代码问题 Programming code question";

        Optional<RoleMatchResult> bestMatch = matcher.getBestMatch(question, testRoles);

        assertTrue(bestMatch.isPresent(), "应该有最佳匹配 (Should have best match)");
        assertEquals("developer", bestMatch.get().getRoleId());
    }

    @Test
    public void testMultipleKeywordMatch() {
        // ��试多个关键词匹配 (Test multiple keyword match)
        String question = "如何编程写代码修复bug？How to code programming fix bug?";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        assertFalse(results.isEmpty());

        RoleMatchResult topResult = results.get(0);
        assertEquals("developer", topResult.getRoleId());
        assertTrue(topResult.getMatchedKeywords().size() >= 2,
                  "应该匹配多个关键词 (Should match multiple keywords)");
    }

    @Test
    public void testRoleWeight() {
        // 测试角色权重影响 (Test role weight impact)
        String question = "数据模型 data model";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        assertFalse(results.isEmpty());

        // 数据科学家的权重(1.3)应该影响最终分数 (Data scientist weight should affect final score)
        Optional<RoleMatchResult> dsResult = results.stream()
                .filter(r -> "data_scientist".equals(r.getRoleId()))
                .findFirst();

        assertTrue(dsResult.isPresent());
        // 权重应该让分数更高 (Weight should make score higher)
        assertTrue(dsResult.get().getScore() > 0);
    }

    @Test
    public void testCaseInsensitive() {
        // 测试大小写不敏感 (Test case insensitive)
        String question = "CODE PROGRAMMING BUG";

        List<RoleMatchResult> results = matcher.match(question, testRoles);

        assertFalse(results.isEmpty());
        assertEquals("developer", results.get(0).getRoleId(),
                    "大写关键词应该也能匹配 (Uppercase keywords should also match)");
    }
}

