package top.yumbo.ai.rag.chunking.strategy;

import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;

import java.util.List;

/**
 * 文档分块策略接口 (Document Chunking Strategy Interface)
 *
 * 支持多种分块策略 (Supports Multiple Chunking Strategies):
 * - PPL-based: 基于困惑度的本地分块 (Local chunking based on perplexity)
 * - LLM-based: 基于大语言模型的智能分块 (Intelligent chunking based on large language models)
 * - Fixed-size: 固定大小分块 (Fixed-size chunking)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
public interface ChunkingStrategy {

    /**
     * 对文档内容进行分块 (Chunk document content)
     *
     * @param content 文档内容 (Document content)
     * @param query 查询（可选，用于查询感知分块）(Query, optional, for query-aware chunking)
     * @param config 分块配置 (Chunking configuration)
     * @return 文档块列表 (List of document chunks)
     * @throws PPLException 分块失败时抛出 (Thrown when chunking fails)
     */
    List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException;

    /**
     * 获取策略名称 (Get strategy name)
     */
    String getStrategyName();

    /**
     * 检查策略是否可用 (Check if strategy is available)
     */
    boolean isAvailable();
}

