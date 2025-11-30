package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.feedback.QARecordService;
import top.yumbo.ai.rag.i18n.ApiMessageProvider;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

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

    public FeedbackController(QARecordService qaRecordService) {
        this.qaRecordService = qaRecordService;
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
                    "message", ApiMessageProvider.getMissingParams(lang, "recordId, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getInvalidRating(lang)
                ));
            }

            boolean success = qaRecordService.addOverallFeedback(recordId, rating, feedback);

            if (success) {
                log.info(LogMessageProvider.getMessage("log.feedback.overall_received", recordId, rating));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", ApiMessageProvider.getFeedbackReceived(lang)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getRecordNotFound(lang)
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.overall_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
                    "message", ApiMessageProvider.getMissingParams(lang, "recordId, documentName, feedbackType")
                ));
            }

            QARecord.FeedbackType type;
            try {
                type = QARecord.FeedbackType.valueOf(feedbackType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getInvalidFeedbackType(lang)
                ));
            }

            boolean success = qaRecordService.addDocumentFeedback(recordId, documentName, type, reason);

            if (success) {
                String emoji = type == QARecord.FeedbackType.LIKE ? "👍" : "👎";
                log.info(LogMessageProvider.getMessage("log.feedback.document_received", emoji, recordId, documentName));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", ApiMessageProvider.getFeedbackReceived(lang)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getRecordNotFound(lang)
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.document_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
            log.error(LogMessageProvider.getMessage("log.feedback.get_record_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
            log.error(LogMessageProvider.getMessage("log.feedback.get_recent_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
            log.error(LogMessageProvider.getMessage("log.feedback.get_pending_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
            log.error(LogMessageProvider.getMessage("log.feedback.get_statistics_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
                    "message", ApiMessageProvider.getMissingParams(lang, "recordId, documentName, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getInvalidRating(lang)
                ));
            }

            // 调用服务层处理星级评价 / Call service layer to process rating
            boolean success = qaRecordService.addDocumentRating(recordId, documentName, rating, comment);

            if (success) {
                String stars = "⭐".repeat(rating);
                log.info(LogMessageProvider.getMessage("log.feedback.rating_submitted", stars, recordId, documentName, rating, String.format("%+.1f", 0.0)));

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", ApiMessageProvider.getThankYou(lang),
                    "rating", rating,
                    "impact", ApiMessageProvider.getDocumentImpact(lang, rating)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getRecordNotFound(lang)
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.rating_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
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
                    "message", ApiMessageProvider.getMissingParams(lang, "recordId, rating")
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getInvalidRating(lang)
                ));
            }

            // 调用服务层处理整体评价 / Call service layer to process overall rating
            boolean success = qaRecordService.addOverallRating(recordId, rating);

            if (success) {
                // 记录日志 / Log the rating
                String emojiText = ApiMessageProvider.getEmojiDescription(lang, rating);
                log.info(LogMessageProvider.getMessage("log.feedback.overall_rating_submitted",
                    emojiText, recordId, rating));

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", ApiMessageProvider.getThankYou(lang),
                    "rating", rating,
                    "impact", ApiMessageProvider.getOverallImpact(lang, rating)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ApiMessageProvider.getRecordNotFound(lang)
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.rating_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", ApiMessageProvider.getProcessingFailed(lang, e.getMessage())
            ));
        }
    }
}
