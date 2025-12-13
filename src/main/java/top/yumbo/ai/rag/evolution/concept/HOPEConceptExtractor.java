package top.yumbo.ai.rag.evolution.concept;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.RecentQA;
import top.yumbo.ai.rag.hope.model.SkillTemplate;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;

/**
 * HOPE 概念提取器 (HOPE Concept Extractor)
 *
 * 从 HOPE 三层架构中提取最小概念单元
 * (Extract minimal concept units from HOPE three-layer architecture)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class HOPEConceptExtractor {

    private final HOPEKnowledgeManager hopeManager;

    @Autowired
    public HOPEConceptExtractor(@Autowired(required = false) HOPEKnowledgeManager hopeManager) {
        this.hopeManager = hopeManager;
    }

    /**
     * 从低频层提取概念 (Extract concepts from permanent layer)
     *
     * 低频层包含：技能模板和确定性知识
     * (Permanent layer contains: skill templates and factual knowledge)
     */
    public List<MinimalConcept> extractFromPermanentLayer() {
        if (hopeManager == null || !hopeManager.isEnabled()) {
            log.warn("HOPE manager is not available");
            return Collections.emptyList();
        }

        List<MinimalConcept> concepts = new ArrayList<>();

        try {
            // 注意：由于 HOPE 层服务没有直接的 getAllSkills/getAllFacts 方法，
            // 这里提供一个简化的实现，实际应用中可以通过扩展 HOPE 层服务来获取数据
            // (Note: Since HOPE layer services don't have direct getAllSkills/getAllFacts methods,
            // this is a simplified implementation. In practice, extend HOPE layer services to access data)

            log.info("Low-frequency layer concept extraction requires HOPE layer service extension");
            log.info(I18N.get("log.evolution.permanent_extracted"), concepts.size());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.permanent_extract_failed"), e);
        }

        return concepts;
    }

    /**
     * 从中频层提取概念 (Extract concepts from ordinary layer)
     *
     * 中频层包含：高质量的历史 QA
     * (Ordinary layer contains: high-quality historical QA)
     */
    public List<MinimalConcept> extractFromOrdinaryLayer(int minRating, int minAccessCount) {
        if (hopeManager == null || !hopeManager.isEnabled()) {
            return Collections.emptyList();
        }

        List<MinimalConcept> concepts = new ArrayList<>();

        try {
            // 注意：中频层概念提取需要 HOPE 层服务扩展
            // (Note: Ordinary layer concept extraction requires HOPE layer service extension)
            log.info("Mid-frequency layer concept extraction requires HOPE layer service extension");
            log.info(I18N.get("log.evolution.ordinary_extracted"), concepts.size());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.ordinary_extract_failed"), e);
        }

        return concepts;
    }

    /**
     * 从高频层提取概念 (Extract concepts from high frequency layer)
     *
     * 高频层包含：最近的 QA，用于发现新兴概念
     * (High frequency layer contains: recent QA, for discovering emerging concepts)
     */
    public List<MinimalConcept> extractFromHighFrequencyLayer(int limit) {
        if (hopeManager == null || !hopeManager.isEnabled()) {
            return Collections.emptyList();
        }

        List<MinimalConcept> concepts = new ArrayList<>();

        try {
            // 注意：高频层概念提取需要 HOPE 层服务扩展
            // (Note: High frequency layer concept extraction requires HOPE layer service extension)
            log.info("High-frequency layer concept extraction requires HOPE layer service extension (limit: {})", limit);
            log.info(I18N.get("log.evolution.highfreq_extracted"), concepts.size());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.highfreq_extract_failed"), e);
        }

        return concepts;
    }

    /**
     * 提取所有层的概念 (Extract concepts from all layers)
     */
    public List<MinimalConcept> extractAllConcepts() {
        List<MinimalConcept> allConcepts = new ArrayList<>();

        // 从低频层提取 (Extract from permanent layer)
        allConcepts.addAll(extractFromPermanentLayer());

        // 从中频层提取（评分>=4，访问>=10）(Extract from ordinary layer)
        allConcepts.addAll(extractFromOrdinaryLayer(4, 10));

        // 从高频层提取（最近50条）(Extract from high frequency layer)
        allConcepts.addAll(extractFromHighFrequencyLayer(50));

        log.info(I18N.get("log.evolution.all_extracted"), allConcepts.size());

        return allConcepts;
    }

    // ============================================================================
    // 辅助方法 (Helper Methods)
    // ============================================================================

    /**
     * 从 QA 推断角色 (Infer roles from QA)
     *
     * 注：原设计包含从 SkillTemplate 和 FactualKnowledge 推断角色，
     * 需要扩展 HOPE 层服务后实现
     * (Note: Original design includes inferring roles from SkillTemplate and FactualKnowledge,
     * to be implemented after extending HOPE layer services)
     */
    private List<String> inferRolesFromQA(String question) {
        List<String> roles = new ArrayList<>();
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("部署") || lowerQuestion.contains("deploy") || lowerQuestion.contains("运维")) {
            roles.add("devops");
        }
        if (lowerQuestion.contains("代码") || lowerQuestion.contains("code") || lowerQuestion.contains("实现")) {
            roles.add("developer");
        }
        if (lowerQuestion.contains("架构") || lowerQuestion.contains("architecture") || lowerQuestion.contains("设计")) {
            roles.add("architect");
        }
        if (lowerQuestion.contains("算法") || lowerQuestion.contains("algorithm") || lowerQuestion.contains("原理")) {
            roles.add("researcher");
        }
        if (lowerQuestion.contains("需求") || lowerQuestion.contains("产品") || lowerQuestion.contains("用户")) {
            roles.add("product_manager");
        }

        return roles.isEmpty() ? List.of("developer") : roles;
    }

    /**
     * 从问题提取概念名称 (Extract concept name from question)
     */
    private String extractConceptNameFromQuestion(String question) {
        // 简化版：取问题的关键词 (Simplified: extract keywords)
        String name = question.replaceAll("[?？！!。\\s]+", "");
        if (name.length() > 50) {
            name = name.substring(0, 50) + "...";
        }
        return name;
    }

    /**
     * 从 QA 推断概念类型 (Infer concept type from QA)
     */
    private MinimalConcept.ConceptType inferConceptTypeFromQA(String question) {
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("如何") || lowerQuestion.contains("怎么") || lowerQuestion.contains("how to")) {
            return MinimalConcept.ConceptType.PROCESS;
        }
        if (lowerQuestion.contains("什么是") || lowerQuestion.contains("what is")) {
            return MinimalConcept.ConceptType.DEFINITION;
        }
        if (lowerQuestion.contains("为什么") || lowerQuestion.contains("why")) {
            return MinimalConcept.ConceptType.RULE;
        }

        return MinimalConcept.ConceptType.FACT;
    }
}

