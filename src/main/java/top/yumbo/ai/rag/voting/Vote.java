package top.yumbo.ai.rag.voting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 投票 (Vote)
 *
 * 记录单次投票信息
 * (Records a single vote)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

    /**
     * 投票ID (Vote ID)
     */
    private String voteId;

    /**
     * 会话ID (Session ID)
     */
    private String sessionId;

    /**
     * 投票者ID (Voter ID)
     */
    private String voterId;

    /**
     * 投票者类型 (Voter type)
     */
    private VoterType voterType;

    /**
     * 候选ID (Candidate ID)
     */
    private String candidateId;

    /**
     * 评分 (Score, 0-10)
     */
    private double score;

    /**
     * 投票理由 (Reason)
     */
    private String reason;

    /**
     * 权重 (Weight)
     */
    private double weight;

    /**
     * 投票时间 (Timestamp)
     */
    @Builder.Default
    private Date timestamp = new Date();

    /**
     * 获取加权分数 (Get weighted score)
     *
     * @return 加权分数 (Weighted score)
     */
    public double getWeightedScore() {
        return score * weight;
    }

    /**
     * 是否高分 (Is high score)
     *
     * @return 是否高分 (Whether high score)
     */
    public boolean isHighScore() {
        return score >= 7.0;
    }
}

