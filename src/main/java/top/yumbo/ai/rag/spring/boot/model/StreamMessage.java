package top.yumbo.ai.rag.spring.boot.model;

import lombok.Builder;
import lombok.Data;

/**
 * 流式消息 DTO (Streaming Message DTO)
 * 用于流式双轨响应，包含 HOPE 快速答案和 LLM 流式块
 *
 * @author AI Reviewer Team
 * @since 2025-12-10
 */
@Data
@Builder
public class StreamMessage {

    /**
     * 消息类型 (Message type)
     */
    private StreamMessageType type;

    /**
     * 消息内容 (Message content)
     */
    private String content;

    /**
     * HOPE 来源层 (HOPE source layer)
     * 仅当 type = HOPE_ANSWER 时有值
     */
    private String hopeSource;

    /**
     * HOPE 置信度 (HOPE confidence)
     * 仅当 type = HOPE_ANSWER 时有值
     */
    private Double confidence;

    /**
     * 响应时间（毫秒）(Response time in milliseconds)
     * 仅当 type = HOPE_ANSWER 时有值
     */
    private Long responseTime;

    /**
     * LLM 块索引 (LLM chunk index)
     * 仅当 type = LLM_CHUNK 时有值
     */
    private Integer chunkIndex;

    /**
     * 消息时间戳 (Message timestamp)
     */
    private Long timestamp;

    /**
     * 错误消息 (Error message)
     * 仅当 type = ERROR 时有值
     */
    private String error;

    /**
     * HOPE 策略 (HOPE strategy)
     * 仅当 type = HOPE_ANSWER 时有值
     */
    private String strategy;

    /**
     * 总块数 (Total chunks)
     * 仅当 type = LLM_COMPLETE 时有值
     */
    private Integer totalChunks;

    /**
     * 总耗时（毫秒）(Total time in milliseconds)
     * 仅当 type = LLM_COMPLETE 时有值
     */
    private Long totalTime;

    /**
     * 创建错误消息 (Create error message)
     *
     * @param message 错误信息 (Error message)
     * @return 错误消息对象 (Error message object)
     */
    public static StreamMessage error(String message) {
        return StreamMessage.builder()
            .type(StreamMessageType.ERROR)
            .error(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建 HOPE 答案消息 (Create HOPE answer message)
     *
     * @param content HOPE 答案内容 (HOPE answer content)
     * @param source HOPE 来源层 (HOPE source layer)
     * @param confidence 置信度 (Confidence)
     * @param responseTime 响应时间 (Response time)
     * @param strategy 使用的策略 (Strategy used)
     * @return HOPE 答案消息 (HOPE answer message)
     */
    public static StreamMessage hopeAnswer(String content, String source,
                                           double confidence, long responseTime,
                                           String strategy) {
        return StreamMessage.builder()
            .type(StreamMessageType.HOPE_ANSWER)
            .content(content)
            .hopeSource(source)
            .confidence(confidence)
            .responseTime(responseTime)
            .strategy(strategy)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建 LLM 流式块消息 (Create LLM chunk message)
     *
     * @param content 块内容 (Chunk content)
     * @param index 块索引 (Chunk index)
     * @return LLM 块消息 (LLM chunk message)
     */
    public static StreamMessage llmChunk(String content, int index) {
        return StreamMessage.builder()
            .type(StreamMessageType.LLM_CHUNK)
            .content(content)
            .chunkIndex(index)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建 LLM 完成消息 (Create LLM complete message)
     *
     * @param totalChunks 总块数 (Total chunks)
     * @param totalTime 总耗时 (Total time)
     * @return LLM 完成消息 (LLM complete message)
     */
    public static StreamMessage llmComplete(int totalChunks, long totalTime) {
        return StreamMessage.builder()
            .type(StreamMessageType.LLM_COMPLETE)
            .totalChunks(totalChunks)
            .totalTime(totalTime)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}

