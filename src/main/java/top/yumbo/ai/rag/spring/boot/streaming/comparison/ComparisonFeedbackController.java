package top.yumbo.ai.rag.spring.boot.streaming.comparison;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答案对比反馈控制器
 * (Answer Comparison Feedback Controller)
 *
 * 提供 REST API 接口用于：
 * 1. 提交用户对 HOPE vs LLM 答案的选择
 * 2. 查询对比记录
 * 3. 查询投票统计
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/qa/comparison")
@CrossOrigin(origins = "*")
public class ComparisonFeedbackController {

    private final AnswerComparisonService comparisonService;

    @Autowired
    public ComparisonFeedbackController(AnswerComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * 提交对比反馈
     *
     * POST /api/qa/comparison/feedback
     *
     * @param request 反馈请求
     * @return 处理结果
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        try {
            log.info("收到对比反馈: sessionId={}, choice={}",
                request.getSessionId(), request.getChoice());

            // 构建对比记录
            AnswerComparison comparison = AnswerComparison.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .question(request.getQuestion())
                .hopeAnswer(request.getHopeAnswer())
                .hopeConfidence(request.getHopeConfidence())
                .hopeSource(request.getHopeSource())
                .llmAnswer(request.getLlmAnswer())
                .userChoice(parseUserChoice(request.getChoice()))
                .userComment(request.getComment())
                .build();

            // 提交反馈
            AnswerComparisonService.ComparisonResult result =
                comparisonService.submitFeedback(comparison);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("comparisonId", result.getComparisonId());
            response.put("processingAsync", result.isProcessingAsync());

            log.info("对比反馈已提交: comparisonId={}", result.getComparisonId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("提交对比反馈失败", e);
            return ResponseEntity.ok(createErrorResponse(
                I18N.get("comparison.submit.failed") + ": " + e.getMessage()
            ));
        }
    }

    /**
     * 查询对比记录
     *
     * GET /api/qa/comparison/{comparisonId}
     */
    @GetMapping("/{comparisonId}")
    public ResponseEntity<?> getComparison(@PathVariable String comparisonId) {
        try {
            return comparisonService.getComparison(comparisonId)
                .map(comparison -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("comparison", buildComparisonDTO(comparison));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(createErrorResponse(
                    I18N.get("comparison.not.found")
                )));

        } catch (Exception e) {
            log.error("查询对比记录失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 查询投票统计
     *
     * GET /api/qa/comparison/vote-stats?question=xxx
     */
    @GetMapping("/vote-stats")
    public ResponseEntity<?> getVoteStatistics(@RequestParam String question) {
        try {
            return comparisonService.getVoteStatistics(question)
                .map(stats -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("question", stats.getQuestion());
                    response.put("hopeVotes", stats.getHopeVotes());
                    response.put("llmVotes", stats.getLlmVotes());
                    response.put("bothVotes", stats.getBothVotes());
                    response.put("neitherVotes", stats.getNeitherVotes());
                    response.put("totalVotes", stats.getTotalVotes());
                    response.put("hopeWinRate", stats.getHopeWinRate());
                    response.put("llmWinRate", stats.getLlmWinRate());
                    response.put("winner", stats.getWinner());
                    response.put("lastUpdated", stats.getLastUpdated());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(createErrorResponse(
                    I18N.get("comparison.stats.not.found")
                )));

        } catch (Exception e) {
            log.error("查询投票统计失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取最近的对比记录
     *
     * GET /api/qa/comparison/recent?limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentComparisons(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnswerComparison> comparisons =
                comparisonService.getRecentComparisons(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", comparisons.size());
            response.put("comparisons", comparisons.stream()
                .map(this::buildComparisonDTO)
                .toArray());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询最近对比记录失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取对比统计摘要
     *
     * GET /api/qa/comparison/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            List<AnswerComparison> allComparisons =
                comparisonService.getAllComparisons();

            // 统计各种选择的数量
            long hopeCount = allComparisons.stream()
                .filter(c -> c.getUserChoice() == AnswerComparison.UserChoice.HOPE_BETTER)
                .count();
            long llmCount = allComparisons.stream()
                .filter(c -> c.getUserChoice() == AnswerComparison.UserChoice.LLM_BETTER)
                .count();
            long bothCount = allComparisons.stream()
                .filter(c -> c.getUserChoice() == AnswerComparison.UserChoice.BOTH_GOOD)
                .count();
            long neitherCount = allComparisons.stream()
                .filter(c -> c.getUserChoice() == AnswerComparison.UserChoice.NEITHER_GOOD)
                .count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalComparisons", allComparisons.size());
            response.put("hopeWins", hopeCount);
            response.put("llmWins", llmCount);
            response.put("bothGood", bothCount);
            response.put("neitherGood", neitherCount);
            response.put("hopeWinRate", allComparisons.isEmpty() ? 0.0 :
                (double) hopeCount / allComparisons.size());
            response.put("llmWinRate", allComparisons.isEmpty() ? 0.0 :
                (double) llmCount / allComparisons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询统计摘要失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 解析用户选择
     */
    private AnswerComparison.UserChoice parseUserChoice(String choice) {
        switch (choice.toLowerCase()) {
            case "hope":
            case "hope_better":
                return AnswerComparison.UserChoice.HOPE_BETTER;
            case "llm":
            case "llm_better":
                return AnswerComparison.UserChoice.LLM_BETTER;
            case "both":
            case "both_good":
                return AnswerComparison.UserChoice.BOTH_GOOD;
            case "neither":
            case "neither_good":
                return AnswerComparison.UserChoice.NEITHER_GOOD;
            default:
                throw new IllegalArgumentException("无效的选择: " + choice);
        }
    }

    /**
     * 构建对比记录 DTO
     */
    private Map<String, Object> buildComparisonDTO(AnswerComparison comparison) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("comparisonId", comparison.getComparisonId());
        dto.put("sessionId", comparison.getSessionId());
        dto.put("question", comparison.getQuestion());
        dto.put("userChoice", comparison.getUserChoice());
        dto.put("userComment", comparison.getUserComment());
        dto.put("differenceAnalysis", comparison.getDifferenceAnalysis());
        dto.put("createdAt", comparison.getCreatedAt());
        dto.put("feedbackAt", comparison.getFeedbackAt());
        dto.put("processed", comparison.isProcessed());
        return dto;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    /**
     * 反馈请求 DTO
     */
    @lombok.Data
    public static class FeedbackRequest {
        private String sessionId;
        private String userId;
        private String question;
        private String hopeAnswer;
        private Double hopeConfidence;
        private String hopeSource;
        private String llmAnswer;
        private String choice; // hope/llm/both/neither
        private String comment;
    }
}

