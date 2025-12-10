package top.yumbo.ai.rag.role;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * 角色配置类 (Role Configuration)
 *
 * 从 YAML 文件加载角色配置信息
 * (Load role configuration from YAML file)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "rag.role")
public class RoleConfig {

    /**
     * 角色配置文件路径 (Role config file path)
     */
    private String configPath = "config/roles.yml";

    /**
     * 是否启用角色系统 (Whether to enable role system)
     */
    private boolean enabled = true;

    /**
     * 默认角色ID (Default role ID)
     */
    private String defaultRole = "general";

    /**
     * 角色缓存 (Role cache)
     */
    private Map<String, Role> roles = new HashMap<>();

    /**
     * 初始化方法，加载角色配置 (Initialization method, load role config)
     */
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info(I18N.get("role.config.disabled"));
            return;
        }

        log.info(I18N.get("role.config.loading.path", configPath));
        loadRoles();
        log.info(I18N.get("role.config.loaded", roles.size()));
    }

    /**
     * 从 YAML 文件加载角色配置 (Load role config from YAML file)
     */
    @SuppressWarnings("unchecked")
    private void loadRoles() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configPath)) {
            if (input == null) {
                log.warn(I18N.get("role.config.file.notfound", configPath));
                loadDefaultRoles();
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            if (data == null || !data.containsKey("roles")) {
                log.warn(I18N.get("role.config.format.invalid"));
                loadDefaultRoles();
                return;
            }

            List<Map<String, Object>> roleList = (List<Map<String, Object>>) data.get("roles");

            for (Map<String, Object> roleData : roleList) {
                Role role = parseRole(roleData);
                if (role != null) {
                    roles.put(role.getId(), role);
                    log.debug(I18N.get("role.config.role.loaded", role.getId(), role.getName()));
                }
            }

        } catch (Exception e) {
            log.error(I18N.get("role.config.load.error", e.getMessage()), e);
            loadDefaultRoles();
        }
    }

    /**
     * 解析单个角色配置 (Parse single role config)
     */
    @SuppressWarnings("unchecked")
    private Role parseRole(Map<String, Object> roleData) {
        try {
            return Role.builder()
                    .id((String) roleData.get("id"))
                    .name((String) roleData.get("name"))
                    .description((String) roleData.get("description"))
                    .keywords(parseKeywords(roleData.get("keywords")))
                    .weight(parseDouble(roleData.get("weight"), 1.0))
                    .enabled(parseBoolean(roleData.get("enabled"), true))
                    .prompt((String) roleData.get("prompt"))
                    .tags(parseTags(roleData.get("tags")))
                    .indexPath((String) roleData.get("indexPath"))
                    .priority(parseInt(roleData.get("priority"), 0))
                    .build();
        } catch (Exception e) {
            log.error(I18N.get("role.config.role.parse.error", roleData.get("id"), e.getMessage()), e);
            return null;
        }
    }

    /**
     * 解析关键词集合 (Parse keywords set)
     */
    @SuppressWarnings("unchecked")
    private Set<String> parseKeywords(Object value) {
        if (value instanceof List) {
            return new HashSet<>((List<String>) value);
        }
        return new HashSet<>();
    }

    /**
     * 解析标签列表 (Parse tags list)
     */
    @SuppressWarnings("unchecked")
    private List<String> parseTags(Object value) {
        if (value instanceof List) {
            return new ArrayList<>((List<String>) value);
        }
        return new ArrayList<>();
    }

    /**
     * 解析 double 值 (Parse double value)
     */
    private double parseDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * 解析 int 值 (Parse int value)
     */
    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 解析 boolean 值 (Parse boolean value)
     */
    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * 加载默认角色配置 (Load default role config)
     */
    private void loadDefaultRoles() {
        log.info(I18N.get("role.config.loading.default"));

        // 默认通用角色 (Default general role)
        roles.put("general", Role.builder()
                .id("general")
                .name("通用角色 (General)")
                .description("处理通用问题 (Handle general questions)")
                .keywords(new HashSet<>(Arrays.asList("通用", "一般", "其他", "general")))
                .weight(1.0)
                .enabled(true)
                .prompt("你是一个通用AI助手")
                .tags(List.of("general"))
                .indexPath("data/vector-index/role_general.index")
                .priority(0)
                .build());

        log.info(I18N.get("role.config.default.loaded", roles.size()));
    }

    /**
     * 获取角色 (Get role by ID)
     */
    public Role getRole(String roleId) {
        return roles.get(roleId);
    }

    /**
     * 获取所有启用的角色 (Get all enabled roles)
     */
    public List<Role> getEnabledRoles() {
        return roles.values().stream()
                .filter(Role::isEnabled)
                .sorted(Comparator.comparingInt(Role::getPriority).reversed())
                .toList();
    }

    /**
     * 检查角色是否存在 (Check if role exists)
     */
    public boolean hasRole(String roleId) {
        return roles.containsKey(roleId);
    }
}

