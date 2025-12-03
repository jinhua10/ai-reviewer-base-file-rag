package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.impl.parser.image.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

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

    @Autowired(required = false)
    private LLMClient llmClient;

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
                // 优先使用 LLMClient（如果支持图片）
                if (llmClient != null && llmClient.supportsImageInput()) {
                    addLLMClientVisionStrategy(extractor);
                } else {
                    // 退回到独立的 VisionLLMStrategy
                    addVisionLlmStrategy(extractor, config);
                }
                break;

            case "llm-vision":
                // 强制使用 LLMClient
                if (llmClient != null && llmClient.supportsImageInput()) {
                    addLLMClientVisionStrategy(extractor);
                } else {
                    // 如果主 LLM 不支持图片，退回到 vision-llm 配置
                    log.warn("⚠️  主 LLM 不支持图片输入，退回到 vision-llm 配置");
                    addVisionLlmStrategy(extractor, config);
                }
                break;

            case "hybrid":
                // 混合模式：优先 LLM Vision / Vision LLM，其次 OCR
                if (llmClient != null && llmClient.supportsImageInput()) {
                    addLLMClientVisionStrategy(extractor);
                } else {
                    addVisionLlmStrategy(extractor, config);
                }
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
     * 添加 LLMClient Vision 策略（Add LLMClient Vision strategy）
     * 复用主 LLM 客户端进行图片处理
     */
    private void addLLMClientVisionStrategy(SmartImageExtractor extractor) {
        if (llmClient == null) {
            log.warn(LogMessageProvider.getMessage("log.imageproc.llm_vision_no_client"));
            return;
        }

        log.debug("检查 LLM 客户端是否支持图片：模型={}, 支持图片={}",
                  llmClient.getModelName(), llmClient.supportsImageInput());

        if (!llmClient.supportsImageInput()) {
            log.warn(LogMessageProvider.getMessage("log.imageproc.llm_vision_no_image_support", llmClient.getModelName()));
            return;
        }

        log.info(LogMessageProvider.getMessage("log.imageproc.add_llm_vision"));
        log.info(LogMessageProvider.getMessage("log.imageproc.llm_vision_model", llmClient.getModelName()));
        log.info(LogMessageProvider.getMessage("log.imageproc.llm_vision_client_type", llmClient.getClass().getSimpleName()));

        LLMClientVisionStrategy llmVisionStrategy = new LLMClientVisionStrategy(llmClient);
        extractor.addStrategy(llmVisionStrategy);

        if (llmVisionStrategy.isAvailable()) {
            log.info(LogMessageProvider.getMessage("log.imageproc.llm_vision_available"));
        } else {
            log.warn(LogMessageProvider.getMessage("log.imageproc.llm_vision_unavailable"));
        }
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
