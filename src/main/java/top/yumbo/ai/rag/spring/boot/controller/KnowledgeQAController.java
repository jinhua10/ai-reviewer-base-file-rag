package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.service.KnowledgeQAService;
import top.yumbo.ai.rag.spring.boot.service.SimilarQAService;
import top.yumbo.ai.rag.spring.boot.service.QAArchiveService;
import top.yumbo.ai.rag.model.Document;

import java.util.List;
import java.util.Map;
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
    private final SimilarQAService similarQAService;
    private final QAArchiveService qaArchiveService;

    @Autowired
    public KnowledgeQAController(KnowledgeQAService qaService,
                                 SimilarQAService similarQAService,
                                 QAArchiveService qaArchiveService) {
        this.qaService = qaService;
        this.similarQAService = similarQAService;
        this.qaArchiveService = qaArchiveService;
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
        response.setSessionId(answer.getSessionId());
        response.setUsedDocuments(answer.getUsedDocuments());
        response.setTotalRetrieved(answer.getTotalRetrieved());
        response.setHasMoreDocuments(answer.isHasMoreDocuments());
        response.setRecordId(answer.getRecordId());
        response.setSimilarQuestions(answer.getSimilarQuestions());  // 新增：相似问题

        return response;
    }

    /**
     * 使用会话文档进行问答（用于分页引用）
     */
    @PostMapping("/ask-with-session")
    public QuestionResponse askWithSession(@RequestBody SessionQuestionRequest request) {
        log.info("使用会话进行问答: 问题={}, sessionId={}", request.getQuestion(), request.getSessionId());

        AIAnswer answer = qaService.askWithSessionDocuments(request.getQuestion(), request.getSessionId());

        QuestionResponse response = new QuestionResponse();
        response.setQuestion(request.getQuestion());
        response.setAnswer(answer.getAnswer());
        response.setSources(answer.getSources());
        response.setResponseTimeMs(answer.getResponseTimeMs());
        response.setSessionId(answer.getSessionId());
        response.setUsedDocuments(answer.getUsedDocuments());
        response.setTotalRetrieved(answer.getTotalRetrieved());
        response.setHasMoreDocuments(answer.isHasMoreDocuments());
        response.setRecordId(answer.getRecordId());

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
     * 获取知识库统计信息（增强版）
     * 实时扫描文件系统，返回准确的文档数量
     */
    @GetMapping("/statistics")
    public StatisticsResponse getStatistics() {
        log.info("📊 获取统计信息（增强版）");

        KnowledgeQAService.EnhancedStatistics stats = qaService.getEnhancedStatistics();

        StatisticsResponse response = new StatisticsResponse();
        response.setDocumentCount(stats.getDocumentCount());
        response.setIndexedDocumentCount(stats.getIndexedDocumentCount());
        response.setUnindexedCount(stats.getUnindexedCount());
        response.setIndexProgress(stats.getIndexProgress());

        // 添加提示信息
        if (stats.getUnindexedCount() > 0) {
            response.setMessage(String.format(
                "检测到 %d 个未索引的文档。建议执行增量索引以更新知识库。",
                stats.getUnindexedCount()
            ));
            response.setNeedsIndexing(true);
        } else {
            response.setMessage("所有文档均已索引，知识库状态良好。");
            response.setNeedsIndexing(false);
        }

        log.info("📊 统计信息 - 文档总数: {}, 已索引: {}, 未索引: {}, 完成度: {}%",
            stats.getDocumentCount(), stats.getIndexedDocumentCount(),
            stats.getUnindexedCount(), stats.getIndexProgress());

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

    /**
     * 搜索相似问题（基于关键词匹配）
     * 在历史问答记录中查找相似问题
     */
    @GetMapping("/similar")
    public ResponseEntity<?> findSimilarQuestions(
            @RequestParam String question,
            @RequestParam(defaultValue = "30") int minScore,  // 最小相似度分数（0-100）
            @RequestParam(defaultValue = "5") int limit) {

        log.info("🔍 搜索相似问题: {} (minScore={}, limit={})", question, minScore, limit);

        List<SimilarQAService.SimilarQA> similar =
            similarQAService.findSimilar(question, minScore, limit);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", similar.size(),
            "similarQuestions", similar
        ));
    }

    /**
     * 获取归档统计
     * 返回归档问答的统计信息
     */
    @GetMapping("/archive/statistics")
    public ResponseEntity<?> getArchiveStatistics() {
        log.info("📊 获取归档统计");

        var stats = qaArchiveService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    // ========== DTO 类 ==========

    @Data
    public static class QuestionRequest {
        private String question;
    }

    @Data
    public static class SessionQuestionRequest {
        private String question;
        private String sessionId;
    }

    @Data
    public static class QuestionResponse {
        private String question;
        private String answer;
        private List<String> sources;
        private long responseTimeMs;
        private String sessionId;              // 会话ID
        private List<String> usedDocuments;    // 本次使用的文档
        private int totalRetrieved;            // 检索到的总文档数
        private boolean hasMoreDocuments;      // 是否还有更多文档
        private String recordId;               // 记录ID（用于反馈）
        private List<SimilarQAService.SimilarQA> similarQuestions;  // 相似问题推荐
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
        private long documentCount;          // 文件系统中的文档总数
        private long indexedDocumentCount;   // 已索引的文档数量
        private long unindexedCount;         // 未索引的文档数量
        private int indexProgress;           // 索引完成度百分比 (0-100)
        private String message;              // 提示信息
        private boolean needsIndexing;       // 是否需要执行索引
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

