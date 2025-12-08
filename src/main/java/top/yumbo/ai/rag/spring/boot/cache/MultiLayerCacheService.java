package top.yumbo.ai.rag.spring.boot.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.monitoring.PerformanceMonitoringService;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;

import java.time.Duration;
import java.util.Optional;

/**
 * 多层缓存服务
 * (Multi-layer Cache Service)
 *
 * 四层缓存架构：
 * L1 - HOPE 答案缓存（最快，1000条，1小时）
 * L2 - 概念单元缓存（快，5000条，2小时）
 * L3 - LLM 答案缓存（中，500条，30分钟）
 * L4 - 检索结果缓存（慢，2000条，1小时）
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Service
public class MultiLayerCacheService {

    private final PerformanceMonitoringService monitoringService;

    // L1: HOPE 答案缓存
    private final Cache<String, HOPEAnswer> hopeAnswerCache;

    // L2: 概念单元缓存
    private final Cache<String, ConceptUnit> conceptUnitCache;

    // L3: LLM 答案缓存
    private final Cache<String, String> llmAnswerCache;

    // L4: 检索结果缓存
    private final Cache<String, RetrievalResult> retrievalResultCache;

    @Autowired
    public MultiLayerCacheService(
            @Autowired(required = false) PerformanceMonitoringService monitoringService) {
        this.monitoringService = monitoringService;

        // 初始化 L1 缓存
        this.hopeAnswerCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();

        // 初始化 L2 缓存
        this.conceptUnitCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofHours(2))
            .recordStats()
            .build();

        // 初始化 L3 缓存
        this.llmAnswerCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .build();

        // 初始化 L4 缓存
        this.retrievalResultCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();

        log.info("多层缓存服务已初始化: L1(HOPE)=1000, L2(Concept)=5000, L3(LLM)=500, L4(Retrieval)=2000");
    }

    // ==================== L1: HOPE 答案缓存 ====================

    /**
     * 获取 HOPE 答案
     */
    public Optional<HOPEAnswer> getHopeAnswer(String question) {
        String key = normalizeKey(question);
        HOPEAnswer answer = hopeAnswerCache.getIfPresent(key);

        boolean hit = answer != null;
        if (monitoringService != null) {
            monitoringService.recordCacheAccess("L1_HOPE", hit);
        }

        if (hit) {
            log.debug("L1 缓存命中: {}", question);
        }

        return Optional.ofNullable(answer);
    }

    /**
     * 缓存 HOPE 答案
     */
    public void putHopeAnswer(String question, HOPEAnswer answer) {
        String key = normalizeKey(question);
        hopeAnswerCache.put(key, answer);
        log.debug("L1 缓存已更新: {}", question);
    }

    // ==================== L2: 概念单元缓存 ====================

    /**
     * 获取概念单元
     */
    public Optional<ConceptUnit> getConceptUnit(String conceptId) {
        ConceptUnit unit = conceptUnitCache.getIfPresent(conceptId);

        boolean hit = unit != null;
        if (monitoringService != null) {
            monitoringService.recordCacheAccess("L2_CONCEPT", hit);
        }

        if (hit) {
            log.debug("L2 缓存命中: {}", conceptId);
        }

        return Optional.ofNullable(unit);
    }

    /**
     * 缓存概念单元
     */
    public void putConceptUnit(String conceptId, ConceptUnit unit) {
        conceptUnitCache.put(conceptId, unit);
        log.debug("L2 缓存已更新: {}", conceptId);
    }

    // ==================== L3: LLM 答案缓存 ====================

    /**
     * 获取 LLM 答案
     */
    public Optional<String> getLlmAnswer(String question) {
        String key = normalizeKey(question);
        String answer = llmAnswerCache.getIfPresent(key);

        boolean hit = answer != null;
        if (monitoringService != null) {
            monitoringService.recordCacheAccess("L3_LLM", hit);
        }

        if (hit) {
            log.debug("L3 缓存命中: {}", question);
        }

        return Optional.ofNullable(answer);
    }

    /**
     * 缓存 LLM 答案
     */
    public void putLlmAnswer(String question, String answer) {
        String key = normalizeKey(question);
        llmAnswerCache.put(key, answer);
        log.debug("L3 缓存已更新: {}", question);
    }

    // ==================== L4: 检索结果缓存 ====================

    /**
     * 获取检索结果
     */
    public Optional<RetrievalResult> getRetrievalResult(String query) {
        String key = normalizeKey(query);
        RetrievalResult result = retrievalResultCache.getIfPresent(key);

        boolean hit = result != null;
        if (monitoringService != null) {
            monitoringService.recordCacheAccess("L4_RETRIEVAL", hit);
        }

        if (hit) {
            log.debug("L4 缓存命中: {}", query);
        }

        return Optional.ofNullable(result);
    }

    /**
     * 缓存检索结果
     */
    public void putRetrievalResult(String query, RetrievalResult result) {
        String key = normalizeKey(query);
        retrievalResultCache.put(key, result);
        log.debug("L4 缓存已更新: {}", query);
    }

    // ==================== 缓存管理 ====================

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        hopeAnswerCache.invalidateAll();
        conceptUnitCache.invalidateAll();
        llmAnswerCache.invalidateAll();
        retrievalResultCache.invalidateAll();
        log.info("所有缓存已清空");
    }

    /**
     * 清空特定层缓存
     */
    public void clearLayer(int layer) {
        switch (layer) {
            case 1:
                hopeAnswerCache.invalidateAll();
                log.info("L1 缓存已清空");
                break;
            case 2:
                conceptUnitCache.invalidateAll();
                log.info("L2 缓存已清空");
                break;
            case 3:
                llmAnswerCache.invalidateAll();
                log.info("L3 缓存已清空");
                break;
            case 4:
                retrievalResultCache.invalidateAll();
                log.info("L4 缓存已清空");
                break;
            default:
                log.warn("无效的缓存层: {}", layer);
        }
    }

    /**
     * 获取缓存统计
     */
    public CacheStatistics getStatistics() {
        CacheStatistics stats = new CacheStatistics();

        stats.setL1Size(hopeAnswerCache.estimatedSize());
        stats.setL1HitRate(hopeAnswerCache.stats().hitRate());

        stats.setL2Size(conceptUnitCache.estimatedSize());
        stats.setL2HitRate(conceptUnitCache.stats().hitRate());

        stats.setL3Size(llmAnswerCache.estimatedSize());
        stats.setL3HitRate(llmAnswerCache.stats().hitRate());

        stats.setL4Size(retrievalResultCache.estimatedSize());
        stats.setL4HitRate(retrievalResultCache.stats().hitRate());

        return stats;
    }

    /**
     * 标准化缓存键
     */
    private String normalizeKey(String key) {
        return key.toLowerCase().trim()
            .replaceAll("[？?！!。.]", "")
            .replaceAll("\\s+", " ");
    }

    // ==================== 内部数据类 ====================

    /**
     * 概念单元
     */
    @lombok.Data
    @lombok.Builder
    public static class ConceptUnit {
        private String conceptId;
        private String content;
        private String category;
        private double confidence;
        private long timestamp;
    }

    /**
     * 检索结果
     */
    @lombok.Data
    @lombok.Builder
    public static class RetrievalResult {
        private String query;
        private java.util.List<String> documentIds;
        private java.util.Map<String, Double> scores;
        private long timestamp;
    }

    /**
     * 缓存统计
     */
    @lombok.Data
    public static class CacheStatistics {
        private long l1Size;
        private double l1HitRate;
        private long l2Size;
        private double l2HitRate;
        private long l3Size;
        private double l3HitRate;
        private long l4Size;
        private double l4HitRate;
    }
}

