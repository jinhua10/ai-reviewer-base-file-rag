package top.yumbo.ai.rag.loader;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU缓存实现 (LRU Cache Implementation)
 *
 * 基于LinkedHashMap实现的线程安全LRU缓存
 * (Thread-safe LRU cache based on LinkedHashMap)
 *
 * 特性 (Features):
 * - 自动淘汰最久未使用的项 (Automatic eviction of least recently used items)
 * - 线程安全 (Thread-safe)
 * - 访问即更新 (Access updates position)
 * - 统计信息 (Statistics)
 *
 * @param <K> 键类型 (Key type)
 * @param <V> 值类型 (Value type)
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
public class LRUCache<K, V> {

    /**
     * 最大容量 (Maximum capacity)
     */
    private final int maxSize;

    /**
     * 底层存储 (Underlying storage)
     */
    private final LinkedHashMap<K, V> cache;

    /**
     * 读写锁 (Read-write lock)
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 命中次数 (Hit count)
     */
    private long hitCount = 0;

    /**
     * 未命中次数 (Miss count)
     */
    private long missCount = 0;

    /**
     * 淘汰次数 (Eviction count)
     */
    private long evictionCount = 0;

    /**
     * 构造函数 (Constructor)
     *
     * @param maxSize 最大容量 (Maximum capacity)
     */
    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }

        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                boolean shouldRemove = size() > LRUCache.this.maxSize;
                if (shouldRemove) {
                    evictionCount++;
                    log.debug(I18N.get("loader.cache.evicted", eldest.getKey(), size(), maxSize));
                }
                return shouldRemove;
            }
        };

        log.info(I18N.get("loader.cache.created", maxSize));
    }

    /**
     * 获取缓存项 (Get cache item)
     *
     * @param key 键 (Key)
     * @return 值，如果不存在返回null (Value, or null if not exists)
     */
    public V get(K key) {
        lock.readLock().lock();
        try {
            V value = cache.get(key);
            if (value != null) {
                hitCount++;
                log.debug(I18N.get("loader.cache.hit", key, getHitRate()));
            } else {
                missCount++;
                log.debug(I18N.get("loader.cache.miss", key, getHitRate()));
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 放入缓存项 (Put cache item)
     *
     * @param key 键 (Key)
     * @param value 值 (Value)
     * @return 之前的值，如果不存在返回null (Previous value, or null if not exists)
     */
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            V oldValue = cache.put(key, value);
            log.debug(I18N.get("loader.cache.put", key, cache.size(), maxSize));
            return oldValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除缓存项 (Remove cache item)
     *
     * @param key 键 (Key)
     * @return 被移除的值，如果不存在返回null (Removed value, or null if not exists)
     */
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            V value = cache.remove(key);
            if (value != null) {
                log.debug(I18N.get("loader.cache.removed", key, cache.size()));
            }
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查是否包含键 (Check if contains key)
     *
     * @param key 键 (Key)
     * @return 是否包含 (Whether contains)
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空缓存 (Clear cache)
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            log.info(I18N.get("loader.cache.cleared", size));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取缓存大小 (Get cache size)
     *
     * @return 当前大小 (Current size)
     */
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取最大容量 (Get maximum capacity)
     *
     * @return 最大容量 (Maximum capacity)
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * 获取命中率 (Get hit rate)
     *
     * @return 命中率 (Hit rate)
     */
    public double getHitRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double) hitCount / total;
    }

    /**
     * 获取缓存统计信息 (Get cache statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public CacheStatistics getStatistics() {
        lock.readLock().lock();
        try {
            return CacheStatistics.builder()
                    .maxSize(maxSize)
                    .currentSize(cache.size())
                    .hitCount(hitCount)
                    .missCount(missCount)
                    .evictionCount(evictionCount)
                    .hitRate(getHitRate())
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 缓存统计信息 (Cache Statistics)
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStatistics {
        /**
         * 最大容量 (Maximum capacity)
         */
        private int maxSize;

        /**
         * 当前大小 (Current size)
         */
        private int currentSize;

        /**
         * 命中次数 (Hit count)
         */
        private long hitCount;

        /**
         * 未命中次数 (Miss count)
         */
        private long missCount;

        /**
         * 淘汰次数 (Eviction count)
         */
        private long evictionCount;

        /**
         * 命中率 (Hit rate)
         */
        private double hitRate;
    }
}

