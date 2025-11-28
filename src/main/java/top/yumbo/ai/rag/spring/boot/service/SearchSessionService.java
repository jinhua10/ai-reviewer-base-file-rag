package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.model.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 搜索会话管理服务 - 支持分页引用文档
 *
 * @author AI Reviewer Team
 * @since 2025-11-29
 */
@Slf4j
@Service
public class SearchSessionService {

    // 会话存储（sessionId -> SearchSession）
    private final Map<String, SearchSession> sessions = new ConcurrentHashMap<>();

    // 会话超时时间（30分钟）
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    /**
     * 创建新的搜索会话
     *
     * @param question 用户问题
     * @param allDocuments 所有检索到的文档（已排序）
     * @param documentsPerQuery 每次引用的文档数
     * @return 会话ID
     */
    public String createSession(String question, List<Document> allDocuments, int documentsPerQuery) {
        String sessionId = UUID.randomUUID().toString();

        SearchSession session = new SearchSession();
        session.setSessionId(sessionId);
        session.setQuestion(question);
        session.setAllDocuments(new ArrayList<>(allDocuments));
        session.setDocumentsPerQuery(documentsPerQuery);
        session.setCurrentOffset(0);
        session.setCreateTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());

        sessions.put(sessionId, session);

        log.info("📝 创建搜索会话: sessionId={}, 总文档数={}, 每次引用={}",
            sessionId, allDocuments.size(), documentsPerQuery);

        // 清理过期会话
        cleanExpiredSessions();

