package top.yumbo.ai.rag.optimization;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.model.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档分块器（Document chunker）
 * 将大文档拆分为多个小块，以降低内存占用并提高检索精度（Split large documents into smaller chunks to reduce memory usage and improve retrieval accuracy）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class DocumentChunker {

    private final int chunkSize;
    private final int chunkOverlap;
    private final boolean smartSplit;
    private final int maxContentLength;
    private final int maxChunks;

    /**
     * 默认分块配置（Default chunking configuration）
     * 🔧 优化：增加分块大小以支持大文件处理（Optimization: increase chunk size to support large file processing）
     */
    public static final int DEFAULT_CHUNK_SIZE = 2000;  // 2000字符（2000 characters）
    public static final int DEFAULT_CHUNK_OVERLAP = 400; // 400字符重叠（400 characters overlap）
    public static final int DEFAULT_MAX_CONTENT_LENGTH = 100000; // 100000字符（100000 characters）
    public static final int DEFAULT_MAX_CHUNKS = 50; // 每个文档最大分块数（Maximum chunks per document）

    /**
     * 句子结束符（Sentence endings）
     */
    private static final char[] SENTENCE_ENDINGS = {'.', '。', '!', '！', '?', '？', '\n'};

    public DocumentChunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, true, DEFAULT_MAX_CONTENT_LENGTH, DEFAULT_MAX_CHUNKS);
    }

    public DocumentChunker(int chunkSize, int chunkOverlap, boolean smartSplit, int maxContentLength, int maxChunks) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.smartSplit = smartSplit;
        this.maxContentLength = maxContentLength;
        this.maxChunks = maxChunks;

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }

        log.info(LogMessageProvider.getMessage("log.optimization.chunker.initialized",
            chunkSize, chunkOverlap, smartSplit, maxContentLength, maxChunks));
    }

    /**
     * 将文档分块
     *
     * @param document 原始文档
     * @return 分块后的文档列表
     */
    public List<Document> chunk(Document document) {
        String content = document.getContent();

        // 如果文档小于分块大小，直接返回
        if (content.length() <= chunkSize) {
            log.debug("Document {} is small enough, no chunking needed", document.getId());
            return List.of(document);
        }

        List<Document> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int start = 0;

        log.debug("Chunking document {} with content length: {}",
            document.getId(), content.length());

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 智能分割：尝试在句子边界处分割
            if (smartSplit && end < content.length()) {
                int adjustedEnd = findSentenceBoundary(content, start, end);
                if (adjustedEnd > start) {
                    end = adjustedEnd;
                }
            }

            String chunkContent = content.substring(start, end).trim();

            // 跳过空块
            if (chunkContent.isEmpty()) {
                start = end;
                continue;
            }

            Document chunk = createChunk(document, chunkContent, chunkIndex, start, end);
            chunks.add(chunk);

            // 下一个块的起始位置（带重叠）
            int nextStart = end - chunkOverlap;

            // 🔧 修复：确保 start 位置始终向前推进，避免无限循环
            if (nextStart <= start) {
                nextStart = start + 1; // 至少前进1个字符
            }

            start = nextStart;

            chunkIndex++;

            // 超过最大分块数，停止分块
            if (chunkIndex >= maxChunks) {
                log.warn("Document {} exceeded maxChunks limit ({}), stopping chunking",
                    document.getId(), maxChunks);
                break;
            }
        }

        log.info(LogMessageProvider.getMessage("log.optimization.chunker.chunked", document.getId(), chunks.size()));
        return chunks;
    }

    /**
     * 创建分块文档
     */
    private Document createChunk(Document original, String chunkContent,
                                 int chunkIndex, int start, int end) {
        Map<String, Object> metadata = new HashMap<>(original.getMetadata());

        // 添加分块相关元数据
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("chunkStart", start);
        metadata.put("chunkEnd", end);
        metadata.put("parentDocId", original.getId());
        metadata.put("isChunk", true);
        metadata.put("originalLength", original.getContent().length());

        String chunkId = original.getId() + "_chunk_" + chunkIndex;
        String chunkTitle = original.getTitle() + " (Part " + (chunkIndex + 1) + ")";

        return Document.builder()
            .id(chunkId)
            .title(chunkTitle)
            .content(chunkContent)
            .metadata(metadata)
            .build();
    }

    /**
     * 智能查找句子边界
     * 在指定范围内查找最近的句子结束符
     */
    private int findSentenceBoundary(String content, int start, int preferredEnd) {
        // 向后查找最多100个字符
        int searchEnd = Math.min(preferredEnd + 100, content.length());

        // 首先尝试在preferredEnd之后查找句子结束符
        for (int i = preferredEnd; i < searchEnd; i++) {
            if (isSentenceEnding(content.charAt(i))) {
                return i + 1; // 包含句子结束符
            }
        }

        // 如果向后找不到，尝试向前查找（但不超过chunkSize的一半）
        int searchStart = Math.max(preferredEnd - chunkSize / 2, start);
        for (int i = preferredEnd - 1; i >= searchStart; i--) {
            if (isSentenceEnding(content.charAt(i))) {
                return i + 1;
            }
        }

        // 如果都找不到，返回原始位置
        return preferredEnd;
    }

    /**
     * 检查字符是否是句子结束符
     */
    private boolean isSentenceEnding(char c) {
        for (char ending : SENTENCE_ENDINGS) {
            if (c == ending) {
                return true;
            }
        }
        return false;
    }

    /**
     * 批量分块
     *
     * @param documents 原始文档列表
     * @return 分块后的文档列表
     */
    public List<Document> chunkBatch(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();

        for (Document doc : documents) {
            List<Document> chunks = chunk(doc);
            allChunks.addAll(chunks);
        }

        log.info(LogMessageProvider.getMessage("log.optimization.chunker.batch_completed", documents.size(), allChunks.size()));

        return allChunks;
    }

    /**
     * 获取分块统计信息
     */
    public ChunkingStats getChunkingStats(Document document) {
        int contentLength = document.getContent().length();
        int estimatedChunks = (int) Math.ceil((double) contentLength / (chunkSize - chunkOverlap));

        return ChunkingStats.builder()
            .originalLength(contentLength)
            .chunkSize(chunkSize)
            .chunkOverlap(chunkOverlap)
            .estimatedChunks(estimatedChunks)
            .needsChunking(contentLength > chunkSize)
            .build();
    }

    /**
     * 分块统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class ChunkingStats {
        private int originalLength;
        private int chunkSize;
        private int chunkOverlap;
        private int estimatedChunks;
        private boolean needsChunking;

        @Override
        public String toString() {
            return String.format("ChunkingStats[originalLength=%d, chunkSize=%d, overlap=%d, " +
                    "estimatedChunks=%d, needsChunking=%s]",
                originalLength, chunkSize, chunkOverlap, estimatedChunks, needsChunking);
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
        private boolean smartSplit = true;
        private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
        private int maxChunks = DEFAULT_MAX_CHUNKS;

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder chunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Builder smartSplit(boolean smartSplit) {
            this.smartSplit = smartSplit;
            return this;
        }

        public Builder maxContentLength(int maxContentLength) {
            this.maxContentLength = maxContentLength;
            return this;
        }

        public Builder maxChunks(int maxChunks) {
            this.maxChunks = maxChunks;
            return this;
        }

        public DocumentChunker build() {
            return new DocumentChunker(chunkSize, chunkOverlap, smartSplit, maxContentLength, maxChunks);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
