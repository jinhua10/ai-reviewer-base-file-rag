package top.yumbo.ai.rag.ppl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.ppl.config.RerankConfig;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PPL 服务门面类（Facade Pattern）
 * (PPL Service Facade - Facade Pattern)
 *
 * 功能（Features）：
 * 1. 管理多种 PPL 服务实现（ONNX、Ollama、OpenAI）
 *    (Manage multiple PPL service implementations)
 * 2. 支持动态切换提供商
 *    (Support dynamic provider switching)
 * 3. 支持降级策略
 *    (Support fallback strategy)
 * 4. 统一的监控和日志
 *    (Unified monitoring and logging)
 *
 * 注意：不使用 @Service 注解，通过 PPLConfiguration 的 @Bean 方法创建
 * (Note: No @Service annotation, created by @Bean method in PPLConfiguration)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Slf4j
public class PPLServiceFacade {

    private final PPLConfig config;
    private final Map<PPLProviderType, PPLService> services;
    /**
     * -- GETTER --
     *  获取当前提供商
     */
    @Getter
    private volatile PPLProviderType currentProvider;

    public PPLServiceFacade(PPLConfig config, List<PPLService> availableServices) {
        this.config = config;
        this.services = new ConcurrentHashMap<>();

        // 注册所有可用的服务
        for (PPLService service : availableServices) {
            services.put(service.getProviderType(), service);
            log.info("✅ Registered PPL service: {}", service.getProviderType().getDisplayName());
        }

        // 设置默认提供商
        this.currentProvider = PPLProviderType.fromString(config.getDefaultProvider());
    }

