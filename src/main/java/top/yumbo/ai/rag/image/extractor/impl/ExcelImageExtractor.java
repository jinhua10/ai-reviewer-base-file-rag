package top.yumbo.ai.rag.image.extractor.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import top.yumbo.ai.rag.image.extractor.DocumentImageExtractor;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 图片提取器（Excel image extractor）
 * 支持 .xlsx 格式（使用 Apache POI）（Supports .xlsx format (using Apache POI)）
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
            log.info(LogMessageProvider.getMessage("log.image.excel.processing", documentName, workbook.getNumberOfSheets()));

            int sheetNum = 1;

            for (Sheet sheet : workbook) {
                // 提取工作表文本作为上下文（Extract sheet text as context）
                String sheetText = extractSheetText(sheet);

                // 提取工作表中的图片（Extract images from sheet）
                List<ExtractedImage> sheetImages = extractImagesFromSheet(
                    (XSSFSheet) sheet, sheetNum, sheetText
                );

                images.addAll(sheetImages);
                sheetNum++;
            }

            log.info(LogMessageProvider.getMessage("log.image.excel.extracted", images.size(), documentName));
        }

        return images;
    }

    /**
     * 从工作表中提取图片（Extract images from sheet）
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

                        // 跳过过小的图片（Skip small images）
                        if (data.length < 1024) { // 小于 1KB（Less than 1KB）
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

                        log.debug(LogMessageProvider.getMessage("log.image.excel.found", sheetNum, data.length / 1024));
                    } catch (Exception e) {
                        log.warn(LogMessageProvider.getMessage("log.image.excel.extract_failed", sheetNum), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.image.excel.process_failed", sheetNum), e);
        }

        return images;
    }

    /**
     * 提取工作表文本（前几行数据）（Extract sheet text (first few rows of data)）
     */
    private String extractSheetText(Sheet sheet) {
        StringBuilder text = new StringBuilder();

        try {
            // 工作表名称（Sheet name）
            text.append("Sheet: ").append(sheet.getSheetName()).append(". ");

            // 提取前 10 行的文本（Extract text from first 10 rows）
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
                        // 忽略单元格错误（Ignore cell errors）
                    }
                }

                rowCount++;
            }
        } catch (Exception e) {
            log.warn(LogMessageProvider.getMessage("log.image.excel.text_failed"), e);
        }

        String result = text.toString().trim();

        // 限制长度（Limit length）
        if (result.length() > 1000) {
            result = result.substring(0, 1000);
        }

        return result;
    }

    /**
     * 获取单元格值作为字符串（Get cell value as string）
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
     * 从图片类型获取格式（Get format from picture type）
     */
    private String getFormatFromPictureType(int pictureType) {
        // POI 常量定义（POI constant definitions）
        if (pictureType == 6) return "png";      // PICTURE_TYPE_PNG
        if (pictureType == 5) return "jpg";      // PICTURE_TYPE_JPEG
        if (pictureType == 8) return "gif";      // PICTURE_TYPE_GIF (可能不存在)（may not exist）
        if (pictureType == 7 || pictureType == 2) return "bmp";  // PICTURE_TYPE_BMP, PICTURE_TYPE_DIB
        return "png"; // 默认（Default）
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
