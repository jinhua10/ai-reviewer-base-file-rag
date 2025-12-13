package top.yumbo.ai.rag.evolution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 概念冲突模型 (Concept Conflict Model)
 *
 * 记录知识库中出现的概念定义冲突
 * (Records concept definition conflicts in the knowledge base)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptConflict {

    /**
     * 冲突ID (Conflict ID)
     */
    private String id;

    /**
     * 相关问题 (Related question)
     */
    private String question;

    /**
     * 概念A的定义 (Definition of concept A)
     */
    private String conceptA;

    /**
     * 概念B的定义 (Definition of concept B)
     */
    private String conceptB;

    /**
     * 概念A的来源文档 (Source document of concept A)
     */
    private String sourceA;

    /**
     * 概念B的来源文档 (Source document of concept B)
     */
    private String sourceB;

    /**
     * 冲突状态 (Conflict status)
     */
    private ConflictStatus status;

    /**
     * 投票统计 (Vote statistics)
     */
    @Builder.Default
    private Map<String, Integer> votes = new HashMap<>();

    /**
     * 已解决的选择 (Resolved choice: A or B)
     */
    private String resolvedChoice;

    /**
     * 创建时间 (Creation time)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (Update time)
     */
    private LocalDateTime updatedAt;

    /**
     * 解决时间 (Resolution time)
     */
    private LocalDateTime resolvedAt;

    /**
     * 冲突置信度分数 (0-1) (Conflict confidence score)
     */
    private Double confidenceScore;

    /**
     * 冲突类型 (Conflict type)
     */
    private ConflictType type;

    /**
     * 冲突状态枚举 (Conflict status enum)
     */
    public enum ConflictStatus {
        PENDING("待投票", "pending"),
        VOTING("投票中", "voting"),
        RESOLVED("已解决", "resolved"),
        DISMISSED("已忽略", "dismissed");

        private final String zhName;
        private final String enName;

        ConflictStatus(String zhName, String enName) {
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
     * 冲突类型枚举 (Conflict type enum)
     */
    public enum ConflictType {
        DEFINITION_MISMATCH("定义不一致", "Definition mismatch"),
        OUTDATED_INFO("信息过时", "Outdated information"),
        CONTRADICTORY("矛盾冲突", "Contradictory"),
        INCOMPLETE("信息不完整", "Incomplete");

        private final String zhName;
        private final String enName;

        ConflictType(String zhName, String enName) {
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
     * 获取投票总数 (Get total votes)
     */
    public int getTotalVotes() {
        return votes.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 获取选项A的投票数 (Get votes for option A)
     */
    public int getVotesForA() {
        return votes.getOrDefault("A", 0);
    }

    /**
     * 获取选项B的投票数 (Get votes for option B)
     */
    public int getVotesForB() {
        return votes.getOrDefault("B", 0);
    }

    /**
     * 添加投票 (Add vote)
     */
    public void addVote(String choice) {
        votes.put(choice, votes.getOrDefault(choice, 0) + 1);
        this.updatedAt = LocalDateTime.now();
    }
}

