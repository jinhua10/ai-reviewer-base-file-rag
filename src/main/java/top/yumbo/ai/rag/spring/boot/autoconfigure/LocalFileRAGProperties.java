package top.yumbo.ai.rag.spring.boot.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LocalFileRAG 配置属性
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Data
@ConfigurationProperties(prefix = "local-file-rag")
public class LocalFileRAGProperties {

    /**
     * 是否启用 LocalFileRAG
     */
    private boolean enabled = true;

    /**
     * 数据存储路径
     */
    private String storagePath = "./data/rag";

    /**
     * 是否启用缓存
     */
    private boolean enableCache = true;

    /**
     * 是否启用压缩
     */
    private boolean enableCompression = true;

    /**
     * 是否自动创建问答服务
     */
    private boolean autoQaService = false;

    /**
     * 文档源路径（可选，用于自动索引）
     */
    private String documentPath;

    /**
     * 是否在启动时自动索引文档
     */
    private boolean autoIndex = false;

    /**
     * 搜索配置
     */
    private SearchConfig search = new SearchConfig();

    @Data
    public static class SearchConfig {
        /**
         * 默认返回结果数
         */
        private int defaultLimit = 10;

        /**
         * 最大返回结果数
         */
        private int maxLimit = 100;
    }
}

