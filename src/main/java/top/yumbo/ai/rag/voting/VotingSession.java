package top.yumbo.ai.rag.voting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 投票会话 (Voting Session)
 *
 * 管理一次投票过程
 * (Manages a voting process)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingSession {

    /**
     * 会话ID (Session ID)
     */
    private String sessionId;

    /**
     * 关联的冲突ID (Associated conflict ID)
     */
    private String conflictId;

    /**
     * 候选概念列表 (Candidate concept IDs)
     */
    @Builder.Default
    private List<String> candidates = new ArrayList<>();

    /**
     * 投票者列表 (Voter IDs)
     */
    @Builder.Default
    private List<String> voters = new ArrayList<>();

    /**
     * 投票记录 (Vote records)
     */
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    /**
     * 状态 (Status)
     */
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    /**
     * 开始时间 (Start time)
     */
    @Builder.Default
    private Date startTime = new Date();

    /**
     * 结束时间 (End time)
     */
    private Date endTime;

    /**
     * 投票结果 (Voting result)
     */
    private VotingResult result;

    /**
     * 添加候选 (Add candidate)
     *
     * @param candidateId 候选ID (Candidate ID)
     */
    public void addCandidate(String candidateId) {
        if (candidates == null) {
            candidates = new ArrayList<>();
        }
        if (!candidates.contains(candidateId)) {
            candidates.add(candidateId);
        }
    }

    /**
     * 添加投票者 (Add voter)
     *
     * @param voterId 投票者ID (Voter ID)
     */
    public void addVoter(String voterId) {
        if (voters == null) {
            voters = new ArrayList<>();
        }
        if (!voters.contains(voterId)) {
            voters.add(voterId);
        }
    }

    /**
     * 添加投票 (Add vote)
     *
     * @param vote 投票 (Vote)
     */
    public void addVote(Vote vote) {
        if (votes == null) {
            votes = new ArrayList<>();
        }
        votes.add(vote);
    }

    /**
     * 是否激活 (Is active)
     *
     * @return 是否激活 (Whether active)
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    /**
     * 是否已结束 (Is ended)
     *
     * @return 是否已结束 (Whether ended)
     */
    public boolean isEnded() {
        return status == SessionStatus.ENDED;
    }

    /**
     * 获取投票数 (Get vote count)
     *
     * @return 投票数 (Vote count)
     */
    public int getVoteCount() {
        return votes != null ? votes.size() : 0;
    }

    /**
     * 会话状态 (Session Status)
     */
    public enum SessionStatus {
        /**
         * 进行中 (Active)
         */
        ACTIVE,

        /**
         * 已结束 (Ended)
         */
        ENDED,

        /**
         * 已取消 (Cancelled)
         */
        CANCELLED
    }
}

