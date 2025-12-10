package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.model.RecentQA;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.streaming.model.SessionStatus;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式会话监控器
 * (Streaming Session Monitor)
 *
 * 处理中断、超时等异常情况
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Slf4j
@Service
public class StreamingSessionMonitor {

    private final HOPEKnowledgeManager hopeManager;

    // 活跃会话
    // (Active sessions)
    private final Map<String, StreamingSession> activeSessions = new ConcurrentHashMap<>();

    // 草稿存储（用于分析）
    // (Draft storage for analysis)
    private final List<IncompleteDraft> drafts = new ArrayList<>();

    @Autowired
    public StreamingSessionMonitor(@Autowired(required = false) HOPEKnowledgeManager hopeManager) {
        this.hopeManager = hopeManager;
    }

    /**
     * 注册会话
     * (Register session)
     */
    public void registerSession(StreamingSession session) {
        activeSessions.put(session.getSessionId(), session);
        log.debug(I18N.get("log.streaming.session_registered", session.getSessionId()));
    }

    /**
     * 获取会话
     * (Get session)
     */
    public StreamingSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * 用户主动中断
     * (User interrupts actively)
     */
    public void onUserInterrupt(String sessionId) {
        onClientDisconnect(sessionId, "用户中断 (User interrupt)");
    }

    /**
     * 客户端断开连接时调用
     * (Called when client disconnects)
     */
    public void onClientDisconnect(String sessionId, String reason) {
        StreamingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.debug(I18N.get("log.streaming.session_not_found", sessionId));
            return;
        }

        session.markInterrupted(reason);

        log.warn(I18N.get("log.streaming.client_disconnected", sessionId, reason, session.getProgress() * 100));

        // 处理中断会话
        // (Handle interrupted session)
        handleInterruptedSession(session);
    }

    /**
     * 处理中断会话
     * (Handle interrupted session)
     */
    private void handleInterruptedSession(StreamingSession session) {
        // 规则1：如果已经接收 >80% 内容，保存为草稿
        // (Rule 1: If >80% content received, save as draft)
        if (session.getTotalChunks() > 0 &&
            session.getChunksReceived() >= session.getTotalChunks() * 0.8) {

            saveDraft(session, "80%以上内容已生成 (>80% content generated)");
            log.info(I18N.get("log.streaming.draft_saved", session.getSessionId(), session.getProgress() * 100));
        }

        // 规则2：如果已生成 >200 字，且用户停留 >10s，可能是有用的
        // (Rule 2: If >200 chars + >10s dwell, might be useful)
        else if (session.getFullAnswer().length() > 200 &&
                 session.getDurationSeconds() > 10) {

            saveDraft(session, "内容较长且停留时间充足 (Long content + sufficient dwell time)");
            log.info(I18N.get("log.streaming.partial_result_saved", session.getSessionId(),
                session.getFullAnswer().length(),
                session.getDurationSeconds()));
        }

        // 规则3：其他情况，丢弃
        // (Rule 3: Discard in other cases)
        else {
            log.info(I18N.get("log.streaming.discard_incomplete_session", session.getSessionId()));
        }

        activeSessions.remove(session.getSessionId());
    }

    /**
     * 保存草稿（不加入 HOPE，但保留用于分析）
     * (Save draft - not added to HOPE, but kept for analysis)
     */
    private void saveDraft(StreamingSession session, String reason) {
        IncompleteDraft draft = IncompleteDraft.builder()
            .sessionId(session.getSessionId())
            .question(session.getQuestion())
            .partialAnswer(session.getFullAnswer().toString())
            .chunksReceived(session.getChunksReceived())
            .totalChunks(session.getTotalChunks())
            .interruptReason(session.getInterruptReason())
            .saveReason(reason)
            .createdAt(session.getStartTime())
            .build();

        drafts.add(draft);

        // 限制草稿数量（只保留最近100个）
        // (Limit draft count, keep only latest 100)
        if (drafts.size() > 100) {
            drafts.remove(0);
        }
    }

    /**
     * 会话完成时调用
     * (Called when session completes)
     */
    public void onSessionComplete(String sessionId) {
        StreamingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.debug(I18N.get("log.streaming.session_not_found", sessionId));
            return;
        }

        if (session.getStatus() != SessionStatus.COMPLETED) {
            log.debug(I18N.get("log.streaming.session_not_completed", sessionId, session.getStatus()));
            activeSessions.remove(sessionId);
            return;
        }

        // 判断是否加入 HOPE
        // (Check if should add to HOPE)
        if (session.isValid() && !session.isSavedToHOPE()) {
            saveToHOPE(session);
        } else {
            log.debug(I18N.get("log.streaming.session_invalid_or_saved", sessionId));
        }

        activeSessions.remove(sessionId);
    }

    /**
     * 异步保存到 HOPE 中频层
     * (Asynchronously save to HOPE ordinary layer)
     */
    private void saveToHOPE(StreamingSession session) {
        if (hopeManager == null) {
            log.debug(I18N.get("log.streaming.hope_manager_disabled"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                RecentQA qa = RecentQA.builder()
                    .question(session.getQuestion())
                    .answer(session.getFullAnswer().toString())
                    .sessionId(session.getSessionId())
                    .createdAt(session.getStartTime())
                    .completedAt(session.getCompleteTime())
                    .responseTimeSeconds(session.getDurationSeconds())
                    // 初始评分0（等待用户反馈）
                    // (Initial rating 0, waiting for user feedback)
                    .rating(0)
                    .totalRating(0)
                    .ratingCount(0)
                    .accessCount(1)
                    .build();

                hopeManager.getOrdinaryLayer().save(qa);
                session.setSavedToHOPE(true);

                log.info(I18N.get("log.streaming.session_saved_to_hope", session.getSessionId()));

            } catch (Exception e) {
                log.error(I18N.get("log.streaming.save_to_hope_failed", session.getSessionId(), e.getMessage()));
            }
        });
    }

    /**
     * 定时清理超时会话（每5分钟）
     * (Periodically clean up timeout sessions, every 5 minutes)
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupTimeoutSessions() {
        long now = System.currentTimeMillis();
        List<String> timeoutSessionIds = new ArrayList<>();

        for (Map.Entry<String, StreamingSession> entry : activeSessions.entrySet()) {
            StreamingSession session = entry.getValue();

            // 超时阈值：5分钟
            // (Timeout threshold: 5 minutes)
            if (session.getDurationSeconds() > 300) {
                timeoutSessionIds.add(entry.getKey());
            }
        }

        if (!timeoutSessionIds.isEmpty()) {
            log.warn(I18N.get("log.streaming.cleaning_timeout_sessions", timeoutSessionIds.size()));

            for (String sessionId : timeoutSessionIds) {
                onClientDisconnect(sessionId, "超时 (Timeout)");
            }
        }
    }

    /**
     * 获取草稿列表（用于分析）
     * (Get draft list for analysis)
     */
    public List<IncompleteDraft> getDrafts() {
        return new ArrayList<>(drafts);
    }

    /**
     * 获取活跃会话数
     * (Get active session count)
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}

/**
 * 不完整草稿
 * (Incomplete draft)
 */
@Data
@Builder
class IncompleteDraft {
    private String sessionId;
    private String question;
    private String partialAnswer;
    private int chunksReceived;
    private int totalChunks;
    private String interruptReason;
    private String saveReason;
    private LocalDateTime createdAt;
}

