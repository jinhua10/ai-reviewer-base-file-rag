package top.yumbo.ai.rag.ai.wishlist;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 愿望单管理器 (Wish List Manager)
 *
 * 功能 (Features):
 * 1. 用户提交愿望 (User submits wishes)
 * 2. 投票排行 (Vote ranking)
 * 3. 自动进入开发计划 (Auto enter development plan)
 * 4. 通知系统 (Notification system)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class WishListManager {

    /**
     * 愿望列表 (Wish list)
     */
    private final Map<String, WishItem> wishes = new ConcurrentHashMap<>();

    /**
     * 投票记录 (Vote records)
     */
    private final Map<String, Set<String>> votes = new ConcurrentHashMap<>();

    /**
     * 开发计划阈值 (Development plan threshold)
     */
    private int developmentThreshold = 10;  // 10票进入开发计划

    /**
     * 每月实现数量 (Monthly implementation count)
     */
    private int monthlyImplementationCount = 3;  // 每月实现Top 3

    // ========== 初始化 (Initialization) ==========

    public WishListManager() {
        log.info(I18N.get("wishlist.initialized"));
    }

    // ========== 愿望提交 (Wish Submission) ==========

    /**
     * 提交愿望 (Submit wish)
     *
     * @param userId 用户ID (User ID)
     * @param title 标题 (Title)
     * @param description 描述 (Description)
     * @return 愿望ID (Wish ID)
     */
    public String submitWish(String userId, String title, String description) {
        try {
            String wishId = UUID.randomUUID().toString();

            WishItem wish = new WishItem();
            wish.setWishId(wishId);
            wish.setSubmitterId(userId);
            wish.setTitle(title);
            wish.setDescription(description);
            wish.setStatus(WishStatus.SUBMITTED);
            wish.setSubmitTime(LocalDateTime.now());
            wish.setVoteCount(0);

            wishes.put(wishId, wish);

            log.info(I18N.get("wishlist.submitted"), title, userId);
            return wishId;

        } catch (Exception e) {
            log.error(I18N.get("wishlist.submit_failed"), e.getMessage(), e);
            return null;
        }
    }

    // ========== 投票管理 (Vote Management) ==========

    /**
     * 为愿望投票 (Vote for wish)
     *
     * @param wishId 愿望ID (Wish ID)
     * @param userId 用户ID (User ID)
     * @return 是否成功 (Success or not)
     */
    public boolean vote(String wishId, String userId) {
        try {
            WishItem wish = wishes.get(wishId);
            if (wish == null) {
                log.warn(I18N.get("wishlist.not_found"), wishId);
                return false;
            }

            // 检查是否已投票 (Check if already voted)
            Set<String> voters = votes.computeIfAbsent(wishId, k -> ConcurrentHashMap.newKeySet());
            if (voters.contains(userId)) {
                log.debug(I18N.get("wishlist.already_voted"), userId, wishId);
                return false;
            }

            // 添加投票 (Add vote)
            voters.add(userId);
            wish.setVoteCount(voters.size());

            log.info(I18N.get("wishlist.voted"), userId, wishId, wish.getVoteCount());

            // 检查是否达到开发阈值 (Check if reached development threshold)
            checkDevelopmentThreshold(wish);

            return true;

        } catch (Exception e) {
            log.error(I18N.get("wishlist.vote_failed"), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否达到开发阈值 (Check development threshold)
     */
    private void checkDevelopmentThreshold(WishItem wish) {
        if (wish.getVoteCount() >= developmentThreshold
            && wish.getStatus() == WishStatus.SUBMITTED) {

            wish.setStatus(WishStatus.IN_PLAN);
            log.info(I18N.get("wishlist.entered_plan"),
                wish.getTitle(), wish.getVoteCount());

            // TODO: 发送通知给投票用户
        }
    }

    /**
     * 取消投票 (Cancel vote)
     */
    public boolean cancelVote(String wishId, String userId) {
        Set<String> voters = votes.get(wishId);
        if (voters != null && voters.remove(userId)) {
            WishItem wish = wishes.get(wishId);
            if (wish != null) {
                wish.setVoteCount(voters.size());
                log.info(I18N.get("wishlist.vote_cancelled"), userId, wishId);
                return true;
            }
        }
        return false;
    }

    // ========== 排行榜 (Ranking) ==========

    /**
     * 获取投票排行榜 (Get vote ranking)
     *
     * @param limit 限制数量 (Limit)
     * @return 排行榜 (Ranking list)
     */
    public List<WishItem> getTopWishes(int limit) {
        return wishes.values().stream()
            .sorted((a, b) -> Integer.compare(b.getVoteCount(), a.getVoteCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取待开发愿望 (Get wishes in plan)
     */
    public List<WishItem> getWishesInPlan() {
        return wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.IN_PLAN)
            .sorted((a, b) -> Integer.compare(b.getVoteCount(), a.getVoteCount()))
            .collect(Collectors.toList());
    }

    /**
     * 获取已实现愿望 (Get implemented wishes)
     */
    public List<WishItem> getImplementedWishes() {
        return wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.IMPLEMENTED)
            .sorted((a, b) -> b.getImplementTime().compareTo(a.getImplementTime()))
            .collect(Collectors.toList());
    }

    // ========== 开发计划 (Development Plan) ==========

    /**
     * 标记为开发中 (Mark as in development)
     */
    public void startDevelopment(String wishId) {
        WishItem wish = wishes.get(wishId);
        if (wish != null) {
            wish.setStatus(WishStatus.IN_DEVELOPMENT);
            wish.setDevelopmentStartTime(LocalDateTime.now());

            log.info(I18N.get("wishlist.dev_started"), wish.getTitle());

            // TODO: 通知投票用户
        }
    }

    /**
     * 标记为已实现 (Mark as implemented)
     */
    public void markAsImplemented(String wishId) {
        WishItem wish = wishes.get(wishId);
        if (wish != null) {
            wish.setStatus(WishStatus.IMPLEMENTED);
            wish.setImplementTime(LocalDateTime.now());

            log.info(I18N.get("wishlist.implemented"), wish.getTitle());

            // TODO: 通知投票用户并奖励积分
            notifyVoters(wishId);
        }
    }

    /**
     * 通知投票用户 (Notify voters)
     */
    private void notifyVoters(String wishId) {
        Set<String> voters = votes.get(wishId);
        if (voters != null) {
            log.info(I18N.get("wishlist.notifying_voters"), voters.size());
            // TODO: 实现实际的通知逻辑
        }
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取愿望单统计 (Get wish list statistics)
     */
    public WishListStats getStats() {
        WishListStats stats = new WishListStats();
        stats.setTotalWishes(wishes.size());

        long submitted = wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.SUBMITTED).count();
        long inPlan = wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.IN_PLAN).count();
        long inDev = wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.IN_DEVELOPMENT).count();
        long implemented = wishes.values().stream()
            .filter(w -> w.getStatus() == WishStatus.IMPLEMENTED).count();

        stats.setSubmittedCount((int) submitted);
        stats.setInPlanCount((int) inPlan);
        stats.setInDevelopmentCount((int) inDev);
        stats.setImplementedCount((int) implemented);

        // 计算总投票数 (Calculate total votes)
        int totalVotes = votes.values().stream()
            .mapToInt(Set::size)
            .sum();
        stats.setTotalVotes(totalVotes);

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 愿望条目 (Wish Item)
     */
    @Data
    public static class WishItem {
        private String wishId;                      // 愿望ID
        private String submitterId;                 // 提交者ID
        private String title;                       // 标题
        private String description;                 // 描述
        private WishStatus status;                  // 状态
        private int voteCount;                      // 投票数
        private LocalDateTime submitTime;           // 提交时间
        private LocalDateTime developmentStartTime; // 开发开始时间
        private LocalDateTime implementTime;        // 实现时间
    }

    /**
     * 愿望状态 (Wish Status)
     */
    public enum WishStatus {
        SUBMITTED("submitted", "已提交", "Submitted"),
        IN_PLAN("in_plan", "计划中", "In Plan"),
        IN_DEVELOPMENT("in_development", "开发中", "In Development"),
        IMPLEMENTED("implemented", "已实现", "Implemented"),
        REJECTED("rejected", "已拒绝", "Rejected");

        private final String code;
        private final String nameCn;
        private final String nameEn;

        WishStatus(String code, String nameCn, String nameEn) {
            this.code = code;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

        public String getCode() { return code; }
        public String getNameCn() { return nameCn; }
        public String getNameEn() { return nameEn; }
    }

    /**
     * 愿望单统计 (Wish List Statistics)
     */
    @Data
    public static class WishListStats {
        private int totalWishes;            // 总愿望数
        private int submittedCount;         // 已提交数
        private int inPlanCount;            // 计划中数
        private int inDevelopmentCount;     // 开发中数
        private int implementedCount;       // 已实现数
        private int totalVotes;             // 总投票数
    }
}

