package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.feedback.HierarchicalFeedback;
import top.yumbo.ai.rag.feedback.HierarchicalFeedback.*;
import top.yumbo.ai.rag.spring.boot.service.ActiveLearningService;
import top.yumbo.ai.rag.spring.boot.service.ActiveLearningService.ActiveLearningRecommendation;
import top.yumbo.ai.rag.spring.boot.service.HierarchicalFeedbackService;
import top.yumbo.ai.rag.spring.boot.service.HierarchicalFeedbackService.FeedbackStatistics;
import top.yumbo.ai.rag.spring.boot.service.HierarchicalFeedbackService.ParagraphInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分层反馈和主动学习 API 控制器
 *
 * 提供段落级、句子级精细反馈接口，以及主动学习推荐接口
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback/hierarchical")
public class HierarchicalFeedbackController {

    private final HierarchicalFeedbackService feedbackService;
    private final ActiveLearningService activeLearningService;

    @Autowired
    public HierarchicalFeedbackController(
            HierarchicalFeedbackService feedbackService,
            @Autowired(required = false) ActiveLearningService activeLearningService) {
        this.feedbackService = feedbackService;
        this.activeLearningService = activeLearningService;
    }

    // ==================== 分层反馈接口 ====================

    /**
     * 提交文档级反馈
     */
    @PostMapping("/document")
    public ResponseEntity<?> submitDocumentFeedback(@RequestBody DocumentFeedbackRequest request) {
        try {
            DocumentLevelFeedback feedback = DocumentLevelFeedback.builder()
                    .rating(request.getRating())
                    .relevance(request.getRelevance())
                    .comment(request.getComment())
                    .tags(request.getTags())
                    .build();

            HierarchicalFeedback result = feedbackService.submitDocumentFeedback(
                    request.getQaRecordId(),
                    request.getDocumentName(),
                    request.getDocumentId(),
                    feedback);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("feedbackId", result.getId());
            response.put("message", "文档级反馈已保存");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("提交文档级反馈失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 提交段落级反馈
     */
    @PostMapping("/paragraph")
    public ResponseEntity<?> submitParagraphFeedback(@RequestBody ParagraphFeedbackRequest request) {
        try {
            ParagraphFeedback feedback = ParagraphFeedback.builder()
                    .paragraphIndex(request.getParagraphIndex())
                    .contentPreview(request.getContentPreview())
                    .startOffset(request.getStartOffset())
                    .endOffset(request.getEndOffset())
                    .helpful(request.isHelpful())
                    .relevanceScore(request.getRelevanceScore())
                    .feedbackType(request.getFeedbackType())
                    .comment(request.getComment())
                    .build();

            HierarchicalFeedback result = feedbackService.submitParagraphFeedback(
                    request.getQaRecordId(),
                    request.getDocumentName(),
                    request.getDocumentId(),
                    feedback);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("feedbackId", result.getId());
            response.put("paragraphIndex", request.getParagraphIndex());
            response.put("message", "段落级反馈已保存");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("提交段落级反馈失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 提交句子级反馈（高亮标记）
     */
    @PostMapping("/sentence")
    public ResponseEntity<?> submitSentenceFeedback(@RequestBody SentenceFeedbackRequest request) {
        try {
            SentenceFeedback feedback = SentenceFeedback.builder()
                    .sentenceIndex(request.getSentenceIndex())
                    .content(request.getContent())
                    .startOffset(request.getStartOffset())
                    .endOffset(request.getEndOffset())
                    .highlightType(request.getHighlightType())
                    .annotation(request.getAnnotation())
                    .keyInformation(request.isKeyInformation())
                    .build();

            HierarchicalFeedback result = feedbackService.submitSentenceFeedback(
                    request.getQaRecordId(),
                    request.getDocumentName(),
                    request.getDocumentId(),
                    feedback);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("feedbackId", result.getId());
            response.put("highlightType", request.getHighlightType());
            response.put("message", "句子级反馈已保存");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("提交句子级反馈失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 批量提交高亮标记
     */
    @PostMapping("/highlights")
    public ResponseEntity<?> submitHighlights(@RequestBody HighlightsRequest request) {
        try {
            List<SentenceFeedback> highlights = request.getHighlights().stream()
                    .map(h -> SentenceFeedback.builder()
                            .sentenceIndex(h.getSentenceIndex())
                            .content(h.getContent())
                            .startOffset(h.getStartOffset())
                            .endOffset(h.getEndOffset())
                            .highlightType(h.getHighlightType())
                            .annotation(h.getAnnotation())
                            .keyInformation(h.isKeyInformation())
                            .build())
                    .toList();

            HierarchicalFeedback result = feedbackService.submitHighlights(
                    request.getQaRecordId(),
                    request.getDocumentName(),
                    request.getDocumentId(),
                    highlights);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("feedbackId", result.getId());
            response.put("highlightCount", highlights.size());
            response.put("message", "批量高亮已保存");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量提交高亮失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取文档的分层反馈
     */
    @GetMapping("/{qaRecordId}/{documentName}")
    public ResponseEntity<?> getFeedback(
            @PathVariable String qaRecordId,
            @PathVariable String documentName) {
        try {
            return feedbackService.getFeedback(qaRecordId, documentName)
                    .map(feedback -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "feedback", feedback
                    )))
                    .orElse(ResponseEntity.ok(Map.of(
                            "success", true,
                            "feedback", (Object) null,
                            "message", "未找到反馈记录"
                    )));
        } catch (Exception e) {
            log.error("获取反馈失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 分析文档段落
     */
    @PostMapping("/analyze-paragraphs")
    public ResponseEntity<?> analyzeParagraphs(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "文档内容不能为空"
                ));
            }

            List<ParagraphInfo> paragraphs = feedbackService.analyzeDocumentParagraphs(content);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paragraphs", paragraphs,
                    "count", paragraphs.size()
            ));
        } catch (Exception e) {
            log.error("分析段落失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取反馈统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            FeedbackStatistics stats = feedbackService.getStatistics();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", stats
            ));
        } catch (Exception e) {
            log.error("获取统计失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== 主动学习接口 ====================

    /**
     * 获取主动学习推荐
     */
    @PostMapping("/active-learning/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestBody RecommendationRequest request) {
        if (activeLearningService == null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recommendations", Map.of(),
                    "message", "主动学习服务未启用"
            ));
        }

        try {
            ActiveLearningRecommendation recommendations = activeLearningService.getRecommendations(
                    request.getQuestion(),
                    request.getRetrievedDocs(),
                    request.getTopKUsed());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recommendations", recommendations,
                    "needsConfirmation", recommendations.needsUserConfirmation()
            ));
        } catch (Exception e) {
            log.error("获取推荐失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 提交主动学习反馈
     */
    @PostMapping("/active-learning/feedback")
    public ResponseEntity<?> submitActiveLearningFeedback(@RequestBody ActiveLearningFeedbackRequest request) {
        if (activeLearningService == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "主动学习服务未启用"
            ));
        }

        try {
            activeLearningService.processFeedback(
                    request.getDocumentName(),
                    request.isRelevant(),
                    request.getQuestion());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "主动学习反馈已记录"
            ));
        } catch (Exception e) {
            log.error("提交主动学习反馈失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 记录查询历史（用于主动学习）
     */
    @PostMapping("/active-learning/record-history")
    public ResponseEntity<?> recordQueryHistory(@RequestBody QueryHistoryRequest request) {
        if (activeLearningService == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "主动学习服务未启用"
            ));
        }

        try {
            activeLearningService.recordQueryHistory(
                    request.getQuestion(),
                    request.getUsedDocuments(),
                    request.getHighRatedDocuments(),
                    request.getRating());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "查询历史已记录"
            ));
        } catch (Exception e) {
            log.error("记录查询历史失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== 请求对象 ====================

    @Data
    public static class DocumentFeedbackRequest {
        private String qaRecordId;
        private String documentName;
        private String documentId;
        private Integer rating;
        private RelevanceLevel relevance;
        private String comment;
        private List<String> tags;
    }

    @Data
    public static class ParagraphFeedbackRequest {
        private String qaRecordId;
        private String documentName;
        private String documentId;
        private int paragraphIndex;
        private String contentPreview;
        private int startOffset;
        private int endOffset;
        private boolean helpful;
        private Integer relevanceScore;
        private ParagraphFeedbackType feedbackType;
        private String comment;
    }

    @Data
    public static class SentenceFeedbackRequest {
        private String qaRecordId;
        private String documentName;
        private String documentId;
        private int sentenceIndex;
        private String content;
        private int startOffset;
        private int endOffset;
        private HighlightType highlightType;
        private String annotation;
        private boolean keyInformation;
    }

    @Data
    public static class HighlightsRequest {
        private String qaRecordId;
        private String documentName;
        private String documentId;
        private List<SentenceFeedbackRequest> highlights;
    }

    @Data
    public static class RecommendationRequest {
        private String question;
        private List<top.yumbo.ai.rag.model.Document> retrievedDocs;
        private int topKUsed;
    }

    @Data
    public static class ActiveLearningFeedbackRequest {
        private String documentName;
        private boolean relevant;
        private String question;
    }

    @Data
    public static class QueryHistoryRequest {
        private String question;
        private List<String> usedDocuments;
        private List<String> highRatedDocuments;
        private int rating;
    }
}

