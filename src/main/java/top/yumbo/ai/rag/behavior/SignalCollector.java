package top.yumbo.ai.rag.behavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信号采集器 (Signal Collector)
 * 采集用户在使用系统时产生的各种行为信号
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class SignalCollector {

    private static final Logger logger = LoggerFactory.getLogger(SignalCollector.class);

    /**
     * 事件存储 (Event Storage)
     * Key: answerId, Value: 该答案的信号事件列表
     */
    private final Map<String, List<BehaviorSignalEvent>> eventStore;

    /**
     * 会话追踪 (Session Tracking)
     * Key: sessionId, Value: 会话开始时间
     */
    private final Map<String, LocalDateTime> sessionTracker;

    /**
     * 答案查看追踪 (Answer View Tracking)
     * Key: answerId, Value: 首次查看时间
     */
    private final Map<String, LocalDateTime> viewTracker;

    // ========== 构造函数 (Constructors) ==========

    public SignalCollector() {
        this.eventStore = new ConcurrentHashMap<>();
        this.sessionTracker = new ConcurrentHashMap<>();
        this.viewTracker = new ConcurrentHashMap<>();
    }

    // ========== 点击信号采集 (Click Signal Collection) ==========

    /**
     * 采集点击信号 (Collect Click Signal)
     *
     * @param userId 用户ID
     * @param answerId 答案ID
     * @param clickTarget 点击目标（copy/reference/expand等）
     */
    public BehaviorSignalEvent collectClickSignal(String userId, String answerId, String clickTarget) {
        logger.debug(I18N.get("behavior.collect.click"), userId, answerId, clickTarget);

        SignalType signalType = determineClickSignalType(clickTarget);
        BehaviorSignalEvent event = createEvent(userId, answerId, signalType);
        event.addContext("click_target", clickTarget);

        storeEvent(event);
        return event;
    }

    /**
     * 判断点击信号类型 (Determine Click Signal Type)
     */
    private SignalType determineClickSignalType(String clickTarget) {
        return switch (clickTarget.toLowerCase()) {
            case "copy" -> SignalType.COPY_ANSWER;
            case "reference", "link" -> SignalType.CLICK_REFERENCE;
            case "expand", "detail" -> SignalType.EXPAND_DETAIL;
            case "scroll_down" -> SignalType.SCROLL_DOWN;
            case "close" -> SignalType.CLOSE_IMMEDIATELY;
            default -> {
                logger.warn(I18N.get("behavior.collect.unknown_target"), clickTarget);
                yield SignalType.EXPAND_DETAIL;
            }
        };
    }

    // ========== 时间信号采集 (Time Signal Collection) ==========

    /**
     * 采集时间信号 (Collect Time Signal)
     *
     * @param userId 用户ID
     * @param answerId 答案ID
     * @param readDuration 阅读时长（秒）
     * @param expectedDuration 预期阅读时长（秒）
     */
    public BehaviorSignalEvent collectTimeSignal(String userId, String answerId,
                                                   long readDuration, long expectedDuration) {
        logger.debug(I18N.get("behavior.collect.time"), userId, answerId, readDuration, expectedDuration);

        SignalType signalType = determineTimeSignalType(readDuration, expectedDuration);
        BehaviorSignalEvent event = createEvent(userId, answerId, signalType);
        event.addContext("read_duration", readDuration);
        event.addContext("expected_duration", expectedDuration);
        event.addContext("duration_ratio", (double) readDuration / expectedDuration);

        storeEvent(event);
        return event;
    }

    /**
     * 判断时间信号类型 (Determine Time Signal Type)
     */
    private SignalType determineTimeSignalType(long actual, long expected) {
        double ratio = (double) actual / expected;

        if (ratio < 0.3) {
            return SignalType.READ_TIME_SHORT;
        } else if (ratio > 1.5) {
            return SignalType.READ_TIME_LONG;
        } else {
            return SignalType.READ_TIME_NORMAL;
        }
    }

    /**
     * 记录返回访问 (Record Return Visit)
     *
     * @param userId 用户ID
     * @param answerId 答案ID
     */
    public BehaviorSignalEvent collectReturnVisit(String userId, String answerId) {
        logger.debug(I18N.get("behavior.collect.return"), userId, answerId);

        LocalDateTime firstView = viewTracker.get(answerId);
        LocalDateTime now = LocalDateTime.now();

        BehaviorSignalEvent event = createEvent(userId, answerId, SignalType.RETURN_VISIT);

        if (firstView != null) {
            Duration gap = Duration.between(firstView, now);
            event.addContext("time_gap_minutes", gap.toMinutes());
            // 时间间隔越长，信号强度越高 (Longer gap = stronger signal)
            double strength = Math.min(1.0, gap.toMinutes() / 60.0);
            event.setStrength(strength);
        }

        storeEvent(event);
        return event;
    }

    // ========== 交互信号采集 (Interaction Signal Collection) ==========

    /**
     * 采集交互信号 (Collect Interaction Signal)
     *
     * @param userId 用户ID
     * @param answerId 答案ID
     * @param interactionType 交互类型（followup/share/report/edit）
     * @param details 交互详情
     */
    public BehaviorSignalEvent collectInteractionSignal(String userId, String answerId,
                                                         String interactionType, String details) {
        logger.debug(I18N.get("behavior.collect.interaction"), userId, answerId, interactionType);

        SignalType signalType = determineInteractionSignalType(interactionType);
        BehaviorSignalEvent event = createEvent(userId, answerId, signalType);
        event.addContext("interaction_type", interactionType);
        event.addContext("details", details);

        // 特殊处理追问信号 (Special handling for followup)
        if (signalType == SignalType.ASK_FOLLOWUP) {
            adjustFollowupStrength(event, details);
        }

        storeEvent(event);
        return event;
    }

    /**
     * 判断交互信号类型 (Determine Interaction Signal Type)
     */
    private SignalType determineInteractionSignalType(String interactionType) {
        return switch (interactionType.toLowerCase()) {
            case "followup", "ask" -> SignalType.ASK_FOLLOWUP;
            case "share" -> SignalType.SHARE_ANSWER;
            case "report", "error" -> SignalType.REPORT_ERROR;
            case "edit" -> SignalType.EDIT_ANSWER;
            default -> {
                logger.warn(I18N.get("behavior.collect.unknown_interaction"), interactionType);
                yield SignalType.ASK_FOLLOWUP;
            }
        };
    }

    /**
     * 调整追问信号强度 (Adjust Followup Signal Strength)
     * 根据追问内容判断是正面还是负面
     */
    private void adjustFollowupStrength(BehaviorSignalEvent event, String details) {
        // 简单的关键词匹配 (Simple keyword matching)
        String lower = details.toLowerCase();

        // 正面关键词：详细、更多、深入等 (Positive keywords)
        if (lower.contains("detail") || lower.contains("more") || lower.contains("深入")
                || lower.contains("详细") || lower.contains("更多")) {
            event.setStrength(1.0); // 正面追问 (Positive followup)
        }
        // 负面关键词：不清楚、错误、问题等 (Negative keywords)
        else if (lower.contains("unclear") || lower.contains("wrong") || lower.contains("不清楚")
                || lower.contains("错误") || lower.contains("问题")) {
            event.setStrength(0.3); // 负面追问 (Negative followup)
        }
    }

    // ========== 导航信号采集 (Navigation Signal Collection) ==========

    /**
     * 采集导航信号 (Collect Navigation Signal)
     *
     * @param userId 用户ID
     * @param answerId 答案ID
     * @param navigationType 导航类型（search_again/view_alternative）
     */
    public BehaviorSignalEvent collectNavigationSignal(String userId, String answerId, String navigationType) {
        logger.debug(I18N.get("behavior.collect.navigation"), userId, answerId, navigationType);

        SignalType signalType = determineNavigationSignalType(navigationType);
        BehaviorSignalEvent event = createEvent(userId, answerId, signalType);
        event.addContext("navigation_type", navigationType);

        storeEvent(event);
        return event;
    }

    /**
     * 判断导航信号类型 (Determine Navigation Signal Type)
     */
    private SignalType determineNavigationSignalType(String navigationType) {
        return switch (navigationType.toLowerCase()) {
            case "search_again", "retry" -> SignalType.SEARCH_AGAIN;
            case "view_alternative", "compare" -> SignalType.VIEW_ALTERNATIVE;
            default -> {
                logger.warn(I18N.get("behavior.collect.unknown_navigation"), navigationType);
                yield SignalType.VIEW_ALTERNATIVE;
            }
        };
    }

    // ========== 辅助方法 (Helper Methods) ==========

    /**
     * 创建事件 (Create Event)
     */
    private BehaviorSignalEvent createEvent(String userId, String answerId, SignalType signalType) {
        BehaviorSignalEvent event = new BehaviorSignalEvent(userId, null, answerId, signalType);
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());

        // 记录首次查看时间 (Record first view time)
        viewTracker.putIfAbsent(answerId, LocalDateTime.now());

        return event;
    }

    /**
     * 存储事件 (Store Event)
     */
    private void storeEvent(BehaviorSignalEvent event) {
        String answerId = event.getAnswerId();
        eventStore.computeIfAbsent(answerId, k -> new ArrayList<>()).add(event);

        logger.debug(I18N.get("behavior.collect.stored"), event.getEventId(), answerId);
    }

    /**
     * 获取答案的所有信号事件 (Get All Events for Answer)
     */
    public List<BehaviorSignalEvent> getEvents(String answerId) {
        return eventStore.getOrDefault(answerId, Collections.emptyList());
    }

    /**
     * 清除答案的事件 (Clear Events for Answer)
     */
    public void clearEvents(String answerId) {
        eventStore.remove(answerId);
        viewTracker.remove(answerId);
        logger.info(I18N.get("behavior.collect.cleared"), answerId);
    }

    /**
     * 获取统计信息 (Get Statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_answers", eventStore.size());
        stats.put("total_events", eventStore.values().stream().mapToInt(List::size).sum());
        stats.put("active_sessions", sessionTracker.size());
        return stats;
    }
}

