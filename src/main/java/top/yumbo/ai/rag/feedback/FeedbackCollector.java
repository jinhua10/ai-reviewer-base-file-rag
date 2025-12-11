package top.yumbo.ai.rag.feedback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反馈收集器 (Feedback Collector)
 *
 * 收集用户的显式和隐式反馈
 * (Collects explicit and implicit user feedback)
 *
 * 核心功能 (Core Features):
 * - 显式反馈收集 (Explicit feedback collection)
 * - 隐式行为分析 (Implicit behavior analysis)
 * - 反馈存储 (Feedback storage)
 * - 反馈统计 (Feedback statistics)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class FeedbackCollector {

    @Autowired
    private BehaviorAnalyzer behaviorAnalyzer;

    /**
     * 会话行为信号缓存 (Session behavior signals cache)
     * Key: sessionId, Value: List of signals
     */
    private final Map<String, List<BehaviorSignal>> sessionSignals = new ConcurrentHashMap<>();

    /**
     * 反馈存储 (Feedback storage)
     * 临时内存存储，后续可扩展为数据库
     */
    private final List<Feedback> feedbackStorage = Collections.synchronizedList(new ArrayList<>());

    /**
     * 收集显式反馈 (Collect explicit feedback)
     *
     * @param sessionId 会话ID (Session ID)
     * @param userId 用户ID (User ID)
     * @param question 问题 (Question)
     * @param answer 答案 (Answer)
     * @param value 反馈值（1:好, -1:差, 0:中立） (Feedback value: 1:good, -1:bad, 0:neutral)
     * @param tags 反馈标签 (Feedback tags)
     * @param comment 评论 (Comment)
     * @return 反馈对象 (Feedback object)
     */
    public Feedback collectExplicit(String sessionId, String userId, String question,
                                   String answer, double value, String[] tags, String comment) {
        long startTime = System.currentTimeMillis();

        log.info(I18N.get("feedback.collect.explicit", sessionId, userId, value));

        // 构建反馈对象 (Build feedback object)
        Feedback feedback = Feedback.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .question(question)
                .answer(answer)
                .type(Feedback.FeedbackType.EXPLICIT)
                .source(Feedback.FeedbackSource.USER)
                .value(value)
                .tags(tags)
                .comment(comment)
                .createTime(new Date())
                .status(Feedback.ProcessingStatus.PENDING)
                .build();

        // 存储反馈 (Store feedback)
        feedbackStorage.add(feedback);

        long duration = System.currentTimeMillis() - startTime;
        log.info(I18N.get("feedback.collect.success", feedback.getId(), duration));

        return feedback;
    }

    /**
     * 记录行为信号 (Record behavior signal)
     *
     * @param signal 行为信号 (Behavior signal)
     */
    public void recordBehavior(BehaviorSignal signal) {
        if (signal == null || signal.getSessionId() == null) {
            log.warn(I18N.get("feedback.behavior.invalid_signal"));
            return;
        }

        String sessionId = signal.getSessionId();

        sessionSignals.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(signal);

        log.debug(I18N.get("feedback.behavior.recorded",
                signal.getType(), sessionId, sessionSignals.get(sessionId).size()));

        // 如果信号足够多，尝试推断隐式反馈 (Try to infer implicit feedback if enough signals)
        if (sessionSignals.get(sessionId).size() >= 3) {
            tryInferImplicitFeedback(sessionId);
        }
    }

    /**
     * 尝试推断隐式反馈 (Try to infer implicit feedback)
     *
     * @param sessionId 会话ID (Session ID)
     */
    private void tryInferImplicitFeedback(String sessionId) {
        List<BehaviorSignal> signals = sessionSignals.get(sessionId);
        if (signals == null || signals.isEmpty()) {
            return;
        }

        log.debug(I18N.get("feedback.implicit.trying", sessionId, signals.size()));

        // 使用行为分析器推断反馈 (Use behavior analyzer to infer feedback)
        Feedback implicitFeedback = behaviorAnalyzer.analyzeBehavior(sessionId, signals);

        if (implicitFeedback != null) {
            // 生成ID (Generate ID)
            implicitFeedback.setId(UUID.randomUUID().toString());

            // 存储推断的反馈 (Store inferred feedback)
            feedbackStorage.add(implicitFeedback);

            log.info(I18N.get("feedback.implicit.inferred",
                    implicitFeedback.getId(), sessionId, implicitFeedback.getValue()));
        }
    }

    /**
     * 结束会话并推断最终反馈 (End session and infer final feedback)
     *
     * @param sessionId 会话ID (Session ID)
     * @param userId 用户ID (User ID)
     * @param question 问题 (Question)
     * @param answer 答案 (Answer)
     * @return 推断的反馈，如果无法推断返回null (Inferred feedback, null if cannot infer)
     */
    public Feedback endSessionAndInfer(String sessionId, String userId,
                                      String question, String answer) {
        List<BehaviorSignal> signals = sessionSignals.remove(sessionId);

        if (signals == null || signals.isEmpty()) {
            log.debug(I18N.get("feedback.session.no_signals", sessionId));
            return null;
        }

        log.info(I18N.get("feedback.session.ending", sessionId, signals.size()));

        // 推断最终反馈 (Infer final feedback)
        Feedback implicitFeedback = behaviorAnalyzer.analyzeBehavior(sessionId, signals);

        if (implicitFeedback != null) {
            implicitFeedback.setId(UUID.randomUUID().toString());
            implicitFeedback.setUserId(userId);
            implicitFeedback.setQuestion(question);
            implicitFeedback.setAnswer(answer);

            feedbackStorage.add(implicitFeedback);

            log.info(I18N.get("feedback.session.ended",
                    implicitFeedback.getId(), sessionId));
        }

        return implicitFeedback;
    }

    /**
     * 获取会话的所有反馈 (Get all feedback for session)
     *
     * @param sessionId 会话ID (Session ID)
     * @return 反馈列表 (Feedback list)
     */
    public List<Feedback> getFeedbackBySession(String sessionId) {
        List<Feedback> result = new ArrayList<>();

        for (Feedback feedback : feedbackStorage) {
            if (sessionId.equals(feedback.getSessionId())) {
                result.add(feedback);
            }
        }

        return result;
    }

    /**
     * 获取用户的所有反馈 (Get all feedback for user)
     *
     * @param userId 用户ID (User ID)
     * @return 反馈列表 (Feedback list)
     */
    public List<Feedback> getFeedbackByUser(String userId) {
        List<Feedback> result = new ArrayList<>();

        for (Feedback feedback : feedbackStorage) {
            if (userId.equals(feedback.getUserId())) {
                result.add(feedback);
            }
        }

        return result;
    }

    /**
     * 获取待处理的反馈 (Get pending feedback)
     *
     * @return 待处理反馈列表 (Pending feedback list)
     */
    public List<Feedback> getPendingFeedback() {
        List<Feedback> result = new ArrayList<>();

        for (Feedback feedback : feedbackStorage) {
            if (feedback.getStatus() == Feedback.ProcessingStatus.PENDING) {
                result.add(feedback);
            }
        }

        return result;
    }

    /**
     * 标记反馈为已处理 (Mark feedback as processed)
     *
     * @param feedbackId 反馈ID (Feedback ID)
     */
    public void markAsProcessed(String feedbackId) {
        for (Feedback feedback : feedbackStorage) {
            if (feedbackId.equals(feedback.getId())) {
                feedback.setStatus(Feedback.ProcessingStatus.PROCESSED);
                feedback.setProcessedTime(new Date());
                log.info(I18N.get("feedback.marked.processed", feedbackId));
                break;
            }
        }
    }

    /**
     * 获取反馈统计 (Get feedback statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public FeedbackStats getStatistics() {
        FeedbackStats stats = new FeedbackStats();

        for (Feedback feedback : feedbackStorage) {
            stats.incrementTotal();

            if (feedback.isExplicit()) {
                stats.incrementExplicit();
            } else {
                stats.incrementImplicit();
            }

            if (feedback.isPositive()) {
                stats.incrementPositive();
            } else if (feedback.isNegative()) {
                stats.incrementNegative();
            }

            if (feedback.getStatus() == Feedback.ProcessingStatus.PROCESSED) {
                stats.incrementProcessed();
            }
        }

        return stats;
    }

    /**
     * 清空所有反馈 (Clear all feedback)
     * 仅用于测试 (For testing only)
     */
    public void clearAll() {
        feedbackStorage.clear();
        sessionSignals.clear();
        log.info(I18N.get("feedback.cleared"));
    }
}

