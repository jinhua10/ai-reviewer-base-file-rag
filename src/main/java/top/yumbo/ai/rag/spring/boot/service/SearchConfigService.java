package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

/**
 * 搜索配置服务 - 支持动态修改检索参数
 *
 * @author AI Reviewer Team
 * @since 2025-11-29
 */
@Slf4j
@Service
public class SearchConfigService {

    private final KnowledgeQAProperties properties;

    // 运行时配置覆盖（优先级高于yml配置）
    private RuntimeConfig runtimeConfig = new RuntimeConfig();

    public SearchConfigService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取Lucene检索的top-k（优先使用运行时配置）
     */
    public int getLuceneTopK() {
        return runtimeConfig.luceneTopK != null
            ? runtimeConfig.luceneTopK
            : properties.getVectorSearch().getLuceneTopK();
    }

    /**
     * 获取向量检索的top-k（优先使用运行时配置）
     */
    public int getVectorTopK() {
        return runtimeConfig.vectorTopK != null
            ? runtimeConfig.vectorTopK
            : properties.getVectorSearch().getVectorTopK();
    }

    /**
     * 获取混合检索的top-k（优先使用运行时配置）
     */
    public int getHybridTopK() {
        return runtimeConfig.hybridTopK != null
            ? runtimeConfig.hybridTopK
            : properties.getVectorSearch().getHybridTopK();
    }

    /**
     * 获取每次问答引用的文档数（优先使用运行时配置）
     */
    public int getDocumentsPerQuery() {
        return runtimeConfig.documentsPerQuery != null
            ? runtimeConfig.documentsPerQuery
            : properties.getVectorSearch().getDocumentsPerQuery();
    }

    /**
     * 获取最小评分阈值（优先使用运行时配置）
     */
    public float getMinScoreThreshold() {
        return runtimeConfig.minScoreThreshold != null
            ? runtimeConfig.minScoreThreshold
            : properties.getVectorSearch().getMinScoreThreshold();
    }

    /**
     * 更新Lucene检索的top-k
     */
    public void setLuceneTopK(int luceneTopK) {
        if (luceneTopK <= 0) {
            throw new IllegalArgumentException("luceneTopK must be positive");
        }
        runtimeConfig.luceneTopK = luceneTopK;
        log.info("✅ 动态配置已更新: luceneTopK = {}", luceneTopK);
    }

    /**
     * 更新向量检索的top-k
     */
    public void setVectorTopK(int vectorTopK) {
        if (vectorTopK <= 0) {
            throw new IllegalArgumentException("vectorTopK must be positive");
        }
        runtimeConfig.vectorTopK = vectorTopK;
        log.info("✅ 动态配置已更新: vectorTopK = {}", vectorTopK);
    }

    /**
     * 更新混合检索的top-k
     */
    public void setHybridTopK(int hybridTopK) {
        if (hybridTopK <= 0) {
            throw new IllegalArgumentException("hybridTopK must be positive");
        }
        runtimeConfig.hybridTopK = hybridTopK;
        log.info("✅ 动态配置已更新: hybridTopK = {}", hybridTopK);
    }

    /**
     * 更新每次问答引用的文档数
     */
    public void setDocumentsPerQuery(int documentsPerQuery) {
        if (documentsPerQuery <= 0) {
            throw new IllegalArgumentException("documentsPerQuery must be positive");
        }
        runtimeConfig.documentsPerQuery = documentsPerQuery;
        log.info("✅ 动态配置已更新: documentsPerQuery = {}", documentsPerQuery);
    }

    /**
     * 更新最小评分阈值
     */
    public void setMinScoreThreshold(float minScoreThreshold) {
        if (minScoreThreshold < 0 || minScoreThreshold > 1) {
            throw new IllegalArgumentException("minScoreThreshold must be between 0 and 1");
        }
        runtimeConfig.minScoreThreshold = minScoreThreshold;
        log.info("✅ 动态配置已更新: minScoreThreshold = {}", minScoreThreshold);
    }

    /**
     * 批量更新配置
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
        log.info("✅ 批量配置更新完成");
    }

    /**
     * 重置为默认配置（yml配置）
     */
    public void resetToDefault() {
        runtimeConfig = new RuntimeConfig();
        log.info("✅ 配置已重置为默认值（yml配置）");
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

