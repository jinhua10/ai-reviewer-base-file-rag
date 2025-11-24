package top.yumbo.ai.rag.security;

import lombok.extern.slf4j.Slf4j;
import java.util.Set;

/**
 * 授权服务
 */
@Slf4j
public class AuthorizationService {
    
    /**
     * 检查权限
     */
    public boolean hasPermission(User user, String permission) {
        if (user == null) {
            return false;
        }
        
        Set<String> roles = user.getRoles();
        if (roles == null) {
            return false;
        }
        
        // 管理员拥有所有权限
        if (roles.contains("ADMIN")) {
            return true;
        }
        
        // 检查具体权限
        return switch (permission) {
            case "document:read" -> roles.contains("USER") || roles.contains("READER");
            case "document:write" -> roles.contains("USER") || roles.contains("WRITER");
            case "document:delete" -> roles.contains("USER") || roles.contains("ADMIN");
            case "admin:manage" -> roles.contains("ADMIN");
            default -> false;
        };
    }
    
    /**
     * 要求权限
     */
    public void requirePermission(User user, String permission) {
        if (!hasPermission(user, permission)) {
            throw new SecurityException("Permission denied: " + permission);
        }
    }
}