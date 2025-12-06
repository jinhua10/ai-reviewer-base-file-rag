package top.yumbo.ai.rag.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片信息（Image information）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageInfo {

    /**
     * 图片ID（Image ID）
     */
    private String imageId;

    /**
     * 文档ID（Document ID）
     */
    private String documentId;

    /**
     * 文件名（Filename）
     */
    private String filename;

    /**
     * 原始文件名（Original filename）
     */
    private String originalFilename;

    /**
     * 文件路径（File path）
     */
    private String filePath;

    /**
     * 文件大小（字节）（File size (bytes)）
     */
    private long fileSize;

    /**
     * 图片格式（Image format）
     */
    private String format;

    /**
     * 图片描述（可选）（Image description (optional)）
     */
    private String description;

    /**
     * 图片在文档中的位置（字符偏移量）
     * Position in document (character offset)
     * 用于将提取的图片文本插入到正确位置
     * Used to insert extracted image text at the correct position
     */
    private Integer positionInDocument;

    /**
     * 图片前的上下文文本（用于 Vision LLM 理解图片）
     * Context text before the image (for Vision LLM understanding)
     */
    private String contextBefore;

    /**
     * 图片后的上下文文本（用于 Vision LLM 理解图片）
     * Context text after the image (for Vision LLM understanding)
     */
    private String contextAfter;

    /**
     * Vision LLM 提取的图片文本内容
     * Extracted image text content by Vision LLM
     */
    private String extractedText;

    /**
     * 获取访问 URL（Get access URL）
     */
    public String getUrl() {
        return String.format("/api/images/%s/%s", documentId, filename);
    }

    /**
     * 获取 Markdown 引用（Get Markdown reference）
     */
    public String getMarkdownReference() {
        String alt = description != null ? description : originalFilename;
        return String.format("![%s](%s)", alt, getUrl());
    }
}
