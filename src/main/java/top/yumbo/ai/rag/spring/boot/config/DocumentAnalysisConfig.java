package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分析配置 (Document Analysis Configuration)
 * 
 * 负责配置文档分析的参数，包括容量配置、召回配置、压缩配置等
 * (Responsible for configuring document analysis parameters, including capacity configuration, 
 * recall configuration, compression configuration, etc.)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "document-analysis.memo")
public class DocumentAnalysisConfig {

    // ==================== 容量配置 ====================
    // (Capacity Configuration)

    /**
     * 默认短期记忆容量 (Default short-term memory capacity)
     * 系统默认的短期记忆条目数量
     * (Default number of entries for short-term memory in the system)
     */
    private int defaultShortTermCapacity = 3;

    /**
     * 按文档类型的短期记忆容量 (Short-term memory capacity by document type)
     * 不同文档类型对应的短期记忆容量配置
     * (Short-term memory capacity configuration for different document types)
     */
    private Map<String, Integer> shortTermCapacityByType = new HashMap<>();

    /**
     * 长期备忘录最大条目数 (Maximum number of long-term memo entries)
     * 系统允许存储的长期备忘录条目上限
     * (Maximum number of long-term memo entries allowed in the system)
     */
    private int longTermMaxEntries = 100;

    /**
     * 单条备忘录最大 token 数 (Maximum token count per memo entry)
     * 每条备忘录记录允许的最大token数量
     * (Maximum number of tokens allowed per memo entry)
     */
    private int memoEntryMaxTokens = 200;

    // ==================== 召回配置 ====================
    // (Recall Configuration)

    /**
     * 召回相关条目数 (Number of relevant entries to recall)
     * 检索时返回的相关条目数量
     * (Number of relevant entries to return during retrieval)
     */
    private int recallTopK = 3;

    /**
     * 相关性阈值 (Relevance threshold)
     * 条目被视为相关的最小相关性分数
     * (Minimum relevance score for an entry to be considered relevant)
     */
    private double relevanceThreshold = 0.3;

    /**
     * 相关性权重配置 (Relevance weight configuration)
     * 不同相关性因素的权重配置
     * (Weight configuration for different relevance factors)
     */
    private RelevanceWeights relevanceWeights = new RelevanceWeights();

    // ==================== 压缩配置 ====================
    // (Compression Configuration)

    /**
     * 默认压缩策略 (Default compression strategy)
     * 系统默认使用的内容压缩策略
     * (Default content compression strategy used by the system)
     */
    private String defaultCompressionStrategy = "hybrid";

    /**
     * 按文档类型的压缩策略 (Compression strategy by document type)
     * 不同文档类型使用的压缩策略映射
     * (Compression strategy mapping for different document types)
     */
    private Map<String, String> compressionStrategyByType = new HashMap<>();

    /**
     * 压缩比例 (Compression ratio)
     * 内容压缩的目标比例
     * (Target ratio for content compression)
     */
    private double compressionRatio = 0.3;

    /**
     * 最小保留 token 数 (Minimum保留 token count)
     * 压缩后保留的最小token数量
     * (Minimum number of tokens to retain after compression)
     */
    private int minCompressedTokens = 50;

    // ==================== 分层处理配置 ====================
    // (Layered Processing Configuration)

    /**
     * 聚合配置 (Aggregation configuration)
     * 内容聚合的配置参数
     * (Configuration parameters for content aggregation)
     */
    private AggregationConfig aggregation = new AggregationConfig();

    /**
     * 跳过重复处理配置 (Skip reprocessing configuration)
     * 跳过重复处理的配置参数
     * (Configuration parameters for skipping reprocessing)
     */
    private SkipReprocessingConfig skipReprocessing = new SkipReprocessingConfig();

    // ==================== 阶段性输出配置 ====================
    // (Stage Output Configuration)

    /**
     * 阶段性输出配置 (Stage output configuration)
     * 阶段性输出的配置参数
     * (Configuration parameters for stage output)
     */
    private StageOutputConfig stageOutput = new StageOutputConfig();

    // ==================== Token 预算配置 ====================
    // (Token Budget Configuration)

    /**
     * Prompt 预算配置 (Prompt budget configuration)
     * 提示词预算的配置参数
     * (Configuration parameters for prompt budget)
     */
    private PromptBudgetConfig promptBudget = new PromptBudgetConfig();

    // ==================== 关键词配置 ====================
    // (Keyword Configuration)

    /**
     * 关键词配置 (Keyword configuration)
     * 关键词提取和处理的配置参数
     * (Configuration parameters for keyword extraction and processing)
     */
    private KeywordConfig keyword = new KeywordConfig();

    // ==================== 内部配置类 ====================
    // (Internal Configuration Classes)

