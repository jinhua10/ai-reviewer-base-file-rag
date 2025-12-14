package top.yumbo.ai.rag.hope.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.QuestionClassifier.QuestionTypeConfig;
import top.yumbo.ai.rag.hope.persistence.config.PersistenceConfig;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 持久化管理器
 * (Persistence Manager)
 *
 * <p>
 * 职责 (Responsibilities):
 * <ul>
 *   <li>✅ 统一入口 - 提供统一的持久化访问接口</li>
 *   <li>✅ 策略管理 - 根据配置选择和切换策略</li>
 *   <li>✅ 降级处理 - 当前策略失败时自动降级</li>
 *   <li>✅ 健康检查 - 监控持久化服务健康状态</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例 (Usage Example):
 * <pre>{@code
 * @Autowired
 * private PersistenceManager manager;
 *
 * // 保存数据（自动使用配置的策略）
 * manager.saveQuestionType(config);
 *
 * // 切换策略
 * manager.switchStrategy(PersistenceStrategy.REDIS);
 *
 * // 获取健康状态
 * Map<String, Object> health = manager.getHealthInfo();
 * }</pre>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
@Slf4j
@Service
public class PersistenceManager {

    @Autowired
    private PersistenceFactory factory;

    @Autowired
    private PersistenceConfig config;

    private volatile QuestionClassifierPersistence currentPersistence;

    @PostConstruct
    public void init() {
        try {
            // 从配置加载策略
            PersistenceStrategy strategy = PersistenceStrategy.fromCode(config.getStrategy());

            log.info("Initializing persistence manager with strategy: {} ({})",
                    strategy.getDescription(), strategy.getCode());

            // 切换到配置的策略
            switchStrategy(strategy);

            log.info("✅ Persistence manager initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize persistence manager", e);

            // 降级到JSON文件存储
            try {
                switchStrategy(PersistenceStrategy.JSON_FILE);
                log.warn("⚠️ Fallback to JSON_FILE strategy");
            } catch (Exception ex) {
                log.error("❌ Fatal: Cannot initialize any persistence strategy", ex);
                throw new RuntimeException("Failed to initialize persistence manager", ex);
            }
        }
    }

