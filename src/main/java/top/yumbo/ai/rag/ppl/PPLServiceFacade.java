package top.yumbo.ai.rag.ppl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
 *
 * 功能：
 * 1. 管理多种 PPL 服务实现（ONNX、Ollama、OpenAI）
 * 2. 支持动态切换提供商
 * 3. 支持降级策略
 * 4. 统一的监控和日志
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Slf4j
@Service
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
     * 计算文本困惑度（带降级）
     */
    public double calculatePerplexity(String text) throws PPLException {
        return executeWithFallback(service -> service.calculatePerplexity(text),
                "calculatePerplexity");
    }

    /**
     * 批量计算困惑度（带降级）
     */
    public Map<String, Double> batchCalculatePerplexity(List<String> texts) {
        try {
            return executeWithFallback(service -> service.batchCalculatePerplexity(texts),
                    "batchCalculatePerplexity");
        } catch (PPLException e) {
            log.error("❌ Batch calculate perplexity failed", e);
            // 降级：返回所有失败的结果
            Map<String, Double> results = new HashMap<>();
            texts.forEach(text -> results.put(text, Double.MAX_VALUE));
            return results;
        }
    }

    /**
     * 文档切分（带降级）
     */
    public List<DocumentChunk> chunk(String content, String query) throws PPLException {
        ChunkConfig chunkConfig = config.getChunking();
        return executeWithFallback(service -> service.chunk(content, query, chunkConfig),
                "chunk");
    }

    /**
     * 文档重排序（带降级）
     */
    public List<Document> rerank(String question, List<Document> candidates) throws PPLException {
        RerankConfig rerankConfig = config.getReranking();

        // 检查是否启用
        if (!rerankConfig.isEnabled()) {
            log.debug("PPL Rerank is disabled, returning original order");
            return candidates;
        }

        return executeWithFallback(service -> service.rerank(question, candidates, rerankConfig),
                "rerank");
    }

    /**
     * 切换提供商
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
     * 获取所有可用的提供商
     */
    public List<PPLProviderType> getAvailableProviders() {
        return new ArrayList<>(services.keySet());
    }

    /**
     * 获取所有提供商的健康状态
     */
    public Map<PPLProviderType, Boolean> getHealthStatus() {
        Map<PPLProviderType, Boolean> status = new HashMap<>();
        services.forEach((type, service) -> status.put(type, service.isHealthy()));
        return status;
    }

    /**
     * 获取所有提供商的性能指标
     */
    public Map<PPLProviderType, PPLMetrics> getAllMetrics() {
        Map<PPLProviderType, PPLMetrics> metrics = new HashMap<>();
        services.forEach((type, service) -> metrics.put(type, service.getMetrics()));
        return metrics;
    }

    /**
     * 执行操作并支持降级
     */
    private <T> T executeWithFallback(ServiceOperation<T> operation, String operationName)
            throws PPLException {

        // 尝试当前提供商
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

                // 尝试降级
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
     * 尝试降级到备用提供商
     */
    private <T> T tryFallback(ServiceOperation<T> operation, String operationName, Exception originalException)
            throws PPLException {

        log.info("🔄 Trying fallback for {}...", operationName);

        for (String providerName : config.getFallbackOrder()) {
            PPLProviderType type = PPLProviderType.fromString(providerName);

            // 跳过当前提供商（已经失败）
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

        // 所有降级都失败
        throw new PPLException(currentProvider,
                operationName + " failed and all fallbacks exhausted", originalException);
    }

    /**
     * 获取当前服务实例
     */
    private PPLService getCurrentService() {
        return getService(currentProvider);
    }

    /**
     * 获取指定提供商的服务实例
     */
    private PPLService getService(PPLProviderType type) {
        return services.get(type);
    }

    /**
     * 服务操作函数式接口
     */
    @FunctionalInterface
    private interface ServiceOperation<T> {
        T execute(PPLService service) throws PPLException;
    }
}

