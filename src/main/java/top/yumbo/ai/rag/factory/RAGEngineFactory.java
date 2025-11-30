package top.yumbo.ai.rag.factory;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.core.StorageEngine;
import top.yumbo.ai.rag.impl.cache.CaffeineCacheEngine;
import top.yumbo.ai.rag.impl.index.LuceneIndexEngine;
import top.yumbo.ai.rag.impl.storage.FileSystemStorageEngine;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * RAG引擎工厂 (RAG engine factory)
 * 用于创建各种引擎的默认实现 (Used to create default implementations of various engines)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class RAGEngineFactory {

    /**
     * 创建默认的存储引擎 (Create default storage engine)
     */
    public static StorageEngine createStorageEngine(RAGConfiguration config) {
        log.info(LogMessageProvider.getMessage("log.factory.create_filesystem"));
        return new FileSystemStorageEngine(config.getStorage());
    }

    /**
     * 创建默认的索引引擎 (Create default index engine)
     */
    public static IndexEngine createIndexEngine(RAGConfiguration config) {
        log.info(LogMessageProvider.getMessage("log.factory.create_lucene"));
        return new LuceneIndexEngine(config.getIndex(), config.getStorage().getBasePath());
    }

    /**
     * 创建默认的缓存引擎 (Create default cache engine)
     */
    public static CacheEngine createCacheEngine(RAGConfiguration config) {
        log.info(LogMessageProvider.getMessage("log.factory.create_caffeine"));
        return new CaffeineCacheEngine(config.getCache());
    }
}
