package top.yumbo.ai.rag.hope.monitor;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.ResponseStrategy;
import top.yumbo.ai.rag.hope.model.HOPEQueryResult;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * HOPE 监控服务 - 收集和报告性能指标
 * (HOPE Monitor Service - Collects and reports performance metrics)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class HOPEMonitorService {

    private final HOPEConfig config;
    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMetrics metrics;

    // 健康检查阈值
    private static final double MIN_SAVINGS_RATE = 0.1;  // 最小节省率 10%
    private static final double MAX_AVG_RESPONSE_TIME = 5000;  // 最大平均响应时间 5s
    private static final double MAX_ERROR_RATE = 0.05;  // 最大错误率 5%

    @Autowired
    public HOPEMonitorService(HOPEConfig config,
                               HOPEKnowledgeManager hopeManager) {
        this.config = config;
        this.hopeManager = hopeManager;
        this.metrics = new HOPEMetrics();
    }

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            log.info(I18N.get("hope.monitor.init_success"));
        }
    }

    /**
     * 记录查询（供 KnowledgeQAService 调用）
     */
    public void recordQuery(ResponseStrategy strategy, HOPEQueryResult result, long responseTimeMs) {
        if (!config.isEnabled()) {
            return;
        }

        try {
            String strategyType = strategy != null ? strategy.name() : "FULL_RAG";
            String hitLayer = result != null ? result.getSourceLayer() : null;
            metrics.recordQuery(strategyType, hitLayer, responseTimeMs);
        } catch (Exception e) {
            log.warn("Failed to record query metrics", e);
        }
    }

    /**
     * 记录学习事件
     */
    public void recordLearn() {
        if (config.isEnabled()) {
            metrics.recordLearn();
        }
    }

    /**
     * 记录晋升事件
     */
    public void recordPromotion() {
        if (config.isEnabled()) {
            metrics.recordPromotion();
        }
    }

    /**
     * 记录错误
     */
    public void recordError() {
        if (config.isEnabled()) {
            metrics.recordError();
        }
    }

    /**
     * 获取性能指标摘要
     */
    public HOPEMetrics.MetricsSummary getMetricsSummary() {
        return metrics.getSummary();
    }

    /**
     * 获取完整的监控仪表盘数据
     */
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 1. HOPE 启用状态
        dashboard.put("enabled", config.isEnabled());

        // 2. 性能指标
        HOPEMetrics.MetricsSummary summary = metrics.getSummary();
        dashboard.put("metrics", summary);

        // 3. 三层统计
        if (config.isEnabled()) {
            dashboard.put("layerStats", hopeManager.getStatistics());
        }

        // 4. 健康状态
        dashboard.put("health", getHealthStatus());

        // 5. 优化建议
        dashboard.put("suggestions", getOptimizationSuggestions(summary));

        // 6. 时间戳
        dashboard.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return dashboard;
    }

    /**
     * 健康检查
     */
    public HealthStatus getHealthStatus() {
        HOPEMetrics.MetricsSummary summary = metrics.getSummary();

        HealthStatus status = new HealthStatus();
        status.setStatus("healthy");

        // 检查 LLM 节省率
        if (summary.getTotalQueries() > 100 && summary.getLlmSavingsRate() < MIN_SAVINGS_RATE) {
            status.setStatus("warning");
            status.getIssues().add("LLM 节省率低于 " + (MIN_SAVINGS_RATE * 100) + "%");
        }

        // 检查响应时间
        if (summary.getAvgResponseTimeMs() > MAX_AVG_RESPONSE_TIME) {
            status.setStatus("warning");
            status.getIssues().add("平均响应时间超过 " + MAX_AVG_RESPONSE_TIME + "ms");
        }

        // 检查错误率
        if (summary.getTotalQueries() > 0) {
            double errorRate = (double) summary.getErrors() / summary.getTotalQueries();
            if (errorRate > MAX_ERROR_RATE) {
                status.setStatus("unhealthy");
                status.getIssues().add("错误率超过 " + (MAX_ERROR_RATE * 100) + "%");
            }
        }

        return status;
    }

    /**
     * 生成优化建议
     */
    public java.util.List<String> getOptimizationSuggestions(HOPEMetrics.MetricsSummary summary) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        // 1. 如果直接回答率低
        if (summary.getTotalQueries() > 50) {
            double directRate = (double) summary.getDirectAnswers() / summary.getTotalQueries();
            if (directRate < 0.1) {
                suggestions.add("💡 直接回答率较低，建议添加更多确定性知识到低频层");
            }
        }

        // 2. 如果低频层命中率低
        if (summary.getTotalQueries() > 50 && summary.getPermanentHits() < summary.getTotalQueries() * 0.05) {
            suggestions.add("💡 低频层命中率低，建议检查技能模板和确定性知识的配置");
        }

        // 3. 如果中频层命中率低但有大量学习事件
        if (summary.getLearnEvents() > 100 && summary.getOrdinaryHits() < summary.getLearnEvents() * 0.1) {
            suggestions.add("💡 中频层学习效果不佳，建议调整相似度阈值或晋升条件");
        }

        // 4. 如果晋升很少
        if (summary.getLearnEvents() > 200 && summary.getPromotions() < 5) {
            suggestions.add("💡 知识晋升很少，建议降低晋升阈值以积累更多永久知识");
        }

        // 5. 如果响应时间差异大
        if (summary.getDirectAnswerAvgTimeMs() > 0 && summary.getFullRAGAvgTimeMs() > 0) {
            double speedup = summary.getFullRAGAvgTimeMs() / summary.getDirectAnswerAvgTimeMs();
            if (speedup > 10) {
                suggestions.add("✅ 直接回答比完整 RAG 快 " + String.format("%.1f", speedup) + " 倍，HOPE 效果良好");
            }
        }

        // 6. 如果一切正常
        if (suggestions.isEmpty() && summary.getTotalQueries() > 0) {
            suggestions.add("✅ HOPE 系统运行正常，无需优化");
        }

        return suggestions;
    }

    /**
     * 定时打印性能报告（每小时）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void printHourlyReport() {
        if (!config.isEnabled()) {
            return;
        }

        HOPEMetrics.MetricsSummary summary = metrics.getSummary();

        log.info(I18N.get("hope.monitor.hourly_report"));
        log.info("  总查询: {}, 直接回答: {}, 模板增强: {}, 完整RAG: {}",
            summary.getTotalQueries(),
            summary.getDirectAnswers(),
            summary.getTemplateAnswers(),
            summary.getFullRAGAnswers());
        log.info("  LLM 节省率: {:.1f}%, 平均响应时间: {:.0f}ms",
            summary.getLlmSavingsRate() * 100,
            summary.getAvgResponseTimeMs());
        log.info("  层级命中 - 低频: {}, 中频: {}, 高频: {}",
            summary.getPermanentHits(),
            summary.getOrdinaryHits(),
            summary.getHighFreqHits());
    }

    /**
     * 重置指标
     */
    public void resetMetrics() {
        metrics.reset();
        log.info(I18N.get("hope.monitor.metrics_reset"));
    }

    /**
     * 健康状态
     */
    @lombok.Data
    public static class HealthStatus {
        private String status = "healthy";  // healthy, warning, unhealthy
        private java.util.List<String> issues = new java.util.ArrayList<>();
    }
}

