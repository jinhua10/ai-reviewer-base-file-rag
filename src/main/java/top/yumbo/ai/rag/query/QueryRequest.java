package top.yumbo.ai.rag.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询请求模型
 * 封装所有查询参数
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
     * 查询文本
     */
    private String queryText;

    /**
     * 查询字段
     */
    @Builder.Default
    private String[] fields = new String[]{"title", "content"};

    /**
     * 返回结果数量限制
     */
    @Builder.Default
    private int limit = 10;

    /**
     * 结果偏移量（用于分页）
     */
    @Builder.Default
    private int offset = 0;

    /**
     * 过滤条件
     */
    @Builder.Default
    private Map<String, String> filters = new HashMap<>();

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向
     */
    @Builder.Default
    private SortOrder sortOrder = SortOrder.RELEVANCE;

    /**
     * 是否启用模糊查询
     */
    @Builder.Default
    private boolean enableFuzzy = false;

    /**
     * 最小分数阈值
     */
    @Builder.Default
    private float minScore = 0.0f;

    /**
     * 排序方向枚举
     */
    public enum SortOrder {
        ASC, DESC, RELEVANCE
    }

    /**
     * 添加过滤条件
     */
    public QueryRequest addFilter(String field, String value) {
        this.filters.put(field, value);
        return this;
    }

    /**
     * 生成缓存键
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