    /**
     * 相关性权重 (Relevance Weights)
     * 
     * 定义不同因素对相关性计算的权重
     * (Defines weights for different factors in relevance calculation)
     */
    @Data
    public static class RelevanceWeights {
        /**
         * 关键词匹配权重 (Keyword match weight)
         * 关键词匹配对相关性的影响权重
         * (Weight of keyword matching in relevance calculation)
         */
        private double keywordMatch = 0.4;
        
        /**
         * 实体匹配权重 (Entity match weight)
         * 实体匹配对相关性的影响权重
         * (Weight of entity matching in relevance calculation)
         */
        private double entityMatch = 0.2;
        
        /**
         * 重要性权重 (Importance weight)
         * 内容重要性对相关性的影响权重
         * (Weight of content importance in relevance calculation)
         */
        private double importance = 0.2;
        
        /**
         * 新近度权重 (Recency weight)
         * 内容新近度对相关性的影响权重
         * (Weight of content recency in relevance calculation)
         */
        private double recency = 0.1;
        
        /**
         * 结构距离权重 (Structural distance weight)
         * 结构距离对相关性的影响权重
         * (Weight of structural distance in relevance calculation)
         */
        private double structuralDistance = 0.1;
    }

    /**
     * 聚合配置 (Aggregation Configuration)
     * 
     * 定义内容聚合的参数和策略
     * (Defines parameters and strategies for content aggregation)
     */
    @Data
    public static class AggregationConfig {
        /**
         * 是否启用聚合 (Whether to enable aggregation)
         * 控制是否启用内容聚合功能
         * (Controls whether content aggregation is enabled)
         */
        private boolean enabled = true;
        
        /**
         * 触发阈值 (Trigger threshold)
         * 触发聚合的最小条目数量
         * (Minimum number of entries to trigger aggregation)
         */
        private int triggerThreshold = 20;
        
        /**
         * 默认策略 (Default strategy)
         * 默认使用的聚合策略
         * (Default aggregation strategy to use)
         */
        private String defaultStrategy = "structural";
        
        /**
         * 每组最大条目数 (Maximum entries per group)
         * 每个聚合组允许的最大条目数
         * (Maximum number of entries allowed per aggregation group)
         */
        private int maxEntriesPerGroup = 5;
        
        /**
         * 组目标 token 数 (Group target token count)
         * 每个聚合组的目标token数量
         * (Target token count for each aggregation group)
         */
        private int groupTargetTokens = 300;
        
        /**
         * 独立条目配置 (Independent entry configuration)
         * 独立条目处理的配置参数
         * (Configuration parameters for independent entry processing)
         */
        private IndependentEntryConfig independentEntry = new IndependentEntryConfig();
    }

    /**
     * 独立条目配置 (Independent Entry Configuration)
     * 
     * 定义独立条目处理的参数
     * (Defines parameters for independent entry processing)
     */
    @Data
    public static class IndependentEntryConfig {
        /**
         * 重要性阈值 (Importance threshold)
         * 条目被视为重要性的最小阈值
         * (Minimum threshold for an entry to be considered important)
         */
        private double importanceThreshold = 0.8;
        
        /**
         * 是否检测关键数据 (Whether to detect critical data)
         * 控制是否启用关键数据检测功能
         * (Controls whether critical data detection is enabled)
         */
        private boolean detectCriticalData = true;
        
        /**
         * 是否检测结论关键词 (Whether to detect conclusion keywords)
         * 控制是否启用结论关键词检测
         * (Controls whether conclusion keyword detection is enabled)
         */
        private boolean detectConclusionKeywords = true;
        
        /**
         * 结论关键词列表 (Conclusion keywords list)
         * 用于识别结论的关键词列表
         * (Keyword list used to identify conclusions)
         */
        private List<String> conclusionKeywords = Arrays.asList(
                "总结", "结论", "关键", "重要", "核心", "决策", "建议", "风险",
                "summary", "conclusion", "key", "important"
        );
    }

    /**
     * 跳过重复处理配置 (Skip Reprocessing Configuration)
     * 
     * 定义跳过重复处理的参数
     * (Defines parameters for skipping reprocessing)
     */
    @Data
    public static class SkipReprocessingConfig {
        /**
         * 最小 token 阈值 (Minimum token threshold)
         * 跳过重复处理的最小token数量
         * (Minimum token count to skip reprocessing)
         */
        private int minTokensThreshold = 50;
        
        /**
         * 目标压缩比例 (Target compression ratio)
         * 跳过重复处理的目标压缩比例
         * (Target compression ratio for skipping reprocessing)
         */
        private double targetCompressionRatio = 0.3;
        
        /**
         * 最大压缩轮数 (Maximum compression rounds)
         * 允许的最大压缩轮数
         * (Maximum allowed compression rounds)
         */
        private int maxCompressionRounds = 2;
        
        /**
         * 是否检测结构化项目符号 (Whether to detect structured bullets)
         * 控制是否启用结构化项目符号检测
         * (Controls whether structured bullet detection is enabled)
         */
        private boolean detectStructuredBullets = true;
    }

