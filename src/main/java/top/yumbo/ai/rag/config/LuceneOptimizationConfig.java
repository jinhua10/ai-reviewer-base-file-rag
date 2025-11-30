package top.yumbo.ai.rag.config;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.NoMergePolicy;

/**
 * Lucene 优化配置类 (Lucene optimization configuration class)
 * 提供 Lucene 索引写入器的优化配置 (Provides optimization configuration for Lucene index writers)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public class LuceneOptimizationConfig {

    /**
     * 优化索引写入器配置 (Optimize index writer configuration)
     *
     * @param config 索引写入器配置 (index writer config)
     */
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

    /**
     * 为批量索引优化配置 (Optimize configuration for bulk indexing)
     *
     * @param config 索引写入器配置 (index writer config)
     */
    public static void optimizeForBulkIndexing(IndexWriterConfig config) {
        config.setRAMBufferSizeMB(1024.0);
        config.setMergePolicy(NoMergePolicy.INSTANCE);
        config.setMaxBufferedDocs(10000);
    }
}
