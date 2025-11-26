package top.yumbo.ai.rag.chunking;

import java.util.List;

/**
 * 文档切分器接口
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public interface DocumentChunker {

    /**
     * 切分文档
     *
     * @param content 文档内容
     * @param query 用户查询（可选，用于智能切分）
     * @return 文档块列表
     */
    List<DocumentChunk> chunk(String content, String query);

    /**
     * 切分文档（无查询上下文）
     *
     * @param content 文档内容
     * @return 文档块列表
     */
    default List<DocumentChunk> chunk(String content) {
        return chunk(content, null);
    }

    /**
     * 获取切分器名称
     */
    String getName();

    /**
     * 获取切分器描述
     */
    String getDescription();
}

