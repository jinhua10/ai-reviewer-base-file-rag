package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

/**
 * 索引引擎接口
 * 负责文档的索引构建和搜索
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface IndexEngine {

    /**
     * 索引文档
     *
     * @param document 文档对象
     */
    void indexDocument(Document document);

    /**
     * 批量索引文档
     *
     * @param documents 文档列表
     */
    void indexBatch(Iterable<Document> documents);

    /**
     * 更新索引
     *
     * @param docId 文档ID
     * @param document 新的文档对象
     */
    void updateIndex(String docId, Document document);

    /**
     * 从索引中删除文档
     *
     * @param docId 文档ID
     */
    void deleteFromIndex(String docId);

    /**
     * 搜索文档
     *
     * @param query 查询对象
     * @return 搜索结果
     */
    SearchResult search(Query query);

    /**
     * 优化索引
     * 合并索引段，提高查询性能
     */
    void optimize();

    /**
     * 提交索引更改
     * 将内存中的更改持久化到磁盘
     */
    void commit();

    /**
     * 获取索引的文档数量
     *
     * @return 文档数量
     */
    long getDocumentCount();

    /**
     * 关闭索引引擎
     * 释放资源
     */
    void close();

    /**
     * 重建索引
     * 清空现有索引并重新构建
     */
    void rebuild();
}

