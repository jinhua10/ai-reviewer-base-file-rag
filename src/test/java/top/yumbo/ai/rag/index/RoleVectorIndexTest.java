package top.yumbo.ai.rag.index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.role.Role;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleVectorIndex 单元测试
 * (RoleVectorIndex Unit Test)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class RoleVectorIndexTest {

    @TempDir
    Path tempDir;

    private Role testRole;
    private RoleVectorIndex index;

    @BeforeEach
    public void setUp() {
        // 创建测试角色 (Create test role)
        testRole = Role.builder()
                .id("developer")
                .name("开发者")
                .description("开发角色")
                .keywords(new HashSet<>(Arrays.asList("Java", "Spring")))
                .weight(1.0)
                .enabled(true)
                .priority(1)
                .build();

        // 创建索引 (Create index)
        String indexPath = tempDir.resolve("test_index.dat").toString();
        index = new RoleVectorIndex(testRole, indexPath);
    }

    /**
     * 创建测试用的768维向量 (Create 768-dim vector for testing)
     */
    private float[] createTestVector(float value) {
        float[] vector = new float[768];
        for (int i = 0; i < 768; i++) {
            vector[i] = value;
        }
        return vector;
    }

    @Test
    public void testInitialization() {
        // Then: 索引应该正确初始化
        assertNotNull(index);
        assertEquals(testRole, index.getRole());
        assertEquals(RoleVectorIndex.IndexStatus.UNLOADED, index.getStatus());
        assertEquals(0, index.getDocumentCount().get());
    }

    @Test
    public void testLoadIndex() throws IOException {
        // When: 加载索引
        index.load();

        // Then: 状态应该变为 LOADED
        assertEquals(RoleVectorIndex.IndexStatus.LOADED, index.getStatus());
        assertNotNull(index.getIndexEngine());
        assertNotNull(index.getLastLoadTime());
    }

    @Test
    public void testLoadAlreadyLoaded() throws IOException {
        // Given: 索引已加载
        index.load();

        // When: 再次加载
        index.load();

        // Then: 应该正常处理，不抛出异常
        assertEquals(RoleVectorIndex.IndexStatus.LOADED, index.getStatus());
    }

    @Test
    public void testUnloadIndex() throws IOException {
        // Given: 索引已加载
        index.load();

        // When: 卸载索引
        index.unload();

        // Then: 状态应该变为 UNLOADED
        assertEquals(RoleVectorIndex.IndexStatus.UNLOADED, index.getStatus());
        assertNull(index.getIndexEngine());
    }

    @Test
    public void testAddDocument() throws IOException {
        // Given: 索引已加载
        index.load();

        Document doc = Document.builder()
                .id("doc1")
                .title("Test Document")
                .content("Test content")
                .build();

        // When: 添加文档
        index.addDocument(doc, createTestVector(0.1f));

        // Then: 文档计数应该增加
        assertEquals(1, index.getDocumentCount().get());
    }

    @Test
    public void testAddDocumentWithoutLoad() {
        // Given: 索引未加载
        Document doc = Document.builder()
                .id("doc1")
                .title("Test")
                .content("Content")
                .build();

        // When & Then: 应该抛出异常
        assertThrows(IOException.class, () -> {
            index.addDocument(doc, createTestVector(0.1f));
        });
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        // Given: 添加一些文档
        index.load();

        Document doc1 = Document.builder()
                .id("doc1")
                .title("Document 1")
                .content("Content 1")
                .build();

        index.addDocument(doc1, createTestVector(0.1f));

        // When: 保存并卸载
        index.save();
        index.unload();

        // Then: 重新加载后文档计数应该保持
        index.load();
        assertEquals(1, index.getDocumentCount().get());
    }

    @Test
    public void testGetStatistics() throws IOException {
        // Given: 索引已加载
        index.load();

        // When: 获取统计信息
        IndexStatistics stats = index.getStatistics();

        // Then: 统计信息应该正确
        assertNotNull(stats);
        assertEquals(testRole.getId(), stats.getRoleId());
        assertEquals(testRole.getName(), stats.getRoleName());
        assertEquals(RoleVectorIndex.IndexStatus.LOADED, stats.getStatus());
        assertNotNull(stats.getCreatedAt());
    }

    @Test
    public void testClearIndex() throws IOException {
        // Given: 索引中有文档
        index.load();

        Document doc = Document.builder()
                .id("doc1")
                .title("Test")
                .content("Content")
                .build();

        index.addDocument(doc, createTestVector(0.1f));
        assertEquals(1, index.getDocumentCount().get());

        // When: 清空索引
        index.clear();

        // Then: 文档计数应该为0
        assertEquals(0, index.getDocumentCount().get());
    }

    @Test
    public void testBatchAddDocuments() throws IOException {
        // Given: 索引已加载
        index.load();

        List<Document> docs = new ArrayList<>();
        List<float[]> vectors = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            docs.add(Document.builder()
                    .id("doc" + i)
                    .title("Document " + i)
                    .content("Content " + i)
                    .build());
            vectors.add(createTestVector(0.1f * i));
        }

        // When: 批量添加
        index.addDocuments(docs, vectors);

        // Then: 文档计数应该正确
        assertEquals(5, index.getDocumentCount().get());
    }

    @Test
    public void testBatchAddWithMismatchSize() throws IOException {
        // Given: 索引已加载
        index.load();

        List<Document> docs = Arrays.asList(
                Document.builder().id("doc1").build()
        );
        List<float[]> vectors = Arrays.asList(
                createTestVector(0.1f),
                createTestVector(0.2f)
        );

        // When & Then: 应该抛出异常（文档数和向量数不匹配）
        assertThrows(IllegalArgumentException.class, () -> {
            index.addDocuments(docs, vectors);
        });
    }

    @Test
    public void testDeleteDocument() throws IOException {
        // Given: 索引中有文档
        index.load();

        Document doc = Document.builder()
                .id("doc1")
                .title("Test")
                .content("Content")
                .build();

        index.addDocument(doc, createTestVector(0.1f));
        assertEquals(1, index.getDocumentCount().get());

        // When: 删除文档
        boolean deleted = index.deleteDocument("doc1");

        // Then: 应该删除成功
        assertTrue(deleted);
        assertEquals(0, index.getDocumentCount().get());
    }

    @Test
    public void testDeleteNonExistentDocument() throws IOException {
        // Given: 索引已加载但为空
        index.load();

        // When: 删除不存在的文档
        boolean deleted = index.deleteDocument("non-existent");

        // Then: 应该返回 false
        assertFalse(deleted);
    }

    @Test
    public void testAccessTimeUpdates() throws IOException {
        // Given: 索引已加载
        index.load();
        assertNotNull(index.getLastAccessTime());

        // 记录初始访问时间
        var initialAccessTime = index.getLastAccessTime();

        // 等待一小段时间
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When: 添加文档
        Document doc = Document.builder()
                .id("doc1")
                .title("Test")
                .content("Content")
                .build();

        index.addDocument(doc, createTestVector(0.1f));

        // Then: 访问时间应该更新
        assertNotEquals(initialAccessTime, index.getLastAccessTime());
    }
}

