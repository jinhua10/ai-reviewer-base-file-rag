package top.yumbo.ai.rag.spring.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 主题信息DTO / Theme Info DTO
 *
 * 主题列表中的简要信息
 * Brief information in theme list
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeInfoDTO {

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
     * 主题类型 / Theme type
     */
    private String type;

    /**
     * 来源 / Source
     */
    private String source;

    /**
     * 版本号 / Version
     */
    private String version;

    /**
     * 作者 / Author
     */
    private String author;

    /**
     * 预览图 / Preview
     */
    private String preview;

    /**
     * 上传日期 / Upload date
     */
    private String uploadDate;
}