    @PostConstruct
    public void init() {
        log.info("🚀 Initializing PPL Service Facade...");

        // 验证配置
        config.validate();

        // 验证默认提供商是否可用
        PPLService defaultService = getService(currentProvider);
        if (defaultService == null) {
            log.warn("⚠️ Default provider {} is not available, trying fallback...",
                    currentProvider.getDisplayName());

            // 尝试降级
            if (config.isEnableFallback()) {
                for (String providerName : config.getFallbackOrder()) {
                    PPLProviderType type = PPLProviderType.fromString(providerName);
                    PPLService service = getService(type);
                    if (service != null && service.isHealthy()) {
                        log.info("✅ Fallback to provider: {}", type.getDisplayName());
                        currentProvider = type;
                        break;
                    }
                }
            }
        }

        log.info("✅ PPL Service Facade initialized with provider: {}",
                currentProvider.getDisplayName());

        // 预热服务
        PPLService service = getCurrentService();
        if (service != null) {
            try {
                service.warmup();
                log.info("✅ Service warmup completed");
            } catch (Exception e) {
                log.warn("⚠️ Service warmup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * 计算文本困惑度（带降级）(Calculate text perplexity with fallback)
     * 
     * @param text 待计算的文本 (Text to calculate)
     * @return 困惑度值 (Perplexity value)
     * @throws PPLException 计算失败时抛出 (Thrown when calculation fails)
     */
    public double calculatePerplexity(String text) throws PPLException {
        return executeWithFallback(service -> service.calculatePerplexity(text),
                "calculatePerplexity");
    }

    /**
     * 批量计算困惑度（带降级）(Batch calculate perplexity with fallback)
     * 
     * @param texts 待计算的文本列表 (List of texts to calculate)
     * @return 文本到困惑度的映射 (Mapping from text to perplexity)
     */
    public Map<String, Double> batchCalculatePerplexity(List<String> texts) {
        try {
            return executeWithFallback(service -> service.batchCalculatePerplexity(texts),
                    "batchCalculatePerplexity");
        } catch (PPLException e) {
            log.error("❌ Batch calculate perplexity failed", e);
            // 降级：返回所有失败的结果 (Fallback: return results with all failures)
            Map<String, Double> results = new HashMap<>();
            texts.forEach(text -> results.put(text, Double.MAX_VALUE));
            return results;
        }
    }

    /**
     * 文档切分（带降级）(Document chunking with fallback)
     * 
     * @param content 文档内容 (Document content)
     * @param query 查询问题 (Query question)
     * @return 切分后的文档块列表 (List of document chunks after chunking)
     * @throws PPLException 切分失败时抛出 (Thrown when chunking fails)
     */
    public List<DocumentChunk> chunk(String content, String query) throws PPLException {
        ChunkConfig chunkConfig = config.getChunking();
        return executeWithFallback(service -> service.chunk(content, query, chunkConfig),
                "chunk");
    }

    /**
     * 文档重排序（带降级）(Document reranking with fallback)
     * 
     * @param question 查询问题 (Query question)
     * @param candidates 候选文档列表 (Candidate document list)
     * @return 重排序后的文档列表 (Reranked document list)
     * @throws PPLException 重排序失败时抛出 (Thrown when reranking fails)
     */
    public List<Document> rerank(String question, List<Document> candidates) throws PPLException {
        RerankConfig rerankConfig = config.getReranking();

        // 检查是否启用 (Check if enabled)
        if (!rerankConfig.isEnabled()) {
            log.debug("PPL Rerank is disabled, returning original order");
            return candidates;
        }

        return executeWithFallback(service -> service.rerank(question, candidates, rerankConfig),
                "rerank");
    }

    /**
     * 切换提供商 (Switch provider)
     * 
     * @param newProvider 新的提供商类型 (New provider type)
     * @throws PPLException 切换失败时抛出 (Thrown when switch fails)
     */
    public synchronized void switchProvider(PPLProviderType newProvider) throws PPLException {
        PPLService service = getService(newProvider);
        if (service == null) {
            throw new PPLException(newProvider, "Provider not available");
        }

        if (!service.isHealthy()) {
            throw new PPLException(newProvider, "Provider is not healthy");
        }

        PPLProviderType oldProvider = currentProvider;
        currentProvider = newProvider;

        log.info("✅ Switched PPL provider: {} → {}",
                oldProvider.getDisplayName(), newProvider.getDisplayName());
    }

    /**
     * 获取所有可用的提供商 (Get all available providers)
     * 
     * @return 提供商类型列表 (List of provider types)
     */
    public List<PPLProviderType> getAvailableProviders() {
        return new ArrayList<>(services.keySet());
    }

    /**
     * 获取所有提供商的健康状态 (Get health status of all providers)
     * 
     * @return 提供商类型到健康状态的映射 (Mapping from provider type to health status)
     */
    public Map<PPLProviderType, Boolean> getHealthStatus() {
        Map<PPLProviderType, Boolean> status = new HashMap<>();
        services.forEach((type, service) -> status.put(type, service.isHealthy()));
        return status;
    }

    /**
     * 获取所有提供商的性能指标 (Get performance metrics of all providers)
     * 
     * @return 提供商类型到性能指标的映射 (Mapping from provider type to performance metrics)
     */
    public Map<PPLProviderType, PPLMetrics> getAllMetrics() {
        Map<PPLProviderType, PPLMetrics> metrics = new HashMap<>();
        services.forEach((type, service) -> metrics.put(type, service.getMetrics()));
        return metrics;
    }

    /**
     * 执行操作并支持降级 (Execute operation with fallback support)
     * 
     * @param operation 要执行的操作 (Operation to execute)
     * @param operationName 操作名称 (Operation name)
     * @return 操作结果 (Operation result)
     * @throws PPLException 操作失败时抛出 (Thrown when operation fails)
     */
    private <T> T executeWithFallback(ServiceOperation<T> operation, String operationName)
            throws PPLException {

        // 尝试当前提供商 (Try current provider)
        PPLService service = getCurrentService();
        if (service != null) {
            try {
                long startTime = System.currentTimeMillis();
                T result = operation.execute(service);
                long elapsed = System.currentTimeMillis() - startTime;

                service.getMetrics().recordSuccess(elapsed);
                log.debug("✅ {} completed in {}ms using {}",
                        operationName, elapsed, currentProvider.getDisplayName());

                return result;
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - System.nanoTime() / 1_000_000;
                service.getMetrics().recordFailure(elapsed);

                log.warn("⚠️ {} failed using {}: {}",
                        operationName, currentProvider.getDisplayName(), e.getMessage());

                // 尝试降级 (Try fallback)
                if (config.isEnableFallback()) {
                    return tryFallback(operation, operationName, e);
                } else {
                    throw new PPLException(currentProvider, operationName + " failed", e);
                }
            }
        } else {
            throw new PPLException(currentProvider, "Service not available");
        }
    }

    /**
     * 尝试降级到备用提供商 (Try fallback to backup provider)
     * 
     * @param operation 要执行的操作 (Operation to execute)
     * @param operationName 操作名称 (Operation name)
     * @param originalException 原始异常 (Original exception)
     * @return 操作结果 (Operation result)
     * @throws PPLException 所有降级都失败时抛出 (Thrown when all fallbacks fail)
     */
    private <T> T tryFallback(ServiceOperation<T> operation, String operationName, Exception originalException)
            throws PPLException {

        log.info("🔄 Trying fallback for {}...", operationName);

        for (String providerName : config.getFallbackOrder()) {
            PPLProviderType type = PPLProviderType.fromString(providerName);

            // 跳过当前提供商（已经失败）(Skip current provider (already failed))
            if (type == currentProvider) {
                continue;
            }

            PPLService service = getService(type);
            if (service != null && service.isHealthy()) {
                try {
                    log.info("🔄 Attempting fallback to {}", type.getDisplayName());

                    long startTime = System.currentTimeMillis();
                    T result = operation.execute(service);
                    long elapsed = System.currentTimeMillis() - startTime;

                    service.getMetrics().recordSuccess(elapsed);
                    log.info("✅ Fallback succeeded using {}", type.getDisplayName());

                    return result;
                } catch (Exception e) {
                    log.warn("⚠️ Fallback to {} failed: {}",
                            type.getDisplayName(), e.getMessage());
                }
            }
        }

        // 所有降级都失败 (All fallbacks failed)
        throw new PPLException(currentProvider,
                operationName + " failed and all fallbacks exhausted", originalException);
    }

    /**
     * 获取当前服务实例 (Get current service instance)
     * 
     * @return 当前服务实例 (Current service instance)
     */
    private PPLService getCurrentService() {
        return getService(currentProvider);
    }

    /**
     * 获取指定提供商的服务实例 (Get service instance of specified provider)
     * 
     * @param type 提供商类型 (Provider type)
     * @return 服务实例 (Service instance)
     */
    private PPLService getService(PPLProviderType type) {
        return services.get(type);
    }

    /**
     * 服务操作函数式接口 (Service operation functional interface)
     */
    @FunctionalInterface
    private interface ServiceOperation<T> {
        /**
         * 执行操作 (Execute operation)
         * 
         * @param 服务实例
         * @return 操作结果
         * @throws PPLException 操作失败时抛出
         */
        T execute(PPLService service) throws PPLException;
    }
}

