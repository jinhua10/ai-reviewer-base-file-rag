package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.dto.ThemeConfigDTO;
import top.yumbo.ai.rag.spring.boot.dto.ThemeInfoDTO;
import top.yumbo.ai.rag.spring.boot.dto.ThemeUploadResponse;
import top.yumbo.ai.rag.spring.boot.service.ThemeManagementService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主题管理控制器 / Theme Management Controller
 *
 * 提供主题管理的REST API接口
 * Provides REST API for theme management
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/themes")
@CrossOrigin(origins = "*") // 允许跨域 / Allow CORS
public class ThemeManagementController {

    @Autowired
    private ThemeManagementService themeManagementService;

    /**
     * 上传主题 / Upload theme
     *
     * POST /api/themes/upload
     *
     * @param lang 界面语言 / UI language (zh/en)
     * @param themeConfig 主题配置JSON字符串 / Theme configuration JSON string
     * @param files 主题文件数组 / Theme files array
     * @return 上传响应 / Upload response
     */
    @PostMapping("/upload")
    public ResponseEntity<ThemeUploadResponse> uploadTheme(
        @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang,
        @RequestParam("themeConfig") String themeConfig,
        @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        // 标准化语言代码 / Normalize language code
        String language = normalizeLanguage(lang);

        log.info(I18N.get("theme.controller.upload-received"));
        log.debug("Theme config: {}", themeConfig);

        if (files != null) {
            log.info(I18N.get("theme.controller.uploading-files", files.length));
        }

        ThemeUploadResponse response = themeManagementService.uploadTheme(themeConfig, files);

        if (response.isSuccess()) {
            log.info(I18N.get("theme.controller.upload-success", response.getThemeId()));
            // 使用前端语言返回消息 / Return message in frontend language
            response.setMessage(I18N.getLang("theme.response.upload-success", language));
            return ResponseEntity.ok(response);
        } else {
            log.error(I18N.get("theme.controller.upload-failed", response.getError()));
            // 使用前端语言返回错误消息 / Return error message in frontend language
            response.setError(I18N.getLang("theme.response.upload-failed", language));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 获取主题列表 / Get theme list
     *
     * GET /api/themes/list
     *
     * @return 主题列表 / Theme list
     */
    @GetMapping("/list")
    public ResponseEntity<List<ThemeInfoDTO>> getThemeList() {
        log.info(I18N.get("theme.controller.list-fetching"));

        List<ThemeInfoDTO> themes = themeManagementService.getThemeList();

        log.info(I18N.get("theme.controller.list-returned", themes.size()));
        return ResponseEntity.ok(themes);
    }

    /**
     * 获取主题详情 / Get theme details
     *
     * GET /api/themes/{themeId}
     *
     * @param lang 界面语言 / UI language (zh/en)
     * @param themeId 主题ID / Theme ID
     * @return 主题配置 / Theme configuration
     */
    @GetMapping("/{themeId}")
    public ResponseEntity<?> getThemeById(
        @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang,
        @PathVariable String themeId
    ) {
        String language = normalizeLanguage(lang);

        log.info(I18N.get("theme.controller.detail-fetching", themeId));

        ThemeConfigDTO theme = themeManagementService.getThemeById(themeId);

        if (theme != null) {
            log.info(I18N.get("theme.controller.detail-found", themeId));
            return ResponseEntity.ok(theme);
        } else {
            log.warn(I18N.get("theme.controller.detail-not-found", themeId));
            Map<String, Object> error = new HashMap<>();
            error.put("error", I18N.getLang("theme.error.not-found", language));
            error.put("themeId", themeId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 删除主题 / Delete theme
     *
     * DELETE /api/themes/{themeId}
     *
     * @param lang 界面语言 / UI language (zh/en)
     * @param themeId 主题ID / Theme ID
     * @return 删除结果 / Delete result
     */
    @DeleteMapping("/{themeId}")
    public ResponseEntity<Map<String, Object>> deleteTheme(
        @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang,
        @PathVariable String themeId
    ) {
        String language = normalizeLanguage(lang);

        log.info(I18N.get("theme.controller.deleting", themeId));

        boolean success = themeManagementService.deleteTheme(themeId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            log.info(I18N.get("theme.controller.delete-success", themeId));
            response.put("message", I18N.getLang("theme.response.delete-success", language));
            return ResponseEntity.ok(response);
        } else {
            log.error(I18N.get("theme.controller.delete-failed", themeId));
            response.put("error", I18N.getLang("theme.response.delete-failed", language));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 同步主题 / Sync theme
     *
     * PUT /api/themes/sync
     *
     * @param lang 界面语言 / UI language (zh/en)
     * @param themeConfig 主题配置 / Theme configuration
     * @return 同步结果 / Sync result
     */
    @PutMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncTheme(
        @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang,
        @RequestBody ThemeConfigDTO themeConfig
    ) {
        String language = normalizeLanguage(lang);

        log.info(I18N.get("theme.controller.syncing", themeConfig.getId()));

        boolean success = themeManagementService.syncTheme(themeConfig);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        if (success) {
            log.info(I18N.get("theme.controller.sync-success", themeConfig.getId()));
            response.put("message", I18N.getLang("theme.response.sync-success", language));
            return ResponseEntity.ok(response);
        } else {
            log.error(I18N.get("theme.controller.sync-failed", themeConfig.getId()));
            response.put("error", I18N.getLang("theme.response.sync-failed", language));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 健康检查 / Health check
     *
     * GET /api/themes/health
     *
     * @return 健康状态 / Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", I18N.get("theme.health.healthy"));
        health.put("service", I18N.get("theme.health.service"));
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    // ========== 辅助方法 / Helper Methods ==========

    /**
     * 标准化语言代码 / Normalize language code
     *
     * 将各种格式的语言代码统一为标准格式
     * Unify various language code formats to standard format
     *
     * @param lang 原始语言代码 / Original language code (zh-CN, zh_CN, en-US, en, etc.)
     * @return 标准化的语言代码 / Normalized language code (zh or en)
     */
    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isEmpty()) {
            return "zh"; // 默认中文 / Default to Chinese
        }

        // 转换为小写并提取主语言代码 / Convert to lowercase and extract primary language code
        String normalized = lang.toLowerCase();

        // 处理常见格式 / Handle common formats
        // zh-CN, zh_CN, zh-Hans → zh
        // en-US, en_US, en-GB → en
        if (normalized.startsWith("zh")) {
            return "zh";
        } else if (normalized.startsWith("en")) {
            return "en";
        }

        // 默认返回中文 / Default to Chinese
        return "zh";
    }
}

