package top.yumbo.ai.rag.hope.integration;

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
 * @since 2025-12-07
 */
@Slf4j
public class HOPEEnhancedLLMClient implements LLMClient {

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
        this.delegate = delegate;
        this.hopeManager = hopeManager;
        this.hopeMonitor = hopeMonitor;
        this.config = config != null ? config : new HOPELLMConfig();
    }

    /**
     * 设置当前会话ID（可选）
     */
    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    /**
     * 清除当前会话ID
     */
    public static void clearSessionId() {
        currentSessionId.remove();
    }

    /**
     * 获取最后的查询信息（用于反馈学习）
     */
    public static LastQuery getLastQuery() {
        return lastQuery.get();
    }

    /**
     * 手动触发学习（当用户给出反馈时调用）
     */
    public void learnFromFeedback(String question, String answer, int rating) {
        if (hopeManager != null && hopeManager.isEnabled() && rating >= config.getMinRatingForLearning()) {
            String sessionId = currentSessionId.get();
            hopeManager.learn(question, answer, rating, sessionId);
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

                log.debug("HOPE query: needsLLM={}, source={}, confidence={}",
                    hopeResult.isNeedsLLM(), hopeResult.getSourceLayer(), hopeResult.getConfidence());

                // 直接回答策略
                if (strategy == ResponseStrategy.DIRECT_ANSWER && hopeResult.canDirectAnswer()) {
                    long elapsed = System.currentTimeMillis() - startTime;

                    if (hopeMonitor != null) {
                        hopeMonitor.recordQuery(strategy, hopeResult, elapsed);
                    }

                    // 记录最后查询
                    lastQuery.set(new LastQuery(prompt, hopeResult.getAnswer(),
                        hopeResult.getSourceLayer(), true, elapsed));

                    log.info("HOPE direct answer from {} in {}ms",
                        hopeResult.getSourceLayer(), elapsed);
                    return hopeResult.getAnswer();
                }

                // 模板增强策略
                if (strategy == ResponseStrategy.TEMPLATE_ANSWER && hopeResult.hasSkillTemplate()) {
                    SkillTemplate template = hopeResult.getSkillTemplate();
                    String optimizedPrompt = hopeManager.buildOptimizedPrompt(prompt, template, null);
                    if (optimizedPrompt != null) {
                        prompt = optimizedPrompt;
                        log.debug("Using skill template: {}", template.getName());
                    }
                }

                // 参考增强：将相似问答作为上下文
                if (hopeResult.hasSimilarReference() && config.isReferenceEnhanceEnabled()) {
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append("参考以下相似问答：\n");
                    for (HOPEQueryResult.SimilarQA similar : hopeResult.getSimilarQAs()) {
                        contextBuilder.append("Q: ").append(similar.getQuestion()).append("\n");
                        contextBuilder.append("A: ").append(similar.getAnswer()).append("\n\n");
                    }
                    contextBuilder.append("---\n现在请回答：\n").append(prompt);
                    prompt = contextBuilder.toString();
                }

            } catch (Exception e) {
                log.warn("HOPE query failed, fallback to direct LLM call: {}", e.getMessage());
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

        // 5. 记录最后查询
        lastQuery.set(new LastQuery(prompt, result, null, false, elapsed));

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
     * 获取底层 LLM 客户端
     */
    public LLMClient getDelegate() {
        return delegate;
    }

    /**
     * 最后查询信息（用于反馈学习）
     */
    public static class LastQuery {
        private final String question;
        private final String answer;
        private final String hopeSource;
        private final boolean directAnswer;
        private final long responseTimeMs;

        public LastQuery(String question, String answer, String hopeSource,
                         boolean directAnswer, long responseTimeMs) {
            this.question = question;
            this.answer = answer;
            this.hopeSource = hopeSource;
            this.directAnswer = directAnswer;
            this.responseTimeMs = responseTimeMs;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getHopeSource() { return hopeSource; }
        public boolean isDirectAnswer() { return directAnswer; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }

    /**
     * HOPE LLM 配置
     */
    public static class HOPELLMConfig {
        private boolean hopeQueryEnabled = true;
        private boolean autoLearnEnabled = true;
        private boolean referenceEnhanceEnabled = true;
        private int autoLearnRating = 3;        // 自动学习默认评分
        private int minRatingForLearning = 4;   // 手动反馈学习最小评分

        public boolean isHopeQueryEnabled() { return hopeQueryEnabled; }
        public void setHopeQueryEnabled(boolean hopeQueryEnabled) { this.hopeQueryEnabled = hopeQueryEnabled; }
        public boolean isAutoLearnEnabled() { return autoLearnEnabled; }
        public void setAutoLearnEnabled(boolean autoLearnEnabled) { this.autoLearnEnabled = autoLearnEnabled; }
        public boolean isReferenceEnhanceEnabled() { return referenceEnhanceEnabled; }
        public void setReferenceEnhanceEnabled(boolean referenceEnhanceEnabled) { this.referenceEnhanceEnabled = referenceEnhanceEnabled; }
        public int getAutoLearnRating() { return autoLearnRating; }
        public void setAutoLearnRating(int autoLearnRating) { this.autoLearnRating = autoLearnRating; }
        public int getMinRatingForLearning() { return minRatingForLearning; }
        public void setMinRatingForLearning(int minRatingForLearning) { this.minRatingForLearning = minRatingForLearning; }
    }
}

