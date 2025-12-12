package top.yumbo.ai.rag.spring.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主题上传响应DTO / Theme Upload Response DTO
 *
 * 主题上传操作的响应数据
 * Response data for theme upload operation
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeUploadResponse {

    /**
     * 操作是否成功 / Operation success
     */
    private boolean success;

    /**
     * 主题ID / Theme ID
     */
    private String themeId;

    /**
     * 服务器存储路径 / Server storage path
     */
    private String path;

    /**
     * 响应消息 / Response message
     */
    private String message;

    /**
     * 错误信息（如果失败）/ Error message (if failed)
     */
    private String error;
}

