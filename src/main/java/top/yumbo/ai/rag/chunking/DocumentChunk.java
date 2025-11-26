package top.yumbo.ai.rag.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档块
 * 表示切分后的文档片段
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 块内容
     */
    private String content;

    /**
     * 块标题（可选）
     */
    private String title;

    /**
     * 块索引（从0开始）
     */
    private int index;

    /**
     * 总块数
     */
    private int totalChunks;

    /**
     * 起始位置（在原文档中的字符偏移）
     */
    private int startPosition;

    /**
     * 结束位置（在原文档中的字符偏移）
     */
    private int endPosition;

    /**
     * 元数据（可选）
     */
    private String metadata;

    /**
     * 获取块的显示名称
     */
    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return String.format("Part %d/%d", index + 1, totalChunks);
    }

    /**
     * 获取内容长度
     */
    public int getLength() {
        return content != null ? content.length() : 0;
    }

    /**
     * 是否是第一个块
     */
    public boolean isFirst() {
        return index == 0;
    }

    /**
     * 是否是最后一个块
     */
    public boolean isLast() {
        return index == totalChunks - 1;
    }
}

