package top.yumbo.ai.rag.ai.engine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 引擎管理器 (AI Engine Manager)
 * 支持多种 AI 引擎方案的灵活切换
 *
 * 支持的方案 (Supported Solutions):
 * 1. 本地 Ollama 服务 (Local Ollama Service)
 * 2. 在线 Ollama 服务 (Remote Ollama Service)
 * 3. 在线 API 服务 (Online API Service) - GPT/Claude/Gemini
 * 4. 混合模式 (Hybrid Mode) - 智能路由
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class AIEngineManager {

    /**
     * 已注册的引擎 (Registered engines)
     */
    private final Map<EngineType, AIEngine> engines = new ConcurrentHashMap<>();

    /**
     * 当前激活的引擎类型 (Current active engine type)
     */
    private EngineType activeEngineType = EngineType.AUTO;

    /**
     * 引擎健康状态缓存 (Engine health cache)
     */
    private final Map<EngineType, EngineHealth> healthCache = new ConcurrentHashMap<>();

    // ========== 初始化 (Initialization) ==========

    public AIEngineManager() {
        initializeEngines();
    }

    /**
     * 初始化所有支持的引擎 (Initialize all supported engines)
     */
    private void initializeEngines() {
        // 引擎将在具体实现类中注册 (Engines will be registered in implementation classes)
        log.info(I18N.get("ai.engine.manager.initialized"));
    }

    /**
     * 注册引擎 (Register engine)
     */
    public void registerEngine(EngineType type, AIEngine engine) {
        engines.put(type, engine);
        log.info(I18N.get("ai.engine.registered"), type.getNameCn(), type.getNameEn());
    }

    // ========== 引擎选择与切换 (Engine Selection & Switching) ==========

    /**
     * 获取可用引擎 (Get available engine)
     * 根据当前配置和健康状态自动选择最佳引擎
     */
    public AIEngine getEngine() {
        if (activeEngineType == EngineType.AUTO) {
            return selectBestEngine();
        }

        AIEngine engine = engines.get(activeEngineType);
        if (engine != null && isHealthy(activeEngineType)) {
            return engine;
        }

        // 当前引擎不可用，自动降级 (Current engine unavailable, auto fallback)
        log.warn(I18N.get("ai.engine.fallback"), activeEngineType.getNameCn());
        return selectBestEngine();
    }

    /**
     * 自动选择最佳引擎 (Auto select best engine)
     *
     * 优先级策略 (Priority strategy):
     * 1. 本地 Ollama（如果可用且性能足够）
     * 2. 在线 Ollama（如果配置了）
     * 3. 在线 API（托底方案）
     */
    private AIEngine selectBestEngine() {
        // 1. 尝试本地 Ollama (Try local Ollama)
        if (isHealthy(EngineType.LOCAL_OLLAMA)) {
            log.info(I18N.get("ai.engine.selected"), EngineType.LOCAL_OLLAMA.getNameCn());
            return engines.get(EngineType.LOCAL_OLLAMA);
        }

        // 2. 尝试在线 Ollama (Try remote Ollama)
        if (isHealthy(EngineType.REMOTE_OLLAMA)) {
            log.info(I18N.get("ai.engine.selected"), EngineType.REMOTE_OLLAMA.getNameCn());
            return engines.get(EngineType.REMOTE_OLLAMA);
        }

        // 3. 使用在线 API（托底）(Use online API as fallback)
        log.info(I18N.get("ai.engine.selected"), EngineType.ONLINE_API.getNameCn());
        return engines.get(EngineType.ONLINE_API);
    }

    /**
     * 手动切换引擎 (Manually switch engine)
     */
    public void switchEngine(EngineType targetType) {
        if (!engines.containsKey(targetType)) {
            throw new IllegalArgumentException(
                I18N.get("ai.engine.not_found", targetType.getNameCn())
            );
        }

        EngineType oldType = activeEngineType;
        activeEngineType = targetType;

        log.info(I18N.get("ai.engine.switched"), oldType.getNameCn(), targetType.getNameCn());
    }

    // ========== 健康检查 (Health Check) ==========

    /**
     * 检查引擎是否健康 (Check if engine is healthy)
     */
    public boolean isHealthy(EngineType type) {
        // 从缓存获取 (Get from cache)
        EngineHealth health = healthCache.get(type);

        // 缓存未命中或已过期，重新检查 (Cache miss or expired, recheck)
        if (health == null || health.isExpired()) {
            health = checkEngineHealth(type);
            healthCache.put(type, health);
        }

        return health.isHealthy();
    }

    /**
     * 执行健康检查 (Perform health check)
     */
    private EngineHealth checkEngineHealth(EngineType type) {
        AIEngine engine = engines.get(type);
        if (engine == null) {
            return EngineHealth.unhealthy(I18N.get("ai.engine.not_registered"));
        }

        try {
            // 调用引擎的健康检查方法 (Call engine's health check)
            boolean healthy = engine.healthCheck();
            return healthy ?
                EngineHealth.healthy() :
                EngineHealth.unhealthy(I18N.get("ai.engine.health_check_failed"));

        } catch (Exception e) {
            log.warn(I18N.get("ai.engine.health_check_error"), type.getNameCn(), e.getMessage());
            return EngineHealth.unhealthy(e.getMessage());
        }
    }

    /**
     * 获取所有引擎状态 (Get all engines status)
     */
    public Map<EngineType, EngineStatus> getAllEnginesStatus() {
        Map<EngineType, EngineStatus> statusMap = new HashMap<>();

        for (EngineType type : EngineType.values()) {
            if (type == EngineType.AUTO) continue;

            AIEngine engine = engines.get(type);
            if (engine != null) {
                boolean healthy = isHealthy(type);
                EngineStatus status = new EngineStatus(
                    type,
                    healthy,
                    engine.getModelInfo(),
                    engine.estimatePerformance()
                );
                statusMap.put(type, status);
            }
        }

        return statusMap;
    }

    // ========== 引擎类型枚举 (Engine Type Enum) ==========

    /**
     * 引擎类型 (Engine Type)
     */
    public enum EngineType {
        /**
         * 自动选择 (Auto select)
         */
        AUTO("auto", "自动选择", "Auto Select"),

        /**
         * 本地 Ollama 服务 (Local Ollama)
         * 适用场景: 有独立显卡或性能较好的 CPU
         */
        LOCAL_OLLAMA("local_ollama", "本地 Ollama", "Local Ollama"),

        /**
         * 在线 Ollama 服务 (Remote Ollama)
         * 适用场景: 公司内网有 Ollama 服务器
         */
        REMOTE_OLLAMA("remote_ollama", "在线 Ollama", "Remote Ollama"),

        /**
         * 在线 API 服务 (Online API)
         * 适用场景: GPT/Claude/Gemini 等商业 API
         */
        ONLINE_API("online_api", "在线 API", "Online API");

        private final String code;
        private final String nameCn;
        private final String nameEn;

        EngineType(String code, String nameCn, String nameEn) {
            this.code = code;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

        public String getCode() { return code; }
        public String getNameCn() { return nameCn; }
        public String getNameEn() { return nameEn; }
    }

    // ========== 健康状态类 (Health Status Class) ==========

    /**
     * 引擎健康状态 (Engine Health)
     */
    @Data
    public static class EngineHealth {
        private boolean healthy;
        private String reason;
        private long checkTime;
        private static final long CACHE_TTL = 30_000; // 30秒缓存 (30s cache)

        public static EngineHealth healthy() {
            EngineHealth health = new EngineHealth();
            health.healthy = true;
            health.checkTime = System.currentTimeMillis();
            return health;
        }

        public static EngineHealth unhealthy(String reason) {
            EngineHealth health = new EngineHealth();
            health.healthy = false;
            health.reason = reason;
            health.checkTime = System.currentTimeMillis();
            return health;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - checkTime > CACHE_TTL;
        }
    }

    /**
     * 引擎状态 (Engine Status)
     */
    @Data
    public static class EngineStatus {
        private final EngineType type;
        private final boolean available;
        private final String modelInfo;
        private final AIEngine.PerformanceLevel performance;
    }
}

