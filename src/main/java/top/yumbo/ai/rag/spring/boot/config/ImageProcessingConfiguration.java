package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.impl.parser.image.*;

/**
 * 图片处理配置
 * <p>
 * 根据配置创建相应的图片提取器策略
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
     * 创建智能图片提取器
     */
    @Bean
    @ConditionalOnMissingBean
    public SmartImageExtractor smartImageExtractor() {
        log.info("🖼️  初始化图片处理功能...");

        KnowledgeQAProperties.ImageProcessingConfig config = properties.getImageProcessing();
        String strategy = config.getStrategy();

        log.info("   配置策略: {}", strategy);

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
                // 使用默认占位符策略
                log.info("   使用占位符策略（默认）");
                break;
        }

        // 显示激活的策略
        ImageContentExtractorStrategy activeStrategy = extractor.getActiveStrategy();
        log.info("✅ 图片处理策略已激活: {}", activeStrategy.getStrategyName());

        return extractor;
    }

    /**
     * 添加 OCR 策略
     */
    private void addOcrStrategy(SmartImageExtractor extractor, KnowledgeQAProperties.ImageProcessingConfig config) {
        if (config.isEnableOcr()) {
            KnowledgeQAProperties.OcrConfig ocrConfig = config.getOcr();
            String tessdataPath = resolveEnvVariable(ocrConfig.getTessdataPath());
            String language = ocrConfig.getLanguage();

            log.info("   添加 OCR 策略:");
            log.info("      - Tessdata路径: {}", tessdataPath != null ? tessdataPath : "系统默认");
            log.info("      - 识别语言: {}", language);

            TesseractOCRStrategy ocrStrategy = new TesseractOCRStrategy(tessdataPath, language);
            extractor.addStrategy(ocrStrategy);

            if (ocrStrategy.isAvailable()) {
                log.info("   ✅ OCR 策略可用");
            } else {
                log.warn("   ⚠️  OCR 策略不可用: 缺少 tess4j 依赖");
                log.warn("   💡 提示: 添加依赖 net.sourceforge.tess4j:tess4j:5.9.0");
            }
        }
    }

    /**
     * 添加 Vision LLM 策略
     */
    private void addVisionLlmStrategy(SmartImageExtractor extractor, KnowledgeQAProperties.ImageProcessingConfig config) {
        KnowledgeQAProperties.VisionLlmConfig visionConfig = config.getVisionLlm();

        if (visionConfig.isEnabled()) {
            String apiKey = resolveEnvVariable(visionConfig.getApiKey());
            String model = visionConfig.getModel();
            String endpoint = resolveEnvVariable(visionConfig.getEndpoint());

            if (apiKey != null && !apiKey.isEmpty()) {
                log.info("   添加 Vision LLM 策略:");
                log.info("      - 模型: {}", model);
                if (endpoint != null && !endpoint.isEmpty()) {
                    log.info("      - 端点: {}", endpoint);
                }

                VisionLLMStrategy visionStrategy = new VisionLLMStrategy(apiKey, model, endpoint);
                extractor.addStrategy(visionStrategy);

                if (visionStrategy.isAvailable()) {
                    log.info("   ✅ Vision LLM 策略可用");
                } else {
                    log.warn("   ⚠️  Vision LLM 策略不可用");
                }
            } else {
                log.warn("   ⚠️  Vision LLM 已启用但未配置 API Key");
                log.warn("   💡 提示: 设置环境变量 VISION_LLM_API_KEY 或配置 knowledge.qa.image-processing.vision-llm.api-key");
            }
        }
    }

    /**
     * 解析环境变量占位符
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

