package top.yumbo.ai.rag.hope.monitor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HOPE 性能指标收集器
 * (HOPE Performance Metrics Collector)
 * 
 * 负责收集、存储和计算 HOPE 系统的各种性能指标
 * (Responsible for collecting, storing and calculating various performance metrics of HOPE system)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
public class HOPEMetrics {

    // 1. 查询统计 (Query statistics)
    private final AtomicLong totalQueries = new AtomicLong(0);      // 总查询数 (Total query count)
    private final AtomicLong directAnswers = new AtomicLong(0);     // 直接回答数 (Direct answer count)
    private final AtomicLong templateAnswers = new AtomicLong(0);  // 模板回答数 (Template answer count)
    private final AtomicLong fullRAGAnswers = new AtomicLong(0);    // 完整RAG回答数 (Full RAG answer count)

    // 2. 层级命中统计 (Layer hit statistics)
    private final AtomicLong permanentHits = new AtomicLong(0);      // 低频层命中数 (Permanent layer hit count)
    private final AtomicLong ordinaryHits = new AtomicLong(0);       // 中频层命中数 (Ordinary layer hit count)
    private final AtomicLong highFreqHits = new AtomicLong(0);       // 高频层命中数 (High frequency layer hit count)

    // 3. 响应时间统计（毫秒）(Response time statistics in milliseconds)
    private final AtomicLong totalResponseTime = new AtomicLong(0);   // 总响应时间 (Total response time)
    private final AtomicLong directAnswerTime = new AtomicLong(0);   // 直接回答总时间 (Total direct answer time)
    private final AtomicLong templateAnswerTime = new AtomicLong(0);  // 模板回答总时间 (Total template answer time)
    private final AtomicLong fullRAGTime = new AtomicLong(0);        // 完整RAG总时间 (Total full RAG time)

    // 4. 学习统计 (Learning statistics)
    private final AtomicLong learnEvents = new AtomicLong(0);        // 学习事件数 (Learning event count)
    private final AtomicLong promotions = new AtomicLong(0);         // 晋升事件数 (Promotion event count)

    // 5. 错误统计 (Error statistics)
    private final AtomicLong errors = new AtomicLong(0);              // 错误数 (Error count)

    // 6. 按小时统计 (Hourly statistics)
    private final Map<String, HourlyStats> hourlyStats = new ConcurrentHashMap<>();

    // 7. 统计起始时间 (Statistics start time)
    private LocalDateTime startTime = LocalDateTime.now();

    /**
     * 记录查询指标
     * (Record query metrics)
     * 
     * 根据策略类型和命中层级更新相应的计数器和响应时间
     * (Updates corresponding counters and response times based on strategy type and hit layer)
     * 
     * @param strategyType 策略类型 (Strategy type): DIRECT_ANSWER, TEMPLATE_ANSWER, FULL_RAG
     * @param hitLayer 命中层级 (Hit layer): permanent, ordinary, high_frequency
     * @param responseTimeMs 响应时间(毫秒) (Response time in milliseconds)
     */
    public void recordQuery(String strategyType, String hitLayer, long responseTimeMs) {
        // 1. 更新总查询数和总响应时间 (Update total query count and total response time)
        totalQueries.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);

        // 2. 按策略类型统计 (Statistics by strategy type)
        switch (strategyType) {
            case "DIRECT_ANSWER" -> {
                directAnswers.incrementAndGet();
                directAnswerTime.addAndGet(responseTimeMs);
            }
            case "TEMPLATE_ANSWER" -> {
                templateAnswers.incrementAndGet();
                TemplateAnswerTime.addAndGet(responseTimeMs);
            }
            case "FULL_RAG" -> {
                fullRAGAnswers.incrementAndGet();
                fullRAGTime.addAndGet(responseTimeMs);
            }
        }

        // 3. 按命中层级统计 (Statistics by hit layer)
        if (hitLayer != null) {
            switch (hitLayer) {
                case "permanent" -> permanentHits.incrementAndGet();
                case "ordinary" -> ordinaryHits.incrementAndGet();
                case "high_frequency" -> highFreqHits.incrementAndGet();
            }
        }

