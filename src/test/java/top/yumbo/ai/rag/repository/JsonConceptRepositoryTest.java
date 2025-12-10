package top.yumbo.ai.rag.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.yumbo.ai.rag.concept.ConceptType;
import top.yumbo.ai.rag.concept.ConceptUnit;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON 概念仓库测试类 (JSON Concept Repository Test)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class JsonConceptRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonConceptRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new JsonConceptRepository();
        repository.storagePath = tempDir.toString();
        repository.init();
    }

    @Test
    public void testSaveAndFindById() {
        // 创建测试概念 (Create test concept)
        ConceptUnit concept = createTestConcept("test-001", "Docker");

        // 保存 (Save)
        ConceptUnit saved = repository.save(concept);

        assertNotNull(saved);
        assertEquals(concept.getId(), saved.getId());

        // 查找 (Find)
        Optional<ConceptUnit> found = repository.findById("test-001");

        assertTrue(found.isPresent());
        assertEquals("Docker", found.get().getName());
    }

    @Test
    public void testFindByName() {
        // 创建多个同名概念 (Create multiple concepts with same name)
        repository.save(createTestConcept("test-001", "Docker"));
        repository.save(createTestConcept("test-002", "Docker"));
        repository.save(createTestConcept("test-003", "Kubernetes"));

        // 按名称查找 (Find by name)
        List<ConceptUnit> dockerConcepts = repository.findByName("Docker");

        assertEquals(2, dockerConcepts.size());
    }

    @Test
    public void testFindByRoleId() {
        // 创建不同角色的概念 (Create concepts with different roles)
        ConceptUnit concept1 = createTestConcept("test-001", "Java");
        concept1.setRoleId("developer");
        repository.save(concept1);

        ConceptUnit concept2 = createTestConcept("test-002", "Python");
        concept2.setRoleId("developer");
        repository.save(concept2);

        ConceptUnit concept3 = createTestConcept("test-003", "ML Model");
        concept3.setRoleId("data_scientist");
        repository.save(concept3);

        // 按角色查找 (Find by role)
        List<ConceptUnit> devConcepts = repository.findByRoleId("developer");

        assertEquals(2, devConcepts.size());
    }

    @Test
    public void testFindAllEnabled() {
        // 创建启用和禁用的概念 (Create enabled and disabled concepts)
        ConceptUnit concept1 = createTestConcept("test-001", "Enabled 1");
        concept1.setEnabled(true);
        repository.save(concept1);

        ConceptUnit concept2 = createTestConcept("test-002", "Disabled");
        concept2.setEnabled(false);
        repository.save(concept2);

        ConceptUnit concept3 = createTestConcept("test-003", "Enabled 2");
        concept3.setEnabled(true);
        repository.save(concept3);

        // 查找启用的概念 (Find enabled concepts)
        List<ConceptUnit> enabled = repository.findAllEnabled();

        assertEquals(2, enabled.size());
        assertTrue(enabled.stream().allMatch(ConceptUnit::isEnabled));
    }

    @Test
    public void testFindNeedsReview() {
        // 创建需要审核的概念 (Create concepts needing review)
        ConceptUnit concept1 = createTestConcept("test-001", "Normal");
        concept1.setNeedsReview(false);
        repository.save(concept1);

        ConceptUnit concept2 = createTestConcept("test-002", "Needs Review");
        concept2.setNeedsReview(true);
        repository.save(concept2);

        // 查找需要审核的 (Find needs review)
        List<ConceptUnit> needsReview = repository.findNeedsReview();

        assertEquals(1, needsReview.size());
        assertEquals("test-002", needsReview.get(0).getId());
    }

    @Test
    public void testFindNeedsEvolution() {
        // 创建需要演化的概念 (Create concepts needing evolution)
        ConceptUnit concept1 = createTestConcept("test-001", "Healthy");
        concept1.setHealthScore(0.9);
        concept1.setDisputeCount(0);
        repository.save(concept1);

        ConceptUnit concept2 = createTestConcept("test-002", "Needs Evolution");
        concept2.setHealthScore(0.5);  // Low health score
        concept2.setDisputeCount(5);   // High dispute count
        repository.save(concept2);

        // 查找需要演化的 (Find needs evolution)
        List<ConceptUnit> needsEvolution = repository.findNeedsEvolution();

        assertEquals(1, needsEvolution.size());
        assertEquals("test-002", needsEvolution.get(0).getId());
    }

    @Test
    public void testSaveAll() {
        // 批量保存 (Batch save)
        List<ConceptUnit> concepts = Arrays.asList(
                createTestConcept("test-001", "Concept 1"),
                createTestConcept("test-002", "Concept 2"),
                createTestConcept("test-003", "Concept 3")
        );

        int count = repository.saveAll(concepts);

        assertEquals(3, count);
        assertEquals(3, repository.count());
    }

    @Test
    public void testDeleteById() {
        // 保存并删除 (Save and delete)
        ConceptUnit concept = createTestConcept("test-001", "To Delete");
        repository.save(concept);

        assertTrue(repository.existsById("test-001"));

        boolean deleted = repository.deleteById("test-001");

        assertTrue(deleted);
        assertFalse(repository.existsById("test-001"));
    }

    @Test
    public void testDeleteNonExistent() {
        // 删除不存在的概念 (Delete non-existent concept)
        boolean deleted = repository.deleteById("non-existent");

        assertFalse(deleted);
    }

    @Test
    public void testCount() {
        // 测试计数 (Test count)
        assertEquals(0, repository.count());

        repository.save(createTestConcept("test-001", "Concept 1"));
        assertEquals(1, repository.count());

        repository.save(createTestConcept("test-002", "Concept 2"));
        assertEquals(2, repository.count());

        repository.deleteById("test-001");
        assertEquals(1, repository.count());
    }

    @Test
    public void testClear() {
        // 测试清空 (Test clear)
        repository.save(createTestConcept("test-001", "Concept 1"));
        repository.save(createTestConcept("test-002", "Concept 2"));

        assertEquals(2, repository.count());

        repository.clear();

        assertEquals(0, repository.count());
    }

    @Test
    public void testPersistenceAcrossInstances() {
        // 测试持久化（跨实例） (Test persistence across instances)
        ConceptUnit concept = createTestConcept("test-001", "Persistent Concept");
        repository.save(concept);

        // 创建新实例 (Create new instance)
        JsonConceptRepository newRepository = new JsonConceptRepository();
        newRepository.storagePath = tempDir.toString();
        newRepository.init();

        // 验证数据已持久化 (Verify data is persisted)
        Optional<ConceptUnit> found = newRepository.findById("test-001");

        assertTrue(found.isPresent());
        assertEquals("Persistent Concept", found.get().getName());
    }

    @Test
    public void testUpdateConcept() {
        // 测试更新概念 (Test update concept)
        ConceptUnit concept = createTestConcept("test-001", "Original Name");
        repository.save(concept);

        // 修改并重新保存 (Modify and save again)
        concept.setName("Updated Name");
        concept.setDescription("Updated Description");
        repository.save(concept);

        // 验证更新 (Verify update)
        Optional<ConceptUnit> found = repository.findById("test-001");

        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertEquals("Updated Description", found.get().getDescription());
    }

    // ==================== 辅助方法 (Helper Methods) ====================

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

