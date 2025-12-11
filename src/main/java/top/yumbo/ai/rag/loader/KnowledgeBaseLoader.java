package top.yumbo.ai.rag.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.index.IndexBuilder;
import top.yumbo.ai.rag.index.RoleVectorIndex;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 知识库加载器 (Knowledge Base Loader)
 *
 * 负责按需加载和卸载角色向量索引
 * (Responsible for on-demand loading and unloading of role vector indices)
 *
 * 特性 (Features):
 * - 懒加载 (Lazy loading)
 * - LRU缓存 (LRU cache)
 * - 智能预热 (Smart preload)
 * - 异步加载 (Asynchronous loading)
 * - 统计监控 (Statistics monitoring)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class KnowledgeBaseLoader {

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private IndexBuilder indexBuilder;

    /**
     * LRU缓存 (LRU cache)
     */
    private LRUCache<String, RoleVectorIndex> cache;

    /**
     * 加载统计 (Loading statistics)
     */
    private final LoadingStats loadingStats = new LoadingStats();

    /**
     * 预加载策略 (Preload strategy)
     */
    private PreloadStrategy preloadStrategy;

    /**
     * 异步加载线程池 (Asynchronous loading thread pool)
     */
    private ExecutorService loadExecutor;

    /**
     * 最大缓存大小 (Maximum cache size)
     */
    private static final int MAX_CACHE_SIZE = 5;

    /**
     * 初始化 (Initialization)
     */
    @PostConstruct
    public void init() {
        log.info(I18N.get("loader.kb.initializing"));

        // 初始化LRU缓存 (Initialize LRU cache)
        this.cache = new LRUCache<>(MAX_CACHE_SIZE);

        // 初始化预加载策略 (Initialize preload strategy)
        PreloadStrategy.PreloadConfig config = PreloadStrategy.PreloadConfig.builder()
                .maxPreloadCount(3)
                .priorityWeight(1.0)
                .usageWeight(2.0)
                .recencyWeight(1.5)
                .build();
        this.preloadStrategy = new PreloadStrategy(config);

        // 初始化线程池 (Initialize thread pool)
        this.loadExecutor = Executors.newFixedThreadPool(2,
                r -> new Thread(r, "KnowledgeBaseLoader"));

        log.info(I18N.get("loader.kb.initialized", MAX_CACHE_SIZE));

        // 启动预热 (Start preloading)
        preloadIndices();
    }

    /**
     * 销毁 (Destruction)
     */
    @PreDestroy
    public void destroy() {
        log.info(I18N.get("loader.kb.destroying"));

        // 关闭线程池 (Shutdown thread pool)
        if (loadExecutor != null) {
            loadExecutor.shutdown();
            try {
                if (!loadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    loadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                loadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 卸载所有索引 (Unload all indices)
        cache.clear();

        log.info(I18N.get("loader.kb.destroyed"));
    }

    /**
     * 获取角色索引（懒加载） (Get role index with lazy loading)
     *
     * @param roleId 角色ID (Role ID)
     * @return 角色索引 (Role index)
     * @throws IOException 如果加载失败 (If loading fails)
     */
    public RoleVectorIndex getIndex(String roleId) throws IOException {
        if (roleId == null || roleId.isEmpty()) {
            throw new IllegalArgumentException("Role ID cannot be null or empty");
        }

        // 1. 尝试从缓存获取 (Try to get from cache)
        RoleVectorIndex index = cache.get(roleId);
        if (index != null) {
            loadingStats.recordCacheHit(roleId);
            preloadStrategy.recordUsage(roleId);
            log.debug(I18N.get("loader.kb.cache_hit", roleId));
            return index;
        }

        // 2. 缓存未命中，加载索引 (Cache miss, load index)
        log.info(I18N.get("loader.kb.loading", roleId));
        long startTime = System.currentTimeMillis();

        try {
            index = loadIndex(roleId);
            long loadTime = System.currentTimeMillis() - startTime;

            // 3. 放入缓存 (Put into cache)
            cache.put(roleId, index);

            // 4. 记录统计 (Record statistics)
            loadingStats.recordLoad(roleId, loadTime);
            preloadStrategy.recordUsage(roleId);

            log.info(I18N.get("loader.kb.loaded", roleId, loadTime));
            return index;

        } catch (Exception e) {
            loadingStats.recordLoadFailure(roleId, e.getMessage());
            log.error(I18N.get("loader.kb.load_failed", roleId, e.getMessage()), e);
            throw new IOException("Failed to load index for role: " + roleId, e);
        }
    }

    /**
     * 异步预加载索引 (Asynchronously preload index)
     *
     * @param roleId 角色ID (Role ID)
     * @return 异步结果 (Asynchronous result)
     */
    public CompletableFuture<Void> preloadIndexAsync(String roleId) {
        return CompletableFuture.runAsync(() -> {
            try {
                getIndex(roleId);
                log.info(I18N.get("loader.kb.preload_success", roleId));
            } catch (Exception e) {
                log.warn(I18N.get("loader.kb.preload_failed", roleId, e.getMessage()));
            }
        }, loadExecutor);
    }

    /**
     * 卸载角色索引 (Unload role index)
     *
     * @param roleId 角色ID (Role ID)
     * @return 是否成功 (Whether successful)
     */
    public boolean unloadIndex(String roleId) {
        RoleVectorIndex index = cache.remove(roleId);
        if (index != null) {
            try {
                index.unload();
                log.info(I18N.get("loader.kb.unloaded", roleId));
                return true;
            } catch (Exception e) {
                log.error(I18N.get("loader.kb.unload_failed", roleId, e.getMessage()), e);
                return false;
            }
        }
        return false;
    }

    /**
     * 预热索引 (Preheat indices)
     */
    public void preloadIndices() {
        log.info(I18N.get("loader.kb.preloading"));

        // 获取所有启用的角色 (Get all enabled roles)
        List<Role> enabledRoles = roleManager.getEnabledRoles();

        // 决定预加载哪些角色 (Decide which roles to preload)
        List<Role> rolesToPreload = preloadStrategy.decidePreloadRoles(enabledRoles);

        // 异步预加载 (Asynchronously preload)
        for (Role role : rolesToPreload) {
            preloadIndexAsync(role.getId());
        }

        log.info(I18N.get("loader.kb.preload_scheduled", rolesToPreload.size()));
    }

    /**
     * 获取缓存统计 (Get cache statistics)
     *
     * @return 缓存统计 (Cache statistics)
     */
    public LRUCache.CacheStatistics getCacheStatistics() {
        return cache.getStatistics();
    }

    /**
     * 获取加载统计 (Get loading statistics)
     *
     * @return 加载统计 (Loading statistics)
     */
    public LoadingStats.StatsReport getLoadingStatistics() {
        return loadingStats.generateReport();
    }

    /**
     * 刷新预加载策略 (Refresh preload strategy)
     */
    public void refreshPreloadStrategy() {
        log.info(I18N.get("loader.kb.refreshing_preload"));
        preloadIndices();
    }

    /**
     * 加载索引 (Load index)
     *
     * @param roleId 角色ID (Role ID)
     * @return 索引 (Index)
     * @throws IOException 如果加载失败 (If loading fails)
     */
    private RoleVectorIndex loadIndex(String roleId) throws IOException {
        // 获取角色 (Get role)
        Role role = roleManager.getRole(roleId);
        if (role == null) {
            throw new IOException("Role not found: " + roleId);
        }

        // 从IndexBuilder获取索引 (Get index from IndexBuilder)
        RoleVectorIndex index = indexBuilder.getOrCreateIndex(role);

        // 加载索引 (Load index)
        index.load();

        return index;
    }
}

