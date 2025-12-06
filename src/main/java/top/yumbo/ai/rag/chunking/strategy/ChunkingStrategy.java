package top.yumbo.ai.rag.chunking.strategy;

import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;

import java.util.List;

/**
 * 文档分块策略接口
 * Document chunking strategy interface
 *
 * 支持多种分块策略：
 * - PPL-based: 基于困惑度的本地分块
 * - LLM-based: 基于大语言模型的智能分块
 * - Fixed-size: 固定大小分块
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
public interface ChunkingStrategy {

    /**
     * 对文档内容进行分块
     *
     * @param content 文档内容
     * @param query 查询（可选，用于查询感知分块）
     * @param config 分块配置
     * @return 文档块列表
     * @throws PPLException 分块失败时抛出
     */
    List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException;

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 检查策略是否可用
     */
    boolean isAvailable();
}

