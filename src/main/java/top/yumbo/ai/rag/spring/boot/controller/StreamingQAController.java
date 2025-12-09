package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.streaming.HybridStreamingService;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 流式响应控制器
 * (Streaming Response Controller)
 *
 * 提供双轨流式响应 API
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Slf4j
@RestController
@RequestMapping("/api/qa/stream")
@CrossOrigin
public class StreamingQAController {

    private final HybridStreamingService streamingService;

    public StreamingQAController(HybridStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * 发起流式问答
     * (Initiate streaming Q&A)
     *
     * POST /api/qa/stream
     *
     * @param request 请求体
     * @return 会话信息和 HOPE 快速答案
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> ask(@RequestBody StreamingRequest request) {
        log.info("📝 收到流式问答请求 (Received streaming Q&A request): question={}",
            request.getQuestion());

        try {
            // 启动双轨响应
            // (Start dual-track response)
            var response = streamingService.ask(request.getQuestion(), request.getUserId());

            // 等待 HOPE 快速答案（通常 <300ms）
            // (Wait for HOPE fast answer, usually <300ms)
            HOPEAnswer hopeAnswer = null;
            try {
                hopeAnswer = response.getHopeFuture().get();
            } catch (Exception e) {
                log.warn("获取 HOPE 答案失败 (Failed to get HOPE answer): {}", e.getMessage());
            }

            // 返回会话信息
            // (Return session info)
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", response.getSessionId());
            result.put("question", response.getQuestion());
            result.put("hopeAnswer", hopeAnswer);
            result.put("sseUrl", "/api/qa/stream/" + response.getSessionId());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("流式问答失败 (Streaming Q&A failed): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 订阅 LLM 流式输出（SSE）
     * (Subscribe to LLM streaming output via SSE)
     *
     * GET /api/qa/stream/{sessionId}
     *
     * @param sessionId 会话ID
     * @return SSE 流
     */
    @GetMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String sessionId) {
        log.info("📡 客户端订阅流式输出 (Client subscribed to streaming): sessionId={}", sessionId);

        SseEmitter emitter = streamingService.createSSEStream(sessionId);

        if (emitter == null) {
            log.warn("会话不存在 (Session not found): sessionId={}", sessionId);
            // 返回错误的 SSE
            emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Session not found"));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送错误失败 (Failed to send error): {}", e.getMessage());
            }
        }

        return emitter;
    }

    /**
     * 获取会话状态
     * (Get session status)
     *
     * GET /api/qa/stream/{sessionId}/status
     *
     * @param sessionId 会话ID
     * @return 会话状态
     */
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String sessionId) {
        StreamingSession session = streamingService.getSession(sessionId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);
        status.put("status", session.getStatus().name());
        status.put("progress", session.getProgress());
        status.put("durationSeconds", session.getDurationSeconds());
        status.put("answerLength", session.getFullAnswer().length());

        return ResponseEntity.ok(status);
    }

