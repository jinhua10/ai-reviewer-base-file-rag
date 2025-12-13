package top.yumbo.ai.rag.evolution.concept;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 最小概念单元 (Minimal Concept Unit)
 *
 * 从 HOPE 架构中提取的最小知识单元，支持角色分类
 * (Minimal knowledge unit extracted from HOPE architecture, supporting role classification)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinimalConcept {

    /**
     * 概念ID (Concept ID)
     */
    private String id;

    /**
     * 概念名称 (Concept name)
     */
    private String name;

    /**
     * 概念描述 (Concept description)
     */
    private String description;

    /**
     * 概念类型 (Concept type)
     */
    private ConceptType type;

    /**
     * 关联角色列表 (Associated roles)
     */
    private List<String> roles;

    /**
     * 置信度 (Confidence score: 0.0-1.0)
     */
    private Double confidence;

    /**
     * 来源层级 (Source layer from HOPE)
     */
    private HOPELayer sourceLayer;

    /**
     * 来源文档 (Source document)
     */
    private String sourceDocument;

    /**
     * 关系映射 (Relations to other concepts)
     */
    private Map<String, List<String>> relations;

    /**
     * 标签 (Tags for categorization)
     */
    private List<String> tags;

    /**
     * 访问次数 (Access count)
     */
    private Integer accessCount;

    /**
     * 创建时间 (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (Last update time)
     */
    private LocalDateTime updatedAt;

    /**
     * 版本号 (Version number)
     */
    private Integer version;

    /**
     * 元数据 (Metadata)
     */
    private Map<String, Object> metadata;

    /**
     * 概念类型枚举 (Concept type enum)
     */
    public enum ConceptType {
        DEFINITION("定义", "Definition"),
        PROCESS("流程", "Process"),
        SKILL("技能", "Skill"),
        FACT("事实", "Fact"),
        RELATIONSHIP("关系", "Relationship"),
        RULE("规则", "Rule");

        private final String zhName;
        private final String enName;

        ConceptType(String zhName, String enName) {
            this.zhName = zhName;
            this.enName = enName;
        }

        public String getZhName() {
            return zhName;
        }

        public String getEnName() {
            return enName;
        }
    }

    /**
     * HOPE 层级枚举 (HOPE layer enum)
     */
    public enum HOPELayer {
        PERMANENT("低频层", "Permanent Layer"),
        ORDINARY("中频层", "Ordinary Layer"),
        HIGH_FREQUENCY("高频层", "High Frequency Layer"),
        UNKNOWN("未知", "Unknown");

        private final String zhName;
        private final String enName;

        HOPELayer(String zhName, String enName) {
            this.zhName = zhName;
            this.enName = enName;
        }

        public String getZhName() {
            return zhName;
        }

        public String getEnName() {
            return enName;
        }
    }
}

