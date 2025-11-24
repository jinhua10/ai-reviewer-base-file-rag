package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.llm.MockLLMClient;
import top.yumbo.ai.rag.spring.boot.llm.OpenAILLMClient;

/**
 * LLM 客户端配置
 *
 * 支持多种 LLM 提供商：
 * - openai: OpenAI 兼容 API（默认，支持 OpenAI、DeepSeek 等）
 * - mock: Mock 模式（测试用，返回固定回答）
 *
 * OpenAI 兼容 API 说明：
 * OpenAILLMClient 支持所有 OpenAI API 兼容的服务，包括：
 * - OpenAI (GPT-4o, GPT-4, GPT-3.5)
 * - DeepSeek (deepseek-chat)
 * - 其他兼容 OpenAI API 格式的服务
 *
 * 通过配置不同的 api-url 和 model 即可切换不同的服务
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    private final KnowledgeQAProperties properties;

    public LLMConfiguration(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * OpenAI 兼容 LLM 客户端（默认）
     * 支持 OpenAI、DeepSeek 等所有 OpenAI API 兼容的服务
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "knowledge.qa.llm",
        name = "provider",
        havingValue = "openai",
        matchIfMissing = true  // 默认使用 openai（支持所有兼容 API）
    )
    @ConditionalOnMissingBean
    public LLMClient openAILLMClient() {
        String apiKey = resolveEnvVariable(properties.getLlm().getApiKey());
        String model = properties.getLlm().getModel();
        String apiUrl = properties.getLlm().getApiUrl();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("⚠️  未配置 LLM API Key");
            log.warn("💡 提示: 设置环境变量:");
            log.warn("      - DeepSeek: export AI_API_KEY=your-deepseek-key");
            log.warn("      - OpenAI: export OPENAI_API_KEY=your-openai-key");
            log.warn("💡 将降级使用 Mock 模式");
            return new MockLLMClient();
        }

        // 根据 API URL 判断使用的服务
        String serviceName = "OpenAI";
        if (apiUrl != null && apiUrl.contains("deepseek")) {
            serviceName = "DeepSeek";
        }

        log.info("🤖 创建 {} LLM 客户端", serviceName);
        log.info("   - 模型: {}", model);
        log.info("   - API: {}", apiUrl);

        return new OpenAILLMClient(apiKey, model, apiUrl);
    }

    /**
     * Mock LLM 客户端（测试用）
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "knowledge.qa.llm",
        name = "provider",
        havingValue = "mock"
    )
    @ConditionalOnMissingBean
    public LLMClient mockLLMClient() {
        log.info("🤖 创建 Mock LLM 客户端（仅用于测试）");
        log.info("   ⚠️  Mock 模式将返回固定的模拟回答");
        log.info("   💡 如需使用真实 LLM，请配置:");
        log.info("      knowledge.qa.llm.provider=openai");
        log.info("      并设置相应的 API Key 和 URL");
        return new MockLLMClient();
    }

    /**
     * 解析环境变量占位符
     */
    private String resolveEnvVariable(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 处理 ${VAR:default} 格式
        if (value.startsWith("${") && value.endsWith("}")) {
            String content = value.substring(2, value.length() - 1);
            String[] parts = content.split(":", 2);
            String envVar = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";

            String envValue = System.getenv(envVar);
            return envValue != null && !envValue.isEmpty() ? envValue : defaultValue;
        }

        return value;
    }
}

