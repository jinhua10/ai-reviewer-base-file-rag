package top.yumbo.ai.rag.service.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.llm.OpenAILLMClient;

/**
 * 模型切换服务 (Model Switch Service)
 *
 * 提供动态切换 LLM 模型的功能
 * (Provides dynamic LLM model switching functionality)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class ModelSwitchService {

    @Autowired(required = false)
    private LLMClient llmClient;

    @Autowired
    private KnowledgeQAProperties properties;

    // 当前模型配置 (Current model configuration)
    private ModelConfig currentConfig;

    // 预定义模型配置 (Predefined model configurations)
    private static final ModelConfig LOCAL_MODEL = new ModelConfig(
        "local",
        "http://localhost:11434/v1/chat/completions",
        "qwen2.5:latest",
        "Ollama 本地模型"
    );

    private static final ModelConfig ONLINE_OPENAI = new ModelConfig(
        "online-openai",
        "https://api.openai.com/v1/chat/completions",
        "gpt-4o",
        "OpenAI GPT-4o"
    );

    private static final ModelConfig ONLINE_DEEPSEEK = new ModelConfig(
        "online-deepseek",
        "https://api.deepseek.com/v1/chat/completions",
        "deepseek-chat",
        "DeepSeek Chat"
    );

    /**
     * 切换模型 (Switch model)
     *
     * @param modelType 模型类型 (Model type): local, online-openai, online-deepseek, custom
     * @param customEndpoint 自定义端点 (Custom endpoint, optional)
     * @param customModel 自定义模型名 (Custom model name, optional)
     * @return 切换结果 (Switch result)
     */
    public SwitchResult switchModel(String modelType, String customEndpoint, String customModel) {
        log.info(I18N.get("model.switch.start"), modelType);

        try {
            ModelConfig targetConfig = switch (modelType.toLowerCase()) {
                case "local" -> LOCAL_MODEL;
                case "online-openai", "online" -> ONLINE_OPENAI;
                case "online-deepseek", "deepseek" -> ONLINE_DEEPSEEK;
                case "custom" -> {
                    if (customEndpoint == null || customModel == null) {
                        throw new IllegalArgumentException(I18N.get("model.switch.custom_required"));
                    }
                    yield new ModelConfig("custom", customEndpoint, customModel, "自定义模型");
                }
                default -> throw new IllegalArgumentException(I18N.get("model.switch.unknown_type", modelType));
            };

            // 根据类型选择配置 (Select configuration by type)

            // 更新配置 (Update configuration)
            updateLLMConfig(targetConfig);

            // 保存当前配置 (Save current configuration)
            currentConfig = targetConfig;

            SwitchResult result = new SwitchResult();
            result.setSuccess(true);
            result.setMessage(I18N.get("model.switch.success", targetConfig.getDescription()));
            result.setModelType(targetConfig.getType());
            result.setEndpoint(targetConfig.getEndpoint());
            result.setModel(targetConfig.getModel());
            result.setDescription(targetConfig.getDescription());

            log.info(I18N.get("model.switch.complete"), targetConfig.getDescription());
            return result;

        } catch (Exception e) {
            log.error(I18N.get("model.switch.failed", e.getMessage()), e);

            SwitchResult result = new SwitchResult();
            result.setSuccess(false);
            result.setMessage(I18N.get("model.switch.failed", e.getMessage()));
            return result;
        }
    }

    /**
     * 更新 LLM 配置 (Update LLM configuration)
     */
    private void updateLLMConfig(ModelConfig config) {
        // 更新配置对象 (Update configuration object)
        KnowledgeQAProperties.LlmConfig llmConfig = properties.getLlm();
        llmConfig.setApiUrl(config.getEndpoint());
        llmConfig.setModel(config.getModel());

        // 如果 LLM 客户端是 OpenAILLMClient，尝试更新 (If LLM client is OpenAILLMClient, try to update)
        if (llmClient instanceof OpenAILLMClient) {
            log.info(I18N.get("model.switch.recreating"));
            // 注意：实际生产环境中，需要重新创建 Bean 或使用可配置的客户端
            // 这里只更新配置，新的请求会使用新配置
            // (Note: In production, need to recreate Bean or use configurable client)
            // (Here we only update config, new requests will use new config)
        }

        log.info(I18N.get("model.switch.config_updated"), config.getEndpoint(), config.getModel());
    }

    /**
     * 获取当前模型配置 (Get current model configuration)
     */
    public ModelConfig getCurrentConfig() {
        if (currentConfig == null) {
            // 从配置文件读取当前配置 (Read current config from properties)
            KnowledgeQAProperties.LlmConfig llmConfig = properties.getLlm();
            currentConfig = new ModelConfig(
                "current",
                llmConfig.getApiUrl(),
                llmConfig.getModel(),
                "当前配置"
            );
        }
        return currentConfig;
    }

    /**
     * 获取所有可用模型 (Get all available models)
     */
    public java.util.List<ModelConfig> getAvailableModels() {
        return java.util.Arrays.asList(LOCAL_MODEL, ONLINE_OPENAI, ONLINE_DEEPSEEK);
    }

    // ==================== 内部类 (Inner Classes) ====================

    /**
     * 模型配置 (Model Configuration)
     */
    @Data
    public static class ModelConfig {
        private String type;
        private String endpoint;
        private String model;
        private String description;

        public ModelConfig(String type, String endpoint, String model, String description) {
            this.type = type;
            this.endpoint = endpoint;
            this.model = model;
            this.description = description;
        }
    }

    /**
     * 切换结果 (Switch Result)
     */
    @Data
    public static class SwitchResult {
        private boolean success;
        private String message;
        private String modelType;
        private String endpoint;
        private String model;
        private String description;
    }
}

