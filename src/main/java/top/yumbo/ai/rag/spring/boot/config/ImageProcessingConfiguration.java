package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.impl.parser.image.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * 图片处理配置（Image processing configuration）
 * <p>
 * 根据配置创建相应的图片提取器策略（Creates corresponding image extractor strategies based on configuration）
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Configuration
public class ImageProcessingConfiguration {

    private final KnowledgeQAProperties properties;

    public ImageProcessingConfiguration(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建智能图片提取器（Create smart image extractor）
     */
    @Bean
    @ConditionalOnMissingBean
    public SmartImageExtractor smartImageExtractor() {
        log.info(LogMessageProvider.getMessage("log.imageproc.init"));

        KnowledgeQAProperties.ImageProcessingConfig config = properties.getImageProcessing();
        String strategy = config.getStrategy();

        log.info(LogMessageProvider.getMessage("log.imageproc.strategy", strategy));

        SmartImageExtractor extractor = new SmartImageExtractor();

        // 根据配置添加策略
        switch (strategy.toLowerCase()) {
            case "ocr":
                addOcrStrategy(extractor, config);
                break;

            case "vision-llm":
                addVisionLlmStrategy(extractor, config);
                break;

            case "hybrid":
                // 混合模式：优先 Vision LLM，其次 OCR
                addVisionLlmStrategy(extractor, config);
                addOcrStrategy(extractor, config);
                break;

            case "placeholder":
            default:
                log.info(LogMessageProvider.getMessage("log.imageproc.placeholder"));
                break;
        }

        // 显示激活的策略
        ImageContentExtractorStrategy activeStrategy = extractor.getActiveStrategy();
        log.info(LogMessageProvider.getMessage("log.imageproc.activated", activeStrategy.getStrategyName()));

        return extractor;
    }

    /**
     * 添加 OCR 策略（Add OCR strategy）
     */
    private void addOcrStrategy(SmartImageExtractor extractor, KnowledgeQAProperties.ImageProcessingConfig config) {
        if (config.isEnableOcr()) {
            KnowledgeQAProperties.OcrConfig ocrConfig = config.getOcr();
            String tessdataPath = resolveEnvVariable(ocrConfig.getTessdataPath());
            String language = ocrConfig.getLanguage();

            log.info(LogMessageProvider.getMessage("log.imageproc.add_ocr"));
            log.info(LogMessageProvider.getMessage("log.imageproc.tessdata", tessdataPath != null ? tessdataPath : "系统默认"));
            log.info(LogMessageProvider.getMessage("log.imageproc.language", language));

            TesseractOCRStrategy ocrStrategy = new TesseractOCRStrategy(tessdataPath, language);
            extractor.addStrategy(ocrStrategy);

            if (ocrStrategy.isAvailable()) {
                log.info(LogMessageProvider.getMessage("log.imageproc.ocr_available"));
            } else {
                log.warn(LogMessageProvider.getMessage("log.imageproc.ocr_unavailable"));
                log.warn(LogMessageProvider.getMessage("log.imageproc.ocr_hint"));
            }
        }
    }

    /**
     * 添加 Vision LLM 策略（Add Vision LLM strategy）
     */
    private void addVisionLlmStrategy(SmartImageExtractor extractor, KnowledgeQAProperties.ImageProcessingConfig config) {
        KnowledgeQAProperties.VisionLlmConfig visionConfig = config.getVisionLlm();

        if (visionConfig.isEnabled()) {
            String apiKey = resolveEnvVariable(visionConfig.getApiKey());
            String model = visionConfig.getModel();
            String endpoint = resolveEnvVariable(visionConfig.getEndpoint());

            if (apiKey != null && !apiKey.isEmpty()) {
                log.info(LogMessageProvider.getMessage("log.imageproc.add_vision"));
                log.info(LogMessageProvider.getMessage("log.imageproc.vision_model", model));
                if (endpoint != null && !endpoint.isEmpty()) {
                    log.info(LogMessageProvider.getMessage("log.imageproc.vision_endpoint", endpoint));
                }

                VisionLLMStrategy visionStrategy = new VisionLLMStrategy(apiKey, model, endpoint);
                extractor.addStrategy(visionStrategy);

                if (visionStrategy.isAvailable()) {
                    log.info(LogMessageProvider.getMessage("log.imageproc.vision_available"));
                } else {
                    log.warn(LogMessageProvider.getMessage("log.imageproc.vision_unavailable"));
                }
            } else {
                log.warn(LogMessageProvider.getMessage("log.imageproc.vision_no_apikey"));
                log.warn(LogMessageProvider.getMessage("log.imageproc.vision_apikey_hint"));
            }
        }
    }

    /**
     * 解析环境变量占位符（Resolve environment variable placeholders）
     */
    private String resolveEnvVariable(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 处理 ${VAR:default} 格式
        if (value.startsWith("${") && value.endsWith("}")) {
            String content = value.substring(2, value.length() - 1);
            String[] parts = content.split(":", 2);
            String envVar = parts[0];
            String defaultValue = parts.length > 1 ? parts[1] : "";

            String envValue = System.getenv(envVar);
            return envValue != null && !envValue.isEmpty() ? envValue : defaultValue;
        }

        return value;
    }
}
