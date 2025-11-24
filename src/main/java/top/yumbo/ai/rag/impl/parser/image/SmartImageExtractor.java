package top.yumbo.ai.rag.impl.parser.image;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能图片内容提取器
 *
 * 支持多种策略，按优先级自动选择可用的策略:
 * 1. Vision LLM（语义理解）
 * 2. Tesseract OCR（文字识别）
 * 3. Placeholder（占位符，兜底）
 *
 * 使用方法:
 * <pre>
 * // 默认配置（使用占位符）
 * SmartImageExtractor extractor = new SmartImageExtractor();
 *
 * // 启用 OCR
 * SmartImageExtractor extractor = SmartImageExtractor.withOCR();
 *
 * // 启用 Vision LLM
 * SmartImageExtractor extractor = SmartImageExtractor.withVisionLLM(apiKey);
 *
 * // 自定义策略
 * SmartImageExtractor extractor = new SmartImageExtractor();
 * extractor.addStrategy(new TesseractOCRStrategy());
 * extractor.addStrategy(new VisionLLMStrategy(apiKey, model, endpoint));
 * </pre>
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class SmartImageExtractor {

    private final List<ImageContentExtractorStrategy> strategies;
    private ImageContentExtractorStrategy activeStrategy;

    /**
     * 默认构造函数（只使用占位符）
     */
    public SmartImageExtractor() {
        this.strategies = new ArrayList<>();
        this.strategies.add(new PlaceholderImageStrategy());
        selectActiveStrategy();
    }

    /**
     * 添加策略
     */
    public SmartImageExtractor addStrategy(ImageContentExtractorStrategy strategy) {
        if (strategy != null) {
            strategies.add(0, strategy); // 添加到最前面（高优先级）
            selectActiveStrategy();
        }
        return this;
    }

    /**
     * 选择可用的策略
     */
    private void selectActiveStrategy() {
        for (ImageContentExtractorStrategy strategy : strategies) {
            if (strategy.isAvailable()) {
                activeStrategy = strategy;
                log.info("✅ 选择图片处理策略: {}", strategy.getStrategyName());
                return;
            }
        }

        // 兜底：使用占位符
        activeStrategy = new PlaceholderImageStrategy();
        log.warn("⚠️  没有可用的图片处理策略，使用占位符");
    }

    /**
     * 提取图片内容
     */
    public String extractContent(InputStream imageStream, String imageName) {
        if (activeStrategy == null) {
            return String.format("[图片: %s]", imageName);
        }

        try {
            return activeStrategy.extractContent(imageStream, imageName);
        } catch (Exception e) {
            log.error("图片内容提取失败: {}", imageName, e);
            return String.format("[图片: %s - 提取失败]", imageName);
        }
    }

    /**
     * 提取图片内容
     */
    public String extractContent(File imageFile) {
        if (activeStrategy == null) {
            return String.format("[图片: %s]", imageFile.getName());
        }

        try {
            return activeStrategy.extractContent(imageFile);
        } catch (Exception e) {
            log.error("图片内容提取失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - 提取失败]", imageFile.getName());
        }
    }

    /**
     * 获取当前激活的策略
     */
    public ImageContentExtractorStrategy getActiveStrategy() {
        return activeStrategy;
    }

    /**
     * 获取所有策略
     */
    public List<ImageContentExtractorStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }

    // ==================== 便捷工厂方法 ====================

    /**
     * 创建默认提取器（只使用占位符）
     */
    public static SmartImageExtractor createDefault() {
        return new SmartImageExtractor();
    }

    /**
     * 创建启用 OCR 的提取器
     */
    public static SmartImageExtractor withOCR() {
        SmartImageExtractor extractor = new SmartImageExtractor();
        extractor.addStrategy(new TesseractOCRStrategy());
        return extractor;
    }

    /**
     * 创建启用 OCR 的提取器（自定义配置）
     */
    public static SmartImageExtractor withOCR(String tessdataPath, String language) {
        SmartImageExtractor extractor = new SmartImageExtractor();
        extractor.addStrategy(new TesseractOCRStrategy(tessdataPath, language));
        return extractor;
    }

    /**
     * 创建启用 Vision LLM 的提取器
     */
    public static SmartImageExtractor withVisionLLM(String apiKey) {
        SmartImageExtractor extractor = new SmartImageExtractor();
        extractor.addStrategy(new VisionLLMStrategy(apiKey, null, null));
        return extractor;
    }

    /**
     * 创建启用 Vision LLM 的提取器（自定义配置）
     */
    public static SmartImageExtractor withVisionLLM(String apiKey, String model, String endpoint) {
        SmartImageExtractor extractor = new SmartImageExtractor();
        extractor.addStrategy(new VisionLLMStrategy(apiKey, model, endpoint));
        return extractor;
    }

    /**
     * 创建混合模式提取器（OCR + Vision LLM）
     */
    public static SmartImageExtractor withHybrid(String visionApiKey) {
        SmartImageExtractor extractor = new SmartImageExtractor();
        // 优先尝试 Vision LLM（语义理解）
        extractor.addStrategy(new VisionLLMStrategy(visionApiKey, null, null));
        // 其次尝试 OCR（文字识别）
        extractor.addStrategy(new TesseractOCRStrategy());
        return extractor;
    }

    /**
     * 从环境变量自动配置
     */
    public static SmartImageExtractor fromEnv() {
        SmartImageExtractor extractor = new SmartImageExtractor();

        // 尝试从环境变量加载 Vision LLM
        String visionApiKey = System.getenv("VISION_LLM_API_KEY");
        if (visionApiKey != null && !visionApiKey.isEmpty()) {
            extractor.addStrategy(VisionLLMStrategy.fromEnv());
        }

        // 尝试加载 OCR
        String enableOCR = System.getenv("ENABLE_OCR");
        if ("true".equalsIgnoreCase(enableOCR)) {
            String tessdataPath = System.getenv("TESSDATA_PREFIX");
            String language = System.getenv("OCR_LANGUAGE");
            extractor.addStrategy(new TesseractOCRStrategy(tessdataPath, language));
        }

        return extractor;
    }
}

