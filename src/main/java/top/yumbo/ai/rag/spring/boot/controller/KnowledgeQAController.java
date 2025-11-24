package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.service.KnowledgeQAService;
import top.yumbo.ai.rag.model.Document;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库问答 REST API 控制器
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
public class KnowledgeQAController {

    private final KnowledgeQAService qaService;

    public KnowledgeQAController(KnowledgeQAService qaService) {
        this.qaService = qaService;
    }

    /**
     * 问答接口
     */
    @PostMapping("/ask")
    public QuestionResponse ask(@RequestBody QuestionRequest request) {
        log.info("收到问题: {}", request.getQuestion());

        AIAnswer answer = qaService.ask(request.getQuestion());

        QuestionResponse response = new QuestionResponse();
        response.setQuestion(request.getQuestion());
        response.setAnswer(answer.getAnswer());
        response.setSources(answer.getSources());
        response.setResponseTimeMs(answer.getResponseTimeMs());

        return response;
    }

    /**
     * 搜索文档接口
     */
    @GetMapping("/search")
    public SearchResponse search(@RequestParam String query,
                                 @RequestParam(defaultValue = "10") int limit) {
        log.info("搜索文档: {} (limit={})", query, limit);

        List<Document> documents = qaService.searchDocuments(query, limit);

        SearchResponse response = new SearchResponse();
        response.setQuery(query);
        response.setTotal(documents.size());
        response.setDocuments(documents.stream()
            .map(this::toDocumentInfo)
            .collect(Collectors.toList()));

        return response;
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/statistics")
    public StatisticsResponse getStatistics() {
        LocalFileRAG.Statistics stats = qaService.getStatistics();

        StatisticsResponse response = new StatisticsResponse();
        response.setDocumentCount(stats.getDocumentCount());
        response.setIndexedDocumentCount(stats.getIndexedDocumentCount());

        return response;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public HealthResponse health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setMessage("知识库问答系统运行正常");
        return response;
    }

    /**
     * 触发知识库重建（管理接口）
     */
    @PostMapping("/rebuild")
    public RebuildResponse rebuild() {
        log.info("收到知识库重建请求");

        try {
            BuildResult result = qaService.rebuildKnowledgeBase();

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(true);
            response.setMessage("知识库重建完成");
            response.setProcessedFiles(result.getSuccessCount());
            response.setTotalDocuments(result.getTotalDocuments());
            response.setDurationMs(result.getBuildTimeMs());

            return response;
        } catch (Exception e) {
            log.error("知识库重建失败", e);

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(false);
            response.setMessage("知识库重建失败: " + e.getMessage());
            response.setSuggestion("请检查日志文件获取详细错误信息");

            return response;
        }
    }

    /**
     * 触发知识库增量索引（管理接口）
     * 只处理新增和修改的文档，性能更优
     */
    @PostMapping("/incremental-index")
    public RebuildResponse incrementalIndex() {
        log.info("收到知识库增量索引请求");

        try {
            BuildResult result = qaService.incrementalIndexKnowledgeBase();

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(true);

            if (result.getSuccessCount() > 0) {
                response.setMessage(String.format("增量索引完成，更新了 %d 个文件", result.getSuccessCount()));
            } else {
                response.setMessage("所有文件都是最新的，无需更新");
            }

            response.setProcessedFiles(result.getSuccessCount());
            response.setTotalDocuments(result.getTotalDocuments());
            response.setDurationMs(result.getBuildTimeMs());

            return response;
        } catch (Exception e) {
            log.error("增量索引失败", e);

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(false);
            response.setMessage("增量索引失败: " + e.getMessage());
            response.setSuggestion("请检查日志文件获取详细错误信息");

            return response;
        }
    }

    // ========== DTO 类 ==========

    @Data
    public static class QuestionRequest {
        private String question;
    }

    @Data
    public static class QuestionResponse {
        private String question;
        private String answer;
        private List<String> sources;
        private long responseTimeMs;
    }

    @Data
    public static class SearchResponse {
        private String query;
        private int total;
        private List<DocumentInfo> documents;
    }

    @Data
    public static class DocumentInfo {
        private String id;
        private String title;
        private String content;
        private String excerpt;
    }

    @Data
    public static class StatisticsResponse {
        private long documentCount;
        private long indexedDocumentCount;
    }

    @Data
    public static class HealthResponse {
        private String status;
        private String message;
    }

    @Data
    public static class RebuildResponse {
        private boolean success;
        private String message;
        private String suggestion;
        private int processedFiles;
        private int totalDocuments;
        private long durationMs;
    }

    // ========== 辅助方法 ==========

    private DocumentInfo toDocumentInfo(Document doc) {
        DocumentInfo info = new DocumentInfo();
        info.setId(doc.getId());
        info.setTitle(doc.getTitle());
        info.setContent(doc.getContent());

        // 生成摘要（前200字符）
        String content = doc.getContent();
        String excerpt = content.length() > 200
            ? content.substring(0, 200) + "..."
            : content;
        info.setExcerpt(excerpt);

        return info;
    }
}

