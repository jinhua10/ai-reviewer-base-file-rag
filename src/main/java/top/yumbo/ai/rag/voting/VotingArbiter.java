package top.yumbo.ai.rag.voting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.conflict.ConflictCase;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投票仲裁器 (Voting Arbiter)
 *
 * 通过投票机制解决知识冲突
 * (Resolves knowledge conflicts through voting mechanism)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class VotingArbiter {

    /**
     * 会话存储 (Session storage)
     */
    private final Map<String, VotingSession> sessionStorage = new ConcurrentHashMap<>();

    /**
     * 最小投票数要求 (Minimum votes required)
     */
    private static final int MIN_VOTES = 3;

    /**
     * 自动决策置信度阈值 (Auto-decision confidence threshold)
     */
    private static final double AUTO_DECISION_THRESHOLD = 0.8;

    /**
     * 创建投票会话 (Create voting session)
     *
     * @param conflict 冲突案例 (Conflict case)
     * @return 投票会话 (Voting session)
     */
    public VotingSession createSession(ConflictCase conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }

        log.info(I18N.get("voting.session.create", conflict.getConflictId()));

        VotingSession session = VotingSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .conflictId(conflict.getConflictId())
                .candidates(new ArrayList<>(conflict.getConceptIds()))
                .build();

        sessionStorage.put(session.getSessionId(), session);

        log.info(I18N.get("voting.session.created",
                session.getSessionId(), session.getCandidates().size()));

        return session;
    }

    /**
     * 投票 (Cast vote)
     *
     * @param sessionId 会话ID (Session ID)
     * @param voterId 投票者ID (Voter ID)
     * @param voterType 投票者类型 (Voter type)
     * @param candidateId 候选ID (Candidate ID)
     * @param score 评分 (Score)
     * @param reason 理由 (Reason)
     * @return 投票对象 (Vote object)
     */
    public Vote castVote(String sessionId, String voterId, VoterType voterType,
                        String candidateId, double score, String reason) {
        VotingSession session = sessionStorage.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (!session.isActive()) {
            throw new IllegalStateException("Session is not active: " + sessionId);
        }

        log.info(I18N.get("voting.vote.casting", voterId, candidateId, score));

        // 创建投票 (Create vote)
        Vote vote = Vote.builder()
                .voteId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .voterId(voterId)
                .voterType(voterType)
                .candidateId(candidateId)
                .score(score)
                .reason(reason)
                .weight(voterType.getDefaultWeight())
                .build();

        // 添加到会话 (Add to session)
        session.addVote(vote);
        session.addVoter(voterId);

        log.info(I18N.get("voting.vote.casted", vote.getVoteId(), session.getVoteCount()));

        // 检查是否可以自动决策 (Check if can auto-decide)
        if (shouldAutoDecide(session)) {
            makeDecision(sessionId);
        }

        return vote;
    }

    /**
     * 结束会话并做出决策 (End session and make decision)
     *
     * @param sessionId 会话ID (Session ID)
     * @return 投票结果 (Voting result)
     */
    public VotingResult makeDecision(String sessionId) {
        VotingSession session = sessionStorage.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        log.info(I18N.get("voting.decision.making", sessionId, session.getVoteCount()));

        // 计算每个候选的加权总分 (Calculate weighted scores)
        Map<String, Double> weightedScores = new HashMap<>();
        for (Vote vote : session.getVotes()) {
            String candidateId = vote.getCandidateId();
            double weightedScore = vote.getWeightedScore();
            weightedScores.merge(candidateId, weightedScore, Double::sum);
        }

        // 找出获胜者 (Find winner)
        String winnerId = null;
        double maxScore = 0.0;
        double secondScore = 0.0;

        for (Map.Entry<String, Double> entry : weightedScores.entrySet()) {
            double score = entry.getValue();
            if (score > maxScore) {
                secondScore = maxScore;
                maxScore = score;
                winnerId = entry.getKey();
            } else if (score > secondScore) {
                secondScore = score;
            }
        }

        // 计算指标 (Calculate metrics)
        double margin = maxScore - secondScore;
        double confidence = calculateConfidence(session, maxScore, margin);
        double consensus = calculateConsensus(session, winnerId);

        // 构建结果 (Build result)
        VotingResult result = VotingResult.builder()
                .winnerId(winnerId)
                .totalVotes(session.getVoteCount())
                .weightedScores(weightedScores)
                .confidence(confidence)
                .margin(margin)
                .consensus(consensus)
                .build();

        // 更新会话 (Update session)
        session.setResult(result);
        session.setStatus(VotingSession.SessionStatus.ENDED);
        session.setEndTime(new Date());

        log.info(I18N.get("voting.decision.made",
                winnerId, confidence, consensus));

        return result;
    }

    /**
     * 获取会话 (Get session)
     *
     * @param sessionId 会话ID (Session ID)
     * @return 会话 (Session)
     */
    public VotingSession getSession(String sessionId) {
        return sessionStorage.get(sessionId);
    }

    /**
     * 获取所有会话 (Get all sessions)
     *
     * @return 会话列表 (Session list)
     */
    public List<VotingSession> getAllSessions() {
        return new ArrayList<>(sessionStorage.values());
    }

    /**
     * 获取活跃会话 (Get active sessions)
     *
     * @return 活跃会话列表 (Active session list)
     */
    public List<VotingSession> getActiveSessions() {
        return sessionStorage.values().stream()
                .filter(VotingSession::isActive)
                .toList();
    }

    /**
     * 判断是否应该自动决策 (Should auto-decide)
     *
     * @param session 会话 (Session)
     * @return 是否应该决策 (Whether should decide)
     */
    private boolean shouldAutoDecide(VotingSession session) {
        // 投票数足够且有明确倾向
        if (session.getVoteCount() < MIN_VOTES) {
            return false;
        }

        // 计算当前领先者的得分
        Map<String, Double> scores = new HashMap<>();
        for (Vote vote : session.getVotes()) {
            scores.merge(vote.getCandidateId(), vote.getWeightedScore(), Double::sum);
        }

        if (scores.isEmpty()) {
            return false;
        }

        double maxScore = Collections.max(scores.values());
        double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();

        // 领先者得分占比超过阈值
        return (maxScore / totalScore) >= AUTO_DECISION_THRESHOLD;
    }

    /**
     * 计算置信度 (Calculate confidence)
     *
     * @param session 会话 (Session)
     * @param maxScore 最高分 (Max score)
     * @param margin 领先优势 (Margin)
     * @return 置信度 (Confidence)
     */
    private double calculateConfidence(VotingSession session, double maxScore, double margin) {
        // 基于投票数、领先优势和绝对分数
        double voteFactor = Math.min(1.0, session.getVoteCount() / 10.0);
        double marginFactor = Math.min(1.0, margin / maxScore);
        double scoreFactor = Math.min(1.0, maxScore / 50.0); // 假设满分50

        return (voteFactor + marginFactor + scoreFactor) / 3.0;
    }

    /**
     * 计算共识度 (Calculate consensus)
     *
     * @param session 会话 (Session)
     * @param winnerId 获胜者ID (Winner ID)
     * @return 共识度 (Consensus)
     */
    private double calculateConsensus(VotingSession session, String winnerId) {
        if (winnerId == null || session.getVoteCount() == 0) {
            return 0.0;
        }

        long votesForWinner = session.getVotes().stream()
                .filter(v -> winnerId.equals(v.getCandidateId()))
                .count();

        return (double) votesForWinner / session.getVoteCount();
    }

    /**
     * 清空所有会话 (Clear all sessions)
     * 仅用于测试 (For testing only)
     */
    public void clearAll() {
        sessionStorage.clear();
        log.info(I18N.get("voting.cleared"));
    }
}

