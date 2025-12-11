package top.yumbo.ai.rag.role.detector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色匹配结果 (Role Match Result)
 *
 * 表示角色检测的匹配结果
 * (Represents the result of role detection)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMatchResult {

    /**
     * 角色ID (Role ID)
     */
    private String roleId;

    /**
     * 角色名称 (Role name)
     */
    private String roleName;

    /**
     * 匹配分数 (Match score)
     * 分数越高，匹配度越高 (Higher score means better match)
     */
    private double score;

    /**
     * 置信度 (Confidence)
     * 范围: 0.0 - 1.0
     */
    private double confidence;

    /**
     * 检测方法 (Detection method)
     * 可选值: keyword, ai, preference, hybrid
     */
    private String method;

    /**
     * 匹配的关键词列表 (Matched keywords)
     */
    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    /**
     * 原因说明 (Reason)
     */
    private String reason;

    /**
     * 额外信息 (Extra info)
     */
    private String extraInfo;

    /**
     * 是否是高置信度匹配 (Is high confidence match)
     *
     * @return 是否高置信度 (Whether high confidence)
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * 是否是中等置信度匹配 (Is medium confidence match)
     *
     * @return 是否中等置信度 (Whether medium confidence)
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * 是否是低置信度匹配 (Is low confidence match)
     *
     * @return 是否低置信度 (Whether low confidence)
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * 获取置信度等级描述 (Get confidence level description)
     *
     * @return 置信度等级 (Confidence level)
     */
    public String getConfidenceLevel() {
        if (isHighConfidence()) {
            return "HIGH";
        } else if (isMediumConfidence()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}

