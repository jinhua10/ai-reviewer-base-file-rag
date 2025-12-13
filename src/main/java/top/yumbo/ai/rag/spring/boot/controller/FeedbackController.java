package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.feedback.QARecordService;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.List;
import java.util.Map;

/**
 * 用户反馈控制器
 * 处理用户对问答结果的反馈
 *
 * @author AI Reviewer Team
 * @since 2025-11-27
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final QARecordService qaRecordService;
    private final HOPEKnowledgeManager hopeManager;

    @Autowired
    public FeedbackController(QARecordService qaRecordService,
                              @Autowired(required = false) HOPEKnowledgeManager hopeManager) {
        this.qaRecordService = qaRecordService;
        this.hopeManager = hopeManager;
    }

    /**
     * 提交整体反馈 / Submit overall feedback
     */
    @PostMapping("/overall")
    public ResponseEntity<?> submitOverallFeedback(@RequestBody Map<String, Object> request) {
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数，默认中文 / Get language parameter, default Chinese

        try {
            String recordId = (String) request.get("recordId");
            Integer rating = (Integer) request.get("rating");
            String feedback = (String) request.get("feedback");

            if (recordId == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "recordId, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.invalid_rating", lang)
                ));
            }

            boolean success = qaRecordService.addOverallFeedback(recordId, rating, feedback);

            if (success) {
                log.info(I18N.get("log.feedback.overall_received", recordId, rating));

                // 触发 HOPE 学习（评分 >= 4 分时学习）
                // Trigger HOPE learning (when rating >= 4)
                if (hopeManager != null && hopeManager.isEnabled() && rating >= 4) {
                    try {
                        var record = qaRecordService.getRecord(recordId);
                        if (record.isPresent()) {
                            QARecord qaRecord = record.get();
                            String hopeSessionId = (String) request.get("hopeSessionId");
                            hopeManager.learn(qaRecord.getQuestion(), qaRecord.getAnswer(), rating, hopeSessionId);
                            log.info(I18N.get("hope.learn.recorded", rating));
                        }
                    } catch (Exception e) {
                        log.warn("HOPE learning failed: {}", e.getMessage());
                    }
                }

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", I18N.getLang("feedback.api.success.feedback_received", lang)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.record_not_found", lang)
                ));
            }

        } catch (Exception e) {
            log.error(I18N.get("log.feedback.overall_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 提交文档反馈 / Submit document feedback
     */
    @PostMapping("/document")
    public ResponseEntity<?> submitDocumentFeedback(@RequestBody Map<String, Object> request) {
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数，默认中文 / Get language parameter, default Chinese

        try {
            String recordId = (String) request.get("recordId");
            String documentName = (String) request.get("documentName");
            String feedbackType = (String) request.get("feedbackType");
            String reason = (String) request.get("reason");

            if (recordId == null || documentName == null || feedbackType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "recordId, documentName, feedbackType")
                ));
            }

            QARecord.FeedbackType type;
            try {
                type = QARecord.FeedbackType.valueOf(feedbackType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.invalid_feedback_type", lang)
                ));
            }

            boolean success = qaRecordService.addDocumentFeedback(recordId, documentName, type, reason);

            if (success) {
                String emoji = type == QARecord.FeedbackType.LIKE ? "👍" : "👎";
                log.info(I18N.get("log.feedback.document_received", emoji, recordId, documentName));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", I18N.getLang("feedback.api.success.feedback_received", lang)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.record_not_found", lang)
                ));
            }

        } catch (Exception e) {
            log.error(I18N.get("log.feedback.document_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取问答记录 / Get QA record
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<?> getRecord(@PathVariable String recordId, @RequestParam(defaultValue = "zh") String lang) {
        try {
            var record = qaRecordService.getRecord(recordId);

            if (record.isPresent()) {
                return ResponseEntity.ok(record.get());
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error(I18N.get("log.feedback.get_record_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取最近的问答记录 / Get recent QA records
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentRecords(@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "zh") String lang) {
        try {
            List<QARecord> records = qaRecordService.getRecentRecords(limit);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error(I18N.get("log.feedback.get_recent_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取待审核的记录 / Get pending records
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRecords(@RequestParam(defaultValue = "zh") String lang) {
        try {
            List<QARecord> records = qaRecordService.getPendingRecords();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error(I18N.get("log.feedback.get_pending_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取统计信息 / Get statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(@RequestParam(defaultValue = "zh") String lang) {
        try {
            var stats = qaRecordService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error(I18N.get("log.feedback.get_statistics_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 星级评价文档有用性（用户友好的评分接口）/ Rate document quality (user-friendly rating API)
     *
     * @param request 包含 recordId, documentName, rating (1-5星), lang / Contains recordId, documentName, rating (1-5 stars), lang
     * @return 响应结果 / Response result
     */
    @PostMapping("/document/rate")
    public ResponseEntity<?> rateDocumentQuality(@RequestBody Map<String, Object> request) {
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数，默认中文 / Get language parameter, default Chinese

        try {
            String recordId = (String) request.get("recordId");
            String documentName = (String) request.get("documentName");
            Integer rating = (Integer) request.get("rating");
            String comment = (String) request.get("comment");

            // 参数验证 / Parameter validation
            if (recordId == null || documentName == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "recordId, documentName, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.invalid_rating", lang)
                ));
            }

            // 调用服务层处理星级评价 / Call service layer to process rating
            boolean success = qaRecordService.addDocumentRating(recordId, documentName, rating, comment);

            if (success) {
                String stars = "⭐".repeat(rating);
                log.info(I18N.get("log.feedback.rating_submitted", stars, recordId, documentName, rating, String.format("%+.1f", 0.0)));

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", I18N.getLang("feedback.api.message.thank_you", lang),
                    "rating", rating,
                    "impact", I18N.getLang("feedback.api.message.document_impact", lang, rating)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.record_not_found", lang)
                ));
            }

        } catch (Exception e) {
            log.error(I18N.get("log.feedback.rating_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 表情评价整体回答质量（用户友好的评分接口）/ Emoji rating for overall answer quality
     *
     * @param request 包含 recordId, rating (1-5), lang / Contains recordId, rating (1-5), lang
     * @return 响应结果 / Response result
     */
    @PostMapping("/overall/rate")
    public ResponseEntity<?> rateOverallQuality(@RequestBody Map<String, Object> request) {
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数，默认中文 / Get language parameter, default Chinese

        try {
            String recordId = (String) request.get("recordId");
            Integer rating = (Integer) request.get("rating");

            // 参数验证 / Parameter validation
            if (recordId == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "recordId, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.invalid_rating", lang)
                ));
            }

            // 调用服务层处理整体评价 / Call service layer to process overall rating
            boolean success = qaRecordService.addOverallRating(recordId, rating);

            if (success) {
                // 记录日志 / Log the rating
                String emojiText = I18N.getLang("feedback.emoji.description." + rating, lang);
                log.info(I18N.get("log.feedback.overall_rating_submitted",
                    emojiText, recordId, rating));

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", I18N.getLang("feedback.api.message.thank_you", lang),
                    "rating", rating,
                    "impact", I18N.getLang("feedback.api.message.overall_impact", lang, rating)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.record_not_found", lang)
                ));
            }

        } catch (Exception e) {
            log.error(I18N.get("log.feedback.rating_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取高赞提示词推荐 / Get highly rated prompt recommendations
     * 根据策略类型返回历史高评分提示词
     *
     * @param strategy 策略类型 (Strategy type)
     * @param limit 限制数量 (Limit)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 提示词推荐列表 (Prompt recommendations)
     */
    @GetMapping("/prompts/recommendations")
    public ResponseEntity<?> getPromptRecommendations(
            @RequestParam(required = false, defaultValue = "all") String strategy,
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            if (limit < 1 || limit > 50) {
                limit = 10; // 默认限制
            }

            log.debug(I18N.get("feedback.prompts.query.start"), strategy, limit);

            List<QARecordService.PromptRecommendation> recommendations = 
                qaRecordService.getTopRatedPrompts(strategy, limit);

            log.info(I18N.get("feedback.prompts.query.success"), recommendations.size(), strategy);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "strategy", strategy,
                "count", recommendations.size(),
                "prompts", recommendations
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.prompts.query.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    // ============================================================================
    // 概念冲突与演化 (Concept Conflicts & Evolution)
    // ============================================================================

    /**
     * 获取冲突列表 / Get conflict list
     *
     * @param status 状态过滤 (Status filter: pending/resolved/all)
     * @param page 页码 (Page number)
     * @param pageSize 每页大小 (Page size)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 冲突列表 (Conflict list)
     */
    @GetMapping("/conflicts")
    public ResponseEntity<?> getConflicts(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            log.debug(I18N.get("feedback.conflicts.query.start"), status, page, pageSize);

            // TODO: 实际实现需要接入概念冲突检测服务
            // For now, return mock data for frontend development
            List<Map<String, Object>> mockConflicts = createMockConflicts();

            // 状态过滤
            List<Map<String, Object>> filteredConflicts = mockConflicts;
            if (!"all".equals(status)) {
                filteredConflicts = mockConflicts.stream()
                    .filter(c -> status.equals(c.get("status")))
                    .toList();
            }

            // 分页
            int total = filteredConflicts.size();
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            List<Map<String, Object>> pagedConflicts = 
                start < total ? filteredConflicts.subList(start, end) : List.of();

            log.info(I18N.get("feedback.conflicts.query.success"), total, pagedConflicts.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "list", pagedConflicts,
                "total", total,
                "page", page,
                "pageSize", pageSize,
                "totalPages", (int) Math.ceil((double) total / pageSize)
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.conflicts.query.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 投票 / Vote on conflict
     *
     * @param request 投票请求 (Vote request: conflictId, choice, userId, reason)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 投票结果 (Vote result)
     */
    @PostMapping("/vote")
    public ResponseEntity<?> vote(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            String conflictId = (String) request.get("conflictId");
            String choice = (String) request.get("choice"); // "A" or "B"
            String userId = (String) request.getOrDefault("userId", "anonymous");
            String reason = (String) request.get("reason");

            if (conflictId == null || choice == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "conflictId, choice")
                ));
            }

            if (!"A".equals(choice) && !"B".equals(choice)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.vote.error.invalid_choice", lang)
                ));
            }

            log.info(I18N.get("feedback.vote.submitted"), userId, conflictId, choice);

            // TODO: 实际实现需要接入投票服务
            // For now, return success for frontend development

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", I18N.getLang("feedback.vote.success", lang),
                "conflictId", conflictId,
                "choice", choice,
                "impact", I18N.getLang("feedback.vote.impact", lang, choice)
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.vote.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取演化历史 / Get evolution history
     *
     * @param conceptId 概念ID (Concept ID)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 演化历史 (Evolution history)
     */
    @GetMapping("/evolution/{conceptId}")
    public ResponseEntity<?> getEvolutionHistory(
            @PathVariable String conceptId,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            log.debug(I18N.get("feedback.evolution.query.start"), conceptId);

            // TODO: 实际实现需要接入概念演化服务
            // For now, return mock data for frontend development
            List<Map<String, Object>> mockHistory = createMockEvolutionHistory(conceptId);

            log.info(I18N.get("feedback.evolution.query.success"), conceptId, mockHistory.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conceptId", conceptId,
                "history", mockHistory
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.evolution.query.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取质量监控数据 / Get quality monitoring data
     *
     * @param lang 语言代码 (Language code: zh/en)
     * @return 质量监控数据 (Quality monitoring data)
     */
    @GetMapping("/quality-monitor")
    public ResponseEntity<?> getQualityMonitor(
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            log.debug(I18N.get("feedback.quality.query.start"));

            // TODO: 实际实现需要接入质量监控服务
            // For now, return mock data for frontend development
            Map<String, Object> mockData = createMockQualityMonitor();

            log.info(I18N.get("feedback.quality.query.success"));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", mockData
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.quality.query.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 提交反馈（通用接口）/ Submit feedback (generic)
     *
     * @param data 反馈数据 (Feedback data)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 提交结果 (Submit result)
     */
    @PostMapping
    public ResponseEntity<?> submitFeedback(
            @RequestBody Map<String, Object> data,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            String type = (String) data.get("type");
            
            if (type == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", I18N.getLang("feedback.api.error.missing_params", lang, "type")
                ));
            }

            log.info(I18N.get("feedback.submit.received"), type);

            // TODO: 根据类型路由到不同的处理逻辑
            // For now, return success for frontend development

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", I18N.getLang("feedback.api.success.feedback_received", lang)
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.submit.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 获取反馈列表（通用接口）/ Get feedback list (generic)
     *
     * @param page 页码 (Page number)
     * @param pageSize 每页大小 (Page size)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 反馈列表 (Feedback list)
     */
    @GetMapping
    public ResponseEntity<?> getFeedbackList(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        
        try {
            log.debug(I18N.get("feedback.list.query.start"), page, pageSize);

            // 使用现有的 recent 记录作为反馈列表
            List<QARecord> records = qaRecordService.getRecentRecords(pageSize);
            
            log.info(I18N.get("feedback.list.query.success"), records.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "list", records,
                "page", page,
                "pageSize", pageSize
            ));

        } catch (Exception e) {
            log.error(I18N.get("feedback.list.query.failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", I18N.getLang("feedback.api.error.processing_failed", lang, e.getMessage())
            ));
        }
    }

    // ============================================================================
    // Mock数据生成方法（用于前端开发）
    // Mock Data Generation Methods (For Frontend Development)
    // ============================================================================

    /**
     * 创建模拟冲突数据 / Create mock conflict data
     */
    private List<Map<String, Object>> createMockConflicts() {
        return List.of(
            Map.of(
                "id", "conflict-1",
                "question", "什么是微服务架构？",
                "conceptA", "微服务是一种将应用程序构建为一系列小型、独立服务的架构风格。",
                "conceptB", "微服务架构是一种分布式系统设计模式，每个服务负责单一业务功能。",
                "status", "pending",
                "votes", Map.of("A", 5, "B", 3),
                "createdAt", System.currentTimeMillis() - 86400000
            ),
            Map.of(
                "id", "conflict-2",
                "question", "如何优化数据库查询性能？",
                "conceptA", "通过添加索引和优化SQL语句来提升查询速度。",
                "conceptB", "使用缓存、读写分离和分库分表等技术优化性能。",
                "status", "voting",
                "votes", Map.of("A", 12, "B", 15),
                "createdAt", System.currentTimeMillis() - 172800000
            ),
            Map.of(
                "id", "conflict-3",
                "question", "什么是RESTful API？",
                "conceptA", "RESTful API是基于REST架构风格的Web服务接口。",
                "conceptB", "REST API使用HTTP方法实现CRUD操作的无状态接口。",
                "status", "resolved",
                "votes", Map.of("A", 8, "B", 20),
                "resolvedChoice", "B",
                "createdAt", System.currentTimeMillis() - 259200000
            )
        );
    }

    /**
     * 创建模拟演化历史数据 / Create mock evolution history data
     */
    private List<Map<String, Object>> createMockEvolutionHistory(String conceptId) {
        return List.of(
            Map.of(
                "id", "evo-1",
                "conceptId", conceptId,
                "version", 1,
                "content", "初始版本：微服务是一种架构风格",
                "author", "system",
                "timestamp", System.currentTimeMillis() - 604800000,
                "reason", "初始创建"
            ),
            Map.of(
                "id", "evo-2",
                "conceptId", conceptId,
                "version", 2,
                "content", "更新版本：微服务是一种将应用程序构建为小型独立服务的架构风格",
                "author", "admin",
                "timestamp", System.currentTimeMillis() - 432000000,
                "reason", "用户反馈优化"
            ),
            Map.of(
                "id", "evo-3",
                "conceptId", conceptId,
                "version", 3,
                "content", "当前版本：微服务是一种分布式架构风格，每个服务独立部署、独立扩展",
                "author", "expert",
                "timestamp", System.currentTimeMillis() - 86400000,
                "reason", "社区投票决定"
            )
        );
    }

    /**
     * 创建模拟质量监控数据 / Create mock quality monitor data
     */
    private Map<String, Object> createMockQualityMonitor() {
        return Map.of(
            "totalConflicts", 127,
            "pendingConflicts", 45,
            "resolvedConflicts", 82,
            "concepts", List.of(
                Map.of(
                    "id", "concept-1",
                    "name", "微服务架构",
                    "conflicts", 5,
                    "avgRating", 4.5
                ),
                Map.of(
                    "id", "concept-2",
                    "name", "数据库优化",
                    "conflicts", 3,
                    "avgRating", 4.8
                ),
                Map.of(
                    "id", "concept-3",
                    "name", "RESTful API",
                    "conflicts", 2,
                    "avgRating", 4.6
                )
            ),
            "recentActivity", List.of(
                Map.of(
                    "type", "conflict_created",
                    "conceptName", "容器化部署",
                    "timestamp", System.currentTimeMillis() - 3600000
                ),
                Map.of(
                    "type", "conflict_resolved",
                    "conceptName", "CI/CD流程",
                    "timestamp", System.currentTimeMillis() - 7200000
                )
            )
        );
    }
}


