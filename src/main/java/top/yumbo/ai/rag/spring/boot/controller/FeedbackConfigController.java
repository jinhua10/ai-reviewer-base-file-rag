package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.config.FeedbackConfig;
import top.yumbo.ai.rag.feedback.DocumentWeightService;

import java.util.HashMap;
import java.util.Map;

/**
 * 反馈配置管理控制器
 *
 * 提供动态修改反馈配置的API接口
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/api/feedback/config")
public class FeedbackConfigController {

    @Autowired
    private FeedbackConfig feedbackConfig;

    @Autowired
    private DocumentWeightService documentWeightService;

    /**
     * 获取当前配置
     */
    @GetMapping
    public ResponseEntity<?> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("requireApproval", feedbackConfig.isRequireApproval());
        config.put("autoApply", feedbackConfig.isAutoApply());
        config.put("likeWeightIncrement", feedbackConfig.getLikeWeightIncrement());
        config.put("dislikeWeightDecrement", feedbackConfig.getDislikeWeightDecrement());
        config.put("minWeight", feedbackConfig.getMinWeight());
        config.put("maxWeight", feedbackConfig.getMaxWeight());
        config.put("enableDynamicWeighting", feedbackConfig.isEnableDynamicWeighting());

        log.info("📋 获取反馈配置");
        return ResponseEntity.ok(config);
    }

    /**
     * 更新配置
     */
    @PostMapping
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> updates) {
        try {
            if (updates.containsKey("requireApproval")) {
                boolean requireApproval = (Boolean) updates.get("requireApproval");
                feedbackConfig.setRequireApproval(requireApproval);
                log.info("🔧 更新配置: requireApproval = {}", requireApproval);
            }

            if (updates.containsKey("autoApply")) {
                boolean autoApply = (Boolean) updates.get("autoApply");
                feedbackConfig.setAutoApply(autoApply);
                log.info("🔧 更新配置: autoApply = {}", autoApply);
            }

            if (updates.containsKey("likeWeightIncrement")) {
                double value = ((Number) updates.get("likeWeightIncrement")).doubleValue();
                feedbackConfig.setLikeWeightIncrement(value);
                log.info("🔧 更新配置: likeWeightIncrement = {}", value);
            }

            if (updates.containsKey("dislikeWeightDecrement")) {
                double value = ((Number) updates.get("dislikeWeightDecrement")).doubleValue();
                feedbackConfig.setDislikeWeightDecrement(value);
                log.info("🔧 更新配置: dislikeWeightDecrement = {}", value);
            }

            if (updates.containsKey("enableDynamicWeighting")) {
                boolean enable = (Boolean) updates.get("enableDynamicWeighting");
                feedbackConfig.setEnableDynamicWeighting(enable);
                log.info("🔧 更新配置: enableDynamicWeighting = {}", enable);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "配置更新成功",
                "config", getConfig().getBody()
            ));

        } catch (Exception e) {
            log.error("更新配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "配置更新失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 切换审核模式
     */
    @PostMapping("/toggle-approval")
    public ResponseEntity<?> toggleApproval(@RequestBody Map<String, Boolean> request) {
        boolean requireApproval = request.getOrDefault("requireApproval", false);
        feedbackConfig.setRequireApproval(requireApproval);

        String mode = requireApproval ? "需要审核" : "自动生效";
        log.info("🔄 切换审核模式: {}", mode);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "审核模式已切换为: " + mode,
            "requireApproval", requireApproval
        ));
    }

    /**
     * 获取文档权重统计
     */
    @GetMapping("/weights/statistics")
    public ResponseEntity<?> getWeightStatistics() {
        Map<String, Object> stats = documentWeightService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有文档权重
     */
    @GetMapping("/weights")
    public ResponseEntity<?> getAllWeights() {
        Map<String, DocumentWeightService.DocumentWeight> weights =
            documentWeightService.getAllWeights();
        return ResponseEntity.ok(weights);
    }

    /**
     * 重置文档权重
     */
    @PostMapping("/weights/reset")
    public ResponseEntity<?> resetWeight(@RequestBody Map<String, String> request) {
        String documentName = request.get("documentName");

        if (documentName == null || documentName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "文档名称不能为空"
            ));
        }

        documentWeightService.resetWeight(documentName);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "文档权重已重置: " + documentName
        ));
    }

    /**
     * 清除所有权重
     */
    @PostMapping("/weights/clear")
    public ResponseEntity<?> clearAllWeights() {
        documentWeightService.clearAllWeights();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "所有文档权重已清除"
        ));
    }
}

