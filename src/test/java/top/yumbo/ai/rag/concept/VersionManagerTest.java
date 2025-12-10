package top.yumbo.ai.rag.concept;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 版本管理器测试类 (Version Manager Test)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class VersionManagerTest {

    private VersionManager versionManager;

    @BeforeEach
    public void setUp() {
        versionManager = new VersionManager();
    }

    /**
     * 测试创建初始版本 (Test creating initial version)
     */
    @Test
    public void testCreateInitialVersion() {
        // 创建测试概念 (Create test concept)
        ConceptUnit concept = createTestConcept("test-001", "Docker");

        // 创建初始版本 (Create initial version)
        ConceptHistory history = versionManager.createInitialVersion(concept);

        assertNotNull(history, "历史记录不应为空 (History should not be null)");
        assertEquals(1, history.getCurrentVersion(), "初始版本号应为1 (Initial version should be 1)");
        assertEquals(1, history.getVersionCount(), "版本数量应为1 (Version count should be 1)");

        ConceptVersion initialVersion = history.getLatestVersion();
        assertNotNull(initialVersion, "初始版本不应为空 (Initial version should not be null)");
        assertEquals(ConceptVersion.ChangeType.CREATE, initialVersion.getChangeType());
        assertTrue(initialVersion.isStable(), "初始版本应标记为稳定 (Initial version should be stable)");
    }

    /**
     * 测试创建新版本 (Test creating new version)
     */
    @Test
    public void testCreateNewVersion() {
        // 创建初始版本 (Create initial version)
        ConceptUnit concept = createTestConcept("test-002", "Kubernetes");
        versionManager.createInitialVersion(concept);

        // 修改概念并创建新版本 (Modify concept and create new version)
        concept.setDescription("Updated description");
        ConceptVersion newVersion = versionManager.createNewVersion(
                concept,
                ConceptVersion.ChangeType.UPDATE,
                "更新描述 (Update description)",
                "内容改进 (Content improvement)",
                "testUser"
        );

        assertNotNull(newVersion, "新版本不应为空 (New version should not be null)");
        assertEquals(2, newVersion.getVersionNumber(), "版本号应为2 (Version number should be 2)");
        assertEquals(2, concept.getVersion(), "概念版本号应更新为2 (Concept version should be updated to 2)");
        assertTrue(concept.isHasHistory(), "概念应有历史记录 (Concept should have history)");

        ConceptHistory history = versionManager.getHistory(concept.getId());
        assertEquals(2, history.getVersionCount(), "应有2个版本 (Should have 2 versions)");
    }

    /**
     * 测试获取版本 (Test getting versions)
     */
    @Test
    public void testGetVersions() {
        ConceptUnit concept = createTestConcept("test-003", "Spring Boot");
        versionManager.createInitialVersion(concept);

        // 创建多个版本 (Create multiple versions)
        for (int i = 2; i <= 5; i++) {
            concept.setDescription("Description v" + i);
            versionManager.createNewVersion(
                    concept,
                    ConceptVersion.ChangeType.UPDATE,
                    "Update " + i,
                    "Iteration " + i,
                    "testUser"
            );
        }

        // 测试获取特定版本 (Test getting specific version)
        ConceptVersion v3 = versionManager.getVersion(concept.getId(), 3);
        assertNotNull(v3, "版本3应存在 (Version 3 should exist)");
        assertEquals(3, v3.getVersionNumber());

        // 测试获取最新版本 (Test getting latest version)
        ConceptVersion latest = versionManager.getLatestVersion(concept.getId());
        assertNotNull(latest);
        assertEquals(5, latest.getVersionNumber(), "最新版本应为5 (Latest version should be 5)");

        // 测试获取最近版本 (Test getting recent versions)
        var recentVersions = versionManager.getRecentVersions(concept.getId(), 3);
        assertEquals(3, recentVersions.size(), "应返回最近3个版本 (Should return recent 3 versions)");
        assertEquals(5, recentVersions.get(2).getVersionNumber(), "最后一个应是版本5 (Last one should be v5)");
    }

    /**
     * 测试版本回滚 (Test version rollback)
     */
    @Test
    public void testRollbackVersion() {
        ConceptUnit concept = createTestConcept("test-004", "Redis");
        versionManager.createInitialVersion(concept);

        // 创建几个版本 (Create several versions)
        concept.setDescription("Version 2 description");
        versionManager.createNewVersion(concept, ConceptVersion.ChangeType.UPDATE, "v2", "update", "user1");

        concept.setDescription("Version 3 description");
        versionManager.createNewVersion(concept, ConceptVersion.ChangeType.UPDATE, "v3", "update", "user1");

        // 回滚到版本2 (Rollback to version 2)
        ConceptUnit rolledBack = versionManager.rollbackToVersion(concept.getId(), 2, "admin");

        assertNotNull(rolledBack, "回滚后的概念不应为空 (Rolled back concept should not be null)");
        assertEquals("Version 2 description", rolledBack.getDescription());

        // 回滚会创建新版本 (Rollback creates new version)
        ConceptHistory history = versionManager.getHistory(concept.getId());
        assertEquals(4, history.getVersionCount(), "回滚后应有4个版本 (Should have 4 versions after rollback)");
    }

    /**
     * 测试版本历史查询 (Test version history query)
     */
    @Test
    public void testVersionHistory() {
        ConceptUnit concept = createTestConcept("test-005", "MongoDB");
        ConceptHistory history = versionManager.createInitialVersion(concept);

        assertTrue(history.hasVersions(), "应有版本 (Should have versions)");
        assertNotNull(history.getCreationVersion(), "应有创建版本 (Should have creation version)");

        // 创建不同类型的版本 (Create different types of versions)
        concept.setDescription("Fixed description");
        versionManager.createNewVersion(concept, ConceptVersion.ChangeType.FIX, "fix", "bug fix", "user1");

        concept.setDescription("Evolved description");
        versionManager.createNewVersion(concept, ConceptVersion.ChangeType.EVOLUTION, "evolution", "concept evolved", "system");

        history = versionManager.getHistory(concept.getId());
        assertEquals(1, history.getFixCount(), "应有1个修正版本 (Should have 1 fix version)");
        assertEquals(1, history.getEvolutionCount(), "应有1个演化版本 (Should have 1 evolution version)");
    }

    /**
     * 测试版本清理 (Test version cleanup)
     */
    @Test
    public void testVersionCleanup() {
        ConceptUnit concept = createTestConcept("test-006", "PostgreSQL");
        versionManager.createInitialVersion(concept);

        // 创建大量版本以触发清理 (Create many versions to trigger cleanup)
        for (int i = 2; i <= 65; i++) {
            concept.setDescription("Description v" + i);
            versionManager.createNewVersion(
                    concept,
                    ConceptVersion.ChangeType.UPDATE,
                    "Update " + i,
                    "Iteration",
                    "testUser"
            );
        }

        ConceptHistory history = versionManager.getHistory(concept.getId());

        // 清理应该已经自动触发 (Cleanup should have been triggered automatically)
        assertTrue(history.getVersionCount() < 65,
                "版本数量应少于65（清理后） (Version count should be less than 65 after cleanup)");

        // 创建版本应该被保留 (Creation version should be kept)
        assertNotNull(history.getCreationVersion(),
                "创建版本应被保留 (Creation version should be kept)");
    }

    /**
     * 测试概念单元的业务方法 (Test ConceptUnit business methods)
     */
    @Test
    public void testConceptUnitMethods() {
        ConceptUnit concept = createTestConcept("test-007", "Kafka");

        // 测试访问次数 (Test access count)
        assertEquals(0, concept.getAccessCount());
        concept.incrementAccessCount();
        assertEquals(1, concept.getAccessCount());
        assertNotNull(concept.getLastAccessedAt());

        // 测试争议处理 (Test dispute handling)
        assertEquals(0, concept.getDisputeCount());
        assertEquals(1.0, concept.getHealthScore(), 0.001);

        concept.incrementDisputeCount();
        assertEquals(1, concept.getDisputeCount());
        assertTrue(concept.isNeedsReview());
        assertEquals(0.9, concept.getHealthScore(), 0.001);

        // 测试演化检查 (Test evolution check)
        assertFalse(concept.needsEvolution());

        concept.incrementDisputeCount();
        concept.incrementDisputeCount();
        assertTrue(concept.needsEvolution(), "3次争议后应需要演化 (Should need evolution after 3 disputes)");

        // 测试层次检查 (Test hierarchy check)
        assertTrue(concept.isRoot(), "应是根节点 (Should be root)");
        assertTrue(concept.isLeaf(), "应是叶子节点 (Should be leaf)");

        concept.setParentId("parent-001");
        concept.getChildIds().add("child-001");
        assertFalse(concept.isRoot(), "不应是根节点 (Should not be root)");
        assertFalse(concept.isLeaf(), "不应是叶子节点 (Should not be leaf)");
    }

    /**
     * 测试概念类型 (Test ConceptType)
     */
    @Test
    public void testConceptType() {
        // 测试枚举转换 (Test enum conversion)
        assertEquals(ConceptType.TECHNICAL, ConceptType.fromString("TECHNICAL"));
        assertEquals(ConceptType.TECHNICAL, ConceptType.fromString("技术"));
        assertEquals(ConceptType.GENERAL, ConceptType.fromString("unknown"));
        assertEquals(ConceptType.GENERAL, ConceptType.fromString(null));

        // 测试名称获取 (Test name retrieval)
        assertEquals("技术", ConceptType.TECHNICAL.getChineseName());
        assertEquals("Technical", ConceptType.TECHNICAL.getEnglishName());
    }

    /**
     * 测试统计信息 (Test statistics)
     */
    @Test
    public void testStatistics() {
        // 创建多个概念 (Create multiple concepts)
        for (int i = 1; i <= 5; i++) {
            ConceptUnit concept = createTestConcept("test-stat-" + i, "Concept " + i);
            versionManager.createInitialVersion(concept);

            // 为每个概念创建几个版本 (Create several versions for each concept)
            for (int j = 2; j <= 3; j++) {
                concept.setDescription("Version " + j);
                versionManager.createNewVersion(
                        concept,
                        ConceptVersion.ChangeType.UPDATE,
                        "Update",
                        "Test",
                        "user"
                );
            }
        }

        var stats = versionManager.getStatistics();

        assertEquals(5, stats.get("totalConcepts"), "应有5个概念 (Should have 5 concepts)");
        assertEquals(15, stats.get("totalVersions"), "应有15个版本 (Should have 15 versions total)");
        assertEquals(3.0, (Double) stats.get("avgVersionsPerConcept"), 0.001,
                "平均每个概念3个版本 (Average 3 versions per concept)");
    }

    // ==================== 辅助方法 (Helper Methods) ====================

    /**
     * 创建测试概念 (Create test concept)
     */
    private ConceptUnit createTestConcept(String id, String name) {
        return ConceptUnit.builder()
                .id(id)
                .name(name)
                .type(ConceptType.TECHNICAL)
                .level(2)
                .roleId("developer")
                .definition("Definition of " + name)
                .description("Description of " + name)
                .keywords(Arrays.asList("keyword1", "keyword2"))
                .examples(Arrays.asList("example1", "example2"))
                .importance(0.8)
                .enabled(true)
                .build();
    }
}

