package top.yumbo.ai.rag.conflict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 冲突案例 (Conflict Case)
 *
 * 记录检测到的知识冲突
 * (Records detected knowledge conflicts)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictCase {

    /**
     * 冲突ID (Conflict ID)
     */
    private String conflictId;

    /**
     * 冲突概念列表 (Conflicting concept IDs)
     */
    @Builder.Default
    private List<String> conceptIds = new ArrayList<>();

    /**
     * 冲突类型 (Conflict type)
     */
    private ConflictType conflictType;

    /**
     * 严重程度 (Severity, 0-1)
     */
    private double severity;

    /**
     * 状态 (Status)
     */
    @Builder.Default
    private ConflictStatus status = ConflictStatus.PENDING;

    /**
     * 冲突评分 (Conflict score)
     */
    private ConflictScore score;

    /**
     * 冲突描述 (Description)
     */
    private String description;

    /**
     * 检测时间 (Detection time)
     */
    @Builder.Default
    private Date createTime = new Date();

    /**
     * 解决时间 (Resolution time)
     */
    private Date resolvedTime;

    /**
     * 解决方案 (Resolution)
     */
    private String resolution;

    /**
     * 是否高严重度 (Is high severity)
     *
     * @return 是否严重 (Whether severe)
     */
    public boolean isHighSeverity() {
        return severity >= 0.7;
    }

    /**
     * 是否待处理 (Is pending)
     *
     * @return 是否待处理 (Whether pending)
     */
    public boolean isPending() {
        return status == ConflictStatus.PENDING;
    }

    /**
     * 是否已解决 (Is resolved)
     *
     * @return 是否已解决 (Whether resolved)
     */
    public boolean isResolved() {
        return status == ConflictStatus.RESOLVED;
    }

    /**
     * 添加概念ID (Add concept ID)
     *
     * @param conceptId 概念ID (Concept ID)
     */
    public void addConceptId(String conceptId) {
        if (conceptIds == null) {
            conceptIds = new ArrayList<>();
        }
        if (!conceptIds.contains(conceptId)) {
            conceptIds.add(conceptId);
        }
    }

    /**
     * 冲突状态 (Conflict Status)
     */
    public enum ConflictStatus {
        /**
         * 待审核 (Pending review)
         */
        PENDING,

        /**
         * 审核中 (Under review)
         */
        REVIEWING,

        /**
         * 已解决 (Resolved)
         */
        RESOLVED,

        /**
         * 已忽略 (Ignored)
         */
        IGNORED
    }
}

