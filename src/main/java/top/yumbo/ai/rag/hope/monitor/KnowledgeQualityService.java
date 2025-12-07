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
 * 知识质量评估服务
 * (Knowledge Quality Assessment Service)
 *
 * 评估 HOPE 三层知识的质量，提供优化建议
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class KnowledgeQualityService {

    private final HOPEConfig config;
    private final PermanentLayerService permanentLayer;
    private final OrdinaryLayerService ordinaryLayer;

    @Autowired
    public KnowledgeQualityService(HOPEConfig config,
                                    PermanentLayerService permanentLayer,
                                    OrdinaryLayerService ordinaryLayer) {
        this.config = config;
        this.permanentLayer = permanentLayer;
        this.ordinaryLayer = ordinaryLayer;
    }

    /**
     * 执行完整的质量评估
     */
    public QualityReport assess() {
        if (!config.isEnabled()) {
            return QualityReport.builder()
                .overallScore(0)
                .status("disabled")
                .build();
        }

        QualityReport.QualityReportBuilder builder = QualityReport.builder();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // 1. 评估低频层
        PermanentLayerAssessment permAssessment = assessPermanentLayer();
        builder.permanentLayer(permAssessment);
        issues.addAll(permAssessment.getIssues());
        recommendations.addAll(permAssessment.getRecommendations());

        // 2. 评估中频层
        OrdinaryLayerAssessment ordAssessment = assessOrdinaryLayer();
        builder.ordinaryLayer(ordAssessment);
        issues.addAll(ordAssessment.getIssues());
        recommendations.addAll(ordAssessment.getRecommendations());

        // 3. 计算综合评分
        int overallScore = calculateOverallScore(permAssessment, ordAssessment);
        builder.overallScore(overallScore);

        // 4. 确定状态
        String status;
        if (overallScore >= 80) {
            status = "excellent";
        } else if (overallScore >= 60) {
            status = "good";
        } else if (overallScore >= 40) {
            status = "fair";
        } else {
            status = "needs_improvement";
        }
        builder.status(status);

        builder.issues(issues);
        builder.recommendations(recommendations);

        return builder.build();
    }

    /**
     * 评估低频层质量
     */
    private PermanentLayerAssessment assessPermanentLayer() {
        PermanentLayerAssessment assessment = new PermanentLayerAssessment();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        Map<String, Object> stats = permanentLayer.getStatistics();
        int skillCount = ((Number) stats.getOrDefault("skillTemplateCount", 0)).intValue();
        int factCount = ((Number) stats.getOrDefault("factualKnowledgeCount", 0)).intValue();
        int enabledSkills = ((Number) stats.getOrDefault("enabledSkills", 0)).intValue();
        int enabledFacts = ((Number) stats.getOrDefault("enabledFacts", 0)).intValue();

        assessment.setSkillTemplateCount(skillCount);
        assessment.setFactualKnowledgeCount(factCount);
        assessment.setEnabledSkills(enabledSkills);
        assessment.setEnabledFacts(enabledFacts);

        // 检查技能模板数量
        if (skillCount < 3) {
            issues.add("技能模板数量不足（当前: " + skillCount + "）");
            recommendations.add("建议添加更多技能模板，如：错误排查、性能优化、安全审计等");
        }

        // 检查确定性知识数量
        if (factCount < 5) {
            issues.add("确定性知识数量不足（当前: " + factCount + "）");
            recommendations.add("建议从 README、配置文件等提取更多确定性知识");
        }

        // 检查禁用率
        if (skillCount > 0) {
            double disabledRate = 1.0 - (double) enabledSkills / skillCount;
            if (disabledRate > 0.3) {
                issues.add("技能模板禁用率过高（" + String.format("%.0f%%", disabledRate * 100) + "）");
                recommendations.add("检查禁用的技能模板，改进或删除低质量模板");
            }
        }

        // 计算评分
        int score = 100;
        score -= Math.max(0, (3 - skillCount) * 10);  // 每少一个模板扣10分
        score -= Math.max(0, (5 - factCount) * 5);    // 每少一个知识扣5分
        score -= issues.size() * 5;  // 每个问题扣5分
        assessment.setScore(Math.max(0, score));

        assessment.setIssues(issues);
        assessment.setRecommendations(recommendations);

        return assessment;
    }

    /**
     * 评估中频层质量
     */
    private OrdinaryLayerAssessment assessOrdinaryLayer() {
        OrdinaryLayerAssessment assessment = new OrdinaryLayerAssessment();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        Map<String, Object> stats = ordinaryLayer.getStatistics();
        int totalCount = ((Number) stats.getOrDefault("totalCount", 0)).intValue();
        long promotedCount = ((Number) stats.getOrDefault("promotedCount", 0L)).longValue();
        double avgRating = ((Number) stats.getOrDefault("avgRating", 0.0)).doubleValue();
        long totalAccess = ((Number) stats.getOrDefault("totalAccess", 0L)).longValue();

        assessment.setTotalQACount(totalCount);
        assessment.setPromotedCount(promotedCount);
        assessment.setAverageRating(avgRating);
        assessment.setTotalAccess(totalAccess);

        // 检查问答数量
        if (totalCount == 0) {
            issues.add("中频层无问答记录");
            recommendations.add("开始使用系统并给予反馈，以积累知识");
        }

        // 检查平均评分
        if (totalCount > 10 && avgRating < 3.5) {
            issues.add("问答平均评分较低（" + String.format("%.1f", avgRating) + "）");
            recommendations.add("检查低分问答，分析原因并改进检索或提示词");
        }

        // 检查晋升情况
        if (totalCount > 50 && promotedCount == 0) {
            issues.add("没有知识晋升到低频层");
            recommendations.add("建议降低晋升阈值，或检查问答质量");
        }

        // 检查访问分布
        if (totalCount > 0) {
            double avgAccess = (double) totalAccess / totalCount;
            assessment.setAverageAccess(avgAccess);
            if (avgAccess < 1.5) {
                recommendations.add("问答复用率低，建议优化相似度匹配算法");
            }
        }

        // 计算评分
        int score = 100;
        if (totalCount == 0) {
            score = 50;  // 无数据给基础分
        } else {
            score -= Math.max(0, (3.5 - avgRating) * 20);  // 评分低扣分
            score -= issues.size() * 5;
        }
        assessment.setScore(Math.max(0, Math.min(100, score)));

        assessment.setIssues(issues);
        assessment.setRecommendations(recommendations);

        return assessment;
    }

    /**
     * 计算综合评分
     */
    private int calculateOverallScore(PermanentLayerAssessment perm, OrdinaryLayerAssessment ord) {
        // 低频层权重 60%，中频层权重 40%
        return (int) (perm.getScore() * 0.6 + ord.getScore() * 0.4);
    }

    /**
     * 质量报告
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityReport {
        private int overallScore;
        private String status;
        private PermanentLayerAssessment permanentLayer;
        private OrdinaryLayerAssessment ordinaryLayer;
        private List<String> issues;
        private List<String> recommendations;
    }

    /**
     * 低频层评估
     */
    @Data
    public static class PermanentLayerAssessment {
        private int score;
        private int skillTemplateCount;
        private int factualKnowledgeCount;
        private int enabledSkills;
        private int enabledFacts;
        private List<String> issues = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
    }

    /**
     * 中频层评估
     */
    @Data
    public static class OrdinaryLayerAssessment {
        private int score;
        private int totalQACount;
        private long promotedCount;
        private double averageRating;
        private long totalAccess;
        private double averageAccess;
        private List<String> issues = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
    }
}

