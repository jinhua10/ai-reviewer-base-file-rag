package top.yumbo.ai.rag.evolution.concept;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应竞争 (Role Response Competition)
 * <p>
 * 实现"举手抢答"机制，让多个角色对同一问题进行竞争响应
 * (Implements "raise hand to answer" mechanism, allowing multiple roles to compete for responding to the same question)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseBid {

    /**
     * 竞标ID (Bid ID)
     */
    private String id;

    /**
     * 问题 (Question)
     */
    private String question;

    /**
     * 角色名称 (Role name)
     */
    private String roleName;

    /**
     * 是否为通用角色 (Is general role)
     */
    private boolean isGeneralRole;

    /**
     * 置信度分数 (Confidence score: 0.0-1.0)
     * 角色对这个问题的把握程度
     */
    private Double confidenceScore;

    /**
     * 专业度分数 (Expertise score: 0.0-1.0)
     * 角色在相关领域的专业程度
     */
    private Double expertiseScore;

    /**
     * 相关概念数量 (Number of related concepts)
     * 角色知识库中相关概念的数量
     */
    private Integer relatedConceptCount;

    /**
     * 推荐的专业角色（仅通用角色）(Recommended expert roles - only for general role)
     * 通用角色识别出最适合回答的专业角色
     */
    private List<String> recommendedExpertRoles;

    /**
     * 响应时间 (Response time)
     */
    private LocalDateTime responseTime;

    /**
     * 综合得分 (Overall score)
     * 用于排序和选择最佳响应角色
     */
    private Double overallScore;

    /**
     * 响应理由 (Response reason)
     */
    private String reason;

    /**
     * 响应类型 (Response type)
     */
    private ResponseType responseType;

    /**
     * 响应类型枚举 (Response type enum)
     */
    @Getter
    public enum ResponseType {
        DIRECT_ANSWER("直接回答", "Direct Answer"),           // 专业角色：我能直接回答
        FORWARD("转发", "Forward"),                           // 通用角色：我知道谁能回答
        COLLABORATE("协作", "Collaborate"),                   // 需要多个角色协作
        UNCERTAIN("不确定", "Uncertain");                     // 不确定能否回答

        private final String zhName;
        private final String enName;

        ResponseType(String zhName, String enName) {
            this.zhName = zhName;
            this.enName = enName;
        }

    }

    /**
     * 计算综合得分 (Calculate overall score)
     * <p>
     * 专业角色：置信度 × 专业度 × 相关概念权重
     * 通用角色：置信度 × 0.5（降低权重，优先专业角色）
     */
    public void calculateOverallScore() {
        if (isGeneralRole) {
            // 通用角色得分较低，除非没有专业角色响应
            this.overallScore = confidenceScore * 0.5;
        } else {
            // 专业角色得分：综合考虑置信度、专业度和相关概念数
            double conceptBonus = Math.min(relatedConceptCount / 10.0, 0.2);
            this.overallScore = (confidenceScore * 0.6) + (expertiseScore * 0.3) + conceptBonus;
        }
    }
}

