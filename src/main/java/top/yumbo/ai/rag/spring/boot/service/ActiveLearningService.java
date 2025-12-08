package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.DocumentWeightService;
import top.yumbo.ai.rag.model.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主动学习服务
 *
 * 系统主动推荐可能相关的文档，让用户确认/否认
 * 通过主动学习加速模型收敛
 *
 * 📈 优化说明（2025-12-05）：
 * 主动学习可减少 2-3 次反馈交互
 * 详见: md/20251205140000-RAG系统收敛性分析.md
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Service
public class ActiveLearningService {

    private final DocumentWeightService documentWeightService;

    /** 不确定性阈值：分数在此范围内的文档需要用户确认 */
    private static final double UNCERTAINTY_LOW = 0.3;
    private static final double UNCERTAINTY_HIGH = 0.7;

    /** 最大推荐数量 */
    private static final int MAX_RECOMMENDATIONS = 5;

    /** 历史查询缓存（用于发现相关文档） */
    private final Map<String, List<QueryHistory>> queryHistoryCache = new LinkedHashMap<String, List<QueryHistory>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<QueryHistory>> eldest) {
            return size() > 1000; // 最多缓存1000个查询
        }
    };

    @Autowired
    public ActiveLearningService(@Autowired(required = false) DocumentWeightService documentWeightService) {
        this.documentWeightService = documentWeightService;
    }

    /**
     * 获取主动学习推荐
     *
     * @param question 用户问题
     * @param retrievedDocs 已检索到的文档
     * @param topKUsed 已使用的文档数量
     * @return 推荐结果
     */
    public ActiveLearningRecommendation getRecommendations(String question,
            List<Document> retrievedDocs, int topKUsed) {

        ActiveLearningRecommendation recommendation = new ActiveLearningRecommendation();
        recommendation.setQuestion(question);

        // 1. 找出不确定性高的文档（边界文档）
        List<UncertainDocument> uncertainDocs = findUncertainDocuments(retrievedDocs, topKUsed);
        recommendation.setUncertainDocuments(uncertainDocs);

        // 2. 找出可能遗漏的相关文档
        List<PotentiallyRelevantDocument> potentialDocs = findPotentiallyRelevantDocuments(
                question, retrievedDocs);
        recommendation.setPotentiallyRelevantDocuments(potentialDocs);

        // 3. 基于历史反馈推荐
        List<HistoryBasedRecommendation> historyRecs = getHistoryBasedRecommendations(question);
        recommendation.setHistoryBasedRecommendations(historyRecs);

        // 4. 计算推荐置信度
        recommendation.setConfidenceScore(calculateConfidence(retrievedDocs, topKUsed));

        // 5. 生成推荐理由
        recommendation.setRecommendationReason(generateRecommendationReason(recommendation));

        log.debug(I18N.get("active_learning.log.recommendation_generated",
                uncertainDocs.size(), potentialDocs.size(), historyRecs.size()));

        return recommendation;
    }

    /**
     * 找出不确定性高的文档（需要用户确认）
     */
    private List<UncertainDocument> findUncertainDocuments(List<Document> docs, int topKUsed) {
        List<UncertainDocument> uncertainDocs = new ArrayList<>();

        if (docs == null || docs.isEmpty()) {
            return uncertainDocs;
        }

        // 边界文档：排名在 topK 附近但未被使用的文档
        int boundaryStart = Math.max(0, topKUsed - 2);
        int boundaryEnd = Math.min(docs.size(), topKUsed + 5);

        for (int i = boundaryStart; i < boundaryEnd; i++) {
            if (i >= topKUsed) { // 只推荐未使用的文档
                Document doc = docs.get(i);
                double uncertaintyScore = calculateUncertaintyScore(doc, i, docs.size());

                if (uncertaintyScore >= UNCERTAINTY_LOW && uncertaintyScore <= UNCERTAINTY_HIGH) {
                    UncertainDocument uncertainDoc = new UncertainDocument();
                    uncertainDoc.setDocument(doc);
                    uncertainDoc.setRank(i + 1);
                    uncertainDoc.setUncertaintyScore(uncertaintyScore);
                    uncertainDoc.setReason(generateUncertaintyReason(doc, i, topKUsed));
                    uncertainDocs.add(uncertainDoc);
                }
            }
        }

        // 限制数量
        return uncertainDocs.stream()
                .sorted((a, b) -> Double.compare(b.getUncertaintyScore(), a.getUncertaintyScore()))
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());
    }

    /**
     * 找出可能遗漏的相关文档
     */
    private List<PotentiallyRelevantDocument> findPotentiallyRelevantDocuments(
            String question, List<Document> retrievedDocs) {

        List<PotentiallyRelevantDocument> potentialDocs = new ArrayList<>();

        if (documentWeightService == null) {
            return potentialDocs;
        }

        // 找出历史上被正反馈过、但这次没有被检索到的高权重文档
        Set<String> retrievedIds = retrievedDocs.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());

        // 获取高权重文档
        Map<String, Double> highWeightDocs = getHighWeightDocuments();

        for (Map.Entry<String, Double> entry : highWeightDocs.entrySet()) {
            String docName = entry.getKey();
            double weight = entry.getValue();

            // 如果这个高权重文档没有被检索到
            if (!retrievedIds.contains(docName) && weight > 1.5) {
                PotentiallyRelevantDocument potential = new PotentiallyRelevantDocument();
                potential.setDocumentName(docName);
                potential.setHistoricalWeight(weight);
                potential.setReason(I18N.get("active_learning.log.potential_doc_reason"));
                potentialDocs.add(potential);
            }
        }

        return potentialDocs.stream()
                .sorted((a, b) -> Double.compare(b.getHistoricalWeight(), a.getHistoricalWeight()))
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 基于历史查询获取推荐
     */
    private List<HistoryBasedRecommendation> getHistoryBasedRecommendations(String question) {
        List<HistoryBasedRecommendation> recommendations = new ArrayList<>();

        // 查找相似的历史查询
        List<QueryHistory> similarQueries = findSimilarQueries(question);

        for (QueryHistory history : similarQueries) {
            if (history.getHighRatedDocuments() != null) {
                for (String docName : history.getHighRatedDocuments()) {
                    HistoryBasedRecommendation rec = new HistoryBasedRecommendation();
                    rec.setDocumentName(docName);
                    rec.setSimilarQuestion(history.getQuestion());
                    rec.setSimilarityScore(history.getSimilarityScore());
                    rec.setHistoricalRating(history.getRating());
                    recommendations.add(rec);
                }
            }
        }

        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 记录查询历史（用于后续推荐）
     */
    public void recordQueryHistory(String question, List<String> usedDocuments,
            List<String> highRatedDocuments, int rating) {

        QueryHistory history = new QueryHistory();
        history.setQuestion(question);
        history.setUsedDocuments(usedDocuments);
        history.setHighRatedDocuments(highRatedDocuments);
        history.setRating(rating);
        history.setTimestamp(System.currentTimeMillis());

        // 提取关键词作为索引
        String key = extractKeywords(question);
        queryHistoryCache.computeIfAbsent(key, k -> new ArrayList<>()).add(history);

        log.debug(I18N.get("active_learning.log.save_history", question.substring(0, Math.min(50, question.length()))));
    }

    /**
     * 处理用户对推荐的反馈
     */
    public void processFeedback(String documentName, boolean isRelevant, String question) {
        if (documentWeightService != null) {
            // 根据用户反馈调整权重
            double adjustment = isRelevant ? 0.3 : -0.3;
            documentWeightService.applyFeedback(documentName,
                    isRelevant ? top.yumbo.ai.rag.feedback.QARecord.FeedbackType.LIKE
                              : top.yumbo.ai.rag.feedback.QARecord.FeedbackType.DISLIKE);

            log.info(I18N.get("active_learning.log.feedback_processed", documentName, 
                    I18N.get(isRelevant ? "active_learning.feedback.relevant" : "active_learning.feedback.irrelevant"), adjustment));
        }
    }

    /**
     * 计算不确定性分数
     */
    private double calculateUncertaintyScore(Document doc, int rank, int totalDocs) {
        // 基于排名的不确定性：排名越靠近边界，不确定性越高
        double rankUncertainty = 1.0 - Math.abs(rank - totalDocs * 0.5) / (totalDocs * 0.5);

        // 基于权重的不确定性
        double weight = documentWeightService != null
                ? documentWeightService.getDocumentWeight(doc.getTitle())
                : 1.0;
        double weightUncertainty = Math.abs(weight - 1.0) < 0.5 ? 0.8 : 0.3;

        return (rankUncertainty + weightUncertainty) / 2;
    }

    /**
     * 计算整体置信度
     */
    private double calculateConfidence(List<Document> docs, int topKUsed) {
        if (docs == null || docs.isEmpty()) {
            return 0.0;
        }

        // 如果 topK 文档权重都较高，置信度高
        double avgWeight = 0.0;
        int count = Math.min(topKUsed, docs.size());

        if (documentWeightService != null) {
            for (int i = 0; i < count; i++) {
                avgWeight += documentWeightService.getDocumentWeight(docs.get(i).getTitle());
            }
            avgWeight /= count;
        } else {
            avgWeight = 1.0;
        }

        return Math.min(1.0, avgWeight / 2.0);
    }

    /**
     * 生成不确定性原因
     */
    private String generateUncertaintyReason(Document doc, int rank, int topKUsed) {
        if (rank == topKUsed) {
            return "该文档排名刚好在使用边界，可能包含有用信息";
        } else if (rank < topKUsed + 3) {
            return "该文档排名接近使用范围，相关性待确认";
        } else {
            return "该文档与查询有一定相关性，但排名较低";
        }
    }

    /**
     * 生成推荐理由
     */
    private String generateRecommendationReason(ActiveLearningRecommendation recommendation) {
        StringBuilder reason = new StringBuilder();

        if (!recommendation.getUncertainDocuments().isEmpty()) {
            reason.append("发现 ").append(recommendation.getUncertainDocuments().size())
                    .append(" 个边界文档需要确认。");
        }

        if (!recommendation.getPotentiallyRelevantDocuments().isEmpty()) {
            reason.append("有 ").append(recommendation.getPotentiallyRelevantDocuments().size())
                    .append(" 个历史高分文档可能相关。");
        }

        if (!recommendation.getHistoryBasedRecommendations().isEmpty()) {
            reason.append("基于相似问题推荐 ")
                    .append(recommendation.getHistoryBasedRecommendations().size())
                    .append(" 个文档。");
        }

        if (reason.length() == 0) {
            reason.append("当前检索结果置信度较高，无需额外确认。");
        }

        return reason.toString();
    }

    /**
     * 查找相似的历史查询
     */
    private List<QueryHistory> findSimilarQueries(String question) {
        List<QueryHistory> similar = new ArrayList<>();
        String key = extractKeywords(question);

        // 简单的关键词匹配
        for (Map.Entry<String, List<QueryHistory>> entry : queryHistoryCache.entrySet()) {
            if (hasSimilarKeywords(key, entry.getKey())) {
                for (QueryHistory history : entry.getValue()) {
                    history.setSimilarityScore(calculateSimilarity(question, history.getQuestion()));
                    similar.add(history);
                }
            }
        }

        return similar.stream()
                .filter(h -> h.getSimilarityScore() > 0.3)
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 提取关键词
     */
    private String extractKeywords(String text) {
        return text.replaceAll("[\\s,.;:?!]+", " ")
                .toLowerCase()
                .trim();
    }

    /**
     * 检查关键词相似性
     */
    private boolean hasSimilarKeywords(String key1, String key2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(key1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(key2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        return intersection.size() >= Math.min(2, Math.min(words1.size(), words2.size()) / 2);
    }

    /**
     * 计算文本相似度
     */
    private double calculateSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(extractKeywords(text1).split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(extractKeywords(text2).split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * 获取高权重文档
     */
    private Map<String, Double> getHighWeightDocuments() {
        // 这里简化实现，实际应该从 DocumentWeightService 获取
        return new HashMap<>();
    }

    // ==================== 内部数据类 ====================

    /**
     * 主动学习推荐结果
     */
    @Data
    public static class ActiveLearningRecommendation {
        private String question;
        private List<UncertainDocument> uncertainDocuments = new ArrayList<>();
        private List<PotentiallyRelevantDocument> potentiallyRelevantDocuments = new ArrayList<>();
        private List<HistoryBasedRecommendation> historyBasedRecommendations = new ArrayList<>();
        private double confidenceScore;
        private String recommendationReason;

        /**
         * 是否需要用户确认
         */
        public boolean needsUserConfirmation() {
            return !uncertainDocuments.isEmpty()
                    || !potentiallyRelevantDocuments.isEmpty()
                    || confidenceScore < 0.6;
        }

        /**
         * 获取所有推荐文档名称
         */
        public List<String> getAllRecommendedDocuments() {
            List<String> docs = new ArrayList<>();
            uncertainDocuments.forEach(d -> docs.add(d.getDocument().getTitle()));
            potentiallyRelevantDocuments.forEach(d -> docs.add(d.getDocumentName()));
            historyBasedRecommendations.forEach(d -> docs.add(d.getDocumentName()));
            return docs;
        }
    }

    /**
     * 不确定文档
     */
    @Data
    public static class UncertainDocument {
        private Document document;
        private int rank;
        private double uncertaintyScore;
        private String reason;
    }

    /**
     * 潜在相关文档
     */
    @Data
    public static class PotentiallyRelevantDocument {
        private String documentName;
        private double historicalWeight;
        private String reason;
    }

    /**
     * 基于历史的推荐
     */
    @Data
    public static class HistoryBasedRecommendation {
        private String documentName;
        private String similarQuestion;
        private double similarityScore;
        private int historicalRating;
    }

    /**
     * 查询历史
     */
    @Data
    private static class QueryHistory {
        private String question;
        private List<String> usedDocuments;
        private List<String> highRatedDocuments;
        private int rating;
        private long timestamp;
        private double similarityScore;
    }
}

