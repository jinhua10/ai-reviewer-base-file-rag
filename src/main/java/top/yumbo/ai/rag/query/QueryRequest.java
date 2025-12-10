package top.yumbo.ai.rag.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询请求模型（Query request model）
 * 封装所有查询参数（Encapsulates all query parameters）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    /**
     * 查询文本 (Query text)
     * 要搜索的关键词或问题文本
     * (Keyword or question text to search for)
     */
    private String queryText;

    /**
     * 查询字段 (Query fields)
     * 指定要搜索的字段数组，默认搜索标题和内容
     * (Array of fields to search in, default searches title and content)
     */
    @Builder.Default
    private String[] fields = new String[]{"title", "content"};

    /**
     * 返回结果数量限制 (Result count limit)
     * 指定返回的最大结果数量，用于控制结果集大小
     * (Maximum number of results to return, used to control result set size)
     */
    @Builder.Default
    private int limit = 10;

    /**
     * 结果偏移量（用于分页）(Result offset for pagination)
     * 从第几个结果开始返回，用于实现分页功能
     * (Starting position of results to return, used for pagination)
     */
    @Builder.Default
    private int offset = 0;

    /**
     * 过滤条件 (Filter conditions)
     * 字段名和值的映射，用于缩小搜索范围
     * (Map of field names and values, used to narrow search scope)
     */
    @Builder.Default
    private Map<String, String> filters = new HashMap<>();

    /**
     * 排序字段 (Sort field)
     * 指定用于排序的字段名
     * (Field name used for sorting)
     */
    private String sortField;

    /**
     * 排序方向 (Sort order)
     * 指定排序方向，默认按相关性排序
     * (Sort direction, defaults to relevance sorting)
     */
    @Builder.Default
    private SortOrder sortOrder = SortOrder.RELEVANCE;

    /**
     * 是否启用模糊查询 (Whether to enable fuzzy query)
     * 控制是否启用模糊匹配功能
     * (Controls whether to enable fuzzy matching)
     */
    @Builder.Default
    private boolean enableFuzzy = false;

    /**
     * 最小分数阈值 (Minimum score threshold)
     * 只返回分数高于此阈值的结果
     * (Only return results with scores above this threshold)
     */
    @Builder.Default
    private float minScore = 0.0f;

    /**
     * 排序方向枚举 (Sort order enumeration)
     * 定义支持的排序方向
     * (Defines supported sort directions)
     */
    public enum SortOrder {
        /**
         * 升序 (Ascending order)
         * 从小到大排序
         * (Sort from small to large)
         */
        ASC, 
        
        /**
         * 降序 (Descending order)
         * 从大到小排序
         * (Sort from large to small)
         */
        DESC, 
        
        /**
         * 相关性 (Relevance)
         * 按相关性分数排序
         * (Sort by relevance score)
         */
        RELEVANCE
    }

    /**
     * 添加过滤条件 (Add filter condition)
     * 添加一个新的过滤条件到查询中
     * (Adds a new filter condition to the query)
     * 
     * @param field 字段名 (Field name)
     * @param value 字段值 (Field value)
     * @return 当前查询对象，支持链式调用 (Current query object, supports chain calls)
     */
    public QueryRequest addFilter(String field, String value) {
        this.filters.put(field, value);
        return this;
    }

    /**
     * 生成缓存键 (Generate cache key)
     * 基于查询参数生成唯一的缓存键
     * (Generates a unique cache key based on query parameters)
     * 
     * @return 缓存键字符串 (Cache key string)
     */
    public String getCacheKey() {
        StringBuilder sb = new StringBuilder();
        sb.append("query:").append(queryText);
        sb.append(":fields:").append(String.join(",", fields));
        sb.append(":limit:").append(limit);
        sb.append(":offset:").append(offset);
        if (!filters.isEmpty()) {
            sb.append(":filters:").append(filters.toString());
        }
        if (sortField != null) {
            sb.append(":sort:").append(sortField).append(":").append(sortOrder);
        }
        return sb.toString();
    }
}
