package top.yumbo.ai.rag.evolution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 概念版本 (Concept Version)
 *
 * 记录概念的版本信息
 * (Records concept version information)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptVersion {

    /**
     * 版本ID (Version ID)
     */
    private String versionId;

    /**
     * 概念ID (Concept ID)
     */
    private String conceptId;

    /**
     * 版本号 (Version number)
     */
    private String version;

    /**
     * 版本内容 (Version content)
     */
    private String content;

    /**
     * 变更说明 (Changes description)
     */
    private String changes;

    /**
     * 作者 (Author)
     */
    private String author;

    /**
     * 审批者 (Approver)
     */
    private String approver;

    /**
     * 创建时间 (Create time)
     */
    @Builder.Default
    private Date createTime = new Date();

    /**
     * 状态 (Status)
     */
    @Builder.Default
    private VersionStatus status = VersionStatus.DRAFT;

    /**
     * 父版本ID (Parent version ID)
     */
    private String parentVersion;

    /**
     * 元数据 (Metadata)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 是否草稿 (Is draft)
     *
     * @return 是否草稿 (Whether draft)
     */
    public boolean isDraft() {
        return status == VersionStatus.DRAFT;
    }

    /**
     * 是否已发布 (Is published)
     *
     * @return 是否已发布 (Whether published)
     */
    public boolean isPublished() {
        return status == VersionStatus.PUBLISHED;
    }

    /**
     * 是否已废弃 (Is deprecated)
     *
     * @return 是否已废弃 (Whether deprecated)
     */
    public boolean isDeprecated() {
        return status == VersionStatus.DEPRECATED;
    }

    /**
     * 添加元数据 (Add metadata)
     *
     * @param key 键 (Key)
     * @param value 值 (Value)
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * 版本状态 (Version Status)
     */
    public enum VersionStatus {
        /**
         * 草稿 (Draft)
         */
        DRAFT,

        /**
         * 审核中 (Reviewing)
         */
        REVIEWING,

        /**
         * 已发布 (Published)
         */
        PUBLISHED,

        /**
         * 已废弃 (Deprecated)
         */
        DEPRECATED
    }
}

