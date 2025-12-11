package top.yumbo.ai.rag.ai.engine;

import lombok.Data;
import lombok.Getter;

import java.util.Map;

/**
 * AI 引擎接口 (AI Engine Interface)
 * 统一的 AI 引擎抽象，支持多种实现方案
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
public interface AIEngine {

    /**
     * 健康检查 (Health check)
     *
     * @return true 如果引擎可用 (true if engine is available)
     */
    boolean healthCheck();

    /**
     * 生成回答 (Generate response)
     *
     * @param prompt 提示词 (Prompt)
     * @param options 选项 (Options)
     * @return 生成的回答 (Generated response)
     */
    String generate(String prompt, GenerateOptions options);

    /**
     * 流式生成回答 (Stream generate response)
     *
     * @param prompt 提示词 (Prompt)
     * @param options 选项 (Options)
     * @param callback 回调函数，接收每个 token (Callback for each token)
     */
    void generateStream(String prompt, GenerateOptions options, StreamCallback callback);

    /**
     * 获取模型信息 (Get model info)
     *
     * @return 模型信息字符串 (Model info string)
     */
    String getModelInfo();

    /**
     * 估算性能等级 (Estimate performance level)
     *
     * @return 性能等级 (Performance level)
     */
    PerformanceLevel estimatePerformance();

    /**
     * 获取引擎配置 (Get engine configuration)
     *
     * @return 配置映射 (Configuration map)
     */
    Map<String, Object> getConfiguration();

    /**
     * 更新引擎配置 (Update engine configuration)
     *
     * @param config 新配置 (New configuration)
     */
    void updateConfiguration(Map<String, Object> config);

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 生成选项 (Generate Options)
     */
    @Data
    class GenerateOptions {
        private String model;           // 模型名称 (Model name)
        private double temperature;     // 温度 (Temperature)
        private int maxTokens;          // 最大 token 数 (Max tokens)
        private double topP;            // Top-p 采样 (Top-p sampling)
        private String systemPrompt;    // 系统提示 (System prompt)

        public GenerateOptions() {
            // 默认值 (Default values)
            this.temperature = 0.7;
            this.maxTokens = 2048;
            this.topP = 0.9;
        }
    }

    /**
     * 流式回调接口 (Stream Callback Interface)
     */
    @FunctionalInterface
    interface StreamCallback {
        /**
         * 接收每个生成的 token
         *
         * @param token 生成的 token
         * @param done 是否完成 (Whether done)
         */
        void onToken(String token, boolean done);
    }

    /**
     * 性能等级 (Performance Level)
     */
    @Getter
    enum PerformanceLevel {
        HIGH("high", "高性能", "High"),           // > 100 tokens/s
        MEDIUM("medium", "中等", "Medium"),       // 30-100 tokens/s
        LOW("low", "较低", "Low"),                // 10-30 tokens/s
        VERY_LOW("very_low", "很低", "Very Low"), // < 10 tokens/s
        UNKNOWN("unknown", "未知", "Unknown");

        private final String code;
        private final String labelCn;
        private final String labelEn;

        PerformanceLevel(String code, String labelCn, String labelEn) {
            this.code = code;
            this.labelCn = labelCn;
            this.labelEn = labelEn;
        }

    }
}

