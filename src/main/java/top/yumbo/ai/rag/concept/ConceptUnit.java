package top.yumbo.ai.rag.concept;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 概念单元 (Concept Unit)
 *
 * 表示系统中的一个独立、完整的语义最小单位
 * (Represents an independent, complete minimum semantic unit in the system)
 *
 * 特征 (Characteristics):
 * - 自包含：脱离上下文仍可理解 (Self-contained: understandable without context)
 * - 完整性：包含概念的核心要素 (Completeness: contains core elements)
 * - 原子性：不可再分割而不失去意义 (Atomicity: indivisible without losing meaning)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptUnit {

    // ==================== 基本信息 (Basic Information) ====================

    /**
     * 唯一标识 (Unique identifier)
     */
    private String id;

    /**
     * 概念名称 (Concept name)
     */
    private String name;

    /**
     * 概念类型 (Concept type)
     */
    @Builder.Default
    private ConceptType type = ConceptType.GENERAL;

    /**
     * 层级深度 (Hierarchy level)
     * 0=文档 (Document), 1=章节 (Section), 2=概念 (Concept), 3=子概念 (Sub-concept)...
     */
    private int level;

    /**
     * 所属角色ID (Role ID)
     */
    private String roleId;

    // ==================== 语义信息 (Semantic Information) ====================

    /**
     * 核心定义 (Core definition)
     */
    private String definition;

    /**
     * 详细描述 (Detailed description)
     */
    private String description;

    /**
     * 关键词列表 (Keywords list)
     */
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    /**
     * 示例列表 (Examples list)
     */
    @Builder.Default
    private List<String> examples = new ArrayList<>();

    // ==================== 层次关系 (Hierarchical Relations) ====================

    /**
     * 父概念ID (Parent concept ID)
     */
    private String parentId;

    /**
     * 子概念ID列表 (Child concept IDs)
     */
    @Builder.Default
    private List<String> childIds = new ArrayList<>();

    /**
     * 相关概念ID列表 (Related concept IDs)
     */
    @Builder.Default
    private List<String> relatedIds = new ArrayList<>();

    // ==================== 向量表示 (Vector Representation) ====================

    /**
     * 语义向量 (Semantic embedding)
     */
    private float[] embedding;

    /**
     * 向量维度 (Embedding dimension)
     */
    private int embeddingDimension;

    // ==================== 版本管理 (Version Management) ====================

    /**
     * 当前版本号 (Current version number)
     */
    @Builder.Default
    private int version = 1;

    /**
     * 是否有历史版本 (Has version history)
     */
    @Builder.Default
    private boolean hasHistory = false;

    /**
     * 创建时间 (Creation time)
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 最后更新时间 (Last update time)
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 创建者 (Creator)
     */
    private String createdBy;

    /**
     * 最后修改者 (Last modifier)
     */
    private String updatedBy;

    // ==================== 质量指标 (Quality Metrics) ====================

    /**
     * 重要性评分 (Importance score)
     * 范围: 0.0 - 1.0
     */
    @Builder.Default
    private double importance = 0.5;

    /**
     * 健康度评分 (Health score)
     * 范围: 0.0 - 1.0
     */
    @Builder.Default
    private double healthScore = 1.0;

    /**
     * 争议次数 (Dispute count)
     */
    @Builder.Default
    private int disputeCount = 0;

    /**
     * 访问次数 (Access count)
     */
    @Builder.Default
    private long accessCount = 0L;

    /**
     * 最后访问时间 (Last access time)
     */
    private LocalDateTime lastAccessedAt;

    // ==================== 状态标记 (Status Flags) ====================

    /**
     * 是否启用 (Whether enabled)
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 是否需要审核 (Needs review)
     */
    @Builder.Default
    private boolean needsReview = false;

    /**
     * 是否正在投票中 (In voting)
     */
    @Builder.Default
    private boolean inVoting = false;

    /**
     * 当前投票会话ID (Current voting session ID)
     */
    private String currentVotingSessionId;

    // ==================== 元数据 (Metadata) ====================

    /**
     * 元数据映射 (Metadata map)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 源文档ID (Source document ID)
     */
    private String sourceDocumentId;

    /**
     * 源文档路径 (Source document path)
     */
    private String sourceDocumentPath;

    // ==================== 业务方法 (Business Methods) ====================

    /**
     * 增加访问次数 (Increment access count)
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 增加争议次数 (Increment dispute count)
     */
    public void incrementDisputeCount() {
        this.disputeCount++;
        this.needsReview = true;
        updateHealthScore();
    }

    /**
     * 更新健康度评分 (Update health score)
     * 基于争议次数和其他因素计算 (Calculate based on dispute count and other factors)
     */
    public void updateHealthScore() {
        // 简单的健康度计算公式 (Simple health score calculation)
        // 健康度 = 1.0 - (争议次数 * 0.1)，最低为 0
        this.healthScore = Math.max(0.0, 1.0 - (this.disputeCount * 0.1));
    }

    /**
     * 创建新版本 (Create new version)
     */
    public void createNewVersion() {
        this.version++;
        this.hasHistory = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 是否需要演化 (Needs evolution)
     * 当健康度低于阈值或争议次数过多时 (When health score is below threshold or too many disputes)
     */
    public boolean needsEvolution() {
        return this.healthScore < 0.7 || this.disputeCount >= 3;
    }

    /**
     * 检查是否是叶子节点 (Check if is leaf node)
     */
    public boolean isLeaf() {
        return this.childIds == null || this.childIds.isEmpty();
    }

    /**
     * 检查是否是根节点 (Check if is root node)
     */
    public boolean isRoot() {
        return this.parentId == null || this.parentId.isEmpty();
    }
}

