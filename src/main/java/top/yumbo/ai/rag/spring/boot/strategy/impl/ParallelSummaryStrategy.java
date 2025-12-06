package top.yumbo.ai.rag.spring.boot.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 并行摘要策略（Parallel Summary Strategy）
 *
 * <p>并行处理所有文档生成独立摘要，然后合并生成综合报告</p>
 * <p>Process all documents in parallel to generate independent summaries, then merge into comprehensive report</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Slf4j
@Component
public class ParallelSummaryStrategy extends AbstractAnalysisStrategy {

    /** 单文档摘要提示词模板（Single document summary prompt template） */
    private static final String SUMMARY_PROMPT_TEMPLATE = """
            请对以下文档内容进行摘要，提取核心观点和关键信息：
            (Please summarize the following document content, extracting core viewpoints and key information:)
            
            文档名(Document name)：%s
            
            内容(Content)：
            %s
            
            请提供(Please provide)：
            1. 核心主题（一句话概括）(Core theme - one sentence summary)
            2. 关键要点（3-5个）(Key points - 3-5 items)
            3. 重要数据或结论(Important data or conclusions)
            """;

    /** 合并摘要提示词模板（Merge summaries prompt template） */
    private static final String MERGE_PROMPT_TEMPLATE = """
            以下是多个文档的摘要，请综合分析并生成最终报告：
            (Below are summaries of multiple documents, please analyze comprehensively and generate final report:)
            
            用户问题(User question)：%s
            
            各文档摘要(Document summaries)：
            %s
            
            请生成一份综合分析报告，包括(Please generate a comprehensive analysis report, including)：
            1. 总体概述(Overall overview)
            2. 各文档的核心观点(Core viewpoints of each document)
            3. 共同点和差异(Common points and differences)
            4. 综合结论(Comprehensive conclusion)
            """;

    @Override
    public String getId() {
        return "parallel-summary";
    }

    @Override
    public String getName() {
        return "并行摘要策略(Parallel Summary Strategy)";
    }

    @Override
    public String getDescription() {
        return "并行处理多个文档，生成各自摘要后综合分析(Process multiple documents in parallel, generate summaries and then analyze comprehensively)";
    }

    @Override
    public StrategyCapabilities getCapabilities() {
        return StrategyCapabilities.builder()
                .supportedTypes(Set.of(
                        StrategyCapabilities.AnalysisType.SUMMARY,
                        StrategyCapabilities.AnalysisType.COMPREHENSIVE
                ))
                .minDocuments(1)
                .maxDocuments(20)
                .optimalDocuments(5)
                .tokenCost(StrategyCapabilities.CostLevel.MEDIUM)
                .speed(StrategyCapabilities.SpeedLevel.FAST)
                .quality(StrategyCapabilities.QualityLevel.GOOD)
                .relationPreserve(StrategyCapabilities.RelationLevel.MEDIUM)
                .build();
    }

    @Override
    public int evaluateSuitability(AnalysisContext context) {
        int score = 50;

        // 文档数量评分（Document count scoring）
        int docCount = context.getDocumentCount();
        if (docCount >= 2 && docCount <= 10) {
            score += 30;
        } else if (docCount > 10) {
            score += 20;
        }

        // 问题类型评分（Question type scoring）
        String question = context.getQuestion().toLowerCase();
        if (question.contains("总结") || question.contains("概括") ||
            question.contains("summary") || question.contains("summarize")) {
            score += 20;
        }

        return Math.min(100, score);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback) {
        List<AnalysisContext.DocumentContent> documents = context.getDocumentContents();

        if (documents == null || documents.isEmpty()) {
            return AnalysisResult.failure("没有可分析的文档内容(No document content to analyze)");
        }

        // 阶段1：并行生成各文档摘要（Phase 1: Generate summaries in parallel）
        callback.onProgress(10, "并行生成文档摘要(Generating document summaries in parallel)...");
        Map<String, String> summaries = generateSummariesParallel(documents, callback);

        if (summaries.isEmpty()) {
            return AnalysisResult.failure("摘要生成失败(Summary generation failed)");
        }

        // 阶段2：合并摘要生成综合报告（Phase 2: Merge summaries into comprehensive report）
        callback.onProgress(70, "综合分析生成报告(Generating comprehensive report)...");
        String mergedSummaries = formatSummaries(summaries);
        String finalReport = generateFinalReport(context.getQuestion(), mergedSummaries);

        // 提取关键点（Extract key points）
        List<String> keyPoints = extractKeyPointsFromSummaries(summaries);

        return AnalysisResult.builder()
                .success(true)
                .answer(finalReport)
                .comprehensiveSummary(finalReport)
                .finalReport(finalReport)
                .keyPoints(keyPoints)
                .metadata(Map.of(
                        "documentCount", documents.size(),
                        "summaryCount", summaries.size()
                ))
                .build();
    }

