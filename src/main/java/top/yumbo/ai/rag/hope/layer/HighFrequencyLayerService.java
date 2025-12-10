package top.yumbo.ai.rag.hope.layer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.model.SessionContext;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 高频层服务 - 管理实时会话上下文
 * (High-Frequency Layer Service - Manages real-time session context)
 *
 * 特点：
 * - 存储当前会话的对话历史
 * - 管理临时定义和更正
 * - 提供上下文增强
 * - 自动过期清理
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class HighFrequencyLayerService {

    private final HOPEConfig config;

    // 会话上下文缓存（使用 Caffeine）
    private Cache<String, SessionContext> sessionCache;

    // 话题关键词提取的简单模式
    private static final String[] TOPIC_KEYWORDS = {
        "RAG", "检索", "索引", "向量", "LLM", "大模型", "AI", "人工智能",
        "Spring", "Java", "Python", "代码", "配置", "部署", "API",
        "文档", "知识库", "问答", "搜索", "查询"
    };

    @Autowired
    public HighFrequencyLayerService(HOPEConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        // 1. 检查HOPE是否启用 (Check if HOPE is enabled)
        if (!config.isEnabled()) {
            log.info(I18N.get("hope.high_frequency.disabled"));
            return;
        }

        // 2. 获取缓存配置参数 (Get cache configuration parameters)
        int maxSessions = config.getHighFrequency().getMaxSessions();
        int timeoutMinutes = config.getHighFrequency().getSessionTimeoutMinutes();

        // 3. 初始化 Caffeine 缓存 (Initialize Caffeine cache)
        sessionCache = Caffeine.newBuilder()
            .maximumSize(maxSessions)
            .expireAfterAccess(timeoutMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build();

        // 4. 记录初始化成功日志 (Log successful initialization)
        log.info(I18N.get("hope.high_frequency.init_success", maxSessions, timeoutMinutes));
    }

    /**
     * 查询高频层
     * (Query high-frequency layer)
     * 
     * @param sessionId 会话ID (Session ID)
     * @param question 用户问题 (User question)
     * @return 查询结果 (Query result)
     */
    public HighFreqQueryResult query(String sessionId, String question) {
        HighFreqQueryResult result = new HighFreqQueryResult();

        if (sessionId == null || sessionId.isEmpty()) {
            return result;
        }

        SessionContext context = sessionCache.getIfPresent(sessionId);
        if (context == null) {
            return result;
        }

        result.setFound(true);
        result.setSessionContext(context);

        // 1. 检查是否有相关的临时定义 (Check for relevant temporary definitions)
        List<String> relevantDefinitions = findRelevantDefinitions(context, question);
        if (!relevantDefinitions.isEmpty()) {
            result.setHasRelevantContext(true);
            result.getContexts().addAll(relevantDefinitions);
        }

        // 2. 构建对话上下文摘要 (Build conversation context summary)
        String contextSummary = context.buildContextSummary();
        if (!contextSummary.isEmpty()) {
            result.setHasRelevantContext(true);
            result.setConversationSummary(contextSummary);
        }

        // 3. 检测话题延续
        if (isTopicContinuation(context, question)) {
            result.setTopicContinuation(true);
            result.setCurrentTopic(context.getCurrentTopic());
        }

        return result;
    }

    /**
     * 获取或创建会话
     * (Get or create session)
     * 
     * @param sessionId 会话ID (Session ID)
     * @return 会话上下文 (Session context)
     */
    public SessionContext getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }

        final String finalSessionId = sessionId;
        return sessionCache.get(finalSessionId, id -> {
            SessionContext newContext = SessionContext.builder()
                .sessionId(id)
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .history(new ArrayList<>())
                .tempDefinitions(new ArrayList<>())
                .build();
            log.debug(I18N.get("hope.high_frequency.session_created", id));
            return newContext;
        });
    }

    /**
     * 更新会话上下文
     * (Update session context)
     * 
     * @param sessionId 会话ID (Session ID)
     * @param question 用户问题 (User question)
     * @param answer 回答内容 (Answer content)
     */
    public void updateContext(String sessionId, String question, String answer) {
        SessionContext context = getOrCreateSession(sessionId);

        // 1. 添加对话历史 (Add conversation history)
        context.addTurn("user", question);
        context.addTurn("assistant", answer);

        // 2. 检测并更新当前话题 (Detect and update current topic)
        String detectedTopic = detectTopic(question);
        if (detectedTopic != null) {
            context.setCurrentTopic(detectedTopic);
        }

        // 3. 限制历史记录数量 (Limit history record count)
        int maxHistory = config.getHighFrequency().getMaxHistoryPerSession();
        trimHistory(context, maxHistory);

        // 检测临时定义（简单实现：检测 "xxx 是 yyy" 模式）
        extractTempDefinitions(context, question, answer);

        sessionCache.put(sessionId, context);
        log.debug(I18N.get("hope.high_frequency.context_updated", sessionId, context.getTurnCount()));
    }

    /**
     * 添加临时定义
     * (Add temporary definition)
     * 
     * @param sessionId 会话ID (Session ID)
     * @param term 术语 (Term)
     * @param definition 定义 (Definition)
     */
    public void addTempDefinition(String sessionId, String term, String definition) {
        SessionContext context = getOrCreateSession(sessionId);
        context.addTempDefinition(term, definition);
        sessionCache.put(sessionId, context);
        log.debug(I18N.get("hope.high_frequency.definition_added", term, sessionId));
    }

    /**
     * 清除会话
     * (Clear session)
     * 
     * @param sessionId 会话ID (Session ID)
     */
    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessionCache.invalidate(sessionId);
            log.debug(I18N.get("hope.high_frequency.session_cleared", sessionId));
        }
    }

    /**
     * 查找与问题相关的临时定义
     */
    private List<String> findRelevantDefinitions(SessionContext context, String question) {
        List<String> relevant = new ArrayList<>();

        if (context.getTempDefinitions() == null) {
            return relevant;
        }

        String lowerQuestion = question.toLowerCase();
        for (SessionContext.TempDefinition def : context.getTempDefinitions()) {
            if (lowerQuestion.contains(def.getTerm().toLowerCase())) {
                relevant.add(def.getTerm() + ": " + def.getDefinition());
            }
        }

        return relevant;
    }

    /**
     * 检测是否是话题延续
     */
    private boolean isTopicContinuation(SessionContext context, String question) {
        if (context.getCurrentTopic() == null) {
            return false;
        }

        // 简单检测：问题中是否包含当前话题关键词
        String topic = context.getCurrentTopic().toLowerCase();
        String lowerQuestion = question.toLowerCase();

        // 检测代词引用（这、那、它、这个、那个等）
        String[] pronouns = {"这", "那", "它", "这个", "那个", "上面", "刚才", "之前"};
        for (String pronoun : pronouns) {
            if (lowerQuestion.contains(pronoun)) {
                return true;
            }
        }

        // 检测话题关键词
        String[] topicWords = topic.split("\\s+");
        for (String word : topicWords) {
            if (word.length() >= 2 && lowerQuestion.contains(word)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检测话题
     */
    private String detectTopic(String question) {
        String lowerQuestion = question.toLowerCase();

        for (String keyword : TOPIC_KEYWORDS) {
            if (lowerQuestion.contains(keyword.toLowerCase())) {
                return keyword;
            }
        }

        // 尝试提取问题中的核心词
        // 简单实现：取问题中最长的词
        String[] words = question.split("[\\s,，。？?!！]+");
        String longest = "";
        for (String word : words) {
            if (word.length() > longest.length() && word.length() >= 2) {
                longest = word;
            }
        }

        return longest.isEmpty() ? null : longest;
    }

    /**
     * 限制历史记录数量
     */
    private void trimHistory(SessionContext context, int maxHistory) {
        if (context.getHistory() != null && context.getHistory().size() > maxHistory * 2) {
            // 保留最后 maxHistory 轮对话（每轮2条：user + assistant）
            int start = context.getHistory().size() - maxHistory * 2;
            context.setHistory(new ArrayList<>(context.getHistory().subList(start, context.getHistory().size())));
        }
    }

    /**
     * 从对话中提取临时定义
     */
    private void extractTempDefinitions(SessionContext context, String question, String answer) {
        // 检测用户的定义式陈述："xxx 是指 yyy" 或 "xxx 指的是 yyy"
        String[] defPatterns = {
            "(.+?)\\s*是指\\s*(.+)",
            "(.+?)\\s*指的是\\s*(.+)",
            "(.+?)\\s*定义为\\s*(.+)",
            "(.+?)\\s*就是\\s*(.+)"
        };

        for (String pattern : defPatterns) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(question);
            if (matcher.find()) {
                String term = matcher.group(1).trim();
                String definition = matcher.group(2).trim();
                if (term.length() >= 2 && term.length() <= 20 && definition.length() >= 2) {
                    context.addTempDefinition(term, definition);
                    log.debug(I18N.get("hope.high_frequency.auto_definition", term));
                }
                break;
            }
        }
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取统计信息
     * (Get statistics information)
     * 
     * @return 统计信息 (Statistics information)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        if (sessionCache != null) {
            stats.put("activeSessions", sessionCache.estimatedSize());
            stats.put("hitRate", sessionCache.stats().hitRate());
            stats.put("evictionCount", sessionCache.stats().evictionCount());
        } else {
            stats.put("activeSessions", 0);
            stats.put("status", "not initialized");
        }
        return stats;
    }

    /**
     * 高频层查询结果
     * (High-frequency layer query result)
     */
    @Data
    public static class HighFreqQueryResult {
        /**
         * 是否找到匹配
         * (Whether found a match)
         */
        private boolean found;
        
        /**
         * 是否有相关上下文
         * (Whether has relevant context)
         */
        private boolean hasRelevantContext;
        
        /**
         * 会话上下文
         * (Session context)
         */
        private SessionContext sessionContext;
        
        /**
         * 上下文列表
         * (Context list)
         */
        private List<String> contexts = new ArrayList<>();
        
        /**
         * 对话摘要
         * (Conversation summary)
         */
        private String conversationSummary;
        
        /**
         * 是否是话题延续
         * (Whether is topic continuation)
         */
        private boolean topicContinuation;
        
        /**
         * 当前话题
         * (Current topic)
         */
        private String currentTopic;
    }
}

