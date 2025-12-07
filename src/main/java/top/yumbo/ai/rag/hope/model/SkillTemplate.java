package top.yumbo.ai.rag.hope.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 技能模板 - 存储在低频层的可复用处理模式
 * (Skill Template - Reusable processing patterns stored in permanent layer)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTemplate {

    /**
     * 模板唯一标识
     */
    private String id;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 问题匹配模式（正则表达式）
     */
    private String pattern;

    /**
     * 问题关键词列表
     */
    private String[] keywords;

    /**
     * Prompt 模板
     * 支持变量: {question}, {context}, {language}, {code} 等
     */
    private String promptTemplate;

    /**
     * 置信度 (0-1)
     */
    private double confidence;

    /**
     * 使用次数
     */
    private long usageCount;

    /**
     * 成功次数（用户正面反馈）
     */
    private long successCount;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsed;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (usageCount == 0) {
            return 0.0;
        }
        return (double) successCount / usageCount;
    }

    /**
     * 记录一次使用
     */
    public void recordUsage(boolean success) {
        this.usageCount++;
        if (success) {
            this.successCount++;
        }
        this.lastUsed = LocalDateTime.now();
    }
}

