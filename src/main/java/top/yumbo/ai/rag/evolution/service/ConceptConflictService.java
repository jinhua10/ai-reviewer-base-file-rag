package top.yumbo.ai.rag.evolution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.evolution.model.ConceptConflict;
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
 * 概念冲突服务 (Concept Conflict Service)
 *
 * 负责管理概念冲突的检测、存储和查询
 * (Responsible for managing detection, storage, and querying of concept conflicts)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class ConceptConflictService {

    private static final String CONFLICTS_DIR = "./data/evolution/conflicts";
    private final ObjectMapper objectMapper;
    private final Path conflictsPath;

    // 内存缓存 (In-memory cache)
    private final Map<String, ConceptConflict> conflictCache = new ConcurrentHashMap<>();

    public ConceptConflictService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.conflictsPath = Paths.get(CONFLICTS_DIR);
        initStorageDirectory();
        loadConflictsFromDisk();
    }

    /**
     * 初始化存储目录 (Initialize storage directory)
     */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(conflictsPath);
            log.info(I18N.get("log.evolution.conflict_dir_created"), conflictsPath);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.conflict_dir_failed"), e);
        }
    }

    /**
     * 从磁盘加载冲突数据 (Load conflicts from disk)
     */
    private void loadConflictsFromDisk() {
        try {
            if (!Files.exists(conflictsPath)) {
                return;
            }

            try (var paths = Files.walk(conflictsPath, 1)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadConflict);
            }

            log.info(I18N.get("log.evolution.conflicts_loaded"), conflictCache.size());
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.conflicts_load_failed"), e);
        }
    }

    /**
     * 加载单个冲突 (Load single conflict)
     */
    private void loadConflict(Path file) {
        try {
            ConceptConflict conflict = objectMapper.readValue(file.toFile(), ConceptConflict.class);
            conflictCache.put(conflict.getId(), conflict);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.conflict_load_failed", file), e);
        }
    }

    /**
     * 创建新冲突 (Create new conflict)
     */
    public ConceptConflict createConflict(String question, String conceptA, String conceptB,
                                         String sourceA, String sourceB) {
        ConceptConflict conflict = ConceptConflict.builder()
            .id("conflict-" + UUID.randomUUID())
            .question(question)
            .conceptA(conceptA)
            .conceptB(conceptB)
            .sourceA(sourceA)
            .sourceB(sourceB)
            .status(ConceptConflict.ConflictStatus.PENDING)
            .votes(new HashMap<>())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .confidenceScore(0.8) // 默认置信度 (Default confidence)
            .type(ConceptConflict.ConflictType.DEFINITION_MISMATCH)
            .build();

        // 初始化投票计数 (Initialize vote counts)
        conflict.getVotes().put("A", 0);
        conflict.getVotes().put("B", 0);

        conflictCache.put(conflict.getId(), conflict);
        saveConflict(conflict);

        log.info(I18N.get("log.evolution.conflict_created"), conflict.getId(), question);
        return conflict;
    }

    /**
     * 获取冲突 (Get conflict)
     */
    public Optional<ConceptConflict> getConflict(String conflictId) {
        return Optional.ofNullable(conflictCache.get(conflictId));
    }

    /**
     * 获取所有冲突 (Get all conflicts)
     */
    public List<ConceptConflict> getAllConflicts() {
        return new ArrayList<>(conflictCache.values());
    }

    /**
     * 按状态过滤冲突 (Filter conflicts by status)
     */
    public List<ConceptConflict> getConflictsByStatus(ConceptConflict.ConflictStatus status) {
        return conflictCache.values().stream()
            .filter(c -> c.getStatus() == status)
            .sorted(Comparator.comparing(ConceptConflict::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * 分页获取冲突 (Get conflicts with pagination)
     */
    public List<ConceptConflict> getConflictsPaged(String status, int page, int pageSize) {
        List<ConceptConflict> conflicts;

        if ("all".equals(status)) {
            conflicts = getAllConflicts();
        } else {
            try {
                ConceptConflict.ConflictStatus statusEnum =
                    ConceptConflict.ConflictStatus.valueOf(status.toUpperCase());
                conflicts = getConflictsByStatus(statusEnum);
            } catch (IllegalArgumentException e) {
                conflicts = getAllConflicts();
            }
        }

        // 排序 (Sort by creation time, newest first)
        conflicts.sort(Comparator.comparing(ConceptConflict::getCreatedAt).reversed());

        // 分页 (Pagination)
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, conflicts.size());

        if (start >= conflicts.size()) {
            return Collections.emptyList();
        }

        return conflicts.subList(start, end);
    }

    /**
     * 添加投票 (Add vote)
     */
    public boolean addVote(String conflictId, String choice) {
        Optional<ConceptConflict> conflictOpt = getConflict(conflictId);
        if (conflictOpt.isEmpty()) {
            return false;
        }

        ConceptConflict conflict = conflictOpt.get();
        conflict.addVote(choice);

        // 更新状态为投票中 (Update status to voting)
        if (conflict.getStatus() == ConceptConflict.ConflictStatus.PENDING) {
            conflict.setStatus(ConceptConflict.ConflictStatus.VOTING);
        }

        // 检查是否达到自动决策阈值 (Check if auto-decision threshold is reached)
        checkAutoResolve(conflict);

        saveConflict(conflict);

        log.info(I18N.get("log.evolution.vote_added"), choice, conflictId);
        return true;
    }

    /**
     * 检查是否自动解决冲突 (Check if conflict should be auto-resolved)
     */
    private void checkAutoResolve(ConceptConflict conflict) {
        int totalVotes = conflict.getTotalVotes();
        int votesA = conflict.getVotesForA();
        int votesB = conflict.getVotesForB();

        // 最少投票数阈值 (Minimum votes threshold)
        final int MIN_VOTES = 10;
        // 获胜比例阈值 (Winning ratio threshold)
        final double WIN_RATIO = 0.7;

        if (totalVotes >= MIN_VOTES) {
            double ratioA = (double) votesA / totalVotes;
            double ratioB = (double) votesB / totalVotes;

            if (ratioA >= WIN_RATIO) {
                resolveConflict(conflict, "A");
            } else if (ratioB >= WIN_RATIO) {
                resolveConflict(conflict, "B");
            }
        }
    }

    /**
     * 解决冲突 (Resolve conflict)
     */
    public void resolveConflict(ConceptConflict conflict, String choice) {
        conflict.setStatus(ConceptConflict.ConflictStatus.RESOLVED);
        conflict.setResolvedChoice(choice);
        conflict.setResolvedAt(LocalDateTime.now());
        conflict.setUpdatedAt(LocalDateTime.now());

        saveConflict(conflict);

        log.info(I18N.get("log.evolution.conflict_resolved"),
            conflict.getId(), choice, conflict.getTotalVotes());
    }

    /**
     * 手动解决冲突 (Manually resolve conflict)
     */
    public boolean resolveConflict(String conflictId, String choice) {
        Optional<ConceptConflict> conflictOpt = getConflict(conflictId);
        if (conflictOpt.isEmpty()) {
            return false;
        }

        resolveConflict(conflictOpt.get(), choice);
        return true;
    }

    /**
     * 保存冲突到磁盘 (Save conflict to disk)
     */
    private void saveConflict(ConceptConflict conflict) {
        try {
            Path file = conflictsPath.resolve(conflict.getId() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), conflict);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.conflict_save_failed", conflict.getId()), e);
        }
    }

    /**
     * 获取冲突统计 (Get conflict statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long total = conflictCache.size();
        long pending = getConflictsByStatus(ConceptConflict.ConflictStatus.PENDING).size();
        long voting = getConflictsByStatus(ConceptConflict.ConflictStatus.VOTING).size();
        long resolved = getConflictsByStatus(ConceptConflict.ConflictStatus.RESOLVED).size();

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("voting", voting);
        stats.put("resolved", resolved);
        stats.put("resolveRate", total > 0 ? (double) resolved / total : 0.0);

        return stats;
    }
}

