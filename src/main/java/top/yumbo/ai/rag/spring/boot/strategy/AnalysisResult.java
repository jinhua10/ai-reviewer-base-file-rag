package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分析结果（Analysis Result）
 *
 * <p>封装分析策略执行后的所有结果数据</p>
 * <p>Encapsulates all result data after analysis strategy execution</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Data
@Builder
public class AnalysisResult {

    /** 是否成功（Whether successful） */
    private boolean success;

    /** 主要答案/结论（Main answer/conclusion） */
    private String answer;

    /** 综合摘要（Comprehensive summary） */
    private String comprehensiveSummary;

    /** 最终报告 - 详细版（Final report - detailed version） */
    private String finalReport;

    /** 关键要点列表（List of key points） */
    private List<String> keyPoints;

    /** 文档间关联（Document relationships） */
    private List<DocumentRelation> relations;

    /** 对比结果 - 如果是对比分析（Comparison result - if comparison analysis） */
    private ComparisonResult comparison;

    /** 因果链 - 如果是因果分析（Causal chain - if causal analysis） */
    private List<CausalLink> causalChain;

    /** 使用的策略（Strategies used） */
    private List<String> strategiesUsed;

    /** 执行时间（毫秒）（Execution time in milliseconds） */
    private long executionTimeMs;

    /** Token消耗（Token consumption） */
    private int tokensUsed;

    /** 错误信息 - 如果失败（Error message - if failed） */
    private String errorMessage;

    /** 元数据（Metadata） */
    private Map<String, Object> metadata;

    /**
     * 文档关联（Document Relation）
     *
     * <p>描述两个文档之间的关系</p>
     * <p>Describes relationship between two documents</p>
     */
    @Data
    @Builder
    public static class DocumentRelation {
        /** 文档1（Document 1） */
        private String doc1;

        /** 文档2（Document 2） */
        private String doc2;

        /** 关系类型：similar, related, contradicts, supplements（Relation type） */
        private String relationType;

        /** 关系描述（Relation description） */
        private String description;

        /** 置信度 0-1（Confidence 0-1） */
        private double confidence;
    }

    /**
     * 对比结果（Comparison Result）
     *
     * <p>结构化的对比分析结果</p>
     * <p>Structured comparison analysis result</p>
     */
    @Data
    @Builder
    public static class ComparisonResult {
        /** 共同点（Common points） */
        private List<String> commonPoints;

        /** 差异点（Differences） */
        private List<String> differences;

        /** 优缺点：文档名 -> 优点/缺点列表（Pros and cons: doc name -> list） */
        private Map<String, List<String>> prosAndCons;

        /** 推荐/建议（Recommendation） */
        private String recommendation;
    }

    /**
     * 因果链接（Causal Link）
     *
     * <p>描述因果关系的单个环节</p>
     * <p>Describes a single link in causal relationship</p>
     */
    @Data
    @Builder
    public static class CausalLink {
        /** 原因（Cause） */
        private String cause;

        /** 结果（Effect） */
        private String effect;

        /** 证据（Evidence） */
        private String evidence;

        /** 来源文档（Source document） */
        private String sourceDocument;

        /** 置信度 0-1（Confidence 0-1） */
        private double confidence;
    }

    /**
     * 创建成功结果（Create success result）
     *
     * @param answer 答案（Answer）
     * @return 成功的分析结果（Successful analysis result）
     */
    public static AnalysisResult success(String answer) {
        return AnalysisResult.builder()
                .success(true)
                .answer(answer)
                .build();
    }

    /**
     * 创建失败结果（Create failure result）
     *
     * @param errorMessage 错误信息（Error message）
     * @return 失败的分析结果（Failed analysis result）
     */
    public static AnalysisResult failure(String errorMessage) {
        return AnalysisResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

