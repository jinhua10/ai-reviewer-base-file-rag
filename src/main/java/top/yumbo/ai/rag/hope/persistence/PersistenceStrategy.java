package top.yumbo.ai.rag.hope.persistence;

import lombok.Getter;

/**
 * 持久化策略枚举
 * (Persistence Strategy Enum)
 *
 * <p>
 * 定义支持的持久化后端类型
 * (Defines supported persistence backend types)
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
@Getter
public enum PersistenceStrategy {

    /**
     * JSON 文件存储（默认）
     * (JSON File Storage - Default)
     * <p>
     * 特点 (Features):
     * - 轻量级，无需外部依赖
     * - 适合小规模数据（<10000类型）
     * - 分片存储，支持懒加载
     * - 内存占用: ~10MB
     */
    JSON_FILE("json-file", "JSON文件存储", "top.yumbo.ai.rag.hope.persistence.impl.JsonFilePersistence"),

    /**
     * H2 嵌入式数据库
     * (H2 Embedded Database)
     * <p>
     * 特点 (Features):
     * - 嵌入式，无需外部服务
     * - 支持SQL查询
     * - 适合中等规模数据（<100000类型）
     * - 事务支持
     */
    H2_DATABASE("h2", "H2数据库", "top.yumbo.ai.rag.hope.persistence.impl.H2Persistence"),

    /**
     * SQLite 数据库
     * (SQLite Database)
     * <p>
     * 特点 (Features):
     * - 单文件数据库
     * - 跨平台
     * - 适合中等规模数据
     * - 稳定可靠
     */
    SQLITE("sqlite", "SQLite数据库", "top.yumbo.ai.rag.hope.persistence.impl.SqlitePersistence"),

    /**
     * Redis 缓存
     * (Redis Cache)
     * <p>
     * 特点 (Features):
     * - 高性能内存存储
     * - 分布式支持
     * - 适合大规模数据（100000+类型）
     * - 需要外部Redis服务
     */
    REDIS("redis", "Redis缓存", "top.yumbo.ai.rag.hope.persistence.impl.RedisPersistence"),

    /**
     * MongoDB 文档数据库
     * (MongoDB Document Database)
     * <p>
     * 特点 (Features):
     * - 文档型存储
     * - 分布式支持
     * - 适合海量数据（1000000+类型）
     * - 需要外部MongoDB服务
     */
    MONGODB("mongodb", "MongoDB数据库", "top.yumbo.ai.rag.hope.persistence.impl.MongoDBPersistence"),

    /**
     * ElasticSearch 搜索引擎
     * (ElasticSearch Search Engine)
     * <p>
     * 特点 (Features):
     * - 全文搜索能力强大
     * - 分布式高可用
     * - 适合海量数据（1000000+类型）
     * - 支持复杂查询和聚合
     * - 实时搜索和分析
     * - 需要外部ElasticSearch服务
     */
    ELASTICSEARCH("elasticsearch", "ElasticSearch搜索引擎", "top.yumbo.ai.rag.hope.persistence.impl.ElasticsearchPersistence"),

    /**
     * 混合存储（Redis + 文件）
     * (Hybrid Storage - Redis + File)
     * <p>
     * 特点 (Features):
     * - Redis作为一级缓存
     * - 文件作为持久化存储
     * - 兼顾性能和可靠性
     * - 最佳实践推荐
     */
    HYBRID("hybrid", "混合存储", "top.yumbo.ai.rag.hope.persistence.impl.HybridPersistence"),

    /**
     * 内存存储（仅用于测试）
     * (Memory Storage - For testing only)
     * <p>
     * 特点 (Features):
     * - 纯内存存储
     * - 最快性能
     * - 数据不持久化
     * - 仅用于单元测试
     */
    MEMORY("memory", "内存存储", "top.yumbo.ai.rag.hope.persistence.impl.MemoryPersistence");

    private final String code;
    private final String description;
    private final String implementationClass;

    PersistenceStrategy(String code, String description, String implementationClass) {
        this.code = code;
        this.description = description;
        this.implementationClass = implementationClass;
    }

    /**
     * 从代码获取策略 (Get strategy from code)
     *
     * @param code 策略代码
     * @return 持久化策略
     */
    public static PersistenceStrategy fromCode(String code) {
        for (PersistenceStrategy strategy : values()) {
            if (strategy.getCode().equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        return JSON_FILE;  // 默认返回JSON文件存储
    }
}

