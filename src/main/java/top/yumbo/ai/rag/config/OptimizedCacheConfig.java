package top.yumbo.ai.rag.config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
public class OptimizedCacheConfig {
    public static <K, V> Cache<K, V> createDocumentCache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(2))
            .expireAfterWrite(Duration.ofHours(6))
            .recordStats()
            .initialCapacity(1000)
            .build();
    }
    public static <K, V> Cache<K, V> createQueryCache() {
        return Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .initialCapacity(5000)
            .build();
    }
    public static <K, V> Cache<K, V> createHotDataCache() {
        return Caffeine.newBuilder()
            .maximumWeight(100_000_000)
            .weigher((K key, V value) -> 1000)
            .expireAfterAccess(Duration.ofMinutes(15))
            .recordStats()
            .build();
    }
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
