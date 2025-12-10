package top.yumbo.ai.rag.hope.integration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.ResponseStrategy;
import top.yumbo.ai.rag.hope.model.HOPEQueryResult;
import top.yumbo.ai.rag.hope.model.SkillTemplate;
import top.yumbo.ai.rag.hope.monitor.HOPEMonitorService;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.List;

/**
 * HOPE 增强的 LLM 客户端装饰器
 * (HOPE Enhanced LLM Client Decorator)
 *
 * 在每次 LLM 调用前查询 HOPE 三层，调用后自动学习
 * (Query HOPE three layers before each LLM call, auto-learn after call)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
public class HOPEEnhancedLLMClient implements LLMClient {

    /**
     * -- GETTER --
     *  获取底层 LLM 客户端
     */
    @Getter
    private final LLMClient delegate;
    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMonitorService hopeMonitor;
    private final HOPELLMConfig config;

    // 线程本地变量，存储当前会话ID
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    // 线程本地变量，存储最后的查询结果（用于反馈学习）
    private static final ThreadLocal<LastQuery> lastQuery = new ThreadLocal<>();

    public HOPEEnhancedLLMClient(LLMClient delegate,
                                  HOPEKnowledgeManager hopeManager,
                                  HOPEMonitorService hopeMonitor,
                                  HOPELLMConfig config) {
        // 1. 设置底层LLM客户端 (Set underlying LLM client)
        this.delegate = delegate;
        // 2. 设置HOPE知识管理器 (Set HOPE knowledge manager)
        this.hopeManager = hopeManager;
        // 3. 设置监控服务 (Set monitoring service)
        this.hopeMonitor = hopeMonitor;
        // 4. 设置配置，使用默认配置如果为空 (Set configuration, use default if null)
        this.config = config != null ? config : new HOPELLMConfig();
    }

    /**
     * 设置当前会话ID（可选）
     * (Set current session ID (optional))
     * 
     * @param sessionId 会话ID (Session ID)
     */
    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    /**
     * 清除当前会话ID
     * (Clear current session ID)
     */
    public static void clearSessionId() {
        currentSessionId.remove();
    }

    /**
     * 获取最后的查询信息（用于反馈学习）
     * (Get last query information for feedback learning)
     * 
     * @return 最后的查询信息 (Last query information)
     */
    public static LastQuery getLastQuery() {
        return lastQuery.get();
    }

    /**
     * 手动触发学习（当用户给出反馈时调用）
     * (Manually trigger learning (called when user gives feedback))
     * 
     * @param question 用户问题 (User question)
     * @param answer 回答内容 (Answer content)
     * @param rating 评分 (Rating)
     */
    public void learnFromFeedback(String question, String answer, int rating) {
        // 1. 检查HOPE管理器是否可用 (Check if HOPE manager is available)
        if (hopeManager != null && hopeManager.isEnabled() && rating >= config.getMinRatingForLearning()) {
            // 2. 获取当前会话ID (Get current session ID)
            String sessionId = currentSessionId.get();
            // 3. 记录学习信息 (Record learning information)
            hopeManager.learn(question, answer, rating, sessionId);
            // 4. 记录调试日志 (Log debug information)
            log.debug(I18N.get("hope.learn.recorded", rating));
        }
    }

    @Override
    public String generate(String prompt) {
        return generateWithHOPE(prompt, null, null, null);
    }

    @Override
    public String generate(String prompt, String systemPrompt) {
        return generateWithHOPE(prompt, systemPrompt, null, null);
    }

    @Override
    public String generateWithImage(String prompt, String imageUrl, String systemPrompt) {
        // 图片请求不使用 HOPE 缓存，但仍然记录学习
        long startTime = System.currentTimeMillis();
        String result = delegate.generateWithImage(prompt, imageUrl, systemPrompt);
        long elapsed = System.currentTimeMillis() - startTime;

        // 记录监控
        if (hopeMonitor != null) {
            hopeMonitor.recordQuery(ResponseStrategy.FULL_RAG, null, elapsed);
        }

        // 自动学习（图片处理结果）
        if (config.isAutoLearnEnabled() && hopeManager != null && hopeManager.isEnabled()) {
            // 图片请求的学习评分默认为 3（中等）
            String sessionId = currentSessionId.get();
            hopeManager.learn(prompt, result, 3, sessionId);
        }

        return result;
    }

    @Override
    public String generateWithImages(String prompt, List<String> imageUrls, String systemPrompt) {
        long startTime = System.currentTimeMillis();
        String result = delegate.generateWithImages(prompt, imageUrls, systemPrompt);
        long elapsed = System.currentTimeMillis() - startTime;

        if (hopeMonitor != null) {
            hopeMonitor.recordQuery(ResponseStrategy.FULL_RAG, null, elapsed);
        }

        return result;
    }

    /**
     * 核心方法：带 HOPE 增强的生成
     */
    private String generateWithHOPE(String prompt, String systemPrompt,
                                     String imageUrl, List<String> imageUrls) {
        long startTime = System.currentTimeMillis();
        String sessionId = currentSessionId.get();

        // 1. HOPE 智能查询
        if (hopeManager != null && hopeManager.isEnabled() && config.isHopeQueryEnabled()) {
            try {
                HOPEQueryResult hopeResult = hopeManager.smartQuery(prompt, sessionId);
                ResponseStrategy strategy = hopeManager.getStrategy(prompt, hopeResult);

                log.debug(I18N.get("hope.query.debug_info"),
                    hopeResult.isNeedsLLM(), hopeResult.getSourceLayer(), hopeResult.getConfidence());

                // 直接回答策略
                if (strategy == ResponseStrategy.DIRECT_ANSWER && hopeResult.canDirectAnswer()) {
                    long elapsed = System.currentTimeMillis() - startTime;

                    if (hopeMonitor != null) {
                        hopeMonitor.recordQuery(strategy, hopeResult, elapsed);
                    }

                    // 记录最后查询 (Record last query)
                    lastQuery.set(new LastQuery(prompt, hopeResult.getAnswer(),
                        hopeResult.getSourceLayer(), true, elapsed,
                        hopeResult.getConfidence(), strategy.name()));

                    log.info(I18N.get("hope.direct_answer.success"),
                        hopeResult.getSourceLayer(), elapsed);
                    return hopeResult.getAnswer();
                }

                // 模板增强策略
                if (strategy == ResponseStrategy.TEMPLATE_ANSWER && hopeResult.hasSkillTemplate()) {
                    SkillTemplate template = hopeResult.getSkillTemplate();
                    String optimizedPrompt = hopeManager.buildOptimizedPrompt(prompt, template, null);
                    if (optimizedPrompt != null) {
                        prompt = optimizedPrompt;
                        log.debug(I18N.get("hope.template.used"), template.getName());
                    }
                }

                // 参考增强：将相似问答作为上下文
                if (hopeResult.hasSimilarReference() && config.isReferenceEnhanceEnabled()) {
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append(I18N.get("hope.reference_used")).append(":\n");
                    for (HOPEQueryResult.SimilarQA similar : hopeResult.getSimilarQAs()) {
                        contextBuilder.append("Q: ").append(similar.getQuestion()).append("\n");
                        contextBuilder.append("A: ").append(similar.getAnswer()).append("\n\n");
                    }
                    contextBuilder.append("---\n").append(I18N.get("hope.strategy.answer_now")).append("\n").append(prompt);
                    prompt = contextBuilder.toString();
                }

            } catch (Exception e) {
                log.warn(I18N.get("hope.query.failed"), e.getMessage());
            }
        }

        // 2. 调用底层 LLM
        String result;
        if (systemPrompt != null) {
            result = delegate.generate(prompt, systemPrompt);
        } else {
            result = delegate.generate(prompt);
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // 3. 记录监控指标
        if (hopeMonitor != null) {
            hopeMonitor.recordQuery(ResponseStrategy.FULL_RAG, null, elapsed);
        }

        // 4. 自动学习
        if (config.isAutoLearnEnabled() && hopeManager != null && hopeManager.isEnabled()) {
            // 自动学习默认评分
            int autoLearnRating = config.getAutoLearnRating();
            hopeManager.learn(prompt, result, autoLearnRating, sessionId);
        }

        // 5. 记录最后查询 (Record last query)
        lastQuery.set(new LastQuery(prompt, result, null, false, elapsed,
                0.0, "FULL_RAG"));

        return result;
    }

    @Override
    public boolean supportsImageInput() {
        return delegate.supportsImageInput();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    /**
     * 最后查询信息（用于反馈学习）(Last query information for feedback learning)
     */
    public static class LastQuery {
        private final String question;
        private final String answer;
        private final String hopeSource;
        private final boolean directAnswer;
        private final long responseTimeMs;
        private final double confidence;        // HOPE 置信度 (HOPE confidence)
        private final String strategyUsed;      // 使用的策略 (Strategy used)

        public LastQuery(String question, String answer, String hopeSource,
                         boolean directAnswer, long responseTimeMs) {
            this(question, answer, hopeSource, directAnswer, responseTimeMs, 0.0, null);
        }

        public LastQuery(String question, String answer, String hopeSource,
                         boolean directAnswer, long responseTimeMs,
                         double confidence, String strategyUsed) {
            this.question = question;
            this.answer = answer;
            this.hopeSource = hopeSource;
            this.directAnswer = directAnswer;
            this.responseTimeMs = responseTimeMs;
            this.confidence = confidence;
            this.strategyUsed = strategyUsed != null ? strategyUsed :
                (directAnswer ? "DIRECT_ANSWER" : "FULL_RAG");
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getHopeSource() { return hopeSource; }
        public boolean isDirectAnswer() { return directAnswer; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public long getResponseTime() { return responseTimeMs; }  // 别名 (Alias)
        public double getConfidence() { return confidence; }
        public String getStrategyUsed() { return strategyUsed; }
    }

    /**
     * HOPE LLM 配置
     */
    @Data
    public static class HOPELLMConfig {
        private boolean hopeQueryEnabled = true;
        private boolean autoLearnEnabled = true;
        private boolean referenceEnhanceEnabled = true;
        private int autoLearnRating = 3;        // 自动学习默认评分
        private int minRatingForLearning = 4;   // 手动反馈学习最小评分

    }
}

