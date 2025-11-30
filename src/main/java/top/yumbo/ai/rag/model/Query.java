package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询模型（Query model）
 * 封装搜索查询的所有参数（Encapsulates all parameters of a search query）
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    /**
     * 查询文本（Query text）
     */
    private String queryText;

    /**
     * 查询字段（默认查询所有字段）（Query fields (default query all fields)）
     */
    @Builder.Default
    private String[] fields = new String[]{"title", "content"};

    /**
     * 返回结果数量限制（Return result count limit）
     */
    @Builder.Default
    private int limit = 10;

    /**
     * 结果偏移量（用于分页）（Result offset (for pagination)）
     */
    @Builder.Default
    private int offset = 0;

    /**
     * 过滤条件（Filter conditions）
     */
    @Builder.Default
    private Map<String, String> filters = new HashMap<>();

    /**
     * 排序字段（Sort field）
     */
    private String sortField;

    /**
     * 排序方向（Sort direction）
     */
    private String sortOrder;

    /**
     * 最小分数阈值（Minimum score threshold）
     */
    private Float minScore;

    /**
     * 最大分数阈值（Maximum score threshold）
     */
    private Float maxScore;

    /**
     * 查询超时时间（毫秒）（Query timeout (milliseconds)）
     */
    private Long timeoutMs;

    /**
     * 是否启用模糊搜索（Whether to enable fuzzy search）
     */
    @Builder.Default
    private Boolean fuzzy = false;

    /**
     * 模糊搜索编辑距离（Fuzzy search edit distance）
     */
    @Builder.Default
    private Integer fuzzyDistance = 2;

    /**
     * 是否启用通配符搜索（Whether to enable wildcard search）
     */
    @Builder.Default
    private Boolean wildcard = false;

    /**
     * 是否启用短语搜索（Whether to enable phrase search）
     */
    @Builder.Default
    private Boolean phrase = false;

    /**
     * 搜索操作符（Search operator）
     */
    @Builder.Default
    private String operator = "OR";

    /**
     * 搜索模式（Search mode）
     */
    @Builder.Default
    private SearchMode searchMode = SearchMode.STANDARD;

    /**
     * 搜索模式枚举（Search mode enumeration）
     */
    public enum SearchMode {
        /**
         * 标准搜索（Standard search）
         */
        STANDARD,

        /**
         * 布尔搜索（Boolean search）
         */
        BOOLEAN,

        /**
         * 向量搜索（Vector search）
         */
        VECTOR,

        /**
         * 混合搜索（Hybrid search）
         */
        HYBRID
    }

    /**
     * 添加过滤条件（Add filter condition）
     */
    public void addFilter(String key, String value) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.put(key, value);
    }

    /**
     * 移除过滤条件（Remove filter condition）
     */
    public void removeFilter(String key) {
        if (filters != null) {
            filters.remove(key);
        }
    }

    /**
     * 清空过滤条件（Clear filter conditions）
     */
    public void clearFilters() {
        if (filters != null) {
            filters.clear();
        }
    }

    /**
     * 获取过滤条件值（Get filter condition value）
     */
    public String getFilter(String key) {
        return filters != null ? filters.get(key) : null;
    }

    /**
     * 检查是否包含过滤条件（Check if contains filter condition）
     */
    public boolean hasFilter(String key) {
        return filters != null && filters.containsKey(key);
    }

    /**
     * 获取所有过滤条件（Get all filter conditions）
     */
    public Map<String, String> getFilters() {
        return filters != null ? new HashMap<>(filters) : new HashMap<>();
    }

    /**
     * 设置过滤条件（Set filter conditions）
     */
    public void setFilters(Map<String, String> filters) {
        this.filters = filters != null ? new HashMap<>(filters) : null;
    }

    /**
     * 创建简单查询（Create simple query）
     */
    public static Query simple(String queryText) {
        return Query.builder()
                .queryText(queryText)
                .build();
    }

    /**
     * 创建高级查询（Create advanced query）
     */
    public static Query advanced(String queryText, String[] fields, int limit) {
        return Query.builder()
                .queryText(queryText)
                .fields(fields)
                .limit(limit)
                .build();
    }

    /**
     * 创建过滤查询（Create filtered query）
     */
    public static Query filtered(String queryText, Map<String, String> filters) {
        return Query.builder()
                .queryText(queryText)
                .filters(filters)
                .build();
    }
}
