package top.yumbo.ai.rag.evolution.concept;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 角色知识库 (Role-based Knowledge Repository)
 *
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
    public enum RoleType {
        DEVELOPER("developer", "开发者", "Developer"),
        DEVOPS("devops", "运维工程师", "DevOps Engineer"),
        ARCHITECT("architect", "架构师", "Architect"),
        RESEARCHER("researcher", "研究员", "Researcher"),
        PRODUCT_MANAGER("product_manager", "产品经理", "Product Manager"),
        DATA_SCIENTIST("data_scientist", "数据科学家", "Data Scientist"),
        SECURITY_ENGINEER("security_engineer", "安全工程师", "Security Engineer"),
        TESTER("tester", "测试工程师", "Test Engineer");

        private final String code;
        private final String zhName;
        private final String enName;

        RoleType(String code, String zhName, String enName) {
            this.code = code;
            this.zhName = zhName;
            this.enName = enName;
        }

        public String getCode() {
            return code;
        }

        public String getZhName() {
            return zhName;
        }

        public String getEnName() {
            return enName;
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

