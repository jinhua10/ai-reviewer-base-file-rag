package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 图片提取器（PDF image extractor）
 * 使用 Apache PDFBox 提取 PDF 中的图片（Use Apache PDFBox to extract images from PDF）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class PdfImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(documentStream.readAllBytes())) {
            int totalPages = document.getNumberOfPages();
            log.info(LogMessageProvider.getMessage("log.image.pdf.processing", documentName, totalPages));

            for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                PDPage page = document.getPage(pageNum);

                // 提取页面文本作为上下文（Extract page text as context）
                String pageText = extractPageText(document, pageNum);

                // 提取页面图片（Extract page images）
                List<ExtractedImage> pageImages = extractImagesFromPage(
                        page, pageNum + 1, pageText
                );

                images.addAll(pageImages);
            }

            log.info(LogMessageProvider.getMessage("log.image.pdf.extracted", images.size(), documentName));
        }

        return images;
    }

    /**
     * 从页面中提取图片（Extract images from page）
     */
    private List<ExtractedImage> extractImagesFromPage(PDPage page, int pageNum, String pageText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            PDResources resources = page.getResources();
            if (resources == null) {
                return images;
            }

            // 遍历页面中的所有图片资源
            for (var cosName : resources.getXObjectNames()) {
                try {
                    var xObject = resources.getXObject(cosName);

                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject imageXObject = (PDImageXObject) xObject;

                        // 转换为 BufferedImage
                        BufferedImage bufferedImage = imageXObject.getImage();

                        // 跳过过小的图片（可能是图标或装饰）
                        if (bufferedImage.getWidth() < 50 || bufferedImage.getHeight() < 50) {
                            continue;
                        }

                        // 转换为字节数组
                        byte[] imageData = imageToBytes(bufferedImage, "png");

                        ExtractedImage extractedImage = ExtractedImage.builder()
                                .data(imageData)
                                .format("png")
                                .originalName(cosName.getName())
                                .position(pageNum)
                                .contextText(pageText)
                                .width(bufferedImage.getWidth())
                                .height(bufferedImage.getHeight())
                                .fileSize(imageData.length)
                                .build();

                        images.add(extractedImage);

                        log.debug(LogMessageProvider.getMessage("log.image.pdf.found", pageNum, bufferedImage.getWidth(), bufferedImage.getHeight(), imageData.length / 1024));
                    }
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.image.pdf.extract_failed", cosName.getName()), e);
                }
            }
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.image.pdf.process_failed", pageNum), e);
        }

        return images;
    }

    /**
     * 提取页面文本（Extract page text）
     */
    private String extractPageText(PDDocument document, int pageNum) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNum + 1);
            stripper.setEndPage(pageNum + 1);

            String text = stripper.getText(document);

            // 限制上下文长度（取前后各 500 字符）
            if (text.length() > 1000) {
                text = text.substring(0, 1000);
            }

            return text.trim();
        } catch (Exception e) {
            log.warn(LogMessageProvider.getMessage("log.image.pdf.text_failed", pageNum), e);
            return "";
        }
    }

    /**
     * 将 BufferedImage 转换为字节数组（Convert BufferedImage to byte array）
     */
    private byte[] imageToBytes(BufferedImage image, String format) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        }
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String getName() {
        return "PDF Image Extractor";
    }
}
