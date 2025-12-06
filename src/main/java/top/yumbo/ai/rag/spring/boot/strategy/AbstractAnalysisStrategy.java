package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.service.KnowledgeQAService;

import java.util.List;

/**
 * 抽象策略基类（Abstract Strategy Base Class）
 *
 * <p>提供策略的通用功能实现，包括LLM调用、文档处理等</p>
 * <p>Provides common functionality implementation for strategies, including LLM calls, document processing, etc.</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Slf4j
public abstract class AbstractAnalysisStrategy implements MultiDocAnalysisStrategy {

    @Autowired
    protected KnowledgeQAService knowledgeQAService;

    /**
     * 执行分析的模板方法（Template method for executing analysis）
     */
    @Override
    public AnalysisResult analyze(AnalysisContext context, ProgressCallback progressCallback) {
        long startTime = System.currentTimeMillis();
        ProgressCallback callback = progressCallback != null ? progressCallback : ProgressCallback.empty();

        try {
            log.info(LogMessageProvider.getMessage("strategy.abstract.log.analysis_start",
                    getId(), context.getDocumentCount()));

            callback.onProgress(0, LogMessageProvider.getMessage("strategy.abstract.log.analysis_start", getId(), ""));

            // 执行具体分析（Execute specific analysis）
            AnalysisResult result = doAnalyze(context, callback);

            // 设置元数据（Set metadata）
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            result.setStrategiesUsed(List.of(getId()));

            callback.onProgress(100, LogMessageProvider.getMessage("strategy.abstract.log.analysis_complete", getId(), executionTime));

            log.info(LogMessageProvider.getMessage("strategy.abstract.log.analysis_complete",
                    getId(), executionTime));
            return result;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("strategy.abstract.log.analysis_failed", getId()), e);
            return AnalysisResult.failure(e.getMessage());
        }
    }

    /**
     * 执行具体分析 - 由子类实现（Execute specific analysis - implemented by subclass）
     *
     * @param context 分析上下文（Analysis context）
     * @param callback 进度回调（Progress callback）
     * @return 分析结果（Analysis result）
     */
    protected abstract AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback);

    /**
     * 调用LLM进行分析（Call LLM for analysis）
     *
     * @param prompt 提示词（Prompt）
     * @param context 上下文（Context）
     * @return LLM回答（LLM response）
     */
    protected String callLLM(String prompt, String context) {
        try {
            return knowledgeQAService.askWithContext(prompt, context);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("strategy.abstract.log.llm_call_failed"), e);
            throw new RuntimeException(LogMessageProvider.getMessage("strategy.abstract.log.llm_call_failed") + ": " + e.getMessage(), e);
        }
    }

    /**
     * 合并多个文档内容（Merge multiple document contents）
     *
     * @param contents 文档内容列表（List of document contents）
     * @param maxLength 最大长度（Maximum length）
     * @return 合并后的内容（Merged content）
     */
    protected String mergeDocumentContents(List<AnalysisContext.DocumentContent> contents, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int avgLength = maxLength / Math.max(1, contents.size());

        for (int i = 0; i < contents.size(); i++) {
            AnalysisContext.DocumentContent doc = contents.get(i);
            sb.append("## 文档(Document) ").append(i + 1).append(": ").append(doc.getName()).append("\n\n");

            String content = doc.getContent();
            if (content != null) {
                if (content.length() > avgLength) {
                    content = content.substring(0, avgLength) + "...(内容已截断/content truncated)";
                }
                sb.append(content).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 提取关键词（Extract keywords）
     *
     * @param text 文本（Text）
     * @return 关键词列表（List of keywords）
     */
    protected List<String> extractKeywords(String text) {
        // 简单的关键词提取（Simple keyword extraction）
        String[] words = text.split("[\\s,，.。!！?？;；:：]+");
        return java.util.Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .distinct()
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 预估资源消耗（Estimate resource consumption）
     */
    @Override
    public ResourceEstimate estimateResources(AnalysisContext context) {
        long totalLength = context.getTotalContentLength();
        int docCount = context.getDocumentCount();

        // 基础估算（Basic estimation）
        long estimatedTokens = (totalLength / 4) + 500; // 粗略估算（Rough estimation）
        long estimatedTime = 5000L + ((long) docCount * 3000); // 基础5秒 + 每文档3秒（Base 5s + 3s per doc）

        return ResourceEstimate.builder()
                .estimatedTokens(estimatedTokens)
                .estimatedTimeMs(estimatedTime)
                .confidenceLevel(0.7)
                .build();
    }
}

