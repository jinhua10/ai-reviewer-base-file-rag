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
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTemplate {

    /**
     * 模板唯一标识
     * (Template unique ID)
     */
    private String id;

    /**
     * 技能名称
     * (Skill name)
     */
    private String name;

    /**
     * 问题匹配模式（正则表达式）
     * (Question matching pattern - regex)
     */
    private String pattern;

    /**
     * 问题关键词列表
     * (Question keyword list)
     */
    private String[] keywords;

    /**
     * Prompt 模板
     * (Prompt template)
     *
     * 支持变量: {question}, {context}, {language}, {code} 等
     * (Supports variables: {question}, {context}, {language}, {code}, etc.)
     */
    private String promptTemplate;

    /**
     * 置信度 (0-1)
     * (Confidence score 0-1)
     */
    private double confidence;

    /**
     * 使用次数
     * (Usage count)
     */
    private long usageCount;

    /**
     * 成功次数（用户正面反馈）
     * (Success count - positive user feedback)
     */
    private long successCount;

    /**
     * 最后使用时间
     * (Last used time)
     */
    private LocalDateTime lastUsed;

    /**
     * 创建时间
     * (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 是否启用
     * (Whether enabled)
     */
    private boolean enabled;

    /**
     * 获取成功率
     * (Get success rate)
     * 
     * @return 成功率分数 (0-1) (Success rate score (0-1))
     */
    public double getSuccessRate() {
        if (usageCount == 0) {
            return 0.0;
        }
        return (double) successCount / usageCount;
    }

    /**
     * 记录一次使用
     * (Record a usage)
     * 
     * @param success 是否成功 (Whether successful)
     */
    public void recordUsage(boolean success) {
        this.usageCount++;
        if (success) {
            this.successCount++;
        }
        this.lastUsed = LocalDateTime.now();
    }
}

