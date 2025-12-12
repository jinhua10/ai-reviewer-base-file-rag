package top.yumbo.ai.rag.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置管理服务 (System Configuration Management Service)
 * <p>
 * 提供动态更新和管理系统配置的功能
 * (Provides dynamic system configuration update and management)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class SystemConfigService {

    @Autowired
    private KnowledgeQAProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 配置文件路径 (Configuration file path)
    private static final String CONFIG_DIR = "data/config";
    private static final String SYSTEM_CONFIG_FILE = CONFIG_DIR + "/system-config.json";
    private static final String MODEL_CONFIG_FILE = CONFIG_DIR + "/model-config.json";

    /**
     * 更新系统配置 (Update system configuration)
     *
     * @param config 配置项 (Configuration items)
     * @return 更新结果 (Update result)
     */
    public ConfigUpdateResult updateSystemConfig(Map<String, Object> config) {
        log.info(I18N.get("admin.config.updating"), "system");

        try {
            // 1. 验证配置 (Validate configuration)
            validateSystemConfig(config);

            // 2. 应用配置到当前属性 (Apply configuration to current properties)
            applySystemConfig(config);

            // 3. 持久化配置到文件 (Persist configuration to file)
            saveConfigToFile(SYSTEM_CONFIG_FILE, config);

            ConfigUpdateResult result = new ConfigUpdateResult();
            result.setSuccess(true);
            result.setMessage(I18N.get("admin.config.updated_success", "系统配置"));
            result.setTimestamp(LocalDateTime.now());
            result.setConfigType("system");

            log.info(I18N.get("admin.config.updated"), "system");
            return result;

        } catch (Exception e) {
            log.error(I18N.get("admin.config.update_failed", "system", e.getMessage()), e);

            ConfigUpdateResult result = new ConfigUpdateResult();
            result.setSuccess(false);
            result.setMessage(I18N.get("admin.config.update_failed", "system", e.getMessage()));
            result.setTimestamp(LocalDateTime.now());
            return result;
        }
    }

    /**
     * 更新模型配置 (Update model configuration)
     *
     * @param config 配置项 (Configuration items)
     * @return 更新结果 (Update result)
     */
    public ConfigUpdateResult updateModelConfig(Map<String, Object> config) {
        log.info(I18N.get("admin.config.updating"), "model");

        try {
            // 1. 验证配置 (Validate configuration)
            validateModelConfig(config);

            // 2. 应用配置到 LLM 属性 (Apply configuration to LLM properties)
            applyModelConfig(config);

            // 3. 持久化配置到文件 (Persist configuration to file)
            saveConfigToFile(MODEL_CONFIG_FILE, config);

            ConfigUpdateResult result = new ConfigUpdateResult();
            result.setSuccess(true);
            result.setMessage(I18N.get("admin.config.updated_success", "模型配置"));
            result.setTimestamp(LocalDateTime.now());
            result.setConfigType("model");

            log.info(I18N.get("admin.config.updated"), "model");
            return result;

        } catch (Exception e) {
            log.error(I18N.get("admin.config.update_failed", "model", e.getMessage()), e);

            ConfigUpdateResult result = new ConfigUpdateResult();
            result.setSuccess(false);
            result.setMessage(I18N.get("admin.config.update_failed", "model", e.getMessage()));
            result.setTimestamp(LocalDateTime.now());
            return result;
        }
    }

    /**
     * 获取当前系统配置 (Get current system configuration)
     */
    public Map<String, Object> getCurrentSystemConfig() {
        Map<String, Object> config = new HashMap<>();

        // 从当前属性读取 (Read from current properties)
        if (properties.getDocument() != null) {
            config.put("maxFileSizeMb", properties.getDocument().getMaxFileSizeMb());
            config.put("maxContentSizeMb", properties.getDocument().getMaxContentSizeMb());
            config.put("chunkSize", properties.getDocument().getChunkSize());
            config.put("supportedFormats", properties.getDocument().getSupportedFormats());
        }

        if (properties.getKnowledgeBase() != null) {
            config.put("storagePath", properties.getKnowledgeBase().getStoragePath());
            config.put("rebuildOnStartup", properties.getKnowledgeBase().isRebuildOnStartup());
            config.put("enableCache", properties.getKnowledgeBase().isEnableCache());
        }

        if (properties.getCache() != null) {
            config.put("cacheTtlMinutes", properties.getCache().getTtlMinutes());
            config.put("cacheMaxSize", properties.getCache().getMaxSize());
        }

        return config;
    }

    /**
     * 获取当前模型配置 (Get current model configuration)
     */
    public Map<String, Object> getCurrentModelConfig() {
        Map<String, Object> config = new HashMap<>();

        KnowledgeQAProperties.LlmConfig llmConfig = properties.getLlm();
        if (llmConfig != null) {
            config.put("provider", llmConfig.getProvider());
            config.put("model", llmConfig.getModel());
            config.put("apiUrl", llmConfig.getApiUrl());
            config.put("maxContextLength", llmConfig.getMaxContextLength());
            config.put("maxDocLength", llmConfig.getMaxDocLength());
            config.put("maxDocumentsPerQuery", llmConfig.getMaxDocumentsPerQuery());
        }

        return config;
    }

    // ==================== 私有辅助方法 (Private helper methods) ====================

    /**
     * 验证系统配置 (Validate system configuration)
     */
    private void validateSystemConfig(Map<String, Object> config) {
        // 验证必要字段 (Validate required fields)
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException(I18N.get("admin.config.empty"));
        }

        // 可以添加更多验证逻辑 (Can add more validation logic)
        log.debug(I18N.get("admin.config.validated"), "system");
    }

    /**
     * 验证模型配置 (Validate model configuration)
     */
    private void validateModelConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException(I18N.get("admin.config.empty"));
        }

        log.debug(I18N.get("admin.config.validated"), "model");
    }

    /**
     * 应用系统配置 (Apply system configuration)
     * <p>
     * 注意：配置类的大部分字段是只读的（没有setter）
     * (Note: Most configuration class fields are read-only without setters)
     * 这里只记录配置更改，实际应用需要重启服务
     * (Changes are logged here, actual application requires service restart)
     */
    private void applySystemConfig(Map<String, Object> config) {
        // 记录配置更改（Configuration changes are logged)
        log.info(I18N.get("admin.config.applied"), "system");
        log.info("System config to be applied (requires restart): {}", config);

        // 注意：由于 Spring Boot 配置类的限制，大部分配置需要重启服务才能生效
        // (Note: Due to Spring Boot config class limitations, most changes require service restart)
    }

    /**
     * 应用模型配置 (Apply model configuration)
     */
    private void applyModelConfig(Map<String, Object> config) {
        KnowledgeQAProperties.LlmConfig llmConfig = properties.getLlm();
        if (llmConfig == null) {
            return;
        }

        if (config.containsKey("model")) {
            llmConfig.setModel((String) config.get("model"));
        }

        if (config.containsKey("apiUrl")) {
            llmConfig.setApiUrl((String) config.get("apiUrl"));
        }

        if (config.containsKey("maxContextLength")) {
            Object value = config.get("maxContextLength");
            if (value instanceof Integer) {
                llmConfig.setMaxContextLength((Integer) value);
            }
        }

        if (config.containsKey("maxDocLength")) {
            Object value = config.get("maxDocLength");
            if (value instanceof Integer) {
                llmConfig.setMaxDocLength((Integer) value);
            }
        }

        if (config.containsKey("maxDocumentsPerQuery")) {
            Object value = config.get("maxDocumentsPerQuery");
            if (value instanceof Integer) {
                llmConfig.setMaxDocumentsPerQuery((Integer) value);
            }
        }

        log.info(I18N.get("admin.config.applied"), "model");
    }

    /**
     * 保存配置到文件 (Save configuration to file)
     */
    private void saveConfigToFile(String filePath, Map<String, Object> config) throws Exception {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(filePath);
        try (FileWriter writer = new FileWriter(configFile)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            writer.write(json);
        }

        log.info(I18N.get("admin.config.saved"), filePath);
    }

    // ==================== 内部类 (Inner Classes) ====================

    /**
     * 配置更新结果 (Configuration Update Result)
     */
    @Data
    public static class ConfigUpdateResult {
        private boolean success;
        private String message;
        private LocalDateTime timestamp;
        private String configType;
    }
}

