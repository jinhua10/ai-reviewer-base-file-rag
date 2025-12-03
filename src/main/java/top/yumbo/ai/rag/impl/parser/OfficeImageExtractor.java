package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.impl.parser.image.ImagePositionInfo;
import top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor;
import top.yumbo.ai.rag.impl.parser.image.VisionLLMStrategy;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Office文档图片提取器
 * 
 * 专门用于从Office文档（PPTX、DOCX、XLSX）中提取图片并进行OCR识别
 * 支持以幻灯片为最小单位的批量处理
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class OfficeImageExtractor {

    private final SmartImageExtractor imageExtractor;
    private final int batchSize; // 批量处理的幻灯片数量

    public OfficeImageExtractor(SmartImageExtractor imageExtractor) {
        this(imageExtractor, 1); // 默认每次处理1张幻灯片
    }

    public OfficeImageExtractor(SmartImageExtractor imageExtractor, int batchSize) {
        this.imageExtractor = imageExtractor;
        this.batchSize = Math.max(1, batchSize); // 至少为1
    }

    /**
     * 从PPTX文件中提取图片内容
     * 以幻灯片为最小单位，支持批量处理多张幻灯片
     */
    public String extractFromPPTX(File file) {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            
            List<XSLFSlide> allSlides = ppt.getSlides();
            log.info(LogMessageProvider.getMessage("log.office.pptx_start", file.getName(), allSlides.size()));
            log.info("批量处理配置: 每次处理 {} 张幻灯片", batchSize);

            // 检查是否支持批量处理
            boolean supportsBatch = imageExtractor.getActiveStrategy() instanceof VisionLLMStrategy;

            if (supportsBatch && batchSize > 1) {
                // 使用批量处理模式
                content.append(extractWithBatchMode(allSlides, ppt));
            } else {
                // 使用单张幻灯片模式
                content.append(extractWithSingleMode(allSlides));
            }

            log.info(LogMessageProvider.getMessage("log.office.pptx_complete", file.getName()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.office.pptx_failed", file.getName()), e);
            content.append(LogMessageProvider.getMessage("log.office.process_failed", e.getMessage()));
        }

        return content.toString();
    }

    /**
     * 批量处理模式：以幻灯片为最小单位，批量发送给 Vision LLM
     */
    private String extractWithBatchMode(List<XSLFSlide> allSlides, XMLSlideShow ppt) {
        StringBuilder content = new StringBuilder();
        VisionLLMStrategy visionStrategy = (VisionLLMStrategy) imageExtractor.getActiveStrategy();

        int totalSlides = allSlides.size();
        int processedSlides = 0;

        while (processedSlides < totalSlides) {
            int endIndex = Math.min(processedSlides + batchSize, totalSlides);
            List<XSLFSlide> batchSlides = allSlides.subList(processedSlides, endIndex);

            log.info("📦 批量处理幻灯片 {}-{}/{}", processedSlides + 1, endIndex, totalSlides);

            // 收集这批幻灯片的所有图片（带位置信息）
            List<ImagePositionInfo> batchImages = new ArrayList<>();
            StringBuilder batchTextContent = new StringBuilder();

            for (int i = 0; i < batchSlides.size(); i++) {
                XSLFSlide slide = batchSlides.get(i);
                int slideNumber = processedSlides + i + 1;

                // 添加幻灯片标题
                batchTextContent.append(LogMessageProvider.getMessage("log.office.slide_title", slideNumber));

                // 提取文本内容
                StringBuilder slideText = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            slideText.append(text).append("\n");
                        }
                    }
                }

                if (!slideText.isEmpty()) {
                    batchTextContent.append(LogMessageProvider.getMessage("log.office.slide_text"))
                                   .append(slideText);
                }

                // 提取该幻灯片的所有图片
                int imageIndex = 0;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFPictureShape picture) {
                        XSLFPictureData pictureData = picture.getPictureData();
                        byte[] imageData = pictureData.getData();
                        String imageName = String.format("slide%d_image%d.%s",
                            slideNumber, ++imageIndex, getPPTExtension(pictureData.getType()));

                        // 获取图片位置信息
                        Rectangle2D anchor = picture.getAnchor();
                        ImagePositionInfo imgPos = new ImagePositionInfo(
                            imageData,
                            imageName,
                            anchor.getX(),
                            anchor.getY(),
                            anchor.getWidth(),
                            anchor.getHeight(),
                            batchImages.size()
                        );

                        batchImages.add(imgPos);

                        log.debug("收集图片: {} (位置: {:.0f},{:.0f}, 大小: {:.0f}x{:.0f}KB)",
                            imageName, anchor.getX(), anchor.getY(),
                            anchor.getWidth(), anchor.getHeight(), imageData.length / 1024.0);
                    }
                }
            }

            // 批量处理这批幻灯片的所有图片
            if (!batchImages.isEmpty()) {
                log.info("📸 本批次共 {} 张图片，开始批量分析...", batchImages.size());
                String imageContent = visionStrategy.extractContentBatchWithPosition(batchImages);

                if (imageContent != null && !imageContent.trim().isEmpty()) {
                    batchTextContent.append("\n\n")
                                   .append(LogMessageProvider.getMessage("log.office.image_section"))
                                   .append(imageContent);
                    log.info("✅ 批量分析完成: {} 张图片 -> {} 字符", batchImages.size(), imageContent.length());
                }
            } else {
                log.info("ℹ️  本批次幻灯片无图片");
            }

            content.append(batchTextContent);
            processedSlides = endIndex;
        }

        return content.toString();
    }

    /**
     * 单张幻灯片模式：逐个处理每张幻灯片的每张图片
     */
    private String extractWithSingleMode(List<XSLFSlide> allSlides) {
        StringBuilder content = new StringBuilder();

        int slideNumber = 0;
        for (XSLFSlide slide : allSlides) {
            slideNumber++;
            content.append(LogMessageProvider.getMessage("log.office.slide_title", slideNumber));

            // 提取文本内容
            StringBuilder slideText = new StringBuilder();
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    String text = textShape.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        slideText.append(text).append("\n");
                    }
                }
            }

            if (!slideText.isEmpty()) {
                content.append(LogMessageProvider.getMessage("log.office.slide_text")).append(slideText);
            }

            // 提取图片
            int imageCount = 0;
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFPictureShape picture) {
                    imageCount++;
                    XSLFPictureData pictureData = picture.getPictureData();

                    byte[] imageData = pictureData.getData();
                    String imageName = String.format("slide%d_image%d.%s",
                        slideNumber, imageCount, getPPTExtension(pictureData.getType()));

                    log.info(LogMessageProvider.getMessage("log.office.extract_image",
                        imageName, imageData.length / 1024));

                    // 使用OCR提取图片文字
                    String extractedText = imageExtractor.extractContent(
                        new ByteArrayInputStream(imageData), imageName);

                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info(LogMessageProvider.getMessage("log.office.extract_success",
                            imageName, extractedText.length()));
                        content.append(LogMessageProvider.getMessage("log.office.image_content"))
                               .append(extractedText);
                    } else {
                        log.warn(LogMessageProvider.getMessage("log.office.extract_empty", imageName));
                    }
                }
            }

            if (imageCount > 0) {
                log.info(LogMessageProvider.getMessage("log.office.slide_images", slideNumber, imageCount));
            }
        }
        
        return content.toString();
    }

    /**
     * 从DOCX文件中提取图片内容
     */
    public String extractFromDOCX(File file) {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            
            log.info(LogMessageProvider.getMessage("log.office.docx_start", file.getName()));

            // 提取文本
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }
            
            // 提取图片
            List<XWPFPictureData> pictures = doc.getAllPictures();
            if (!pictures.isEmpty()) {
                content.append(LogMessageProvider.getMessage("log.office.image_section"));

                int imageCount = 0;
                for (XWPFPictureData picture : pictures) {
                    imageCount++;
                    byte[] imageData = picture.getData();
                    String imageName = String.format("image%d.%s", 
                        imageCount, getExtension(picture.getPictureType()));
                    
                    log.info(LogMessageProvider.getMessage("log.office.extract_image",
                        imageName, imageData.length / 1024));

                    String extractedText = imageExtractor.extractContent(
                        new ByteArrayInputStream(imageData), imageName);
                    
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info(LogMessageProvider.getMessage("log.office.extract_success",
                            imageName, extractedText.length()));
                        content.append(extractedText);
                    } else {
                        log.warn(LogMessageProvider.getMessage("log.office.extract_empty", imageName));
                    }
                }
                
                log.info(LogMessageProvider.getMessage("log.office.docx_images", imageCount));
            }

            log.info(LogMessageProvider.getMessage("log.office.docx_complete", file.getName()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.office.docx_failed", file.getName()), e);
            content.append(LogMessageProvider.getMessage("log.office.process_failed", e.getMessage()));
        }
        
        return content.toString();
    }

    /**
     * 从XLSX文件中提取图片内容
     */
    public String extractFromXLSX(File file) {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             org.apache.poi.ss.usermodel.Workbook workbook = new XSSFWorkbook(fis)) {
            
            log.info(LogMessageProvider.getMessage("log.office.xlsx_start",
                file.getName(), workbook.getNumberOfSheets()));

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                content.append(LogMessageProvider.getMessage("log.office.sheet_title", sheet.getSheetName()));

                // 提取单元格内容
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        String cellValue = getCellValue(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            rowText.append(cellValue).append("\t");
                        }
                    }
                    if (rowText.length() > 0) {
                        content.append(rowText.toString().trim()).append("\n");
                    }
                }
                
                // 提取图片（XLSX图片提取较复杂，需要特殊处理）
                if (workbook instanceof XSSFWorkbook) {
                    extractXSSFImages((XSSFWorkbook) workbook, i, content);
                }
            }
            
            log.info(LogMessageProvider.getMessage("log.office.xlsx_complete", file.getName()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.office.xlsx_failed", file.getName()), e);
            content.append(LogMessageProvider.getMessage("log.office.process_failed", e.getMessage()));
        }
        
        return content.toString();
    }

    /**
     * 提取XLSX中的图片
     */
    private void extractXSSFImages(XSSFWorkbook workbook, int sheetIndex, StringBuilder content) {
        try {
            XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            
            if (drawing != null) {
                List<XSSFShape> shapes = drawing.getShapes();
                int imageCount = 0;
                
                for (XSSFShape shape : shapes) {
                    if (shape instanceof XSSFPicture) {
                        imageCount++;
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFPictureData pictureData = picture.getPictureData();
                        
                        byte[] imageData = pictureData.getData();
                        String imageName = String.format("sheet%d_image%d.%s", 
                            sheetIndex + 1, imageCount, getExtension(pictureData.getPictureType()));
                        
                        log.info(LogMessageProvider.getMessage("log.office.extract_image",
                            imageName, imageData.length / 1024));

                        String extractedText = imageExtractor.extractContent(
                            new ByteArrayInputStream(imageData), imageName);
                        
                        if (extractedText != null && !extractedText.trim().isEmpty()) {
                            log.info(LogMessageProvider.getMessage("log.office.extract_success",
                                imageName, extractedText.length()));
                            content.append(LogMessageProvider.getMessage("log.office.image_content"))
                                   .append(extractedText);
                        } else {
                            log.warn(LogMessageProvider.getMessage("log.office.extract_empty", imageName));
                        }
                    }
                }
                
                if (imageCount > 0) {
                    log.info(LogMessageProvider.getMessage("log.office.sheet_images",
                        sheetIndex + 1, imageCount));
                }
            }
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.office.xlsx_extract_failed"), e);
        }
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 根据PowerPoint图片类型获取扩展名
     */
    private String getPPTExtension(org.apache.poi.sl.usermodel.PictureData.PictureType pictureType) {
        if (pictureType == null) {
            return "img";
        }
        switch (pictureType) {
            case PNG:
                return "png";
            case JPEG:
                return "jpg";
            case EMF:
                return "emf";
            case WMF:
                return "wmf";
            case DIB:
                return "dib";
            case PICT:
                return "pict";
            case GIF:
                return "gif";
            case TIFF:
                return "tiff";
            default:
                return "img";
        }
    }

    /**
     * 根据图片类型获取扩展名（Excel/Word）
     */
    private String getExtension(int pictureType) {
        switch (pictureType) {
            case org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG:
                return "png";
            case org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_JPEG:
                return "jpg";
            case org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_EMF:
                return "emf";
            case org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_WMF:
                return "wmf";
            case org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_DIB:
                return "dib";
            default:
                return "img";
        }
    }
}

