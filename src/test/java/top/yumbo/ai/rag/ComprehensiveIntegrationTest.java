package top.yumbo.ai.rag;
import org.junit.jupiter.api.*;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.util.DocumentUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComprehensiveIntegrationTest {
    private static LocalFileRAG rag;
    private static Path tempDir;
    @BeforeAll
    static void setUpAll() throws Exception {
        tempDir = Files.createTempDirectory("rag-test");
        rag = LocalFileRAG.builder()
            .storagePath(tempDir.toString())
            .enableCache(true)
            .enableCompression(true)
            .build();
    }
    @AfterAll
    static void tearDownAll() throws Exception {
        if (rag != null) {
            rag.close();
        }
        deleteDirectory(tempDir.toFile());
    }
    @Test
    @Order(1)
    void testIndexDocuments() throws Exception {
        Document doc1 = DocumentUtils.fromText("Java Programming", "Learn Java basics");
        Document doc2 = DocumentUtils.fromText("Python Guide", "Python for beginners");
        Document doc3 = DocumentUtils.fromText("JavaScript Tutorial", "JS fundamentals");
        String id1 = rag.index(doc1);
        String id2 = rag.index(doc2);
        String id3 = rag.index(doc3);
        rag.commit();
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(id3);
    }
    @Test
    @Order(2)
    void testSearchDocuments() throws Exception {
        Query query = Query.builder()
            .queryText("Java")
            .limit(10)
            .build();
        SearchResult result = rag.search(query);
        assertTrue(result.getTotalHits() > 0);
        assertTrue(result.getDocuments().get(0).getTitle().contains("Java"));
    }
    @Test
    @Order(3)
    void testGetDocument() throws Exception {
        Query query = Query.builder()
            .queryText("Python")
            .limit(1)
            .build();
        SearchResult result = rag.search(query);
        if (result.getTotalHits() > 0) {
            String docId = result.getDocuments().get(0).getId();
            Document doc = rag.getDocument(docId);
            assertNotNull(doc);
            assertEquals("Python Guide", doc.getTitle());
        }
    }
    @Test
    @Order(4)
    void testUpdateDocument() throws Exception {
        Query query = Query.builder()
            .queryText("JavaScript")
            .limit(1)
            .build();
        SearchResult result = rag.search(query);
        if (result.getTotalHits() > 0) {
            String docId = result.getDocuments().get(0).getId();
            Document updatedDoc = DocumentUtils.fromText("JS Advanced", "Advanced JavaScript");
            updatedDoc.setId(docId);
            boolean updated = rag.updateDocument(docId, updatedDoc);
            assertTrue(updated);
            rag.commit();
            Document retrieved = rag.getDocument(docId);
            assertEquals("JS Advanced", retrieved.getTitle());
        }
    }
    @Test
    @Order(5)
    void testDeleteDocument() throws Exception {
        Query query = Query.builder()
            .queryText("Python")
            .limit(1)
            .build();
        SearchResult result = rag.search(query);
        if (result.getTotalHits() > 0) {
            String docId = result.getDocuments().get(0).getId();
            boolean deleted = rag.deleteDocument(docId);
            assertTrue(deleted);
            rag.commit();
            Document retrieved = rag.getDocument(docId);
            assertNull(retrieved);
        }
    }
    @Test
    @Order(6)
    void testGetStatistics() {
        var stats = rag.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getDocumentCount() >= 0);
        assertTrue(stats.getIndexedDocumentCount() >= 0);
    }
    @Test
    @Order(7)
    void testSearchWithPagination() throws Exception {
        for (int i = 0; i < 20; i++) {
            Document doc = DocumentUtils.fromText("Doc " + i, "Content " + i);
            rag.index(doc);
        }
        rag.commit();
        Query query = Query.builder()
            .queryText("Content")
            .limit(10)
            .build();
        SearchResult result = rag.search(query);
        assertTrue(result.getTotalHits() >= 20);
        assertTrue(result.getDocuments().size() <= 10);
    }
    @Test
    @Order(8)
    void testCachePerformance() throws Exception {
        Query query = Query.builder()
            .queryText("Java")
            .limit(10)
            .build();
        long start1 = System.currentTimeMillis();
        SearchResult result1 = rag.search(query);
        long time1 = System.currentTimeMillis() - start1;
        long start2 = System.currentTimeMillis();
        SearchResult result2 = rag.search(query);
        long time2 = System.currentTimeMillis() - start2;
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(time2 <= time1, "Second search should be faster due to cache");
    }
    @Test
    @Order(9)
    void testOptimizeIndex() {
        assertDoesNotThrow(() -> rag.optimizeIndex());
    }
    @Test
    @Order(10)
    void testBatchIndexing() throws Exception {
        for (int i = 0; i < 100; i++) {
            Document doc = DocumentUtils.fromText("Batch Doc " + i, "Batch content " + i);
            rag.index(doc);
            if (i % 10 == 0) {
                rag.commit();
            }
        }
        rag.commit();
        var stats = rag.getStatistics();
        assertTrue(stats.getIndexedDocumentCount() >= 100);
    }
    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}

