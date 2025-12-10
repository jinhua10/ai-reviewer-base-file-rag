package top.yumbo.ai.rag.chunking.impl;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.ChunkingConfig;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.DocumentChunker;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单文档切分器 (Simple document chunker)
 * 按固定长度切分，性能最好但可能在句子中间切断
 * (Chunks by fixed length; best performance but may cut sentences)
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

        // 防止处理超大文档导致内存溢出（从配置读取）(Prevent OOM when processing large documents - read from config)
        int maxContentLength = config.getMaxContentLength();
        if (content.length() > maxContentLength) {
            log.warn(I18N.get("log.chunk.content_truncate", content.length(), maxContentLength));
            content = content.substring(0, maxContentLength);
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        int position = 0;
        int index = 0;

        // 限制最大块数，防止内存溢出（从配置读取）(Limit max chunks to prevent OOM - read from config)
        int maxChunks = config.getMaxChunks();

        while (position < content.length() && index < maxChunks) {
            int end = Math.min(position + chunkSize, content.length());

            // 如果配置了在句子边界切分，尝试调整结束位置 (If sentence boundary splitting is configured, try to adjust end position)
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

            // 移动位置，考虑重叠 (Move position, considering overlap)
            position = end - overlap;
            if (position <= 0 || position >= content.length()) {
                break;
            }
        }

        if (index >= maxChunks) {
            log.warn(I18N.get("log.chunk.max_chunks_reached", maxChunks));
        }

        // 更新总块数 (Update total chunk count)
        int totalChunks = chunks.size();
        chunks.forEach(chunk -> chunk.setTotalChunks(totalChunks));

        log.debug(I18N.get("log.chunk.simple_summary", content.length(), totalChunks, chunkSize, overlap));

        return chunks;
    }

    /**
     * 调整到句子边界 (Adjust to sentence boundary)
     */
    private int adjustToSentenceBoundary(String text, int position) {
        int searchRange = Math.min(100, position / 10); // 搜索范围：10%或100字符 (Search range: 10% or 100 characters)
        int searchStart = Math.max(0, position - searchRange);

        // 向前搜索句子结束符 (Search backwards for sentence ending)
        for (int i = position - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                return i + 1;
            }
        }

        return position;
    }

    /**
     * 判断是否是句子结束符 (Check sentence ending)
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
        return "按固定长度切分，性能最好 (Chunks by fixed length)";
    }
}
