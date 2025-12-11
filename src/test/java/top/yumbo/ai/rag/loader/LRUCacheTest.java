package top.yumbo.ai.rag.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LRUCache 单元测试
 */
class LRUCacheTest {

    private LRUCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>(3); // 最大容量3
    }

    @Test
    void testPutAndGet() {
        // When: 放入元素
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // Then: 可以获取到
        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
    }

    @Test
    void testLRUEviction() {
        // Given: 缓存满了
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // When: 放入第4个元素
        cache.put("key4", "value4");

        // Then: 最久未使用的key1应该被淘汰
        assertNull(cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
    }

    @Test
    void testAccessOrder() {
        // Given: 缓存满了
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        // When: 访问key1，然后放入key4
        cache.get("key1"); // key1变成最近使用
        cache.put("key4", "value4");

        // Then: key2应该被淘汰（最久未访问）
        assertEquals("value1", cache.get("key1"));
        assertNull(cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
    }

    @Test
    void testRemove() {
        // Given
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // When: 移除key1
        String removed = cache.remove("key1");

        // Then
        assertEquals("value1", removed);
        assertNull(cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
    }

    @Test
    void testContainsKey() {
        // Given
        cache.put("key1", "value1");

        // Then
        assertTrue(cache.containsKey("key1"));
        assertFalse(cache.containsKey("key2"));
    }

    @Test
    void testClear() {
        // Given
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size());

        // When: 清空
        cache.clear();

        // Then
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testHitRate() {
        // Given
        cache.put("key1", "value1");

        // When: 2次命中，1次未命中
        cache.get("key1"); // hit
        cache.get("key1"); // hit
        cache.get("key2"); // miss

        // Then: 命中率应该是 2/3 ≈ 0.67
        double hitRate = cache.getHitRate();
        assertEquals(2.0 / 3.0, hitRate, 0.01);
    }

    @Test
    void testStatistics() {
        // Given
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4"); // 触发淘汰

        cache.get("key2"); // hit
        cache.get("key5"); // miss

        // When: 获取统计信息
        LRUCache.CacheStatistics stats = cache.getStatistics();

        // Then
        assertEquals(3, stats.getMaxSize());
        assertEquals(3, stats.getCurrentSize());
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertEquals(1, stats.getEvictionCount());
        assertEquals(0.5, stats.getHitRate(), 0.01);
    }

    @Test
    void testInvalidMaxSize() {
        // When & Then: 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new LRUCache<String, String>(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new LRUCache<String, String>(-1);
        });
    }
}

