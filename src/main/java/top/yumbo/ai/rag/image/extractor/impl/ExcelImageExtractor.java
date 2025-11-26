package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 图片提取器
 * 支持 .xlsx 格式（使用 Apache POI）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class ExcelImageExtractor implements DocumentImageExtractor {

    @Override
    public List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception {
        List<ExtractedImage> images = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(documentStream)) {
            log.info("📄 Processing Excel: {}, sheets: {}", documentName, workbook.getNumberOfSheets());

            int sheetNum = 1;

            for (Sheet sheet : workbook) {
                // 提取工作表文本作为上下文
                String sheetText = extractSheetText(sheet);

                // 提取工作表中的图片
                List<ExtractedImage> sheetImages = extractImagesFromSheet(
                    (XSSFSheet) sheet, sheetNum, sheetText
                );

                images.addAll(sheetImages);
                sheetNum++;
            }

            log.info("✅ Extracted {} images from Excel: {}", images.size(), documentName);
        }

        return images;
    }

    /**
     * 从工作表中提取图片
     */
    private List<ExtractedImage> extractImagesFromSheet(XSSFSheet sheet, int sheetNum, String sheetText) {
        List<ExtractedImage> images = new ArrayList<>();

        try {
            XSSFDrawing drawing = sheet.getDrawingPatriarch();

            if (drawing == null) {
                return images;
            }

            for (XSSFShape shape : drawing.getShapes()) {
                if (shape instanceof XSSFPicture) {
                    XSSFPicture picture = (XSSFPicture) shape;

                    try {
                        XSSFPictureData pictureData = picture.getPictureData();
                        byte[] data = pictureData.getData();

                        // 跳过过小的图片
                        if (data.length < 1024) { // 小于 1KB
                            continue;
                        }

                        String format = getFormatFromPictureType(pictureData.getPictureType());

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
    private String extractSheetText(Sheet sheet) {
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
     * 从图片类型获取格式
     */
    private String getFormatFromPictureType(int pictureType) {
        switch (pictureType) {
            case Workbook.PICTURE_TYPE_PNG:
                return "png";
            case Workbook.PICTURE_TYPE_JPEG:
                return "jpg";
            case Workbook.PICTURE_TYPE_GIF:
                return "gif";
            case Workbook.PICTURE_TYPE_BMP:
            case Workbook.PICTURE_TYPE_DIB:
                return "bmp";
            default:
                return "png";
        }
    }

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".xlsx");
    }

    @Override
    public String getName() {
        return "Excel Image Extractor";
    }
}

