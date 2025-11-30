package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.feedback.QARecordService;
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
     * 提交整体反馈
     */
    @PostMapping("/overall")
    public ResponseEntity<?> submitOverallFeedback(@RequestBody Map<String, Object> request) {
        try {
            String recordId = (String) request.get("recordId");
            Integer rating = (Integer) request.get("rating");
            String feedback = (String) request.get("feedback");

            if (recordId == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "recordId 和 rating 不能为空"
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "rating 必须在 1-5 之间"
                ));
            }

            boolean success = qaRecordService.addOverallFeedback(recordId, rating, feedback);

            if (success) {
                log.info(LogMessageProvider.getMessage("log.feedback.overall_received", recordId, rating));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "感谢您的反馈！"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "记录不存在"
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.overall_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 提交文档反馈
     */
    @PostMapping("/document")
    public ResponseEntity<?> submitDocumentFeedback(@RequestBody Map<String, Object> request) {
        try {
            String recordId = (String) request.get("recordId");
            String documentName = (String) request.get("documentName");
            String feedbackType = (String) request.get("feedbackType");
            String reason = (String) request.get("reason");

            if (recordId == null || documentName == null || feedbackType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "recordId, documentName 和 feedbackType 不能为空"
                ));
            }

            QARecord.FeedbackType type;
            try {
                type = QARecord.FeedbackType.valueOf(feedbackType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "feedbackType 必须是 LIKE 或 DISLIKE"
                ));
            }

            boolean success = qaRecordService.addDocumentFeedback(recordId, documentName, type, reason);

            if (success) {
                String emoji = type == QARecord.FeedbackType.LIKE ? "👍" : "👎";
                log.info(LogMessageProvider.getMessage("log.feedback.document_received", emoji, recordId, documentName));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "感谢您的反馈！"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "记录不存在"
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.document_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取问答记录
     */
    @GetMapping("/record/{recordId}")
    public ResponseEntity<?> getRecord(@PathVariable String recordId) {
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
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取最近的问答记录
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentRecords(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<QARecord> records = qaRecordService.getRecentRecords(limit);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.get_recent_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取待审核的记录
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRecords() {
        try {
            List<QARecord> records = qaRecordService.getPendingRecords();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.get_pending_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            var stats = qaRecordService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.get_statistics_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 星级评价文档有用性（用户友好的评分接口）
     *
     * @param request 包含 recordId, documentName, rating (1-5星)
     * @return 响应结果
     */
    @PostMapping("/document/rate")
    public ResponseEntity<?> rateDocumentQuality(@RequestBody Map<String, Object> request) {
        try {
            String recordId = (String) request.get("recordId");
            String documentName = (String) request.get("documentName");
            Integer rating = (Integer) request.get("rating");
            String comment = (String) request.get("comment");

            // 参数验证
            if (recordId == null || documentName == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "recordId, documentName 和 rating 不能为空"
                ));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "评分必须在 1-5 星之间"
                ));
            }

            // 调用服务层处理星级评价
            boolean success = qaRecordService.addDocumentRating(recordId, documentName, rating, comment);

            if (success) {
                String stars = "⭐".repeat(rating);
                log.info(LogMessageProvider.getMessage("log.feedback.rating_submitted", stars, recordId, documentName, rating, String.format("%+.1f", 0.0)));

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "感谢您的评价！",
                    "rating", rating,
                    "impact", getImpactDescription(rating)
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "记录不存在"
                ));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.feedback.rating_failed"), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取评分影响的用户友好描述
     */
    private String getImpactDescription(int rating) {
        switch (rating) {
            case 5:
                return "这个文档非常有用！系统会优先推荐它 🚀";
            case 4:
                return "这个文档很有帮助，系统会增加推荐权重 📈";
            case 3:
                return "这个文档还行，系统会保持当前权重 ➡️";
            case 2:
                return "这个文档帮助不大，系统会降低推荐权重 📉";
            case 1:
                return "这个文档没有帮助，系统会大幅降低推荐权重 ⚠️";
            default:
                return "";
        }
    }
}
