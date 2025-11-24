package top.yumbo.ai.rag.core;

import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;

import java.util.List;
import java.util.stream.Stream;

/**
 * 存储引擎接口
 * 负责文档的持久化存储和检索
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface StorageEngine {

    /**
     * 存储文档
     *
     * @param document 文档对象
     * @return 文档ID
     */
    String store(Document document);

    /**
     * 批量存储文档
     *
     * @param documents 文档列表
     * @return 成功存储的数量
     */
    int storeBatch(List<Document> documents);

    /**
     * 检索文档
     *
     * @param id 文档ID
     * @return 文档对象，如果不存在则返回null
     */
    Document retrieve(String id);

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 更新文档
     *
     * @param id 文档ID
     * @param document 新的文档对象
     * @return 是否更新成功
     */
    boolean update(String id, Document document);

    /**
     * 列出所有文档
     *
     * @return 文档流
     */
    Stream<Document> listAll();

    /**
     * 根据条件列出文档
     *
     * @param query 查询条件
     * @return 文档流
     */
    Stream<Document> list(Query query);

    /**
     * 获取文档总数
     *
     * @return 文档数量
     */
    long count();

    /**
     * 获取所有文档ID
     *
     * @return 文档ID列表
     */
    List<String> getAllDocumentIds();

    /**
     * 检查文档是否存在
     *
     * @param id 文档ID
     * @return 是否存在
     */
    boolean exists(String id);

    /**
     * 清空所有文档
     */
    void clear();
}

