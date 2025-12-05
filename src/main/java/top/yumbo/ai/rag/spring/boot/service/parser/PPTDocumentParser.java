package top.yumbo.ai.rag.spring.boot.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSource;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PPT 文档解析器
 *
 * 将 PPT/PPTX 文件解析为幻灯片片段列表
 */
@Slf4j
@Component
public class PPTDocumentParser implements DocumentParser {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList("ppt", "pptx");

    @Override
    public boolean supports(String documentPath, String mimeType) {
        if (documentPath == null) {
            return false;
        }

        String lowerPath = documentPath.toLowerCase();
        return lowerPath.endsWith(".ppt") || lowerPath.endsWith(".pptx")
                || "application/vnd.ms-powerpoint".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(mimeType);
    }

    @Override
    public List<DocumentSegment> parse(String documentPath) throws IOException {
        File file = new File(documentPath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + documentPath);
        }

        List<DocumentSegment> segments = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            List<XSLFSlide> slides = ppt.getSlides();
            int totalSlides = slides.size();

            log.info("📊 解析 PPT 文件: {} ({} 张幻灯片)", file.getName(), totalSlides);

            // 创建文档来源信息
            DocumentSource source = DocumentSource.builder()
                    .documentType("ppt")
                    .documentName(file.getName())
                    .documentPath(documentPath)
                    .totalSegments(totalSlides)
                    .fileSize(file.length())
                    .mimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    .build();

            // 解析每张幻灯片
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                int slideNumber = i + 1;

                DocumentSegment segment = parseSlide(slide, slideNumber, source);
                segments.add(segment);

                log.debug("  解析幻灯片 {}: {}", slideNumber, segment.getTitle());
            }

        } catch (Exception e) {
            log.error("解析 PPT 文件失败: {}", documentPath, e);
            throw new IOException("解析 PPT 文件失败: " + e.getMessage(), e);
        }

        return segments;
    }

    @Override
    public List<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String getParserName() {
        return "PPT 文档解析器";
    }

    /**
     * 解析单张幻灯片
     */
    private DocumentSegment parseSlide(XSLFSlide slide, int slideNumber, DocumentSource source) {
        String title = extractTitle(slide);
        String content = extractContent(slide);
        List<String> images = extractImages(slide);
        List<String> tables = extractTables(slide);

        return DocumentSegment.builder()
                .id("slide-" + slideNumber)
                .index(slideNumber)
                .type(SegmentType.SLIDE)
                .title(title != null ? title : "幻灯片 " + slideNumber)
                .textContent(content)
                .images(images)
                .tables(tables)
                .source(source)
                .build();
    }

    /**
     * 提取幻灯片标题
     */
    private String extractTitle(XSLFSlide slide) {
        // 尝试从标题占位符获取
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape) shape;
                // 检查是否是标题占位符
                if (textShape.getTextType() != null &&
                    textShape.getTextType().name().contains("TITLE")) {
                    String text = textShape.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        return text.trim();
                    }
                }
            }
        }

        // 降级：取第一个非空文本框的内容作为标题
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                String text = ((XSLFTextShape) shape).getText();
                if (text != null && !text.trim().isEmpty()) {
                    // 取第一行作为标题
                    String firstLine = text.split("\n")[0].trim();
                    if (!firstLine.isEmpty() && firstLine.length() <= 100) {
                        return firstLine;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 提取幻灯片文本内容
     */
    private String extractContent(XSLFSlide slide) {
        StringBuilder content = new StringBuilder();

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape) shape;
                String text = textShape.getText();
                if (text != null && !text.trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append("\n\n");
                    }
                    content.append(text.trim());
                }
            } else if (shape instanceof XSLFTable) {
                // 表格内容在 extractTables 中处理
            }
        }

        return content.toString();
    }

    /**
     * 提取幻灯片中的图片信息
     */
    private List<String> extractImages(XSLFSlide slide) {
        List<String> images = new ArrayList<>();

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFPictureShape) {
                XSLFPictureShape picture = (XSLFPictureShape) shape;
                XSLFPictureData pictureData = picture.getPictureData();
                if (pictureData != null) {
                    // 记录图片信息（不存储实际数据，节省内存）
                    String imageInfo = String.format("[图片: %s, 类型: %s]",
                            shape.getShapeName(),
                            pictureData.getContentType());
                    images.add(imageInfo);
                }
            }
        }

        return images;
    }

    /**
     * 提取幻灯片中的表格
     */
    private List<String> extractTables(XSLFSlide slide) {
        List<String> tables = new ArrayList<>();

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTable) {
                XSLFTable table = (XSLFTable) shape;
                StringBuilder tableContent = new StringBuilder();
                tableContent.append("[表格]\n");

                for (int row = 0; row < table.getNumberOfRows(); row++) {
                    XSLFTableRow tableRow = table.getRows().get(row);
                    List<String> cells = new ArrayList<>();
                    for (XSLFTableCell cell : tableRow.getCells()) {
                        cells.add(cell.getText() != null ? cell.getText() : "");
                    }
                    tableContent.append("| ").append(String.join(" | ", cells)).append(" |\n");
                }

                tables.add(tableContent.toString());
            }
        }

        return tables;
    }
}

