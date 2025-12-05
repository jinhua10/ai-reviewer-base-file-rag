package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 关键词倒排索引
 *
 * 用于快速检索包含特定关键词的备忘录条目
 */
@Slf4j
@Component
public class KeywordInvertedIndex {

    /** 倒排索引：关键词 -> 条目ID列表 */
    private final Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();

    /** 条目存储：条目ID -> 条目 */
    private final Map<String, MemoEntry> entryStore = new ConcurrentHashMap<>();

    /** 关键词频率：关键词 -> 出现次数 */
    private final Map<String, Integer> keywordFrequency = new ConcurrentHashMap<>();

    /** 文档频率：关键词 -> 包含该词的文档数 */
    private final Map<String, Integer> documentFrequency = new ConcurrentHashMap<>();

    /**
     * 添加条目到索引
     *
     * @param entry 备忘录条目
     */
    public void addEntry(MemoEntry entry) {
        if (entry == null || entry.getId() == null) {
            return;
        }

        String entryId = entry.getId();
        entryStore.put(entryId, entry);

        // 获取关键词
        List<String> keywords = entry.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return;
        }

        Set<String> uniqueKeywords = new HashSet<>(keywords);

        for (String keyword : uniqueKeywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }

            String normalizedKeyword = normalizeKeyword(keyword);

            // 更新倒排索引
            invertedIndex.computeIfAbsent(normalizedKeyword, k -> ConcurrentHashMap.newKeySet())
                        .add(entryId);

            // 更新文档频率
            documentFrequency.merge(normalizedKeyword, 1, Integer::sum);
        }

        // 更新关键词频率
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeKeyword(keyword);
            keywordFrequency.merge(normalizedKeyword, 1, Integer::sum);
        }

        log.debug("索引条目: {} ({} 个关键词)", entryId, uniqueKeywords.size());
    }

    /**
     * 移除条目
     *
     * @param entryId 条目ID
     */
    public void removeEntry(String entryId) {
        MemoEntry entry = entryStore.remove(entryId);
        if (entry == null) {
            return;
        }

        List<String> keywords = entry.getKeywords();
        if (keywords == null) {
            return;
        }

        Set<String> uniqueKeywords = new HashSet<>(keywords);

        for (String keyword : uniqueKeywords) {
            String normalizedKeyword = normalizeKeyword(keyword);

            Set<String> entryIds = invertedIndex.get(normalizedKeyword);
            if (entryIds != null) {
                entryIds.remove(entryId);
                if (entryIds.isEmpty()) {
                    invertedIndex.remove(normalizedKeyword);
                }
            }

            documentFrequency.computeIfPresent(normalizedKeyword, (k, v) -> v > 1 ? v - 1 : null);
        }

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeKeyword(keyword);
            keywordFrequency.computeIfPresent(normalizedKeyword, (k, v) -> v > 1 ? v - 1 : null);
        }
    }

    /**
     * 搜索包含指定关键词的条目
     *
     * @param keyword 关键词
     * @return 条目列表
     */
    public List<MemoEntry> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        Set<String> entryIds = invertedIndex.get(normalizedKeyword);

        if (entryIds == null || entryIds.isEmpty()) {
            return Collections.emptyList();
        }

        return entryIds.stream()
                .map(entryStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 搜索包含任意一个关键词的条目（OR 查询）
     *
     * @param keywords 关键词列表
     * @return 条目列表及其匹配分数
     */
    public List<ScoredEntry> searchByKeywordsOr(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> entryScores = new HashMap<>();
        int totalDocs = entryStore.size();

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeKeyword(keyword);
            Set<String> entryIds = invertedIndex.get(normalizedKeyword);

            if (entryIds == null || entryIds.isEmpty()) {
                continue;
            }

            // 计算 IDF
            int df = documentFrequency.getOrDefault(normalizedKeyword, 1);
            double idf = Math.log((double) totalDocs / (df + 1)) + 1;

            for (String entryId : entryIds) {
                MemoEntry entry = entryStore.get(entryId);
                if (entry == null) continue;

                // 计算 TF
                long tf = entry.getKeywords().stream()
                        .filter(k -> normalizeKeyword(k).equals(normalizedKeyword))
                        .count();

                // TF-IDF 分数
                double score = tf * idf;
                entryScores.merge(entryId, score, Double::sum);
            }
        }

        return entryScores.entrySet().stream()
                .map(e -> new ScoredEntry(entryStore.get(e.getKey()), e.getValue()))
                .filter(se -> se.entry != null)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .collect(Collectors.toList());
    }

    /**
     * 搜索包含所有关键词的条目（AND 查询）
     *
     * @param keywords 关键词列表
     * @return 条目列表
     */
    public List<MemoEntry> searchByKeywordsAnd(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> resultIds = null;

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeKeyword(keyword);
            Set<String> entryIds = invertedIndex.get(normalizedKeyword);

            if (entryIds == null || entryIds.isEmpty()) {
                return Collections.emptyList();
            }

            if (resultIds == null) {
                resultIds = new HashSet<>(entryIds);
            } else {
                resultIds.retainAll(entryIds);
            }

            if (resultIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        if (resultIds == null) {
            return Collections.emptyList();
        }

        return resultIds.stream()
                .map(entryStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门关键词
     *
     * @param topK 返回数量
     * @return 关键词及其频率
     */
    public List<KeywordFrequency> getTopKeywords(int topK) {
        return keywordFrequency.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> new KeywordFrequency(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取索引统计信息
     */
    public IndexStats getStats() {
        IndexStats stats = new IndexStats();
        stats.totalEntries = entryStore.size();
        stats.totalKeywords = invertedIndex.size();
        stats.avgKeywordsPerEntry = entryStore.isEmpty() ? 0 :
                keywordFrequency.values().stream().mapToInt(Integer::intValue).sum() / (double) entryStore.size();
        return stats;
    }

    /**
     * 清空索引
     */
    public void clear() {
        invertedIndex.clear();
        entryStore.clear();
        keywordFrequency.clear();
        documentFrequency.clear();
        log.debug("关键词索引已清空");
    }

    /**
     * 标准化关键词
     */
    private String normalizeKeyword(String keyword) {
        return keyword.toLowerCase().trim();
    }

    // ==================== 数据类 ====================

    /**
     * 带分数的条目
     */
    public static class ScoredEntry {
        public final MemoEntry entry;
        public final double score;

        public ScoredEntry(MemoEntry entry, double score) {
            this.entry = entry;
            this.score = score;
        }
    }

    /**
     * 关键词频率
     */
    public static class KeywordFrequency {
        public final String keyword;
        public final int frequency;

        public KeywordFrequency(String keyword, int frequency) {
            this.keyword = keyword;
            this.frequency = frequency;
        }
    }

    /**
     * 索引统计
     */
    public static class IndexStats {
        public int totalEntries;
        public int totalKeywords;
        public double avgKeywordsPerEntry;
    }
}