        // 4. 更新小时统计 (Update hourly statistics)
        updateHourlyStats(strategyType, responseTimeMs);
    }

    /**
     * 记录学习事件 (Record learning event)
     * 
     * 当系统学习新知识时调用此方法
     * (Called when the system learns new knowledge)
     */
    public void recordLearn() {
        learnEvents.incrementAndGet();
    }

    /**
     * 记录晋升事件 (Record promotion event)
     * 
     * 当知识从低层级晋升到高层级时调用此方法
     * (Called when knowledge is promoted from lower layer to higher layer)
     */
    public void recordPromotion() {
        promotions.incrementAndGet();
    }

    /**
     * 记录错误事件 (Record error event)
     * 
     * 当系统发生错误时调用此方法
     * (Called when an error occurs in the system)
     */
    public void recordError() {
        errors.incrementAndGet();
    }

    /**
     * 更新小时统计数据 (Update hourly statistics data)
     * 
     * 为当前小时创建或更新统计数据
     * (Creates or updates statistical data for the current hour)
     * 
     * @param strategyType 策略类型 (Strategy type)
     * @param responseTimeMs 响应时间(毫秒) (Response time in milliseconds)
     */
    private void updateHourlyStats(String strategyType, long responseTimeMs) {
        // 1. 生成小时键值 (Generate hour key)
        String hourKey = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
        // 2. 更新对应小时的统计数据 (Update statistics for the corresponding hour)
        hourlyStats.computeIfAbsent(hourKey, k -> new HourlyStats()).update(strategyType, responseTimeMs);
    }

    /**
     * 获取 LLM 调用节省率 (Get LLM call savings rate)
     * 
     * 计算通过直接回答和模板回答避免的 LLM 调用比例
     * (Calculates the proportion of LLM calls avoided through direct answers and template answers)
     * 
     * @return 节省率，范围 [0.0, 1.0] (Savings rate, range [0.0, 1.0])
     */
    public double getLLMSavingsRate() {
        long total = totalQueries.get();
        if (total == 0) return 0.0;
        return (double) (directAnswers.get() + templateAnswers.get()) / total;
    }

    /**
     * 获取平均响应时间 (Get average response time)
     * 
     * 计算所有查询的平均响应时间
     * (Calculates the average response time for all queries)
     * 
     * @return 平均响应时间(毫秒) (Average response time in milliseconds)
     */
    public double getAverageResponseTime() {
        long total = totalQueries.get();
        if (total == 0) return 0.0;
        return (double) totalResponseTime.get() / total;
    }

    /**
     * 获取直接回答平均响应时间 (Get direct answer average response time)
     * 
     * 计算直接回答类型的查询平均响应时间
     * (Calculates the average response time for direct answer type queries)
     * 
     * @return 直接回答平均响应时间(毫秒) (Direct answer average response time in milliseconds)
     */
    public double getDirectAnswerAvgTime() {
        long count = directAnswers.get();
        if (count == 0) return 0.0;
        return (double) directAnswerTime.get() / count;
    }

    /**
     * 获取完整 RAG 平均响应时间 (Get full RAG average response time)
     * 
     * 计算完整 RAG 类型的查询平均响应时间
     * (Calculates the average response time for full RAG type queries)
     * 
     * @return 完整 RAG 平均响应时间(毫秒) (Full RAG average response time in milliseconds)
     */
    public double getFullRAGAvgTime() {
        long count = fullRAGAnswers.get();
        if (count == 0) return 0.0;
        return (double) fullRAGTime.get() / count;
    }

    /**
     * 获取性能指标摘要报告 (Get performance metrics summary report)
     * 
     * 收集所有当前指标数据并计算衍生指标，生成完整的摘要报告
     * (Collects all current metric data and calculates derived metrics to generate a complete summary report)
     * 
     * @return 指标摘要对象 (Metrics summary object)
     */
    public MetricsSummary getSummary() {
        return MetricsSummary.builder()
            .totalQueries(totalQueries.get())
            .directAnswers(directAnswers.get())
            .templateAnswers(templateAnswers.get())
            .fullRAGAnswers(fullRAGAnswers.get())
            .permanentHits(permanentHits.get())
            .ordinaryHits(ordinaryHits.get())
            .highFreqHits(highFreqHits.get())
            .llmSavingsRate(getLLMSavingsRate())
            .avgResponseTimeMs(getAverageResponseTime())
            .directAnswerAvgTimeMs(getDirectAnswerAvgTime())
            .fullRAGAvgTimeMs(getFullRAGAvgTime())
            .learnEvents(learnEvents.get())
            .promotions(promotions.get())
            .errors(errors.get())
            .uptimeMinutes(java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes())
            .build();
    }

    /**
     * 重置所有统计数据
     * (Reset all statistical data)
     * 
     * 将所有计数器清零，清除小时统计数据，并重置起始时间
     * (Clears all counters to zero, clears hourly statistics, and resets the start time)
     */
    public void reset() {
        // 1. 重置查询计数器 (Reset query counters)
        totalQueries.set(0);
        directAnswers.set(0);
        templateAnswers.set(0);
        fullRAGAnswers.set(0);
        
        // 2. 重置层级命中计数器 (Reset layer hit counters)
        permanentHits.set(0);
        ordinaryHits.set(0);
        highFreqHits.set(0);
        
        // 3. 重置响应时间计数器 (Reset response time counters)
        totalResponseTime.set(0);
        directAnswerTime.set(0);
        templateAnswerTime.set(0);
        fullRAGTime.set(0);
        
        // 4. 重置学习事件计数器 (Reset learning event counters)
        learnEvents.set(0);
        promotions.set(0);
        
        // 5. 重置错误计数器 (Reset error counter)
        errors.set(0);
        
        // 6. 清除小时统计数据 (Clear hourly statistics)
        hourlyStats.clear();
        
        // 7. 重置起始时间 (Reset start time)
        startTime = LocalDateTime.now();
    }

    /**
     * 小时统计数据 (Hourly Statistics)
     * 
     * 存储单个小时内的查询统计信息
     * (Stores query statistics within a single hour)
     */
    @Data
    public static class HourlyStats {
        private final AtomicLong queries = new AtomicLong(0);         // 查询总数 (Total queries)
        private final AtomicLong directAnswers = new AtomicLong(0);   // 直接回答数 (Direct answer count)
        private final AtomicLong totalTime = new AtomicLong(0);       // 总响应时间 (Total response time)

        /**
         * 更新小时统计数据 (Update hourly statistics)
         * 
         * @param strategyType 策略类型 (Strategy type)
         * @param responseTimeMs 响应时间(毫秒) (Response time in milliseconds)
         */
        public void update(String strategyType, long responseTimeMs) {
            // 1. 更新查询总数和总时间 (Update total queries and total time)
            queries.incrementAndGet();
            totalTime.addAndGet(responseTimeMs);
            
            // 2. 如果是直接回答，更新直接回答计数 (If direct answer, update direct answer count)
            if ("DIRECT_ANSWER".equals(strategyType)) {
                directAnswers.incrementAndGet();
            }
        }
    }

    /**
     * 性能指标摘要报告 (Performance Metrics Summary Report)
     * 
     * 包含所有性能指标的汇总数据和计算结果
     * (Contains summary data and calculation results of all performance metrics)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private long totalQueries;              // 总查询数 (Total queries)
        private long directAnswers;             // 直接回答数 (Direct answers)
        private long templateAnswers;            // 模板回答数 (Template answers)
        private long fullRAGAnswers;             // 完整RAG回答数 (Full RAG answers)
        private long permanentHits;              // 低频层命中数 (Permanent layer hits)
        private long ordinaryHits;               // 中频层命中数 (Ordinary layer hits)
        private long highFreqHits;               // 高频层命中数 (High frequency layer hits)
        private double llmSavingsRate;           // LLM节省率 (LLM savings rate)
        private double avgResponseTimeMs;        // 平均响应时间(毫秒) (Average response time in milliseconds)
        private double directAnswerAvgTimeMs;    // 直接回答平均时间(毫秒) (Direct answer average time in milliseconds)
        private double fullRAGAvgTimeMs;         // 完整RAG平均时间(毫秒) (Full RAG average time in milliseconds)
        private long learnEvents;                // 学习事件数 (Learning events)
        private long promotions;                 // 晋升事件数 (Promotions)
        private long errors;                     // 错误数 (Errors)
        private long uptimeMinutes;              // 运行时间(分钟) (Uptime in minutes)
    }
}

