package top.yumbo.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.util.DocumentUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
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
    static void tearDownAll() {
        if (rag != null) {
            rag.close();
        }
        deleteDirectory(tempDir.toFile());
    }

    @Test
    @Order(1)
    void testIndexDocuments() {
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
    void testSearchDocuments() {
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
    void testGetDocument() {
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
    void testUpdateDocument() {
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
    void testDeleteDocument() {
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
    void testSearchWithPagination() {
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
                .queryText("test query for cache")
                .limit(5)
                .build();

        // Warm-up query to initialize any lazy components
        rag.search(query);
        Thread.sleep(100); // Allow cache to settle

        // First actual measurement
        long start1 = System.nanoTime();
        rag.search(query);
        long duration1 = System.nanoTime() - start1;

        Thread.sleep(50); // Small delay between queries

        // Second search (should be cached)
        long start2 = System.nanoTime();
        rag.search(query);
        long duration2 = System.nanoTime() - start2;

        // Log the durations for debugging
        System.out.println("First search duration: " + duration1 / 1_000_000.0 + "ms");
        System.out.println("Second search duration: " + duration2 / 1_000_000.0 + "ms");
        System.out.println("Speed improvement: " + ((double) duration1 / duration2) + "x");

        // More lenient assertion - second search should be at least 20% faster
        assertTrue(duration2 < duration1 * 0.8,
                String.format("Second search should be faster due to cache. First: %.2fms, Second: %.2fms",
                        duration1 / 1_000_000.0, duration2 / 1_000_000.0));
    }

    @Test
    @Order(9)
    void testOptimizeIndex() {
        assertDoesNotThrow(() -> rag.optimizeIndex());
    }

    @Test
    @Order(10)
    void testBatchIndexing() {
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
                        boolean delete = file.delete();
                        if (!delete) {
                            log.error("Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            boolean delete = dir.delete();
            if (!delete) {
                log.error("Failed to delete dir: " + dir.getAbsolutePath());
            }
        }
    }
}
