package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 相关性召回服务
 *
 * 基于多维度相关性计算，从备忘录中召回最相关的条目
 */
@Slf4j
@Service
public class RelevanceRecallService {

    private final KeywordInvertedIndex keywordIndex;

    @Value("${document-analysis.memo.relevance-weights.keyword-match:0.4}")
    private double keywordMatchWeight;

    @Value("${document-analysis.memo.relevance-weights.importance:0.2}")
    private double importanceWeight;

    @Value("${document-analysis.memo.relevance-weights.recency:0.1}")
    private double recencyWeight;

    @Value("${document-analysis.memo.relevance-weights.structural-distance:0.1}")
    private double structuralDistanceWeight;

    @Value("${document-analysis.memo.relevance-weights.type-match:0.2}")
    private double typeMatchWeight;

    public RelevanceRecallService(KeywordInvertedIndex keywordIndex) {
        this.keywordIndex = keywordIndex;
    }

    /**
     * 召回与当前片段相关的备忘录条目
     *
     * @param currentSegment 当前片段
     * @param allMemos 所有备忘录
     * @param topK 返回数量
     * @param maxTokens 最大 token 数
     * @return 相关条目列表
     */
    public List<MemoEntry> recall(DocumentSegment currentSegment,
                                  List<MemoEntry> allMemos,
                                  int topK,
                                  int maxTokens) {
        if (currentSegment == null || allMemos == null || allMemos.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取当前片段的关键词
        List<String> currentKeywords = extractKeywords(currentSegment);

        // 计算每个备忘录的相关性分数
        List<ScoredMemo> scoredMemos = new ArrayList<>();

        for (MemoEntry memo : allMemos) {
            // 排除当前片段自己
            if (memo.getSegmentIndex() == currentSegment.getIndex()) {
                continue;
            }

            double score = calculateRelevanceScore(currentSegment, currentKeywords, memo, allMemos.size());

            if (score > 0) {
                scoredMemos.add(new ScoredMemo(memo, score));
            }
        }

        // 按分数排序
        scoredMemos.sort((a, b) -> Double.compare(b.score, a.score));

        // 选择 top-K 并控制 token 数
        List<MemoEntry> result = new ArrayList<>();
        int totalTokens = 0;

        for (ScoredMemo sm : scoredMemos) {
            if (result.size() >= topK) {
                break;
            }

            int entryTokens = sm.memo.getTokenCount();
            if (totalTokens + entryTokens > maxTokens) {
                continue;
            }

            result.add(sm.memo);
            totalTokens += entryTokens;
        }

        log.debug("召回 {} 个相关条目，总 token: {}", result.size(), totalTokens);

        return result;
    }

    /**
     * 基于关键词快速召回
     *
     * @param keywords 关键词列表
     * @param topK 返回数量
     * @return 相关条目列表
     */
    public List<MemoEntry> recallByKeywords(List<String> keywords, int topK) {
        List<KeywordInvertedIndex.ScoredEntry> scoredEntries = keywordIndex.searchByKeywordsOr(keywords);

        return scoredEntries.stream()
                .limit(topK)
                .map(se -> se.entry)
                .collect(Collectors.toList());
    }

    /**
     * 计算相关性分数
     */
    private double calculateRelevanceScore(DocumentSegment currentSegment,
                                           List<String> currentKeywords,
                                           MemoEntry memo,
                                           int totalMemos) {
        double score = 0.0;

        // 1. 关键词匹配分数
        double keywordScore = calculateKeywordMatchScore(currentKeywords, memo.getKeywords());
        score += keywordScore * keywordMatchWeight;

        // 2. 重要性分数
        double importanceScore = memo.getImportance();
        score += importanceScore * importanceWeight;

        // 3. 时效性分数（越近越高）
        double recencyScore = calculateRecencyScore(currentSegment.getIndex(), memo.getSegmentIndex(), totalMemos);
        score += recencyScore * recencyWeight;

        // 4. 结构距离分数
        double structuralScore = calculateStructuralDistanceScore(currentSegment.getIndex(), memo.getSegmentIndex(), totalMemos);
        score += structuralScore * structuralDistanceWeight;

        // 5. 类型匹配分数
        double typeScore = calculateTypeMatchScore(currentSegment.getType(), memo.getSegmentType());
        score += typeScore * typeMatchWeight;

        return score;
    }

    /**
     * 计算关键词匹配分数
     */
    private double calculateKeywordMatchScore(List<String> currentKeywords, List<String> memoKeywords) {
        if (currentKeywords == null || currentKeywords.isEmpty() ||
            memoKeywords == null || memoKeywords.isEmpty()) {
            return 0.0;
        }

        Set<String> currentSet = currentKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> memoSet = memoKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Jaccard 相似度
        Set<String> intersection = new HashSet<>(currentSet);
        intersection.retainAll(memoSet);

        Set<String> union = new HashSet<>(currentSet);
        union.addAll(memoSet);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 计算时效性分数
     */
    private double calculateRecencyScore(int currentIndex, int memoIndex, int total) {
        // 距离越近，分数越高
        int distance = Math.abs(currentIndex - memoIndex);
        return 1.0 - (double) distance / total;
    }

    /**
     * 计算结构距离分数
     */
    private double calculateStructuralDistanceScore(int currentIndex, int memoIndex, int total) {
        // 同一章节/区域的分数更高
        // 简单实现：假设每10个片段为一个区域
        int regionSize = Math.max(10, total / 5);
        int currentRegion = currentIndex / regionSize;
        int memoRegion = memoIndex / regionSize;

        if (currentRegion == memoRegion) {
            return 1.0;
        } else {
            int regionDistance = Math.abs(currentRegion - memoRegion);
            return Math.max(0, 1.0 - regionDistance * 0.2);
        }
    }

    /**
     * 计算类型匹配分数
     */
    private double calculateTypeMatchScore(SegmentType currentType, SegmentType memoType) {
        if (currentType == null || memoType == null) {
            return 0.5;
        }

        if (currentType == memoType) {
            return 1.0;
        }

        // 相似类型
        if (isSimilarType(currentType, memoType)) {
            return 0.7;
        }

        return 0.3;
    }

    /**
     * 判断是否是相似类型
     */
    private boolean isSimilarType(SegmentType type1, SegmentType type2) {
        // 幻灯片和页面类似
        if ((type1 == SegmentType.SLIDE && type2 == SegmentType.PAGE) ||
            (type1 == SegmentType.PAGE && type2 == SegmentType.SLIDE)) {
            return true;
        }

        // 章节和段落类似
        if ((type1 == SegmentType.CHAPTER && type2 == SegmentType.PARAGRAPH) ||
            (type1 == SegmentType.PARAGRAPH && type2 == SegmentType.CHAPTER)) {
            return true;
        }

        return false;
    }

    /**
     * 从片段中提取关键词
     */
    private List<String> extractKeywords(DocumentSegment segment) {
        List<String> keywords = new ArrayList<>();

        // 从标题提取
        if (segment.getTitle() != null) {
            keywords.addAll(tokenize(segment.getTitle()));
        }

        // 从内容提取（取前500字符）
        if (segment.getTextContent() != null) {
            String content = segment.getTextContent();
            if (content.length() > 500) {
                content = content.substring(0, 500);
            }
            keywords.addAll(tokenize(content));
        }

        // 去重并限制数量
        return keywords.stream()
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * 简单分词
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        // 按标点和空格分割
        String[] tokens = text.split("[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF]+");

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim().toLowerCase();
            if (token.length() >= 2 && !token.matches("\\d+")) {
                result.add(token);
            }
        }

        return result;
    }

    /**
     * 带分数的备忘录
     */
    private static class ScoredMemo {
        final MemoEntry memo;
        final double score;

        ScoredMemo(MemoEntry memo, double score) {
            this.memo = memo;
            this.score = score;
        }
    }
}

