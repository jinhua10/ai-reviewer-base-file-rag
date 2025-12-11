package top.yumbo.ai.rag.conflict;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冲突检测器 (Conflict Detector)
 *
 * 检测知识库中的概念冲突
 * (Detects conflicts in knowledge base)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class ConflictDetector {

    @Autowired
    private SimilarityCalculator similarityCalculator;

    /**
     * 冲突案例存储 (Conflict case storage)
     */
    private final Map<String, ConflictCase> conflictStorage = new ConcurrentHashMap<>();

    /**
     * 最小冲突分数阈值 (Minimum conflict score threshold)
     */
    private static final double MIN_CONFLICT_SCORE = 0.6;

    /**
     * 检测冲突 (Detect conflict)
     *
     * @param doc1 文档1 (Document 1)
     * @param doc2 文档2 (Document 2)
     * @param vector1 向量1 (Vector 1)
     * @param vector2 向量2 (Vector 2)
     * @return 冲突案例，如果无冲突返回null (Conflict case, null if no conflict)
     */
    public ConflictCase detectConflict(Document doc1, Document doc2,
                                      float[] vector1, float[] vector2) {
        if (doc1 == null || doc2 == null) {
            return null;
        }

        log.info(I18N.get("conflict.detect.start", doc1.getId(), doc2.getId()));
        long startTime = System.currentTimeMillis();

        try {
            // 1. 计算相似度 (Calculate similarity)
            double semanticSim = similarityCalculator.calculateSemanticSimilarity(vector1, vector2);
            double keywordSim = similarityCalculator.calculateKeywordSimilarity(
                    doc1.getContent(), doc2.getContent());
            double structureSim = similarityCalculator.calculateStructureSimilarity(doc1, doc2);

            // 2. 综合评分 (Combine scores)
            double similarityScore = similarityCalculator.combineScores(
                    semanticSim, keywordSim, structureSim);

            // 3. 判断是否相似 (Check if similar)
            if (!similarityCalculator.isSimilar(similarityScore)) {
                log.debug(I18N.get("conflict.detect.not_similar", similarityScore));
                return null;
            }

            // 4. 分析差异 (Analyze difference)
            double differenceScore = analyzeDifference(doc1, doc2);

            // 5. 计算置信度 (Calculate confidence)
            double confidence = calculateConfidence(similarityScore, differenceScore);

            // 6. 构建冲突评分 (Build conflict score)
            ConflictScore score = ConflictScore.builder()
                    .similarityScore(similarityScore)
                    .differenceScore(differenceScore)
                    .confidenceScore(confidence)
                    .build();
            score.calculateFinalScore();

            // 7. 判断是否冲突 (Check if conflict)
            if (score.getFinalScore() < MIN_CONFLICT_SCORE) {
                log.debug(I18N.get("conflict.detect.score_too_low", score.getFinalScore()));
                return null;
            }

            // 8. 创建冲突案例 (Create conflict case)
            ConflictCase conflict = ConflictCase.builder()
                    .conflictId(UUID.randomUUID().toString())
                    .conflictType(determineConflictType(doc1, doc2, differenceScore))
                    .score(score)
                    .severity(score.getFinalScore())
                    .description(generateDescription(doc1, doc2, score))
                    .build();

            conflict.addConceptId(doc1.getId());
            conflict.addConceptId(doc2.getId());

            // 9. 存储冲突 (Store conflict)
            conflictStorage.put(conflict.getConflictId(), conflict);

            long duration = System.currentTimeMillis() - startTime;
            log.info(I18N.get("conflict.detect.found",
                    conflict.getConflictId(), conflict.getConflictType(), duration));

            return conflict;

        } catch (Exception e) {
            log.error(I18N.get("conflict.detect.failed",
                    doc1.getId(), doc2.getId(), e.getMessage()), e);
            return null;
        }
    }

    /**
     * 批量扫描冲突 (Scan for conflicts)
     *
     * @param documents 文档列表 (Document list)
     * @param vectors 向量列表 (Vector list)
     * @return 检测到的冲突列表 (Detected conflicts)
     */
    public List<ConflictCase> scanForConflicts(List<Document> documents, List<float[]> vectors) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        log.info(I18N.get("conflict.scan.start", documents.size()));
        List<ConflictCase> conflicts = new ArrayList<>();

        // 两两比较 (Pairwise comparison)
        for (int i = 0; i < documents.size(); i++) {
            for (int j = i + 1; j < documents.size(); j++) {
                ConflictCase conflict = detectConflict(
                        documents.get(i), documents.get(j),
                        vectors != null && vectors.size() > i ? vectors.get(i) : null,
                        vectors != null && vectors.size() > j ? vectors.get(j) : null
                );

                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        log.info(I18N.get("conflict.scan.complete", conflicts.size(), documents.size()));
        return conflicts;
    }

    /**
     * 获取所有冲突 (Get all conflicts)
     *
     * @return 冲突列表 (Conflict list)
     */
    public List<ConflictCase> getAllConflicts() {
        return new ArrayList<>(conflictStorage.values());
    }

    /**
     * 获取待处理冲突 (Get pending conflicts)
     *
     * @return 待处理冲突列表 (Pending conflict list)
     */
    public List<ConflictCase> getPendingConflicts() {
        return conflictStorage.values().stream()
                .filter(ConflictCase::isPending)
                .toList();
    }

    /**
     * 解决冲突 (Resolve conflict)
     *
     * @param conflictId 冲突ID (Conflict ID)
     * @param resolution 解决方案 (Resolution)
     */
    public void resolveConflict(String conflictId, String resolution) {
        ConflictCase conflict = conflictStorage.get(conflictId);
        if (conflict != null) {
            conflict.setStatus(ConflictCase.ConflictStatus.RESOLVED);
            conflict.setResolution(resolution);
            conflict.setResolvedTime(new Date());
            log.info(I18N.get("conflict.resolved", conflictId));
        }
    }

    /**
     * 分析差异 (Analyze difference)
     *
     * @param doc1 文档1 (Document 1)
     * @param doc2 文档2 (Document 2)
     * @return 差异分数 (Difference score)
     */
    private double analyzeDifference(Document doc1, Document doc2) {
        // 简化实现：基于内容长度差异
        String content1 = doc1.getContent() != null ? doc1.getContent() : "";
        String content2 = doc2.getContent() != null ? doc2.getContent() : "";

        int len1 = content1.length();
        int len2 = content2.length();

        if (len1 == 0 || len2 == 0) {
            return 1.0; // 完全不同
        }

        // 计算编辑距离的近似值
        double diff = Math.abs(len1 - len2) / (double) Math.max(len1, len2);
        return Math.min(1.0, diff * 2); // 放大差异
    }

    /**
     * 计算置信度 (Calculate confidence)
     *
     * @param similarity 相似度 (Similarity)
     * @param difference 差异度 (Difference)
     * @return 置信度 (Confidence)
     */
    private double calculateConfidence(double similarity, double difference) {
        // 相似度高且差异明显 = 高置信度
        return (similarity + difference) / 2.0;
    }

    /**
     * 确定冲突类型 (Determine conflict type)
     *
     * @param doc1 文档1 (Document 1)
     * @param doc2 文档2 (Document 2)
     * @param differenceScore 差异分数 (Difference score)
     * @return 冲突类型 (Conflict type)
     */
    private ConflictType determineConflictType(Document doc1, Document doc2, double differenceScore) {
        // 简化实现：基于差异程度
        if (differenceScore >= 0.8) {
            return ConflictType.FACTUAL;
        } else if (differenceScore >= 0.6) {
            return ConflictType.SEMANTIC;
        } else if (differenceScore >= 0.4) {
            return ConflictType.TEMPORAL;
        } else {
            return ConflictType.SCOPE;
        }
    }

    /**
     * 生成描述 (Generate description)
     *
     * @param doc1 文档1 (Document 1)
     * @param doc2 文档2 (Document 2)
     * @param score 评分 (Score)
     * @return 描述 (Description)
     */
    private String generateDescription(Document doc1, Document doc2, ConflictScore score) {
        return String.format("检测到文档 %s 和 %s 之间存在冲突，相似度: %.2f，差异度: %.2f",
                doc1.getId(), doc2.getId(), score.getSimilarityScore(), score.getDifferenceScore());
    }

    /**
     * 清空所有冲突 (Clear all conflicts)
     * 仅用于测试 (For testing only)
     */
    public void clearAll() {
        conflictStorage.clear();
        log.info(I18N.get("conflict.cleared"));
    }
}

