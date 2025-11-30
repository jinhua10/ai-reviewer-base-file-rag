package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.List;
import java.util.stream.Stream;

/**
 * 存储引擎接口 (Storage engine interface)
 * 负责文档的持久化存储和检索 (Responsible for document persistent storage and retrieval)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface StorageEngine {

    /**
     * 存储文档 (Store document)
     *
     * @param document 文档对象 (document object)
     * @return 文档ID (document ID)
     */
    String store(Document document);

    /**
     * 批量存储文档 (Batch store documents)
     *
     * @param documents 文档列表 (list of documents)
     * @return 成功存储的数量 (number of successfully stored)
     */
    int storeBatch(List<Document> documents);

    /**
     * 检索文档 (Retrieve document)
     *
     * @param id 文档ID (document ID)
     * @return 文档对象，如果不存在则返回null (document object, returns null if not exists)
     */
    Document retrieve(String id);

    /**
     * 删除文档 (Delete document)
     *
     * @param id 文档ID (document ID)
     * @return 是否删除成功 (whether deletion successful)
     */
    boolean delete(String id);

    /**
     * 更新文档 (Update document)
     *
     * @param id 文档ID (document ID)
     * @param document 新的文档对象 (new document object)
     * @return 是否更新成功 (whether update successful)
     */
    boolean update(String id, Document document);

    /**
     * 列出所有文档 (List all documents)
     *
     * @return 文档流 (document stream)
     */
    Stream<Document> listAll();

    /**
     * 根据条件列出文档 (List documents by query)
     *
     * @param query 查询条件 (query condition)
     * @return 文档流 (document stream)
     */
    Stream<Document> list(Query query);

    /**
     * 获取文档总数 (Get total document count)
     *
     * @return 文档数量 (document count)
     */
    long count();

    /**
     * 获取所有文档ID (Get all document IDs)
     *
     * @return 文档ID列表 (list of document IDs)
     */
    List<String> getAllDocumentIds();

    /**
     * 检查文档是否存在 (Check if document exists)
     *
     * @param id 文档ID (document ID)
     * @return 是否存在 (whether exists)
     */
    boolean exists(String id);

    /**
     * 清空所有文档 (Clear all documents)
     */
    void clear();
}
