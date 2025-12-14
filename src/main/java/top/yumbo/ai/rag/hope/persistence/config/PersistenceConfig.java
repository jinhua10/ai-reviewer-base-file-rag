package top.yumbo.ai.rag.hope.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 持久化配置
 * (Persistence Configuration)
 *
 * <p>
 * 配置文件示例 (application.yml):
 * <pre>
 * question-classifier:
 *   persistence:
 *     strategy: json-file  # 或 h2, redis, mongodb, hybrid
 *     cache-size: 100
 *     flush-interval: 10
 *     backup-interval: 3600
 *
 *     # JSON文件配置
 *     json-file:
 *       data-dir: data/question-classifier
 *       compression: true
 *
 *     # H2数据库配置
 *     h2:
 *       url: jdbc:h2:./data/question-classifier
 *       username: sa
 *       password:
 *
 *     # Redis配置
 *     redis:
 *       host: localhost
 *       port: 6379
 *       password:
 *       database: 0
 *
 *     # MongoDB配置
 *     mongodb:
 *       uri: mongodb://localhost:27017/question-classifier
 * </pre>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "question-classifier.persistence")
public class PersistenceConfig {

    /**
     * 持久化策略 (Persistence strategy)
     * 可选值: json-file, h2, sqlite, redis, mongodb, hybrid, memory
     */
    private String strategy = "json-file";

    /**
     * LRU缓存大小 (LRU cache size)
     */
    private int cacheSize = 100;

    /**
     * 刷新间隔（秒）(Flush interval in seconds)
     */
    private int flushInterval = 10;

    /**
     * 备份间隔（秒）(Backup interval in seconds)
     */
    private int backupInterval = 3600;

    /**
     * 是否启用自动备份 (Enable auto backup)
     */
    private boolean autoBackup = true;

    /**
     * 是否启用压缩 (Enable compression)
     */
    private boolean compression = false;

    /**
     * JSON文件配置 (JSON file configuration)
     */
    private JsonFileConfig jsonFile = new JsonFileConfig();

    /**
     * H2数据库配置 (H2 database configuration)
     */
    private H2Config h2 = new H2Config();

    /**
     * SQLite配置 (SQLite configuration)
     */
    private SqliteConfig sqlite = new SqliteConfig();

    /**
     * Redis配置 (Redis configuration)
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * MongoDB配置 (MongoDB configuration)
     */
    private MongoDBConfig mongodb = new MongoDBConfig();

    /**
     * ElasticSearch配置 (ElasticSearch configuration)
     */
    private ElasticsearchConfig elasticsearch = new ElasticsearchConfig();

    /**
     * 混合存储配置 (Hybrid configuration)
     */
    private HybridConfig hybrid = new HybridConfig();

    /**
     * JSON文件配置
     */
    @Data
    public static class JsonFileConfig {
        private String dataDir = "data/question-classifier";
        private boolean compression = false;
        private int maxBackups = 30;
    }

    /**
     * H2数据库配置
     */
    @Data
    public static class H2Config {
        private String url = "jdbc:h2:./data/question-classifier";
        private String username = "sa";
        private String password = "";
        private int maxPoolSize = 10;
    }

    /**
     * SQLite配置
     */
    @Data
    public static class SqliteConfig {
        private String dbPath = "data/question-classifier.db";
        private int maxPoolSize = 5;
    }

    /**
     * Redis配置
     */
    @Data
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        private int timeout = 3000;
        private String keyPrefix = "qc:";
    }

    /**
     * MongoDB配置
     */
    @Data
    public static class MongoDBConfig {
        private String uri = "mongodb://localhost:27017/question-classifier";
        private String database = "question-classifier";
        private String collection = "question_types";
    }

    /**
     * ElasticSearch配置
     */
    @Data
    public static class ElasticsearchConfig {
        private String hosts = "localhost:9200";  // 多个主机用逗号分隔
        private String scheme = "http";           // http 或 https
        private String username = "";             // 用户名（如果启用了安全）
        private String password = "";             // 密码
        private String indexPrefix = "qc-";       // 索引前缀
        private int connectionTimeout = 5000;     // 连接超时（毫秒）
        private int socketTimeout = 60000;        // Socket超时（毫秒）
        private int maxRetryTimeout = 60000;      // 最大重试超时
        private boolean sniffOnFailure = true;    // 失败时嗅探
    }

    /**
     * 混合存储配置
     */
    @Data
    public static class HybridConfig {
        private String cacheStrategy = "redis";
        private String storageStrategy = "json-file";
        private int cacheTtl = 3600;  // 缓存过期时间（秒）
    }

    /**
     * 获取扩展配置 (Get extended config)
     */
    public Map<String, Object> getConfigForStrategy(String strategy) {
        Map<String, Object> config = new HashMap<>();

        switch (strategy.toLowerCase()) {
            case "json-file":
                config.put("dataDir", jsonFile.getDataDir());
                config.put("compression", jsonFile.isCompression());
                config.put("maxBackups", jsonFile.getMaxBackups());
                break;

            case "h2":
                config.put("url", h2.getUrl());
                config.put("username", h2.getUsername());
                config.put("password", h2.getPassword());
                config.put("maxPoolSize", h2.getMaxPoolSize());
                break;

            case "sqlite":
                config.put("dbPath", sqlite.getDbPath());
                config.put("maxPoolSize", sqlite.getMaxPoolSize());
                break;

            case "redis":
                config.put("host", redis.getHost());
                config.put("port", redis.getPort());
                config.put("password", redis.getPassword());
                config.put("database", redis.getDatabase());
                config.put("timeout", redis.getTimeout());
                config.put("keyPrefix", redis.getKeyPrefix());
                break;

            case "mongodb":
                config.put("uri", mongodb.getUri());
                config.put("database", mongodb.getDatabase());
                config.put("collection", mongodb.getCollection());
                break;

            case "elasticsearch":
                config.put("hosts", elasticsearch.getHosts());
                config.put("scheme", elasticsearch.getScheme());
                config.put("username", elasticsearch.getUsername());
                config.put("password", elasticsearch.getPassword());
                config.put("indexPrefix", elasticsearch.getIndexPrefix());
                config.put("connectionTimeout", elasticsearch.getConnectionTimeout());
                config.put("socketTimeout", elasticsearch.getSocketTimeout());
                config.put("maxRetryTimeout", elasticsearch.getMaxRetryTimeout());
                config.put("sniffOnFailure", elasticsearch.isSniffOnFailure());
                break;

            case "hybrid":
                config.put("cacheStrategy", hybrid.getCacheStrategy());
                config.put("storageStrategy", hybrid.getStorageStrategy());
                config.put("cacheTtl", hybrid.getCacheTtl());
                break;
        }

        // 通用配置
        config.put("cacheSize", cacheSize);
        config.put("flushInterval", flushInterval);
        config.put("backupInterval", backupInterval);
        config.put("autoBackup", autoBackup);
        config.put("compression", compression);

        return config;
    }
}

