package top.yumbo.ai.rag.image.extractor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从文档中提取的图片信息（Image information extracted from document）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedImage {

    /**
     * 图片数据（字节数组）（Image data (byte array)）
     */
    private byte[] data;

    /**
     * 图片格式（jpg, png, gif 等）（Image format (jpg, png, gif, etc.)）
     */
    private String format;

    /**
     * 原始文件名（如果有）（Original filename (if any)）
     */
    private String originalName;

    /**
     * 图片在文档中的位置（页码、幻灯片号等）（Image position in document (page number, slide number, etc.)）
     */
    private int position;

    /**
     * 图片在文档文本中的字符位置（Character position in document text）
     * 用于将图片文本插入到正确位置（Used to insert image text at the correct position）
     */
    private Integer charPositionInDocument;

    /**
     * 图片在文档中的上下文文本（Context text of image in document）
     * 用于 AI 分析图片的语义（Used for AI to analyze image semantics）
     */
    private String contextText;

    /**
     * 图片前的上下文（约100字符）（Context before image (about 100 characters)）
     * 用于帮助 Vision LLM 更好地理解图片（Used to help Vision LLM better understand the image）
     */
    private String contextBefore;

    /**
     * 图片后的上下文（约100字符）（Context after image (about 100 characters)）
     * 用于帮助 Vision LLM 更好地理解图片（Used to help Vision LLM better understand the image）
     */
    private String contextAfter;

    /**
     * 图片类型（由 AI 分析得出）（Image type (determined by AI analysis)）
     * 例如：架构图、流程图、数据图、截图、照片等（For example: architecture diagram, flowchart, data chart, screenshot, photo, etc.）
     */
    private String imageType;

    /**
     * AI 生成的图片描述（AI generated image description）
     */
    private String aiDescription;

    /**
     * 图片宽度（Image width）
     */
    private int width;

    /**
     * 图片高度（Image height）
     */
    private int height;

    /**
     * 文件大小（字节）（File size (bytes)）
     */
    private long fileSize;

    /**
     * 获取显示名称（Get display name）
     */
    public String getDisplayName() {
        if (originalName != null && !originalName.isEmpty()) {
            return originalName;
        }

        if (imageType != null && !imageType.isEmpty()) {
            return imageType + "_" + position;
        }

        return "image_" + position + "." + format;
    }
}
