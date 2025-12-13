package top.yumbo.ai.rag.evolution.concept;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 角色知识库 (Role-based Knowledge Repository)
 * <p>
 * 针对不同角色维护的专属知识视图
 * (Dedicated knowledge view maintained for different roles)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleKnowledgeBase {

    /**
     * 角色名称 (Role name)
     */
    private String roleName;

    /**
     * 角色描述 (Role description)
     */
    private String roleDescription;

    /**
     * 概念ID列表 (List of concept IDs)
     */
    private List<String> conceptIds;

    /**
     * 概念权重映射 (Concept weight mapping: conceptId -> weight)
     */
    private Map<String, Double> conceptWeights;

    /**
     * 关注的概念类型 (Focused concept types)
     */
    private List<MinimalConcept.ConceptType> focusedTypes;

    /**
     * 优先标签 (Priority tags)
     */
    private List<String> priorityTags;

    /**
     * 知识统计 (Knowledge statistics)
     */
    private KnowledgeStats stats;

    /**
     * 创建时间 (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (Last update time)
     */
    private LocalDateTime updatedAt;

    /**
     * 知识统计内部类 (Knowledge statistics inner class)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeStats {
        /**
         * 总概念数 (Total concepts)
         */
        private Integer totalConcepts;

        /**
         * 高置信度概念数 (High confidence concepts)
         */
        private Integer highConfidenceConcepts;

        /**
         * 最近更新数 (Recent updates)
         */
        private Integer recentUpdates;

        /**
         * 平均置信度 (Average confidence)
         */
        private Double averageConfidence;

        /**
         * 按类型分组的统计 (Statistics by type)
         */
        private Map<MinimalConcept.ConceptType, Integer> countsByType;

        /**
         * 按层级分组的统计 (Statistics by layer)
         */
        private Map<MinimalConcept.HOPELayer, Integer> countsByLayer;
    }

    /**
     * 预定义角色类型 (Predefined role types)
     */
    @Getter
    public enum RoleType {
        GENERAL("general", "通用角色", "General Role", 0.3, true),  // 广度优先，理解浅，负责转发
        DEVELOPER("developer", "开发者", "Developer", 0.9, false),
        DEVOPS("devops", "运维工程师", "DevOps Engineer", 0.9, false),
        ARCHITECT("architect", "架构师", "Architect", 0.9, false),
        RESEARCHER("researcher", "研究员", "Researcher", 0.9, false),
        PRODUCT_MANAGER("product_manager", "产品经理", "Product Manager", 0.9, false),
        DATA_SCIENTIST("data_scientist", "数据科学家", "Data Scientist", 0.9, false),
        SECURITY_ENGINEER("security_engineer", "安全工程师", "Security Engineer", 0.9, false),
        TESTER("tester", "测试工程师", "Test Engineer", 0.9, false);

        private final String code;
        private final String zhName;
        private final String enName;
        private final double expertiseLevel;  // 专业度：0.3=通用浅层理解，0.9=专业深度理解
        private final boolean isGeneralRole;  // 是否为通用角色（负责转发）

        RoleType(String code, String zhName, String enName, double expertiseLevel, boolean isGeneralRole) {
            this.code = code;
            this.zhName = zhName;
            this.enName = enName;
            this.expertiseLevel = expertiseLevel;
            this.isGeneralRole = isGeneralRole;
        }

        /**
         * 根据代码获取角色类型 (Get role type by code)
         */
        public static RoleType fromCode(String code) {
            for (RoleType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
}

