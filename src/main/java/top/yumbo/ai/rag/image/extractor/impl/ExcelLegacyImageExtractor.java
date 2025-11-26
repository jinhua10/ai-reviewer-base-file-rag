package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 97-2003 图片提取器
 * 支持 .xls 格式（使用 Apache POI HSSF）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class ExcelLegacyImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (HSSFWorkbook workbook = new HSSFWorkbook(documentStream)) {
            log.info("📄 Processing Excel 97-2003: {}, sheets: {}",
                    documentName, workbook.getNumberOfSheets());

            int sheetNum = 1;

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                HSSFSheet sheet = workbook.getSheetAt(i);

                // 提取工作表文本作为上下文
                String sheetText = extractSheetText(sheet);

                // 提取工作表中的图片
                List<ExtractedImage> sheetImages = extractImagesFromSheet(
                    sheet, sheetNum, sheetText
                );

                images.addAll(sheetImages);
                sheetNum++;
            }

            log.info("✅ Extracted {} images from Excel 97-2003: {}", images.size(), documentName);
        }

        return images;
    }

    /**
     * 从工作表中提取图片
     */
    private List<ExtractedImage> extractImagesFromSheet(HSSFSheet sheet, int sheetNum, String sheetText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            HSSFPatriarch patriarch = sheet.getDrawingPatriarch();

            if (patriarch == null) {
                return images;
            }

            for (HSSFShape shape : patriarch.getChildren()) {
                if (shape instanceof HSSFPicture) {
                    HSSFPicture picture = (HSSFPicture) shape;

                    try {
                        HSSFPictureData pictureData = picture.getPictureData();
                        byte[] data = pictureData.getData();

                        // 跳过过小的图片
                        if (data.length < 1024) { // 小于 1KB
                            continue;
                        }

                        String format = getFormatFromPictureType(pictureData.getFormat());

                        ExtractedImage extractedImage = ExtractedImage.builder()
                                .data(data)
                                .format(format)
                                .originalName("sheet" + sheetNum + "_image" + images.size())
                                .position(sheetNum)
                                .contextText(sheetText)
                                .fileSize(data.length)
                                .build();

                        images.add(extractedImage);

                        log.debug("  📸 Image found on sheet {}: {}KB",
                                sheetNum, data.length / 1024);
                    } catch (Exception e) {
                        log.warn("Failed to extract picture from sheet {}", sheetNum, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process sheet {}", sheetNum, e);
        }

        return images;
    }

    /**
     * 提取工作表文本（前几行数据）
     */
    private String extractSheetText(HSSFSheet sheet) {
        StringBuilder text = new StringBuilder();

        try {
            // 工作表名称
            text.append("Sheet: ").append(sheet.getSheetName()).append(". ");

            // 提取前 10 行的文本
            int rowCount = 0;
            for (Row row : sheet) {
                if (rowCount >= 10) break;

                for (Cell cell : row) {
                    try {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            text.append(cellValue).append(" ");
                        }
                    } catch (Exception e) {
                        // 忽略单元格错误
                    }
                }

                rowCount++;
            }
        } catch (Exception e) {
            log.warn("Failed to extract sheet text", e);
        }

        String result = text.toString().trim();

        // 限制长度
        if (result.length() > 1000) {
            result = result.substring(0, 1000);
        }

        return result;
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
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
     * 从图片格式获取扩展名
     */
    private String getFormatFromPictureType(int pictureFormat) {
        // HSSF picture format constants
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_PNG) return "png";
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_JPEG) return "jpg";
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_DIB) return "bmp";
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_WMF) return "wmf";
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_EMF) return "emf";
        if (pictureFormat == HSSFWorkbook.PICTURE_TYPE_PICT) return "pict";
        return "png"; // 默认
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".xls");
    }

    @Override
    public String getName() {
        return "Excel 97-2003 Image Extractor";
    }
}

