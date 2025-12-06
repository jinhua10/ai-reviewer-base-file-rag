package top.yumbo.ai.rag.chunking.strategy;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单分块策略（降级方案）
 * Simple chunking strategy (fallback)
 *
 * 使用固定大小和段落边界进行分块
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
public class SimpleChunkingStrategy implements ChunkingStrategy {

    public SimpleChunkingStrategy() {
        log.info("📝 使用简单分块策略（降级方案）");
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = content.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() > config.getMaxChunkSize() &&
                currentChunk.length() > 0) {

                // 保存当前块
                chunks.add(DocumentChunk.builder()
                        .content(currentChunk.toString().trim())
                        .index(chunkIndex++)
                        .build());
                currentChunk = new StringBuilder();
            }

            currentChunk.append(para).append("\n\n");
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                    .content(currentChunk.toString().trim())
                    .index(chunkIndex)
                    .build());
        }

        log.info("📄 简单分块完成：{} 块", chunks.size());
        return chunks;
    }

    @Override
    public String getStrategyName() {
        return "Simple Chunking (Fallback)";
    }

    @Override
    public boolean isAvailable() {
        return true;  // 总是可用
    }
}

