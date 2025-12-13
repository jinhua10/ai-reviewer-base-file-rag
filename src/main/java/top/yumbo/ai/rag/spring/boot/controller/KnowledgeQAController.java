package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.model.BuildResult;
import top.yumbo.ai.rag.spring.boot.service.KnowledgeQAService;
import top.yumbo.ai.rag.spring.boot.service.RoleKnowledgeQAService;
import top.yumbo.ai.rag.spring.boot.service.SimilarQAService;
import top.yumbo.ai.rag.spring.boot.service.QAArchiveService;
import top.yumbo.ai.rag.spring.boot.streaming.HybridStreamingService;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
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
    private final RoleKnowledgeQAService roleKnowledgeQAService;
    private final HybridStreamingService hybridStreamingService;

    @Autowired
    public KnowledgeQAController(KnowledgeQAService qaService,
                                 SimilarQAService similarQAService,
                                 QAArchiveService qaArchiveService,
                                 RoleKnowledgeQAService roleKnowledgeQAService,
                                 HybridStreamingService hybridStreamingService) {
        this.qaService = qaService;
        this.similarQAService = similarQAService;
        this.qaArchiveService = qaArchiveService;
        this.roleKnowledgeQAService = roleKnowledgeQAService;
        this.hybridStreamingService = hybridStreamingService;
    }

    /**
     * 智能问答接口（统一入口）/ Intelligent Q&A endpoint (unified entry)
     * 
     * 根据参数自动路由到对应的处理逻辑：
     * - knowledgeMode="none": 直接调用 LLM 回答（不检索）
     * - knowledgeMode="rag": 使用传统 RAG 检索知识库回答
     * - knowledgeMode="role": 使用角色知识库回答
     *
     * @param request 问题请求（包含 knowledgeMode 和 roleName 参数）
     * @return 统一响应格式
     */
    @PostMapping("/ask")
    public QuestionResponse ask(@RequestBody QuestionRequest request) {
        // 解析知识库模式 (Parse knowledge mode)
        String knowledgeMode = request.getKnowledgeMode();
        boolean useKnowledgeBase = request.getUseKnowledgeBase() != null ? request.getUseKnowledgeBase() : true;
        
        // 如果指定了 knowledgeMode，优先使用 (If knowledgeMode is specified, use it with priority)
        if (knowledgeMode != null && !knowledgeMode.isEmpty()) {
            useKnowledgeBase = !"none".equals(knowledgeMode);
        }

        String roleName = request.getRoleName();
        boolean useRoleKnowledge = "role".equals(knowledgeMode);

        log.info(I18N.get("knowledge_qa.log.received_question", request.getQuestion()) +
                 " [mode: " + knowledgeMode + ", role: " + roleName + ", RAG: " + useKnowledgeBase + "]");

        AIAnswer answer;
        
        if (!useKnowledgeBase) {
            // 直接 LLM 模式（不使用 RAG）/ Direct LLM mode (without RAG)
            answer = qaService.askDirectLLM(request.getQuestion());
        } else if (useRoleKnowledge && roleName != null && !roleName.isEmpty()) {
            // 使用角色知识库模式 / Use role-based knowledge base mode
            log.info(I18N.get("role.knowledge.api.role-mode"), roleName);
            answer = roleKnowledgeQAService.askWithRole(request.getQuestion(), roleName);
        } else {
            // 使用知识库 RAG 模式 / Use knowledge base RAG mode
            answer = qaService.ask(request.getQuestion(), request.getHopeSessionId());
        }
        
        // 支持 HOPE 会话ID / Support HOPE session ID
        // AIAnswer answer = qaService.ask(request.getQuestion(), request.getHopeSessionId());

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
        response.setSimilarQuestions(answer.getSimilarQuestions());

        // 新增 HOPE 相关字段 / New: HOPE related fields
        response.setHopeSource(answer.getHopeSource());
        response.setDirectAnswer(answer.isDirectAnswer());
        response.setStrategyUsed(answer.getStrategyUsed());
        response.setHopeConfidence(answer.getHopeConfidence());

        return response;
    }

    /**
     * 智能问答接口 - 双轨流式版本 / Intelligent Q&A endpoint - Dual-track Streaming version
     * <p>
     * 双轨架构：
     * 1. 立即返回 HOPE 快速答案（<300ms）
     * 2. 返回 SSE URL 用于订阅 LLM 详细答案（流式）
     * <p>
     * Dual-track architecture:
     * 1. Immediately return HOPE fast answer (<300ms)
     * 2. Return SSE URL for subscribing to LLM detailed answer (streaming)
     * <p>
     * 支持三种知识库模式：
     * - knowledgeMode="none": 直接 LLM
     * - knowledgeMode="rag": 传统 RAG
     * - knowledgeMode="role": 角色知识库
     *
     * @param request 问题请求
     * @return 会话信息 + HOPE 快速答案 + SSE URL
     */
    @PostMapping("/ask-stream")
    public ResponseEntity<Map<String, Object>> askStream(@RequestBody QuestionRequest request) {
        // 解析知识库模式 (Parse knowledge mode)
        String knowledgeMode = request.getKnowledgeMode();
        boolean useKnowledgeBase = request.getUseKnowledgeBase() != null ? request.getUseKnowledgeBase() : true;

        if (knowledgeMode != null && !knowledgeMode.isEmpty()) {
            useKnowledgeBase = !"none".equals(knowledgeMode);
        }

        String roleName = request.getRoleName();
        boolean useRoleKnowledge = "role".equals(knowledgeMode);

        log.info(I18N.get("knowledge_qa.log.received_question", request.getQuestion()) +
                 " [mode: " + knowledgeMode + ", role: " + roleName + ", RAG: " + useKnowledgeBase + ", dual-track: true]");

        try {
            // 启动双轨响应 (Start dual-track response)
            var response = hybridStreamingService.ask(request.getQuestion(), "user", useKnowledgeBase);

            // 等待 HOPE 快速答案 (Wait for HOPE fast answer)
            HOPEAnswer hopeAnswer = null;
            try {
                hopeAnswer = response.getHopeFuture().get();
            } catch (Exception e) {
                log.warn("获取 HOPE 答案失败 (Failed to get HOPE answer): {}", e.getMessage());
            }

            // 返回会话信息 (Return session info)
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("sessionId", response.getSessionId());
            result.put("question", response.getQuestion());
            result.put("hopeAnswer", hopeAnswer);
            result.put("sseUrl", "/api/qa/stream/" + response.getSessionId());
            result.put("knowledgeMode", knowledgeMode);
            result.put("roleName", roleName);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(I18N.get("role.knowledge.api.streaming-failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 订阅 LLM 流式输出 / Subscribe to LLM streaming output
     * <p>
     * 用于接收双轨架构中的 LLM 详细答案（流式）
     * (Used to receive LLM detailed answer in dual-track architecture)
     *
     * @param sessionId 会话ID
     * @return SSE 流
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream(@PathVariable String sessionId) {
        log.info("📡 客户端订阅流式输出 (Client subscribed to streaming): sessionId={}", sessionId);

        SseEmitter emitter = hybridStreamingService.createSSEStream(sessionId);

        if (emitter == null) {
            log.warn("会话不存在 (Session not found): sessionId={}", sessionId);
            emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Session not found"));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送错误失败 (Failed to send error): {}", e.getMessage());
            }
        }

        return emitter;
    }

    /**
     * 使用会话文档进行问答（用于分页引用）/ QA with session documents (for pagination)
     * 
     * 支持知识库模式：
     * - knowledgeMode="none": 直接 LLM 回答
     * - knowledgeMode="rag": 使用会话文档 RAG 检索
     * - knowledgeMode="role": 使用角色知识库
     */
    @PostMapping("/ask-with-session")
    public QuestionResponse askWithSession(@RequestBody SessionQuestionRequest request) {
        // 解析知识库模式 (Parse knowledge mode)
        String knowledgeMode = request.getKnowledgeMode();
        boolean useKnowledgeBase = request.getUseKnowledgeBase() != null ? request.getUseKnowledgeBase() : true;
        
        // 如果指定了 knowledgeMode，优先使用 (If knowledgeMode is specified, use it with priority)
        if (knowledgeMode != null && !knowledgeMode.isEmpty()) {
            useKnowledgeBase = !"none".equals(knowledgeMode);
        }

        String roleName = request.getRoleName();
        boolean useRoleKnowledge = "role".equals(knowledgeMode);

        log.info(I18N.get("knowledge_qa.log.session_question",
            request.getQuestion(), request.getSessionId()) +
            " [mode: " + knowledgeMode + ", role: " + roleName + ", RAG: " + useKnowledgeBase + "]");

        AIAnswer answer;
        
        if (!useKnowledgeBase) {
            // 直接 LLM 模式（不使用会话文档）/ Direct LLM mode (without session documents)
            answer = qaService.askDirectLLM(request.getQuestion());
        } else if (useRoleKnowledge && roleName != null && !roleName.isEmpty()) {
            // 使用角色知识库模式 / Use role-based knowledge base mode
            log.info(I18N.get("role.knowledge.api.role-mode-session"), roleName);
            answer = roleKnowledgeQAService.askWithRole(request.getQuestion(), roleName);
        } else {
            // 使用会话文档 RAG 模式 / Use session documents RAG mode
            answer = qaService.askWithSessionDocuments(request.getQuestion(), request.getSessionId());
        }

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
     * 检查索引状态 / Check indexing status
     */
    @GetMapping("/indexing-status")
    public IndexingStatusResponse checkIndexingStatus(@RequestParam(value = "lang", defaultValue = "zh") String lang) {
        IndexingStatusResponse response = new IndexingStatusResponse();
        response.setIndexing(qaService.isIndexing());

        if (response.isIndexing()) {
            response.setMessage(I18N.getLang("knowledge_qa.log.indexing_in_progress", lang));
        } else {
            response.setMessage(I18N.getLang("knowledge_qa.log.indexing_idle", lang));
        }

        return response;
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

    /**
     * 获取角色贡献排行榜 / Get role contribution leaderboard
     */
    @GetMapping("/role/leaderboard")
    public ResponseEntity<?> getRoleLeaderboard() {
        log.info(I18N.get("role.knowledge.api.get-leaderboard"));

        List<RoleKnowledgeQAService.RoleCredit> leaderboard =
            roleKnowledgeQAService.getLeaderboard();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "leaderboard", leaderboard
        ));
    }

    /**
     * 获取活跃悬赏列表 / Get active bounties
     */
    @GetMapping("/bounty/active")
    public ResponseEntity<?> getActiveBounties() {
        log.info(I18N.get("role.knowledge.api.get-bounties"));

        List<RoleKnowledgeQAService.BountyRequest> bounties =
            roleKnowledgeQAService.getActiveBounties();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "count", bounties.size(),
            "bounties", bounties
        ));
    }

    /**
     * 提交悬赏答案 / Submit bounty answer
     */
    @PostMapping("/bounty/{bountyId}/submit")
    public ResponseEntity<?> submitBountyAnswer(
            @PathVariable String bountyId,
            @RequestBody BountySubmitRequest request,
            @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        log.info(I18N.get("role.knowledge.api.submit-bounty"), bountyId, request.getRoleName());

        try {
            RoleKnowledgeQAService.BountySubmission submission =
                roleKnowledgeQAService.submitBountyAnswer(
                    bountyId,
                    request.getRoleName(),
                    request.getAnswer(),
                    request.getSources()
                );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", I18N.getLang("role.knowledge.api.submit-success", lang),
                "submission", submission
            ));
        } catch (Exception e) {
            log.error(I18N.get("role.knowledge.api.submit-bounty-failed"), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ========== DTO 类 ==========

    @Data
    public static class QuestionRequest {
        private String question;
        private String hopeSessionId;  // HOPE 会话ID（用于上下文增强）
        private Boolean useKnowledgeBase;  // true: RAG模式, false: 直接LLM, null: 默认RAG（兼容旧版）

        /**
         * 知识库模式 (Knowledge base mode)
         * 可选值 (Options):
         * - "none": 不使用RAG，直接LLM (Direct LLM without RAG)
         * - "rag": 使用传统RAG (Traditional RAG)
         * - "role": 使用角色知识库 (Role-based knowledge base)
         * null 或空表示使用传统RAG (null or empty means traditional RAG)
         */
        private String knowledgeMode;

        /**
         * 角色名称 (Role name)
         * 当 knowledgeMode="role" 时使用
         * (Used when knowledgeMode="role")
         * 例如: developer, devops, architect, general 等
         */
        private String roleName;
    }

    @Data
    public static class SessionQuestionRequest {
        private String question;
        private String sessionId;
        private Boolean useKnowledgeBase;  // true: RAG模式, false: 直接LLM, null: 默认RAG（兼容旧版）

        /**
         * 知识库模式 (Knowledge base mode)
         */
        private String knowledgeMode;

        /**
         * 角色名称 (Role name)
         */
        private String roleName;
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

        // HOPE 相关字段 / HOPE related fields
        private String hopeSource;             // HOPE 来源层
        private boolean directAnswer;          // 是否为直接回答
        private String strategyUsed;           // 使用的策略
        private double hopeConfidence;         // HOPE 置信度
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

    @Data
    public static class IndexingStatusResponse {
        private boolean indexing;
        private String message;
    }

    @Data
    public static class BountySubmitRequest {
        private String roleName;
        private String answer;
        private List<String> sources;
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
