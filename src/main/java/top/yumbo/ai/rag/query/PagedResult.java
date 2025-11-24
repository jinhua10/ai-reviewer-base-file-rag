package top.yumbo.ai.rag.query;
import lombok.Getter;
import top.yumbo.ai.rag.model.SearchResult;
@Getter
public class PagedResult {
    private final SearchResult searchResult;
    private final int currentPage;
    private final int pageSize;
    private final int totalPages;
    private final boolean hasNext;
    private final boolean hasPrevious;
    public PagedResult(SearchResult result, int currentPage, int pageSize) {
        this.searchResult = result;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) result.getTotalHits() / pageSize);
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }
    public long getTotalHits() {
        return searchResult.getTotalHits();
    }
    public long getQueryTimeMs() {
        return searchResult.getQueryTimeMs();
    }
    public java.util.List<top.yumbo.ai.rag.model.Document> getDocuments() {
        return searchResult.getDocuments();
    }
}
