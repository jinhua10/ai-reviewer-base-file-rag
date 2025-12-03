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
    private top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService cacheService; // 幻灯片缓存服务（可选）

    public OfficeImageExtractor(SmartImageExtractor imageExtractor) {
        this(imageExtractor, 1); // 默认每次处理1张幻灯片
    }

    public OfficeImageExtractor(SmartImageExtractor imageExtractor, int batchSize) {
        this(imageExtractor, batchSize, null);
    }

    public OfficeImageExtractor(SmartImageExtractor imageExtractor, int batchSize,
                               top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService cacheService) {
        this.imageExtractor = imageExtractor;
        this.batchSize = Math.max(1, batchSize); // 至少为1
        this.cacheService = cacheService;
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
                content.append(extractWithBatchMode(allSlides, file));
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
     * 支持缓存，避免重复处理
     */
    private String extractWithBatchMode(List<XSLFSlide> allSlides, File pptFile) {
        StringBuilder content = new StringBuilder();
        VisionLLMStrategy visionStrategy = (VisionLLMStrategy) imageExtractor.getActiveStrategy();

        int totalSlides = allSlides.size();
        String pptPath = pptFile.getAbsolutePath();

        // 加载 PPT 缓存
        top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.PPTCache pptCache = null;
        if (cacheService != null) {
            pptCache = cacheService.getPPTCache(pptPath);
            if (pptCache == null) {
                // 创建新的 PPT 缓存
                pptCache = new top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.PPTCache();
                pptCache.setFilePath(pptPath);
                pptCache.setFileLastModified(pptFile.lastModified());
                pptCache.setFileSize(pptFile.length());
                pptCache.setTotalSlides(totalSlides);
                pptCache.setCacheTime(System.currentTimeMillis());
            }
        }

        int processedSlides = 0;
        int cachedCount = 0;
        int processedCount = 0;

        while (processedSlides < totalSlides) {
            int endIndex = Math.min(processedSlides + batchSize, totalSlides);
            List<XSLFSlide> batchSlides = allSlides.subList(processedSlides, endIndex);

            log.info("📦 处理幻灯片 {}-{}/{}", processedSlides + 1, endIndex, totalSlides);

            // 检查这批幻灯片是否需要处理
            List<Integer> slidesToProcess = new ArrayList<>();
            List<ImagePositionInfo> batchImages = new ArrayList<>();
            StringBuilder batchTextContent = new StringBuilder();

            for (int i = 0; i < batchSlides.size(); i++) {
                XSLFSlide slide = batchSlides.get(i);
                int slideNumber = processedSlides + i + 1;

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

                // 收集图片数据用于计算哈希
                List<byte[]> slideImageData = new ArrayList<>();
                List<ImagePositionInfo> slideImages = new ArrayList<>();

                int imageIndex = 0;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFPictureShape picture) {
                        XSLFPictureData pictureData = picture.getPictureData();
                        byte[] imageData = pictureData.getData();
                        slideImageData.add(imageData);

                        String imageName = String.format("slide%d_image%d.%s",
                            slideNumber, ++imageIndex, getPPTExtension(pictureData.getType()));

                        Rectangle2D anchor = picture.getAnchor();
                        ImagePositionInfo imgPos = new ImagePositionInfo(
                            imageData, imageName,
                            anchor.getX(), anchor.getY(),
                            anchor.getWidth(), anchor.getHeight(),
                            batchImages.size()
                        );

                        slideImages.add(imgPos);
                    }
                }

                // 计算幻灯片哈希
                String slideHash = cacheService != null ?
                    cacheService.calculateSlideHash(slideText.toString(), slideImageData) : null;

                // 检查缓存
                top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.SlideCache cachedSlide = null;
                if (cacheService != null && pptCache != null) {
                    cachedSlide = pptCache.getSlides().get(slideNumber);
                }

                boolean useCache = false;
                if (cacheService != null && cachedSlide != null && slideHash != null) {
                    useCache = !cacheService.needsUpdate(slideHash, cachedSlide);
                }

                // 添加幻灯片标题
                batchTextContent.append(LogMessageProvider.getMessage("log.office.slide_title", slideNumber));

                if (!slideText.isEmpty()) {
                    batchTextContent.append(LogMessageProvider.getMessage("log.office.slide_text"))
                                   .append(slideText);
                }

                if (useCache) {
                    // 使用缓存
                    log.info("💾 使用缓存: 幻灯片 {} ({} 张图片)", slideNumber, cachedSlide.getImageCount());
                    if (cachedSlide.getVisionLLMResult() != null && !cachedSlide.getVisionLLMResult().isEmpty()) {
                        batchTextContent.append("\n\n")
                                       .append(LogMessageProvider.getMessage("log.office.image_section"))
                                       .append(cachedSlide.getVisionLLMResult());
                    }
                    cachedCount++;
                } else {
                    // 需要处理
                    if (!slideImages.isEmpty()) {
                        slidesToProcess.add(slideNumber);
                        batchImages.addAll(slideImages);

                        // 创建新的缓存条目
                        if (cacheService != null && pptCache != null) {
                            top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.SlideCache newCache =
                                new top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.SlideCache();
                            newCache.setSlideNumber(slideNumber);
                            newCache.setContentHash(slideHash);
                            newCache.setSlideText(slideText.toString());
                            newCache.setImageCount(slideImages.size());
                            newCache.setProcessTime(System.currentTimeMillis());
                            pptCache.getSlides().put(slideNumber, newCache);
                        }
                    }
                }
            }

            // 批量处理需要更新的幻灯片图片
            if (!batchImages.isEmpty()) {
                log.info("📸 需要处理 {} 张图片（来自 {} 张幻灯片）", batchImages.size(), slidesToProcess.size());
                String imageContent = visionStrategy.extractContentBatchWithPosition(batchImages);

                if (imageContent != null && !imageContent.trim().isEmpty()) {
                    batchTextContent.append("\n\n")
                                   .append(LogMessageProvider.getMessage("log.office.image_section"))
                                   .append(imageContent);

                    // 更新缓存
                    if (cacheService != null && pptCache != null && !slidesToProcess.isEmpty()) {
                        // 将结果保存到对应的幻灯片缓存中
                        for (int slideNum : slidesToProcess) {
                            top.yumbo.ai.rag.spring.boot.service.SlideContentCacheService.SlideCache slideCache =
                                pptCache.getSlides().get(slideNum);
                            if (slideCache != null) {
                                slideCache.setVisionLLMResult(imageContent);
                            }
                        }
                    }

                    log.info("✅ 批量分析完成: {} 张图片 -> {} 字符", batchImages.size(), imageContent.length());
                    processedCount += slidesToProcess.size();
                }
            }

            content.append(batchTextContent);
            processedSlides = endIndex;
        }

        // 保存 PPT 缓存
        if (cacheService != null && pptCache != null) {
            cacheService.savePPTCache(pptPath, pptCache);
            log.info("💾 缓存统计: 使用缓存 {} 张，新处理 {} 张，总计 {} 张",
                cachedCount, processedCount, totalSlides);
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

