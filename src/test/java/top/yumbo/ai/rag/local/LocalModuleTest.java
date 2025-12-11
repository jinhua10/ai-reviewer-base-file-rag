package top.yumbo.ai.rag.local;

import org.junit.jupiter.api.*;
import top.yumbo.ai.rag.local.LocalKnowledgeManager.LocalKnowledge;
import top.yumbo.ai.rag.local.LocalVectorIndex.VectorEntry;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4.5.1 本地优先架构测试
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@DisplayName("Phase 4.5.1 - 本地优先架构测试")
class LocalModuleTest {

    private LocalKnowledgeManager knowledgeManager;
    private LocalStorageEngine storageEngine;
    private LocalVectorIndex vectorIndex;
    private OfflineMode offlineMode;

    private static final String TEST_USER_ID = "test-user-001";
    private static final String TEST_ROLE_ID = "developer";

    @BeforeEach
    void setUp() {
        knowledgeManager = new LocalKnowledgeManager(TEST_USER_ID);
        storageEngine = new LocalStorageEngine();
        vectorIndex = new LocalVectorIndex();
        offlineMode = new OfflineMode();
        offlineMode.setCheckInterval(60); // 减少测试时的检测频率
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        offlineMode.shutdown();
    }

    // ========== LocalKnowledgeManager 测试 ==========

    @Test
    @DisplayName("测试本地知识库初始化")
    void testLocalKnowledgeManagerInit() {
        // Then
        assertNotNull(knowledgeManager);
        assertEquals(TEST_USER_ID, knowledgeManager.getUserId());

        // 验证目录是否创建
        String userPath = knowledgeManager.getUserDataPath(TEST_USER_ID);
        assertTrue(new File(userPath).exists());
    }

    @Test
    @DisplayName("测试获取用户数据路径")
    void testGetUserDataPath() {
        // When
        String path = knowledgeManager.getUserDataPath(TEST_USER_ID);

        // Then
        assertNotNull(path);
        assertTrue(path.contains(TEST_USER_ID));
    }

    @Test
    @DisplayName("测试获取角色数据路径")
    void testGetRoleDataPath() {
        // When
        String path = knowledgeManager.getRoleDataPath(TEST_USER_ID, TEST_ROLE_ID);

        // Then
        assertNotNull(path);
        assertTrue(path.contains(TEST_USER_ID));
        assertTrue(path.contains(TEST_ROLE_ID));
    }

    @Test
    @DisplayName("测试获取统计信息")
    void testGetStatistics() {
        // When
        var stats = knowledgeManager.getStatistics(TEST_USER_ID);

        // Then
        assertNotNull(stats);
        assertEquals(TEST_USER_ID, stats.getUserId());
        assertNotNull(stats.getUpdateTime());
    }

    // ========== LocalStorageEngine 测试 ==========

    @Test
    @DisplayName("测试存储引擎初始化")
    void testStorageEngineInit() {
        // Then
        assertNotNull(storageEngine);
        assertNotNull(storageEngine.getObjectMapper());
    }

    @Test
    @DisplayName("测试保存和加载对象")
    void testSaveAndLoad() {
        // Given
        String testPath = "./test-data/test-object.json";
        TestObject testObj = new TestObject("test-id", "test-content");

        // When
        storageEngine.save(testObj, testPath);
        TestObject loaded = storageEngine.load(testPath, TestObject.class);

        // Then
        assertNotNull(loaded);
        assertEquals(testObj.getId(), loaded.getId());
        assertEquals(testObj.getContent(), loaded.getContent());

        // Cleanup
        storageEngine.delete(testPath);
    }

    @Test
    @DisplayName("测试文件是否存在")
    void testFileExists() {
        // Given
        String testPath = "./test-data/exists-test.json";

        // When
        boolean beforeSave = storageEngine.exists(testPath);
        storageEngine.save(new TestObject("id", "content"), testPath);
        boolean afterSave = storageEngine.exists(testPath);

        // Then
        assertFalse(beforeSave);
        assertTrue(afterSave);

        // Cleanup
        storageEngine.delete(testPath);
    }

    // ========== LocalVectorIndex 测试 ==========

    @Test
    @DisplayName("测试向量索引初始化")
    void testVectorIndexInit() {
        // Then
        assertNotNull(vectorIndex);
        assertEquals(768, vectorIndex.getDimension());
    }

