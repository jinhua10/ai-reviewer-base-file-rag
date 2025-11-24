package top.yumbo.ai.rag.monitor;
import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
@Data
public class PerformanceMetrics {
    private final LongAdder indexedDocuments = new LongAdder();
    private final LongAdder indexErrors = new LongAdder();
    private final AtomicLong totalIndexTime = new AtomicLong(0);
    private final LongAdder searchCount = new LongAdder();
    private final LongAdder searchErrors = new LongAdder();
    private final AtomicLong totalSearchTime = new AtomicLong(0);
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    public void recordIndex(long timeMs, boolean success) {
        indexedDocuments.increment();
        totalIndexTime.addAndGet(timeMs);
        if (!success) {
            indexErrors.increment();
        }
    }
    public void recordSearch(long timeMs, boolean success) {
        searchCount.increment();
        totalSearchTime.addAndGet(timeMs);
        if (!success) {
            searchErrors.increment();
        }
    }
    public void recordCache(boolean hit) {
        if (hit) {
            cacheHits.increment();
        } else {
            cacheMisses.increment();
        }
    }
    public double getAvgIndexTime() {
        long count = indexedDocuments.sum();
        return count > 0 ? (double) totalIndexTime.get() / count : 0;
    }
    public double getAvgSearchTime() {
        long count = searchCount.sum();
        return count > 0 ? (double) totalSearchTime.get() / count : 0;
    }
    public double getCacheHitRate() {
        long hits = cacheHits.sum();
        long total = hits + cacheMisses.sum();
        return total > 0 ? (double) hits / total : 0;
    }
    public void reset() {
        indexedDocuments.reset();
        indexErrors.reset();
        totalIndexTime.set(0);
        searchCount.reset();
        searchErrors.reset();
        totalSearchTime.set(0);
        cacheHits.reset();
        cacheMisses.reset();
    }
    public String generateReport() {
        return String.format(
            "Performance Metrics:\n" +
            "  Indexed: %d docs, Avg: %.2fms, Errors: %d\n" +
            "  Searches: %d, Avg: %.2fms, Errors: %d\n" +
            "  Cache Hit Rate: %.2f%%",
            indexedDocuments.sum(), getAvgIndexTime(), indexErrors.sum(),
            searchCount.sum(), getAvgSearchTime(), searchErrors.sum(),
            getCacheHitRate() * 100
        );
    }
}
