package top.yumbo.ai.rag.ppl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * PPL 服务统一配置
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge.qa.ppl")
public class PPLConfig {

    /**
     * 默认提供商：onnx, ollama, openai
     */
    private String defaultProvider = "onnx";

    /**
     * 启用降级策略
     * true: 当前提供商失败时自动切换到备用提供商
     */
    private boolean enableFallback = true;

    /**
     * 降级顺序（优先级从高到低）
     */
    private List<String> fallbackOrder = Arrays.asList("onnx", "ollama", "openai");

    /**
     * ONNX 配置
     */
    private OnnxConfig onnx = new OnnxConfig();

    /**
     * Ollama 配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * OpenAI 配置
     */
    private OpenAIConfig openai = new OpenAIConfig();

    /**
     * Chunking 配置
     */
    private ChunkConfig chunking = new ChunkConfig();

    /**
     * Reranking 配置
     */
    private RerankConfig reranking = new RerankConfig();

    /**
     * ONNX 配置
     */
    @Data
    public static class OnnxConfig {
        /**
         * 是否启用 ONNX
         */
        private boolean enabled = true;

        /**
         * 模型文件路径
         */
        private String modelPath = "./models/gpt2-medium-int8/model.onnx";

        /**
         * Tokenizer 文件路径
         */
        private String tokenizerPath = "./models/gpt2-medium-int8/tokenizer.json";

        /**
         * 最大批处理大小
         */
        private int maxBatchSize = 8;

        /**
         * 是否启用缓存
         */
        private boolean useCache = true;

        /**
         * 缓存大小
         */
        private int cacheSize = 10000;

        /**
         * 缓存过期时间（秒）
         */
        private int cacheTtl = 3600;
    }

    /**
     * Ollama 配置（使用国产 Qwen 模型）
     */
    @Data
    public static class OllamaConfig {
        /**
         * 是否启用 Ollama
         */
        private boolean enabled = false;

        /**
         * Ollama 服务地址
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * 使用的模型（阿里通义千问 Qwen）
         * qwen2.5:0.5b - 轻量级，适合 PPL 计算（推荐）
         * qwen2.5:1.5b - 平衡性能
         * qwen2.5:7b - 高质量
         */
        private String model = "qwen2.5:0.5b";

        /**
         * 请求超时时间（毫秒）
         */
        private int timeout = 30000;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 连接池大小
         */
        private int connectionPoolSize = 10;
    }

    /**
     * OpenAI 配置（兼容通义千问 API）
     */
    @Data
    public static class OpenAIConfig {
        /**
         * 是否启用 OpenAI
         */
        private boolean enabled = false;

        /**
         * API Key（支持通义千问）
         * 环境变量：QW_API_KEY 或 AI_API_KEY
         */
        private String apiKey = "${QW_API_KEY:${AI_API_KEY:}}";

        /**
         * 使用的模型
         * 通义千问：qwen-turbo（快速）、qwen-plus（平衡）、qwen-max（高质量）
         * OpenAI：gpt-4o、gpt-4-turbo
         */
        private String model = "qwen-turbo";

        /**
         * API 端点
         * 通义千问：https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
         * OpenAI：https://api.openai.com/v1/chat/completions
         */
        private String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        /**
         * 请求超时时间（毫秒）
         */
        private int timeout = 60000;

        /**
         * 是否使用 logprobs（用于计算 PPL）
         * 注意：通义千问暂不支持 logprobs
         */
        private boolean useLogprobs = false;
    }

    /**
     * 验证配置的有效性
     */
    public void validate() {
        if (defaultProvider == null || defaultProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultProvider cannot be empty");
        }

        // 验证默认提供商是否在有效列表中
        try {
            top.yumbo.ai.rag.ppl.PPLProviderType.fromString(defaultProvider);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid defaultProvider: " + defaultProvider);
        }

        // 验证子配置
        if (chunking != null) {
            chunking.validate();
        }
        if (reranking != null) {
            reranking.validate();
        }
    }
}

