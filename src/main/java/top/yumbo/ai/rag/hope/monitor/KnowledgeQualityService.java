package top.yumbo.ai.rag.hope.monitor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.layer.OrdinaryLayerService;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.SkillTemplate;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识质量评估服务 (Knowledge Quality Assessment Service)
 * 
 * 负责评估 HOPE 三层知识结构的质量，并提供针对性的优化建议
 * (Responsible for evaluating the quality of HOPE three-layer knowledge structure and providing targeted optimization suggestions)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class KnowledgeQualityService {

    private final HOPEConfig config;
    private final PermanentLayerService permanentLayer;
    private final OrdinaryLayerService ordinaryLayer;

    /**
     * 构造函数 (Constructor)
     * 
     * @param config HOPE 配置 (HOPE configuration)
     * @param permanentLayer 低频层服务 (Permanent layer service)
     * @param ordinaryLayer 中频层服务 (Ordinary layer service)
     */
    @Autowired
    public KnowledgeQualityService(HOPEConfig config,
                                    PermanentLayerService permanentLayer,
                                    OrdinaryLayerService ordinaryLayer) {
        this.config = config;
        this.permanentLayer = permanentLayer;
        this.ordinaryLayer = ordinaryLayer;
    }

    /**
     * 执行完整的知识质量评估 (Execute complete knowledge quality assessment)
     * 
     * 算法说明 (Algorithm description):
     * 1. 检查配置是否启用
     * 2. 评估低频层知识质量
     * 3. 评估中频层知识质量
     * 4. 计算综合评分
     * 5. 确定整体状态
     * 6. 汇总问题和建议
     * 
     * @return 质量评估报告 (Quality assessment report)
     */
    public QualityReport assess() {
        // 1. 检查配置是否启用 (Check if configuration is enabled)
        if (!config.isEnabled()) {
            return QualityReport.builder()
                .overallScore(0)
                .status("disabled")  // 已禁用状态 (Disabled status)
                .build();
        }

        // 2. 初始化报告构建器和问题/建议列表 (Initialize report builder and issue/recommendation lists)
        QualityReport.QualityReportBuilder builder = QualityReport.builder();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 3. 评估低频层 (Assess permanent layer)
        PermanentLayerAssessment permAssessment = assessPermanentLayer();
        builder.permanentLayer(permAssessment);
        issues.addAll(permAssessment.getIssues());
        recommendations.addAll(permAssessment.getRecommendations());

        // 4. 评估中频层 (Assess ordinary layer)
        OrdinaryLayerAssessment ordAssessment = assessOrdinaryLayer();
        builder.ordinaryLayer(ordAssessment);
        issues.addAll(ordAssessment.getIssues());
        recommendations.addAll(ordAssessment.getRecommendations());

        // 5. 计算综合评分 (Calculate overall score)
        int overallScore = calculateOverallScore(permAssessment, ordAssessment);
        builder.overallScore(overallScore);

        // 6. 确定状态 (Determine status)
        String status;
        if (overallScore >= 80) {
            status = "excellent";      // 优秀 (Excellent)
        } else if (overallScore >= 60) {
            status = "good";           // 良好 (Good)
        } else if (overallScore >= 40) {
            status = "fair";           // 一般 (Fair)
        } else {
            status = "needs_improvement"; // 需要改进 (Needs improvement)
        }
        builder.status(status);

        // 7. 添加问题和建议 (Add issues and recommendations)
        builder.issues(issues);
        builder.recommendations(recommendations);

        // 8. 构建并返回报告 (Build and return report)
        return builder.build();
    }

    /**
     * 评估低频层知识质量 (Assess quality of permanent layer knowledge)
     * 
     * 评估技能模板和确定性知识的数量、启用情况和质量
     * (Evaluates the quantity, enabled status and quality of skill templates and factual knowledge)
     * 
     * @return 低频层评估结果 (Permanent layer assessment result)
     */
    private PermanentLayerAssessment assessPermanentLayer() {
        // 1. 初始化评估对象 (Initialize assessment object)
        PermanentLayerAssessment assessment = new PermanentLayerAssessment();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 2. 获取低频层统计数据 (Get permanent layer statistics)
        Map<String, Object> stats = permanentLayer.getStatistics();
        int skillCount = ((Number) stats.getOrDefault("skillTemplateCount", 0)).intValue();
        int factCount = ((Number) stats.getOrDefault("factualKnowledgeCount", 0)).intValue();
        int enabledSkills = ((Number) stats.getOrDefault("enabledSkills", 0)).intValue();
        int enabledFacts = ((Number) stats.getOrDefault("enabledFacts", 0)).intValue();

        // 3. 设置基础数据 (Set basic data)
        assessment.setSkillTemplateCount(skillCount);
        assessment.setFactualKnowledgeCount(factCount);
        assessment.setEnabledSkills(enabledSkills);
        assessment.setEnabledFacts(enabledFacts);

        // 4. 检查技能模板数量 (Check skill template count)
        if (skillCount < 3) {
            issues.add(I18N.get("hope.quality.insufficient_skills", skillCount));
            recommendations.add(I18N.get("hope.quality.add_more_skills"));
        }

        // 5. 检查确定性知识数量 (Check factual knowledge count)
        if (factCount < 5) {
            issues.add(I18N.get("hope.quality.insufficient_facts", factCount));
            recommendations.add(I18N.get("hope.quality.add_more_facts"));
        }

        // 6. 检查禁用率 (Check disabled rate)
        if (skillCount > 0) {
            double disabledRate = 1.0 - (double) enabledSkills / skillCount;
            if (disabledRate > 0.3) {
                issues.add(I18N.get("hope.quality.high_disabled_rate", String.format("%.0f%%", disabledRate * 100)));
                recommendations.add(I18N.get("hope.quality.check_disabled_skills"));
            }
        }

        // 7. 计算评分 (Calculate score)
        int score = 100;
        score -= Math.max(0, (3 - skillCount) * 10);  // 每少一个模板扣10分 (Deduct 10 points for each missing template)
        score -= Math.max(0, (5 - factCount) * 5);    // 每少一个知识扣5分 (Deduct 5 points for each missing knowledge)
        score -= issues.size() * 5;  // 每个问题扣5分 (Deduct 5 points for each issue)
        assessment.setScore(Math.max(0, score));

        // 8. 设置问题和建议 (Set issues and recommendations)
        assessment.setIssues(issues);
        assessment.setRecommendations(recommendations);

        return assessment;
    }

    /**
     * 评估中频层知识质量 (Assess quality of ordinary layer knowledge)
     * 
     * 评估问答对的数量、评分、晋升情况和访问分布
     * (Evaluates the quantity, rating, promotion status and access distribution of Q&A pairs)
     * 
     * @return 中频层评估结果 (Ordinary layer assessment result)
     */
    private OrdinaryLayerAssessment assessOrdinaryLayer() {
        // 1. 初始化评估对象 (Initialize assessment object)
        OrdinaryLayerAssessment assessment = new OrdinaryLayerAssessment();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 2. 获取中频层统计数据 (Get ordinary layer statistics)
        Map<String, Object> stats = ordinaryLayer.getStatistics();
        int totalCount = ((Number) stats.getOrDefault("totalCount", 0)).intValue();
        long promotedCount = ((Number) stats.getOrDefault("promotedCount", 0L)).longValue();
        double avgRating = ((Number) stats.getOrDefault("avgRating", 0.0)).doubleValue();
        long totalAccess = ((Number) stats.getOrDefault("totalAccess", 0L)).longValue();

        // 3. 设置基础数据 (Set basic data)
        assessment.setTotalQACount(totalCount);
        assessment.setPromotedCount(promotedCount);
        assessment.setAverageRating(avgRating);
        assessment.setTotalAccess(totalAccess);

        // 4. 检查问答数量 (Check Q&A count)
        if (totalCount == 0) {
            issues.add(I18N.get("hope.quality.no_qa_records"));
            recommendations.add(I18N.get("hope.quality.use_and_feedback"));
        }

        // 5. 检查平均评分 (Check average rating)
        if (totalCount > 10 && avgRating < 3.5) {
            issues.add(I18N.get("hope.quality.low_rating", String.format("%.1f", avgRating)));
            recommendations.add(I18N.get("hope.quality.check_low_rating"));
        }

        // 6. 检查晋升情况 (Check promotion status)
        if (totalCount > 50 && promotedCount == 0) {
            issues.add(I18N.get("hope.quality.no_promotions"));
            recommendations.add(I18N.get("hope.quality.check_promotion"));
        }

        // 7. 检查访问分布 (Check access distribution)
        if (totalCount > 0) {
            double avgAccess = (double) totalAccess / totalCount;
            assessment.setAverageAccess(avgAccess);
            if (avgAccess < 1.5) {
                recommendations.add(I18N.get("hope.quality.low_reuse"));
            }
        }

        // 8. 计算评分 (Calculate score)
        int score = 100;
        if (totalCount == 0) {
            score = 50;  // 无数据给基础分 (Give base score for no data)
        } else {
            score -= Math.max(0, (3.5 - avgRating) * 20);  // 评分低扣分 (Deduct points for low rating)
            score -= issues.size() * 5;  // 每个问题扣5分 (Deduct 5 points for each issue)
        }
        assessment.setScore(Math.max(0, Math.min(100, score)));

        // 9. 设置问题和建议 (Set issues and recommendations)
        assessment.setIssues(issues);
        assessment.setRecommendations(recommendations);

        return assessment;
    }

    /**
     * 计算综合评分 (Calculate overall score)
     * 
     * 根据低频层和中频层的评分计算综合评分
     * 低频层权重 60%，中频层权重 40%
     * (Calculates overall score based on permanent and ordinary layer assessments
     * Permanent layer weight: 60%, Ordinary layer weight: 40%)
     * 
     * @param perm 低频层评估结果 (Permanent layer assessment result)
     * @param ord 中频层评估结果 (Ordinary layer assessment result)
     * @return 综合评分 (Overall score)
     */
    private int calculateOverallScore(PermanentLayerAssessment perm, OrdinaryLayerAssessment ord) {
        // 低频层权重 60%，中频层权重 40% (Permanent layer weight 60%, Ordinary layer weight 40%)
        return (int) (perm.getScore() * 0.6 + ord.getScore() * 0.4);
    }

    /**
     * 质量评估报告 (Quality Assessment Report)
     * 
     * 包含整体评估结果、各层评估详情、问题列表和建议列表
     * (Contains overall assessment results, detailed layer assessments, issue list and recommendation list)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityReport {
        private int overallScore;                     // 综合评分 (Overall score)
        private String status;                        // 评估状态 (Assessment status): excellent, good, fair, needs_improvement
        private PermanentLayerAssessment permanentLayer; // 低频层评估 (Permanent layer assessment)
        private OrdinaryLayerAssessment ordinaryLayer;   // 中频层评估 (Ordinary layer assessment)
        private List<String> issues;                  // 问题列表 (List of issues)
        private List<String> recommendations;         // 建议列表 (List of recommendations)
    }

    /**
     * 低频层评估结果 (Permanent Layer Assessment Result)
     * 
     * 包含低频层的评分、统计数据、问题列表和建议列表
     * (Contains permanent layer score, statistics, issue list and recommendation list)
     */
    @Data
    public static class PermanentLayerAssessment {
        private int score;                           // 评分 (Score)
        private int skillTemplateCount;               // 技能模板数量 (Skill template count)
        private int factualKnowledgeCount;            // 确定性知识数量 (Factual knowledge count)
        private int enabledSkills;                    // 启用的技能数量 (Enabled skills count)
        private int enabledFacts;                     // 启用的知识数量 (Enabled facts count)
        private List<String> issues = new ArrayList<>();          // 问题列表 (List of issues)
        private List<String> recommendations = new ArrayList<>(); // 建议列表 (List of recommendations)
    }

    /**
     * 中频层评估结果 (Ordinary Layer Assessment Result)
     * 
     * 包含中频层的评分、统计数据、问题列表和建议列表
     * (Contains ordinary layer score, statistics, issue list and recommendation list)
     */
    @Data
    public static class OrdinaryLayerAssessment {
        private int score;                           // 评分 (Score)
        private int totalQACount;                    // 问答对总数 (Total Q&A pair count)
        private long promotedCount;                  // 晋升数量 (Promotion count)
        private double averageRating;                // 平均评分 (Average rating)
        private long totalAccess;                    // 总访问次数 (Total access count)
        private double averageAccess;                // 平均访问次数 (Average access count)
        private List<String> issues = new ArrayList<>();          // 问题列表 (List of issues)
        private List<String> recommendations = new ArrayList<>(); // 建议列表 (List of recommendations)
    }
}

