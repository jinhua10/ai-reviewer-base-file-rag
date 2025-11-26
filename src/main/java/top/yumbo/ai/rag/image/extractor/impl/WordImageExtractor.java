package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Word 文档图片提取器
 * 支持 .docx 格式（使用 Apache POI）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class WordImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(documentStream)) {
            log.info("📄 Processing Word document: {}", documentName);

            int position = 1;

            // 遍历所有段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // 提取段落周围的文本作为上下文
                String contextText = extractContext(document, paragraph);

                // 提取段落中的图片
                List<ExtractedImage> paragraphImages = extractImagesFromParagraph(
                    paragraph, position, contextText
                );

                images.addAll(paragraphImages);
                position += paragraphImages.size();
            }

            // 提取表格中的图片
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            String contextText = cell.getText();
                            List<ExtractedImage> cellImages = extractImagesFromParagraph(
                                paragraph, position, contextText
                            );
                            images.addAll(cellImages);
                            position += cellImages.size();
                        }
                    }
                }
            }

            log.info("✅ Extracted {} images from Word: {}", images.size(), documentName);
        }

        return images;
    }

    /**
     * 从段落中提取图片
     */
    private List<ExtractedImage> extractImagesFromParagraph(XWPFParagraph paragraph,
                                                             int position,
                                                             String contextText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            for (XWPFRun run : paragraph.getRuns()) {
                // 获取嵌入的图片
                List<XWPFPicture> pictures = run.getEmbeddedPictures();

                for (XWPFPicture picture : pictures) {
                    try {
                        XWPFPictureData pictureData = picture.getPictureData();

                        byte[] data = pictureData.getData();

                        // 跳过过小的图片
                        if (data.length < 1024) { // 小于 1KB
                            continue;
                        }

                        String format = getFormatFromMimeType(pictureData.getPackagePart().getContentType());

                        ExtractedImage extractedImage = ExtractedImage.builder()
                                .data(data)
                                .format(format)
                                .originalName(pictureData.getFileName())
                                .position(position)
                                .contextText(contextText)
                                .fileSize(data.length)
                                .build();

                        images.add(extractedImage);

                        log.debug("  📸 Image found: {}, {}KB",
                                pictureData.getFileName(), data.length / 1024);
                    } catch (Exception e) {
                        log.warn("Failed to extract picture", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process paragraph", e);
        }

        return images;
    }

    /**
     * 提取图片周围的上下文文本
     */
    private String extractContext(XWPFDocument document, XWPFParagraph currentParagraph) {
        StringBuilder context = new StringBuilder();

        List<XWPFParagraph> paragraphs = document.getParagraphs();
        int currentIndex = paragraphs.indexOf(currentParagraph);

        // 获取前后各 2 个段落的文本
        int start = Math.max(0, currentIndex - 2);
        int end = Math.min(paragraphs.size(), currentIndex + 3);

        for (int i = start; i < end; i++) {
            context.append(paragraphs.get(i).getText()).append(" ");
        }

        String text = context.toString().trim();

        // 限制长度
        if (text.length() > 1000) {
            text = text.substring(0, 1000);
        }

        return text;
    }

    /**
     * 从 MIME 类型获取格式
     */
    private String getFormatFromMimeType(String mimeType) {
        if (mimeType.contains("png")) return "png";
        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return "jpg";
        if (mimeType.contains("gif")) return "gif";
        if (mimeType.contains("bmp")) return "bmp";
        return "png"; // 默认
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".docx");
    }

    @Override
    public String getName() {
        return "Word Image Extractor";
    }
}

