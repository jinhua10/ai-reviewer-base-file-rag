package top.yumbo.ai.rag.image.extractor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 从文档中提取的图片信息
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
     * 图片数据（字节数组）
     */
    private byte[] data;

    /**
     * 图片格式（jpg, png, gif 等）
     */
    private String format;

    /**
     * 原始文件名（如果有）
     */
    private String originalName;

    /**
     * 图片在文档中的位置（页码、幻灯片号等）
     */
    private int position;

    /**
     * 图片在文档中的上下文文本
     * 用于 AI 分析图片的语义
     */
    private String contextText;

    /**
     * 图片类型（由 AI 分析得出）
     * 例如：架构图、流程图、数据图、截图、照片等
     */
    private String imageType;

    /**
     * AI 生成的图片描述
     */
    private String aiDescription;

    /**
     * 图片宽度
     */
    private int width;

    /**
     * 图片高度
     */
    private int height;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 获取显示名称
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