    /**
     * 阶段性输出配置 (Stage Output Configuration)
     * 
     * 定义阶段性输出的参数
     * (Defines parameters for stage output)
     */
    @Data
    public static class StageOutputConfig {
        /**
         * 是否启用阶段性输出 (Whether to enable stage output)
         * 控制是否启用阶段性输出功能
         * (Controls whether stage output is enabled)
         */
        private boolean enabled = true;
        
        /**
         * 触发点 (Trigger points)
         * 阶段性输出的触发点百分比
         * (Trigger point percentages for stage output)
         */
        private int[] triggerPoints = {25, 50, 75};
        
        /**
         * 是否实时分段输出 (Whether to output segments in real-time)
         * 控制是否实时输出分段内容
         * (Controls whether segments are output in real-time)
         */
        private boolean realtimeSegmentOutput = true;
        
        /**
         * 是否自动导出备忘录文档 (Whether to automatically export memo document)
         * 控制是否自动导出备忘录文档
         * (Controls whether memo documents are automatically exported)
         */
        private boolean autoExportMemoDocument = true;
        
        /**
         * 默认导出格式 (Default export format)
         * 默认的文档导出格式
         * (Default document export format)
         */
        private String defaultExportFormat = "markdown";
    }

    /**
     * Prompt 预算配置 (Prompt Budget Configuration)
     * 
     * 定义 Prompt 预算的参数
     * (Defines parameters for prompt budget)
     */
    @Data
    public static class PromptBudgetConfig {
        /**
         * 总 token 数 (Total token count)
         * Prompt 总的 token 数量
         * (Total number of tokens for prompt)
         */
        private int total = 4000;
        
        /**
         * 当前分段 token 数 (Current segment token count)
         * 当前分段的 token 数量
         * (Token count for current segment)
         */
        private int currentSegment = 1500;
        
        /**
         * 短期记忆 token 数 (Short-term memory token count)
         * 短期记忆的 token 数量
         * (Token count for short-term memory)
         */
        private int shortTermMemory = 1000;
        
        /**
         * 长期召回 token 数 (Long-term recall token count)
         * 长期召回的 token 数量
         * (Token count for long-term recall)
         */
        private int longTermRecall = 800;
        
        /**
         * 指令 token 数 (Instruction token count)
         * 指令的 token 数量
         * (Token count for instructions)
         */
        private int instruction = 500;
        
        /**
         * 安全边界 token 数 (Safety margin token count)
         * 安全边界的 token 数量
         * (Token count for safety margin)
         */
        private int safetyMargin = 200;
        
        /**
         * 类型覆盖配置 (Type override configuration)
         * 不同类型的预算覆盖配置
         * (Budget override configuration for different types)
         */
        private Map<String, PromptBudgetOverride> typeOverrides = new HashMap<>();
    }

    /**
     * Prompt 预算覆盖配置 (Prompt Budget Override Configuration)
     * 
     * 定义特定类型的 Prompt 预算覆盖参数
     * (Defines prompt budget override parameters for specific types)
     */
    @Data
    public static class PromptBudgetOverride {
        /**
         * 总 token 数覆盖 (Total token count override)
         * 覆盖的总 token 数量
         * (Override for total token count)
         */
        private Integer total;
        
        /**
         * 当前分段 token 数覆盖 (Current segment token count override)
         * 覆盖的当前分段 token 数量
         * (Override for current segment token count)
         */
        private Integer currentSegment;
        
        /**
         * 短期记忆 token 数覆盖 (Short-term memory token count override)
         * 覆盖的短期记忆 token 数量
         * (Override for short-term memory token count)
         */
        private Integer shortTermMemory;
        
        /**
         * 长期召回 token 数覆盖 (Long-term recall token count override)
         * 覆盖的长期召回 token 数量
         * (Override for long-term recall token count)
         */
        private Integer longTermRecall;
    }

    /**
     * 关键词配置 (Keyword Configuration)
     * 
     * 定义关键词处理的参数
     * (Defines parameters for keyword processing)
     */
    @Data
    public static class KeywordConfig {
        /**
         * 每条目最大关键词数 (Maximum keywords per entry)
         * 每条目允许的最大关键词数量
         * (Maximum number of keywords allowed per entry)
         */
        private int maxKeywordsPerEntry = 10;
        
        /**
         * 是否过滤停用词 (Whether to filter stopwords)
         * 控制是否过滤停用词
         * (Controls whether to filter stopwords)
         */
        private boolean filterStopwords = true;
        
        /**
         * 最小关键词长度 (Minimum keyword length)
         * 关键词的最小长度
         * (Minimum length for keywords)
         */
        private int minKeywordLength = 2;
        
        /**
         * 是否提取命名实体 (Whether to extract named entities)
         * 控制是否提取命名实体
         * (Controls whether to extract named entities)
         */
        private boolean extractNamedEntities = true;
    }
}

