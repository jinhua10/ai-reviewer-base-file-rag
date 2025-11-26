package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.image.ImageStorageService;

/**
 * 存储配置类
 * 配置文档块和图片的存储服务
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
@Configuration
public class StorageConfiguration {

    /**
     * 文档块存储服务
     */
    @Bean
    public ChunkStorageService chunkStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        log.info("Initializing ChunkStorageService with path: {}", storagePath);
        return new ChunkStorageService(storagePath);
    }

    /**
     * 图片存储服务
     */
    @Bean
    public ImageStorageService imageStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        log.info("Initializing ImageStorageService with path: {}", storagePath);
        return new ImageStorageService(storagePath);
    }
}

