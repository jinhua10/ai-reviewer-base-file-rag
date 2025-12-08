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

