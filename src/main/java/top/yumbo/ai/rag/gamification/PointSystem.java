package top.yumbo.ai.rag.gamification;

import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 积分系统 (Point System)
 * 管理用户积分的获取、扣除和等级计算
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class PointSystem {

    private static final Logger logger = LoggerFactory.getLogger(PointSystem.class);

    /**
     * 用户积分 (User Points)
     * Key: userId, Value: 总积分
     */
    private final Map<String, Integer> userPoints;

    /**
     * 积分历史 (Point History)
     * Key: userId, Value: 交易列表
     */
    private final Map<String, List<PointTransaction>> pointHistory;

    /**
     * 积分规则 (Point Rules)
     */
    private final Map<String, Integer> pointRules;

    public PointSystem() {
        this.userPoints = new ConcurrentHashMap<>();
        this.pointHistory = new ConcurrentHashMap<>();
        this.pointRules = new HashMap<>();
        initializeRules();
    }

    /**
     * 初始化积分规则 (Initialize Point Rules)
     */
    private void initializeRules() {
        // 显式反馈 (Explicit feedback)
        pointRules.put("like", 10);
        pointRules.put("dislike", 10);
        pointRules.put("comment", 20);
        pointRules.put("detailed_reason", 30);

        // 隐式行为 (Implicit behavior)
        pointRules.put("full_read", 5);
        pointRules.put("copy_answer", 8);
        pointRules.put("share_answer", 15);

        // 贡献 (Contribution)
        pointRules.put("submit_conflict", 50);
        pointRules.put("vote", 20);
        pointRules.put("expert_review", 100);

        // 连续行为 (Streak)
        pointRules.put("streak_7days", 50);
        pointRules.put("streak_30days", 200);

        logger.info(I18N.get("gamification.points.rules_initialized"), pointRules.size());
    }

    /**
     * 获得积分 (Earn Points)
     */
    public int earnPoints(String userId, String action, int multiplier) {
        int basePoints = pointRules.getOrDefault(action, 0);
        int earnedPoints = basePoints * multiplier;

        // 更新总积分 (Update total points)
        int currentPoints = userPoints.getOrDefault(userId, 0);
        int newTotal = currentPoints + earnedPoints;
        userPoints.put(userId, newTotal);

        // 记录交易 (Record transaction)
        PointTransaction transaction = new PointTransaction(
                userId, action, earnedPoints, TransactionType.EARN, LocalDateTime.now()
        );
        pointHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(transaction);

        logger.info(I18N.get("gamification.points.earned"),
                userId, earnedPoints, action, newTotal);

        return newTotal;
    }

    /**
     * 扣除积分 (Deduct Points)
     */
    public int deductPoints(String userId, int points, String reason) {
        int currentPoints = userPoints.getOrDefault(userId, 0);
        int newTotal = Math.max(0, currentPoints - points);
        userPoints.put(userId, newTotal);

        // 记录交易 (Record transaction)
        PointTransaction transaction = new PointTransaction(
                userId, reason, -points, TransactionType.DEDUCT, LocalDateTime.now()
        );
        pointHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(transaction);

        logger.info(I18N.get("gamification.points.deducted"),
                userId, points, reason, newTotal);

        return newTotal;
    }

    /**
     * 获取用户积分 (Get User Points)
     */
    public int getPoints(String userId) {
        return userPoints.getOrDefault(userId, 0);
    }

    /**
     * 获取积分历史 (Get Point History)
     */
    public List<PointTransaction> getHistory(String userId) {
        return pointHistory.getOrDefault(userId, Collections.emptyList());
    }

    /**
     * 计算用户等级 (Calculate User Level)
     */
    public UserLevel calculateLevel(String userId) {
        int points = getPoints(userId);

        if (points >= 5000) {
            return UserLevel.MASTER; // 大师 (Master)
        } else if (points >= 2000) {
            return UserLevel.EXPERT; // 专家 (Expert)
        } else if (points >= 500) {
            return UserLevel.CONTRIBUTOR; // 贡献者 (Contributor)
        } else if (points >= 100) {
            return UserLevel.ACTIVE; // 活跃者 (Active)
        } else {
            return UserLevel.NOVICE; // 新手 (Novice)
        }
    }

    /**
     * 用户等级枚举 (User Level Enum)
     */
    @Getter
    public enum UserLevel {
        NOVICE(0, 99, "新手", "Novice"),
        ACTIVE(100, 499, "活跃者", "Active"),
        CONTRIBUTOR(500, 1999, "贡献者", "Contributor"),
        EXPERT(2000, 4999, "专家", "Expert"),
        MASTER(5000, Integer.MAX_VALUE, "大师", "Master");

        private final int minPoints;
        private final int maxPoints;
        private final String nameCn;
        private final String nameEn;

        UserLevel(int minPoints, int maxPoints, String nameCn, String nameEn) {
            this.minPoints = minPoints;
            this.maxPoints = maxPoints;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

    }
}

/**
 * 积分交易记录 (Point Transaction)
 */
@Data
class PointTransaction {
    private String userId;
    private String action;
    private int points;
    private TransactionType type;
    private LocalDateTime timestamp;

    public PointTransaction(String userId, String action, int points,
                            TransactionType type, LocalDateTime timestamp) {
        this.userId = userId;
        this.action = action;
        this.points = points;
        this.type = type;
        this.timestamp = timestamp;
    }

}

enum TransactionType {
    EARN, DEDUCT, REDEEM
}

