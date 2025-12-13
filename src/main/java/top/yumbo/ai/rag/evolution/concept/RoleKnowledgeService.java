package top.yumbo.ai.rag.evolution.concept;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 角色知识库管理服务 (Role Knowledge Base Management Service)
 *
 * 管理不同角色的专属知识视图，支持按角色提取和查询概念
 * (Manage role-specific knowledge views, support role-based concept extraction and query)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class RoleKnowledgeService {

    private static final String ROLE_KB_DIR = "./data/evolution/role-knowledge";
    private final ObjectMapper objectMapper;
    private final Path roleKBPath;

    // 内存缓存：角色名 -> 知识库 (In-memory cache: role name -> knowledge base)
    private final Map<String, RoleKnowledgeBase> roleKBCache = new ConcurrentHashMap<>();

    // 概念存储：概念ID -> 概念 (Concept storage: concept ID -> concept)
    private final Map<String, MinimalConcept> conceptCache = new ConcurrentHashMap<>();

    private final HOPEConceptExtractor conceptExtractor;

    @Autowired
    public RoleKnowledgeService(@Autowired(required = false) HOPEConceptExtractor conceptExtractor) {
        this.conceptExtractor = conceptExtractor;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.roleKBPath = Paths.get(ROLE_KB_DIR);

        initStorageDirectory();
        loadRoleKnowledgeBases();
        initializeDefaultRoles();
    }

    /**
     * 初始化存储目录 (Initialize storage directory)
     */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(roleKBPath);
            Files.createDirectories(roleKBPath.resolve("concepts"));
            Files.createDirectories(roleKBPath.resolve("roles"));
            log.info(I18N.get("log.evolution.role_kb_dir_created"), roleKBPath);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.role_kb_dir_failed"), e);
        }
    }

    /**
     * 加载角色知识库 (Load role knowledge bases)
     */
    private void loadRoleKnowledgeBases() {
        try {
            Path rolesDir = roleKBPath.resolve("roles");
            if (!Files.exists(rolesDir)) {
                return;
            }

            try (var paths = Files.walk(rolesDir, 1)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadRoleKB);
            }

            log.info(I18N.get("log.evolution.role_kb_loaded"), roleKBCache.size());
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.role_kb_load_failed"), e);
        }

        // 加载概念 (Load concepts)
        loadConcepts();
    }

    /**
     * 加载单个角色知识库 (Load single role knowledge base)
     */
    private void loadRoleKB(Path file) {
        try {
            RoleKnowledgeBase kb = objectMapper.readValue(file.toFile(), RoleKnowledgeBase.class);
            roleKBCache.put(kb.getRoleName(), kb);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.role_kb_file_load_failed", file), e);
        }
    }

    /**
     * 加载概念 (Load concepts)
     */
    private void loadConcepts() {
        try {
            Path conceptsDir = roleKBPath.resolve("concepts");
            if (!Files.exists(conceptsDir)) {
                return;
            }

            try (var paths = Files.walk(conceptsDir, 1)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadConcept);
            }

            log.info(I18N.get("log.evolution.concepts_loaded"), conceptCache.size());
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.concepts_load_failed"), e);
        }
    }

    /**
     * 加载单个概念 (Load single concept)
     */
    private void loadConcept(Path file) {
        try {
            MinimalConcept concept = objectMapper.readValue(file.toFile(), MinimalConcept.class);
            conceptCache.put(concept.getId(), concept);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.concept_load_failed", file), e);
        }
    }

    /**
     * 初始化默认角色 (Initialize default roles)
     */
    private void initializeDefaultRoles() {
        for (RoleKnowledgeBase.RoleType roleType : RoleKnowledgeBase.RoleType.values()) {
            if (!roleKBCache.containsKey(roleType.getCode())) {
                createRoleKnowledgeBase(roleType);
            }
        }
    }

    /**
     * 创建角色知识库 (Create role knowledge base)
     */
    public RoleKnowledgeBase createRoleKnowledgeBase(RoleKnowledgeBase.RoleType roleType) {
        RoleKnowledgeBase kb = RoleKnowledgeBase.builder()
            .roleName(roleType.getCode())
            .roleDescription(roleType.getZhName())
            .conceptIds(new ArrayList<>())
            .conceptWeights(new HashMap<>())
            .focusedTypes(getFocusedTypesForRole(roleType))
            .priorityTags(getPriorityTagsForRole(roleType))
            .stats(RoleKnowledgeBase.KnowledgeStats.builder()
                .totalConcepts(0)
                .highConfidenceConcepts(0)
                .recentUpdates(0)
                .averageConfidence(0.0)
                .countsByType(new HashMap<>())
                .countsByLayer(new HashMap<>())
                .build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        roleKBCache.put(roleType.getCode(), kb);
        saveRoleKB(kb);

        log.info(I18N.get("log.evolution.role_kb_created"), roleType.getCode());
        return kb;
    }

    /**
     * 从 HOPE 提取并分配概念到角色 (Extract from HOPE and assign concepts to roles)
     */
    public Map<String, Integer> extractAndAssignConcepts() {
        if (conceptExtractor == null) {
            log.warn("Concept extractor not available");
            return Collections.emptyMap();
        }

        Map<String, Integer> assignmentCount = new HashMap<>();

        try {
            // 提取所有概念 (Extract all concepts)
            List<MinimalConcept> allConcepts = conceptExtractor.extractAllConcepts();

            // 保存概念 (Save concepts)
            for (MinimalConcept concept : allConcepts) {
                conceptCache.put(concept.getId(), concept);
                saveConcept(concept);
            }

            // 分配概念到角色 (Assign concepts to roles)
            for (MinimalConcept concept : allConcepts) {
                List<String> roles = concept.getRoles();
                if (roles == null || roles.isEmpty()) {
                    roles = List.of("developer"); // 默认角色 (Default role)
                }

                for (String roleName : roles) {
                    assignConceptToRole(concept.getId(), roleName, calculateWeight(concept));
                    assignmentCount.put(roleName, assignmentCount.getOrDefault(roleName, 0) + 1);
                }
            }

            // 更新统计 (Update statistics)
            updateAllStats();

            log.info(I18N.get("log.evolution.concepts_assigned"),
                allConcepts.size(), assignmentCount);

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.concepts_assign_failed"), e);
        }

        return assignmentCount;
    }

    /**
     * 分配概念到角色 (Assign concept to role)
     */
    public void assignConceptToRole(String conceptId, String roleName, double weight) {
        RoleKnowledgeBase kb = roleKBCache.get(roleName);
        if (kb == null) {
            // 如果角色不存在，创建一个 (Create if role doesn't exist)
            RoleKnowledgeBase.RoleType roleType = RoleKnowledgeBase.RoleType.fromCode(roleName);
            if (roleType == null) {
                log.warn("Unknown role: {}", roleName);
                return;
            }
            kb = createRoleKnowledgeBase(roleType);
        }

        // 添加概念ID (Add concept ID)
        if (!kb.getConceptIds().contains(conceptId)) {
            kb.getConceptIds().add(conceptId);
        }

        // 设置权重 (Set weight)
        kb.getConceptWeights().put(conceptId, weight);
        kb.setUpdatedAt(LocalDateTime.now());

        saveRoleKB(kb);
    }

    /**
     * 获取角色的概念列表 (Get concepts for role)
     */
    public List<MinimalConcept> getConceptsForRole(String roleName) {
        RoleKnowledgeBase kb = roleKBCache.get(roleName);
        if (kb == null) {
            return Collections.emptyList();
        }

        return kb.getConceptIds().stream()
            .map(conceptCache::get)
            .filter(Objects::nonNull)
            .sorted((a, b) -> {
                // 按权重和置信度排序 (Sort by weight and confidence)
                double weightA = kb.getConceptWeights().getOrDefault(a.getId(), 0.5);
                double weightB = kb.getConceptWeights().getOrDefault(b.getId(), 0.5);
                double scoreA = weightA * a.getConfidence();
                double scoreB = weightB * b.getConfidence();
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }

    /**
     * 按类型获取角色的概念 (Get concepts for role by type)
     */
    public List<MinimalConcept> getConceptsForRoleByType(String roleName,
                                                         MinimalConcept.ConceptType type) {
        return getConceptsForRole(roleName).stream()
            .filter(c -> c.getType() == type)
            .collect(Collectors.toList());
    }

    /**
     * 按层级获取角色的概念 (Get concepts for role by layer)
     */
    public List<MinimalConcept> getConceptsForRoleByLayer(String roleName,
                                                          MinimalConcept.HOPELayer layer) {
        return getConceptsForRole(roleName).stream()
            .filter(c -> c.getSourceLayer() == layer)
            .collect(Collectors.toList());
    }

    /**
     * 搜索角色的概念 (Search concepts for role)
     */
    public List<MinimalConcept> searchConceptsForRole(String roleName, String keyword) {
        return getConceptsForRole(roleName).stream()
            .filter(c -> {
                String kw = keyword.toLowerCase();
                return c.getName().toLowerCase().contains(kw) ||
                       (c.getDescription() != null && c.getDescription().toLowerCase().contains(kw)) ||
                       (c.getTags() != null && c.getTags().stream()
                           .anyMatch(tag -> tag.toLowerCase().contains(kw)));
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取角色知识库 (Get role knowledge base)
     */
    public Optional<RoleKnowledgeBase> getRoleKnowledgeBase(String roleName) {
        return Optional.ofNullable(roleKBCache.get(roleName));
    }

    /**
     * 获取所有角色 (Get all roles)
     */
    public List<RoleKnowledgeBase> getAllRoles() {
        return new ArrayList<>(roleKBCache.values());
    }

    /**
     * 更新所有统计 (Update all statistics)
     */
    private void updateAllStats() {
        for (RoleKnowledgeBase kb : roleKBCache.values()) {
            updateStats(kb);
        }
    }

    /**
     * 更新统计 (Update statistics)
     */
    private void updateStats(RoleKnowledgeBase kb) {
        List<MinimalConcept> concepts = getConceptsForRole(kb.getRoleName());

        RoleKnowledgeBase.KnowledgeStats stats = RoleKnowledgeBase.KnowledgeStats.builder()
            .totalConcepts(concepts.size())
            .highConfidenceConcepts((int) concepts.stream()
                .filter(c -> c.getConfidence() >= 0.8)
                .count())
            .recentUpdates((int) concepts.stream()
                .filter(c -> c.getUpdatedAt() != null &&
                    c.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count())
            .averageConfidence(concepts.stream()
                .mapToDouble(MinimalConcept::getConfidence)
                .average()
                .orElse(0.0))
            .countsByType(concepts.stream()
                .collect(Collectors.groupingBy(
                    MinimalConcept::getType,
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().intValue())))
            .countsByLayer(concepts.stream()
                .collect(Collectors.groupingBy(
                    MinimalConcept::getSourceLayer,
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().intValue())))
            .build();

        kb.setStats(stats);
        saveRoleKB(kb);
    }

    /**
     * 保存角色知识库 (Save role knowledge base)
     */
    private void saveRoleKB(RoleKnowledgeBase kb) {
        try {
            Path file = roleKBPath.resolve("roles").resolve(kb.getRoleName() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), kb);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.role_kb_save_failed", kb.getRoleName()), e);
        }
    }

    /**
     * 保存概念 (Save concept)
     */
    private void saveConcept(MinimalConcept concept) {
        try {
            Path file = roleKBPath.resolve("concepts").resolve(concept.getId() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), concept);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.concept_save_failed", concept.getId()), e);
        }
    }

    // ============================================================================
    // 辅助方法 (Helper Methods)
    // ============================================================================

    /**
     * 获取角色关注的概念类型 (Get focused concept types for role)
     */
    private List<MinimalConcept.ConceptType> getFocusedTypesForRole(RoleKnowledgeBase.RoleType roleType) {
        return switch (roleType) {
            case DEVELOPER -> List.of(MinimalConcept.ConceptType.SKILL,
                    MinimalConcept.ConceptType.PROCESS,
                    MinimalConcept.ConceptType.FACT);
            case DEVOPS -> List.of(MinimalConcept.ConceptType.PROCESS,
                    MinimalConcept.ConceptType.SKILL,
                    MinimalConcept.ConceptType.RULE);
            case ARCHITECT -> List.of(MinimalConcept.ConceptType.DEFINITION,
                    MinimalConcept.ConceptType.RELATIONSHIP,
                    MinimalConcept.ConceptType.RULE);
            case RESEARCHER -> List.of(MinimalConcept.ConceptType.DEFINITION,
                    MinimalConcept.ConceptType.RELATIONSHIP,
                    MinimalConcept.ConceptType.FACT);
            default -> Arrays.asList(MinimalConcept.ConceptType.values());
        };
    }

    /**
     * 获取角色优先标签 (Get priority tags for role)
     */
    private List<String> getPriorityTagsForRole(RoleKnowledgeBase.RoleType roleType) {
        return switch (roleType) {
            case DEVELOPER -> List.of("code", "api", "implementation", "debugging");
            case DEVOPS -> List.of("deployment", "monitoring", "performance", "configuration");
            case ARCHITECT -> List.of("design", "architecture", "scalability", "pattern");
            case RESEARCHER -> List.of("algorithm", "theory", "paper", "principle");
            case PRODUCT_MANAGER -> List.of("requirement", "user", "feature", "scenario");
            default -> Collections.emptyList();
        };
    }

    /**
     * 计算概念权重 (Calculate concept weight)
     */
    private double calculateWeight(MinimalConcept concept) {
        double weight = concept.getConfidence();

        // 低频层权重高 (Higher weight for permanent layer)
        if (concept.getSourceLayer() == MinimalConcept.HOPELayer.PERMANENT) {
            weight *= 1.5;
        }

        // 访问次数影响 (Access count impact)
        if (concept.getAccessCount() != null && concept.getAccessCount() > 0) {
            weight += Math.min(concept.getAccessCount() / 100.0, 0.2);
        }

        return Math.min(weight, 2.0);
    }

    /**
     * 获取统计信息 (Get statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRoles", roleKBCache.size());
        stats.put("totalConcepts", conceptCache.size());
        stats.put("roleStats", roleKBCache.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getStats()
            )));
        return stats;
    }
}

