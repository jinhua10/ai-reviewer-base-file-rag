package top.yumbo.ai.rag.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 角色实体类 (Role Entity)
 *
 * 表示系统中的一个角色，包含角色的基本信息和配置
 * (Represents a role in the system, including basic information and configuration)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /**
     * 角色唯一标识 (Role unique identifier)
     */
    private String id;

    /**
     * 角色名称 (Role name)
     */
    private String name;

    /**
     * 角色描述 (Role description)
     */
    private String description;

    /**
     * 角色关键词列表 (Role keywords list)
     * 用于关键词匹配检测 (Used for keyword matching detection)
     */
    private Set<String> keywords;

    /**
     * 角色权重 (Role weight)
     * 用于多角色检索时的结果加权 (Used for result weighting in multi-role retrieval)
     */
    private double weight;

    /**
     * 是否启用 (Whether enabled)
     */
    private boolean enabled;

    /**
     * 角色专属提示词 (Role-specific prompt)
     * 用于 LLM 角色识别 (Used for LLM role identification)
     */
    private String prompt;

    /**
     * 角色标签 (Role tags)
     * 用于分类和过滤 (Used for classification and filtering)
     */
    private List<String> tags;

    /**
     * 索引路径 (Index path)
     * 该角色对应的向量索引存储路径 (Vector index storage path for this role)
     */
    private String indexPath;

    /**
     * 优先级 (Priority)
     * 数值越大优先级越高 (Higher value means higher priority)
     */
    private int priority;
}

