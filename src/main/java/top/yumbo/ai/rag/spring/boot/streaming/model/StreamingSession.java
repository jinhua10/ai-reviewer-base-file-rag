package top.yumbo.ai.rag.spring.boot.streaming.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 流式会话
 * (Streaming Session)
 *
 * 管理 LLM 流式生成的会话状态
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Slf4j
@Data
public class StreamingSession {

    private String sessionId;
    private String question;
    private StringBuilder fullAnswer = new StringBuilder();
    private List<Consumer<String>> subscribers = new ArrayList<>();

    private SessionStatus status = SessionStatus.STREAMING;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime completeTime;

    // 中断容错字段
    // (Interruption tolerance fields)
    private boolean interrupted = false;
    private String interruptReason;
    private int chunksReceived = 0;
    private int totalChunks = -1;  // -1 表示未知

    // 用于 HOPE 学习的元数据
    // (Metadata for HOPE learning)
    private String userId;
    private boolean savedToHOPE = false;

    public StreamingSession(String sessionId, String question) {
        this.sessionId = sessionId;
        this.question = question;
    }

    /**
     * 添加文本块
     * (Append text chunk)
     */
    public void appendChunk(String chunk) {
        fullAnswer.append(chunk);
        chunksReceived++;
    }

    /**
     * 通知所有订阅者
     * (Notify all subscribers)
     */
    public void notifySubscribers(String chunk) {
        for (Consumer<String> subscriber : subscribers) {
            try {
                subscriber.accept(chunk);
            } catch (Exception e) {
                log.warn("通知订阅者失败 (Failed to notify subscriber): {}", e.getMessage());
            }
        }
    }

    /**
     * 添加订阅者
     * (Add subscriber)
     */
    public void addSubscriber(Consumer<String> subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * 标记为完成
     * (Mark as completed)
     */
    public void markComplete() {
        this.status = SessionStatus.COMPLETED;
        this.completeTime = LocalDateTime.now();
        log.info("✅ 流式会话完成 (Streaming session completed): sessionId={}, duration={}s",
            sessionId, getDurationSeconds());
    }

    /**
     * 标记为中断
     * (Mark as interrupted)
     */
    public void markInterrupted(String reason) {
        this.interrupted = true;
        this.interruptReason = reason;
        this.status = SessionStatus.INTERRUPTED;
        log.warn("⚠️ 流式会话中断 (Streaming session interrupted): sessionId={}, reason={}, progress={}/{}",
            sessionId, reason, chunksReceived, totalChunks);
    }

    /**
     * 标记为错误
     * (Mark as error)
     */
    public void markError(Exception error) {
        this.status = SessionStatus.ERROR;
        this.interruptReason = error.getMessage();
        log.error("❌ 流式会话错误 (Streaming session error): sessionId={}, error={}",
            sessionId, error.getMessage());
    }

    /**
     * 判断会话是否有效（用于 HOPE 学习）
     * (Check if session is valid for HOPE learning)
     */
    public boolean isValid() {
        // 1. 必须完成
        if (status != SessionStatus.COMPLETED) {
            return false;
        }

        // 2. 内容不能太短（至少50字）
        if (fullAnswer.length() < 50) {
            log.debug("会话无效：内容太短 (Session invalid: content too short) length={}",
                fullAnswer.length());
            return false;
        }

        // 3. 时长不能太短（至少2秒，避免错误）
        if (getDurationSeconds() < 2) {
            log.debug("会话无效：时长太短 (Session invalid: duration too short) duration={}s",
                getDurationSeconds());
            return false;
        }

        return true;
    }

    /**
     * 获取会话时长（秒）
     * (Get session duration in seconds)
     */
    public long getDurationSeconds() {
        LocalDateTime end = completeTime != null ? completeTime : LocalDateTime.now();
        return Duration.between(startTime, end).getSeconds();
    }

    /**
     * 获取完成进度（0-1）
     * (Get completion progress)
     */
    public double getProgress() {
        if (totalChunks <= 0) {
            return 0.0;
        }
        return (double) chunksReceived / totalChunks;
    }
}

