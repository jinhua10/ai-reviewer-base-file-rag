package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 流式问答配置属性 (Streaming QA Configuration Properties)
 *
 * <p>
 * 用于配置流式问答相关的参数，包括 SSE 超时时间、分块大小等
 * (Used to configure streaming QA parameters, including SSE timeout, chunk size, etc.)
 * </p>
 *
 * <p>
 * 配置示例 (Configuration example):
 * <pre>
 * streaming:
 *   qa:
 *     timeout-ms: 180000        # 3分钟超时
 *     chunk-size: 5             # 每块5个字符
 *     chunk-delay-ms: 50        # 每块延迟50毫秒
 * </pre>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "streaming.qa")
public class StreamingQAProperties {

    /**
     * SSE 连接超时时间（毫秒）
     * SSE connection timeout (milliseconds)
     *
     * <p>
     * 默认值：180000 (3分钟)
     * Default: 180000 (3 minutes)
     * </p>
     *
     * <p>
     * 建议范围 (Recommended range):
     * <ul>
     *   <li>简单查询 (Simple queries): 60000 - 120000 (1-2分钟)</li>
     *   <li>复杂查询 (Complex queries): 180000 - 300000 (3-5分钟)</li>
     *   <li>超大文档/多图片 (Large docs/many images): 300000 - 600000 (5-10分钟)</li>
     * </ul>
     * </p>
     */
    private Long timeoutMs = 180000L;

    /**
     * 模拟流式输出时的分块大小（字符数）
     * Chunk size for simulated streaming (characters)
     *
     * <p>
     * 默认值：5（逐字显示效果）
     * Default: 5 (character-by-character effect)
     * </p>
     *
     * <p>
     * 说明：在非真实流式场景下，将完整答案分块发送时每块的大小
     * Note: When simulating streaming, size of each chunk when sending complete answer
     * </p>
     */
    private Integer chunkSize = 5;

    /**
     * 模拟流式输出时的延迟（毫秒）
     * Delay for simulated streaming (milliseconds)
     *
     * <p>
     * 默认值：50（较快的打字速度）
     * Default: 50 (fast typing speed)
     * </p>
     *
     * <p>
     * 说明：每个分块之间的延迟，用于模拟打字效果
     * Note: Delay between chunks to simulate typing effect
     * </p>
     */
    private Long chunkDelayMs = 50L;

    /**
     * 获取超时时间（秒）
     * Get timeout in seconds
     *
     * @return 超时时间（秒）
     */
    public long getTimeoutSeconds() {
        return timeoutMs / 1000;
    }

    /**
     * 设置超时时间（秒）
     * Set timeout in seconds
     *
     * @param seconds 超时时间（秒）
     */
    public void setTimeoutSeconds(long seconds) {
        this.timeoutMs = seconds * 1000;
    }
}

