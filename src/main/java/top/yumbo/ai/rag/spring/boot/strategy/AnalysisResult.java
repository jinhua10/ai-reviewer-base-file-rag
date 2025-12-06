package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分析结果
 * (Analysis Result)
 */
@Data
@Builder
public class AnalysisResult {

    /**
     * 是否成功
     * (Whether successful)
     */
    private boolean success;

    /**
     * 主要答案/结论
     * (Main answer/conclusion)
     */
    private String answer;

    /**
     * 综合摘要
     * (Comprehensive summary)
     */
    private String comprehensiveSummary;

    /**
     * 最终报告（详细版）
     * (Final report - detailed version)
     */
    private String finalReport;

    /**
     * 关键要点列表
     * (List of key points)
     */
    private List<String> keyPoints;

    /**
     * 文档间关联
     * (Document relationships)
     */
    private List<DocumentRelation> relations;

    /**
     * 对比结果（如果是对比分析）
     * (Comparison result - if comparison analysis)
     */
    private ComparisonResult comparison;

    /**
     * 因果链（如果是因果分析）
     * (Causal chain - if causal analysis)
     */
    private List<CausalLink> causalChain;

    /**
     * 使用的策略
     * (Strategies used)
     */
    private List<String> strategiesUsed;

    /**
     * 执行时间（毫秒）
     * (Execution time in milliseconds)
     */
    private long executionTimeMs;

    /**
     * Token消耗
     * (Token consumption)
     */
    private int tokensUsed;

    /**
     * 错误信息（如果失败）
     * (Error message - if failed)
     */
    private String errorMessage;

    /**
     * 元数据
     * (Metadata)
     */
    private Map<String, Object> metadata;

    /**
     * 文档关联
     * (Document Relation)
     */
    @Data
    @Builder
    public static class DocumentRelation {
        private String doc1;
        private String doc2;
        private String relationType;  // similar, related, contradicts, supplements
        private String description;
        private double confidence;
    }

    /**
     * 对比结果
     * (Comparison Result)
     */
    @Data
    @Builder
    public static class ComparisonResult {
        private List<String> commonPoints;
        private List<String> differences;
        private Map<String, List<String>> prosAndCons;
        private String recommendation;
    }

    /**
     * 因果链接
     * (Causal Link)
     */
    @Data
    @Builder
    public static class CausalLink {
        private String cause;
        private String effect;
        private String evidence;
        private String sourceDocument;
        private double confidence;
    }

    /**
     * 创建成功结果
     * (Create success result)
     */
    public static AnalysisResult success(String answer) {
        return AnalysisResult.builder()
                .success(true)
                .answer(answer)
                .build();
    }

    /**
     * 创建失败结果
     * (Create failure result)
     */
    public static AnalysisResult failure(String errorMessage) {
        return AnalysisResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

