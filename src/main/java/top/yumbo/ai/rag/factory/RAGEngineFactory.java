package top.yumbo.ai.rag.factory;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.CacheEngine;
import top.yumbo.ai.rag.core.IndexEngine;
import top.yumbo.ai.rag.core.StorageEngine;
import top.yumbo.ai.rag.impl.cache.CaffeineCacheEngine;
import top.yumbo.ai.rag.impl.index.LuceneIndexEngine;
import top.yumbo.ai.rag.impl.storage.FileSystemStorageEngine;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * RAG引擎工厂
 * 用于创建各种引擎的默认实现
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class RAGEngineFactory {

    private static String fmt(String key, Object... args) {
        try {
            Locale locale = Locale.getDefault();
            Locale use = (locale != null && "zh".equalsIgnoreCase(locale.getLanguage())) ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
            ResourceBundle bundle = ResourceBundle.getBundle("messages", use);
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, args == null ? new Object[0] : args);
        } catch (MissingResourceException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建默认的存储引擎
     */
    public static StorageEngine createStorageEngine(RAGConfiguration config) {
        String m = fmt("log.factory.create_filesystem");
        if (m != null) {
            log.info(m);
        } else {
            log.info("Creating FileSystemStorageEngine");
        }
        return new FileSystemStorageEngine(config.getStorage());
    }

    /**
     * 创建默认的索引引擎
     */
    public static IndexEngine createIndexEngine(RAGConfiguration config) {
        String m = fmt("log.factory.create_lucene");
        if (m != null) {
            log.info(m);
        } else {
            log.info("Creating LuceneIndexEngine");
        }
        return new LuceneIndexEngine(config.getIndex(), config.getStorage().getBasePath());
    }

    /**
     * 创建默认的缓存引擎
     */
    public static CacheEngine createCacheEngine(RAGConfiguration config) {
        String m = fmt("log.factory.create_caffeine");
        if (m != null) {
            log.info(m);
        } else {
            log.info("Creating CaffeineCacheEngine");
        }
        return new CaffeineCacheEngine(config.getCache());
    }
}
