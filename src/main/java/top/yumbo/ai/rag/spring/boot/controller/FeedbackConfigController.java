package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.config.FeedbackConfig;
import top.yumbo.ai.rag.feedback.DocumentWeightService;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

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
     * 获取当前配置 / Get current configuration
     */
    @GetMapping
    public ResponseEntity<?> getConfig(@RequestParam(value = "lang", defaultValue = "zh") String lang) {
        Map<String, Object> config = new HashMap<>();
        config.put("requireApproval", feedbackConfig.isRequireApproval());
        config.put("autoApply", feedbackConfig.isAutoApply());
        config.put("likeWeightIncrement", feedbackConfig.getLikeWeightIncrement());
        config.put("dislikeWeightDecrement", feedbackConfig.getDislikeWeightDecrement());
        config.put("minWeight", feedbackConfig.getMinWeight());
        config.put("maxWeight", feedbackConfig.getMaxWeight());
        config.put("enableDynamicWeighting", feedbackConfig.isEnableDynamicWeighting());

        log.info(LogMessageProvider.getMessage("feedback_config.log.get_config", lang));
        return ResponseEntity.ok(config);
    }

    /**
     * 更新配置 / Update configuration
     */
    @PostMapping
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Object> updates) {
        String lang = (String) updates.getOrDefault("lang", "zh"); // 获取语言参数 / Get language parameter

        try {
            if (updates.containsKey("requireApproval")) {
                boolean requireApproval = (Boolean) updates.get("requireApproval");
                feedbackConfig.setRequireApproval(requireApproval);
                log.info(LogMessageProvider.getMessage("feedback_config.log.update_require_approval", lang, requireApproval));
            }

            if (updates.containsKey("autoApply")) {
                boolean autoApply = (Boolean) updates.get("autoApply");
                feedbackConfig.setAutoApply(autoApply);
                log.info(LogMessageProvider.getMessage("feedback_config.log.update_auto_apply", lang, autoApply));
            }

            if (updates.containsKey("likeWeightIncrement")) {
                double value = ((Number) updates.get("likeWeightIncrement")).doubleValue();
                feedbackConfig.setLikeWeightIncrement(value);
                log.info(LogMessageProvider.getMessage("feedback_config.log.update_like_weight", lang, value));
            }

            if (updates.containsKey("dislikeWeightDecrement")) {
                double value = ((Number) updates.get("dislikeWeightDecrement")).doubleValue();
                feedbackConfig.setDislikeWeightDecrement(value);
                log.info(LogMessageProvider.getMessage("feedback_config.log.update_dislike_weight", lang, value));
            }

            if (updates.containsKey("enableDynamicWeighting")) {
                boolean enable = (Boolean) updates.get("enableDynamicWeighting");
                feedbackConfig.setEnableDynamicWeighting(enable);
                log.info(LogMessageProvider.getMessage("feedback_config.log.update_enable_weighting", lang, enable));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", LogMessageProvider.getMessage("feedback_config.api.success.config_updated", lang),
                "config", getConfig(lang).getBody()
            ));

        } catch (Exception e) {
            log.error("Configuration update failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", LogMessageProvider.getMessage("feedback_config.api.error.update_failed", lang, e.getMessage())
            ));
        }
    }

    /**
     * 切换审核模式 / Toggle approval mode
     */
    @PostMapping("/toggle-approval")
    public ResponseEntity<?> toggleApproval(@RequestBody Map<String, Object> request) {
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数 / Get language parameter
        boolean requireApproval = (Boolean) request.getOrDefault("requireApproval", false);
        feedbackConfig.setRequireApproval(requireApproval);

        String modeKey = requireApproval ? "feedback_config.api.mode.require_approval" : "feedback_config.api.mode.auto_apply";
        String mode = LogMessageProvider.getMessage(modeKey, lang);
        log.info(LogMessageProvider.getMessage("feedback_config.log.toggle_mode", lang, mode));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", LogMessageProvider.getMessage("feedback_config.api.success.mode_switched", lang, mode),
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
     * 重置文档权重 / Reset document weight
     */
    @PostMapping("/weights/reset")
    public ResponseEntity<?> resetWeight(@RequestBody Map<String, String> request) {
        String lang = request.getOrDefault("lang", "zh"); // 获取语言参数 / Get language parameter
        String documentName = request.get("documentName");

        if (documentName == null || documentName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", LogMessageProvider.getMessage("feedback_config.api.error.document_name_empty", lang)
            ));
        }

        documentWeightService.resetWeight(documentName);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", LogMessageProvider.getMessage("feedback_config.api.success.weight_reset", lang, documentName)
        ));
    }

    /**
     * 清除所有权重 / Clear all weights
     */
    @PostMapping("/weights/clear")
    public ResponseEntity<?> clearAllWeights(@RequestBody(required = false) Map<String, String> request) {
        String lang = request != null ? request.getOrDefault("lang", "zh") : "zh"; // 获取语言参数 / Get language parameter

        documentWeightService.clearAllWeights();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", LogMessageProvider.getMessage("feedback_config.api.success.weights_cleared", lang)
        ));
    }
}


