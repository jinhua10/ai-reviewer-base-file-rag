package top.yumbo.ai.rag.spring.boot.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 并行摘要策略
 * (Parallel Summary Strategy)
 *
 * 并行处理所有文档生成独立摘要，然后合并生成综合报告
 * (Process all documents in parallel to generate independent summaries, then merge into comprehensive report)
 */
@Slf4j
@Component
public class ParallelSummaryStrategy extends AbstractAnalysisStrategy {

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            请对以下文档内容进行摘要，提取核心观点和关键信息：
            
            文档名：%s
            
            内容：
            %s
            
            请提供：
            1. 核心主题（一句话概括）
            2. 关键要点（3-5个）
            3. 重要数据或结论
            """;

    private static final String MERGE_PROMPT_TEMPLATE = """
            以下是多个文档的摘要，请综合分析并生成最终报告：
            
            用户问题：%s
            
            各文档摘要：
            %s
            
            请生成一份综合分析报告，包括：
            1. 总体概述
            2. 各文档的核心观点
            3. 共同点和差异
            4. 综合结论
            """;

    @Override
    public String getId() {
        return "parallel-summary";
    }

    @Override
    public String getName() {
        return "并行摘要策略";
    }

    @Override
    public String getDescription() {
        return "并行处理多个文档，生成各自摘要后综合分析";
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

        // 文档数量评分
        int docCount = context.getDocumentCount();
        if (docCount >= 2 && docCount <= 10) {
            score += 30;
        } else if (docCount > 10) {
            score += 20;
        }

        // 问题类型评分
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
            return AnalysisResult.failure("没有可分析的文档内容");
        }

        // 阶段1：并行生成各文档摘要
        callback.onProgress(10, "并行生成文档摘要...");
        Map<String, String> summaries = generateSummariesParallel(documents, callback);

        if (summaries.isEmpty()) {
            return AnalysisResult.failure("摘要生成失败");
        }

        // 阶段2：合并摘要生成综合报告
        callback.onProgress(70, "综合分析生成报告...");
        String mergedSummaries = formatSummaries(summaries);
        String finalReport = generateFinalReport(context.getQuestion(), mergedSummaries);

        // 提取关键点
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
     * 并行生成文档摘要
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
                                String.format("已完成 %d/%d 文档摘要", index + 1, total));

                    } catch (Exception e) {
                        log.error("生成文档摘要失败: {}", doc.getName(), e);
                        summaries.put(doc.getName(), "摘要生成失败: " + e.getMessage());
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("并行摘要生成失败", e);
        } finally {
            executor.shutdown();
        }

        return summaries;
    }

    /**
     * 生成单个文档的摘要
     */
    private String generateSingleSummary(AnalysisContext.DocumentContent doc) {
        String content = doc.getContent();
        if (content == null || content.trim().isEmpty()) {
            return "文档内容为空";
        }

        // 限制内容长度
        if (content.length() > 4000) {
            content = content.substring(0, 4000) + "...(内容已截断)";
        }

        String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, doc.getName(), content);
        return callLLM(prompt, "");
    }

    /**
     * 格式化摘要列表
     */
    private String formatSummaries(Map<String, String> summaries) {
        StringBuilder sb = new StringBuilder();
        int index = 1;

        for (Map.Entry<String, String> entry : summaries.entrySet()) {
            sb.append("### 文档").append(index).append(": ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
            index++;
        }

        return sb.toString();
    }

    /**
     * 生成最终报告
     */
    private String generateFinalReport(String question, String mergedSummaries) {
        String prompt = String.format(MERGE_PROMPT_TEMPLATE, question, mergedSummaries);
        return callLLM(prompt, "");
    }

    /**
     * 从摘要中提取关键点
     */
    private List<String> extractKeyPointsFromSummaries(Map<String, String> summaries) {
        List<String> keyPoints = new ArrayList<>();

        for (Map.Entry<String, String> entry : summaries.entrySet()) {
            String summary = entry.getValue();
            // 简单提取：查找带有序号或标记的行
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

        // 去重并限制数量
        return keyPoints.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
}

