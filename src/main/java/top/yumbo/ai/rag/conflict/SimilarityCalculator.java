package top.yumbo.ai.rag.conflict;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * 相似度计算器 (Similarity Calculator)
 *
 * 计算两个概念之间的相似度
 * (Calculates similarity between two concepts)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class SimilarityCalculator {

    /**
     * 语义相似度阈值 (Semantic similarity threshold)
     */
    private static final double SEMANTIC_THRESHOLD = 0.85;

    /**
     * 计算语义相似度 (Calculate semantic similarity)
     * 使用向量余弦相似度
     *
     * @param vector1 向量1 (Vector 1)
     * @param vector2 向量2 (Vector 2)
     * @return 相似度 (Similarity, 0-1)
     */
    public double calculateSemanticSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null) {
            log.warn(I18N.get("conflict.similarity.null_vector"));
            return 0.0;
        }

        if (vector1.length != vector2.length) {
            log.warn(I18N.get("conflict.similarity.vector_length_mismatch",
                    vector1.length, vector2.length));
            return 0.0;
        }

        // 计算余弦相似度 (Calculate cosine similarity)
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));

        log.debug(I18N.get("conflict.similarity.semantic_calculated", similarity));

        return Math.max(0.0, Math.min(1.0, similarity));
    }

    /**
     * 计算关键词相似度 (Calculate keyword similarity)
     * 使用 Jaccard 相似度
     *
     * @param content1 内容1 (Content 1)
     * @param content2 内容2 (Content 2)
     * @return 相似度 (Similarity, 0-1)
     */
    public double calculateKeywordSimilarity(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return 0.0;
        }

        // 提取关键词 (Extract keywords)
        Set<String> keywords1 = extractKeywords(content1);
        Set<String> keywords2 = extractKeywords(content2);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // 计算 Jaccard 相似度 (Calculate Jaccard similarity)
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        double similarity = (double) intersection.size() / union.size();

        log.debug(I18N.get("conflict.similarity.keyword_calculated",
                similarity, intersection.size(), union.size()));

        return similarity;
    }

    /**
     * 计算结构相似度 (Calculate structure similarity)
     * 比较文档的结构特征
     *
     * @param doc1 文档1 (Document 1)
     * @param doc2 文档2 (Document 2)
     * @return 相似度 (Similarity, 0-1)
     */
    public double calculateStructureSimilarity(Document doc1, Document doc2) {
        if (doc1 == null || doc2 == null) {
            return 0.0;
        }

        double score = 0.0;
        int comparisons = 0;

        // 比较内容长度 (Compare content length)
        if (doc1.getContent() != null && doc2.getContent() != null) {
            int len1 = doc1.getContent().length();
            int len2 = doc2.getContent().length();
            double lengthSim = 1.0 - Math.abs(len1 - len2) / (double) Math.max(len1, len2);
            score += lengthSim;
            comparisons++;
        }

        // 比较来源类型 (Compare source type)
        // Note: Document类没有getType方法，这里跳过类型比较

        // 比较来源 (Compare source)
        if (doc1.getSource() != null && doc2.getSource() != null) {
            score += doc1.getSource().equals(doc2.getSource()) ? 1.0 : 0.0;
            comparisons++;
        }

        double similarity = comparisons > 0 ? score / comparisons : 0.0;

        log.debug(I18N.get("conflict.similarity.structure_calculated", similarity));

        return similarity;
    }

    /**
     * 综合评分 (Combine scores)
     *
     * @param semanticScore 语义分数 (Semantic score)
     * @param keywordScore 关键词分数 (Keyword score)
     * @param structureScore 结构分数 (Structure score)
     * @return 综合分数 (Combined score)
     */
    public double combineScores(double semanticScore, double keywordScore, double structureScore) {
        // 权重：语义0.6，关键词0.25，结构0.15
        double combined = semanticScore * 0.6 + keywordScore * 0.25 + structureScore * 0.15;

        log.info(I18N.get("conflict.similarity.combined",
                combined, semanticScore, keywordScore, structureScore));

        return combined;
    }

    /**
     * 判断是否相似 (Check if similar)
     *
     * @param similarityScore 相似度分数 (Similarity score)
     * @return 是否相似 (Whether similar)
     */
    public boolean isSimilar(double similarityScore) {
        return similarityScore >= SEMANTIC_THRESHOLD;
    }

    /**
     * 提取关键词 (Extract keywords)
     * 简单实现：分词并过滤停用词
     *
     * @param content 内容 (Content)
     * @return 关键词集合 (Keyword set)
     */
    private Set<String> extractKeywords(String content) {
        Set<String> keywords = new HashSet<>();

        if (content == null || content.trim().isEmpty()) {
            return keywords;
        }

        // 简单分词 (Simple tokenization)
        String[] words = content.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() >= 2 && !isStopWord(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * 判断是否是停用词 (Check if stop word)
     *
     * @param word 词 (Word)
     * @return 是否停用词 (Whether stop word)
     */
    private boolean isStopWord(String word) {
        // 简单的停用词列表
        Set<String> stopWords = Set.of(
                "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
                "in", "with", "to", "for", "of", "as", "by", "from", "that",
                "的", "了", "是", "在", "有", "和", "与", "或", "但", "为"
        );
        return stopWords.contains(word.toLowerCase());
    }
}