    @Test
    @DisplayName("测试添加向量")
    void testAddVector() {
        // Given
        VectorEntry entry = new VectorEntry();
        entry.setDocumentId("doc-001");
        entry.setRoleId(TEST_ROLE_ID);
        entry.setVector(createRandomVector(768));

        // When
        vectorIndex.addVector(entry);

        // Then
        assertEquals(1, vectorIndex.getVectorIndex().size());
        assertTrue(vectorIndex.getVectorIndex().containsKey("doc-001"));
    }

    @Test
    @DisplayName("测试向量搜索")
    void testVectorSearch() {
        // Given
        // 添加测试向量
        for (int i = 0; i < 10; i++) {
            VectorEntry entry = new VectorEntry();
            entry.setDocumentId("doc-" + i);
            entry.setRoleId(TEST_ROLE_ID);
            entry.setVector(createRandomVector(768));
            vectorIndex.addVector(entry);
        }

        // When
        float[] queryVector = createRandomVector(768);
        var results = vectorIndex.search(queryVector, 5);

        // Then
        assertNotNull(results);
        assertTrue(results.size() <= 5);
    }

    @Test
    @DisplayName("测试按角色搜索")
    void testSearchByRole() {
        // Given
        VectorEntry entry1 = new VectorEntry();
        entry1.setDocumentId("doc-dev-001");
        entry1.setRoleId("developer");
        entry1.setVector(createRandomVector(768));
        vectorIndex.addVector(entry1);

        VectorEntry entry2 = new VectorEntry();
        entry2.setDocumentId("doc-test-001");
        entry2.setRoleId("tester");
        entry2.setVector(createRandomVector(768));
        vectorIndex.addVector(entry2);

        // When
        float[] queryVector = createRandomVector(768);
        var devResults = vectorIndex.search(queryVector, 10, "developer");
        var testResults = vectorIndex.search(queryVector, 10, "tester");

        // Then
        assertEquals(1, devResults.size());
        assertEquals(1, testResults.size());
        assertEquals("doc-dev-001", devResults.get(0).getDocumentId());
        assertEquals("doc-test-001", testResults.get(0).getDocumentId());
    }

    @Test
    @DisplayName("测试获取索引统计")
    void testGetIndexStats() {
        // Given
        for (int i = 0; i < 5; i++) {
            VectorEntry entry = new VectorEntry();
            entry.setDocumentId("doc-" + i);
            entry.setVector(createRandomVector(768));
            vectorIndex.addVector(entry);
        }

        // When
        var stats = vectorIndex.getStats();

        // Then
        assertNotNull(stats);
        assertEquals(5, stats.getTotalVectors());
        assertEquals(768, stats.getDimension());
        assertTrue(stats.getEstimatedMemoryBytes() > 0);
    }

    // ========== OfflineMode 测试 ==========

    @Test
    @DisplayName("测试离线模式初始化")
    void testOfflineModeInit() {
        // Then
        assertNotNull(offlineMode);
        assertFalse(offlineMode.isOffline());
    }

    @Test
    @DisplayName("测试手动进入离线模式")
    void testEnterOfflineMode() {
        // When
        offlineMode.enterOfflineMode();

        // Then
        assertTrue(offlineMode.isOffline());
        assertFalse(offlineMode.isOnline());
    }

    @Test
    @DisplayName("测试降级策略")
    void testDegradationPolicy() {
        // When - 在线模式
        var onlinePolicy = offlineMode.getDegradationPolicy();

        // Then
        assertTrue(onlinePolicy.isLocalSearchEnabled());
        assertTrue(onlinePolicy.isOnlineSearchEnabled());

        // When - 离线模式
        offlineMode.enterOfflineMode();
        var offlinePolicy = offlineMode.getDegradationPolicy();

        // Then
        assertTrue(offlinePolicy.isLocalSearchEnabled());
        assertFalse(offlinePolicy.isOnlineSearchEnabled());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建随机向量
     */
    private float[] createRandomVector(int dimension) {
        Random random = new Random();
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }

    /**
     * 测试对象
     */
    static class TestObject {
        private String id;
        private String content;

        public TestObject() {}

        public TestObject(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

