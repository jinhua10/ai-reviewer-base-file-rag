package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.feedback.QARecordService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 相似问题检测服务（方案2：基于关键词匹配）
 * 不依赖向量检索，使用简单的关键词匹配来查找相似问题
 *
 * 特点：
 * - ✅ 无需向量模型，轻量级
 * - ✅ 基于历史问答记录
 * - ✅ 关键词重叠度计算
 * - ✅ 支持中英文
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Slf4j
@Service
public class SimilarQAService {

    private final QARecordService qaRecordService;

    // 停用词（中英文）
    private static final Set<String> STOP_WORDS = Set.of(
        // 中文停用词
        "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这",
        // 英文停用词
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "should", "could", "can", "may", "might", "must", "shall",
        "what", "which", "who", "when", "where", "why", "how", "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them"
    );

    @Autowired
    public SimilarQAService(QARecordService qaRecordService) {
        this.qaRecordService = qaRecordService;
    }

    /**
     * 查找相似问题
     *
     * @param question  用户问题
     * @param minScore  最小相似度分数（0-100）
     * @param limit     返回数量上限
     * @return 相似问题列表，按相似度降序排序
     */
    public List<SimilarQA> findSimilar(String question, int minScore, int limit) {
        try {
            // 1. 提取查询问题的关键词
            Set<String> queryKeywords = extractKeywords(question);
            if (queryKeywords.isEmpty()) {
                log.debug("查询问题没有有效关键词: {}", question);
                return Collections.emptyList();
            }

            log.debug("查询关键词: {}", queryKeywords);

            // 2. 获取历史问答记录（只取高评分的）
            List<QARecord> records = qaRecordService.getRecentRecords(100); // 取最近100条
            List<SimilarQA> candidates = new ArrayList<>();

            // 3. 计算每条记录的相似度
            for (QARecord record : records) {
                // 跳过低评分记录（只取评分 >= 4 的）
                if (record.getOverallRating() == null || record.getOverallRating() < 4) {
                    continue;
                }

                Set<String> recordKeywords = extractKeywords(record.getQuestion());
                if (recordKeywords.isEmpty()) {
                    continue;
                }

                // 计算关键词重叠度
                int similarity = calculateSimilarity(queryKeywords, recordKeywords);

                if (similarity >= minScore) {
                    SimilarQA qa = new SimilarQA();
                    qa.setQuestion(record.getQuestion());
                    qa.setAnswer(record.getAnswer());
                    qa.setRating(record.getOverallRating());
                    qa.setRecordId(record.getId());
                    qa.setSimilarity(similarity / 100.0f); // 转换为0-1范围

                    candidates.add(qa);
                }
            }

            // 4. 按相似度排序并限制返回数量
            List<SimilarQA> results = candidates.stream()
                .sorted(Comparator.comparing(SimilarQA::getSimilarity).reversed()
                       .thenComparing(SimilarQA::getRating).reversed())
                .limit(limit)
                .collect(Collectors.toList());

            log.info("🔍 找到 {} 个相似问题（关键词：{}）", results.size(), queryKeywords);

            return results;

        } catch (Exception e) {
            log.error("❌ 查找相似问题失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 提取关键词
     * 支持中英文，移除停用词
     */
    private Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> keywords = new HashSet<>();

        // 转小写
        text = text.toLowerCase();

        // 分词：按空格、标点符号分割
        String[] tokens = text.split("[\\s\\p{Punct}]+");

        for (String token : tokens) {
            token = token.trim();

            // 跳过短词和停用词
            if (token.length() < 2 || STOP_WORDS.contains(token)) {
                continue;
            }

            keywords.add(token);
        }

        // 对于中文，按字符提取（如果没有空格分词）
        if (keywords.isEmpty() && containsChinese(text)) {
            // 提取2-3字的词组
            for (int i = 0; i < text.length() - 1; i++) {
                String bigram = text.substring(i, Math.min(i + 2, text.length()));
                if (bigram.length() == 2 && !STOP_WORDS.contains(bigram)) {
                    keywords.add(bigram);
                }
            }
        }

        return keywords;
    }

    /**
     * 计算相似度（0-100分）
     * 使用 Jaccard 相似度
     */
    private int calculateSimilarity(Set<String> keywords1, Set<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0;
        }

        // 计算交集
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        // 计算并集
        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        // Jaccard 相似度
        double jaccard = (double) intersection.size() / union.size();

        return (int) (jaccard * 100);
    }

    /**
     * 判断是否包含中文字符
     */
    private boolean containsChinese(String text) {
        return text.chars().anyMatch(c ->
            Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
        );
    }

    /**
     * 相似问答结果
     */
    @Data
    public static class SimilarQA {
        /**
         * 原始问题
         */
        private String question;

        /**
         * 回答内容
         */
        private String answer;

        /**
         * 评分（1-5）
         */
        private int rating;

        /**
         * 记录ID
         */
        private String recordId;

        /**
         * 相似度（0.0-1.0）
         */
        private float similarity;
    }
}

