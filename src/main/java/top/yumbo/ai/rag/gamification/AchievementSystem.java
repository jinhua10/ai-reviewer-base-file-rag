package top.yumbo.ai.rag.gamification;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成就系统 (Achievement System)
 * 管理用户成就的解锁和进度追踪
 *
 * @author AI Assistant
 * @since 2025-12-11
 */
public class AchievementSystem {

    private static final Logger logger = LoggerFactory.getLogger(AchievementSystem.class);

    /**
     * 所有成就定义 (All Achievement Definitions)
     */
    private final Map<String, Achievement> achievements;

    /**
     * 用户成就 (User Achievements)
     * Key: userId, Value: 已解锁的成就ID列表
     */
    private final Map<String, Set<String>> userAchievements;

    /**
     * 用户成就进度 (User Achievement Progress)
     * Key: userId:achievementId, Value: 进度值
     */
    private final Map<String, Integer> achievementProgress;

    public AchievementSystem() {
        this.achievements = new ConcurrentHashMap<>();
        this.userAchievements = new ConcurrentHashMap<>();
        this.achievementProgress = new ConcurrentHashMap<>();
        initializeAchievements();
    }

    /**
     * 初始化成就 (Initialize Achievements)
     */
    private void initializeAchievements() {
        // 新手引导成就 (Onboarding achievements)
        registerAchievement(new Achievement(
                "first_feedback", "初次尝试", "First Try",
                "完成第一次反馈", "Complete first feedback",
                1, 10, AchievementType.ONBOARDING
        ));

        registerAchievement(new Achievement(
                "first_vote", "民主先锋", "Democracy Pioneer",
                "完成第一次投票", "Complete first vote",
                1, 15, AchievementType.ONBOARDING
        ));

        // 活跃成就 (Activity achievements)
        registerAchievement(new Achievement(
                "streak_7", "坚持不懈", "Persistent",
                "连续7天活跃", "Active for 7 consecutive days",
                7, 50, AchievementType.ACTIVITY
        ));

        registerAchievement(new Achievement(
                "streak_30", "钢铁意志", "Iron Will",
                "连续30天活跃", "Active for 30 consecutive days",
                30, 200, AchievementType.ACTIVITY
        ));

        // 贡献成就 (Contribution achievements)
        registerAchievement(new Achievement(
                "feedback_100", "反馈达人", "Feedback Master",
                "提供100次反馈", "Provide 100 feedbacks",
                100, 100, AchievementType.CONTRIBUTION
        ));

        registerAchievement(new Achievement(
                "vote_50", "投票专家", "Voting Expert",
                "投票50次", "Vote 50 times",
                50, 80, AchievementType.CONTRIBUTION
        ));

        registerAchievement(new Achievement(
                "conflict_10", "质量卫士", "Quality Guardian",
                "提交10个冲突", "Submit 10 conflicts",
                10, 150, AchievementType.CONTRIBUTION
        ));

        // 专家成就 (Expert achievements)
        registerAchievement(new Achievement(
                "accuracy_90", "慧眼识珠", "Sharp Eye",
                "投票准确率>90%", "Voting accuracy >90%",
                90, 200, AchievementType.EXPERT
        ));

        registerAchievement(new Achievement(
                "contribution_50", "黄金贡献", "Golden Contribution",
                "贡献被采纳50次", "50 contributions accepted",
                50, 250, AchievementType.EXPERT
        ));

        logger.info(I18N.get("gamification.achievement.initialized"), achievements.size());
    }

    /**
     * 注册成就 (Register Achievement)
     */
    private void registerAchievement(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }

    /**
     * 解锁成就 (Unlock Achievement)
     */
    public boolean unlockAchievement(String userId, String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) {
            logger.warn(I18N.get("gamification.achievement.not_found"), achievementId);
            return false;
        }

        // 检查是否已解锁 (Check if already unlocked)
        Set<String> userUnlocked = userAchievements.computeIfAbsent(userId, k -> new HashSet<>());
        if (userUnlocked.contains(achievementId)) {
            return false; // 已解锁 (Already unlocked)
        }

        // 解锁成就 (Unlock)
        userUnlocked.add(achievementId);
        achievement.incrementUnlockCount();

        logger.info(I18N.get("gamification.achievement.unlocked"),
                userId, achievement.getNameCn(), achievement.getRewardPoints());

        return true;
    }

    /**
     * 更新成就进度 (Update Achievement Progress)
     */
    public void updateProgress(String userId, String achievementId, int progress) {
        String key = userId + ":" + achievementId;
        achievementProgress.put(key, progress);

        Achievement achievement = achievements.get(achievementId);
        if (achievement != null && progress >= achievement.getRequirement()) {
            unlockAchievement(userId, achievementId);
        }
    }

    /**
     * 获取成就进度 (Get Achievement Progress)
     */
    public Map<String, AchievementProgress> getProgress(String userId) {
        Map<String, AchievementProgress> progress = new HashMap<>();
        Set<String> unlocked = userAchievements.getOrDefault(userId, Collections.emptySet());

        for (Achievement achievement : achievements.values()) {
            boolean isUnlocked = unlocked.contains(achievement.getId());
            int currentProgress = achievementProgress.getOrDefault(
                    userId + ":" + achievement.getId(), 0);

            progress.put(achievement.getId(), new AchievementProgress(
                    achievement, isUnlocked, currentProgress
            ));
        }

        return progress;
    }

    /**
     * 获取已解锁成就 (Get Unlocked Achievements)
     */
    public List<Achievement> getUnlockedAchievements(String userId) {
        Set<String> unlocked = userAchievements.getOrDefault(userId, Collections.emptySet());
        List<Achievement> result = new ArrayList<>();

        for (String achievementId : unlocked) {
            Achievement achievement = achievements.get(achievementId);
            if (achievement != null) {
                result.add(achievement);
            }
        }

        return result;
    }
}

/**
 * 成就 (Achievement)
 */
@Data
class Achievement {
    private String id;
    private String nameCn;
    private String nameEn;
    private String descriptionCn;
    private String descriptionEn;
    private int requirement;
    private int rewardPoints;
    private AchievementType type;
    private int unlockCount; // 解锁次数统计 (Unlock count)

    public Achievement(String id, String nameCn, String nameEn,
                       String descriptionCn, String descriptionEn,
                       int requirement, int rewardPoints, AchievementType type) {
        this.id = id;
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.descriptionCn = descriptionCn;
        this.descriptionEn = descriptionEn;
        this.requirement = requirement;
        this.rewardPoints = rewardPoints;
        this.type = type;
        this.unlockCount = 0;
    }


    public void incrementUnlockCount() { this.unlockCount++; }
}

enum AchievementType {
    ONBOARDING,   // 新手引导
    ACTIVITY,     // 活跃成就
    CONTRIBUTION, // 贡献成就
    EXPERT        // 专家成就
}

/**
 * 成就进度 (Achievement Progress)
 */
@Data
class AchievementProgress {
    private Achievement achievement;
    private boolean unlocked;
    private int currentProgress;

    public AchievementProgress(Achievement achievement, boolean unlocked, int currentProgress) {
        this.achievement = achievement;
        this.unlocked = unlocked;
        this.currentProgress = currentProgress;
    }


    public double getProgressPercentage() {
        if (unlocked) return 100.0;
        return Math.min(100.0, (double) currentProgress / achievement.getRequirement() * 100);
    }
}

