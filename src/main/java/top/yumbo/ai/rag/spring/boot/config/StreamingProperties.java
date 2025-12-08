package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 流式响应配置
 * (Streaming Response Configuration)
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "knowledge.qa.streaming")
public class StreamingProperties {

    /**
     * 是否启用流式响应
     * (Enable streaming response)
     */
    private boolean enabled = true;

    /**
     * HOPE 快速查询超时（毫秒）
     * (HOPE fast query timeout in milliseconds)
     */
    private long hopeQueryTimeout = 300;

    /**
     * LLM 流式超时（毫秒）
     * (LLM streaming timeout in milliseconds)
     */
    private long llmStreamingTimeout = 300000;  // 5分钟

    /**
     * SSE 超时（毫秒）
     * (SSE timeout in milliseconds)
     */
    private long sseTimeout = 300000;  // 5分钟

    /**
     * 是否保存中断会话的草稿
     * (Save draft for interrupted sessions)
     */
    private boolean saveDraft = true;

    /**
     * 草稿保存阈值
     * (Draft save threshold)
     */
    private DraftThreshold draftThreshold = new DraftThreshold();

    /**
     * 会话有效性判断
     * (Session validity criteria)
     */
    private ValidityCriteria validityCriteria = new ValidityCriteria();

    @Data
    public static class DraftThreshold {
        /**
         * 最小进度百分比（保存草稿）
         * (Minimum progress percentage to save draft)
         */
        private double minProgress = 0.8;  // 80%

        /**
         * 最小答案长度（字符）
         * (Minimum answer length in characters)
         */
        private int minAnswerLength = 200;

        /**
         * 最小停留时间（秒）
         * (Minimum dwell time in seconds)
         */
        private long minDwellTime = 10;
    }

    @Data
    public static class ValidityCriteria {
        /**
         * 最小答案长度（字符）
         * (Minimum answer length in characters)
         */
        private int minAnswerLength = 50;

        /**
         * 最小生成时长（秒）
         * (Minimum generation duration in seconds)
         */
        private long minDuration = 2;
    }
}

