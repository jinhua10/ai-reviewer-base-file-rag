package top.yumbo.ai.rag.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * 文档块 (Document chunk)
 * 表示切分后的文档片段 (Represents a fragment of a chunked document)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@SuppressWarnings("unused")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 块内容 (chunk content)
     */
    private String content;

    /**
     * 块标题（可选） (chunk title, optional)
     */
    private String title;

    /**
     * 块索引（从0开始） (chunk index, zero-based)
     */
    private int index;

    /**
     * 总块数 (total number of chunks)
     */
    private int totalChunks;

    /**
     * 起始位置（在原文档中的字符偏移） (start position in original document)
     */
    private int startPosition;

    /**
     * 结束位置（在原文档中的字符偏移） (end position in original document)
     */
    private int endPosition;

    /**
     * 元数据（可选） (metadata, optional)
     */
    private String metadata;

    /**
     * 获取块的显示名称 (Get display name for the chunk)
     */
    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return LogMessageProvider.getMessage("log.chunk.display_part", index + 1, totalChunks);
    }

    /**
     * 获取内容长度 (Get content length)
     */
    public int getLength() {
        return content != null ? content.length() : 0;
    }

    /**
     * 是否是第一个块 (Is first chunk)
     */
    public boolean isFirst() {
        return index == 0;
    }

    /**
     * 是否是最后一个块 (Is last chunk)
     */
    public boolean isLast() {
        return index == totalChunks - 1;
    }
}
