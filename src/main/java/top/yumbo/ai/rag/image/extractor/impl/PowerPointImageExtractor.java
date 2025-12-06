package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PowerPoint 图片提取器（PowerPoint image extractor）
 * 支持 .pptx 格式（使用 Apache POI）（Supports .pptx format (using Apache POI)）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class PowerPointImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (XMLSlideShow ppt = new XMLSlideShow(documentStream)) {
            log.info(I18N.get("log.image.ppt.processing", documentName, ppt.getSlides().size()));

            int slideNum = 1;

            for (XSLFSlide slide : ppt.getSlides()) {
                // 提取幻灯片文本作为上下文（Extract slide text as context）
                String slideText = extractSlideText(slide);

                // 提取幻灯片中的图片（Extract images from slide）
                List<ExtractedImage> slideImages = extractImagesFromSlide(
                    slide, slideNum, slideText
                );

                images.addAll(slideImages);
                slideNum++;
            }

            log.info(I18N.get("log.image.ppt.extracted", images.size(), documentName));
        }

        return images;
    }

    /**
     * 从幻灯片中提取图片（Extract images from slide）
     */
    private List<ExtractedImage> extractImagesFromSlide(XSLFSlide slide, int slideNum, String slideText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFPictureShape) {
                    XSLFPictureShape picture = (XSLFPictureShape) shape;

                    try {
                        XSLFPictureData pictureData = picture.getPictureData();
                        byte[] data = pictureData.getData();

                        // 跳过过小的图片（Skip small images）
                        if (data.length < 1024) { // 小于 1KB（Less than 1KB）
                            continue;
                        }

                        String format = getFormatFromContentType(pictureData.getContentType());

                        // 尝试获取图片的替代文本（通常是描述性文本）（Try to get alternative text of image (usually descriptive text)）
                        String altText = "";
                        try {
                            // 不同版本的 POI 可能没有 getAlternativeText 方法（Different versions of POI may not have getAlternativeText method）
                            // 使用反射或直接跳过（Use reflection or skip directly）
                            altText = picture.getShapeName();
                        } catch (Exception e) {
                            // 忽略（Ignore）
                        }

                        ExtractedImage extractedImage = ExtractedImage.builder()
                                .data(data)
                                .format(format)
                                .originalName(picture.getShapeName())
                                .position(slideNum)
                                .contextText(slideText + (altText.isEmpty() ? "" : " | " + altText))
                                .fileSize(data.length)
                                .build();

                        images.add(extractedImage);

                        log.debug(I18N.get("log.image.ppt.found", slideNum, picture.getShapeName(), data.length / 1024));
                    } catch (Exception e) {
                        log.warn(I18N.get("log.image.ppt.extract_failed", slideNum), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error(I18N.get("log.image.ppt.process_failed", slideNum), e);
        }

        return images;
    }

    /**
     * 提取幻灯片文本（Extract slide text）
     */
    private String extractSlideText(XSLFSlide slide) {
        StringBuilder text = new StringBuilder();

        try {
            // 获取幻灯片标题（Get slide title）
            String title = slide.getTitle();
            if (title != null && !title.isEmpty()) {
                text.append(title).append(". ");
            }

            // 获取所有文本（Get all text）
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    String shapeText = textShape.getText();
                    if (shapeText != null && !shapeText.isEmpty()) {
                        text.append(shapeText).append(" ");
                    }
                }
            }
        } catch (Exception e) {
            log.warn(I18N.get("log.image.ppt.text_failed"), e);
        }

        String result = text.toString().trim();

        // 限制长度（Limit length）
        if (result.length() > 1000) {
            result = result.substring(0, 1000);
        }

        return result;
    }

    /**
     * 从内容类型获取格式（Get format from content type）
     */
    private String getFormatFromContentType(String contentType) {
        if (contentType.contains("png")) return "png";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("bmp")) return "bmp";
        return "png"; // 默认（Default）
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pptx");
    }

    @Override
    public String getName() {
        return "PowerPoint Image Extractor";
    }
}
