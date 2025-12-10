package top.yumbo.ai.rag.query;
import lombok.Getter;
import top.yumbo.ai.rag.model.SearchResult;

/**
 * 分页结果（Paged result）
 * 封装分页查询的结果信息
 * (Encapsulates paginated query result information)
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Getter
public class PagedResult {
    /**
     * 搜索结果 (Search result)
     * 原始搜索结果对象
     * (Original search result object)
     */
    private final SearchResult searchResult;
    
    /**
     * 当前页码 (Current page number)
     * 当前页的编号，从0开始
     * (Number of the current page, starting from 0)
     */
    private final int currentPage;
    
    /**
     * 每页大小 (Page size)
     * 每页包含的结果数量
     * (Number of results per page)
     */
    private final int pageSize;
    
    /**
     * 总页数 (Total pages)
     * 总共有多少页
     * (Total number of pages)
     */
    private final int totalPages;
    
    /**
     * 是否有下一页 (Whether there is a next page)
     * 指示是否还有下一页
     * (Indicates whether there is a next page)
     */
    private final boolean hasNext;
    
    /**
     * 是否有上一页 (Whether there is a previous page)
     * 指示是否还有上一页
     * (Indicates whether there is a previous page)
     */
    private final boolean hasPrevious;
    /**
     * 构造函数 (Constructor)
     * 
     * @param result 搜索结果 (Search result)
     * @param currentPage 当前页码 (Current page number)
     * @param pageSize 每页大小 (Page size)
     */
    public PagedResult(SearchResult result, int currentPage, int pageSize) {
        this.searchResult = result;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) result.getTotalHits() / pageSize);
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }
    
    /**
     * 获取总命中数 (Get total hits)
     * 
     * @return 总命中数 (Total hits)
     */
    public long getTotalHits() {
        return searchResult.getTotalHits();
    }
    
    /**
     * 获取查询时间（毫秒）(Get query time in milliseconds)
     * 
     * @return 查询时间（毫秒）(Query time in milliseconds)
     */
    public long getQueryTimeMs() {
        return searchResult.getQueryTimeMs();
    }
    
    /**
     * 获取文档列表 (Get document list)
     * 
     * @return 文档列表 (Document list)
     */
    public java.util.List<top.yumbo.ai.rag.model.ScoredDocument> getDocuments() {
        return searchResult.getScoredDocuments();
    }
}