    /**
     * 并行生成文档摘要（Generate document summaries in parallel）
     */
    private Map<String, String> generateSummariesParallel(
            List<AnalysisContext.DocumentContent> documents,
            ProgressCallback callback) {

        Map<String, String> summaries = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(documents.size(), 4)
        );

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int total = documents.size();

            for (int i = 0; i < documents.size(); i++) {
                final int index = i;
                final AnalysisContext.DocumentContent doc = documents.get(i);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String summary = generateSingleSummary(doc);
                        summaries.put(doc.getName(), summary);

                        int progress = 10 + (int) ((index + 1.0) / total * 50);
                        callback.onProgress(progress,
                                String.format("已完成(Completed) %d/%d 文档摘要(document summaries)", index + 1, total));

                    } catch (Exception e) {
                        log.error("生成文档摘要失败(Failed to generate document summary): {}", doc.getName(), e);
                        summaries.put(doc.getName(), "摘要生成失败(Summary generation failed): " + e.getMessage());
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有任务完成（Wait for all tasks to complete）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("并行摘要生成失败(Parallel summary generation failed)", e);
        } finally {
            executor.shutdown();
        }

        return summaries;
    }

    /**
     * 生成单个文档的摘要（Generate summary for single document）
     */
    private String generateSingleSummary(AnalysisContext.DocumentContent doc) {
        String content = doc.getContent();
        if (content == null || content.trim().isEmpty()) {
            return "文档内容为空(Document content is empty)";
        }

        // 限制内容长度（Limit content length）
        if (content.length() > 4000) {
            content = content.substring(0, 4000) + "...(内容已截断/content truncated)";
        }

        String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, doc.getName(), content);
        return callLLM(prompt, "");
    }

    /**
     * 格式化摘要列表（Format summaries list）
     */
    private String formatSummaries(Map<String, String> summaries) {
        StringBuilder sb = new StringBuilder();
        int index = 1;

        for (Map.Entry<String, String> entry : summaries.entrySet()) {
            sb.append("### 文档(Document)").append(index).append(": ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
            index++;
        }

        return sb.toString();
    }

    /**
     * 生成最终报告（Generate final report）
     */
    private String generateFinalReport(String question, String mergedSummaries) {
        String prompt = String.format(MERGE_PROMPT_TEMPLATE, question, mergedSummaries);
        return callLLM(prompt, "");
    }

    /**
     * 从摘要中提取关键点（Extract key points from summaries）
     */
    private List<String> extractKeyPointsFromSummaries(Map<String, String> summaries) {
        List<String> keyPoints = new ArrayList<>();

        for (Map.Entry<String, String> entry : summaries.entrySet()) {
            String summary = entry.getValue();
            // 简单提取：查找带有序号或标记的行（Simple extraction: find lines with numbers or markers）
            String[] lines = summary.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.matches("^[0-9]+[.、].*") || line.startsWith("-") || line.startsWith("•")) {
                    String point = line.replaceFirst("^[0-9]+[.、\\-•]\\s*", "").trim();
                    if (!point.isEmpty() && point.length() < 100) {
                        keyPoints.add(point);
                    }
                }
            }
        }

        // 去重并限制数量（Deduplicate and limit count）
        return keyPoints.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
}

