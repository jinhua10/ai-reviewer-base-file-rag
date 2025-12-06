package top.yumbo.ai.rag.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * LocalFileRAG 自动配置（LocalFileRAG Auto-configuration）
 *
 * 使用方式：（Usage:)
 * 1. 在 application.yml 中配置:（Configure in application.yml:)
 *    local-file-rag:
 *      enabled: true
 *      storage-path: ./data/rag
 *
 * 2. 直接注入使用:（Inject and use directly:)
 *    {@code @Autowired}
 *    private LocalFileRAG rag;
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LocalFileRAGProperties.class)
@ConditionalOnProperty(prefix = "local-file-rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocalFileRAGAutoConfiguration {

    private final LocalFileRAGProperties properties;

    public LocalFileRAGAutoConfiguration(LocalFileRAGProperties properties) {
        this.properties = properties;
    }

    /**
     * 自动配置 LocalFileRAG Bean（Auto-configure LocalFileRAG Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalFileRAG localFileRAG() {
        log.info(I18N.get("log.rag.init"));
        log.info(I18N.get("log.rag.storage", properties.getStoragePath()));
        log.info(I18N.get("log.rag.enable_cache", properties.isEnableCache()));
        log.info(I18N.get("log.rag.enable_compression", properties.isEnableCompression()));

        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath(properties.getStoragePath())
            .enableCache(properties.isEnableCache())
            .enableCompression(properties.isEnableCompression())
            .build();

        log.info(I18N.get("log.rag.init_done"));

        return rag;
    }

    /**
     * 自动配置 RAG 服务（Auto-configure RAG service）
     */
    @Bean
    @ConditionalOnMissingBean
    public SimpleRAGService simpleRAGService(LocalFileRAG rag) {
        log.info(I18N.get("log.rag.simple_init"));

        SimpleRAGService service = new SimpleRAGService(rag, properties);

        log.info(I18N.get("log.rag.simple_init_done"));

        return service;
    }
}
