package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果模型（Search result model）
 * 封装搜索操作的返回结果（Encapsulates the return result of search operation）
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 查询文本（Query text）
     */
    private String query;

    /**
     * 匹配的文档列表（Matched document list）
     */
    @Builder.Default
    private List<ScoredDocument> documents = new ArrayList<>();

    /**
     * 总匹配数量（Total match count）
     */
    private long totalHits;

    /**
     * 查询耗时（毫秒）（Query time cost (milliseconds)）
     */
    private long queryTimeMs;

    /**
     * 是否有更多结果（Whether there are more results）
     */
    private boolean hasMore;

    /**
     * 当前页码（Current page number）
     */
    private int page;

    /**
     * 每页大小（Page size）
     */
    private int pageSize;

    /**
     * 总页数（Total pages）
     */
    private int totalPages;

    /**
     * 搜索建议（Search suggestions）
     */
    private List<String> suggestions;

    /**
     * 搜索统计信息（Search statistics）
     */
    private SearchStatistics statistics;

    /**
     * 获取评分文档列表（Get scored document list）
     */
    public List<ScoredDocument> getScoredDocuments() {
        return documents != null ? documents : new ArrayList<>();
    }

    /**
     * 设置评分文档列表（Set scored document list）
     */
    public void setScoredDocuments(List<ScoredDocument> documents) {
        this.documents = documents != null ? new ArrayList<>(documents) : new ArrayList<>();
    }

    /**
     * 添加评分文档（Add scored document）
     */
    public void addScoredDocument(ScoredDocument document) {
        if (documents == null) {
            documents = new ArrayList<>();
        }
        documents.add(document);
        totalHits = documents.size();
    }

    /**
     * 移除评分文档（Remove scored document）
     */
    public void removeScoredDocument(ScoredDocument document) {
        if (documents != null) {
            documents.remove(document);
            totalHits = documents.size();
        }
    }

    /**
     * 清空结果（Clear results）
     */
    public void clear() {
        if (documents != null) {
            documents.clear();
        }
        totalHits = 0;
        hasMore = false;
        page = 0;
        totalPages = 0;
    }

    /**
     * 检查是否为空结果（Check if empty result）
     */
    public boolean isEmpty() {
        return documents == null || documents.isEmpty();
    }

    /**
     * 获取结果大小（Get result size）
     */
    public int size() {
        return documents != null ? documents.size() : 0;
    }

    /**
     * 获取最高评分（Get highest score）
     */
    public float getMaxScore() {
        if (documents == null || documents.isEmpty()) {
            return 0.0f;
        }
        return documents.stream()
                .map(ScoredDocument::getScore)
                .max(Float::compare)
                .orElse(0.0f);
    }

    /**
     * 获取最低评分（Get lowest score）
     */
    public float getMinScore() {
        if (documents == null || documents.isEmpty()) {
            return 0.0f;
        }
        return documents.stream()
                .map(ScoredDocument::getScore)
                .min(Float::compare)
                .orElse(0.0f);
    }

    /**
     * 获取平均评分（Get average score）
     */
    public double getAverageScore() {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }
        return documents.stream()
                .mapToDouble(ScoredDocument::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 合并搜索结果（Merge search results）
     */
    public void merge(SearchResult other) {
        if (other == null || other.documents == null) {
            return;
        }

        if (documents == null) {
            documents = new ArrayList<>();
        }

        documents.addAll(other.documents);
        totalHits += other.totalHits;
        hasMore = hasMore || other.hasMore;

        // 重新计算页码信息（Recalculate page information）
        if (pageSize > 0) {
            totalPages = (int) Math.ceil((double) totalHits / pageSize);
        }
    }

    /**
     * 创建空结果（Create empty result）
     */
    public static SearchResult empty(String query) {
        return SearchResult.builder()
                .query(query)
                .documents(new ArrayList<>())
                .totalHits(0)
                .queryTimeMs(0)
                .hasMore(false)
                .page(0)
                .pageSize(0)
                .totalPages(0)
                .build();
    }

    /**
     * 从文档列表创建结果（Create result from document list）
     */
    public static SearchResult fromDocuments(String query, List<ScoredDocument> documents) {
        return SearchResult.builder()
                .query(query)
                .documents(documents != null ? new ArrayList<>(documents) : new ArrayList<>())
                .totalHits(documents != null ? documents.size() : 0)
                .queryTimeMs(0)
                .hasMore(false)
                .page(0)
                .pageSize(documents != null ? documents.size() : 0)
                .totalPages(1)
                .build();
    }

    /**
     * 搜索统计信息（Search statistics）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchStatistics {
        /**
         * 索引中的总文档数（Total documents in index）
         */
        private long totalDocumentsInIndex;

        /**
         * 搜索的字段数（Number of fields searched）
         */
        private int fieldsSearched;

        /**
         * 使用的过滤器数（Number of filters used）
         */
        private int filtersApplied;

        /**
         * 缓存命中率（Cache hit rate）
         */
        private double cacheHitRate;

        /**
         * 索引搜索时间（毫秒）（Index search time (milliseconds)）
         */
        private long indexSearchTimeMs;

        /**
         * 后处理时间（毫秒）（Post-processing time (milliseconds)）
         */
        private long postProcessingTimeMs;
    }
}
