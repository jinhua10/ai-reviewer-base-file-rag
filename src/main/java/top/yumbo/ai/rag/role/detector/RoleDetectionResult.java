package top.yumbo.ai.rag.role.detector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.role.Role;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 角色检测结果 (Role Detection Result)
 *
 * 包含完整的角色检测过程和结果
 * (Contains complete role detection process and result)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetectionResult {

    /**
     * 原始问题 (Original question)
     */
    private String question;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 选中的角色 (Selected role)
     */
    private Role selectedRole;

    /**
     * 置信度 (Confidence)
     * 范围: 0.0 - 1.0
     */
    private double confidence;

    /**
     * 最终得分 (Final score)
     */
    private double finalScore;

    /**
     * 所有候选角色 (All candidates)
     */
    @Builder.Default
    private List<RoleMatchResult> allCandidates = new ArrayList<>();

    /**
     * 使用的检测方法 (Detection methods used)
     */
    @Builder.Default
    private List<String> detectionMethods = new ArrayList<>();

    /**
     * 各方法的结果详情 (Method results details)
     */
    @Builder.Default
    private List<RoleMatchResult> methodResults = new ArrayList<>();

    /**
     * 检测时间戳 (Detection timestamp)
     */
    private Date timestamp;

    /**
     * 是否使用默认角色 (Is default role)
     */
    private boolean isDefault;

    /**
     * 是否是高置信度检测 (Is high confidence detection)
     *
     * @return 是否高置信度 (Whether high confidence)
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * 是否是中等置信度检测 (Is medium confidence detection)
     *
     * @return 是否中等置信度 (Whether medium confidence)
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * 是否是低置信度检测 (Is low confidence detection)
     *
     * @return 是否低置信度 (Whether low confidence)
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * 获取置信度等级 (Get confidence level)
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

    /**
     * 获取第二候选角色 (Get second candidate role)
     *
     * @return 第二候选 (Second candidate)
     */
    public RoleMatchResult getSecondCandidate() {
        if (allCandidates.size() >= 2) {
            return allCandidates.get(1);
        }
        return null;
    }

    /**
     * 创建默认结果 (Create default result)
     *
     * @param defaultRole 默认角色 (Default role)
     * @return 默认检测结果 (Default detection result)
     */
    public static RoleDetectionResult createDefault(Role defaultRole) {
        return RoleDetectionResult.builder()
                .selectedRole(defaultRole)
                .confidence(1.0)
                .finalScore(10.0)
                .detectionMethods(List.of("default"))
                .timestamp(new Date())
                .isDefault(true)
                .build();
    }

    /**
     * 转换为简单描述 (Convert to simple description)
     *
     * @return 描述文本 (Description text)
     */
    public String toDescription() {
        if (isDefault) {
            return String.format("使用默认角色: %s (Using default role: %s)",
                    selectedRole.getName(), selectedRole.getName());
        }

        return String.format("检测到角色: %s, 置信度: %.2f (%s) (Detected role: %s, confidence: %.2f (%s))",
                selectedRole.getName(), confidence, getConfidenceLevel(),
                selectedRole.getName(), confidence, getConfidenceLevel());
    }
}

