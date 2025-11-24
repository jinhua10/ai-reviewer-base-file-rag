package top.yumbo.ai.rag.impl.parser.image;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;

/**
 * Tesseract OCR 策略
 *
 * 使用 Tesseract OCR 提取图片中的文字
 *
 * 依赖: net.sourceforge.tess4j:tess4j
 * 需要安装 Tesseract OCR 或配置 tessdata 路径
 *
 * 使用方法:
 * 1. 添加 Maven 依赖:
 *    <dependency>
 *        <groupId>net.sourceforge.tess4j</groupId>
 *        <artifactId>tess4j</artifactId>
 *        <version>5.9.0</version>
 *    </dependency>
 *
 * 2. 下载语言包:
 *    中文: https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
 *    英文: https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
 *
 * 3. 配置 tessdata 路径:
 *    System.setProperty("TESSDATA_PREFIX", "/path/to/tessdata");
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class TesseractOCRStrategy implements ImageContentExtractorStrategy {

    private final String tessdataPath;
    private final String language;
    private boolean available = false;

    /**
     * 默认构造函数（中文+英文）
     */
    public TesseractOCRStrategy() {
        this(null, "chi_sim+eng");
    }

    /**
     * 自定义构造函数
     *
     * @param tessdataPath tessdata 路径（null则使用系统默认）
     * @param language 语言（chi_sim=简体中文，eng=英文）
     */
    public TesseractOCRStrategy(String tessdataPath, String language) {
        this.tessdataPath = tessdataPath;
        this.language = language;
        checkAvailability();
    }

    private void checkAvailability() {
        try {
            // 检查 Tesseract 类是否存在
            Class.forName("net.sourceforge.tess4j.Tesseract");
            available = true;
            log.info("✅ Tesseract OCR 可用 (语言: {})", language);
        } catch (ClassNotFoundException e) {
            available = false;
            log.warn("⚠️  Tesseract OCR 不可用: 缺少 tess4j 依赖");
            log.warn("💡 提示: 添加 Maven 依赖: net.sourceforge.tess4j:tess4j:5.9.0");
        }
    }

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        if (!available) {
            return String.format("[图片: %s - OCR不可用]", imageName);
        }

        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();

            // 配置 tessdata 路径
            if (tessdataPath != null && !tessdataPath.isEmpty()) {
                tesseract.setDatapath(tessdataPath);
            } else {
                // 尝试使用环境变量
                String envPath = System.getenv("TESSDATA_PREFIX");
                if (envPath != null && !envPath.isEmpty()) {
                    tesseract.setDatapath(envPath);
                }
            }

            // 设置语言
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            // 读取图片并进行OCR
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imageStream);
            if (image == null) {
                log.warn("无法读取图片: {}", imageName);
                return String.format("[图片: %s - 无法读取图片数据]", imageName);
            }

            String text = tesseract.doOCR(image);

            if (text == null || text.trim().isEmpty()) {
                log.debug("OCR未识别到文字 [{}]", imageName);
                return String.format("[图片: %s - 未识别到文字]", imageName);
            }

            // 清理文本
            text = text.trim();

            log.info("✅ OCR提取文字 [{}]: {} 字符", imageName, text.length());
            return String.format("\n=== 图片: %s ===\n%s\n=== /图片 ===\n", imageName, text);

        } catch (net.sourceforge.tess4j.TesseractException e) {
            log.error("Tesseract OCR处理失败: {}", imageName, e);
            return String.format("[图片: %s - OCR识别失败: %s]", imageName, e.getMessage());
        } catch (Exception e) {
            log.error("OCR处理失败: {}", imageName, e);
            return String.format("[图片: %s - OCR处理失败: %s]", imageName, e.getMessage());
        }
    }

    @Override
    public String extractContent(File imageFile) {
        if (!available) {
            return String.format("[图片: %s - OCR不可用]", imageFile.getName());
        }

        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();

            // 配置 tessdata 路径
            if (tessdataPath != null && !tessdataPath.isEmpty()) {
                tesseract.setDatapath(tessdataPath);
            } else {
                // 尝试使用环境变量
                String envPath = System.getenv("TESSDATA_PREFIX");
                if (envPath != null && !envPath.isEmpty()) {
                    tesseract.setDatapath(envPath);
                }
            }

            // 设置语言
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            String text = tesseract.doOCR(imageFile);

            if (text == null || text.trim().isEmpty()) {
                log.debug("OCR未识别到文字 [{}]", imageFile.getName());
                return String.format("[图片: %s - 未识别到文字]", imageFile.getName());
            }

            // 清理文本
            text = text.trim();

            log.info("✅ OCR提取文字 [{}]: {} 字符", imageFile.getName(), text.length());
            return String.format("\n=== 图片: %s ===\n%s\n=== /图片 ===\n", imageFile.getName(), text);

        } catch (net.sourceforge.tess4j.TesseractException e) {
            log.error("Tesseract OCR处理失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - OCR识别失败: %s]", imageFile.getName(), e.getMessage());
        } catch (Exception e) {
            log.error("OCR处理失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - OCR处理失败: %s]", imageFile.getName(), e.getMessage());
        }
    }

    @Override
    public String getStrategyName() {
        return "Tesseract OCR";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}

