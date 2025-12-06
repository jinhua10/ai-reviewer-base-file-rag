package top.yumbo.ai.rag.spring.boot.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.util.*;

/**
 * 结构化对比策略（Structured Comparison Strategy）
 *
 * <p>对多个文档进行结构化对比分析，生成对比表格和差异报告</p>
 * <p>Perform structured comparison analysis on multiple documents, generate comparison tables and difference reports</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Slf4j
@Component
public class StructuredCompareStrategy extends AbstractAnalysisStrategy {

    /** 对比分析提示词模板（Comparison analysis prompt template） */
    private static final String COMPARE_PROMPT_TEMPLATE = """
            请对以下多个文档进行详细对比分析：
            (Please perform detailed comparison analysis on the following documents:)
            
            用户问题(User question)：%s
            
            文档内容(Document contents)：
            %s
            
            请按以下结构输出对比分析(Please output comparison analysis in the following structure)：
            
            ## 1. 总体对比概述(Overall Comparison Overview)
            简要说明各文档的主要特点和定位
            (Brief description of main features and positioning of each document)
            
            ## 2. 对比表格(Comparison Table)
            | 维度(Dimension) | 文档1(Doc1) | 文档2(Doc2) | ... |
            |------|-------|-------|-----|
            | 主题(Theme) | ... | ... | ... |
            | 核心观点(Core viewpoints) | ... | ... | ... |
            | 适用场景(Use cases) | ... | ... | ... |
            
            ## 3. 共同点(Common Points)
            - 共同点1(Common point 1)
            - 共同点2(Common point 2)
            
            ## 4. 主要差异(Main Differences)
            - 差异1(Difference 1)
            - 差异2(Difference 2)
            
            ## 5. 各自优缺点(Pros and Cons)
            ### 文档1(Document 1)
            - 优点(Pros)：...
            - 缺点(Cons)：...
            
            ## 6. 结论和建议(Conclusion and Recommendations)
            根据分析给出结论(Provide conclusion based on analysis)
            """;

    @Override
    public String getId() {
        return "structured-compare";
    }

    @Override
    public String getName() {
        return "结构化对比策略(Structured Comparison Strategy)";
    }

    @Override
    public String getDescription() {
        return "对多个文档进行结构化对比，生成对比表格和分析报告(Perform structured comparison on multiple documents, generate comparison tables and analysis reports)";
    }

    @Override
    public StrategyCapabilities getCapabilities() {
        return StrategyCapabilities.builder()
                .supportedTypes(Set.of(StrategyCapabilities.AnalysisType.COMPARE))
                .minDocuments(2)
                .maxDocuments(10)
                .optimalDocuments(3)
                .tokenCost(StrategyCapabilities.CostLevel.MEDIUM)
                .speed(StrategyCapabilities.SpeedLevel.MEDIUM)
                .quality(StrategyCapabilities.QualityLevel.EXCELLENT)
                .relationPreserve(StrategyCapabilities.RelationLevel.HIGH)
                .build();
    }

    @Override
    public int evaluateSuitability(AnalysisContext context) {
        int score = 30;

        // 必须至少2个文档（Must have at least 2 documents）
        if (context.getDocumentCount() < 2) {
            return 0;
        }

        // 2-5个文档最佳（2-5 documents optimal）
        if (context.getDocumentCount() >= 2 && context.getDocumentCount() <= 5) {
            score += 40;
        } else {
            score += 20;
        }

        // 问题类型评分（Question type scoring）
        String question = context.getQuestion().toLowerCase();
        if (question.contains("对比") || question.contains("比较") ||
            question.contains("区别") || question.contains("差异") ||
            question.contains("compare") || question.contains("difference") ||
            question.contains("versus") || question.contains("vs")) {
            score += 30;
        }

        return Math.min(100, score);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback) {
        List<AnalysisContext.DocumentContent> documents = context.getDocumentContents();

        if (documents == null || documents.size() < 2) {
            return AnalysisResult.failure("对比分析至少需要2个文档(Comparison analysis requires at least 2 documents)");
        }

        callback.onProgress(20, "准备文档内容(Preparing document contents)...");

        // 准备文档内容（Prepare document contents）
        String formattedContent = formatDocumentsForComparison(documents);

        callback.onProgress(40, "执行对比分析(Executing comparison analysis)...");

        // 调用LLM进行对比分析（Call LLM for comparison analysis）
        String prompt = String.format(COMPARE_PROMPT_TEMPLATE,
                context.getQuestion(), formattedContent);
        String comparisonResult = callLLM(prompt, "");

        callback.onProgress(80, "提取对比结果(Extracting comparison results)...");

        // 解析结果（Parse results）
        AnalysisResult.ComparisonResult comparison = parseComparisonResult(comparisonResult);
        List<String> keyPoints = extractComparisonKeyPoints(comparisonResult);

        return AnalysisResult.builder()
                .success(true)
                .answer(comparisonResult)
                .comprehensiveSummary(comparisonResult)
                .finalReport(comparisonResult)
                .comparison(comparison)
                .keyPoints(keyPoints)
                .metadata(Map.of(
                        "documentCount", documents.size(),
                        "comparisonType", "structured"
                ))
                .build();
    }

    /**
     * 格式化文档用于对比（Format documents for comparison）
     */
    private String formatDocumentsForComparison(List<AnalysisContext.DocumentContent> documents) {
        StringBuilder sb = new StringBuilder();
        int maxLengthPerDoc = 3000;

        for (int i = 0; i < documents.size(); i++) {
            AnalysisContext.DocumentContent doc = documents.get(i);
            sb.append("### 文档(Document)").append(i + 1).append(": ").append(doc.getName()).append("\n\n");

            String content = doc.getContent();
            if (content != null) {
                if (content.length() > maxLengthPerDoc) {
                    content = content.substring(0, maxLengthPerDoc) + "...(内容已截断/content truncated)";
                }
                sb.append(content).append("\n\n");
            }

            sb.append("---\n\n");
        }

        return sb.toString();
    }

    /**
     * 解析对比结果（Parse comparison result）
     */
    private AnalysisResult.ComparisonResult parseComparisonResult(String result) {
        List<String> commonPoints = new ArrayList<>();
        List<String> differences = new ArrayList<>();
        Map<String, List<String>> prosAndCons = new HashMap<>();
        String recommendation = "";

        String[] sections = result.split("##");
        for (String section : sections) {
            section = section.trim();

            if (section.contains("共同点") || section.contains("Common")) {
                commonPoints.addAll(extractListItems(section));
            } else if (section.contains("差异") || section.contains("不同") || section.contains("Difference")) {
                differences.addAll(extractListItems(section));
            } else if (section.contains("结论") || section.contains("建议") || section.contains("Conclusion")) {
                recommendation = section.replaceFirst("^[^\\n]+\\n", "").trim();
            }
        }

        return AnalysisResult.ComparisonResult.builder()
                .commonPoints(commonPoints)
                .differences(differences)
                .prosAndCons(prosAndCons)
                .recommendation(recommendation)
                .build();
    }

    /**
     * 提取列表项（Extract list items）
     */
    private List<String> extractListItems(String text) {
        List<String> items = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•") || line.matches("^[0-9]+[.、].*")) {
                String item = line.replaceFirst("^[-•0-9.、]+\\s*", "").trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * 从对比结果中提取关键点（Extract key points from comparison result）
     */
    private List<String> extractComparisonKeyPoints(String result) {
        List<String> keyPoints = new ArrayList<>();

        // 提取主要差异和共同点作为关键点（Extract main differences and common points as key points）
        String[] lines = result.split("\n");
        for (String line : lines) {
            line = line.trim();
            if ((line.startsWith("-") || line.startsWith("•")) && line.length() < 100) {
                String point = line.replaceFirst("^[-•]\\s*", "").trim();
                if (!point.isEmpty()) {
                    keyPoints.add(point);
                }
            }
        }

        return keyPoints.stream()
                .distinct()
                .limit(8)
                .toList();
    }
}

