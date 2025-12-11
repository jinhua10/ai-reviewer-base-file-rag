package top.yumbo.ai.rag.behavior;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为信号分析测试 (Behavior Signal Analysis Tests)
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
@DisplayName("行为信号分析测试 (Behavior Signal Analysis Tests)")
public class BehaviorSignalTest {

    private SignalCollector collector;
    private SignalWeighter weighter;
    private AttitudeInferenceEngine inferenceEngine;
    private SignalAggregator aggregator;

    @BeforeEach
    void setUp() {
        collector = new SignalCollector();
        weighter = new SignalWeighter();
        inferenceEngine = new AttitudeInferenceEngine(weighter);
        aggregator = new SignalAggregator(inferenceEngine);
    }

    // ========== SignalCollector Tests ==========

    @Test
    @DisplayName("测试点击信号采集 (Test Click Signal Collection)")
    void testCollectClickSignal() {
        // Given
        String userId = "user001";
        String answerId = "answer001";

        // When
        BehaviorSignalEvent event = collector.collectClickSignal(userId, answerId, "copy");

        // Then
        assertNotNull(event);
        assertEquals(SignalType.COPY_ANSWER, event.getSignalType());
        assertEquals(userId, event.getUserId());
        assertEquals(answerId, event.getAnswerId());
    }

    @Test
    @DisplayName("测试时间信号采集 (Test Time Signal Collection)")
    void testCollectTimeSignal() {
        // Given
        String userId = "user001";
        String answerId = "answer001";
        long readDuration = 10; // 10秒
        long expectedDuration = 60; // 预期60秒

        // When
        BehaviorSignalEvent event = collector.collectTimeSignal(userId, answerId, readDuration, expectedDuration);

        // Then
        assertNotNull(event);
        assertEquals(SignalType.READ_TIME_SHORT, event.getSignalType());
        assertTrue(event.getContext().containsKey("read_duration"));
    }

    @Test
    @DisplayName("测试交互信号采集 (Test Interaction Signal Collection)")
    void testCollectInteractionSignal() {
        // Given
        String userId = "user001";
        String answerId = "answer001";

        // When
        BehaviorSignalEvent event = collector.collectInteractionSignal(userId, answerId, "share", "分享到朋友圈");

        // Then
        assertNotNull(event);
        assertEquals(SignalType.SHARE_ANSWER, event.getSignalType());
        assertEquals("分享到朋友圈", event.getContext("details"));
    }

    @Test
    @DisplayName("测试导航信号采集 (Test Navigation Signal Collection)")
    void testCollectNavigationSignal() {
        // Given
        String userId = "user001";
        String answerId = "answer001";

        // When
        BehaviorSignalEvent event = collector.collectNavigationSignal(userId, answerId, "search_again");

        // Then
        assertNotNull(event);
        assertEquals(SignalType.SEARCH_AGAIN, event.getSignalType());
    }

    // ========== SignalWeighter Tests ==========

    @Test
    @DisplayName("测试信号权重计算 (Test Signal Weight Calculation)")
    void testCalculateWeight() {
        // Given
        BehaviorSignalEvent event = new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER);
        event.setStrength(1.0);
        Map<String, Object> userContext = new HashMap<>();
        userContext.put("role", "expert");

        // When
        double weight = weighter.calculateWeight(event, userContext);

