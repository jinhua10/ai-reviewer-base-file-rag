package top.yumbo.ai.rag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base File RAG 配置类
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
     * 存储配置
     */
    @Builder.Default
    private StorageConfig storage = new StorageConfig();

    /**
     * 索引配置
     */
    @Builder.Default
    private IndexConfig index = new IndexConfig();

    /**
     * 缓存配置
     */
    @Builder.Default
    private CacheConfig cache = new CacheConfig();

    /**
     * 服务器配置
     */
    @Builder.Default
    private ServerConfig server = new ServerConfig();

    /**
     * 存储配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfig {
        /**
         * 数据存储根路径
         */
        @Builder.Default
        private String basePath = "./data";

        /**
         * 是否启用压缩
         */
        @Builder.Default
        private boolean compression = true;

        /**
         * 是否启用加密
         */
        @Builder.Default
        private boolean encryption = false;

        /**
         * 加密算法
         */
        @Builder.Default
        private String encryptionAlgorithm = "AES-256";
    }

    /**
     * 索引配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexConfig {
        /**
         * 分析器类型
         */
        @Builder.Default
        private String analyzer = "standard";

        /**
         * RAM缓冲区大小（MB）
         */
        @Builder.Default
        private int ramBufferSizeMB = 256;

        /**
         * 最大缓冲文档数
         */
        @Builder.Default
        private int maxBufferedDocs = 1000;

        /**
         * 提交间隔（秒）
         */
        @Builder.Default
        private int commitIntervalSeconds = 30;

        /**
         * 合并策略
         */
        @Builder.Default
        private String mergePolicy = "tiered";
    }

    /**
     * 缓存配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheConfig {
        /**
         * 文档缓存大小
         */
        @Builder.Default
        private int documentCacheSize = 1000;

        /**
         * 文档缓存TTL（秒）
         */
        @Builder.Default
        private int documentCacheTtlSeconds = 3600;

        /**
         * 查询缓存大小
         */
        @Builder.Default
        private int queryCacheSize = 10000;

        /**
         * 查询缓存TTL（秒）
         */
        @Builder.Default
        private int queryCacheTtlSeconds = 300;

        /**
         * 是否启用缓存
         */
        @Builder.Default
        private boolean enabled = true;
    }

    /**
     * 服务器配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerConfig {
        /**
         * 服务端口
         */
        @Builder.Default
        private int port = 8080;

        /**
         * 工作线程数
         */
        @Builder.Default
        private int threads = 100;

        /**
         * 请求超时时间（秒）
         */
        @Builder.Default
        private int requestTimeoutSeconds = 30;

        /**
         * 是否启用服务器
         */
        @Builder.Default
        private boolean enabled = false;
    }

    /**
     * 创建默认配置
     */
    public static RAGConfiguration createDefault() {
        return RAGConfiguration.builder().build();
    }
}

