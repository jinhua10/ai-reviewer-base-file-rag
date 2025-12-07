package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片内容提取器
 * 用于处理Excel等文档中的图片和二进制内容
 *
 * 支持的策略：
 * 1. 生成图片描述（尺寸、格式等）
 * 2. OCR识别文字（可选，需要额外依赖）
 * 3. 生成占位符标记
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class ImageContentExtractor {

    private final boolean enableOCR;
    private final boolean includeImageMetadata;
    private final int maxImageDescriptionLength;

    /**
     * OCR引擎接口（可扩展）
     */
    public interface OCREngine {
        /**
         * 识别图片中的文字
         *
         * @param imageData 图片数据
         * @return 识别的文字内容
         */
        String recognizeText(byte[] imageData);
    }

    private OCREngine ocrEngine;

    /**
     * 默认配置
     */
    public ImageContentExtractor() {
        this(false, true, 500);
    }

    /**
     * 自定义配置
     *
     * @param enableOCR 是否启用OCR
     * @param includeImageMetadata 是否包含图片元数据
     * @param maxImageDescriptionLength 图片描述的最大长度
     */
    public ImageContentExtractor(boolean enableOCR, boolean includeImageMetadata, int maxImageDescriptionLength) {
        this.enableOCR = enableOCR;
        this.includeImageMetadata = includeImageMetadata;
        this.maxImageDescriptionLength = maxImageDescriptionLength;

        log.info("ImageContentExtractor initialized: OCR={}, includeMetadata={}",
            enableOCR, includeImageMetadata);
    }

    /**
     * 设置OCR引擎
     */
    public void setOCREngine(OCREngine engine) {
        this.ocrEngine = engine;
        log.info("OCR engine configured: {}", engine.getClass().getSimpleName());
    }

    /**
     * 处理图片数据，转换为文本描述
     *
     * @param imageData 图片二进制数据
     * @param imageName 图片名称（可选）
     * @param imageIndex 图片在文档中的序号
     * @return 图片的文本表示
     */
    public String extractImageContent(byte[] imageData, String imageName, int imageIndex) {
        if (imageData == null || imageData.length == 0) {
            return String.format("[图片%d: 空数据]", imageIndex);
        }

        StringBuilder content = new StringBuilder();
        content.append(String.format("\n[图片%d", imageIndex));

        if (imageName != null && !imageName.isEmpty()) {
            content.append(": ").append(imageName);
        }
        content.append("]\n");

        try {
            // 1. 提取图片基本信息
            if (includeImageMetadata) {
                String metadata = extractImageMetadata(imageData);
                if (metadata != null) {
                    content.append(metadata);
                }
            }

            // 2. 尝试OCR识别文字
            if (enableOCR && ocrEngine != null) {
                try {
                    String ocrText = ocrEngine.recognizeText(imageData);
                    if (ocrText != null && !ocrText.trim().isEmpty()) {
                        content.append("图片中识别的文字: ").append(ocrText.trim()).append("\n");
                    } else {
                        content.append("(图片中未识别到文字)\n");
                    }
                } catch (Exception e) {
                    log.warn("OCR recognition failed for image {}: {}", imageIndex, e.getMessage());
                    content.append("(OCR识别失败)\n");
                }
            }

            // 3. 生成简短的描述
            content.append(generateImageDescription(imageData, imageIndex));

        } catch (Exception e) {
            log.error("Failed to extract image content: {}", e.getMessage());
            content.append("(图片处理失败: ").append(e.getMessage()).append(")\n");
        }

        content.append("[/图片").append(imageIndex).append("]\n");

        // 限制长度
        if (content.length() > maxImageDescriptionLength) {
            return content.substring(0, maxImageDescriptionLength) + "...[已截断]\n";
        }

        return content.toString();
    }

    /**
     * 提取图片元数据（尺寸、格式等）
     */
    private String extractImageMetadata(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

            if (image == null) {
                return null;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            String format = guessImageFormat(imageData);

            return String.format("- 尺寸: %dx%d 像素\n- 格式: %s\n- 大小: %.2f KB\n",
                width, height, format, imageData.length / 1024.0);

        } catch (IOException e) {
            log.debug("Failed to read image metadata: {}", e.getMessage());
            return String.format("- 大小: %.2f KB\n", imageData.length / 1024.0);
        }
    }

    /**
     * 猜测图片格式
     */
    private String guessImageFormat(byte[] imageData) {
        if (imageData.length < 4) {
            return "未知";
        }

        // PNG
        if (imageData[0] == (byte)0x89 && imageData[1] == 0x50 &&
            imageData[2] == 0x4E && imageData[3] == 0x47) {
            return "PNG";
        }

        // JPEG
        if (imageData[0] == (byte)0xFF && imageData[1] == (byte)0xD8) {
            return "JPEG";
        }

        // GIF
        if (imageData[0] == 0x47 && imageData[1] == 0x49 && imageData[2] == 0x46) {
            return "GIF";
        }

        // BMP
        if (imageData[0] == 0x42 && imageData[1] == 0x4D) {
            return "BMP";
        }

        return "未知格式";
    }

    /**
     * 生成图片的简短描述
     */
    private String generateImageDescription(byte[] imageData, int imageIndex) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

            if (image == null) {
                return "- 无法解析图片内容\n";
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // 根据尺寸判断图片类型
            String description;
            if (width > 1000 || height > 1000) {
                description = "大尺寸图片";
            } else if (width < 100 && height < 100) {
                description = "图标或小图";
            } else if (Math.abs(width - height) < 50) {
                description = "方形图片";
            } else if (width > height * 1.5) {
                description = "横向图片";
            } else if (height > width * 1.5) {
                description = "纵向图片";
            } else {
                description = "常规图片";
            }

            return String.format("- 类型: %s\n", description);

        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 生成图片占位符（最简单的策略）
     */
    public String generatePlaceholder(int imageIndex, String imageName) {
        if (imageName != null && !imageName.isEmpty()) {
            return String.format("[图片%d: %s]", imageIndex, imageName);
        } else {
            return String.format("[图片%d]", imageIndex);
        }
    }

    /**
     * 将图片转换为Base64编码（可用于存储或传输）
     */
    public String imageToBase64(byte[] imageData) {
        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * 从Base64解码图片
     */
    public byte[] base64ToImage(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * 检查数据是否为图片
     */
    public boolean isImage(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private boolean enableOCR = false;
        private boolean includeImageMetadata = true;
        private int maxImageDescriptionLength = 500;
        private OCREngine ocrEngine = null;

        public Builder enableOCR(boolean enable) {
            this.enableOCR = enable;
            return this;
        }

        public Builder includeImageMetadata(boolean include) {
            this.includeImageMetadata = include;
            return this;
        }

        public Builder maxImageDescriptionLength(int length) {
            this.maxImageDescriptionLength = length;
            return this;
        }

        public Builder ocrEngine(OCREngine engine) {
            this.ocrEngine = engine;
            this.enableOCR = true; // 自动启用OCR
            return this;
        }

        public ImageContentExtractor build() {
            ImageContentExtractor extractor = new ImageContentExtractor(
                enableOCR, includeImageMetadata, maxImageDescriptionLength);

            if (ocrEngine != null) {
                extractor.setOCREngine(ocrEngine);
            }

            return extractor;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

