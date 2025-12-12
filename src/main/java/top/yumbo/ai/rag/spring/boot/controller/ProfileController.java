package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 个人中心控制器 (Profile Controller)
 *
 * 提供用户个人信息和统计相关的 API
 * (Provides user profile and statistics related API)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    /**
     * 获取用户信息 (Get user info)
     * GET /api/profile/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo() {
        log.info(I18N.get("profile.api.info_request"));

        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId("user-demo");
            userInfo.setNickname("演示用户");
            userInfo.setAvatar("https://ui-avatars.com/api/?name=Demo");
            userInfo.setBio("这是一个演示账号");
            userInfo.setEmail("demo@example.com");
            userInfo.setCreatedAt(LocalDateTime.now().minusMonths(6));
            userInfo.setLastLoginAt(LocalDateTime.now());

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.info_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 更新用户信息 (Update user info)
     * PUT /api/profile/info
     */
    @PutMapping("/info")
    public ResponseEntity<?> updateUserInfo(@RequestBody Map<String, String> request) {
        log.info(I18N.get("profile.api.update_request"));

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", I18N.get("profile.update.success"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.update_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取使用统计 (Get usage statistics)
     * GET /api/profile/{userId}/statistics
     */
    @GetMapping("/{userId}/statistics")
    public ResponseEntity<?> getUsageStatistics(@PathVariable String userId) {
        log.info(I18N.get("profile.api.stats_request"), userId);

        try {
            UsageStatistics stats = new UsageStatistics();
            stats.setTotalQuestions(156L);
            stats.setTotalDocuments(89L);
            stats.setTotalAnswers(142L);
            stats.setTotalFeedbacks(78L);

            Map<String, Long> questionsByRole = new HashMap<>();
            questionsByRole.put("developer", 80L);
            questionsByRole.put("designer", 45L);
            questionsByRole.put("manager", 31L);
            stats.setQuestionsByRole(questionsByRole);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.stats_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取贡献统计 (Get contributions)
     * GET /api/profile/{userId}/contributions
     */
    @GetMapping("/{userId}/contributions")
    public ResponseEntity<?> getContributions(@PathVariable String userId) {
        log.info(I18N.get("profile.api.contrib_request"), userId);

        try {
            Map<String, Object> contributions = new HashMap<>();
            contributions.put("documentsShared", 23);
            contributions.put("helpfulAnswers", 56);
            contributions.put("feedbackGiven", 78);
            contributions.put("commentsPosted", 34);

            return ResponseEntity.ok(contributions);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.contrib_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取成就列表 (Get achievements)
     * GET /api/profile/{userId}/achievements
     */
    @GetMapping("/{userId}/achievements")
    public ResponseEntity<?> getAchievements(@PathVariable String userId) {
        log.info(I18N.get("profile.api.achieve_request"), userId);

        try {
            List<Achievement> achievements = new ArrayList<>();

            Achievement a1 = new Achievement();
            a1.setId("first-question");
            a1.setName("首次提问");
            a1.setDescription("提出第一个问题");
            a1.setIcon("🎯");
            a1.setPoints(10);
            a1.setUnlocked(true);
            a1.setUnlockedAt(LocalDateTime.now().minusMonths(5));
            achievements.add(a1);

            Achievement a2 = new Achievement();
            a2.setId("doc-master");
            a2.setName("文档大师");
            a2.setDescription("上传 50 个文档");
            a2.setIcon("📚");
            a2.setPoints(50);
            a2.setUnlocked(true);
            a2.setUnlockedAt(LocalDateTime.now().minusMonths(2));
            achievements.add(a2);

            Achievement a3 = new Achievement();
            a3.setId("helper");
            a3.setName("乐于助人");
            a3.setDescription("获得 100 个有用反馈");
            a3.setIcon("❤️");
            a3.setPoints(100);
            a3.setUnlocked(false);
            achievements.add(a3);

            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.achieve_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 更新用户设置 (Update settings)
     * PUT /api/profile/settings
     */
    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info(I18N.get("profile.api.settings_request"));

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", I18N.get("profile.settings.updated"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("profile.api.settings_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // DTO 类 (DTO Classes)

    @Data
    public static class UserInfo {
        private String userId;
        private String nickname;
        private String avatar;
        private String bio;
        private String email;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
    }

    @Data
    public static class UsageStatistics {
        private Long totalQuestions;
        private Long totalDocuments;
        private Long totalAnswers;
        private Long totalFeedbacks;
        private Map<String, Long> questionsByRole;
    }

    @Data
    public static class Achievement {
        private String id;
        private String name;
        private String description;
        private String icon;
        private Integer points;
        private boolean unlocked;
        private LocalDateTime unlockedAt;
    }
}

