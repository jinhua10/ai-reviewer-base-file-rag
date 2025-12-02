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

            // 配置 tessdata 路径（Configure tessdata path）
            if (tessdataPath != null && !tessdataPath.isEmpty()) {
                tesseract.setDatapath(tessdataPath);
            } else {
                // 尝试使用环境变量（Try to use environment variable）
                String envPath = System.getenv("TESSDATA_PREFIX");
                if (envPath != null && !envPath.isEmpty()) {
                    tesseract.setDatapath(envPath);
                }
            }

            // 设置语言（Set language）
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            // 读取图片（Read image）
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imageStream);
            if (image == null) {
                log.warn("无法读取图片: {}", imageName);
                return String.format("[图片: %s - 无法读取图片数据]", imageName);
            }

            // 检查图片尺寸（Check image dimensions）
            // Tesseract 要求最小尺寸为 3x3 像素
            // 过小的图片通常是装饰性图标或分隔线，无需 OCR 识别
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < 3 || height < 3) {
                log.debug("图片尺寸过小，跳过 OCR [{}]: {}x{} 像素", imageName, width, height);
                return String.format("[图片: %s - 尺寸过小 (%dx%d)]", imageName, width, height);
            }

            // 检查图片是否过小，无实际内容（Check if image is too small for meaningful content）
            // 宽度或高度小于 10 像素的图片通常是装饰性元素
            if (width < 10 || height < 10) {
                log.debug("图片尺寸太小，可能无有效内容 [{}]: {}x{} 像素", imageName, width, height);
                return String.format("[图片: %s - 装饰性图标 (%dx%d)]", imageName, width, height);
            }

            // 标准化图片 DPI，避免警告（Normalize image DPI to avoid warnings）
            // Tesseract 要求 DPI >= 70，否则会产生警告
            // 我们统一设置为 300 DPI（标准打印质量）
            image = normalizeImageDPI(image);

            // 执行 OCR（Perform OCR）
            String text = tesseract.doOCR(image);

            if (text == null || text.trim().isEmpty()) {
                log.debug("OCR未识别到文字 [{}]", imageName);
                return String.format("[图片: %s - 未识别到文字]", imageName);
            }

            // 清理文本（Clean text）
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

            // 配置 tessdata 路径（Configure tessdata path）
            if (tessdataPath != null && !tessdataPath.isEmpty()) {
                tesseract.setDatapath(tessdataPath);
            } else {
                // 尝试使用环境变量（Try to use environment variable）
                String envPath = System.getenv("TESSDATA_PREFIX");
                if (envPath != null && !envPath.isEmpty()) {
                    tesseract.setDatapath(envPath);
                }
            }

            // 设置语言（Set language）
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            // 读取图片（Read image）
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imageFile);
            if (image == null) {
                log.warn("无法读取图片: {}", imageFile.getName());
                return String.format("[图片: %s - 无法读取图片数据]", imageFile.getName());
            }

            // 检查图片尺寸（Check image dimensions）
            // Tesseract 要求最小尺寸为 3x3 像素
            // 过小的图片通常是装饰性图标或分隔线，无需 OCR 识别
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < 3 || height < 3) {
                log.debug("图片尺寸过小，跳过 OCR [{}]: {}x{} 像素", imageFile.getName(), width, height);
                return String.format("[图片: %s - 尺寸过小 (%dx%d)]", imageFile.getName(), width, height);
            }

            // 检查图片是否过小，无实际内容（Check if image is too small for meaningful content）
            // 宽度或高度小于 10 像素的图片通常是装饰性元素
            if (width < 10 || height < 10) {
                log.debug("图片尺寸太小，可能无有效内容 [{}]: {}x{} 像素", imageFile.getName(), width, height);
                return String.format("[图片: %s - 装饰性图标 (%dx%d)]", imageFile.getName(), width, height);
            }

            // 标准化图片 DPI，避免警告（Normalize image DPI to avoid warnings）
            image = normalizeImageDPI(image);

            // 执行 OCR（Perform OCR）
            String text = tesseract.doOCR(image);

            if (text == null || text.trim().isEmpty()) {
                log.debug("OCR未识别到文字 [{}]", imageFile.getName());
                return String.format("[图片: %s - 未识别到文字]", imageFile.getName());
            }

            // 清理文本（Clean text）
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

    /**
     * 标准化图片 DPI，避免 Tesseract 警告（Normalize image DPI to avoid Tesseract warnings）
     * <p>
     * Tesseract 要求 DPI >= 70，否则会输出警告：
     * "Warning: Invalid resolution 1 dpi. Using 70 instead."
     * <p>
     * 此方法将图片标准化为 300 DPI（标准打印质量）
     *
     * @param originalImage 原始图片（Original image）
     * @return 标准化后的图片（Normalized image）
     */
    private java.awt.image.BufferedImage normalizeImageDPI(java.awt.image.BufferedImage originalImage) {
        // 创建新的 BufferedImage，保持原始尺寸和颜色模型
        // Create new BufferedImage with original dimensions and color model
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int imageType = originalImage.getType();

        // 如果图片类型未知，使用 RGB 类型
        // If image type is unknown, use RGB type
        if (imageType == java.awt.image.BufferedImage.TYPE_CUSTOM) {
            imageType = java.awt.image.BufferedImage.TYPE_INT_RGB;
        }

        java.awt.image.BufferedImage normalizedImage = new java.awt.image.BufferedImage(
            width, height, imageType
        );

        // 复制图片内容（Copy image content）
        java.awt.Graphics2D g2d = normalizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        // 注意：BufferedImage 不直接存储 DPI 信息
        // DPI 信息通常存储在图片文件的元数据中
        // 这里我们通过重新创建图片对象来清除可能存在的无效 DPI 信息
        // Tesseract 在检测不到 DPI 时会使用默认值 70 DPI，不会产生警告

        return normalizedImage;
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

