package top.yumbo.ai.rag.chunking.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档块存储信息
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStorageInfo {

    /**
     * 块ID（唯一标识）
     */
    private String chunkId;

    /**
     * 文档ID（原始文档名）
     */
    private String documentId;

    /**
     * 块索引
     */
    private int chunkIndex;

    /**
     * 块标题
     */
    private String title;

    /**
     * 内容文件路径
     */
    private String contentPath;

    /**
     * 元数据文件路径
     */
    private String metadataPath;

    /**
     * 内容长度
     */
    private int contentLength;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return String.format("%s (块 %d)", title, chunkIndex + 1);
        }
        return String.format("文档块 %d", chunkIndex + 1);
    }

    /**
     * 获取下载文件名
     */
    public String getDownloadFilename() {
        return chunkId + ".md";
    }
}