    /**
     * 切换策略 (Switch strategy)
     *
     * @param strategy 新策略
     * @return 是否成功
     */
    public boolean switchStrategy(PersistenceStrategy strategy) {
        try {
            boolean success = factory.switchStrategy(strategy);
            if (success) {
                currentPersistence = factory.getDefaultInstance();
                log.info("✅ Switched to strategy: {}", strategy.getDescription());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("❌ Failed to switch strategy: {}", strategy.getCode(), e);
            return false;
        }
    }

    /**
     * 获取当前策略 (Get current strategy)
     */
    public PersistenceStrategy getCurrentStrategy() {
        return factory.getCurrentStrategy();
    }

    // ============================================================
    // 代理方法 - 转发到当前持久化实例 (Proxy methods)
    // ============================================================

    public boolean saveQuestionType(QuestionTypeConfig config) {
        return executeWithFallback(() -> currentPersistence.saveQuestionType(config));
    }

    public int saveQuestionTypes(List<QuestionTypeConfig> configs) {
        return executeWithFallback(() -> currentPersistence.saveQuestionTypes(configs));
    }

    public Optional<QuestionTypeConfig> getQuestionType(String typeId) {
        return executeWithFallback(() -> currentPersistence.getQuestionType(typeId));
    }

    public List<QuestionTypeConfig> getAllQuestionTypes() {
        return executeWithFallback(() -> currentPersistence.getAllQuestionTypes());
    }

    public boolean updateQuestionType(QuestionTypeConfig config) {
        return executeWithFallback(() -> currentPersistence.updateQuestionType(config));
    }

    public boolean deleteQuestionType(String typeId) {
        return executeWithFallback(() -> currentPersistence.deleteQuestionType(typeId));
    }

    public boolean saveKeywords(String typeId, List<String> keywords) {
        return executeWithFallback(() -> currentPersistence.saveKeywords(typeId, keywords));
    }

    public boolean addKeywords(String typeId, List<String> keywords) {
        return executeWithFallback(() -> currentPersistence.addKeywords(typeId, keywords));
    }

    public List<String> getKeywords(String typeId) {
        return executeWithFallback(() -> currentPersistence.getKeywords(typeId));
    }

    public Map<String, List<String>> getAllKeywords() {
        return executeWithFallback(() -> currentPersistence.getAllKeywords());
    }

    public boolean savePatterns(String typeId, List<String> patterns) {
        return executeWithFallback(() -> currentPersistence.savePatterns(typeId, patterns));
    }

    public boolean addPatterns(String typeId, List<String> patterns) {
        return executeWithFallback(() -> currentPersistence.addPatterns(typeId, patterns));
    }

    public List<String> getPatterns(String typeId) {
        return executeWithFallback(() -> currentPersistence.getPatterns(typeId));
    }

    public Map<String, List<String>> getAllPatterns() {
        return executeWithFallback(() -> currentPersistence.getAllPatterns());
    }

    public String createBackup() {
        return executeWithFallback(() -> currentPersistence.createBackup());
    }

    public boolean restoreFromBackup(String backupId) {
        return executeWithFallback(() -> currentPersistence.restoreFromBackup(backupId));
    }

    public List<String> listBackups() {
        return executeWithFallback(() -> currentPersistence.listBackups());
    }

    public String getVersion() {
        return executeWithFallback(() -> currentPersistence.getVersion());
    }

    public boolean saveVersion(String version) {
        return executeWithFallback(() -> currentPersistence.saveVersion(version));
    }

    public List<QuestionClassifierPersistence.ChangeRecord> getChangeHistory(int limit) {
        return executeWithFallback(() -> currentPersistence.getChangeHistory(limit));
    }

    public boolean recordChange(QuestionClassifierPersistence.ChangeRecord change) {
        return executeWithFallback(() -> currentPersistence.recordChange(change));
    }

    /**
     * 执行操作并处理降级 (Execute with fallback)
     */
    private <T> T executeWithFallback(java.util.function.Supplier<T> operation) {
        try {
            return operation.get();
        } catch (Exception e) {
            log.error("❌ Persistence operation failed with strategy: {}",
                    getCurrentStrategy().getCode(), e);

            // 如果当前不是JSON文件策略，尝试降级
            if (getCurrentStrategy() != PersistenceStrategy.JSON_FILE) {
                log.warn("⚠️ Attempting to fallback to JSON_FILE strategy");

                try {
                    switchStrategy(PersistenceStrategy.JSON_FILE);
                    return operation.get();
                } catch (Exception ex) {
                    log.error("❌ Fallback also failed", ex);
                }
            }

            throw new RuntimeException("Persistence operation failed", e);
        }
    }

    /**
     * 获取健康信息 (Get health info)
     */
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 基本信息
            health.put("status", "UP");
            health.put("strategy", getCurrentStrategy().getCode());
            health.put("strategyDescription", getCurrentStrategy().getDescription());

            // 测试读取操作
            long startTime = System.currentTimeMillis();
            List<QuestionTypeConfig> types = getAllQuestionTypes();
            long readTime = System.currentTimeMillis() - startTime;

            health.put("typeCount", types.size());
            health.put("readLatency", readTime + "ms");

            // 工厂信息
            health.put("factoryInfo", factory.getStrategyInfo());

            return health;

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return health;
        }
    }

    /**
     * 获取所有可用策略 (Get available strategies)
     */
    public List<Map<String, Object>> getAvailableStrategies() {
        List<Map<String, Object>> strategies = new ArrayList<>();

        for (PersistenceStrategy strategy : factory.getAvailableStrategies()) {
            Map<String, Object> info = new HashMap<>();
            info.put("code", strategy.getCode());
            info.put("description", strategy.getDescription());
            info.put("implementationClass", strategy.getImplementationClass());
            info.put("available", factory.isStrategyAvailable(strategy));
            info.put("current", strategy == getCurrentStrategy());

            strategies.add(info);
        }

        return strategies;
    }
}

