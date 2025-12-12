package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.config.ThemeConfig;
import top.yumbo.ai.rag.spring.boot.dto.ThemeConfigDTO;
import top.yumbo.ai.rag.spring.boot.dto.ThemeInfoDTO;
import top.yumbo.ai.rag.spring.boot.dto.ThemeUploadResponse;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 主题管理服务 / Theme Management Service
 *
 * 处理主题的上传、删除、查询等操作
 * Handles theme upload, delete, query operations
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class ThemeManagementService {

    @Autowired
    private ThemeConfig themeConfig;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 上传主题到服务器 / Upload theme to server
     *
     * @param themeConfigJson 主题配置JSON / Theme configuration JSON
     * @param files 主题文件数组 / Theme files array
     * @return 上传响应 / Upload response
     */
    public ThemeUploadResponse uploadTheme(String themeConfigJson, MultipartFile[] files) {
        try {
            // 1. 解析主题配置 / Parse theme configuration
            log.info(I18N.get("theme.upload.parsing-config"));
            ThemeConfigDTO themeConfig = objectMapper.readValue(themeConfigJson, ThemeConfigDTO.class);
            String themeId = themeConfig.getId();

            if (themeId == null || themeId.isEmpty()) {
                return ThemeUploadResponse.builder()
                    .success(false)
                    .error(I18N.get("theme.upload.invalid-id"))
                    .build();
            }

            // 2. 创建主题目录 / Create theme directory
            Path themePath = getThemePath(themeId);
            if (Files.exists(themePath)) {
                log.warn(I18N.get("theme.upload.directory-exists", themePath));
                deleteDirectory(themePath);
            }

            Files.createDirectories(themePath);
            log.info(I18N.get("theme.upload.directory-created", themePath));

            // 3. 保存主题配置文件 / Save theme configuration file
            Path configPath = themePath.resolve("theme.json");
            Files.writeString(configPath, themeConfigJson);
            log.info(I18N.get("theme.upload.config-saved", configPath));

            // 4. 保存其他文件 / Save other files
            if (files != null && files.length > 0) {
                saveThemeFiles(themePath, files);
            }

            // 5. 返回成功响应 / Return success response
            String relativePath = "/static/themes/" + themeId;
            log.info(I18N.get("theme.upload.success", themeId));

            return ThemeUploadResponse.builder()
                .success(true)
                .themeId(themeId)
                .path(relativePath)
                .message(I18N.get("theme.response.upload-success"))
                .build();

        } catch (IOException e) {
            log.error(I18N.get("theme.upload.failed", e.getMessage()), e);
            return ThemeUploadResponse.builder()
                .success(false)
                .error(I18N.get("theme.upload.failed", e.getMessage()))
                .build();
        }
    }

    /**
     * 获取服务器上的主题列表 / Get theme list from server
     *
     * @return 主题信息列表 / Theme info list
     */
    public List<ThemeInfoDTO> getThemeList() {
        List<ThemeInfoDTO> themes = new ArrayList<>();

        try {
            Path themesDir = Paths.get(themeConfig.getUploadPath());

            if (!Files.exists(themesDir)) {
                log.info(I18N.get("theme.list.directory-not-exist", themesDir));
                Files.createDirectories(themesDir);
                return themes;
            }

            // 遍历主题目录 / Iterate theme directories
            try (var stream = Files.list(themesDir)) {
                stream.filter(Files::isDirectory)
                    .forEach(themePath -> {
                        try {
                            Path configPath = themePath.resolve("theme.json");
                            if (Files.exists(configPath)) {
                                String json = Files.readString(configPath);
                                ThemeConfigDTO config = objectMapper.readValue(json, ThemeConfigDTO.class);

                                // 获取文件最后修改时间 / Get last modified time
                                BasicFileAttributes attrs = Files.readAttributes(themePath, BasicFileAttributes.class);
                                String uploadDate = LocalDateTime.ofInstant(
                                    attrs.lastModifiedTime().toInstant(),
                                    java.time.ZoneId.systemDefault()
                                ).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                                ThemeInfoDTO info = ThemeInfoDTO.builder()
                                    .id(config.getId())
                                    .name(config.getName())
                                    .description(config.getDescription())
                                    .type(config.getType())
                                    .source("server")
                                    .version(config.getVersion())
                                    .author(config.getAuthor())
                                    .preview(config.getPreview())
                                    .uploadDate(uploadDate)
                                    .build();

                                themes.add(info);
                                log.debug(I18N.get("theme.list.found-theme", config.getId()));
                            }
                        } catch (IOException e) {
                            log.error(I18N.get("theme.list.read-failed", themePath), e);
                        }
                    });
            }

            log.info(I18N.get("theme.list.returned", themes.size()));

        } catch (IOException e) {
            log.error(I18N.get("theme.upload.failed", e.getMessage()), e);
        }

        return themes;
    }

    /**
     * 获取指定主题的详细信息 / Get specific theme details
     *
     * @param themeId 主题ID / Theme ID
     * @return 主题配置 / Theme configuration
     */
    public ThemeConfigDTO getThemeById(String themeId) {
        try {
            log.info(I18N.get("theme.detail.fetching", themeId));
            Path themePath = getThemePath(themeId);
            Path configPath = themePath.resolve("theme.json");

            if (!Files.exists(configPath)) {
                log.warn(I18N.get("theme.detail.not-found", themeId));
                return null;
            }

            String json = Files.readString(configPath);
            ThemeConfigDTO config = objectMapper.readValue(json, ThemeConfigDTO.class);

            log.info(I18N.get("theme.detail.retrieved", themeId));
            return config;

        } catch (IOException e) {
            log.error(I18N.get("theme.upload.failed", e.getMessage()), e);
            return null;
        }
    }

    /**
     * 删除指定主题 / Delete specific theme
     *
     * @param themeId 主题ID / Theme ID
     * @return 是否删除成功 / Whether deletion succeeded
     */
    public boolean deleteTheme(String themeId) {
        try {
            log.info(I18N.get("theme.delete.deleting", themeId));
            Path themePath = getThemePath(themeId);

            if (!Files.exists(themePath)) {
                log.warn(I18N.get("theme.delete.not-found", themeId));
                return false;
            }

            // 递归删除目录 / Recursively delete directory
            deleteDirectory(themePath);

            log.info(I18N.get("theme.delete.success", themeId));
            return true;

        } catch (IOException e) {
            log.error(I18N.get("theme.delete.failed", themeId), e);
            return false;
        }
    }

    /**
     * 同步主题配置 / Sync theme configuration
     *
     * @param themeConfig 主题配置 / Theme configuration
     * @return 是否同步成功 / Whether sync succeeded
     */
    public boolean syncTheme(ThemeConfigDTO themeConfig) {
        try {
            String themeId = themeConfig.getId();
            log.info(I18N.get("theme.sync.syncing", themeId));
            Path themePath = getThemePath(themeId);
            Path configPath = themePath.resolve("theme.json");

            // 确保目录存在 / Ensure directory exists
            Files.createDirectories(themePath);

            // 保存配置 / Save configuration
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(themeConfig);
            Files.writeString(configPath, json);

            log.info(I18N.get("theme.sync.success", themeId));
            return true;

        } catch (IOException e) {
            log.error(I18N.get("theme.sync.failed", e.getMessage()), e);
            return false;
        }
    }

    // ========== 辅助方法 / Helper Methods ==========

    /**
     * 获取主题路径 / Get theme path
     */
    private Path getThemePath(String themeId) {
        return Paths.get(themeConfig.getUploadPath(), themeId);
    }

    /**
     * 保存主题文件 / Save theme files
     */
    private void saveThemeFiles(Path themePath, MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // 验证文件类型 / Validate file type
            String contentType = file.getContentType();
            if (contentType != null && !isAllowedFileType(contentType)) {
                log.warn(I18N.get("theme.upload.file-type-not-allowed", contentType));
                continue;
            }

            // 验证文件大小 / Validate file size
            if (file.getSize() > themeConfig.getMaxFileSize()) {
                log.warn(I18N.get("theme.upload.file-size-exceeded", file.getSize()));
                continue;
            }

            // 保存文件 / Save file
            String filename = file.getOriginalFilename();
            if (filename != null) {
                Path filePath = themePath.resolve(filename);
                file.transferTo(filePath);
                log.info(I18N.get("theme.upload.file-saved", filename));
            }
        }
    }

    /**
     * 检查文件类型是否允许 / Check if file type is allowed
     */
    private boolean isAllowedFileType(String contentType) {
        return Arrays.asList(themeConfig.getAllowedTypes()).contains(contentType);
    }

    /**
     * 递归删除目录 / Recursively delete directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error(I18N.get("theme.delete.file-delete-failed", path), e);
                    }
                });
        }
    }
}