    /**
     * 双轨流式响应（HOPE + LLM）
     * (Dual-track streaming response - HOPE + LLM)
     *
     * 同时返回 HOPE 快速答案和 LLM 流式生成
     * (Returns both HOPE quick answer and LLM streaming generation)
     *
     * GET /api/qa/stream/dual-track?question=xxx&sessionId=xxx
     *
     * @param question 用户问题 (User question)
     * @param sessionId HOPE 会话ID（可选）(HOPE session ID, optional)
     * @return SSE 流，包含 HOPE 答案和 LLM 块 (SSE stream with HOPE answer and LLM chunks)
     */
    @GetMapping(value = "/dual-track", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter dualTrackStreaming(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId) {

        log.info(I18N.get("log.streaming.dual_track_start", question));

        SseEmitter emitter = new SseEmitter(60000L); // 60 秒超时

        // 生成 HOPE 会话 ID（如果没有提供）
        String hopeSessionId = sessionId != null ? sessionId :
            "hope_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);

        // 异步处理双轨响应
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 启动双轨服务
                var response = streamingService.ask(question, hopeSessionId);

                // 2. 等待 HOPE 快速答案（带超时）
                CompletableFuture<HOPEAnswer> hopeFuture = response.getHopeFuture();

                HOPEAnswer hopeAnswer = null;
                long hopeStartTime = System.currentTimeMillis();

                try {
                    // 等待最多 300ms
                    hopeAnswer = hopeFuture.get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
                    long hopeTime = System.currentTimeMillis() - hopeStartTime;

                    // 发送 HOPE 答案
                    if (hopeAnswer != null && hopeAnswer.getAnswer() != null && !hopeAnswer.getAnswer().isEmpty()) {
                        top.yumbo.ai.rag.spring.boot.model.StreamMessage hopeMsg =
                            top.yumbo.ai.rag.spring.boot.model.StreamMessage.hopeAnswer(
                                hopeAnswer.getAnswer(),
                                hopeAnswer.getSource(),
                                hopeAnswer.getConfidence(),
                                hopeTime,
                                hopeAnswer.isCanDirectAnswer() ? "DIRECT_ANSWER" : "REFERENCE"
                            );

                        emitter.send(SseEmitter.event()
                            .name("hope")
                            .data(hopeMsg));

                        log.info(I18N.get("log.streaming.hope_sent", hopeTime));
                    }
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn(I18N.get("log.streaming.hope_timeout"));
                    // HOPE 超时，继续 LLM 生成
                } catch (Exception e) {
                    log.error(I18N.get("log.streaming.hope_error"), e);
                }

                // 3. 已经在 streamingService.ask() 中启动了 LLM 生成
                // 通过会话 ID 获取流式输出
                StreamingSession session = streamingService.getSession(response.getSessionId());
                if (session != null) {
                    // 监听 LLM 流式输出
                    int chunkIndex = 0;
                    long llmStartTime = System.currentTimeMillis();
                    int lastLength = 0;

                    while (session.getStatus() == top.yumbo.ai.rag.spring.boot.streaming.model.SessionStatus.STREAMING) {
                        String currentAnswer = session.getFullAnswer().toString();

                        // 发送新的块（仅发送新增内容）
                        if (currentAnswer.length() > lastLength) {
                            String newChunk = currentAnswer.substring(lastLength);
                            top.yumbo.ai.rag.spring.boot.model.StreamMessage llmMsg =
                                top.yumbo.ai.rag.spring.boot.model.StreamMessage.llmChunk(
                                    newChunk,
                                    chunkIndex++
                                );

                            emitter.send(SseEmitter.event()
                                .name("llm")
                                .data(llmMsg));

                            lastLength = currentAnswer.length();
                        }

                        Thread.sleep(100); // 100ms 轮询间隔
                    }

                    // 发送完成消息
                    long llmTime = System.currentTimeMillis() - llmStartTime;
                    top.yumbo.ai.rag.spring.boot.model.StreamMessage completeMsg =
                        top.yumbo.ai.rag.spring.boot.model.StreamMessage.llmComplete(
                            chunkIndex,
                            llmTime
                        );

                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completeMsg));

                    log.info(I18N.get("log.streaming.llm_complete", chunkIndex, llmTime));
                }

                emitter.complete();
                log.info(I18N.get("log.streaming.dual_track_complete"));

            } catch (Exception e) {
                log.error(I18N.get("log.streaming.dual_track_error"), e);

                try {
                    top.yumbo.ai.rag.spring.boot.model.StreamMessage errorMsg =
                        top.yumbo.ai.rag.spring.boot.model.StreamMessage.error(
                            I18N.get("error.streaming.failed", e.getMessage())
                        );

                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorMsg));

                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    log.error(I18N.get("log.streaming.error_send_failed"), sendError);
                }
            }
        });

        // 设置超时和错误回调
        emitter.onTimeout(() -> {
            log.warn(I18N.get("log.streaming.timeout"));
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error(I18N.get("log.streaming.connection_error"), e);
        });

        return emitter;
    }
}

/**
 * 流式请求
 * (Streaming request)
 */
@Data
class StreamingRequest {
    private String question;
    private String userId;
    private String language;  // 可选：zh/en
}