        // Then
        assertTrue(weight > 0, "权重应该为正数");
        assertTrue(weight >= event.getSignalType().getBaseWeight(), "专家权重应该更高");
    }

    @Test
    @DisplayName("测试权重归一化 (Test Weight Normalization)")
    void testNormalizeWeights() {
        // Given
        Map<String, Double> weights = new HashMap<>();
        weights.put("signal1", 1.0);
        weights.put("signal2", 2.0);
        weights.put("signal3", 3.0);

        // When
        Map<String, Double> normalized = weighter.normalizeWeights(weights, 0.0, 1.0);

        // Then
        assertEquals(3, normalized.size());
        assertTrue(normalized.values().stream().allMatch(v -> v >= 0.0 && v <= 1.0));
    }

    // ========== AttitudeInferenceEngine Tests ==========

    @Test
    @DisplayName("测试正面态度推断 (Test Positive Attitude Inference)")
    void testInferPositiveAttitude() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.SHARE_ANSWER));
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.EXPAND_DETAIL));

        // When
        AttitudeScore score = inferenceEngine.inferAttitude(signals, new HashMap<>());

        // Then
        assertNotNull(score);
        assertTrue(score.getRawScore() > 0, "评分应该为正");
        assertTrue(score.isPositive(), "态度应该为正面");
        assertTrue(score.getConfidence() > 0, "置信度应该大于0");
    }

    @Test
    @DisplayName("测试负面态度推断 (Test Negative Attitude Inference)")
    void testInferNegativeAttitude() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.CLOSE_IMMEDIATELY));
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.REPORT_ERROR));
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.SEARCH_AGAIN));

        // When
        AttitudeScore score = inferenceEngine.inferAttitude(signals, new HashMap<>());

        // Then
        assertNotNull(score);
        assertTrue(score.getRawScore() < 0, "评分应该为负");
        assertTrue(score.isNegative(), "态度应该为负面");
    }

    @Test
    @DisplayName("测试置信度计算 (Test Confidence Calculation)")
    void testCalculateConfidence() {
        // Given
        List<BehaviorSignalEvent> manySignals = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            manySignals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        }

        List<BehaviorSignalEvent> fewSignals = new ArrayList<>();
        fewSignals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));

        // When
        double confidenceMany = inferenceEngine.calculateConfidence(manySignals, 1.0);
        double confidenceFew = inferenceEngine.calculateConfidence(fewSignals, 1.0);

        // Then
        assertTrue(confidenceMany > confidenceFew, "更多信号应该有更高置信度");
        assertTrue(confidenceMany >= 0.0 && confidenceMany <= 1.0, "置信度应该在 0-1 范围内");
    }

    @Test
    @DisplayName("测试态度分类 (Test Attitude Classification)")
    void testClassifyAttitude() {
        // When & Then
        assertEquals(AttitudeLevel.VERY_SATISFIED, inferenceEngine.classifyAttitude(0.9));
        assertEquals(AttitudeLevel.SATISFIED, inferenceEngine.classifyAttitude(0.5));
        assertEquals(AttitudeLevel.NEUTRAL, inferenceEngine.classifyAttitude(0.0));
        assertEquals(AttitudeLevel.DISSATISFIED, inferenceEngine.classifyAttitude(-0.5));
        assertEquals(AttitudeLevel.VERY_DISSATISFIED, inferenceEngine.classifyAttitude(-0.9));
    }

    @Test
    @DisplayName("测试解释生成 (Test Explanation Generation)")
    void testGenerateExplanation() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.SHARE_ANSWER));

        // When
        String explanation = inferenceEngine.generateExplanation(signals, 0.8, 0.9);

        // Then
        assertNotNull(explanation);
        assertFalse(explanation.isEmpty());
        assertTrue(explanation.contains("0.8") || explanation.contains("0.80"));
    }

    // ========== SignalAggregator Tests ==========

    @Test
    @DisplayName("测试按用户聚合 (Test Aggregate by User)")
    void testAggregateByUser() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user001", null, "answer002", SignalType.SHARE_ANSWER));
        signals.add(new BehaviorSignalEvent("user002", null, "answer001", SignalType.REPORT_ERROR));

        // When
        Map<String, SignalAggregator.UserAggregation> result = aggregator.aggregateByUser(signals);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("user001"));
        assertTrue(result.containsKey("user002"));
    }

    @Test
    @DisplayName("测试按概念聚合 (Test Aggregate by Concept)")
    void testAggregateByConcept() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user002", null, "answer001", SignalType.SHARE_ANSWER));
        signals.add(new BehaviorSignalEvent("user003", null, "answer002", SignalType.REPORT_ERROR));

        // When
        Map<String, SignalAggregator.ConceptAggregation> result = aggregator.aggregateByConcept(signals);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("answer001"));
        assertTrue(result.containsKey("answer002"));
    }

    @Test
    @DisplayName("测试按角色聚合 (Test Aggregate by Role)")
    void testAggregateByRole() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user002", null, "answer001", SignalType.SHARE_ANSWER));
        signals.add(new BehaviorSignalEvent("user003", null, "answer001", SignalType.REPORT_ERROR));

        Map<String, String> userRoles = new HashMap<>();
        userRoles.put("user001", "expert");
        userRoles.put("user002", "regular");
        userRoles.put("user003", "new_user");

        // When
        Map<String, SignalAggregator.RoleAggregation> result = aggregator.aggregateByRole(signals, userRoles);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey("expert"));
        assertTrue(result.containsKey("regular"));
        assertTrue(result.containsKey("new_user"));
    }

    @Test
    @DisplayName("测试生成报告 (Test Generate Report)")
    void testGenerateReport() {
        // Given
        List<BehaviorSignalEvent> signals = new ArrayList<>();
        signals.add(new BehaviorSignalEvent("user001", null, "answer001", SignalType.COPY_ANSWER));
        signals.add(new BehaviorSignalEvent("user002", null, "answer001", SignalType.SHARE_ANSWER));
        signals.add(new BehaviorSignalEvent("user003", null, "answer002", SignalType.REPORT_ERROR));

        // When
        SignalAggregator.AggregationReport report = aggregator.generateReport(signals);

        // Then
        assertNotNull(report);
    }

    // ========== 集成测试 (Integration Tests) ==========

    @Test
    @DisplayName("端到端测试：从信号采集到态度推断 (E2E: Signal Collection to Attitude Inference)")
    void testEndToEndFlow() {
        // Given: 模拟用户产生多个信号 (Simulate user generating multiple signals)
        String userId = "user001";
        String answerId = "answer001";

        // When: 采集各种信号 (Collect various signals)
        collector.collectClickSignal(userId, answerId, "copy");
        collector.collectTimeSignal(userId, answerId, 90, 60); // 深度阅读 (Deep read)
        collector.collectInteractionSignal(userId, answerId, "share", "分享给同事");

        List<BehaviorSignalEvent> signals = collector.getEvents(answerId);

        // When: 推断态度 (Infer attitude)
        AttitudeScore score = inferenceEngine.inferAttitude(signals, new HashMap<>());

        // Then: 验证结果 (Verify results)
        assertNotNull(score);
        assertEquals(3, signals.size());
        assertTrue(score.isPositive(), "应该推断为正面态度");
        assertTrue(score.getConfidence() > 0.5, "置信度应该较高");
        assertNotNull(score.getExplanation(), "应该生成解释");
    }
}

