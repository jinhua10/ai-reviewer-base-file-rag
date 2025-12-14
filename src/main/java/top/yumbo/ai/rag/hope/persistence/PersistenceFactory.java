package top.yumbo.ai.rag.hope.persistence;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 持久化工厂
 * (Persistence Factory)
 *
 * <p>
 * 职责 (Responsibilities):
 * <ul>
 *   <li>✅ 创建持久化实例 - 根据策略动态创建</li>
 *   <li>✅ 实例缓存 - 单例模式，避免重复创建</li>
 *   <li>✅ 策略切换 - 运行时切换持久化后端</li>
 *   <li>✅ 依赖注入支持 - 集成Spring容器</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用示例 (Usage Example):
 * <pre>{@code
 * // 获取默认实例
 * QuestionClassifierPersistence persistence = factory.getDefaultInstance();
 *
 * // 获取指定策略实例
 * QuestionClassifierPersistence redis = factory.getInstance(PersistenceStrategy.REDIS);
 *
 * // 切换策略
 * factory.switchStrategy(PersistenceStrategy.H2_DATABASE);
 * }</pre>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.1.0
 */
@Slf4j
@Component
public class PersistenceFactory {

    private final ApplicationContext applicationContext;

    /**
     * 实例缓存 (Instance cache)
     * Key: 策略代码
     * Value: 持久化实例
     */
    private final Map<String, QuestionClassifierPersistence> instanceCache = new ConcurrentHashMap<>();

    /**
     * 当前激活的策略 (Current active strategy)
     * -- GETTER --
     * 获取当前策略 (Get current strategy)
     *
     * @return 当前策略
     */
    @Getter
    private volatile PersistenceStrategy currentStrategy;

    public PersistenceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.currentStrategy = PersistenceStrategy.JSON_FILE;  // 默认策略
    }

    /**
     * 获取默认实例 (Get default instance)
     *
     * @return 持久化实例
     */
    public QuestionClassifierPersistence getDefaultInstance() {
        return getInstance(currentStrategy);
    }

    /**
     * 获取指定策略的实例 (Get instance by strategy)
     *
     * @param strategy 持久化策略
     * @return 持久化实例
     */
    public QuestionClassifierPersistence getInstance(PersistenceStrategy strategy) {
        return instanceCache.computeIfAbsent(strategy.getCode(), key -> {
            try {
                log.info("Creating persistence instance: {} ({})",
                        strategy.getDescription(), strategy.getCode());

                // 1. 尝试从Spring容器获取（如果已经注册为Bean）
                QuestionClassifierPersistence bean = tryGetFromSpringContext(strategy);
                if (bean != null) {
                    log.info("✅ Found persistence bean in Spring context: {}", strategy.getCode());
                    return bean;
                }

                // 2. 通过反射创建实例
                QuestionClassifierPersistence instance = createInstance(strategy);
                log.info("✅ Created persistence instance: {}", strategy.getCode());

                return instance;

            } catch (Exception e) {
                log.error("❌ Failed to create persistence instance: {}", strategy.getCode(), e);

                // 降级到JSON文件存储
                if (strategy != PersistenceStrategy.JSON_FILE) {
                    log.warn("⚠️ Fallback to JSON_FILE strategy");
                    return getInstance(PersistenceStrategy.JSON_FILE);
                }

                throw new RuntimeException("Failed to create persistence instance", e);
            }
        });
    }

    /**
     * 切换策略 (Switch strategy)
     *
     * @param newStrategy 新策略
     * @return 是否成功
     */
    public boolean switchStrategy(PersistenceStrategy newStrategy) {
        try {
            // 验证新策略是否可用
            QuestionClassifierPersistence newInstance = getInstance(newStrategy);
            if (newInstance == null) {
                log.error("❌ Cannot switch to strategy: {} (instance is null)", newStrategy.getCode());
                return false;
            }

            PersistenceStrategy oldStrategy = currentStrategy;
            currentStrategy = newStrategy;

            log.info("✅ Switched persistence strategy: {} → {}",
                    oldStrategy.getDescription(), newStrategy.getDescription());

            return true;

        } catch (Exception e) {
            log.error("❌ Failed to switch strategy to: {}", newStrategy.getCode(), e);
            return false;
        }
    }

    /**
     * 从Spring容器获取实例 (Try get from Spring context)
     */
    private QuestionClassifierPersistence tryGetFromSpringContext(PersistenceStrategy strategy) {
        try {
            // 尝试通过类型获取
            Class<?> clazz = Class.forName(strategy.getImplementationClass());
            if (QuestionClassifierPersistence.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends QuestionClassifierPersistence> persistenceClass =
                        (Class<? extends QuestionClassifierPersistence>) clazz;

                Map<String, ? extends QuestionClassifierPersistence> beans =
                        applicationContext.getBeansOfType(persistenceClass);

                if (!beans.isEmpty()) {
                    return beans.values().iterator().next();
                }
            }
        } catch (ClassNotFoundException e) {
            log.debug("Implementation class not found: {}", strategy.getImplementationClass());
        } catch (Exception e) {
            log.debug("Cannot get bean from Spring context: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 通过反射创建实例 (Create instance by reflection)
     */
    private QuestionClassifierPersistence createInstance(PersistenceStrategy strategy) throws Exception {
        Class<?> clazz = Class.forName(strategy.getImplementationClass());

        if (!QuestionClassifierPersistence.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "Implementation class must implement QuestionClassifierPersistence: " +
                            strategy.getImplementationClass()
            );
        }

        @SuppressWarnings("unchecked")
        Class<? extends QuestionClassifierPersistence> persistenceClass =
                (Class<? extends QuestionClassifierPersistence>) clazz;

        // 尝试使用Spring容器注入依赖
        try {
            return applicationContext.getAutowireCapableBeanFactory()
                    .createBean(persistenceClass);
        } catch (Exception e) {
            log.debug("Cannot autowire bean, trying default constructor: {}", e.getMessage());
        }

        // 使用无参构造函数
        Constructor<? extends QuestionClassifierPersistence> constructor =
                persistenceClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        return constructor.newInstance();
    }

    /**
     * 清除缓存 (Clear cache)
     */
    public void clearCache() {
        instanceCache.clear();
        log.info("Persistence instance cache cleared");
    }

    /**
     * 获取所有可用策略 (Get all available strategies)
     *
     * @return 策略数组
     */
    public PersistenceStrategy[] getAvailableStrategies() {
        return PersistenceStrategy.values();
    }

    /**
     * 检查策略是否可用 (Check if strategy is available)
     *
     * @param strategy 策略
     * @return 是否可用
     */
    public boolean isStrategyAvailable(PersistenceStrategy strategy) {
        try {
            QuestionClassifierPersistence instance = getInstance(strategy);
            return instance != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取策略信息 (Get strategy info)
     *
     * @return 策略信息映射
     */
    public Map<String, Object> getStrategyInfo() {
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("current", currentStrategy.getCode());
        info.put("currentDescription", currentStrategy.getDescription());
        info.put("available", getAvailableStrategies().length);
        info.put("cached", instanceCache.size());

        return info;
    }
}

