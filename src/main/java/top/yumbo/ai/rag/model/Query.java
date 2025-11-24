package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询模型
 * 封装搜索查询的所有参数
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
     * 查询文本
     */
    private String queryText;

    /**
     * 查询字段（默认查询所有字段）
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
     * 排序方向（true=升序，false=降序）
     */
    @Builder.Default
    private boolean sortAscending = false;

    /**
     * 模糊查询的最大编辑距离
     */
    @Builder.Default
    private int fuzzyMaxEdits = 2;

    /**
     * 是否启用模糊查询
     */
    @Builder.Default
    private boolean enableFuzzy = false;

    /**
     * 添加过滤条件
     */
    public Query withFilter(String field, String value) {
        if (this.filters == null) {
            this.filters = new HashMap<>();
        }
        this.filters.put(field, value);
        return this;
    }

    /**
     * 设置结果数量限制
     */
    public Query withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 设置偏移量
     */
    public Query withOffset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 创建查询实例的便捷方法
     */
    public static Query of(String queryText) {
        return Query.builder()
                .queryText(queryText)
                .build();
    }
}

