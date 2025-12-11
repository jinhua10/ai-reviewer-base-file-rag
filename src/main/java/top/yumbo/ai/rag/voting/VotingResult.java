package top.yumbo.ai.rag.voting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 投票结果 (Voting Result)
 *
 * 记录投票的最终结果
 * (Records the final result of voting)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingResult {

    /**
     * 获胜候选ID (Winner candidate ID)
     */
    private String winnerId;

    /**
     * 总投票数 (Total votes)
     */
    private int totalVotes;

    /**
     * 加权分数映射 (Weighted scores by candidate)
     * Key: candidateId, Value: weighted score
     */
    @Builder.Default
    private Map<String, Double> weightedScores = new HashMap<>();

    /**
     * 结果置信度 (Result confidence, 0-1)
     */
    private double confidence;

    /**
     * 领先优势 (Margin)
     * 第一名与第二名的分数差距
     */
    private double margin;

    /**
     * 共识度 (Consensus, 0-1)
     * 投票的一致性程度
     */
    private double consensus;

    /**
     * 是否有明确赢家 (Has clear winner)
     *
     * @return 是否明确 (Whether clear)
     */
    public boolean hasClearWinner() {
        return confidence >= 0.7 && margin >= 0.2;
    }

    /**
     * 是否高共识 (Is high consensus)
     *
     * @return 是否高共识 (Whether high consensus)
     */
    public boolean isHighConsensus() {
        return consensus >= 0.7;
    }

    /**
     * 获取候选分数 (Get candidate score)
     *
     * @param candidateId 候选ID (Candidate ID)
     * @return 分数 (Score)
     */
    public double getCandidateScore(String candidateId) {
        return weightedScores.getOrDefault(candidateId, 0.0);
    }

    /**
     * 添加候选分数 (Add candidate score)
     *
     * @param candidateId 候选ID (Candidate ID)
     * @param score 分数 (Score)
     */
    public void addCandidateScore(String candidateId, double score) {
        if (weightedScores == null) {
            weightedScores = new HashMap<>();
        }
        weightedScores.put(candidateId, score);
    }
}

