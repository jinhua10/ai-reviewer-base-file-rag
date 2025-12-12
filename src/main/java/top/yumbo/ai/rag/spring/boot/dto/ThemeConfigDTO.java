package top.yumbo.ai.rag.spring.boot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 主题配置DTO / Theme Configuration DTO
 *
 * 用于主题配置的数据传输
 * Used for theme configuration data transfer
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class ThemeConfigDTO {

    /**
     * 主题ID / Theme ID
     */
    private String id;

    /**
     * 主题名称（多语言）/ Theme name (multilingual)
     */
    private Map<String, String> name;

    /**
     * 主题描述（多语言）/ Theme description (multilingual)
     */
    private Map<String, String> description;

    /**
     * 预览图路径 / Preview image path
     */
    private String preview;

    /**
     * 主题类型 / Theme type
     * builtin | custom
     */
    private String type;

    /**
     * 版本号 / Version number
     */
    private String version;

    /**
     * 作者 / Author
     */
    private String author;

    /**
     * 主题配置 / Theme configuration
     */
    private ThemeSettings config;

    /**
     * 主题状态 / Theme status
     * active | developing
     */
    private String status;

    /**
     * 来源 / Source
     * local | server
     */
    private String source;

    /**
     * 安装日期 / Install date
     */
    @JsonProperty("installDate")
    private String installDate;

    /**
     * 服务器路径 / Server path
     */
    @JsonProperty("serverPath")
    private String serverPath;

    /**
     * 主题设置 / Theme Settings
     */
    @Data
    public static class ThemeSettings {
        /**
         * 布局类型 / Layout type
         */
        private String layout;

        /**
         * 动画风格 / Animation style
         */
        private String animation;

        /**
         * 密度 / Density
         */
        private String density;

        /**
         * 颜色配置 / Color configuration
         */
        private Map<String, String> colors;

        /**
         * 装饰元素 / Decorations
         */
        private String[] decorations;
    }
}

