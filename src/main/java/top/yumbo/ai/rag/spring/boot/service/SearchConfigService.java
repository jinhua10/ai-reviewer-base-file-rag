package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

/**
 * 搜索配置服务 - 支持动态修改检索参数（Search config service - supports dynamic parameter modification）
 *
 * @author AI Reviewer Team
 * @since 2025-11-29
 */
@Slf4j
@Service
public class SearchConfigService {

    private final KnowledgeQAProperties properties;

    // 运行时配置覆盖（优先级高于yml配置）（Runtime config override）
    private RuntimeConfig runtimeConfig = new RuntimeConfig();

    public SearchConfigService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取Lucene检索的top-k（优先使用运行时配置）（Get Lucene top-k）
     */
    public int getLuceneTopK() {
        return runtimeConfig.luceneTopK != null
            ? runtimeConfig.luceneTopK
            : properties.getVectorSearch().getLuceneTopK();
    }

    /**
     * 获取向量检索的top-k（优先使用运行时配置）（Get vector top-k）
     */
    public int getVectorTopK() {
        return runtimeConfig.vectorTopK != null
            ? runtimeConfig.vectorTopK
            : properties.getVectorSearch().getVectorTopK();
    }

    /**
     * 获取混合检索的top-k（优先使用运行时配置）（Get hybrid top-k）
     */
    public int getHybridTopK() {
        return runtimeConfig.hybridTopK != null
            ? runtimeConfig.hybridTopK
            : properties.getVectorSearch().getHybridTopK();
    }

    /**
     * 获取每次问答引用的文档数（优先使用运行时配置）（Get documents per query）
     */
    public int getDocumentsPerQuery() {
        return runtimeConfig.documentsPerQuery != null
            ? runtimeConfig.documentsPerQuery
            : properties.getVectorSearch().getDocumentsPerQuery();
    }

    /**
     * 获取最小评分阈值（优先使用运行时配置）（Get min score threshold）
     */
    public float getMinScoreThreshold() {
        return runtimeConfig.minScoreThreshold != null
            ? runtimeConfig.minScoreThreshold
            : properties.getVectorSearch().getMinScoreThreshold();
    }

    /**
     * 更新Lucene检索的top-k（Update Lucene top-k）
     */
    public void setLuceneTopK(int luceneTopK) {
        if (luceneTopK <= 0) {
            throw new IllegalArgumentException("luceneTopK must be positive");
        }
        runtimeConfig.luceneTopK = luceneTopK;
        log.info(LogMessageProvider.getMessage("search_config.log.lucene_updated", luceneTopK));
    }

    /**
     * 更新向量检索的top-k（Update vector top-k）
     */
    public void setVectorTopK(int vectorTopK) {
        if (vectorTopK <= 0) {
            throw new IllegalArgumentException("vectorTopK must be positive");
        }
        runtimeConfig.vectorTopK = vectorTopK;
        log.info(LogMessageProvider.getMessage("search_config.log.vector_updated", vectorTopK));
    }

    /**
     * 更新混合检索的top-k（Update hybrid top-k）
     */
    public void setHybridTopK(int hybridTopK) {
        if (hybridTopK <= 0) {
            throw new IllegalArgumentException("hybridTopK must be positive");
        }
        runtimeConfig.hybridTopK = hybridTopK;
        log.info(LogMessageProvider.getMessage("search_config.log.hybrid_updated", hybridTopK));
    }

    /**
     * 更新每次问答引用的文档数（Update documents per query）
     */
    public void setDocumentsPerQuery(int documentsPerQuery) {
        if (documentsPerQuery <= 0) {
            throw new IllegalArgumentException("documentsPerQuery must be positive");
        }
        runtimeConfig.documentsPerQuery = documentsPerQuery;
        log.info(LogMessageProvider.getMessage("search_config.log.docs_per_query_updated", documentsPerQuery));
    }

    /**
     * 更新最小评分阈值（Update min score threshold）
     */
    public void setMinScoreThreshold(float minScoreThreshold) {
        if (minScoreThreshold < 0 || minScoreThreshold > 1) {
            throw new IllegalArgumentException("minScoreThreshold must be between 0 and 1");
        }
        runtimeConfig.minScoreThreshold = minScoreThreshold;
        log.info(LogMessageProvider.getMessage("search_config.log.min_score_updated", minScoreThreshold));
    }

    /**
     * 批量更新配置（Batch update config）
     */
    public void updateConfig(SearchConfigUpdate update) {
        if (update.getLuceneTopK() != null) {
            setLuceneTopK(update.getLuceneTopK());
        }
        if (update.getVectorTopK() != null) {
            setVectorTopK(update.getVectorTopK());
        }
        if (update.getHybridTopK() != null) {
            setHybridTopK(update.getHybridTopK());
        }
        if (update.getDocumentsPerQuery() != null) {
            setDocumentsPerQuery(update.getDocumentsPerQuery());
        }
        if (update.getMinScoreThreshold() != null) {
            setMinScoreThreshold(update.getMinScoreThreshold());
        }
        log.info(LogMessageProvider.getMessage("search_config.log.batch_update_complete"));
    }

    /**
     * 重置为默认配置（yml配置）（Reset to default config）
     */
    public void resetToDefault() {
        runtimeConfig = new RuntimeConfig();
        log.info(LogMessageProvider.getMessage("search_config.log.reset_to_default"));
    }

    /**
     * 获取当前配置信息
     */
    public SearchConfigInfo getCurrentConfig() {
        SearchConfigInfo info = new SearchConfigInfo();
        info.setLuceneTopK(getLuceneTopK());
        info.setVectorTopK(getVectorTopK());
        info.setHybridTopK(getHybridTopK());
        info.setDocumentsPerQuery(getDocumentsPerQuery());
        info.setMinScoreThreshold(getMinScoreThreshold());
        info.setUsingRuntimeConfig(runtimeConfig.hasOverrides());
        return info;
    }

    /**
     * 运行时配置（覆盖yml配置）
     */
    @Data
    private static class RuntimeConfig {
        private Integer luceneTopK;
        private Integer vectorTopK;
        private Integer hybridTopK;
        private Integer documentsPerQuery;
        private Float minScoreThreshold;

        public boolean hasOverrides() {
            return luceneTopK != null || vectorTopK != null || hybridTopK != null
                || documentsPerQuery != null || minScoreThreshold != null;
        }
    }

    /**
     * 配置更新请求
     */
    @Data
    public static class SearchConfigUpdate {
        private Integer luceneTopK;
        private Integer vectorTopK;
        private Integer hybridTopK;
        private Integer documentsPerQuery;
        private Float minScoreThreshold;
    }

    /**
     * 配置信息响应
     */
    @Data
    public static class SearchConfigInfo {
        private int luceneTopK;
        private int vectorTopK;
        private int hybridTopK;
        private int documentsPerQuery;
        private float minScoreThreshold;
        private boolean usingRuntimeConfig;
    }
}

