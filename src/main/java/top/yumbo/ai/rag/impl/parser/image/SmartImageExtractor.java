package top.yumbo.ai.rag.impl.parser.image;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能图片内容提取器
 *
 * 支持多种策略，按优先级自动选择可用的策略:
 * 1. Vision LLM（语义理解，推荐）- 使用 GPT-4o 等多模态 LLM
 * 2. Placeholder（占位符，兜底）
 *
 * 使用方法:
 * <pre>
 * // 默认配置（使用占位符）
 * SmartImageExtractor extractor = new SmartImageExtractor();
 *
 * // 启用 OCR
 * // 启用 Vision LLM（推荐）
 * SmartImageExtractor extractor = SmartImageExtractor.withVisionLLM(apiKey);
 *
 * // 自定义策略
 * SmartImageExtractor extractor = new SmartImageExtractor();
 * extractor.addStrategy(new VisionLLMStrategy(apiKey, model, endpoint));
 * </pre>
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class SmartImageExtractor {

    private final List<ImageContentExtractorStrategy> strategies;
    /**
     * -- GETTER --
     *  获取当前激活的策略
     */
    @Getter
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
                log.info(I18N.get("log.imageproc.strategy_selected", strategy.getStrategyName()));
                return;
            }
        }

        // 兜底：使用占位符
        activeStrategy = new PlaceholderImageStrategy();
        log.warn(I18N.get("log.imageproc.strategy_none"));
    }

    /**
     * 提取图片内容
     */
    public String extractContent(InputStream imageStream, String imageName) {
        if (activeStrategy == null) {
            return I18N.get("log.imageproc.image_placeholder", imageName);
        }

        try {
            return activeStrategy.extractContent(imageStream, imageName);
        } catch (Exception e) {
            log.error(I18N.get("log.imageproc.extract_failed", imageName), e);
            return I18N.get("log.imageproc.extract_error", imageName);
        }
    }

    /**
     * 提取图片内容
     */
    public String extractContent(File imageFile) {
        if (activeStrategy == null) {
            return I18N.get("log.imageproc.image_placeholder", imageFile.getName());
        }

        try {
            return activeStrategy.extractContent(imageFile);
        } catch (Exception e) {
            log.error(I18N.get("log.imageproc.extract_failed", imageFile.getName()), e);
            return I18N.get("log.imageproc.extract_error", imageFile.getName());
        }
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
        // 使用 Vision LLM（语义理解）
        extractor.addStrategy(new VisionLLMStrategy(visionApiKey, null, null));
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


        return extractor;
    }
}

