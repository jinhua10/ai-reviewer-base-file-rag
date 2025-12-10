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
 * HOPE 监控服务 (HOPE Monitor Service)
 * 
 * 负责收集、分析和报告 HOPE 系统的性能指标和健康状态
 * (Responsible for collecting, analyzing and reporting performance metrics and health status of HOPE system)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class HOPEMonitorService {

    private final HOPEConfig config;
    private final HOPEKnowledgeManager hopeManager;
    private final HOPEMetrics metrics;

    // 健康检查阈值 (Health check thresholds)
    private static final double MIN_SAVINGS_RATE = 0.1;  // 最小节省率 10% (Minimum savings rate 10%)
    private static final double MAX_AVG_RESPONSE_TIME = 5000;  // 最大平均响应时间 5s (Maximum average response time 5s)
    private static final double MAX_ERROR_RATE = 0.05;  // 最大错误率 5% (Maximum error rate 5%)

    /**
     * 构造函数 (Constructor)
     * 
     * @param config HOPE 配置 (HOPE configuration)
     * @param hopeManager HOPE 知识管理器 (HOPE knowledge manager)
     */
    @Autowired
    public HOPEMonitorService(HOPEConfig config,
                               HOPEKnowledgeManager hopeManager) {
        this.config = config;
        this.hopeManager = hopeManager;
        this.metrics = new HOPEMetrics();
    }

    /**
     * 初始化服务 (Initialize service)
     * 
     * 在服务启动后执行，检查配置并记录初始化状态
     * (Executed after service startup, checks configuration and records initialization status)
     */
    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            log.info(I18N.get("hope.monitor.init_success"));
        }
    }

    /**
     * 记录查询指标 (Record query metrics)
     * 
     * 记录查询的策略类型、命中层级和响应时间，用于后续分析
     * (Records query strategy type, hit layer and response time for subsequent analysis)
     * 
     * @param strategy 响应策略 (Response strategy)
     * @param result 查询结果 (Query result)
     * @param responseTimeMs 响应时间(毫秒) (Response time in milliseconds)
     */
    public void recordQuery(ResponseStrategy strategy, HOPEQueryResult result, long responseTimeMs) {
        // 1. 检查服务是否启用 (Check if service is enabled)
        if (!config.isEnabled()) {
            return;
        }

        try {
            // 2. 提取策略类型 (Extract strategy type)
            String strategyType = strategy != null ? strategy.name() : "FULL_RAG";  // 默认为完整RAG策略 (Default to full RAG strategy)
            // 3. 提取命中层级 (Extract hit layer)
            String hitLayer = result != null ? result.getSourceLayer() : null;
            // 4. 记录指标 (Record metrics)
            metrics.recordQuery(strategyType, hitLayer, responseTimeMs);
        } catch (Exception e) {
            // 5. 记录异常 (Log exception)
            log.warn(I18N.get("hope.monitor.record_query_failed"), e);
        }
    }

    /**
     * 记录学习事件 (Record learning event)
     * 
     * 当系统学习新知识时调用此方法
     * (Called when the system learns new knowledge)
     */
    public void recordLearn() {
        // 1. 检查服务是否启用 (Check if service is enabled)
        if (config.isEnabled()) {
            // 2. 记录学习事件 (Record learning event)
            metrics.recordLearn();
        }
    }

    /**
     * 记录晋升事件 (Record promotion event)
     * 
     * 当知识从低层级晋升到高层级时调用此方法
     * (Called when knowledge is promoted from lower layer to higher layer)
     */
    public void recordPromotion() {
        // 1. 检查服务是否启用 (Check if service is enabled)
        if (config.isEnabled()) {
            // 2. 记录晋升事件 (Record promotion event)
            metrics.recordPromotion();
        }
    }

    /**
     * 记录错误事件 (Record error event)
     * 
     * 当系统发生错误时调用此方法
     * (Called when an error occurs in the system)
     */
    public void recordError() {
        // 1. 检查服务是否启用 (Check if service is enabled)
        if (config.isEnabled()) {
            // 2. 记录错误事件 (Record error event)
            metrics.recordError();
        }
    }

    /**
     * 获取性能指标摘要 (Get performance metrics summary)
     * 
     * @return 性能指标摘要对象 (Performance metrics summary object)
     */
    public HOPEMetrics.MetricsSummary getMetricsSummary() {
        return metrics.getSummary();
    }

    /**
     * 获取完整的监控仪表盘数据 (Get complete monitoring dashboard data)
     * 
     * 收集并组织所有监控数据，包括启用状态、性能指标、层级统计、健康状态和优化建议
     * (Collects and organizes all monitoring data, including enabled status, performance metrics, 
     * layer statistics, health status and optimization suggestions)
     * 
     * @return 仪表盘数据映射 (Dashboard data map)
     */
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 1. HOPE 启用状态 (HOPE enabled status)
        dashboard.put("enabled", config.isEnabled());

        // 2. 性能指标 (Performance metrics)
        HOPEMetrics.MetricsSummary summary = metrics.getSummary();
        dashboard.put("metrics", summary);

        // 3. 三层统计 (Three-layer statistics)
        if (config.isEnabled()) {
            dashboard.put("layerStats", hopeManager.getStatistics());
        }

        // 4. 健康状态 (Health status)
        dashboard.put("health", getHealthStatus());

        // 5. 优化建议 (Optimization suggestions)
        dashboard.put("suggestions", getOptimizationSuggestions(summary));

        // 6. 时间戳 (Timestamp)
        dashboard.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return dashboard;
    }

    /**
     * 执行健康检查 (Perform health check)
     * 
     * 算法说明 (Algorithm description):
     * 1. 获取性能指标摘要
     * 2. 检查 LLM 节省率是否低于阈值
     * 3. 检查平均响应时间是否超过阈值
     * 4. 检查错误率是否超过阈值
     * 5. 根据检查结果设置健康状态
     * 
     * @return 健康状态对象 (Health status object)
     */
    public HealthStatus getHealthStatus() {
        // 1. 获取性能指标摘要 (Get performance metrics summary)
        HOPEMetrics.MetricsSummary summary = metrics.getSummary();

        // 2. 初始化健康状态 (Initialize health status)
        HealthStatus status = new HealthStatus();
        status.setStatus("healthy");  // 健康状态: healthy, warning, unhealthy (Health status: healthy, warning, unhealthy)

        // 3. 检查 LLM 节省率 (Check LLM savings rate)
        if (summary.getTotalQueries() > 100 && summary.getLlmSavingsRate() < MIN_SAVINGS_RATE) {
            status.setStatus("warning");  // 警告状态 (Warning status)
            status.getIssues().add(I18N.get("hope.monitor.low_savings_rate", MIN_SAVINGS_RATE * 100));
        }

        // 4. 检查响应时间 (Check response time)
        if (summary.getAvgResponseTimeMs() > MAX_AVG_RESPONSE_TIME) {
            status.setStatus("warning");  // 警告状态 (Warning status)
            status.getIssues().add(I18N.get("hope.monitor.high_response_time", MAX_AVG_RESPONSE_TIME));
        }

        // 5. 检查错误率 (Check error rate)
        if (summary.getTotalQueries() > 0) {
            double errorRate = (double) summary.getErrors() / summary.getTotalQueries();
            if (errorRate > MAX_ERROR_RATE) {
                status.setStatus("unhealthy");  // 不健康状态 (Unhealthy status)
                status.getIssues().add(I18N.get("hope.monitor.high_error_rate", MAX_ERROR_RATE * 100));
            }
        }

        return status;
    }

    /**
     * 生成系统优化建议 (Generate system optimization suggestions)
     * 
     * 基于性能指标分析系统运行状况，生成针对性的优化建议
     * (Analyzes system operating status based on performance metrics and generates targeted optimization suggestions)
     * 
     * @param summary 性能指标摘要 (Performance metrics summary)
     * @return 优化建议列表 (List of optimization suggestions)
     */
    public java.util.List<String> getOptimizationSuggestions(HOPEMetrics.MetricsSummary summary) {
        java.util.List<String> suggestions = new java.util.ArrayList<>();

        // 1. 检查直接回答率 (Check direct answer rate)
        if (summary.getTotalQueries() > 50) {
            double directRate = (double) summary.getDirectAnswers() / summary.getTotalQueries();
            if (directRate < 0.1) {
                suggestions.add(I18N.get("hope.monitor.suggestion_low_direct_rate"));
            }
        }

        // 2. 检查低频层命中率 (Check low-frequency layer hit rate)
        if (summary.getTotalQueries() > 50 && summary.getPermanentHits() < summary.getTotalQueries() * 0.05) {
            suggestions.add(I18N.get("hope.monitor.suggestion_low_permanent_hit"));
        }

        // 3. 检查中频层学习效果 (Check learning effectiveness of middle-frequency layer)
        if (summary.getLearnEvents() > 100 && summary.getOrdinaryHits() < summary.getLearnEvents() * 0.1) {
            suggestions.add(I18N.get("hope.monitor.suggestion_poor_learning"));
        }

        // 4. 检查知识晋升情况 (Check knowledge promotion status)
        if (summary.getLearnEvents() > 200 && summary.getPromotions() < 5) {
            suggestions.add(I18N.get("hope.monitor.suggestion_few_promotions"));
        }

        // 5. 检查响应时间差异 (Check response time difference)
        if (summary.getDirectAnswerAvgTimeMs() > 0 && summary.getFullRAGAvgTimeMs() > 0) {
            double speedup = summary.getFullRAGAvgTimeMs() / summary.getDirectAnswerAvgTimeMs();
            if (speedup > 10) {
                suggestions.add(I18N.get("hope.monitor.suggestion_good_performance", String.format("%.1f", speedup)));
            }
        }

        // 6. 系统正常运行情况 (Normal system operation)
        if (suggestions.isEmpty() && summary.getTotalQueries() > 0) {
            suggestions.add(I18N.get("hope.monitor.suggestion_no_optimization"));
        }

        return suggestions;
    }

    /**
     * 定时打印性能报告 (Scheduled printing of performance reports)
     * 
     * 每小时执行一次，打印系统运行状态的关键指标
     * (Executed once per hour, prints key indicators of system operation status)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void printHourlyReport() {
        // 1. 检查服务是否启用 (Check if service is enabled)
        if (!config.isEnabled()) {
            return;
        }

        // 2. 获取性能指标摘要 (Get performance metrics summary)
        HOPEMetrics.MetricsSummary summary = metrics.getSummary();

        // 3. 打印报告标题 (Print report title)
        log.info(I18N.get("hope.monitor.hourly_report"));
        
        // 4. 打印查询统计 (Print query statistics)
        log.info(I18N.get("hope.monitor.query_stats"),
            summary.getTotalQueries(),
            summary.getDirectAnswers(),
            summary.getTemplateAnswers(),
            summary.getFullRAGAnswers());
            
        // 5. 打印性能指标 (Print performance metrics)
        log.info(I18N.get("hope.monitor.performance_metrics"),
            summary.getLlmSavingsRate() * 100,
            summary.getAvgResponseTimeMs());
            
        // 6. 打印层级命中统计 (Print layer hit statistics)
        log.info(I18N.get("hope.monitor.layer_hit_stats"),
            summary.getPermanentHits(),
            summary.getOrdinaryHits(),
            summary.getHighFreqHits());
    }

    /**
     * 重置性能指标 (Reset performance metrics)
     * 
     * 清空所有累积的指标数据，重新开始统计
     * (Clears all accumulated metric data and starts statistics over)
     */
    public void resetMetrics() {
        metrics.reset();
        log.info(I18N.get("hope.monitor.metrics_reset"));
    }

    /**
     * 健康状态 (Health Status)
     * 
     * 表示系统当前的健康状态和存在的问题
     * (Represents current health status of the system and existing issues)
     */
    @lombok.Data
    public static class HealthStatus {
        private String status = "healthy";  // 健康状态: healthy, warning, unhealthy (Health status: healthy, warning, unhealthy)
        private java.util.List<String> issues = new java.util.ArrayList<>();  // 问题列表 (List of issues)
    }
}

