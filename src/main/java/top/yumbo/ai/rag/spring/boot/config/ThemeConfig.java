package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 主题配置类 / Theme Configuration
 *
 * 配置主题上传路径和限制
 * Configure theme upload path and restrictions
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "theme")
public class ThemeConfig {

    /**
     * 主题上传路径 / Theme upload path
     * 默认: src/main/resources/static/themes/
     */
    private String uploadPath = "src/main/resources/static/themes/";

    /**
     * 最大文件大小（字节）/ Maximum file size (bytes)
     * 默认: 10MB
     */
    private long maxFileSize = 10 * 1024 * 1024;

    /**
     * 允许的文件类型 / Allowed file types
     */
    private String[] allowedTypes = {
        "application/json",
        "text/css",
        "text/javascript",
        "application/javascript",
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/gif",
        "image/svg+xml"
    };
}

