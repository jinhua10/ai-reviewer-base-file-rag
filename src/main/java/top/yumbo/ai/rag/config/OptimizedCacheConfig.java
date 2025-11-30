package top.yumbo.ai.rag.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

/**
 * 优化缓存配置类 (Optimized cache configuration class)
 * 提供各种缓存配置的工厂方法 (Provides factory methods for various cache configurations)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public class OptimizedCacheConfig {

    /**
     * 创建文档缓存 (Create document cache)
     *
     * @param <K> 键类型 (key type)
     * @param <V> 值类型 (value type)
     * @return 缓存实例 (cache instance)
     */
    public static <K, V> Cache<K, V> createDocumentCache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(2))
            .expireAfterWrite(Duration.ofHours(6))
            .recordStats()
            .initialCapacity(1000)
            .build();
    }

    /**
     * 创建查询缓存 (Create query cache)
     *
     * @param <K> 键类型 (key type)
     * @param <V> 值类型 (value type)
     * @return 缓存实例 (cache instance)
     */
    public static <K, V> Cache<K, V> createQueryCache() {
        return Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .initialCapacity(5000)
            .build();
    }

    /**
     * 创建热点数据缓存 (Create hot data cache)
     *
     * @param <K> 键类型 (key type)
     * @param <V> 值类型 (value type)
     * @return 缓存实例 (cache instance)
     */
    public static <K, V> Cache<K, V> createHotDataCache() {
        return Caffeine.newBuilder()
            .maximumWeight(100_000_000)
            .weigher((K key, V value) -> 1000)
            .expireAfterAccess(Duration.ofMinutes(15))
            .recordStats()
            .build();
    }

    /**
     * 创建自定义缓存 (Create custom cache)
     *
     * @param <K> 键类型 (key type)
     * @param <V> 值类型 (value type)
     * @param maxSize 最大大小 (maximum size)
     * @param expireAfterWrite 写入后过期时间 (expire after write)
     * @param expireAfterAccess 访问后过期时间 (expire after access)
     * @return 缓存实例 (cache instance)
     */
    public static <K, V> Cache<K, V> createCustomCache(
            long maxSize, 
            Duration expireAfterWrite, 
            Duration expireAfterAccess) {
        return Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(expireAfterWrite)
            .expireAfterAccess(expireAfterAccess)
            .recordStats()
            .build();
    }
}
