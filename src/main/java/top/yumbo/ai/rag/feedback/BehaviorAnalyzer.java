package top.yumbo.ai.rag.feedback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.ArrayList;
import java.util.List;

/**
 * 行为分析器 (Behavior Analyzer)
 *
 * 分析用户行为信号，推断隐式反馈
 * (Analyzes user behavior signals to infer implicit feedback)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class BehaviorAnalyzer {

    /**
     * 最小信号数量阈值 (Minimum signal count threshold)
     * 至少需要这么多信号才能推断
     */
    private static final int MIN_SIGNAL_COUNT = 2;

    /**
     * 置信度阈值 (Confidence threshold)
     * 低于此阈值的推断不生成反馈
     */
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    /**
     * 分析行为信号并推断反馈 (Analyze behavior signals and infer feedback)
     *
     * @param sessionId 会话ID (Session ID)
     * @param signals 行为信号列表 (Behavior signal list)
     * @return 推断的反馈，如果置信度不足返回null (Inferred feedback, null if confidence is low)
     */
    public Feedback analyzeBehavior(String sessionId, List<BehaviorSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            log.debug(I18N.get("feedback.behavior.no_signals", sessionId));
            return null;
        }

        if (signals.size() < MIN_SIGNAL_COUNT) {
            log.debug(I18N.get("feedback.behavior.insufficient_signals", sessionId, signals.size()));
            return null;
        }

        log.info(I18N.get("feedback.behavior.analyzing", sessionId, signals.size()));

        // 1. 计算加权平均分数 (Calculate weighted average score)
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (BehaviorSignal signal : signals) {
            double weightedScore = signal.getValue() * signal.getWeight();
            totalWeightedScore += weightedScore;
            totalWeight += Math.abs(signal.getWeight());

            log.debug(I18N.get("feedback.behavior.signal_processed",
                    signal.getType(), signal.getValue(), signal.getWeight()));
        }

        // 2. 归一化得分到[-1, 1] (Normalize score to [-1, 1])
        double normalizedScore = totalWeightedScore / totalWeight;

        // 3. 计算置信度 (Calculate confidence)
        // 信号数量越多，置信度越高
        double confidence = Math.min(1.0, signals.size() / 10.0);

        // 4. 检查置信度阈值 (Check confidence threshold)
        if (confidence < CONFIDENCE_THRESHOLD) {
            log.debug(I18N.get("feedback.behavior.low_confidence", sessionId, confidence));
            return null;
        }

        log.info(I18N.get("feedback.behavior.inferred",
                sessionId, normalizedScore, confidence));

        // 5. 构建隐式反馈 (Build implicit feedback)
        Feedback feedback = Feedback.builder()
                .sessionId(sessionId)
                .type(Feedback.FeedbackType.IMPLICIT)
                .source(Feedback.FeedbackSource.SYSTEM)
                .value(normalizedScore)
                .build();

        // 6. 添加行为数据 (Add behavior data)
        feedback.addBehaviorData("signalCount", signals.size());
        feedback.addBehaviorData("confidence", confidence);
        feedback.addBehaviorData("signals", convertSignalsToMap(signals));

        return feedback;
    }

    /**
     * 转换信号为Map格式 (Convert signals to map format)
     *
     * @param signals 信号列表 (Signal list)
     * @return Map格式 (Map format)
     */
    private List<java.util.Map<String, Object>> convertSignalsToMap(List<BehaviorSignal> signals) {
        List<java.util.Map<String, Object>> result = new ArrayList<>();

        for (BehaviorSignal signal : signals) {
            java.util.Map<String, Object> signalMap = new java.util.HashMap<>();
            signalMap.put("type", signal.getType().name());
            signalMap.put("value", signal.getValue());
            signalMap.put("weight", signal.getWeight());
            signalMap.put("timestamp", signal.getTimestamp());
            result.add(signalMap);
        }

        return result;
    }

    /**
     * 判断是否是正面行为 (Check if behavior is positive)
     *
     * @param signals 行为信号列表 (Behavior signal list)
     * @return 是否正面 (Whether positive)
     */
    public boolean isPositiveBehavior(List<BehaviorSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return false;
        }

        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (BehaviorSignal signal : signals) {
            totalWeightedScore += signal.getValue() * signal.getWeight();
            totalWeight += Math.abs(signal.getWeight());
        }

        return (totalWeightedScore / totalWeight) > 0;
    }

    /**
     * 判断是否是负面行为 (Check if behavior is negative)
     *
     * @param signals 行为信号列表 (Behavior signal list)
     * @return 是否负面 (Whether negative)
     */
    public boolean isNegativeBehavior(List<BehaviorSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return false;
        }

        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (BehaviorSignal signal : signals) {
            totalWeightedScore += signal.getValue() * signal.getWeight();
            totalWeight += Math.abs(signal.getWeight());
        }

        return (totalWeightedScore / totalWeight) < 0;
    }
}

