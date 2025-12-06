package top.yumbo.ai.rag.chunking.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 文档块存储信息 (Document chunk storage info)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@SuppressWarnings("unused")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStorageInfo {

    /**
     * 块ID（唯一标识） (chunk ID - unique)
     */
    private String chunkId;

    /**
     * 文档ID（原始文档名） (document ID - original name)
     */
    private String documentId;

    /**
     * 块索引 (chunk index)
     */
    private int chunkIndex;

    /**
     * 块标题 (chunk title)
     */
    private String title;

    /**
     * 内容文件路径 (content file path)
     */
    private String contentPath;

    /**
     * 元数据文件路径 (metadata file path)
     */
    private String metadataPath;

    /**
     * 内容长度 (content length)
     */
    private int contentLength;

    /**
     * 创建时间 (created at)
     */
    private String createdAt;

    /**
     * 获取显示名称 (Get display name)
     */
    public String getDisplayName() {
        if (title != null && !title.isEmpty()) {
            return String.format("%s (块 %d)", title, chunkIndex + 1);
        }
        return I18N.get("log.chunk.display_part", chunkIndex + 1, Integer.max(chunkIndex + 1, 1));
    }

    /**
     * 获取下载文件名 (Get download filename)
     */
    public String getDownloadFilename() {
        return chunkId + ".md";
    }
}
