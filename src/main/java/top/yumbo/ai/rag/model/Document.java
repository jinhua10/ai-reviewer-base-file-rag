package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档模型（Document model）
 * 表示系统中的一个文档实体（Represents a document entity in the system）
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识（Document unique identifier）
     */
    private String id;

    /**
     * 文档标题（Document title）
     */
    private String title;

    /**
     * 文档内容（Document content）
     */
    private String content;

    /**
     * 文件路径（File path）
     */
    private String filePath;

    /**
     * 文件大小（字节）（File size (bytes)）
     */
    private Long fileSize;

    /**
     * MIME类型（MIME type）
     */
    private String mimeType;

    /**
     * 创建时间（Creation time）
     */
    private Instant createdAt;

    /**
     * 更新时间（Update time）
     */
    private Instant updatedAt;

    /**
     * 文档元数据（Document metadata）
     */
    private Map<String, Object> metadata;

    /**
     * 文档状态（Document status）
     */
    private DocumentStatus status;

    /**
     * 文档来源（Document source）
     */
    private String source;

    /**
     * 文档标签（Document tags）
     */
    private String[] tags;

    /**
     * 文档版本（Document version）
     */
    private String version;

    /**
     * 文档语言（Document language）
     */
    private String language;

    /**
     * 文档摘要（Document summary）
     */
    private String summary;

    /**
     * 文档关键词（Document keywords）
     */
    private String[] keywords;

    /**
     * 文档作者（Document author）
     */
    private String author;

    /**
     * 文档分类（Document category）
     */
    private String category;

    /**
     * 内容哈希（Content hash）
     */
    private String contentHash;

    /**
     * 文档优先级（Document priority）
     */
    private Integer priority;

    /**
     * 是否已索引（Whether indexed）
     */
    private Boolean indexed;

    /**
     * 索引时间（Index time）
     */
    private Instant indexedAt;

    /**
     * 最后访问时间（Last access time）
     */
    private Instant lastAccessedAt;

    /**
     * 访问次数（Access count）
     */
    private Integer accessCount;

    /**
     * 文档评分（Document score）
     */
    private Double score;

    /**
     * 文档权重（Document weight）
     */
    private Double weight;

    /**
     * 文档状态枚举（Document status enumeration）
     */
    public enum DocumentStatus {
        /**
         * 草稿（Draft）
         */
        DRAFT,

        /**
         * 已发布（Published）
         */
        PUBLISHED,

        /**
         * 已归档（Archived）
         */
        ARCHIVED,

        /**
         * 已删除（Deleted）
         */
        DELETED
    }

    /**
     * 获取元数据值（Get metadata value）
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 设置元数据值（Set metadata value）
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * 添加标签（Add tag）
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new String[]{tag};
        } else {
            String[] newTags = new String[tags.length + 1];
            System.arraycopy(tags, 0, newTags, 0, tags.length);
            newTags[tags.length] = tag;
            tags = newTags;
        }
    }

    /**
     * 移除标签（Remove tag）
     */
    public void removeTag(String tag) {
        if (tags != null) {
            String[] newTags = new String[tags.length - 1];
            int index = 0;
            for (String t : tags) {
                if (!t.equals(tag)) {
                    newTags[index++] = t;
                }
            }
            tags = newTags;
        }
    }

    /**
     * 检查是否包含标签（Check if contains tag）
     */
    public boolean hasTag(String tag) {
        if (tags == null) {
            return false;
        }
        for (String t : tags) {
            if (t.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有标签（Get all tags）
     */
    public String[] getTags() {
        return tags != null ? tags.clone() : new String[0];
    }

    /**
     * 设置所有标签（Set all tags）
     */
    public void setTags(String[] tags) {
        this.tags = tags != null ? tags.clone() : null;
    }

    /**
     * 获取关键词（Get keywords）
     */
    public String[] getKeywords() {
        return keywords != null ? keywords.clone() : new String[0];
    }

    /**
     * 设置关键词（Set keywords）
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords != null ? keywords.clone() : null;
    }

    /**
     * 增加访问次数（Increment access count）
     */
    public void incrementAccessCount() {
        if (accessCount == null) {
            accessCount = 0;
        }
        accessCount++;
        lastAccessedAt = Instant.now();
    }

    /**
     * 更新索引时间（Update index time）
     */
    public void updateIndexedAt() {
        indexedAt = Instant.now();
        indexed = true;
    }

    /**
     * 更新修改时间（Update modification time）
     */
    public void updateModifiedAt() {
        updatedAt = Instant.now();
    }

    /**
     * 创建新文档（Create new document）
     */
    public static Document create(String title, String content) {
        return Document.builder()
                .title(title)
                .content(content)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .status(DocumentStatus.DRAFT)
                .indexed(false)
                .accessCount(0)
                .build();
    }

    /**
     * 从文件创建文档（Create document from file）
     */
    public static Document fromFile(String filePath, String content) {
        return Document.builder()
                .title(new java.io.File(filePath).getName())
                .content(content)
                .filePath(filePath)
                .fileSize(new java.io.File(filePath).length())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .status(DocumentStatus.DRAFT)
                .indexed(false)
                .accessCount(0)
                .build();
    }
}
