package top.yumbo.ai.rag.monitor;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class PerformanceMetricsTest {
    private PerformanceMetrics metrics;
    @BeforeEach
    void setUp() {
        metrics = new PerformanceMetrics();
    }
    @Test
    void testRecordIndex() {
        metrics.recordIndex(10, true);
        metrics.recordIndex(20, true);
        assertEquals(2, metrics.getIndexedDocuments().sum());
        assertEquals(15.0, metrics.getAvgIndexTime());
        assertEquals(0, metrics.getIndexErrors().sum());
    }
    @Test
    void testRecordIndexWithError() {
        metrics.recordIndex(10, false);
        assertEquals(1, metrics.getIndexErrors().sum());
    }
    @Test
    void testRecordSearch() {
        metrics.recordSearch(5, true);
        metrics.recordSearch(15, true);
        assertEquals(2, metrics.getSearchCount().sum());
        assertEquals(10.0, metrics.getAvgSearchTime());
    }
    @Test
    void testRecordCache() {
        metrics.recordCache(true);
        metrics.recordCache(true);
        metrics.recordCache(false);
        assertEquals(2, metrics.getCacheHits().sum());
        assertEquals(1, metrics.getCacheMisses().sum());
        assertEquals(0.666, metrics.getCacheHitRate(), 0.01);
    }
    @Test
    void testReset() {
        metrics.recordIndex(10, true);
        metrics.recordSearch(5, true);
        metrics.recordCache(true);
        metrics.reset();
        assertEquals(0, metrics.getIndexedDocuments().sum());
        assertEquals(0, metrics.getSearchCount().sum());
        assertEquals(0, metrics.getCacheHits().sum());
    }
    @Test
    void testGenerateReport() {
        metrics.recordIndex(10, true);
        metrics.recordSearch(5, true);
        metrics.recordCache(true);
        String report = metrics.generateReport();
        assertNotNull(report);
        assertTrue(report.contains("Indexed: 1"));
        assertTrue(report.contains("Searches: 1"));
    }
}
