package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
import top.yumbo.ai.rag.spring.boot.streaming.model.SessionStatus;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingSession;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingResponse;
import top.yumbo.ai.rag.optimization.SmartContextBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 混合流式响应服务
 * (Hybrid Streaming Response Service)
 *
 * 提供双轨响应：
 * 1. HOPE 快速答案 (<300ms)
 * 2. LLM 流式生成 (TTFB <1s)
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Slf4j
@Service
public class HybridStreamingService {

    private final HOPEFastQueryService hopeFastQueryService;
    private final LLMClient llmClient;
    private final StreamingSessionMonitor sessionMonitor;
    private final SmartContextBuilder contextBuilder;

    // 为了兼容现有接口，我们需要这些依赖
    // (For compatibility with existing interfaces, we need these dependencies)
    private final top.yumbo.ai.rag.service.LocalFileRAG rag;
    private final top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine embeddingEngine;
    private final top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine vectorIndexEngine;

    // 活跃会话管理
    // (Active session management)
    private final Map<String, StreamingSession> activeSessions = new ConcurrentHashMap<>();

    public HybridStreamingService(HOPEFastQueryService hopeFastQueryService,
                                   LLMClient llmClient,
                                   StreamingSessionMonitor sessionMonitor,
                                   SmartContextBuilder contextBuilder,
                                   top.yumbo.ai.rag.service.LocalFileRAG rag,
                                   top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine embeddingEngine,
                                   top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine vectorIndexEngine) {
        this.hopeFastQueryService = hopeFastQueryService;
        this.llmClient = llmClient;
        this.sessionMonitor = sessionMonitor;
        this.contextBuilder = contextBuilder;
        this.rag = rag;
        this.embeddingEngine = embeddingEngine;
        this.vectorIndexEngine = vectorIndexEngine;
    }

