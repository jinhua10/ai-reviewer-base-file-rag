package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;
import top.yumbo.ai.rag.role.detector.RoleDetectionResult;
import top.yumbo.ai.rag.role.detector.RoleDetector;

import java.util.*;

/**
 * 角色管理控制器 (Role Management Controller)
 *
 * 提供角色的增删改查和检测功能
 * (Provides CRUD and detection functionality for roles)
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
     * 获取角色列表 (Get role list)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoleList() {
        try {
            List<Role> roles = roleManager != null ? roleManager.getAllRoles() : new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("list", roles);
            response.put("total", roles.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get role list", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get role list: " + e.getMessage()));
        }
    }

    /**
     * 获取角色详情 (Get role detail)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleDetail(@PathVariable String id) {
        try {
            if (roleManager == null) {
                return ResponseEntity.ok(createMockRole(id));
            }
            
            Role role = roleManager.getRole(id);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Failed to get role detail", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get role detail: " + e.getMessage()));
        }
    }

    /**
     * 创建角色 (Create role)
     */
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        try {
            if (roleManager == null) {
                // Mock response for development
                role.setId(UUID.randomUUID().toString());
                role.setEnabled(true);
                role.setWeight(1.0);
                role.setPriority(0);
                log.info("Mock: Created role: {}", role.getName());
                return ResponseEntity.ok(role);
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
            
            log.info("Created role: {} ({})", role.getName(), role.getId());
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Failed to create role", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create role: " + e.getMessage()));
        }
    }

    /**
     * 更新角色 (Update role)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable String id, @RequestBody Role role) {
        try {
            if (roleManager == null) {
                // Mock response for development
                role.setId(id);
                log.info("Mock: Updated role: {}", role.getName());
                return ResponseEntity.ok(role);
            }
            
            Role existingRole = roleManager.getRole(id);
            if (existingRole == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 保持ID不变
            role.setId(id);
            
            // 更新角色
            roleManager.updateRole(role);
            
            log.info("Updated role: {} ({})", role.getName(), id);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Failed to update role", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update role: " + e.getMessage()));
        }
    }

    /**
     * 删除角色 (Delete role)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {
        try {
            if (roleManager == null) {
                // Mock response for development
                log.info("Mock: Deleted role: {}", id);
                return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
            }
            
            // 删除角色
            roleManager.removeRole(id);
            
            log.info("Deleted role: {}", id);
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete role", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete role: " + e.getMessage()));
        }
    }

    /**
     * 检测问题所属角色 (Detect question role)
     */
    @PostMapping("/detect")
    public ResponseEntity<?> detectRole(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question is required"));
            }
            
            if (roleDetector == null) {
                // Mock response for development
                return ResponseEntity.ok(Map.of(
                        "roles", Collections.emptyList(),
                        "message", "Role detector not available"
                ));
            }
            
            // 使用默认userId，如果需要可以从请求中获取
            RoleDetectionResult result = roleDetector.detect(question, "default");
            
            Map<String, Object> response = new HashMap<>();
            response.put("selectedRole", result.getSelectedRole());
            response.put("confidence", result.getConfidence());
            response.put("confidenceLevel", result.getConfidenceLevel());
            response.put("allCandidates", result.getAllCandidates());
            response.put("isDefault", result.isDefault());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to detect role", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to detect role: " + e.getMessage()));
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
