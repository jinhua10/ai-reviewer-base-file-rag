package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 策略能力声明
 * (Strategy Capabilities)
 */
@Data
@Builder
public class StrategyCapabilities {

    /**
     * 支持的分析类型
     * (Supported analysis types)
     */
    private Set<AnalysisType> supportedTypes;

    /**
     * 最小文档数量
     * (Minimum document count)
     */
    @Builder.Default
    private int minDocuments = 1;

    /**
     * 最大文档数量
     * (Maximum document count)
     */
    @Builder.Default
    private int maxDocuments = Integer.MAX_VALUE;

    /**
     * 最佳文档数量
     * (Optimal document count)
     */
    @Builder.Default
    private int optimalDocuments = 5;

    /**
     * 最大内容长度（字符）
     * (Maximum content length in characters)
     */
    @Builder.Default
    private long maxContentLength = Long.MAX_VALUE;

    /**
     * Token消耗级别
     * (Token cost level)
     */
    @Builder.Default
    private CostLevel tokenCost = CostLevel.MEDIUM;

    /**
     * 速度级别
     * (Speed level)
     */
    @Builder.Default
    private SpeedLevel speed = SpeedLevel.MEDIUM;

    /**
     * 输出质量级别
     * (Output quality level)
     */
    @Builder.Default
    private QualityLevel quality = QualityLevel.GOOD;

    /**
     * 关联保持级别
     * (Relation preserve level)
     */
    @Builder.Default
    private RelationLevel relationPreserve = RelationLevel.MEDIUM;

    /**
     * 必需的服务
     * (Required services)
     */
    private List<String> requiredServices;

    /**
     * 可选的服务
     * (Optional services)
     */
    private List<String> optionalServices;

    /**
     * 分析类型
     * (Analysis Type)
     */
    public enum AnalysisType {
        SUMMARY,        // 摘要
        COMPARE,        // 对比
        RELATION,       // 关联
        CAUSAL,         // 因果
        COMPREHENSIVE,  // 综合
        QUERY           // 查询
    }

    /**
     * 成本级别
     * (Cost Level)
     */
    public enum CostLevel {
        LOWEST, LOW, MEDIUM, HIGH, HIGHEST
    }

    /**
     * 速度级别
     * (Speed Level)
     */
    public enum SpeedLevel {
        FASTEST, FAST, MEDIUM, SLOW, SLOWEST
    }

    /**
     * 质量级别
     * (Quality Level)
     */
    public enum QualityLevel {
        BASIC, GOOD, EXCELLENT, BEST
    }

    /**
     * 关联保持级别
     * (Relation Level)
     */
    public enum RelationLevel {
        LOW, MEDIUM, HIGH, BEST
    }
}