        return sessionId;
    }

    /**
     * 获取当前批次的文档
     *
     * @param sessionId 会话ID
     * @return 当前批次的文档和分页信息
     */
    public SessionDocuments getCurrentDocuments(String sessionId) {
        SearchSession session = getSession(sessionId);
        return getDocumentsAtOffset(session, session.getCurrentOffset());
    }

    /**
     * 获取下一批文档
     *
     * @param sessionId 会话ID
     * @return 下一批文档和分页信息
     */
    public SessionDocuments getNextDocuments(String sessionId) {
        SearchSession session = getSession(sessionId);

        int nextOffset = session.getCurrentOffset() + session.getDocumentsPerQuery();
        if (nextOffset >= session.getAllDocuments().size()) {
            log.warn("⚠️ 已经是最后一批文档了: sessionId={}", sessionId);
            return getDocumentsAtOffset(session, session.getCurrentOffset());
        }

        session.setCurrentOffset(nextOffset);
        session.setLastAccessTime(LocalDateTime.now());

        log.info("➡️ 切换到下一批文档: sessionId={}, offset={}", sessionId, nextOffset);

        return getDocumentsAtOffset(session, nextOffset);
    }

    /**
     * 获取上一批文档
     *
     * @param sessionId 会话ID
     * @return 上一批文档和分页信息
     */
    public SessionDocuments getPreviousDocuments(String sessionId) {
        SearchSession session = getSession(sessionId);

        int prevOffset = Math.max(0, session.getCurrentOffset() - session.getDocumentsPerQuery());
        if (prevOffset == session.getCurrentOffset()) {
            log.warn("⚠️ 已经是第一批文档了: sessionId={}", sessionId);
            return getDocumentsAtOffset(session, session.getCurrentOffset());
        }

        session.setCurrentOffset(prevOffset);
        session.setLastAccessTime(LocalDateTime.now());

        log.info("⬅️ 切换到上一批文档: sessionId={}, offset={}", sessionId, prevOffset);

        return getDocumentsAtOffset(session, prevOffset);
    }

    /**
     * 跳转到指定页
     *
     * @param sessionId 会话ID
     * @param page 页码（从1开始）
     * @return 指定页的文档和分页信息
     */
    public SessionDocuments getDocumentsByPage(String sessionId, int page) {
        SearchSession session = getSession(sessionId);

        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }

        int offset = (page - 1) * session.getDocumentsPerQuery();
        if (offset >= session.getAllDocuments().size()) {
            throw new IllegalArgumentException(String.format(
                "页码超出范围: page=%d, 总页数=%d",
                page, getTotalPages(session)));
        }

        session.setCurrentOffset(offset);
        session.setLastAccessTime(LocalDateTime.now());

        log.info("📄 跳转到第{}页: sessionId={}, offset={}", page, sessionId, offset);

        return getDocumentsAtOffset(session, offset);
    }

    /**
     * 获取会话信息
     */
    public SessionInfo getSessionInfo(String sessionId) {
        SearchSession session = getSession(sessionId);

        SessionInfo info = new SessionInfo();
        info.setSessionId(sessionId);
        info.setQuestion(session.getQuestion());
        info.setTotalDocuments(session.getAllDocuments().size());
        info.setDocumentsPerQuery(session.getDocumentsPerQuery());
        info.setCurrentPage(getCurrentPage(session));
        info.setTotalPages(getTotalPages(session));
        info.setHasNext(hasNextPage(session));
        info.setHasPrevious(hasPreviousPage(session));
        info.setRemainingDocuments(getRemainingDocuments(session));
        info.setCreateTime(session.getCreateTime());
        info.setLastAccessTime(session.getLastAccessTime());

        return info;
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("🗑️ 删除搜索会话: sessionId={}", sessionId);
    }

    /**
     * 清理过期会话
     */
    private void cleanExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredSessions = new ArrayList<>();

        sessions.forEach((id, session) -> {
            if (session.getLastAccessTime().plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(now)) {
                expiredSessions.add(id);
            }
        });

        expiredSessions.forEach(id -> {
            sessions.remove(id);
            log.info("🗑️ 清理过期会话: sessionId={}", id);
        });

        if (!expiredSessions.isEmpty()) {
            log.info("🧹 清理了 {} 个过期会话", expiredSessions.size());
        }
    }

    // ============ 私有辅助方法 ============

    private SearchSession getSession(String sessionId) {
        SearchSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在或已过期: " + sessionId);
        }
        return session;
    }

    private SessionDocuments getDocumentsAtOffset(SearchSession session, int offset) {
        List<Document> allDocs = session.getAllDocuments();
        int docsPerQuery = session.getDocumentsPerQuery();

        int endIndex = Math.min(offset + docsPerQuery, allDocs.size());
        List<Document> currentDocs = allDocs.subList(offset, endIndex);

        SessionDocuments result = new SessionDocuments();
        result.setSessionId(session.getSessionId());
        result.setDocuments(currentDocs);
        result.setCurrentPage(getCurrentPage(session));
        result.setTotalPages(getTotalPages(session));
        result.setTotalDocuments(allDocs.size());
        result.setCurrentDocumentCount(currentDocs.size());
        result.setHasNext(hasNextPage(session));
        result.setHasPrevious(hasPreviousPage(session));
        result.setRemainingDocuments(getRemainingDocuments(session));

        return result;
    }

    private int getCurrentPage(SearchSession session) {
        return session.getCurrentOffset() / session.getDocumentsPerQuery() + 1;
    }

    private int getTotalPages(SearchSession session) {
        int total = session.getAllDocuments().size();
        int perPage = session.getDocumentsPerQuery();
        return (total + perPage - 1) / perPage;
    }

    private boolean hasNextPage(SearchSession session) {
        return session.getCurrentOffset() + session.getDocumentsPerQuery()
            < session.getAllDocuments().size();
    }

    private boolean hasPreviousPage(SearchSession session) {
        return session.getCurrentOffset() > 0;
    }

    private int getRemainingDocuments(SearchSession session) {
        return session.getAllDocuments().size()
            - session.getCurrentOffset()
            - session.getDocumentsPerQuery();
    }

    // ============ 内部类 ============

    /**
     * 搜索会话
     */
    @Data
    private static class SearchSession {
        private String sessionId;
        private String question;
        private List<Document> allDocuments;
        private int documentsPerQuery;
        private int currentOffset;
        private LocalDateTime createTime;
        private LocalDateTime lastAccessTime;
    }

    /**
     * 会话文档（带分页信息）
     */
    @Data
    public static class SessionDocuments {
        private String sessionId;
        private List<Document> documents;
        private int currentPage;
        private int totalPages;
        private int totalDocuments;
        private int currentDocumentCount;
        private boolean hasNext;
        private boolean hasPrevious;
        private int remainingDocuments;
    }

    /**
     * 会话信息
     */
    @Data
    public static class SessionInfo {
        private String sessionId;
        private String question;
        private int totalDocuments;
        private int documentsPerQuery;
        private int currentPage;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private int remainingDocuments;
        private LocalDateTime createTime;
        private LocalDateTime lastAccessTime;
    }
}

