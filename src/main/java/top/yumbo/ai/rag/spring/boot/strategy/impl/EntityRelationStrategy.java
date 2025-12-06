package top.yumbo.ai.rag.spring.boot.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.util.*;

/**
 * 实体关联策略
 * (Entity Relation Strategy)
 *
 * 从文档中提取实体，分析实体间的关联关系
 * (Extract entities from documents and analyze relationships between them)
 */
@Slf4j
@Component
public class EntityRelationStrategy extends AbstractAnalysisStrategy {

    private static final String ENTITY_EXTRACT_PROMPT = """
            请从以下文档中提取关键实体和它们之间的关系：
            
            文档：%s
            
            内容：
            %s
            
            请按以下格式输出：
            
            ## 关键实体
            - 实体1: 类型, 描述
            - 实体2: 类型, 描述
            
            ## 实体关系
            - 实体A -> 关系 -> 实体B
            """;

    private static final String RELATION_ANALYSIS_PROMPT = """
            基于以下多个文档的实体和关系信息，进行关联分析：
            
            用户问题：%s
            
            各文档的实体关系：
            %s
            
            请分析：
            1. **跨文档关联**：哪些实体在多个文档中出现？它们的关系如何？
            2. **关系网络**：构建整体的关系图谱描述
            3. **核心实体**：识别最重要的实体及其作用
            4. **关联发现**：发现文档间隐含的关联
            5. **结论**：基于关联分析回答用户问题
            """;

    @Override
    public String getId() {
        return "entity-relation";
    }

    @Override
    public String getName() {
        return "实体关联策略";
    }

    @Override
    public String getDescription() {
        return "提取文档中的实体，分析实体间的关联关系";
    }

    @Override
    public StrategyCapabilities getCapabilities() {
        return StrategyCapabilities.builder()
                .supportedTypes(Set.of(
                        StrategyCapabilities.AnalysisType.RELATION,
                        StrategyCapabilities.AnalysisType.CAUSAL
                ))
                .minDocuments(2)
                .maxDocuments(10)
                .optimalDocuments(4)
                .tokenCost(StrategyCapabilities.CostLevel.MEDIUM)
                .speed(StrategyCapabilities.SpeedLevel.MEDIUM)
                .quality(StrategyCapabilities.QualityLevel.EXCELLENT)
                .relationPreserve(StrategyCapabilities.RelationLevel.BEST)
                .build();
    }

    @Override
    public int evaluateSuitability(AnalysisContext context) {
        int score = 30;

        // 至少2个文档
        if (context.getDocumentCount() < 2) {
            return 0;
        }

        // 2-6个文档最佳
        if (context.getDocumentCount() >= 2 && context.getDocumentCount() <= 6) {
            score += 30;
        } else {
            score += 15;
        }

        // 问题类型评分
        String question = context.getQuestion().toLowerCase();
        if (question.contains("关系") || question.contains("关联") ||
            question.contains("联系") || question.contains("relationship") ||
            question.contains("connection") || question.contains("relate")) {
            score += 40;
        }

        return Math.min(100, score);
    }

    @Override
    protected AnalysisResult doAnalyze(AnalysisContext context, ProgressCallback callback) {
        List<AnalysisContext.DocumentContent> documents = context.getDocumentContents();

        if (documents == null || documents.size() < 2) {
            return AnalysisResult.failure("关联分析至少需要2个文档");
        }

        callback.onProgress(10, "提取文档实体...");

        // 阶段1：从每个文档提取实体和关系
        Map<String, String> documentEntities = new LinkedHashMap<>();
        for (int i = 0; i < documents.size(); i++) {
            AnalysisContext.DocumentContent doc = documents.get(i);
            callback.onProgress(10 + (i + 1) * 20 / documents.size(),
                    "提取实体: " + doc.getName());

            String entities = extractEntities(doc);
            documentEntities.put(doc.getName(), entities);
        }

        callback.onProgress(50, "分析实体关联...");

        // 阶段2：综合分析关联关系
        String formattedEntities = formatEntities(documentEntities);
        String relationAnalysis = analyzeRelations(context.getQuestion(), formattedEntities);

        callback.onProgress(80, "构建关系图谱...");

        // 解析关系
        List<AnalysisResult.DocumentRelation> relations = parseRelations(relationAnalysis);
        List<String> keyPoints = extractKeyPoints(relationAnalysis);

        return AnalysisResult.builder()
                .success(true)
                .answer(relationAnalysis)
                .comprehensiveSummary(relationAnalysis)
                .finalReport(relationAnalysis)
                .relations(relations)
                .keyPoints(keyPoints)
                .metadata(Map.of(
                        "documentCount", documents.size(),
                        "entityCount", documentEntities.size()
                ))
                .build();
    }

    /**
     * 从单个文档提取实体
     */
    private String extractEntities(AnalysisContext.DocumentContent doc) {
        String content = doc.getContent();
        if (content == null || content.trim().isEmpty()) {
            return "无内容";
        }

        // 限制内容长度
        if (content.length() > 3000) {
            content = content.substring(0, 3000) + "...";
        }

        String prompt = String.format(ENTITY_EXTRACT_PROMPT, doc.getName(), content);
        return callLLM(prompt, "");
    }

    /**
     * 格式化实体信息
     */
    private String formatEntities(Map<String, String> documentEntities) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : documentEntities.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 分析关联关系
     */
    private String analyzeRelations(String question, String formattedEntities) {
        String prompt = String.format(RELATION_ANALYSIS_PROMPT, question, formattedEntities);
        return callLLM(prompt, "");
    }

    /**
     * 解析关系
     */
    private List<AnalysisResult.DocumentRelation> parseRelations(String analysis) {
        List<AnalysisResult.DocumentRelation> relations = new ArrayList<>();

        // 简单解析：查找 "A -> 关系 -> B" 模式
        String[] lines = analysis.split("\n");
        for (String line : lines) {
            if (line.contains("->") && line.split("->").length >= 3) {
                String[] parts = line.split("->");
                if (parts.length >= 3) {
                    relations.add(AnalysisResult.DocumentRelation.builder()
                            .doc1(parts[0].trim())
                            .relationType(parts[1].trim())
                            .doc2(parts[2].trim())
                            .confidence(0.8)
                            .build());
                }
            }
        }

        return relations;
    }

    /**
     * 提取关键点
     */
    private List<String> extractKeyPoints(String analysis) {
        List<String> points = new ArrayList<>();
        String[] lines = analysis.split("\n");

        for (String line : lines) {
            line = line.trim();
            if ((line.startsWith("-") || line.startsWith("•") || line.matches("^[0-9]+[.、].*"))
                    && line.length() < 100) {
                String point = line.replaceFirst("^[-•0-9.、]+\\s*", "").trim();
                if (!point.isEmpty()) {
                    points.add(point);
                }
            }
        }

        return points.stream().distinct().limit(8).toList();
    }
}

