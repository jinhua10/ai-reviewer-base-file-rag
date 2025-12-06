package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import top.yumbo.ai.rag.spring.boot.service.KnowledgeQAService;

import java.util.List;

/**
 * 抽象策略基类
 * (Abstract Strategy Base Class)
 *
 * 提供通用功能实现
 * (Provides common functionality implementation)
 */
@Slf4j
public abstract class AbstractAnalysisStrategy implements MultiDocAnalysisStrategy {

    @Autowired
    protected KnowledgeQAService knowledgeQAService;

    @Override
    public AnalysisResult analyze(AnalysisContext context, ProgressCallback progressCallback) {
        long startTime = System.currentTimeMillis();
        ProgressCallback callback = progressCallback != null ? progressCallback : ProgressCallback.empty();

        try {
            log.info("🚀 Starting analysis with strategy: {} for {} documents",
                    getId(), context.getDocumentCount());

            callback.onProgress(0, "开始分析 / Starting analysis");

            // 执行具体分析
            AnalysisResult result = doAnalyze(context, callback);

            // 设置元数据
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(executionTime);
            result.setStrategiesUsed(List.of(getId()));

            callback.onProgress(100, "分析完成 / Analysis completed");

            log.info("✅ Analysis completed with strategy: {} in {}ms", getId(), executionTime);
            return result;

        } catch (Exception e) {
            log.error("❌ Analysis failed with strategy: {}", getId(), e);
            return AnalysisResult.failure(e.getMessage());
        }
    }

    /**
     * 执行具体分析（子类实现）
     * (Execute specific analysis - implemented by subclass)
     */
    protected abstract AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback);

    /**
     * 调用LLM进行分析
     * (Call LLM for analysis)
     */
    protected String callLLM(String prompt, String context) {
        try {
            return knowledgeQAService.askWithContext(prompt, context);
        } catch (Exception e) {
            log.error("LLM call failed", e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 合并多个文档内容
     * (Merge multiple document contents)
     */
    protected String mergeDocumentContents(List<AnalysisContext.DocumentContent> contents, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int avgLength = maxLength / Math.max(1, contents.size());

        for (int i = 0; i < contents.size(); i++) {
            AnalysisContext.DocumentContent doc = contents.get(i);
            sb.append("## 文档 ").append(i + 1).append(": ").append(doc.getName()).append("\n\n");

            String content = doc.getContent();
            if (content != null) {
                if (content.length() > avgLength) {
                    content = content.substring(0, avgLength) + "...(内容已截断)";
                }
                sb.append(content).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 提取关键词
     * (Extract keywords)
     */
    protected List<String> extractKeywords(String text) {
        // 简单的关键词提取
        String[] words = text.split("[\\s,，.。!！?？;；:：]+");
        return java.util.Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .distinct()
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public ResourceEstimate estimateResources(AnalysisContext context) {
        long totalLength = context.getTotalContentLength();
        int docCount = context.getDocumentCount();

        // 基础估算
        long estimatedTokens = (totalLength / 4) + 500; // 粗略估算
        long estimatedTime = 5000 + (docCount * 3000); // 基础5秒 + 每文档3秒

        return ResourceEstimate.builder()
                .estimatedTokens(estimatedTokens)
                .estimatedTimeMs(estimatedTime)
                .confidenceLevel(0.7)
                .build();
    }
}

