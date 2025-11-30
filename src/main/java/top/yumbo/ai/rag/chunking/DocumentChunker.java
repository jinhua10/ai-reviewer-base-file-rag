package top.yumbo.ai.rag.chunking;

import java.util.List;

/**
 * 文档切分器接口 (Document chunker interface)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public interface DocumentChunker {

    /**
     * 切分文档 (Chunk a document)
     *
     * @param content 文档内容 (document content)
     * @param query 用户查询（可选，用于智能切分） (user query, optional - used by smart chunkers)
     * @return 文档块列表 (list of document chunks)
     */
    List<DocumentChunk> chunk(String content, String query);

    /**
     * 切分文档（无查询上下文） (Chunk a document without query context)
     *
     * @param content 文档内容 (document content)
     * @return 文档块列表 (list of document chunks)
     */
    default List<DocumentChunk> chunk(String content) {
        return chunk(content, null);
    }

    /**
     * 获取切分器名称 (Get chunker name)
     */
    String getName();

    /**
     * 获取切分器描述 (Get chunker description)
     */
    String getDescription();
}
