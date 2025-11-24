package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Office文档图片提取器
 * 
 * 专门用于从Office文档（PPTX、DOCX、XLSX）中提取图片并进行OCR识别
 * 
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class OfficeImageExtractor {

    private final SmartImageExtractor imageExtractor;

    public OfficeImageExtractor(SmartImageExtractor imageExtractor) {
        this.imageExtractor = imageExtractor;
    }

    /**
     * 从PPTX文件中提取图片内容
     */
    public String extractFromPPTX(File file) {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            
            log.info("开始处理PPTX文件: {}, 共{}张幻灯片", file.getName(), ppt.getSlides().size());
            
            int slideNumber = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideNumber++;
                content.append(String.format("\n\n========== 幻灯片 %d ==========\n", slideNumber));
                
                // 提取文本内容
                StringBuilder slideText = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            slideText.append(text).append("\n");
                        }
                    }
                }
                
                if (slideText.length() > 0) {
                    content.append("【文字内容】\n").append(slideText);
                }
                
                // 提取图片
                int imageCount = 0;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFPictureShape) {
                        imageCount++;
                        XSLFPictureShape picture = (XSLFPictureShape) shape;
                        XSLFPictureData pictureData = picture.getPictureData();
                        
                        byte[] imageData = pictureData.getData();
                        String imageName = String.format("slide%d_image%d.%s", 
                            slideNumber, imageCount, getPPTExtension(pictureData.getType()));
                        
                        log.info("📷 提取图片: {} ({}KB)", imageName, imageData.length / 1024);

                        // 使用OCR提取图片文字
                        String extractedText = imageExtractor.extractContent(
                            new ByteArrayInputStream(imageData), imageName);
                        
                        if (extractedText != null && !extractedText.trim().isEmpty()) {
                            log.info("✅ 图片内容提取成功: {} -> {} 字符", imageName, extractedText.length());
                            content.append("\n【图片内容】\n").append(extractedText);
                        } else {
                            log.warn("⚠️  图片内容为空: {}", imageName);
                        }
                    }
                }
                
                if (imageCount > 0) {
                    log.info("幻灯片 {} 包含 {} 张图片", slideNumber, imageCount);
                }
            }
            
            log.info("✅ PPTX处理完成: {}", file.getName());
            
        } catch (Exception e) {
            log.error("处理PPTX文件失败: {}", file.getName(), e);
            content.append(String.format("\n[处理失败: %s]\n", e.getMessage()));
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
            
            log.info("开始处理DOCX文件: {}", file.getName());
            
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
                content.append("\n\n========== 文档图片 ==========\n");
                
                int imageCount = 0;
                for (XWPFPictureData picture : pictures) {
                    imageCount++;
                    byte[] imageData = picture.getData();
                    String imageName = String.format("image%d.%s", 
                        imageCount, getExtension(picture.getPictureType()));
                    
                    log.info("📷 提取图片: {} ({}KB)", imageName, imageData.length / 1024);

                    String extractedText = imageExtractor.extractContent(
                        new ByteArrayInputStream(imageData), imageName);
                    
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info("✅ 图片内容提取成功: {} -> {} 字符", imageName, extractedText.length());
                        content.append(extractedText);
                    } else {
                        log.warn("⚠️  图片内容为空: {}", imageName);
                    }
                }
                
                log.info("✅ 从DOCX提取了 {} 张图片", imageCount);
            }
            
        } catch (Exception e) {
            log.error("处理DOCX文件失败: {}", file.getName(), e);
            content.append(String.format("\n[处理失败: %s]\n", e.getMessage()));
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
            
            log.info("开始处理XLSX文件: {}, 共{}个工作表", file.getName(), workbook.getNumberOfSheets());
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                content.append(String.format("\n\n========== 工作表: %s ==========\n", sheet.getSheetName()));
                
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
            
            log.info("✅ XLSX处理完成: {}", file.getName());
            
        } catch (Exception e) {
            log.error("处理XLSX文件失败: {}", file.getName(), e);
            content.append(String.format("\n[处理失败: %s]\n", e.getMessage()));
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
                        
                        log.info("📷 提取图片: {} ({}KB)", imageName, imageData.length / 1024);

                        String extractedText = imageExtractor.extractContent(
                            new ByteArrayInputStream(imageData), imageName);
                        
                        if (extractedText != null && !extractedText.trim().isEmpty()) {
                            log.info("✅ 图片内容提取成功: {} -> {} 字符", imageName, extractedText.length());
                            content.append("\n【图片内容】\n").append(extractedText);
                        } else {
                            log.warn("⚠️  图片内容为空: {}", imageName);
                        }
                    }
                }
                
                if (imageCount > 0) {
                    log.info("工作表 {} 包含 {} 张图片", sheetIndex + 1, imageCount);
                }
            }
        } catch (Exception e) {
            log.error("提取XLSX图片失败", e);
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

