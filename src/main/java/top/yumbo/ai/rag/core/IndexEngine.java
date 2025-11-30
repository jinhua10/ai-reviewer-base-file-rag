package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

/**
 * 索引引擎接口 (Index engine interface)
 * 负责文档的索引构建和搜索 (Responsible for document indexing and search)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface IndexEngine {

    /**
     * 索引文档 (Index document)
     *
     * @param document 文档对象 (document object)
     */
    void indexDocument(Document document);

    /**
     * 批量索引文档 (Batch index documents)
     *
     * @param documents 文档列表 (list of documents)
     */
    void indexBatch(Iterable<Document> documents);

    /**
     * 更新索引 (Update index)
     *
     * @param docId 文档ID (document ID)
     * @param document 新的文档对象 (new document object)
     */
    void updateIndex(String docId, Document document);

    /**
     * 从索引中删除文档 (Delete document from index)
     *
     * @param docId 文档ID (document ID)
     */
    void deleteFromIndex(String docId);

    /**
     * 搜索文档 (Search documents)
     *
     * @param query 查询对象 (query object)
     * @return 搜索结果 (search result)
     */
    SearchResult search(Query query);

    /**
     * 优化索引 (Optimize index)
     * 合并索引段，提高查询性能 (Merge index segments to improve query performance)
     */
    void optimize();

    /**
     * 提交索引更改 (Commit index changes)
     * 将内存中的更改持久化到磁盘 (Persist in-memory changes to disk)
     */
    void commit();

    /**
     * 获取索引的文档数量 (Get document count in index)
     *
     * @return 文档数量 (document count)
     */
    long getDocumentCount();

    /**
     * 关闭索引引擎 (Close index engine)
     * 释放资源 (Release resources)
     */
    void close();

    /**
     * 重建索引 (Rebuild index)
     * 清空现有索引并重新构建 (Clear existing index and rebuild)
     */
    void rebuild();
}
