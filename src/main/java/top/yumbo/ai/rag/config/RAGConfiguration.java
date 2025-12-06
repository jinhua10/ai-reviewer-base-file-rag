package top.yumbo.ai.rag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base File RAG 配置类 (Base File RAG Configuration Class)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGConfiguration {

    /**
     * 存储配置 (Storage configuration)
     */
    @Builder.Default
    private StorageConfig storage = new StorageConfig();

    /**
     * 索引配置 (Index configuration)
     */
    @Builder.Default
    private IndexConfig index = new IndexConfig();

    /**
     * 缓存配置 (Cache configuration)
     */
    @Builder.Default
    private CacheConfig cache = new CacheConfig();


    /**
     * 存储配置 (Storage configuration)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfig {
        /**
         * 数据存储根路径 (Data storage root path)
         */
        @Builder.Default
        private String basePath = "./data";

        /**
         * 是否启用压缩 (Whether to enable compression)
         */
        @Builder.Default
        private boolean compression = true;
    }

    /**
     * 索引配置 (Index configuration)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexConfig {
        /**
         * 分析器类型 (Analyzer type)
         */
        @Builder.Default
        private String analyzer = "standard";

        /**
         * RAM缓冲区大小（MB） (RAM buffer size in MB)
         */
        @Builder.Default
        private int ramBufferSizeMB = 256;

        /**
         * 最大缓冲文档数 (Maximum buffered documents)
         */
        @Builder.Default
        private int maxBufferedDocs = 1000;

        /**
         * 提交间隔（秒） (Commit interval in seconds)
         */
        @Builder.Default
        private int commitIntervalSeconds = 30;

        /**
         * 合并策略 (Merge policy)
         */
        @Builder.Default
        private String mergePolicy = "tiered";
    }

    /**
     * 缓存配置 (Cache configuration)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig {
        /**
         * 文档缓存大小 (Document cache size)
         */
        @Builder.Default
        private int documentCacheSize = 1000;

        /**
         * 文档缓存TTL（秒） (Document cache TTL in seconds)
         */
        @Builder.Default
        private int documentCacheTtlSeconds = 3600;

        /**
         * 查询缓存大小 (Query cache size)
         */
        @Builder.Default
        private int queryCacheSize = 10000;

        /**
         * 查询缓存TTL（秒） (Query cache TTL in seconds)
         */
        @Builder.Default
        private int queryCacheTtlSeconds = 300;

        /**
         * 是否启用缓存 (Whether to enable cache)
         */
        @Builder.Default
        private boolean enabled = true;
    }


    /**
     * 创建默认配置 (Create default configuration)
     */
    public static RAGConfiguration createDefault() {
        return RAGConfiguration.builder().build();
    }
}
