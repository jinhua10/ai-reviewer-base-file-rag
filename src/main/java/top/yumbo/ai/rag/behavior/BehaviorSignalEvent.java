package top.yumbo.ai.rag.behavior;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 行为信号事件 (Behavior Signal Event)
 * 记录用户产生的单个行为信号
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class BehaviorSignalEvent {

    /**
     * 事件ID (Event ID)
     */
    private String eventId;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 会话ID (Session ID)
     */
    private String sessionId;

    /**
     * 问答ID (QA ID)
     */
    private String qaId;

    /**
     * 答案ID (Answer ID)
     */
    private String answerId;

    /**
     * 信号类型 (Signal Type)
     */
    private SignalType signalType;

    /**
     * 信号强度 (Signal Strength)
     * 范围：0.0 ~ 1.0，表示信号的确定性
     */
    private double strength;

    /**
     * 时间戳 (Timestamp)
     */
    private LocalDateTime timestamp;

    /**
     * 上下文信息 (Context Information)
     * 存储额外的上下文数据，如阅读时长、点击位置等
     */
    private Map<String, Object> context;

    // ========== 构造函数 (Constructors) ==========

    public BehaviorSignalEvent() {
        this.context = new HashMap<>();
        this.timestamp = LocalDateTime.now();
        this.strength = 1.0;
    }

    public BehaviorSignalEvent(String userId, String qaId, String answerId, SignalType signalType) {
        this();
        this.userId = userId;
        this.qaId = qaId;
        this.answerId = answerId;
        this.signalType = signalType;
    }

    // ========== Getter/Setter 方法 (Getter/Setter Methods) ==========

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQaId() {
        return qaId;
    }

    public void setQaId(String qaId) {
        this.qaId = qaId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    // ========== 便捷方法 (Convenience Methods) ==========

    /**
     * 添加上下文信息 (Add Context)
     */
    public void addContext(String key, Object value) {
        this.context.put(key, value);
    }

    /**
     * 获取上下文信息 (Get Context)
     */
    public Object getContext(String key) {
        return this.context.get(key);
    }

    /**
     * 获取加权后的信号值 (Get Weighted Signal Value)
     * 权重 = 基础权重 × 信号强度
     */
    public double getWeightedValue() {
        return signalType.getBaseWeight() * strength;
    }

    @Override
    public String toString() {
        return String.format("BehaviorSignalEvent{eventId='%s', userId='%s', qaId='%s', signalType=%s, strength=%.2f, timestamp=%s}",
                eventId, userId, qaId, signalType, strength, timestamp);
    }
}

