package top.yumbo.ai.rag.evolution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 概念演化记录 (Concept Evolution Record)
 *
 * 记录概念定义随时间的变化历史
 * (Records the history of concept definition changes over time)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptEvolution {

    /**
     * 演化记录ID (Evolution record ID)
     */
    private String id;

    /**
     * 概念ID (Concept ID)
     */
    private String conceptId;

    /**
     * 版本号 (Version number)
     */
    private Integer version;

    /**
     * 演化类型 (Evolution type)
     */
    private EvolutionType type;

    /**
     * 标题 (Title)
     */
    private String title;

    /**
     * 描述 (Description)
     */
    private String description;

    /**
     * 当前内容 (Current content)
     */
    private String content;

    /**
     * 变更信息 (Changes information)
     */
    private Map<String, String> changes;

    /**
     * 作者 (Author)
     */
    private String author;

    /**
     * 时间戳 (Timestamp)
     */
    private LocalDateTime timestamp;

    /**
     * 原因/备注 (Reason/notes)
     */
    private String reason;

    /**
     * 相关的冲突ID (Related conflict ID)
     */
    private String relatedConflictId;

    /**
     * 置信度分数 (Confidence score)
     */
    private Double confidence;

    /**
     * 演化类型枚举 (Evolution type enum)
     */
    public enum EvolutionType {
        CREATED("创建", "created"),
        UPDATED("更新", "updated"),
        MERGED("合并", "merged"),
        RESOLVED("解决冲突", "resolved"),
        DEPRECATED("废弃", "deprecated"),
        RESTORED("恢复", "restored");

        private final String zhName;
        private final String enName;

        EvolutionType(String zhName, String enName) {
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

