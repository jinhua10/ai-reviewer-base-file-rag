package top.yumbo.ai.rag.chunking.impl;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.ChunkingConfig;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.DocumentChunker;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单文档切分器
 * 按固定长度切分，性能最好但可能在句子中间切断
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class SimpleDocumentChunker implements DocumentChunker {

    private final ChunkingConfig config;

    public SimpleDocumentChunker(ChunkingConfig config) {
        this.config = config;
        config.validate();
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        // 防止处理超大文档导致内存溢出（从配置读取）
        int maxContentLength = config.getMaxContentLength();
        if (content.length() > maxContentLength) {
            log.warn("Content too large ({} chars), truncating to {} chars to prevent OOM (config: max-chunk-content-length)",
                    content.length(), maxContentLength);
            content = content.substring(0, maxContentLength);
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        int position = 0;
        int index = 0;

        // 限制最大块数，防止内存溢出（从配置读取）
        int maxChunks = config.getMaxChunks();

        while (position < content.length() && index < maxChunks) {
            int end = Math.min(position + chunkSize, content.length());

            // 如果配置了在句子边界切分，尝试调整结束位置
            if (config.isSplitOnSentence() && end < content.length()) {
                end = adjustToSentenceBoundary(content, end);
            }

            String chunkContent = content.substring(position, end).trim();

            if (!chunkContent.isEmpty()) {
                chunks.add(DocumentChunk.builder()
                        .content(chunkContent)
                        .index(index)
                        .totalChunks(-1) // 暂时未知，后面更新
                        .startPosition(position)
                        .endPosition(end)
                        .build());

                index++;
            }

            // 移动位置，考虑重叠
            position = end - overlap;
            if (position <= 0 || position >= content.length()) {
                break;
            }
        }

        if (index >= maxChunks) {
            log.warn("Reached maximum chunk limit ({}, config: max-chunks-per-document), stopping chunking", maxChunks);
        }

        // 更新总块数
        int totalChunks = chunks.size();
        chunks.forEach(chunk -> chunk.setTotalChunks(totalChunks));

        log.debug("Simple chunking: {} chars -> {} chunks (size={}, overlap={})",
                content.length(), totalChunks, chunkSize, overlap);

        return chunks;
    }

    /**
     * 调整到句子边界
     */
    private int adjustToSentenceBoundary(String text, int position) {
        int searchRange = Math.min(100, position / 10); // 搜索范围：10%或100字符
        int searchStart = Math.max(0, position - searchRange);

        // 向前搜索句子结束符
        for (int i = position - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                return i + 1;
            }
        }

        return position;
    }

    /**
     * 判断是否是句子结束符
     */
    private boolean isSentenceEnding(char c) {
        return c == '。' || c == '！' || c == '？' ||
               c == '.' || c == '!' || c == '?' ||
               c == '\n' || c == '\r';
    }

    @Override
    public String getName() {
        return "Simple Chunker";
    }

    @Override
    public String getDescription() {
        return "按固定长度切分，性能最好";
    }
}

