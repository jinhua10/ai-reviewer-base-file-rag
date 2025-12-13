package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;
import top.yumbo.ai.rag.role.detector.RoleDetectionResult;
import top.yumbo.ai.rag.role.detector.RoleDetector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理控制器 (Role Management Controller)
 *
 * 提供角色的增删改查、分页、搜索和检测功能
 * (Provides CRUD, pagination, search and detection functionality for roles)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired(required = false)
    private RoleManager roleManager;

    @Autowired(required = false)
    private RoleDetector roleDetector;

    /**
     * 角色查询请求参数 (Role query request parameters)
     */
    @Data
    public static class RoleQueryRequest {
        /** 页码（从1开始）(Page number, starts from 1) */
        private Integer page = 1;
        
        /** 每页大小 (Page size) */
        private Integer pageSize = 10;
        
        /** 搜索关键词 (Search keyword) */
        private String keyword;
        
        /** 是否启用过滤 (Enable filter) */
        private Boolean enabled;
        
        /** 标签过滤 (Tag filter) */
        private List<String> tags;
        
        /** 排序字段 (Sort field) */
        private String sortBy = "priority";
        
        /** 排序方向 (Sort order: asc/desc) */
        private String sortOrder = "desc";
    }

    /**
     * 角色分页响应 (Role pagination response)
     */
    @Data
    public static class RolePageResponse {
        /** 角色列表 (Role list) */
        private List<Role> list;
        
        /** 总数 (Total count) */
        private long total;
        
        /** 当前页 (Current page) */
        private int page;
        
        /** 每页大小 (Page size) */
        private int pageSize;
        
        /** 总页数 (Total pages) */
        private int totalPages;
    }

    /**
     * 获取角色列表（支持分页、搜索、过滤、排序）
     * (Get role list with pagination, search, filter and sort)
     *
     * @param request 查询请求 (Query request)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 分页结果 (Pagination result)
     */
    @GetMapping
    public ResponseEntity<RolePageResponse> getRoleList(
            RoleQueryRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            log.debug(I18N.get("role.query.start",
                    request.getKeyword(), request.getPage(), request.getPageSize()));

            // 获取所有角色 (Get all roles)
            List<Role> allRoles = roleManager != null ? roleManager.getAllRoles() : new ArrayList<>();
            
            // 搜索过滤 (Search filter)
            List<Role> filteredRoles = filterRoles(allRoles, request);
            
            // 排序 (Sort)
            filteredRoles = sortRoles(filteredRoles, request);
            
            // 分页 (Pagination)
            int page = Math.max(1, request.getPage());
            int pageSize = Math.max(1, Math.min(100, request.getPageSize()));
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, filteredRoles.size());
            
            List<Role> pagedRoles = startIndex < filteredRoles.size() 
                    ? filteredRoles.subList(startIndex, endIndex)
                    : new ArrayList<>();
            
            // 构建响应 (Build response)
            RolePageResponse response = new RolePageResponse();
            response.setList(pagedRoles);
            response.setTotal(filteredRoles.size());
            response.setPage(page);
            response.setPageSize(pageSize);
            response.setTotalPages((int) Math.ceil((double) filteredRoles.size() / pageSize));
            
            log.debug(I18N.get("role.query.success",
                    filteredRoles.size(), pagedRoles.size()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(I18N.get("role.query.failed"), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 过滤角色 (Filter roles)
     */
    private List<Role> filterRoles(List<Role> roles, RoleQueryRequest request) {
        return roles.stream()
                .filter(role -> {
                    // 关键词搜索 (Keyword search)
                    if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                        String keyword = request.getKeyword().toLowerCase();
                        return role.getName().toLowerCase().contains(keyword)
                                || (role.getDescription() != null && role.getDescription().toLowerCase().contains(keyword))
                                || (role.getKeywords() != null && role.getKeywords().stream()
                                        .anyMatch(k -> k.toLowerCase().contains(keyword)));
                    }
                    return true;
                })
                .filter(role -> {
                    // 启用状态过滤 (Enabled filter)
                    if (request.getEnabled() != null) {
                        return role.isEnabled() == request.getEnabled();
                    }
                    return true;
                })
                .filter(role -> {
                    // 标签过滤 (Tag filter)
                    if (request.getTags() != null && !request.getTags().isEmpty()) {
                        if (role.getTags() == null || role.getTags().isEmpty()) {
                            return false;
                        }
                        return request.getTags().stream()
                                .anyMatch(tag -> role.getTags().contains(tag));
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 排序角色 (Sort roles)
     */
    private List<Role> sortRoles(List<Role> roles, RoleQueryRequest request) {
        String sortBy = request.getSortBy();
        boolean ascending = "asc".equalsIgnoreCase(request.getSortOrder());
        
        Comparator<Role> comparator = switch (sortBy) {
            case "name" -> Comparator.comparing(Role::getName);
            case "priority" -> Comparator.comparingInt(Role::getPriority);
            case "weight" -> Comparator.comparingDouble(Role::getWeight);
            case "enabled" -> Comparator.comparing(Role::isEnabled);
            default -> Comparator.comparingInt(Role::getPriority);
        };
        
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return roles.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 获取角色详情 (Get role detail)
     *
     * @param id 角色ID (Role ID)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 角色详情 (Role detail)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleDetail(
            @PathVariable String id,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            if (roleManager == null) {
                return ResponseEntity.ok(createMockRole(id));
            }
            
            Role role = roleManager.getRole(id);
            if (role == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", I18N.getLang("role.update.notfound", lang, id)
                ));
            }
            
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error(I18N.get("role.query.failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", I18N.getLang("role.query.failed", lang)));
        }
    }

    /**
     * 创建角色 (Create role)
     *
     * @param role 角色信息 (Role info)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 创建的角色 (Created role)
     */
    @PostMapping
    public ResponseEntity<?> createRole(
            @RequestBody Role role,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            if (roleManager == null) {
                // Mock response for development
                role.setId(UUID.randomUUID().toString());
                role.setEnabled(true);
                role.setWeight(1.0);
                role.setPriority(0);
                log.info(I18N.get("role.create.mock"), role.getName());
                return ResponseEntity.ok(Map.of(
                        "data", role,
                        "message", I18N.getLang("role.create.success", lang, role.getName(), role.getId())
                ));
            }
            
            // 生成ID
            if (role.getId() == null || role.getId().isEmpty()) {
                role.setId(UUID.randomUUID().toString());
            }
            
            // 设置默认值
            if (role.getWeight() == 0) {
                role.setWeight(1.0);
            }
            
            // 添加角色
            roleManager.addRole(role);
            
            log.info(I18N.get("role.create.success"), role.getName(), role.getId());
            return ResponseEntity.ok(Map.of(
                    "data", role,
                    "message", I18N.getLang("role.create.success", lang, role.getName(), role.getId())
            ));
        } catch (Exception e) {
            log.error(I18N.get("role.create.failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", I18N.getLang("role.create.failed", lang)));
        }
    }

    /**
     * 更新角色 (Update role)
     *
     * @param id 角色ID (Role ID)
     * @param role 角色信息 (Role info)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 更新的角色 (Updated role)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(
            @PathVariable String id,
            @RequestBody Role role,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            if (roleManager == null) {
                // Mock response for development
                role.setId(id);
                log.info(I18N.get("role.update.mock"), role.getName());
                return ResponseEntity.ok(Map.of(
                        "data", role,
                        "message", I18N.getLang("role.update.success", lang, role.getName(), id)
                ));
            }
            
            Role existingRole = roleManager.getRole(id);
            if (existingRole == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", I18N.getLang("role.update.notfound", lang, id)
                ));
            }
            
            // 保持ID不变
            role.setId(id);
            
            // 更新角色
            roleManager.updateRole(role);
            
            log.info(I18N.get("role.update.success"), role.getName(), id);
            return ResponseEntity.ok(Map.of(
                    "data", role,
                    "message", I18N.getLang("role.update.success", lang, role.getName(), id)
            ));
        } catch (Exception e) {
            log.error(I18N.get("role.update.failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", I18N.getLang("role.update.failed", lang)));
        }
    }

    /**
     * 删除角色 (Delete role)
     *
     * @param id 角色ID (Role ID)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 删除结果 (Delete result)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(
            @PathVariable String id,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            if (roleManager == null) {
                // Mock response for development
                log.info(I18N.get("role.delete.mock"), id);
                return ResponseEntity.ok(Map.of(
                        "message", I18N.getLang("role.delete.success", lang, id)
                ));
            }
            
            // 删除角色
            roleManager.removeRole(id);
            
            log.info(I18N.get("role.delete.success"), id);
            return ResponseEntity.ok(Map.of(
                    "message", I18N.getLang("role.delete.success", lang, id)
            ));
        } catch (Exception e) {
            log.error(I18N.get("role.delete.failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", I18N.getLang("role.delete.failed", lang)));
        }
    }

    /**
     * 检测问题所属角色 (Detect question role)
     *
     * @param request 请求体包含问题 (Request body with question)
     * @param lang 语言代码 (Language code: zh/en)
     * @return 检测结果 (Detection result)
     */
    @PostMapping("/detect")
    public ResponseEntity<?> detectRole(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang) {
        try {
            String question = request.get("question");
            
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", I18N.getLang("role.detect.empty-question", lang)));
            }
            
            if (roleDetector == null) {
                // Mock response for development
                return ResponseEntity.ok(Map.of(
                        "roles", Collections.emptyList(),
                        "message", I18N.getLang("role.detect.mock", lang)
                ));
            }
            
            log.info(I18N.get("role.detect.start"), question);
            
            // 使用默认userId，如果需要可以从请求中获取
            RoleDetectionResult result = roleDetector.detect(question, "default");
            
            log.info(I18N.get("role.detect.success"), 
                    result.getSelectedRole().getName(), result.getConfidence());
            
            Map<String, Object> response = new HashMap<>();
            response.put("selectedRole", result.getSelectedRole());
            response.put("confidence", result.getConfidence());
            response.put("confidenceLevel", result.getConfidenceLevel());
            response.put("allCandidates", result.getAllCandidates());
            response.put("isDefault", result.isDefault());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(I18N.get("role.detect.failed"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", I18N.getLang("role.detect.failed", lang)));
        }
    }

    /**
     * 创建模拟角色 (Create mock role for development)
     */
    private Role createMockRole(String id) {
        return Role.builder()
                .id(id)
                .name("Mock Role")
                .description("Mock role for development")
                .keywords(new HashSet<>(Arrays.asList("test", "mock")))
                .weight(1.0)
                .enabled(true)
                .priority(0)
                .build();
    }
}
