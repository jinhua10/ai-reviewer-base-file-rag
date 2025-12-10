package top.yumbo.ai.rag.spring.boot.abtest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A/B 测试控制器
 * (A/B Testing Controller)
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/abtest")
@CrossOrigin(origins = "*")
public class ABTestController {

    private final ABTestService abTestService;

    @Autowired
    public ABTestController(ABTestService abTestService) {
        this.abTestService = abTestService;
    }

    /**
     * 创建 A/B 测试实验 (Create A/B test experiment)
     * 创建一个新的A/B测试实验，包含两个变体进行比较
     * (Creates a new A/B test experiment with two variants for comparison)
     *
     * POST /api/abtest/experiment
     */
    @PostMapping("/experiment")
    public ResponseEntity<?> createExperiment(@RequestBody CreateExperimentRequest request) {
        try {
            ABTestService.ABTestExperiment experiment = abTestService.createExperiment(
                request.getExperimentId(),
                request.getQuestion(),
                request.getVariantA(),
                request.getVariantB()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("experimentId", experiment.getExperimentId());
            response.put("message", "实验已创建");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建实验失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 为用户分配变体 (Assign variant to user)
     * 为指定用户分配实验中的一个变体
     * (Assigns a variant from the experiment to a specified user)
     *
     * POST /api/abtest/assign
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignVariant(@RequestBody AssignRequest request) {
        try {
            ABTestService.Variant variant = abTestService.assignVariant(
                request.getExperimentId(),
                request.getUserId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("variant", variant);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("分配变体失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 记录用户反馈
     *
     * POST /api/abtest/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> recordFeedback(@RequestBody FeedbackRequest request) {
        try {
            abTestService.recordFeedback(
                request.getExperimentId(),
                request.getUserId(),
                request.isSatisfied()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "反馈已记录");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("记录反馈失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取实验统计
     *
     * GET /api/abtest/statistics/{experimentId}
     */
    @GetMapping("/statistics/{experimentId}")
    public ResponseEntity<?> getStatistics(@PathVariable String experimentId) {
        try {
            ABTestService.ExperimentStatistics stats =
                abTestService.getStatistics(experimentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取统计失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 自动决策
     *
     * POST /api/abtest/decide/{experimentId}
     */
    @PostMapping("/decide/{experimentId}")
    public ResponseEntity<?> autoDecide(
            @PathVariable String experimentId,
            @RequestParam(defaultValue = "30") int minSamples,
            @RequestParam(defaultValue = "0.05") double confidenceLevel) {
        try {
            ABTestService.DecisionResult result = abTestService.autoDecide(
                experimentId, minSamples, confidenceLevel);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("decision", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("自动决策失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取所有实验
     *
     * GET /api/abtest/experiments
     */
    @GetMapping("/experiments")
    public ResponseEntity<?> getAllExperiments() {
        try {
            List<ABTestService.ABTestExperiment> experiments =
                abTestService.getAllExperiments();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", experiments.size());
            response.put("experiments", experiments);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取实验列表失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取活跃实验
     *
     * GET /api/abtest/experiments/active
     */
    @GetMapping("/experiments/active")
    public ResponseEntity<?> getActiveExperiments() {
        try {
            List<ABTestService.ABTestExperiment> experiments =
                abTestService.getActiveExperiments();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", experiments.size());
            response.put("experiments", experiments);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取活跃实验失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 停止实验
     *
     * POST /api/abtest/stop/{experimentId}
     */
    @PostMapping("/stop/{experimentId}")
    public ResponseEntity<?> stopExperiment(@PathVariable String experimentId) {
        try {
            abTestService.stopExperiment(experimentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "实验已停止");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("停止实验失败", e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
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

    // ==================== 请求 DTO ====================
    // (Request DTOs)

    /**
     * 创建实验请求 (Create experiment request)
     * 用于创建A/B测试实验的请求参数
     * (Request parameters for creating A/B test experiment)
     */
    @lombok.Data
    public static class CreateExperimentRequest {
        /**
         * 实验ID (Experiment ID)
         * 唯一标识实验的ID
         * (Unique identifier for the experiment)
         */
        private String experimentId;
        
        /**
         * 实验问题 (Experiment question)
         * 实验要解决的问题或测试的目标
         * (Question or goal to be tested in the experiment)
         */
        private String question;
        
        /**
         * 变体A (Variant A)
         * 实验的第一个变体
         * (First variant of the experiment)
         */
        private ABTestService.Variant variantA;
        
        /**
         * 变体B (Variant B)
         * 实验的第二个变体
         * (Second variant of the experiment)
         */
        private ABTestService.Variant variantB;
    }

    @lombok.Data
    public static class AssignRequest {
        private String experimentId;
        private String userId;
    }

    @lombok.Data
    public static class FeedbackRequest {
        private String experimentId;
        private String userId;
        private boolean satisfied;
    }
}

