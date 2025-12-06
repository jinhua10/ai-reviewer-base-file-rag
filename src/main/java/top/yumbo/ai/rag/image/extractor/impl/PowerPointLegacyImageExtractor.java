package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.*;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PowerPoint 97-2003 图片提取器（PowerPoint 97-2003 image extractor）
 * 支持 .ppt 格式（使用 Apache POI HSLF）（Supports .ppt format (using Apache POI HSLF)）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class PowerPointLegacyImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (HSLFSlideShow ppt = new HSLFSlideShow(documentStream)) {
            log.info(I18N.get("log.image.ppt.legacy.processing", documentName, ppt.getSlides().size()));

            int slideNum = 1;

            for (HSLFSlide slide : ppt.getSlides()) {
                // 提取幻灯片文本作为上下文（Extract slide text as context）
                String slideText = extractSlideText(slide);

                // 提取幻灯片中的图片（Extract images from slide）
                List<ExtractedImage> slideImages = extractImagesFromSlide(
                    slide, slideNum, slideText
                );

                images.addAll(slideImages);
                slideNum++;
            }

            log.info(I18N.get("log.image.ppt.legacy.extracted", images.size(), documentName));
        }

        return images;
    }

    /**
     * 从幻灯片中提取图片（Extract images from slide）
     */
    private List<ExtractedImage> extractImagesFromSlide(HSLFSlide slide, int slideNum, String slideText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            for (HSLFShape shape : slide.getShapes()) {
                if (shape instanceof HSLFPictureShape) {
                    HSLFPictureShape picture = (HSLFPictureShape) shape;

                    try {
                        HSLFPictureData pictureData = picture.getPictureData();
                        byte[] data = pictureData.getData();

                        // 跳过过小的图片（Skip small images）
                        if (data.length < 1024) { // 小于 1KB（Less than 1KB）
                            continue;
                        }

                        String format = getFormatFromContentType(pictureData.getType());

                        ExtractedImage extractedImage = ExtractedImage.builder()
                                .data(data)
                                .format(format)
                                .originalName(picture.getShapeName())
                                .position(slideNum)
                                .contextText(slideText)
                                .fileSize(data.length)
                                .build();

                        images.add(extractedImage);

                        log.debug(I18N.get("log.image.ppt.legacy.found", slideNum, picture.getShapeName(), data.length / 1024));
                    } catch (Exception e) {
                        log.warn(I18N.get("log.image.ppt.legacy.extract_failed", slideNum), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error(I18N.get("log.image.ppt.legacy.process_failed", slideNum), e);
        }

        return images;
    }

    /**
     * 提取幻灯片文本（Extract slide text）
     */
    private String extractSlideText(HSLFSlide slide) {
        StringBuilder text = new StringBuilder();

        try {
            // 获取幻灯片标题（Get slide title）
            String title = slide.getTitle();
            if (title != null && !title.isEmpty()) {
                text.append(title).append(". ");
            }

            // 获取所有文本（Get all text）
            for (HSLFShape shape : slide.getShapes()) {
                if (shape instanceof HSLFTextShape) {
                    HSLFTextShape textShape = (HSLFTextShape) shape;
                    String shapeText = textShape.getText();
                    if (shapeText != null && !shapeText.isEmpty()) {
                        text.append(shapeText).append(" ");
                    }
                }
            }
        } catch (Exception e) {
            log.warn(I18N.get("log.image.ppt.legacy.text_failed"), e);
        }

        String result = text.toString().trim();

        // 限制长度（Limit length）
        if (result.length() > 1000) {
            result = result.substring(0, 1000);
        }

        return result;
    }

    /**
     * 从图片类型获取格式（Get format from picture type）
     */
    private String getFormatFromContentType(HSLFPictureData.PictureType pictureType) {
        if (pictureType == null) {
            return "png";
        }

        // 使用 toString() 来判断，避免枚举常量不存在的问题（Use toString() to judge, avoid non-existent enum constants）
        String type = pictureType.toString().toUpperCase();

        if (type.contains("PNG")) return "png";
        if (type.contains("JPEG") || type.contains("JPG")) return "jpg";
        if (type.contains("GIF")) return "gif";
        if (type.contains("BMP") || type.contains("DIB")) return "bmp";
        if (type.contains("WMF") || type.contains("EMF")) return "wmf";
        if (type.contains("PICT")) return "pict";

        return "png"; // 默认（Default）
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".ppt");
    }

    @Override
    public String getName() {
        return "PowerPoint 97-2003 Image Extractor";
    }
}
