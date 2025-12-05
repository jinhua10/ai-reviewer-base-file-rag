package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分析配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "document-analysis.memo")
public class DocumentAnalysisConfig {

    // ==================== 容量配置 ====================

    /** 默认短期记忆容量 */
    private int defaultShortTermCapacity = 3;

    /** 按文档类型的短期记忆容量 */
    private Map<String, Integer> shortTermCapacityByType = new HashMap<>();

    /** 长期备忘录最大条目数 */
    private int longTermMaxEntries = 100;

    /** 单条备忘录最大 token 数 */
    private int memoEntryMaxTokens = 200;

    // ==================== 召回配置 ====================

    /** 召回相关条目数 */
    private int recallTopK = 3;

    /** 相关性阈值 */
    private double relevanceThreshold = 0.3;

    /** 相关性权重配置 */
    private RelevanceWeights relevanceWeights = new RelevanceWeights();

    // ==================== 压缩配置 ====================

    /** 默认压缩策略 */
    private String defaultCompressionStrategy = "hybrid";

    /** 按文档类型的压缩策略 */
    private Map<String, String> compressionStrategyByType = new HashMap<>();

    /** 压缩比例 */
    private double compressionRatio = 0.3;

    /** 最小保留 token 数 */
    private int minCompressedTokens = 50;

    // ==================== 分层处理配置 ====================

    /** 聚合配置 */
    private AggregationConfig aggregation = new AggregationConfig();

    /** 跳过重复处理配置 */
    private SkipReprocessingConfig skipReprocessing = new SkipReprocessingConfig();

    // ==================== 阶段性输出配置 ====================

    /** 阶段性输出配置 */
    private StageOutputConfig stageOutput = new StageOutputConfig();

    // ==================== Token 预算配置 ====================

    /** Prompt 预算配置 */
    private PromptBudgetConfig promptBudget = new PromptBudgetConfig();

    // ==================== 关键词配置 ====================

    /** 关键词配置 */
    private KeywordConfig keyword = new KeywordConfig();

    // ==================== 内部配置类 ====================

    @Data
    public static class RelevanceWeights {
        private double keywordMatch = 0.4;
        private double entityMatch = 0.2;
        private double importance = 0.2;
        private double recency = 0.1;
        private double structuralDistance = 0.1;
    }

    @Data
    public static class AggregationConfig {
        private boolean enabled = true;
        private int triggerThreshold = 20;
        private String defaultStrategy = "structural";
        private int maxEntriesPerGroup = 5;
        private int groupTargetTokens = 300;
        private IndependentEntryConfig independentEntry = new IndependentEntryConfig();
    }

    @Data
    public static class IndependentEntryConfig {
        private double importanceThreshold = 0.8;
        private boolean detectCriticalData = true;
        private boolean detectConclusionKeywords = true;
        private List<String> conclusionKeywords = Arrays.asList(
                "总结", "结论", "关键", "重要", "核心", "决策", "建议", "风险",
                "summary", "conclusion", "key", "important"
        );
    }

    @Data
    public static class SkipReprocessingConfig {
        private int minTokensThreshold = 50;
        private double targetCompressionRatio = 0.3;
        private int maxCompressionRounds = 2;
        private boolean detectStructuredBullets = true;
    }

    @Data
    public static class StageOutputConfig {
        private boolean enabled = true;
        private int[] triggerPoints = {25, 50, 75};
        private boolean realtimeSegmentOutput = true;
        private boolean autoExportMemoDocument = true;
        private String defaultExportFormat = "markdown";
    }

    @Data
    public static class PromptBudgetConfig {
        private int total = 4000;
        private int currentSegment = 1500;
        private int shortTermMemory = 1000;
        private int longTermRecall = 800;
        private int instruction = 500;
        private int safetyMargin = 200;
        private Map<String, PromptBudgetOverride> typeOverrides = new HashMap<>();
    }

    @Data
    public static class PromptBudgetOverride {
        private Integer total;
        private Integer currentSegment;
        private Integer shortTermMemory;
        private Integer longTermRecall;
    }

    @Data
    public static class KeywordConfig {
        private int maxKeywordsPerEntry = 10;
        private boolean filterStopwords = true;
        private int minKeywordLength = 2;
        private boolean extractNamedEntities = true;
    }
}

