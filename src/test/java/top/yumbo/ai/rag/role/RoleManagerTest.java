package top.yumbo.ai.rag.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色管理器测试类 (Role Manager Test)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class RoleManagerTest {

    private RoleManager roleManager;
    private RoleConfig roleConfig;

    @BeforeEach
    public void setUp() {
        // 初始化配置和管理器 (Initialize config and manager)
        roleConfig = new RoleConfig();
        roleConfig.setEnabled(true);
        roleConfig.setDefaultRole("general");
        roleConfig.setConfigPath("config/roles.yml");
        roleConfig.init();

        roleManager = new RoleManager();
        roleManager.roleConfig = roleConfig;
        roleManager.init();
    }

    @Test
    public void testGetRole() {
        // 测试获取开发者角色 (Test getting developer role)
        Role developer = roleManager.getRole("developer");
        assertNotNull(developer, "开发者角色不应为空 (Developer role should not be null)");
        assertEquals("developer", developer.getId());
        assertEquals("开发者 (Developer)", developer.getName());
        assertTrue(developer.isEnabled());
    }

    @Test
    public void testGetDefaultRole() {
        // 测试获取默认角色 (Test getting default role)
        Role defaultRole = roleManager.getDefaultRole();
        assertNotNull(defaultRole, "默认角色不应为空 (Default role should not be null)");
        assertEquals("general", defaultRole.getId());
    }

    @Test
    public void testGetEnabledRoles() {
        // 测试获取所有启用的角色 (Test getting all enabled roles)
        List<Role> enabledRoles = roleManager.getEnabledRoles();
        assertNotNull(enabledRoles);
        assertFalse(enabledRoles.isEmpty(), "启用角色列表不应为空 (Enabled roles list should not be empty)");

        // 验证角色按优先级降序排列 (Verify roles are sorted by priority descending)
        for (int i = 0; i < enabledRoles.size() - 1; i++) {
            assertTrue(enabledRoles.get(i).getPriority() >= enabledRoles.get(i + 1).getPriority(),
                    "角色应按优先级降序排列 (Roles should be sorted by priority descending)");
        }
    }

    @Test
    public void testHasRole() {
        // 测试角色存在性检查 (Test role existence check)
        assertTrue(roleManager.hasRole("developer"), "应该存在开发者角色 (Developer role should exist)");
        assertTrue(roleManager.hasRole("data_scientist"), "应该存在数据科学家角色 (Data scientist role should exist)");
        assertFalse(roleManager.hasRole("non_existent"), "不存在的角色应返回 false (Non-existent role should return false)");
    }

    @Test
    public void testRecordUsage() {
        // 测试记录角色使用 (Test recording role usage)
        String roleId = "developer";
        int initialCount = roleManager.getUsageStats().getOrDefault(roleId, 0);

        roleManager.recordUsage(roleId);
        roleManager.recordUsage(roleId);

        int finalCount = roleManager.getUsageStats().get(roleId);
        assertEquals(initialCount + 2, finalCount, "使用次数应增加2 (Usage count should increase by 2)");
    }

    @Test
    public void testFindRolesByKeywords() {
        // 测试通过关键词查找角色 (Test finding roles by keywords)
        List<String> keywords = Arrays.asList("代码", "编程");
        List<Role> matchedRoles = roleManager.findRolesByKeywords(keywords);

        assertNotNull(matchedRoles);
        assertFalse(matchedRoles.isEmpty(), "应该找到匹配的角色 (Should find matched roles)");

        // 验证开发者角色在匹配列表中 (Verify developer role is in matched list)
        boolean hasDeveloper = matchedRoles.stream()
                .anyMatch(role -> "developer".equals(role.getId()));
        assertTrue(hasDeveloper, "应该匹配到开发者角色 (Should match developer role)");
    }

    @Test
    public void testRoleConfiguration() {
        // 测试角色配置 (Test role configuration)
        assertTrue(roleConfig.isEnabled(), "角色系统应该启用 (Role system should be enabled)");
        assertNotNull(roleConfig.getDefaultRole(), "默认角色ID不应为空 (Default role ID should not be null)");
        assertFalse(roleConfig.getRoles().isEmpty(), "角色列表不应为空 (Roles list should not be empty)");
    }

    @Test
    public void testRoleProperties() {
        // 测试角色属性 (Test role properties)
        Role developer = roleManager.getRole("developer");

        assertNotNull(developer.getKeywords(), "关键词列表不应为空 (Keywords list should not be null)");
        assertFalse(developer.getKeywords().isEmpty(), "关键词列表不应为空 (Keywords list should not be empty)");

        assertNotNull(developer.getPrompt(), "提示词不应为空 (Prompt should not be null)");
        assertFalse(developer.getPrompt().isEmpty(), "提示词不应为空 (Prompt should not be empty)");

        assertNotNull(developer.getIndexPath(), "索引路径不应为空 (Index path should not be null)");
        assertTrue(developer.getWeight() > 0, "权重应该大于0 (Weight should be greater than 0)");
        assertTrue(developer.getPriority() >= 0, "优先级应该大于等于0 (Priority should be >= 0)");
    }
}

