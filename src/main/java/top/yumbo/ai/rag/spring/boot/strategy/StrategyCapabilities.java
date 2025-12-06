package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 策略能力声明（Strategy Capabilities）
 *
 * <p>描述策略的能力边界和性能特征，用于智能调度</p>
 * <p>Describes strategy capability boundaries and performance characteristics for smart dispatching</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Data
@Builder
public class StrategyCapabilities {

    /** 支持的分析类型（Supported analysis types） */
    private Set<AnalysisType> supportedTypes;

    /** 最小文档数量（Minimum document count） */
    @Builder.Default
    private int minDocuments = 1;

    /** 最大文档数量（Maximum document count） */
    @Builder.Default
    private int maxDocuments = Integer.MAX_VALUE;

    /** 最佳文档数量（Optimal document count） */
    @Builder.Default
    private int optimalDocuments = 5;

    /** 最大内容长度 - 字符（Maximum content length in characters） */
    @Builder.Default
    private long maxContentLength = Long.MAX_VALUE;

    /** Token消耗级别（Token cost level） */
    @Builder.Default
    private CostLevel tokenCost = CostLevel.MEDIUM;

    /** 速度级别（Speed level） */
    @Builder.Default
    private SpeedLevel speed = SpeedLevel.MEDIUM;

    /** 输出质量级别（Output quality level） */
    @Builder.Default
    private QualityLevel quality = QualityLevel.GOOD;

    /** 关联保持级别（Relation preserve level） */
    @Builder.Default
    private RelationLevel relationPreserve = RelationLevel.MEDIUM;

    /** 必需的服务（Required services） */
    private List<String> requiredServices;

    /** 可选的服务（Optional services） */
    private List<String> optionalServices;

    /**
     * 分析类型（Analysis Type）
     */
    public enum AnalysisType {
        /** 摘要（Summary） */
        SUMMARY,
        /** 对比（Comparison） */
        COMPARE,
        /** 关联（Relationship） */
        RELATION,
        /** 因果（Causal） */
        CAUSAL,
        /** 综合（Comprehensive） */
        COMPREHENSIVE,
        /** 查询（Query） */
        QUERY
    }

    /**
     * 成本级别（Cost Level）
     */
    public enum CostLevel {
        /** 最低（Lowest） */
        LOWEST,
        /** 低（Low） */
        LOW,
        /** 中等（Medium） */
        MEDIUM,
        /** 高（High） */
        HIGH,
        /** 最高（Highest） */
        HIGHEST
    }

    /**
     * 速度级别（Speed Level）
     */
    public enum SpeedLevel {
        /** 最快（Fastest） */
        FASTEST,
        /** 快（Fast） */
        FAST,
        /** 中等（Medium） */
        MEDIUM,
        /** 慢（Slow） */
        SLOW,
        /** 最慢（Slowest） */
        SLOWEST
    }

    /**
     * 质量级别（Quality Level）
     */
    public enum QualityLevel {
        /** 基础（Basic） */
        BASIC,
        /** 良好（Good） */
        GOOD,
        /** 优秀（Excellent） */
        EXCELLENT,
        /** 最佳（Best） */
        BEST
    }

    /**
     * 关联保持级别（Relation Level）
     */
    public enum RelationLevel {
        /** 低（Low） */
        LOW,
        /** 中等（Medium） */
        MEDIUM,
        /** 高（High） */
        HIGH,
        /** 最佳（Best） */
        BEST
    }
}

