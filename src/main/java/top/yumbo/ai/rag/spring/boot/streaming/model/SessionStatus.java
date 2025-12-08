package top.yumbo.ai.rag.spring.boot.streaming.model;

/**
 * 会话状态
 * (Session Status)
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
public enum SessionStatus {
    /**
     * 正在流式输出
     * (Currently streaming)
     */
    STREAMING,

    /**
     * 已完成
     * (Completed)
     */
    COMPLETED,

    /**
     * 已中断
     * (Interrupted)
     */
    INTERRUPTED,

    /**
     * 发生错误
     * (Error occurred)
     */
    ERROR
}

