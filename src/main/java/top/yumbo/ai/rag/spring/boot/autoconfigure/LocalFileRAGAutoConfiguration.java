package top.yumbo.ai.rag.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.service.LocalFileRAG;

/**
 * LocalFileRAG 自动配置
 *
 * 使用方式：
 * 1. 在 application.yml 中配置:
 *    local-file-rag:
 *      enabled: true
 *      storage-path: ./data/rag
 *
 * 2. 直接注入使用:
 *    @Autowired
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
     * 自动配置 LocalFileRAG Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LocalFileRAG localFileRAG() {
        log.info("🚀 初始化 LocalFileRAG...");
        log.info("   - 存储路径: {}", properties.getStoragePath());
        log.info("   - 启用缓存: {}", properties.isEnableCache());
        log.info("   - 启用压缩: {}", properties.isEnableCompression());

        LocalFileRAG rag = LocalFileRAG.builder()
            .storagePath(properties.getStoragePath())
            .enableCache(properties.isEnableCache())
            .enableCompression(properties.isEnableCompression())
            .build();

        log.info("✅ LocalFileRAG 初始化完成");

        return rag;
    }

    /**
     * 自动配置 RAG 服务
     */
    @Bean
    @ConditionalOnMissingBean
    public SimpleRAGService simpleRAGService(LocalFileRAG rag) {
        log.info("🤖 初始化简易 RAG 问答服务...");

        SimpleRAGService service = new SimpleRAGService(rag, properties);

        log.info("✅ RAG 问答服务初始化完成");

        return service;
    }
}

