package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.feedback.QARecordService;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

import java.util.*;
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
 * - ✅ 支持配置化（历史记录数量、最低评分等）
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Slf4j
@Service
public class SimilarQAService {

    private final QARecordService qaRecordService;
    private final KnowledgeQAProperties properties;

    // 停用词从配置获取（Stopwords from configuration）
    private Set<String> stopWords;

    @Autowired
    public SimilarQAService(QARecordService qaRecordService,
                           KnowledgeQAProperties properties) {
        this.qaRecordService = qaRecordService;
        this.properties = properties;
        // 初始化停用词（从配置加载）
        initStopWords();
    }

    /**
     * 初始化停用词（从配置加载）
     */
    private void initStopWords() {
        stopWords = new HashSet<>();
        KnowledgeQAProperties.SearchConfig searchConfig = properties.getSearch();
        if (searchConfig.getChineseStopWords() != null) {
            stopWords.addAll(searchConfig.getChineseStopWords());
        }
        if (searchConfig.getEnglishStopWords() != null) {
            searchConfig.getEnglishStopWords().forEach(w -> stopWords.add(w.toLowerCase()));
        }
        log.debug("SimilarQAService initialized with {} stopwords", stopWords.size());
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

            // 2. 获取历史问答记录（从配置获取数量限制）
            KnowledgeQAProperties.SimilarQAConfig config = properties.getSimilarQa();
            int historyLimit = config.getHistoryLimit();
            int minRating = config.getMinRating();

            List<QARecord> records = qaRecordService.getRecentRecords(historyLimit);
            List<SimilarQA> candidates = new ArrayList<>();

            // 3. 计算每条记录的相似度
            for (QARecord record : records) {
                // 跳过低评分记录（使用配置的最低评分阈值）
                if (record.getOverallRating() == null || record.getOverallRating() < minRating) {
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

            log.info(I18N.get("log.similar.found", results.size(), queryKeywords));

            return results;

        } catch (Exception e) {
            log.error(I18N.get("log.similar.failed"), e);
            return Collections.emptyList();
        }
    }

    /**
     * 提取关键词
     * 支持中英文，移除停用词（从配置获取）
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

            // 跳过短词和停用词（使用配置的停用词）
            if (token.length() < 2 || stopWords.contains(token)) {
                continue;
            }

            keywords.add(token);
        }

        // 对于中文，按字符提取（如果没有空格分词）
        if (keywords.isEmpty() && containsChinese(text)) {
            // 提取2-3字的词组
            for (int i = 0; i < text.length() - 1; i++) {
                String bigram = text.substring(i, Math.min(i + 2, text.length()));
                if (bigram.length() == 2 && !stopWords.contains(bigram)) {
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
