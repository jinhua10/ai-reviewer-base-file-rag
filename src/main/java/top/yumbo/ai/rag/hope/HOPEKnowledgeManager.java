package top.yumbo.ai.rag.hope;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.layer.HighFrequencyLayerService;
import top.yumbo.ai.rag.hope.layer.OrdinaryLayerService;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.HOPEQueryResult;
import top.yumbo.ai.rag.hope.model.SkillTemplate;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HOPE 知识管理器 - 统一管理三层知识的查询和学习
 * (HOPE Knowledge Manager - Unified management of three-layer knowledge query and learning)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class HOPEKnowledgeManager {

    private final HOPEConfig config;
    /**
     * -- GETTER --
     *  获取低频层服务
     *  (Get permanent layer service)
     */
    @Getter
    private final PermanentLayerService permanentLayer;
    /**
     * -- GETTER --
     *  获取中频层服务
     *  (Get ordinary layer service)
     */
    @Getter
    private final OrdinaryLayerService ordinaryLayer;
    /**
     * -- GETTER --
     *  获取高频层服务
     *  (Get high frequency layer service)
     */
    @Getter
    private final HighFrequencyLayerService highFreqLayer;
    private final QuestionClassifier questionClassifier;
    private final ResponseStrategyDecider strategyDecider;

    @Autowired
    public HOPEKnowledgeManager(HOPEConfig config,
                                PermanentLayerService permanentLayer,
                                OrdinaryLayerService ordinaryLayer,
                                HighFrequencyLayerService highFreqLayer,
                                QuestionClassifier questionClassifier,
                                ResponseStrategyDecider strategyDecider) {
        this.config = config;
        this.permanentLayer = permanentLayer;
        this.ordinaryLayer = ordinaryLayer;
        this.highFreqLayer = highFreqLayer;
        this.questionClassifier = questionClassifier;
        this.strategyDecider = strategyDecider;
    }

    /**
     * 智能查询 - 按层级依次查询
     * (Smart query - Query layers in order)
     *
     * @param question 用户问题
     * @param sessionId 会话ID（用于高频层）
     * @return 查询结果
     */
    public HOPEQueryResult smartQuery(String question, String sessionId) {
        if (!config.isEnabled()) {
            return HOPEQueryResult.builder()
                .needsLLM(true)
                .sourceLayer("disabled")
                .build();
        }

        long startTime = System.currentTimeMillis();
        HOPEQueryResult.HOPEQueryResultBuilder resultBuilder = HOPEQueryResult.builder();

        try {
            // 1. 问题分类
            QuestionClassifier.Classification classification = questionClassifier.classify(question);
            log.debug(I18N.get("hope.query.classified",
                classification.getType(), classification.getComplexity(), classification.getConfidence()));

            // 2. 首先查询高频层（当前会话上下文）- 获取上下文增强
            HighFrequencyLayerService.HighFreqQueryResult highFreqResult = highFreqLayer.query(sessionId, question);
            if (highFreqResult.isHasRelevantContext()) {
                resultBuilder.contexts(highFreqResult.getContexts());

                // 如果是话题延续，添加对话摘要到上下文
                if (highFreqResult.isTopicContinuation() && highFreqResult.getConversationSummary() != null) {
                    resultBuilder.contexts(List.of(highFreqResult.getConversationSummary()));
                }

                if (highFreqResult.getSessionContext() != null) {
                    resultBuilder.sessionContext(HOPEQueryResult.SessionContext.builder()
                        .sessionId(sessionId)
                        .currentTopic(highFreqResult.getCurrentTopic())
                        .build());
                }

                log.debug(I18N.get("hope.high_frequency.context_found", sessionId));
            }

            // 3. 查询低频层（技能知识）
            PermanentLayerService.PermanentQueryResult permResult = permanentLayer.query(question);

            if (permResult.isDirectAnswer()) {
                resultBuilder
                    .answer(permResult.getAnswer())
                    .sourceLayer("permanent")
                    .confidence(permResult.getConfidence())
                    .needsLLM(false)
                    .factualKnowledge(permResult.getFactualKnowledge());

                log.info(I18N.get("hope.query.direct_hit", permResult.getConfidence()));

                long processingTime = System.currentTimeMillis() - startTime;
                resultBuilder.processingTimeMs(processingTime);
                return resultBuilder.build();
            }

            if (permResult.getSkillTemplate() != null) {
                resultBuilder.skillTemplate(permResult.getSkillTemplate());
            }

            // 4. 查询中频层（近期问答）
            OrdinaryLayerService.OrdinaryQueryResult ordResult = ordinaryLayer.query(question);
            if (ordResult.isFound()) {
                if (ordResult.isDirectUsable()) {
                    resultBuilder
                        .answer(ordResult.getBestMatch().getAnswer())
                        .sourceLayer("ordinary")
                        .confidence(ordResult.getSimilarity())
                        .needsLLM(false);

                    log.info(I18N.get("hope.ordinary.direct_hit",
                        ordResult.getBestMatch().getId(), ordResult.getSimilarity()));

                    long processingTime = System.currentTimeMillis() - startTime;
                    resultBuilder.processingTimeMs(processingTime);
                    return resultBuilder.build();

                } else if (ordResult.isAsReference()) {
                    List<HOPEQueryResult.SimilarQA> similarQAs = ordResult.getAllMatches().stream()
                        .map(match -> HOPEQueryResult.SimilarQA.builder()
                            .question(match.getQa().getQuestion())
                            .answer(match.getQa().getAnswer())
                            .similarity(match.getSimilarity())
                            .rating(match.getQa().getRating())
                            .build())
                        .collect(Collectors.toList());

                    resultBuilder.similarQAs(similarQAs);
                    resultBuilder.needsLLM(true);

                    log.info(I18N.get("hope.ordinary.reference_hit",
                        ordResult.getBestMatch().getId(), ordResult.getSimilarity()));
                }
            } else {
                resultBuilder.needsLLM(true);
            }

        } catch (Exception e) {
            log.error(I18N.get("hope.query.error"), e);
            resultBuilder.needsLLM(true);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        resultBuilder.processingTimeMs(processingTime);

        HOPEQueryResult result = resultBuilder.build();

        log.info(I18N.get("hope.query.completed",
            result.isNeedsLLM() ? I18N.get("hope.strategy.needs_llm") : I18N.get("hope.strategy.direct_answer"),
            result.getSourceLayer(),
            processingTime));

        return result;
    }

    /**
     * 获取响应策略
     */
    public ResponseStrategy getStrategy(String question, HOPEQueryResult queryResult) {
        QuestionClassifier.Classification classification = questionClassifier.classify(question);
        return strategyDecider.decide(classification, queryResult);
    }

    /**
     * 构建优化的 Prompt（使用技能模板）
     */
    public String buildOptimizedPrompt(String question, SkillTemplate template, String context) {
        if (template == null || template.getPromptTemplate() == null) {
            return null;
        }

        return template.getPromptTemplate()
            .replace("{question}", question)
            .replace("{content}", context != null ? context : "")
            .replace("{context}", context != null ? context : "");
    }

    /**
     * 学习新知识 - 根据反馈调整层级
     */
    public void learn(String question, String answer, int rating, String sessionId) {
        if (!config.isEnabled()) {
            return;
        }

        try {
            // Phase 2: 保存到中频层（高分答案）
            if (rating >= 4) {
                ordinaryLayer.save(question, answer, rating);
                ordinaryLayer.checkAndPromote();
            }

            // Phase 3: 更新高频层的会话上下文
            if (sessionId != null && !sessionId.isEmpty()) {
                highFreqLayer.updateContext(sessionId, question, answer);
            }

            log.debug(I18N.get("hope.learn.recorded", rating));

        } catch (Exception e) {
            log.error(I18N.get("hope.learn.error"), e);
        }
    }

    /**
     * 添加临时定义到高频层
     */
    public void addTempDefinition(String sessionId, String term, String definition) {
        if (config.isEnabled() && sessionId != null) {
            highFreqLayer.addTempDefinition(sessionId, term, definition);
        }
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        if (config.isEnabled() && sessionId != null) {
            highFreqLayer.clearSession(sessionId);
        }
    }

    /**
     * 记录反馈
     */
    public void recordFeedback(HOPEQueryResult queryResult, boolean positive) {
        if (queryResult.getFactualKnowledge() != null) {
            permanentLayer.recordFactualFeedback(
                queryResult.getFactualKnowledge().getId(), positive);
        }
        if (queryResult.getSkillTemplate() != null) {
            permanentLayer.recordSkillFeedback(
                queryResult.getSkillTemplate().getId(), positive);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", config.isEnabled());
        stats.put("permanent", permanentLayer.getStatistics());
        stats.put("ordinary", ordinaryLayer.getStatistics());
        stats.put("highFrequency", highFreqLayer.getStatistics());
        return stats;
    }

    /**
     * 检查 HOPE 是否启用
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
