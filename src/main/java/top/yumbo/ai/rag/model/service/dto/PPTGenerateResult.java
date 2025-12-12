package top.yumbo.ai.rag.model.service.dto;

import lombok.Data;

/**
 * PPT 生成结果 (PPT Generation Result)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class PPTGenerateResult {

    /**
     * 是否成功 (Success)
     */
    private boolean success;

    /**
     * 消息 (Message)
     */
    private String message;

    /**
     * 文件 URL (File URL)
     */
    private String fileUrl;

    /**
     * 文件名 (File name)
     */
    private String fileName;

    /**
     * 文件大小（字节）(File size in bytes)
     */
    private long fileSize;
}

