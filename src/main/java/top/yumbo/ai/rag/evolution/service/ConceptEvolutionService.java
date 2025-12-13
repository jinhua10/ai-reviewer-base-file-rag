package top.yumbo.ai.rag.evolution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.evolution.concept.HOPEConceptExtractor;
import top.yumbo.ai.rag.evolution.concept.MinimalConcept;
import top.yumbo.ai.rag.evolution.concept.RoleKnowledgeService;
import top.yumbo.ai.rag.evolution.model.ConceptEvolution;
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
 * 概念演化服务 (Concept Evolution Service)
 *
 * 负责管理概念定义的演化历史
 * (Responsible for managing the evolution history of concept definitions)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class ConceptEvolutionService {

    private static final String EVOLUTION_DIR = "./data/evolution/history";
    private final ObjectMapper objectMapper;
    private final Path evolutionPath;

    // 内存缓存：概念ID -> 演化记录列表 (In-memory cache: conceptId -> evolution records)
    private final Map<String, List<ConceptEvolution>> evolutionCache = new ConcurrentHashMap<>();

    // HOPE 概念提取器 (HOPE concept extractor)
    private final HOPEConceptExtractor conceptExtractor;

    // 角色知识服务 (Role knowledge service)
    private final RoleKnowledgeService roleKnowledgeService;

    @Autowired
    public ConceptEvolutionService(@Autowired(required = false) HOPEConceptExtractor conceptExtractor,
                                   @Autowired(required = false) RoleKnowledgeService roleKnowledgeService) {
        this.conceptExtractor = conceptExtractor;
        this.roleKnowledgeService = roleKnowledgeService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.evolutionPath = Paths.get(EVOLUTION_DIR);
        initStorageDirectory();
        loadEvolutionsFromDisk();
    }

    /**
     * 初始化存储目录 (Initialize storage directory)
     */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(evolutionPath);
            log.info(I18N.get("log.evolution.history_dir_created"), evolutionPath);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.history_dir_failed"), e);
        }
    }

    /**
     * 从磁盘加载演化数据 (Load evolutions from disk)
     */
    private void loadEvolutionsFromDisk() {
        try {
            if (!Files.exists(evolutionPath)) {
                return;
            }

            try (var paths = Files.walk(evolutionPath, 2)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadEvolution);
            }

            log.info(I18N.get("log.evolution.history_loaded"),
                evolutionCache.values().stream().mapToInt(List::size).sum());
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.history_load_failed"), e);
        }
    }

    /**
     * 加载单个演化记录 (Load single evolution)
     */
    private void loadEvolution(Path file) {
        try {
            ConceptEvolution evolution = objectMapper.readValue(file.toFile(), ConceptEvolution.class);
            evolutionCache.computeIfAbsent(evolution.getConceptId(), k -> new ArrayList<>())
                .add(evolution);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.evolution_load_failed", file), e);
        }
    }

    /**
     * 记录概念创建 (Record concept creation)
     */
    public ConceptEvolution recordCreation(String conceptId, String content, String author) {
        ConceptEvolution evolution = ConceptEvolution.builder()
            .id(UUID.randomUUID().toString())
            .conceptId(conceptId)
            .version(1)
            .type(ConceptEvolution.EvolutionType.CREATED)
            .title("概念创建")
            .description("初始创建概念定义")
            .content(content)
            .author(author)
            .timestamp(LocalDateTime.now())
            .reason("初始创建")
            .confidence(1.0)
            .build();

        addEvolution(evolution);

        log.info(I18N.get("log.evolution.concept_created"), conceptId);
        return evolution;
    }

    /**
     * 记录概念更新 (Record concept update)
     */
    public ConceptEvolution recordUpdate(String conceptId, String newContent,
                                        String author, String reason) {
        List<ConceptEvolution> history = evolutionCache.get(conceptId);
        int nextVersion = history != null ? history.size() + 1 : 1;

        String oldContent = "";
        if (history != null && !history.isEmpty()) {
            oldContent = history.get(history.size() - 1).getContent();
        }

        Map<String, String> changes = new HashMap<>();
        changes.put("before", oldContent);
        changes.put("after", newContent);

        ConceptEvolution evolution = ConceptEvolution.builder()
            .id(UUID.randomUUID().toString())
            .conceptId(conceptId)
            .version(nextVersion)
            .type(ConceptEvolution.EvolutionType.UPDATED)
            .title("概念更新")
            .description("根据用户反馈优化定义")
            .content(newContent)
            .changes(changes)
            .author(author)
            .timestamp(LocalDateTime.now())
            .reason(reason)
            .confidence(0.9)
            .build();

        addEvolution(evolution);

        log.info(I18N.get("log.evolution.concept_updated"), conceptId, nextVersion);
        return evolution;
    }

    /**
     * 记录冲突解决 (Record conflict resolution)
     */
    public ConceptEvolution recordConflictResolution(String conceptId, String conflictId,
                                                    String winningContent, String losingContent,
                                                    String resolver, String reason) {
        List<ConceptEvolution> history = evolutionCache.get(conceptId);
        int nextVersion = history != null ? history.size() + 1 : 1;

        Map<String, String> changes = new HashMap<>();
        changes.put("before", losingContent);
        changes.put("after", winningContent);
        changes.put("conflictId", conflictId);

        ConceptEvolution evolution = ConceptEvolution.builder()
            .id(UUID.randomUUID().toString())
            .conceptId(conceptId)
            .version(nextVersion)
            .type(ConceptEvolution.EvolutionType.RESOLVED)
            .title("冲突解决")
            .description("通过社区投票确定最终版本")
            .content(winningContent)
            .changes(changes)
            .author(resolver)
            .timestamp(LocalDateTime.now())
            .reason(reason)
            .relatedConflictId(conflictId)
            .confidence(0.95)
            .build();

        addEvolution(evolution);

        log.info(I18N.get("log.evolution.conflict_resolved_history"),
            conceptId, conflictId, nextVersion);
        return evolution;
    }

    /**
     * 添加演化记录 (Add evolution record)
     */
    private void addEvolution(ConceptEvolution evolution) {
        evolutionCache.computeIfAbsent(evolution.getConceptId(), k -> new ArrayList<>())
            .add(evolution);
        saveEvolution(evolution);
    }

    /**
     * 获取概念的演化历史 (Get evolution history of a concept)
     */
    public List<ConceptEvolution> getEvolutionHistory(String conceptId) {
        List<ConceptEvolution> history = evolutionCache.get(conceptId);
        if (history == null) {
            return Collections.emptyList();
        }

        // 按版本号排序 (Sort by version)
        return history.stream()
            .sorted(Comparator.comparing(ConceptEvolution::getVersion))
            .collect(Collectors.toList());
    }

    /**
     * 获取最新版本 (Get latest version)
     */
    public Optional<ConceptEvolution> getLatestVersion(String conceptId) {
        List<ConceptEvolution> history = getEvolutionHistory(conceptId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.get(history.size() - 1));
    }

    /**
     * 获取特定版本 (Get specific version)
     */
    public Optional<ConceptEvolution> getVersion(String conceptId, int version) {
        List<ConceptEvolution> history = getEvolutionHistory(conceptId);
        return history.stream()
            .filter(e -> e.getVersion() == version)
            .findFirst();
    }

    /**
     * 保存演化记录到磁盘 (Save evolution to disk)
     */
    private void saveEvolution(ConceptEvolution evolution) {
        try {
            // 按概念ID创建子目录 (Create subdirectory by concept ID)
            Path conceptDir = evolutionPath.resolve(evolution.getConceptId());
            Files.createDirectories(conceptDir);

            // 文件名：版本号_演化ID (Filename: version_evolutionId)
            String filename = String.format("v%d_%s.json",
                evolution.getVersion(), evolution.getId());
            Path file = conceptDir.resolve(filename);

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), evolution);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.evolution_save_failed", evolution.getId()), e);
        }
    }

    /**
     * 获取演化统计 (Get evolution statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalConcepts = evolutionCache.size();
        int totalEvolutions = evolutionCache.values().stream()
            .mapToInt(List::size)
            .sum();

        // 统计各类型演化数量 (Count evolutions by type)
        Map<ConceptEvolution.EvolutionType, Long> typeCounts =
            evolutionCache.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                    ConceptEvolution::getType,
                    Collectors.counting()
                ));

        stats.put("totalConcepts", totalConcepts);
        stats.put("totalEvolutions", totalEvolutions);
        stats.put("typeCounts", typeCounts);
        stats.put("averageVersions", totalConcepts > 0 ?
            (double) totalEvolutions / totalConcepts : 0.0);

        return stats;
    }

    // ============================================================================
    // HOPE 集成方法 (HOPE Integration Methods)
    // ============================================================================

    /**
     * 从 HOPE 架构提取概念并初始化演化历史 (Extract concepts from HOPE and initialize evolution history)
     *
     * 这是知识演化的起点，从 HOPE 的三层架构中提取最小概念单元
     * (This is the starting point of knowledge evolution, extracting minimal concept units from HOPE's three layers)
     *
     * @return 提取的概念数量 (Number of extracted concepts)
     */
    public int initializeFromHOPE() {
        if (conceptExtractor == null) {
            log.warn(I18N.get("log.evolution.hope_extractor_not_available"));
            return 0;
        }

        int conceptCount = 0;

        try {
            log.info(I18N.get("log.evolution.hope_init_start"));

            // 1. 从低频层提取（高置信度种子概念）(Extract from permanent layer - high confidence seed concepts)
            List<MinimalConcept> permanentConcepts = conceptExtractor.extractFromPermanentLayer();
            for (MinimalConcept concept : permanentConcepts) {
                recordCreation(concept.getId(), concept.getDescription(), "HOPE-Permanent");
                conceptCount++;
            }

            // 2. 从中频层提取（候选概念）(Extract from ordinary layer - candidate concepts)
            List<MinimalConcept> ordinaryConcepts = conceptExtractor.extractFromOrdinaryLayer(4, 10);
            for (MinimalConcept concept : ordinaryConcepts) {
                recordCreation(concept.getId(), concept.getDescription(), "HOPE-Ordinary");
                conceptCount++;
            }

            // 3. 从高频层提取（新兴概念）(Extract from high frequency layer - emerging concepts)
            List<MinimalConcept> highFreqConcepts = conceptExtractor.extractFromHighFrequencyLayer(50);
            for (MinimalConcept concept : highFreqConcepts) {
                recordCreation(concept.getId(), concept.getDescription(), "HOPE-HighFreq");
                conceptCount++;
            }

            log.info(I18N.get("log.evolution.hope_init_complete"), conceptCount);

            // 4. 触发角色知识库更新 (Trigger role knowledge base update)
            if (roleKnowledgeService != null) {
                roleKnowledgeService.extractAndAssignConcepts();
            }

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.hope_init_failed"), e);
        }

        return conceptCount;
    }

    /**
     * 从冲突解决中提取新知识并更新角色知识库 (Extract new knowledge from conflict resolution and update role knowledge bases)
     *
     * @param conflictId 冲突ID (Conflict ID)
     * @param winningContent 获胜的内容 (Winning content)
     * @param losingContent 失败的内容 (Losing content)
     * @param affectedRoles 受影响的角色列表 (List of affected roles)
     * @return 演化记录 (Evolution record)
     */
    public ConceptEvolution recordConflictResolutionWithRoles(String conceptId, String conflictId,
                                                               String winningContent, String losingContent,
                                                               String resolver, String reason,
                                                               List<String> affectedRoles) {
        // 记录标准的冲突解决 (Record standard conflict resolution)
        ConceptEvolution evolution = recordConflictResolution(
            conceptId, conflictId, winningContent, losingContent, resolver, reason
        );

        // 如果有角色知识服务，更新受影响角色的知识库 (If role knowledge service available, update affected roles)
        if (roleKnowledgeService != null && affectedRoles != null && !affectedRoles.isEmpty()) {
            try {
                // 为每个受影响的角色创建或更新概念 (Create or update concept for each affected role)
                MinimalConcept updatedConcept = MinimalConcept.builder()
                    .id(conceptId)
                    .name(extractConceptName(winningContent))
                    .description(winningContent)
                    .type(MinimalConcept.ConceptType.DEFINITION)
                    .roles(affectedRoles)
                    .confidence(0.9) // 通过投票解决的概念有高置信度 (High confidence for voted concepts)
                    .sourceLayer(MinimalConcept.HOPELayer.UNKNOWN)
                    .sourceDocument("Conflict-Resolution")
                    .version(evolution.getVersion())
                    .createdAt(evolution.getTimestamp())
                    .updatedAt(evolution.getTimestamp())
                    .build();

                // 为每个角色分配此概念 (Assign this concept to each role)
                for (String role : affectedRoles) {
                    roleKnowledgeService.assignConceptToRole(conceptId, role, 1.0);
                }

                log.info(I18N.get("log.evolution.role_concepts_updated"),
                    conceptId, affectedRoles);

            } catch (Exception e) {
                log.error(I18N.get("log.evolution.role_concepts_update_failed"), e);
            }
        }

        return evolution;
    }

    /**
     * 获取特定角色的演化历史 (Get evolution history for specific role)
     *
     * @param roleName 角色名称 (Role name)
     * @param limit 限制数量 (Limit)
     * @return 演化记录列表 (List of evolution records)
     */
    public List<ConceptEvolution> getEvolutionHistoryForRole(String roleName, int limit) {
        if (roleKnowledgeService == null) {
            return Collections.emptyList();
        }

        try {
            // 获取该角色的所有概念 (Get all concepts for this role)
            List<MinimalConcept> roleConcepts = roleKnowledgeService.getConceptsForRole(roleName);

            // 获取这些概念的演化历史 (Get evolution history for these concepts)
            return roleConcepts.stream()
                .flatMap(concept -> getEvolutionHistory(concept.getId()).stream())
                .sorted(Comparator.comparing(ConceptEvolution::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.role_history_failed", roleName), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取角色的新知识统计 (Get new knowledge statistics for role)
     *
     * @param roleName 角色名称 (Role name)
     * @param days 最近天数 (Recent days)
     * @return 统计信息 (Statistics)
     */
    public Map<String, Object> getNewKnowledgeStatsForRole(String roleName, int days) {
        Map<String, Object> stats = new HashMap<>();

        if (roleKnowledgeService == null) {
            return stats;
        }

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            List<ConceptEvolution> recentEvolutions = getEvolutionHistoryForRole(roleName, 1000);

            List<ConceptEvolution> newEvolutions = recentEvolutions.stream()
                .filter(e -> e.getTimestamp() != null && e.getTimestamp().isAfter(cutoffDate))
                .toList();

            Map<ConceptEvolution.EvolutionType, Long> typeCount = newEvolutions.stream()
                .collect(Collectors.groupingBy(
                    ConceptEvolution::getType,
                    Collectors.counting()
                ));

            stats.put("roleName", roleName);
            stats.put("days", days);
            stats.put("totalNewKnowledge", newEvolutions.size());
            stats.put("byType", typeCount);
            stats.put("timeRange", Map.of(
                "from", cutoffDate,
                "to", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.role_stats_failed", roleName), e);
        }

        return stats;
    }

    /**
     * 提取概念名称 (Extract concept name from content)
     */
    private String extractConceptName(String content) {
        if (content == null || content.isEmpty()) {
            return "Unknown";
        }

        // 取第一句话或前50个字符 (Take first sentence or first 50 characters)
        String[] sentences = content.split("[。！？.!?]");
        if (sentences.length > 0 && sentences[0].length() <= 50) {
            return sentences[0].trim();
        }

        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }
}

