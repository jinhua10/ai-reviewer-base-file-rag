package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果模型
 * 封装搜索操作的返回结果
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
     * 查询文本
     */
    private String query;

    /**
     * 匹配的文档列表
     */
    @Builder.Default
    private List<ScoredDocument> documents = new ArrayList<>();

    /**
     * 总匹配数量
     */
    private long totalHits;

    /**
     * 查询耗时（毫秒）
     */
    private long queryTimeMs;

    /**
     * 是否有更多结果
     */
    private boolean hasMore;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 添加文档
     */
    public void addDocument(ScoredDocument document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(document);
    }

    /**
     * 获取纯文档列表（不含分数）
     */
    public List<Document> getDocuments() {
        if (this.documents == null) {
            return new ArrayList<>();
        }
        return this.documents.stream()
                .map(ScoredDocument::getDocument)
                .toList();
    }

    /**
     * 获取带分数的文档列表（不可变）
     * 如需修改，请创建副本
     */
    public List<ScoredDocument> getScoredDocuments() {
        return this.documents == null ?
            java.util.Collections.emptyList() :
            java.util.Collections.unmodifiableList(this.documents);
    }
}

