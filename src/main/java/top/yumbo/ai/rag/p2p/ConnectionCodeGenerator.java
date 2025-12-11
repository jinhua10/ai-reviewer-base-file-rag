package top.yumbo.ai.rag.p2p;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接码生成器 (Connection Code Generator)
 *
 * 功能 (Features):
 * 1. 生成唯一的连接码 (Generate unique connection codes)
 * 2. 连接码验证 (Connection code validation)
 * 3. 一次性使用 (One-time use)
 * 4. 有效期管理 (Expiration management)
 *
 * 连接码格式 (Code Format): ABC-XYZ-123
 * - 6位字母 - 6位字母 - 3位数字
 * - 有效期: 10分钟
 * - 一次性使用
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class ConnectionCodeGenerator {

    /**
     * 连接码存储 (Connection code storage)
     * Key: connectionCode, Value: ConnectionCodeInfo
     */
    private final Map<String, ConnectionCodeInfo> codeRegistry = new ConcurrentHashMap<>();

    /**
     * 有效期（毫秒） (Validity period in milliseconds)
     */
    private long validityPeriod = 10 * 60 * 1000; // 10分钟 (10 minutes)

    /**
     * 随机数生成器 (Random number generator)
     */
    private final SecureRandom random = new SecureRandom();

    // ========== 连接码生成 (Code Generation) ==========

    /**
     * 生成连接码 (Generate connection code)
     *
     * @param userId 用户ID (User ID)
     * @param metadata 元数据（可选） (Metadata, optional)
     * @return 连接码 (Connection code)
     */
    public String generateCode(String userId, Map<String, Object> metadata) {
        try {
            // 生成连接码 (Generate code)
            String code = generateRandomCode();

            // 确保唯一性 (Ensure uniqueness)
            while (codeRegistry.containsKey(code)) {
                code = generateRandomCode();
            }

            // 创建连接码信息 (Create code info)
            ConnectionCodeInfo info = new ConnectionCodeInfo();
            info.setCode(code);
            info.setUserId(userId);
            info.setMetadata(metadata);
            info.setCreateTime(LocalDateTime.now());
            info.setExpiryTime(LocalDateTime.now().plusMinutes(10));
            info.setUsed(false);

            // 存储 (Store)
            codeRegistry.put(code, info);

            log.info(I18N.get("p2p.code.generated"), code, userId);

            // 启动清理任务 (Start cleanup task)
            scheduleCleanup(code);

            return code;

        } catch (Exception e) {
            log.error(I18N.get("p2p.code.generate_failed"), userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成随机连接码 (Generate random code)
     * 格式: ABC-XYZ-123
     */
    private String generateRandomCode() {
        String part1 = generateRandomLetters(6);  // 6位字母
        String part2 = generateRandomLetters(6);  // 6位字母
        String part3 = generateRandomDigits(3);   // 3位数字

        return String.format("%s-%s-%s", part1, part2, part3);
    }

    /**
     * 生成随机字母 (Generate random letters)
     */
    private String generateRandomLetters(int length) {
        StringBuilder sb = new StringBuilder();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }

        return sb.toString();
    }

    /**
     * 生成随机数字 (Generate random digits)
     */
    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    // ========== 连接码验证 (Code Validation) ==========

    /**
     * 验证连接码 (Validate connection code)
     *
     * @param code 连接码 (Connection code)
     * @return 是否有效 (Is valid)
     */
    public boolean validateCode(String code) {
        ConnectionCodeInfo info = codeRegistry.get(code);

        if (info == null) {
            log.warn(I18N.get("p2p.code.not_found"), code);
            return false;
        }

        if (info.isUsed()) {
            log.warn(I18N.get("p2p.code.already_used"), code);
            return false;
        }

        if (info.isExpired()) {
            log.warn(I18N.get("p2p.code.expired"), code);
            codeRegistry.remove(code);
            return false;
        }

        return true;
    }

    /**
     * 使用连接码 (Use connection code)
     *
     * @param code 连接码 (Connection code)
     * @return 连接码信息，无效返回 null (Code info, null if invalid)
     */
    public ConnectionCodeInfo useCode(String code) {
        if (!validateCode(code)) {
            return null;
        }

        ConnectionCodeInfo info = codeRegistry.get(code);
        if (info != null) {
            // 标记为已使用 (Mark as used)
            info.setUsed(true);
            info.setUsedTime(LocalDateTime.now());

            log.info(I18N.get("p2p.code.used"), code, info.getUserId());
        }

        return info;
    }

    /**
     * 撤销连接码 (Revoke connection code)
     *
     * @param code 连接码 (Connection code)
     */
    public void revokeCode(String code) {
        ConnectionCodeInfo info = codeRegistry.remove(code);
        if (info != null) {
            log.info(I18N.get("p2p.code.revoked"), code);
        }
    }

    // ========== 清理管理 (Cleanup Management) ==========

    /**
     * 调度清理任务 (Schedule cleanup task)
     */
    private void scheduleCleanup(String code) {
        new Thread(() -> {
            try {
                Thread.sleep(validityPeriod);

                // 清理过期的连接码 (Clean up expired code)
                ConnectionCodeInfo info = codeRegistry.get(code);
                if (info != null && !info.isUsed()) {
                    codeRegistry.remove(code);
                    log.debug(I18N.get("p2p.code.cleaned_up"), code);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 清理所有过期连接码 (Clean up all expired codes)
     */
    public void cleanupExpiredCodes() {
        int cleanedCount = 0;

        for (var entry : codeRegistry.entrySet()) {
            if (entry.getValue().isExpired()) {
                codeRegistry.remove(entry.getKey());
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info(I18N.get("p2p.code.batch_cleaned"), cleanedCount);
        }
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取连接码统计 (Get code statistics)
     */
    public CodeStats getStats() {
        CodeStats stats = new CodeStats();
        stats.setTotalCodes(codeRegistry.size());

        int usedCount = 0;
        int expiredCount = 0;

        for (ConnectionCodeInfo info : codeRegistry.values()) {
            if (info.isUsed()) {
                usedCount++;
            }
            if (info.isExpired()) {
                expiredCount++;
            }
        }

        stats.setUsedCodes(usedCount);
        stats.setExpiredCodes(expiredCount);
        stats.setActiveCodes(codeRegistry.size() - usedCount - expiredCount);

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 连接码信息 (Connection Code Info)
     */
    @Data
    public static class ConnectionCodeInfo {
        private String code;                    // 连接码 (Code)
        private String userId;                  // 创建者用户ID (Creator user ID)
        private Map<String, Object> metadata;   // 元数据 (Metadata)
        private LocalDateTime createTime;       // 创建时间 (Create time)
        private LocalDateTime expiryTime;       // 过期时间 (Expiry time)
        private boolean used;                   // 是否已使用 (Is used)
        private LocalDateTime usedTime;         // 使用时间 (Used time)

        /**
         * 是否已过期 (Is expired)
         */
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    /**
     * 连接码统计 (Code Statistics)
     */
    @Data
    public static class CodeStats {
        private int totalCodes;     // 总连接码数
        private int activeCodes;    // 活跃连接码数
        private int usedCodes;      // 已使用连接码数
        private int expiredCodes;   // 过期连接码数
    }
}

