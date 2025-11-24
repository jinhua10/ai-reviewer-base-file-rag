package top.yumbo.ai.rag.example.simple;

import lombok.Data;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.spring.boot.autoconfigure.SimpleRAGService;

import java.util.List;
import java.util.Map;

/**
 * 极简 REST API
 *
 * 只需注入 SimpleRAGService 即可使用所有功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@RestController
@RequestMapping("/api/rag")
public class SimpleRAGController {

    private final SimpleRAGService ragService;

    public SimpleRAGController(SimpleRAGService ragService) {
        this.ragService = ragService;
    }

    /**
     * 索引文档
     * POST /api/rag/index
     */
    @PostMapping("/index")
    public IndexResponse index(@RequestBody IndexRequest request) {
        String docId = ragService.index(
            request.getTitle(),
            request.getContent(),
            request.getMetadata() != null ? request.getMetadata() : Map.of()
        );
        ragService.commit();

        return new IndexResponse(docId, "索引成功");
    }

    /**
     * 搜索文档
     * GET /api/rag/search?q=关键词&limit=10
     */
    @GetMapping("/search")
    public SearchResponse search(
        @RequestParam String q,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<Document> results = ragService.search(q, limit);
        return new SearchResponse(q, results.size(), results);
    }

    /**
     * 获取统计
     * GET /api/rag/stats
     */
    @GetMapping("/stats")
    public StatsResponse stats() {
        var stats = ragService.getStatistics();
        return new StatsResponse(
            stats.getDocumentCount(),
            stats.getIndexedDocumentCount()
        );
    }

    // DTO 类
    @Data
    public static class IndexRequest {
        private String title;
        private String content;
        private Map<String, Object> metadata;
    }

    @Data
    public static class IndexResponse {
        private final String docId;
        private final String message;
    }

    @Data
    public static class SearchResponse {
        private final String query;
        private final int total;
        private final List<Document> documents;
    }

    @Data
    public static class StatsResponse {
        private final long documentCount;
        private final long indexedCount;
    }
}

