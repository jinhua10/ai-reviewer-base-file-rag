package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.image.DocumentImageExtractionService;
import top.yumbo.ai.rag.image.ImageStorageService;
import top.yumbo.ai.rag.image.analyzer.AIImageAnalyzer;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

/**
 * 存储配置类（Storage configuration class）
 * 配置文档块和图片的存储服务（Configure storage services for document chunks and images）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
@Configuration
public class StorageConfiguration {

    /**
     * 文档块存储服务（Document chunk storage service）
     */
    @Bean
    public ChunkStorageService chunkStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        log.info(LogMessageProvider.getMessage("log.storage.chunk_storage_init", storagePath));
        return new ChunkStorageService(storagePath);
    }

    /**
     * 图片存储服务（Image storage service）
     */
    @Bean
    public ImageStorageService imageStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        log.info(LogMessageProvider.getMessage("log.storage.image_storage_init", storagePath));
        return new ImageStorageService(storagePath);
    }

    /**
     * AI 图片分析器（AI image analyzer）
     */
    @Bean
    public AIImageAnalyzer aiImageAnalyzer(KnowledgeQAProperties properties, LLMClient llmClient) {
        boolean enabled = properties.getLlm().getChunking().getAiChunking().isEnabled();
        String model = properties.getLlm().getChunking().getAiChunking().getModel();

        log.info(LogMessageProvider.getMessage("log.storage.ai_image_analyzer_init", enabled, model));
        return new AIImageAnalyzer(llmClient, enabled, model);
    }

    /**
     * 文档图片提取服务（Document image extraction service）
     */
    @Bean
    public DocumentImageExtractionService documentImageExtractionService(
            ImageStorageService imageStorageService,
            AIImageAnalyzer aiImageAnalyzer,
            KnowledgeQAProperties properties) {

        boolean aiAnalysisEnabled = properties.getLlm().getChunking().getAiChunking().isEnabled();

        log.info(LogMessageProvider.getMessage("log.storage.document_image_extraction_init", aiAnalysisEnabled));
        return new DocumentImageExtractionService(
                imageStorageService,
                aiImageAnalyzer,
                aiAnalysisEnabled
        );
    }
}
