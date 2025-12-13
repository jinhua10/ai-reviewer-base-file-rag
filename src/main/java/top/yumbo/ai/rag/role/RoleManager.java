package top.yumbo.ai.rag.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 角色管理器 (Role Manager)
 *
 * 提供角色的管理和查询功能
 * (Provides role management and query functions)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class RoleManager {

    @Autowired
    RoleConfig roleConfig;  // package-private for testing

    /**
     * 角色使用统计 (Role usage statistics)
     */
    private final Map<String, Integer> roleUsageStats = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info(I18N.get("role.manager.initialized"));
    }

    /**
     * 获取角色 (Get role by ID)
     *
     * @param roleId 角色ID (Role ID)
     * @return 角色对象 (Role object)
     */
    public Role getRole(String roleId) {
        Role role = roleConfig.getRole(roleId);

        if (role == null) {
            log.warn(I18N.get("role.manager.role.notfound", roleId));
            // 返回默认角色 (Return default role)
            role = roleConfig.getRole(roleConfig.getDefaultRole());
        }

        return role;
    }

    /**
     * 获取所有启用的角色 (Get all enabled roles)
     *
     * @return 启用的角色列表 (List of enabled roles)
     */
    public List<Role> getEnabledRoles() {
        return roleConfig.getEnabledRoles();
    }

    /**
     * 检查角色是否存在 (Check if role exists)
     *
     * @param roleId 角色ID (Role ID)
     * @return 是否存在 (Whether exists)
     */
    public boolean hasRole(String roleId) {
        return roleConfig.hasRole(roleId);
    }

    /**
     * 记录角色使用 (Record role usage)
     *
     * @param roleId 角色ID (Role ID)
     */
    public void recordUsage(String roleId) {
        roleUsageStats.merge(roleId, 1, Integer::sum);
        log.debug(I18N.get("role.manager.usage.recorded", roleId, roleUsageStats.get(roleId)));
    }

    /**
     * 获取角色使用统计 (Get role usage statistics)
     *
     * @return 使用统计 Map (Usage statistics map)
     */
    public Map<String, Integer> getUsageStats() {
        return new ConcurrentHashMap<>(roleUsageStats);
    }

    /**
     * 获取默认角色 (Get default role)
     *
     * @return 默认角色 (Default role)
     */
    public Role getDefaultRole() {
        return getRole(roleConfig.getDefaultRole());
    }

    /**
     * 根据关键词查找相关角色 (Find related roles by keywords)
     *
     * @param keywords 关键词列表 (Keywords list)
     * @return 相关角色列表 (Related roles list)
     */
    public List<Role> findRolesByKeywords(List<String> keywords) {
        return getEnabledRoles().stream()
                .filter(role -> {
                    // 检查角色关键词是否包含任何输入关键词 (Check if role keywords contain any input keyword)
                    for (String keyword : keywords) {
                        if (role.getKeywords().contains(keyword.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    /**
     * 重新加载角色配置 (Reload role configuration)
     */
    public void reload() {
        log.info(I18N.get("role.manager.reloading"));
        roleConfig.init();
        log.info(I18N.get("role.manager.reloaded", roleConfig.getRoles().size()));
    }

    /**
     * 获取所有角色 (Get all roles)
     *
     * @return 所有角色列表 (List of all roles)
     */
    public List<Role> getAllRoles() {
        if (roleConfig == null || roleConfig.getRoles() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(roleConfig.getRoles().values());
    }

    /**
     * 添加角色 (Add role)
     *
     * @param role 角色对象 (Role object)
     */
    public void addRole(Role role) {
        if (roleConfig != null && roleConfig.getRoles() != null) {
            roleConfig.getRoles().put(role.getId(), role);
            log.info(I18N.get("role.manager.role.added", role.getId(), role.getName()));
        }
    }

    /**
     * 更新角色 (Update role)
     *
     * @param role 角色对象 (Role object)
     */
    public void updateRole(Role role) {
        if (roleConfig != null && roleConfig.getRoles() != null) {
            roleConfig.getRoles().put(role.getId(), role);
            log.info(I18N.get("role.manager.role.updated", role.getId(), role.getName()));
        }
    }

    /**
     * 删除角色 (Remove role)
     *
     * @param roleId 角色ID (Role ID)
     */
    public void removeRole(String roleId) {
        if (roleConfig != null && roleConfig.getRoles() != null) {
            Role removed = roleConfig.getRoles().remove(roleId);
            if (removed != null) {
                log.info(I18N.get("role.manager.role.removed", roleId, removed.getName()));
            }
        }
    }
}

