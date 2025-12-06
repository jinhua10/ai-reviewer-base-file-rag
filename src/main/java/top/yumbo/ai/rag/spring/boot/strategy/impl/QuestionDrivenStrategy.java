package top.yumbo.ai.rag.spring.boot.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.util.*;

/**
 * 问题导向检索策略
 * (Question-Driven Retrieval Strategy)
 *
 * 基于用户问题，从文档中检索最相关的内容进行回答
 * Token消耗最低的策略
 * (Based on user question, retrieve most relevant content from documents)
 */
@Slf4j
@Component
public class QuestionDrivenStrategy extends AbstractAnalysisStrategy {

    private static final String RETRIEVAL_PROMPT = """
            基于以下文档内容，回答用户的问题。
            
            用户问题：%s
            
            相关文档内容：
            %s
            
            请：
            1. 直接回答问题
            2. 引用具体的文档来源
            3. 如果文档中没有相关信息，明确说明
            """;

    @Override
    public String getId() {
        return "question-driven";
    }

    @Override
    public String getName() {
        return "问题导向检索策略";
    }

    @Override
    public String getDescription() {
        return "基于问题检索最相关内容，Token消耗最低";
    }

    @Override
    public StrategyCapabilities getCapabilities() {
        return StrategyCapabilities.builder()
                .supportedTypes(Set.of(StrategyCapabilities.AnalysisType.QUERY))
                .minDocuments(1)
                .maxDocuments(100)
                .optimalDocuments(10)
                .tokenCost(StrategyCapabilities.CostLevel.LOWEST)
                .speed(StrategyCapabilities.SpeedLevel.FASTEST)
                .quality(StrategyCapabilities.QualityLevel.GOOD)
                .relationPreserve(StrategyCapabilities.RelationLevel.LOW)
                .build();
    }

    @Override
    public int evaluateSuitability(AnalysisContext context) {
        int score = 40;

        // 问题类型评分 - 精确查询最适合
        String question = context.getQuestion().toLowerCase();
        if (question.contains("什么") || question.contains("哪个") ||
            question.contains("多少") || question.contains("是否") ||
            question.contains("what") || question.contains("which") ||
            question.contains("how many") || question.contains("is there")) {
            score += 40;
        }

        // 文档数量越多越适合（节省Token）
        if (context.getDocumentCount() > 5) {
            score += 20;
        }

        return Math.min(100, score);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback) {
        List<AnalysisContext.DocumentContent> documents = context.getDocumentContents();
        String question = context.getQuestion();

        if (documents == null || documents.isEmpty()) {
            return AnalysisResult.failure("没有可分析的文档");
        }

        callback.onProgress(20, "检索相关内容...");

        // 从所有文档中检索与问题最相关的内容
        String relevantContent = retrieveRelevantContent(documents, question);

        callback.onProgress(60, "生成回答...");

        // 基于相关内容回答问题
        String prompt = String.format(RETRIEVAL_PROMPT, question, relevantContent);
        String answer = callLLM(prompt, "");

        callback.onProgress(90, "整理结果...");

        return AnalysisResult.builder()
                .success(true)
                .answer(answer)
                .comprehensiveSummary(answer)
                .metadata(Map.of(
                        "documentCount", documents.size(),
                        "retrievalMethod", "keyword-based"
                ))
                .build();
    }

    /**
     * 检索相关内容
     */
    private String retrieveRelevantContent(
            List<AnalysisContext.DocumentContent> documents,
            String question) {

        // 提取问题关键词
        List<String> keywords = extractKeywords(question);

        StringBuilder relevantContent = new StringBuilder();
        int totalLength = 0;
        int maxLength = 6000; // 限制总长度

        for (AnalysisContext.DocumentContent doc : documents) {
            String content = doc.getContent();
            if (content == null) continue;

            // 分段
            String[] paragraphs = content.split("\n\n+");

            for (String paragraph : paragraphs) {
                // 计算相关性分数
                int relevanceScore = calculateRelevance(paragraph, keywords);

                if (relevanceScore > 0) {
                    String segment = formatSegment(doc.getName(), paragraph);

                    if (totalLength + segment.length() > maxLength) {
                        break;
                    }

                    relevantContent.append(segment).append("\n\n");
                    totalLength += segment.length();
                }
            }

            if (totalLength > maxLength) {
                break;
            }
        }

        if (relevantContent.length() == 0) {
            // 如果没找到相关内容，返回每个文档的开头部分
            return getFallbackContent(documents, maxLength);
        }

        return relevantContent.toString();
    }

    /**
     * 计算段落与关键词的相关性
     */
    private int calculateRelevance(String paragraph, List<String> keywords) {
        String lowerParagraph = paragraph.toLowerCase();
        int score = 0;

        for (String keyword : keywords) {
            if (lowerParagraph.contains(keyword.toLowerCase())) {
                score++;
            }
        }

        return score;
    }

    /**
     * 格式化内容段
     */
    private String formatSegment(String docName, String content) {
        if (content.length() > 500) {
            content = content.substring(0, 500) + "...";
        }
        return String.format("【来源: %s】\n%s", docName, content);
    }

    /**
     * 获取回退内容
     */
    private String getFallbackContent(List<AnalysisContext.DocumentContent> documents, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int lengthPerDoc = maxLength / documents.size();

        for (AnalysisContext.DocumentContent doc : documents) {
            String content = doc.getContent();
            if (content != null) {
                if (content.length() > lengthPerDoc) {
                    content = content.substring(0, lengthPerDoc) + "...";
                }
                sb.append("【").append(doc.getName()).append("】\n");
                sb.append(content).append("\n\n");
            }
        }

        return sb.toString();
    }

    @Override
    public ResourceEstimate estimateResources(AnalysisContext context) {
        // 问题导向策略Token消耗最低
        return ResourceEstimate.builder()
                .estimatedTokens(2000)
                .estimatedTimeMs(3000)
                .confidenceLevel(0.9)
                .build();
    }
}

