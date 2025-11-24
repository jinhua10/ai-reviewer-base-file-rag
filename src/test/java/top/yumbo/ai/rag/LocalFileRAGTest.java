package top.yumbo.ai.rag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.util.DocumentUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalFileRAG 集成测试
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
class LocalFileRAGTest {

    private LocalFileRAG rag;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时目录
        tempDir = Files.createTempDirectory("rag-test-");

        // 创建配置
        RAGConfiguration config = RAGConfiguration.builder()
                .storage(RAGConfiguration.StorageConfig.builder()
                        .basePath(tempDir.toString())
                        .compression(false)
                        .build())
                .cache(RAGConfiguration.CacheConfig.builder()
                        .enabled(true)
                        .documentCacheSize(100)
                        .queryCacheSize(100)
                        .build())
                .build();

        // 初始化RAG
        rag = LocalFileRAG.builder()
                .configuration(config)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (rag != null) {
            rag.close();
        }

        // 清理临时目录
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                // ignore
                            }
                        });
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    void testIndexAndSearch() {
        // 创建测试文档
        Document doc1 = DocumentUtils.fromText(
                "Java编程指南",
                "Java是一门面向对象的编程语言，广泛应用于企业级应用开发。"
        );
        doc1.setCategory("技术");

        Document doc2 = DocumentUtils.fromText(
                "Python入门",
                "Python是一门简洁优雅的编程语言，适合初学者学习。"
        );
        doc2.setCategory("技术");

        // 索引文档
        String id1 = rag.index(doc1);
        String id2 = rag.index(doc2);

        assertNotNull(id1);
        assertNotNull(id2);

        // 提交索引
        rag.commit();

        // 搜索
        Query query = Query.builder()
                .queryText("编程语言")
                .limit(10)
                .build();

        SearchResult result = rag.search(query);

        assertNotNull(result);
        assertTrue(result.getTotalHits() >= 2);
        assertEquals(2, result.getDocuments().size());
    }

    @Test
    void testGetDocument() {
        // 创建并索引文档
        Document doc = DocumentUtils.fromText(
                "测试文档",
                "这是一个测试文档的内容。"
        );

        String id = rag.index(doc);
        rag.commit();

        // 获取文档
        Document retrieved = rag.getDocument(id);

        assertNotNull(retrieved);
        assertEquals("测试文档", retrieved.getTitle());
        assertEquals("这是一个测试文档的内容。", retrieved.getContent());
    }

    @Test
    void testUpdateDocument() {
        // 创建并索引文档
        Document doc = DocumentUtils.fromText(
                "原始标题",
                "原始内容"
        );

        String id = rag.index(doc);
        rag.commit();

        // 更新文档
        Document updatedDoc = DocumentUtils.fromText(
                "新标题",
                "新内容"
        );

        boolean updated = rag.updateDocument(id, updatedDoc);
        assertTrue(updated);

        rag.commit();

        // 验证更新
        Document retrieved = rag.getDocument(id);
        assertNotNull(retrieved);
        assertEquals("新标题", retrieved.getTitle());
        assertEquals("新内容", retrieved.getContent());
    }

    @Test
    void testDeleteDocument() {
        // 创建并索引文档
        Document doc = DocumentUtils.fromText(
                "待删除文档",
                "这个文档将被删除"
        );

        String id = rag.index(doc);
        rag.commit();

        // 删除文档
        boolean deleted = rag.deleteDocument(id);
        assertTrue(deleted);

        rag.commit();

        // 验证删除
        Document retrieved = rag.getDocument(id);
        assertNull(retrieved);
    }

    @Test
    void testSearchWithFilter() {
        // 创建不同分类的文档
        Document doc1 = DocumentUtils.fromText("技术文章", "关于Java的技术文章");
        doc1.setCategory("技术");

        Document doc2 = DocumentUtils.fromText("生活随笔", "关于生活的随笔");
        doc2.setCategory("生活");

        rag.index(doc1);
        rag.index(doc2);
        rag.commit();

        // 带过滤条件搜索
        Query query = Query.builder()
                .queryText("文章")
                .build();
        query.withFilter("category", "技术");

        SearchResult result = rag.search(query);

        assertNotNull(result);
        assertTrue(result.getTotalHits() >= 1);

        // 验证结果都是技术分类
        result.getDocuments().forEach(doc -> {
            assertEquals("技术", doc.getCategory());
        });
    }

    @Test
    void testStatistics() {
        // 索引一些文档
        for (int i = 0; i < 5; i++) {
            Document doc = DocumentUtils.fromText(
                    "文档" + i,
                    "内容" + i
            );
            rag.index(doc);
        }

        rag.commit();

        // 获取统计信息
        LocalFileRAG.Statistics stats = rag.getStatistics();

        assertNotNull(stats);
        assertEquals(5, stats.getDocumentCount());
        assertEquals(5, stats.getIndexedDocumentCount());
    }
}

