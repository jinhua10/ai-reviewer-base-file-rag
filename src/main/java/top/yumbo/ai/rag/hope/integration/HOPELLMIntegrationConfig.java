package top.yumbo.ai.rag.hope.integration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.monitor.HOPEMonitorService;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

/**
 * HOPE LLM 集成组件
 * (HOPE LLM Integration Component)
 *
 * 提供 HOPE 增强的 LLM 客户端工厂方法
 * (Provides factory method for HOPE enhanced LLM client)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "knowledge.qa.hope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HOPELLMIntegrationConfig {

    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMonitorService hopeMonitor;
    private final HOPELLMIntegrationProperties properties;

    private HOPEEnhancedLLMClient.HOPELLMConfig defaultConfig;

    @Autowired
    public HOPELLMIntegrationConfig(HOPEKnowledgeManager hopeManager,
                                     @Autowired(required = false) HOPEMonitorService hopeMonitor,
                                     HOPELLMIntegrationProperties properties) {
        this.hopeManager = hopeManager;
        this.hopeMonitor = hopeMonitor;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // 1. 创建默认配置 (Create default configuration)
        defaultConfig = new HOPEEnhancedLLMClient.HOPELLMConfig();

        // 2. 从 application.yml 读取配置 (Read configuration from application.yml)
        defaultConfig.setHopeQueryEnabled(properties.isQueryBeforeLlm());
        defaultConfig.setAutoLearnEnabled(properties.isAutoLearnEnabled());
        defaultConfig.setAutoLearnRating(properties.getAutoLearnRating());
        defaultConfig.setReferenceEnhanceEnabled(properties.isReferenceEnhanceEnabled());
        defaultConfig.setMinRatingForLearning(properties.getMinRatingForLearning());

        // 3. 记录初始化成功日志 (Log successful initialization)
        log.info(I18N.get("hope.llm.integration_enabled") + " - autoLearn: {}, rating: {}",
                properties.isAutoLearnEnabled(), properties.getAutoLearnRating());
    }

    /**
     * 包装 LLM 客户端为 HOPE 增强版本
     *
     * @param originalClient 原始 LLM 客户端
     * @return HOPE 增强的 LLM 客户端
     */
    public LLMClient wrapWithHOPE(LLMClient originalClient) {
        // 1. 检查原始客户端是否为空 (Check if original client is null)
        if (originalClient == null) {
            return null;
        }

        // 2. 如果已经是 HOPEEnhancedLLMClient，直接返回 (If already HOPEEnhancedLLMClient, return directly)
        if (originalClient instanceof HOPEEnhancedLLMClient) {
            return originalClient;
        }

        // 3. 创建并返回 HOPE 增强客户端 (Create and return HOPE enhanced client)
        return new HOPEEnhancedLLMClient(originalClient, hopeManager, hopeMonitor, defaultConfig);
    }

    /**
     * 包装 LLM 客户端为 HOPE 增强版本（自定义配置）
     * (Wrap LLM client as HOPE enhanced version with custom configuration)
     * 
     * @param originalClient 原始 LLM 客户端 (Original LLM client)
     * @param config 自定义配置 (Custom configuration)
     * @return HOPE 增强的 LLM 客户端 (HOPE enhanced LLM client)
     */
    public LLMClient wrapWithHOPE(LLMClient originalClient, HOPEEnhancedLLMClient.HOPELLMConfig config) {
        if (originalClient == null) {
            return null;
        }

        if (originalClient instanceof HOPEEnhancedLLMClient) {
            return originalClient;
        }

        return new HOPEEnhancedLLMClient(originalClient, hopeManager, hopeMonitor, config);
    }

    /**
     * 获取默认配置
     * (Get default configuration)
     * 
     * @return 默认配置 (Default configuration)
     */
    public HOPEEnhancedLLMClient.HOPELLMConfig getDefaultConfig() {
        return defaultConfig;
    }
}

