package top.yumbo.ai.rag.spring.boot.model;

/**
 * 流式消息类型枚举 (Streaming Message Type Enum)
 *
 * @author AI Reviewer Team
 * @since 2025-12-10
 */
public enum StreamMessageType {

    /**
     * HOPE 快速答案 (HOPE quick answer)
     * 从 HOPE 三层记忆中检索的快速答案
     */
    HOPE_ANSWER,

    /**
     * LLM 流式块 (LLM streaming chunk)
     * LLM 生成的文本块，逐块发送
     */
    LLM_CHUNK,

    /**
     * LLM 生成完成 (LLM generation complete)
     * 标识 LLM 生成已完成
     */
    LLM_COMPLETE,

    /**
     * 错误消息 (Error message)
     * 发生错误时的消息
     */
    ERROR
}

