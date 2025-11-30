package top.yumbo.ai.rag.security;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务（Authentication service）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class AuthenticationService {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public AuthenticationService() {
    }

    /**
     * 注册用户
     */
    public User register(String username, String password, Set<String> roles) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.auth.user_exists"));
        }
        
        String passwordHash = hashPassword(password);
        User user = User.builder()
            .userId(generateUserId())
            .username(username)
            .passwordHash(passwordHash)
            .roles(roles)
            .enabled(true)
            .createdAt(System.currentTimeMillis())
            .build();
        
        users.put(username, user);
        log.info(LogMessageProvider.getMessage("log.auth.registered", username));
        return user;
    }
    
    /**
     * 用户登录
     */
    public String login(String username, String password) {
        User user = users.get(username);
        if (user == null || !user.isEnabled()) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.auth.invalid_credentials"));
        }
        
        String passwordHash = hashPassword(password);
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.auth.invalid_credentials"));
        }
        
        String token = generateToken();
        tokens.put(token, username);
        
        user.setLastLoginAt(System.currentTimeMillis());
        log.info(LogMessageProvider.getMessage("log.auth.logged_in", username));
        return token;
    }
    
    /**
     * 验证Token
     */
    public User validateToken(String token) {
        String username = tokens.get(token);
        if (username == null) {
            return null;
        }
        return users.get(username);
    }
    
    /**
     * 登出
     */
    public void logout(String token) {
        String username = tokens.remove(token);
        if (username != null) {
            log.info(LogMessageProvider.getMessage("log.auth.logged_out", username));
        }
    }
    
    /**
     * 哈希密码
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
    
    /**
     * 生成用户ID
     */
    private String generateUserId() {
        return "user_" + System.currentTimeMillis();
    }
    
    /**
     * 生成Token
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}