    /**
     * 双轨响应：同时提供 HOPE 快速答案和 LLM 流式生成
     * (Dual-track response: HOPE fast answer + LLM streaming)
     *
     * @param question 用户问题
     * @param userId 用户ID
     * @return 流式响应对象
     */
    public StreamingResponse ask(String question, String userId) {
        long startTime = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();

        log.info("🚀 启动双轨响应 (Starting dual-track response): sessionId={}, question={}",
            sessionId, question);

        // 1. 快速查询 HOPE（目标 <300ms）
        // (Quick query HOPE, target <300ms)
        CompletableFuture<HOPEAnswer> hopeFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return hopeFastQueryService.queryFast(question, sessionId);
            } catch (Exception e) {
                log.warn("HOPE 快速查询失败 (HOPE fast query failed): {}", e.getMessage());
                return HOPEAnswer.builder()
                    .canDirectAnswer(false)
                    .source(HOPEAnswer.SourceType.NONE)
                    .build();
            }
        });

        // 2. 启动 LLM 流式生成（目标 TTFB <1s）
        // (Start LLM streaming, target TTFB <1s)
        StreamingSession llmSession = startLLMStreaming(question, sessionId, userId);

        // 3. 创建响应对象
        // (Create response object)
        StreamingResponse response = new StreamingResponse();
        response.setSessionId(sessionId);
        response.setQuestion(question);
        response.setHopeFuture(hopeFuture);
        response.setLlmSession(llmSession);

        log.info("✅ 双轨响应已启动 (Dual-track response started): sessionId={}, duration={}ms",
            sessionId, System.currentTimeMillis() - startTime);

        return response;
    }

    /**
     * 启动 LLM 流式生成
     * (Start LLM streaming generation)
     */
    private StreamingSession startLLMStreaming(String question, String sessionId, String userId) {
        StreamingSession session = new StreamingSession(sessionId, question);
        session.setUserId(userId);

        // 注册会话到监控器
        // (Register session to monitor)
        activeSessions.put(sessionId, session);
        sessionMonitor.registerSession(session);

        // 异步启动流式生成
        // (Start streaming generation asynchronously)
        CompletableFuture.runAsync(() -> {
            try {
                log.debug("开始 LLM 流式生成 (Starting LLM streaming): sessionId={}", sessionId);

                // 1. 简单关键词检索获取文档
                // (Simple keyword search to get documents)
                top.yumbo.ai.rag.model.Query query = top.yumbo.ai.rag.model.Query.builder()
                    .queryText(question)
                    .limit(5)
                    .build();

                top.yumbo.ai.rag.model.SearchResult searchResult = rag.search(query);
                List<Document> docs = searchResult.getDocuments().stream()
                    .map(top.yumbo.ai.rag.model.ScoredDocument::getDocument)
                    .collect(java.util.stream.Collectors.toList());

                // 2. 构建上下文
                // (Build context)
                String context = contextBuilder.buildSmartContext(question, docs);

                // 3. 调用 LLM 流式接口（需要 LLMClient 支持）
                // (Call LLM streaming interface - requires LLMClient support)
                streamFromLLM(session, question, context);

            } catch (Exception e) {
                log.error("LLM 流式生成失败 (LLM streaming failed): sessionId={}, error={}",
                    sessionId, e.getMessage(), e);
                session.markError(e);
                sessionMonitor.onSessionComplete(sessionId);
            }
        });

        return session;
    }

    /**
     * 从 LLM 流式获取响应
     * (Stream response from LLM)
     *
     * 使用 LLMClient 的 Flux 流式接口（响应式流）
     * (Use LLMClient's Flux streaming interface - Reactive Streams)
     */
    private void streamFromLLM(StreamingSession session, String question, String context) {
        // 检查是否支持流式
        // (Check if streaming is supported)
        if (!llmClient.supportsStreaming()) {
            log.warn("⚠️ LLM 客户端不支持流式输出 (LLM client doesn't support streaming): sessionId={}",
                session.getSessionId());

            // 降级：使用同步方式
            // (Fallback: use synchronous method)
            try {
                String prompt = String.format("请根据以下上下文回答问题：\n\n上下文：\n%s\n\n问题：%s", context, question);
                String fullAnswer = llmClient.generate(prompt);

                // 一次性发送完整答案
                // (Send full answer at once)
                session.appendChunk(fullAnswer);
                session.notifySubscribers(fullAnswer);
                session.markComplete();
                sessionMonitor.onSessionComplete(session.getSessionId());

                log.info("✅ 使用同步方式完成 (Completed with synchronous mode): sessionId={}",
                    session.getSessionId());
            } catch (Exception e) {
                log.error("❌ 同步生成错误 (Synchronous generation error): sessionId={}, error={}",
                    session.getSessionId(), e.getMessage());
                session.markError(e);
                sessionMonitor.onSessionComplete(session.getSessionId());
            }
            return;
        }

        // 构建完整的提示词 (Build complete prompt)
        String prompt = String.format("请根据以下上下文回答问题：\n\n上下文：\n%s\n\n问题：%s", context, question);

        // 使用 Flux 响应式流接口
        // (Use Flux reactive streaming interface)
        llmClient.generateStream(prompt)
            .subscribe(
                // onNext: 每个文本块到达时
                // (onNext: when each text chunk arrives)
                chunk -> {
                    session.appendChunk(chunk);
                    session.notifySubscribers(chunk);
                },
                // onError: 错误时
                // (onError: when error occurs)
                error -> {
                    log.error("❌ LLM 流式生成错误 (LLM streaming error): sessionId={}, error={}",
                        session.getSessionId(), error.getMessage());
                    session.markError(error instanceof Exception ?
                        (Exception) error : new RuntimeException(error));
                    sessionMonitor.onSessionComplete(session.getSessionId());
                },
                // onComplete: 完成时
                // (onComplete: when completed)
                () -> {
                    session.markComplete();
                    sessionMonitor.onSessionComplete(session.getSessionId());
                    log.debug("✅ LLM 流式生成完成 (LLM streaming completed): sessionId={}",
                        session.getSessionId());
                }
            );
    }

    /**
     * 创建 SSE 流
     * (Create SSE stream)
     *
     * 用于前端订阅 LLM 流式输出
     * (For frontend to subscribe to LLM streaming output)
     */
    public SseEmitter createSSEStream(String sessionId) {
        StreamingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("会话不存在 (Session not found): sessionId={}", sessionId);
            return null;
        }

        SseEmitter emitter = new SseEmitter(300000L);  // 5分钟超时

        // 订阅会话的输出
        // (Subscribe to session output)
        session.addSubscriber(chunk -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(chunk));
            } catch (IOException e) {
                log.warn("发送 SSE 数据失败 (Failed to send SSE data): {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        // 会话完成时关闭 SSE
        // (Close SSE when session completes)
        CompletableFuture.runAsync(() -> {
            while (session.getStatus() == SessionStatus.STREAMING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("done"));
                emitter.complete();
            } catch (IOException e) {
                log.warn("关闭 SSE 失败 (Failed to close SSE): {}", e.getMessage());
            }
        });

        // 处理客户端断开
        // (Handle client disconnect)
        emitter.onCompletion(() -> {
            log.debug("SSE 连接正常关闭 (SSE connection closed normally): sessionId={}", sessionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时 (SSE connection timeout): sessionId={}", sessionId);
            sessionMonitor.onClientDisconnect(sessionId, "SSE timeout");
        });

        emitter.onError(throwable -> {
            log.warn("SSE 连接错误 (SSE connection error): sessionId={}, error={}",
                sessionId, throwable.getMessage());
            sessionMonitor.onClientDisconnect(sessionId, "SSE error: " + throwable.getMessage());
        });

        return emitter;
    }

    /**
     * 获取会话
     * (Get session)
     */
    public StreamingSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 移除会话
     * (Remove session)
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
}


