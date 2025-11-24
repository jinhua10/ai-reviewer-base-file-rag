package top.yumbo.ai.rag;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.core.StorageEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.model.SearchResult;

import java.io.Closeable;
import java.util.List;

/**
 * Local File RAG - 本地文件存储RAG替代框架
 * 主入口类，提供统一的API接口
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class LocalFileRAG implements Closeable {

    private final RAGConfiguration configuration;
    private final StorageEngine storageEngine;
    private final IndexEngine indexEngine;
    private final CacheEngine cacheEngine;

    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    private LocalFileRAG(RAGConfiguration configuration,
                         StorageEngine storageEngine,
                         IndexEngine indexEngine,
                         CacheEngine cacheEngine) {
        this.configuration = configuration;
        this.storageEngine = storageEngine;
        this.indexEngine = indexEngine;
        this.cacheEngine = cacheEngine;

        log.info("Local File RAG initialized with configuration: {}", configuration);
    }

    /**
     * 索引单个文档
     *
     * @param document 文档对象
     * @return 文档ID
     */
    public String index(Document document) {
        // 1. 存储文档
        String docId = storageEngine.store(document);
        document.setId(docId);

        // 2. 构建索引
        indexEngine.indexDocument(document);

        // 3. 缓存文档
        if (configuration.getCache().isEnabled()) {
            cacheEngine.putDocument(docId, document);
        }

        log.debug("Document indexed successfully: {}", docId);
        return docId;
    }

    /**
     * 批量索引文档
     *
     * @param documents 文档列表
     * @return 成功索引的文档数量
     */
    public int indexBatch(List<Document> documents) {
        // 1. 批量存储
        int count = storageEngine.storeBatch(documents);

        // 2. 批量索引
        indexEngine.indexBatch(documents);

        log.info("Batch indexed {} documents", count);
        return count;
    }

    /**
     * 搜索文档
     *
     * @param query 查询对象
     * @return 搜索结果
     */
    public SearchResult search(Query query) {
        long startTime = System.currentTimeMillis();

        // 1. 尝试从缓存获取
        String queryKey = generateQueryKey(query);
        if (configuration.getCache().isEnabled()) {
            SearchResult cached = cacheEngine.getQueryResult(queryKey);
            if (cached != null) {
                log.debug("Query result retrieved from cache: {}", queryKey);
                return cached;
            }
        }

        // 2. 执行搜索（索引中只有元数据）
        SearchResult result = indexEngine.search(query);

        // 3. 为每个文档加载完整内容
        // 重要：Lucene 索引中只存储了文档元数据，content 需要从 StorageEngine 加载
        for (ScoredDocument scoredDoc : result.getScoredDocuments()) {
            Document doc = scoredDoc.getDocument();

            // 如果文档没有 content（从索引返回的通常没有），从存储加载
            if (doc.getContent() == null || doc.getContent().isEmpty()) {
                Document fullDoc = storageEngine.retrieve(doc.getId());
                if (fullDoc != null) {
                    // 替换为包含完整内容的文档
                    scoredDoc.setDocument(fullDoc);

                    log.trace("Loaded content for document: {}, length: {}",
                        doc.getId(), fullDoc.getContent() != null ? fullDoc.getContent().length() : 0);
                } else {
                    log.warn("Failed to load content for document: {}", doc.getId());
                }
            }
        }

        result.setQueryTimeMs(System.currentTimeMillis() - startTime);

        // 4. 缓存结果
        if (configuration.getCache().isEnabled()) {
            cacheEngine.putQueryResult(queryKey, result);
        }

        log.debug("Search completed in {}ms, found {} results",
                result.getQueryTimeMs(), result.getTotalHits());
        return result;
    }

    /**
     * 获取文档
     *
     * @param docId 文档ID
     * @return 文档对象
     */
    public Document getDocument(String docId) {
        // 1. 尝试从缓存获取
        if (configuration.getCache().isEnabled()) {
            Document cached = cacheEngine.getDocument(docId);
            if (cached != null) {
                return cached;
            }
        }

        // 2. 从存储获取
        Document document = storageEngine.retrieve(docId);

        // 3. 缓存文档
        if (document != null && configuration.getCache().isEnabled()) {
            cacheEngine.putDocument(docId, document);
        }

        return document;
    }

    /**
     * 更新文档
     *
     * @param docId 文档ID
     * @param document 新的文档对象
     * @return 是否更新成功
     */
    public boolean updateDocument(String docId, Document document) {
        // 1. 更新存储
        boolean updated = storageEngine.update(docId, document);

        if (updated) {
            // 2. 更新索引
            indexEngine.updateIndex(docId, document);

            // 3. 使缓存失效
            if (configuration.getCache().isEnabled()) {
                cacheEngine.invalidateDocument(docId);
            }

            log.debug("Document updated: {}", docId);
        }

        return updated;
    }

    /**
     * 删除文档
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    public boolean deleteDocument(String docId) {
        // 1. 从存储删除
        boolean deleted = storageEngine.delete(docId);

        if (deleted) {
            // 2. 从索引删除
            indexEngine.deleteFromIndex(docId);

            // 3. 从缓存删除
            if (configuration.getCache().isEnabled()) {
                cacheEngine.invalidateDocument(docId);
            }

            log.debug("Document deleted: {}", docId);
        }

        return deleted;
    }

    /**
     * 删除所有文档
     *
     * @return 删除的文档数量
     */
    public int deleteAllDocuments() {
        log.info("Deleting all documents...");

        // 1. 获取所有文档ID
        List<String> allDocIds = storageEngine.getAllDocumentIds();
        int count = allDocIds.size();

        if (count == 0) {
            log.info("No documents to delete");
            return 0;
        }

        log.info("Found {} documents to delete", count);

        // 2. 删除所有文档
        for (String docId : allDocIds) {
            try {
                storageEngine.delete(docId);
                indexEngine.deleteFromIndex(docId);

                if (configuration.getCache().isEnabled()) {
                    cacheEngine.invalidateDocument(docId);
                }
            } catch (Exception e) {
                log.warn("Failed to delete document: {}", docId, e);
            }
        }

        // 3. 清空缓存
        if (configuration.getCache().isEnabled()) {
            cacheEngine.clear();
        }

        // 4. 提交更改
        commit();

        log.info("Deleted {} documents", count);
        return count;
    }

    /**
     * 优化索引
     */
    public void optimizeIndex() {
        log.info("Optimizing index...");
        indexEngine.optimize();
        log.info("Index optimization completed");
    }

    /**
     * 提交索引更改
     */
    public void commit() {
        indexEngine.commit();
    }

    /**
     * 获取统计信息
     *
     * @return 统计对象
     */
    public Statistics getStatistics() {
        return Statistics.builder()
                .documentCount(storageEngine.count())
                .indexedDocumentCount(indexEngine.getDocumentCount())
                .cacheStats(configuration.getCache().isEnabled() ?
                        cacheEngine.getStats() : null)
                .build();
    }

    /**
     * 获取索引引擎（用于高级查询处理）
     */
    public IndexEngine getIndexEngine() {
        return indexEngine;
    }

    /**
     * 获取缓存引擎（用于高级查询处理）
     */
    public CacheEngine getCacheEngine() {
        return cacheEngine;
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        log.info("Closing Local File RAG...");
        try {
            indexEngine.commit();
            indexEngine.close();
            cacheEngine.clear();
            log.info("Local File RAG closed successfully");
        } catch (Exception e) {
            log.error("Error closing Local File RAG", e);
        }
    }

    /**
     * 生成查询缓存键
     */
    private String generateQueryKey(Query query) {
        return String.format("query:%s:limit:%d:offset:%d",
                query.getQueryText(), query.getLimit(), query.getOffset());
    }

    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类
     */
    public static class Builder {
        private RAGConfiguration configuration = RAGConfiguration.createDefault();
        private StorageEngine storageEngine;
        private IndexEngine indexEngine;
        private CacheEngine cacheEngine;

        public Builder configuration(RAGConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder storagePath(String path) {
            this.configuration.getStorage().setBasePath(path);
            return this;
        }

        public Builder enableCache(boolean enabled) {
            this.configuration.getCache().setEnabled(enabled);
            return this;
        }

        public Builder enableCompression(boolean enabled) {
            this.configuration.getStorage().setCompression(enabled);
            return this;
        }

        public Builder storageEngine(StorageEngine storageEngine) {
            this.storageEngine = storageEngine;
            return this;
        }

        public Builder indexEngine(IndexEngine indexEngine) {
            this.indexEngine = indexEngine;
            return this;
        }

        public Builder cacheEngine(CacheEngine cacheEngine) {
            this.cacheEngine = cacheEngine;
            return this;
        }

        public LocalFileRAG build() {
            // 如果未提供自定义引擎，则创建默认实现
            if (storageEngine == null) {
                storageEngine = top.yumbo.ai.rag.factory.RAGEngineFactory.createStorageEngine(configuration);
            }
            if (indexEngine == null) {
                indexEngine = top.yumbo.ai.rag.factory.RAGEngineFactory.createIndexEngine(configuration);
            }
            if (cacheEngine == null) {
                cacheEngine = top.yumbo.ai.rag.factory.RAGEngineFactory.createCacheEngine(configuration);
            }

            return new LocalFileRAG(configuration, storageEngine, indexEngine, cacheEngine);
        }
    }

    /**
     * 统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class Statistics {
        private long documentCount;
        private long indexedDocumentCount;
        private CacheEngine.CacheStats cacheStats;
    }
}

