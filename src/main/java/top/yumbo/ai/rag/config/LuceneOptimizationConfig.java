package top.yumbo.ai.rag.config;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.NoMergePolicy;
public class LuceneOptimizationConfig {
    public static void optimizeIndexWriterConfig(IndexWriterConfig config) {
        config.setRAMBufferSizeMB(512.0);
        TieredMergePolicy mergePolicy = new TieredMergePolicy();
        mergePolicy.setMaxMergeAtOnce(10);
        mergePolicy.setSegmentsPerTier(10);
        mergePolicy.setMaxMergedSegmentMB(5 * 1024);
        config.setMergePolicy(mergePolicy);
        ConcurrentMergeScheduler mergeScheduler = new ConcurrentMergeScheduler();
        mergeScheduler.setMaxMergesAndThreads(4, 2);
        config.setMergeScheduler(mergeScheduler);
        config.setCommitOnClose(true);
        config.setUseCompoundFile(false);
    }
    public static void optimizeForBulkIndexing(IndexWriterConfig config) {
        config.setRAMBufferSizeMB(1024.0);
        config.setMergePolicy(NoMergePolicy.INSTANCE);
        config.setMaxBufferedDocs(10000);
    }
}
