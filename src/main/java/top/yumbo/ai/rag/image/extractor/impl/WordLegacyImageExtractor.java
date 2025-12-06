package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.PictureType;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Word 97-2003 文档图片提取器（Word 97-2003 document image extractor）
 * 支持 .doc 格式（使用 Apache POI HWPF）（Supports .doc format (using Apache POI HWPF)）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class WordLegacyImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (HWPFDocument document = new HWPFDocument(documentStream)) {
            log.info(I18N.get("log.image.word.legacy.processing", documentName));

            // 获取所有图片（Get all pictures）
            List<Picture> pictures = document.getPicturesTable().getAllPictures();

            if (pictures.isEmpty()) {
                log.info(I18N.get("log.image.word.legacy.no_images", documentName));
                return images;
            }

            // 提取文档文本作为上下文（Extract document text as context）
            String contextText = extractDocumentText(document);

            int position = 1;
            for (Picture picture : pictures) {
                try {
                    byte[] data = picture.getContent();

                    // 跳过过小的图片（Skip small images）
                    if (data.length < 1024) { // 小于 1KB（Less than 1KB）
                        continue;
                    }

                    String format = getFormatFromPictureType(picture.suggestPictureType());

                    ExtractedImage extractedImage = ExtractedImage.builder()
                            .data(data)
                            .format(format)
                            .originalName("image_" + position)
                            .position(position)
                            .contextText(contextText)
                            .width(picture.getWidth())
                            .height(picture.getHeight())
                            .fileSize(data.length)
                            .build();

                    images.add(extractedImage);

                    log.debug(I18N.get("log.image.word.legacy.found", picture.getWidth(), picture.getHeight(), data.length / 1024));

                    position++;
                } catch (Exception e) {
                    log.warn(I18N.get("log.image.word.legacy.extract_failed", position), e);
                }
            }

            log.info(I18N.get("log.image.word.legacy.extracted", images.size(), documentName));
        }

        return images;
    }

    /**
     * 提取文档文本（Extract document text）
     */
    private String extractDocumentText(HWPFDocument document) {
        try {
            String text = document.getText().toString();

            // 限制长度（Limit length）
            if (text.length() > 1000) {
                text = text.substring(0, 1000);
            }

            return text.trim();
        } catch (Exception e) {
            log.warn(I18N.get("log.image.word.legacy.text_failed"), e);
            return "";
        }
    }

    /**
     * 从图片类型获取格式（Get format from picture type）
     */
    private String getFormatFromPictureType(PictureType pictureType) {
        if (pictureType == null) {
            return "png";
        }

        // 使用 toString() 来判断，避免枚举常量不存在的问题（Use toString() to judge, avoid non-existent enum constants）
        String type = pictureType.toString().toUpperCase();

        if (type.contains("PNG")) return "png";
        if (type.contains("JPEG") || type.contains("JPG")) return "jpg";
        if (type.contains("GIF")) return "gif";
        if (type.contains("BMP") || type.contains("DIB")) return "bmp";
        if (type.contains("TIFF")) return "tiff";
        if (type.contains("WMF") || type.contains("EMF")) return "wmf";

        return "png"; // 默认（Default）
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".doc");
    }

    @Override
    public String getName() {
        return "Word 97-2003 Image Extractor";
    }
}
