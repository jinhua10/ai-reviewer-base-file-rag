package top.yumbo.ai.rag.spring.boot.streaming.model;

import lombok.Data;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingSession;

import java.util.concurrent.CompletableFuture;

/**
 * 流式响应对象
 * (Streaming Response Object)
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Data
public class StreamingResponse {
    private String sessionId;
    private String question;
    private CompletableFuture<HOPEAnswer> hopeFuture;
    private StreamingSession llmSession;
}

