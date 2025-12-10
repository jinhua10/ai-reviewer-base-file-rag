package top.yumbo.ai.rag.hope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.HOPEQueryResult;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 响应策略决策器
 * (Response Strategy Decider)
 *
 * 根据问题分类和 HOPE 查询结果，决定使用哪种响应策略
 * (Decide which response strategy to use based on question classification and HOPE query results)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class ResponseStrategyDecider {

    private final HOPEConfig config;

    @Autowired
    public ResponseStrategyDecider(HOPEConfig config) {
        this.config = config;
    }

    /**
     * 决定响应策略
     *
     * @param classification 问题分类结果 (Question classification result)
     * @param queryResult HOPE 查询结果 (HOPE query result)
     * @return 推荐的响应策略 (Recommended response strategy)
     */
    public ResponseStrategy decide(QuestionClassifier.Classification classification,
                                   HOPEQueryResult queryResult) {

        // 如果 HOPE 未启用，直接使用完整 RAG
        if (!config.isEnabled()) {
            return ResponseStrategy.FULL_RAG;
        }

        double directAnswerThreshold = config.getStrategy().getDirectAnswerConfidence();

        // 策略1: 直接回答
        // 条件：不需要 LLM + 置信度超过阈值
        if (!queryResult.isNeedsLLM()
            && queryResult.getConfidence() >= directAnswerThreshold
            && queryResult.getAnswer() != null) {

            log.info(I18N.get("hope.strategy.direct_answer",
                queryResult.getSourceLayer(), queryResult.getConfidence()));
            return ResponseStrategy.DIRECT_ANSWER;
        }

        // 策略2: 模板增强回答
        // 条件：有技能模板 + 问题不太复杂
        if (queryResult.hasSkillTemplate()
            && config.getStrategy().isEnableSkillTemplates()
            && classification.getComplexity() != QuestionClassifier.ComplexityLevel.COMPLEX) {

            log.info(I18N.get("hope.strategy.template_answer",
                queryResult.getSkillTemplate().getName()));
            return ResponseStrategy.TEMPLATE_ANSWER;
        }

        // 策略3: 参考增强回答
        // 条件：有相似问答参考 + 相似度较高
        if (queryResult.hasSimilarReference()) {
            HOPEQueryResult.SimilarQA bestMatch = queryResult.getSimilarQAs().stream()
                .max((a, b) -> Double.compare(a.getSimilarity(), b.getSimilarity()))
                .orElse(null);

            if (bestMatch != null && bestMatch.getSimilarity() >= 0.7) {
                log.info(I18N.get("hope.strategy.reference_answer",
                    bestMatch.getSimilarity()));
                return ResponseStrategy.REFERENCE_ANSWER;
            }
        }

        // 策略4: 完整 RAG
        log.info(I18N.get("hope.strategy.full_rag"));
        return ResponseStrategy.FULL_RAG;
    }

    /**
     * 获取策略说明
     */
    public String getStrategyExplanation(ResponseStrategy strategy,
                                         QuestionClassifier.Classification classification,
                                         HOPEQueryResult queryResult) {
        StringBuilder explanation = new StringBuilder();

        explanation.append(I18N.get("hope.strategy.label_strategy")).append(strategy.getName()).append("\n");
        explanation.append(I18N.get("hope.strategy.label_question_type")).append(classification.getType()).append("\n");
        explanation.append(I18N.get("hope.strategy.label_complexity")).append(classification.getComplexity()).append("\n");
        explanation.append(I18N.get("hope.strategy.label_needs_llm")).append(strategy.requiresLLM()).append("\n");

        if (queryResult.getSourceLayer() != null) {
            explanation.append(I18N.get("hope.strategy.label_hit_layer")).append(queryResult.getSourceLayer()).append("\n");
            explanation.append(I18N.get("hope.strategy.label_confidence")).append(String.format("%.2f", queryResult.getConfidence())).append("\n");
        }

        if (queryResult.hasSkillTemplate()) {
            explanation.append(I18N.get("hope.strategy.label_skill_template")).append(queryResult.getSkillTemplate().getName()).append("\n");
        }

        return explanation.toString();
    }
}

