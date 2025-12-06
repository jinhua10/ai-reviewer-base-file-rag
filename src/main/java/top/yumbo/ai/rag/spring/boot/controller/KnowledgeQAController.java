package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
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
 * 知识库问答 REST API 控制器 / Knowledge QA REST API Controller
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
     * 问答接口 / Question answering endpoint
     */
    @PostMapping("/ask")
    public QuestionResponse ask(@RequestBody QuestionRequest request) {
        log.info(I18N.get("knowledge_qa.log.received_question", request.getQuestion()));

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
        response.setSimilarQuestions(answer.getSimilarQuestions());  // 新增：相似问题 / New: similar questions

        return response;
    }

    /**
     * 使用会话文档进行问答（用于分页引用）/ QA with session documents (for pagination)
     */
    @PostMapping("/ask-with-session")
    public QuestionResponse askWithSession(@RequestBody SessionQuestionRequest request) {
        log.info(I18N.get("knowledge_qa.log.session_question",
            request.getQuestion(), request.getSessionId()));

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
     * 搜索文档接口 / Search documents endpoint
     */
    @GetMapping("/search")
    public SearchResponse search(@RequestParam String query,
                                 @RequestParam(defaultValue = "10") int limit) {
        log.info(I18N.get("knowledge_qa.log.search_documents", query, limit));

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
     * 获取知识库统计信息（增强版）/ Get knowledge base statistics (enhanced)
     * 实时扫描文件系统，返回准确的文档数量 / Real-time scan filesystem, return accurate document count
     */
    @GetMapping("/statistics")
    public StatisticsResponse getStatistics(@RequestParam(value = "lang", defaultValue = "zh") String lang) {
        log.info(I18N.get("knowledge_qa.log.get_statistics"));

        KnowledgeQAService.EnhancedStatistics stats = qaService.getEnhancedStatistics();

        StatisticsResponse response = new StatisticsResponse();
        response.setDocumentCount(stats.getDocumentCount());
        response.setIndexedDocumentCount(stats.getIndexedDocumentCount());
        response.setUnindexedCount(stats.getUnindexedCount());
        response.setIndexProgress(stats.getIndexProgress());

        // 添加提示信息 / Add hint message
        if (stats.getUnindexedCount() > 0) {
            response.setMessage(I18N.getLang(
                "knowledge_qa.api.message.needs_indexing", lang, stats.getUnindexedCount()));
            response.setNeedsIndexing(true);
        } else {
            response.setMessage(I18N.getLang(
                "knowledge_qa.api.message.all_indexed", lang));
            response.setNeedsIndexing(false);
        }

        log.info(I18N.get("knowledge_qa.log.statistics_result",
            stats.getDocumentCount(), stats.getIndexedDocumentCount(),
            stats.getUnindexedCount(), stats.getIndexProgress()));

        return response;
    }

    /**
     * 健康检查 / Health check
     */
    @GetMapping("/health")
    public HealthResponse health(@RequestParam(value = "lang", defaultValue = "zh") String lang) {
        HealthResponse response = new HealthResponse();
        response.setStatus(I18N.getLang("knowledge_qa.api.status.up", lang));
        response.setMessage(I18N.getLang("knowledge_qa.api.message.system_running", lang));
        return response;
    }

    /**
     * 触发知识库重建（管理接口）/ Trigger knowledge base rebuild (admin endpoint)
     */
    @PostMapping("/rebuild")
    public RebuildResponse rebuild(@RequestBody(required = false) Map<String, String> request) {
        String lang = request != null ? request.getOrDefault("lang", "zh") : "zh"; // 获取语言参数 / Get language parameter
        log.info(I18N.get("knowledge_qa.log.rebuild_request"));

        try {
            BuildResult result = qaService.rebuildKnowledgeBase();

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(true);
            response.setMessage(I18N.getLang("knowledge_qa.api.message.rebuild_complete", lang));
            response.setProcessedFiles(result.getSuccessCount());
            response.setTotalDocuments(result.getTotalDocuments());
            response.setDurationMs(result.getBuildTimeMs());

            return response;
        } catch (Exception e) {
            log.error(I18N.get("knowledge_qa.log.rebuild_failed"), e);

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(false);
            response.setMessage(I18N.getLang("knowledge_qa.api.message.rebuild_failed", lang, e.getMessage()));
            response.setSuggestion(I18N.getLang("knowledge_qa.api.message.rebuild_suggestion", lang));

            return response;
        }
    }

    /**
     * 触发知识库增量索引（管理接口）/ Trigger knowledge base incremental index (admin endpoint)
     * 只处理新增和修改的文档，性能更优 / Only process new and modified documents, better performance
     */
    @PostMapping("/incremental-index")
    public RebuildResponse incrementalIndex(@RequestBody(required = false) Map<String, String> request) {
        String lang = request != null ? request.getOrDefault("lang", "zh") : "zh"; // 获取语言参数 / Get language parameter
        log.info(I18N.get("knowledge_qa.log.incremental_request"));

        try {
            BuildResult result = qaService.incrementalIndexKnowledgeBase();

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(true);

            if (result.getSuccessCount() > 0) {
                response.setMessage(I18N.getLang(
                    "knowledge_qa.api.message.incremental_complete", lang, result.getSuccessCount()));
            } else {
                response.setMessage(I18N.getLang(
                    "knowledge_qa.api.message.all_up_to_date", lang));
            }

            response.setProcessedFiles(result.getSuccessCount());
            response.setTotalDocuments(result.getTotalDocuments());
            response.setDurationMs(result.getBuildTimeMs());

            return response;
        } catch (Exception e) {
            log.error(I18N.get("knowledge_qa.log.incremental_failed"), e);

            RebuildResponse response = new RebuildResponse();
            response.setSuccess(false);
            response.setMessage(I18N.getLang("knowledge_qa.api.message.incremental_failed", lang, e.getMessage()));
            response.setSuggestion(I18N.getLang("knowledge_qa.api.message.rebuild_suggestion", lang));

            return response;
        }
    }

    /**
     * 搜索相似问题（基于关键词匹配）/ Search similar questions (based on keyword matching)
     * 在历史问答记录中查找相似问题 / Search similar questions in historical QA records
     */
    @GetMapping("/similar")
    public ResponseEntity<?> findSimilarQuestions(
            @RequestParam String question,
            @RequestParam(defaultValue = "30") int minScore,  // 最小相似度分数（0-100）/ Min similarity score (0-100)
            @RequestParam(defaultValue = "5") int limit) {

        log.info(I18N.get("knowledge_qa.log.search_similar", question, minScore, limit));

        List<SimilarQAService.SimilarQA> similar =
            similarQAService.findSimilar(question, minScore, limit);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", similar.size(),
            "similarQuestions", similar
        ));
    }

    /**
     * 获取归档统计 / Get archive statistics
     * 返回归档问答的统计信息 / Return statistics of archived QA
     */
    @GetMapping("/archive/statistics")
    public ResponseEntity<?> getArchiveStatistics() {
        log.info(I18N.get("knowledge_qa.log.archive_stats"));

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


