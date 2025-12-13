package top.yumbo.ai.rag.evolution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.evolution.model.ConceptConflict;
import top.yumbo.ai.rag.evolution.model.UserVote;
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
 * 投票服务 (Voting Service)
 *
 * 负责管理用户对概念冲突的投票
 * (Responsible for managing user votes on concept conflicts)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class VotingService {

    private static final String VOTES_DIR = "./data/evolution/votes";
    private final ObjectMapper objectMapper;
    private final Path votesPath;
    private final ConceptConflictService conflictService;
    private final ConceptEvolutionService evolutionService;

    // 内存缓存：用户ID+冲突ID -> 投票记录 (In-memory cache)
    private final Map<String, UserVote> voteCache = new ConcurrentHashMap<>();

    @Autowired
    public VotingService(ConceptConflictService conflictService,
                        ConceptEvolutionService evolutionService) {
        this.conflictService = conflictService;
        this.evolutionService = evolutionService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.votesPath = Paths.get(VOTES_DIR);
        initStorageDirectory();
        loadVotesFromDisk();
    }

    /**
     * 初始化存储目录 (Initialize storage directory)
     */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(votesPath);
            log.info(I18N.get("log.evolution.votes_dir_created"), votesPath);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.votes_dir_failed"), e);
        }
    }

    /**
     * 从磁盘加载投票数据 (Load votes from disk)
     */
    private void loadVotesFromDisk() {
        try {
            if (!Files.exists(votesPath)) {
                return;
            }

            Files.walk(votesPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadVote);

            log.info(I18N.get("log.evolution.votes_loaded"), voteCache.size());
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.votes_load_failed"), e);
        }
    }

    /**
     * 加载单个投票 (Load single vote)
     */
    private void loadVote(Path file) {
        try {
            UserVote vote = objectMapper.readValue(file.toFile(), UserVote.class);
            String key = vote.getUserId() + "_" + vote.getConflictId();
            voteCache.put(key, vote);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.vote_load_failed", file), e);
        }
    }

    /**
     * 提交投票 (Submit vote)
     */
    public UserVote submitVote(String conflictId, String userId, String choice,
                              String reason, String ipAddress) {
        // 检查冲突是否存在 (Check if conflict exists)
        Optional<ConceptConflict> conflictOpt = conflictService.getConflict(conflictId);
        if (conflictOpt.isEmpty()) {
            log.warn(I18N.get("log.evolution.conflict_not_found"), conflictId);
            return null;
        }

        ConceptConflict conflict = conflictOpt.get();

        // 检查冲突状态 (Check conflict status)
        if (conflict.getStatus() == ConceptConflict.ConflictStatus.RESOLVED) {
            log.warn(I18N.get("log.evolution.conflict_already_resolved"), conflictId);
            return null;
        }

        // 检查用户是否已投票 (Check if user already voted)
        String voteKey = userId + "_" + conflictId;
        UserVote existingVote = voteCache.get(voteKey);

        if (existingVote != null) {
            // 更新已有投票 (Update existing vote)
            log.info(I18N.get("log.evolution.vote_updated"), userId, conflictId);

            // 从冲突中移除旧投票 (Remove old vote from conflict)
            conflictService.addVote(conflictId, choice); // 会自动处理 (Will handle automatically)

            existingVote.setChoice(choice);
            existingVote.setReason(reason);
            existingVote.setVotedAt(LocalDateTime.now());

            saveVote(existingVote);
            return existingVote;
        }

        // 创建新投票 (Create new vote)
        UserVote vote = UserVote.builder()
            .id(UUID.randomUUID().toString())
            .conflictId(conflictId)
            .userId(userId)
            .choice(choice)
            .reason(reason)
            .votedAt(LocalDateTime.now())
            .role(UserVote.UserRole.ANONYMOUS) // 默认角色 (Default role)
            .ipAddress(ipAddress)
            .build();

        voteCache.put(voteKey, vote);
        saveVote(vote);

        // 更新冲突的投票计数 (Update conflict vote count)
        conflictService.addVote(conflictId, choice);

        log.info(I18N.get("log.evolution.vote_submitted"), userId, conflictId, choice);

        // 检查是否需要记录演化 (Check if need to record evolution)
        checkAndRecordEvolution(conflict);

        return vote;
    }

    /**
     * 检查并记录演化 (Check and record evolution)
     */
    private void checkAndRecordEvolution(ConceptConflict conflict) {
        if (conflict.getStatus() == ConceptConflict.ConflictStatus.RESOLVED &&
            conflict.getResolvedChoice() != null) {

            String winningConcept = "A".equals(conflict.getResolvedChoice()) ?
                conflict.getConceptA() : conflict.getConceptB();
            String losingConcept = "A".equals(conflict.getResolvedChoice()) ?
                conflict.getConceptB() : conflict.getConceptA();

            String conceptId = "concept-" + conflict.getQuestion().hashCode();

            evolutionService.recordConflictResolution(
                conceptId,
                conflict.getId(),
                winningConcept,
                losingConcept,
                "community",
                "社区投票决定，总票数：" + conflict.getTotalVotes()
            );
        }
    }

    /**
     * 获取用户的投票 (Get user's vote)
     */
    public Optional<UserVote> getUserVote(String userId, String conflictId) {
        String key = userId + "_" + conflictId;
        return Optional.ofNullable(voteCache.get(key));
    }

    /**
     * 获取冲突的所有投票 (Get all votes for a conflict)
     */
    public List<UserVote> getConflictVotes(String conflictId) {
        return voteCache.values().stream()
            .filter(v -> v.getConflictId().equals(conflictId))
            .sorted(Comparator.comparing(UserVote::getVotedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * 获取投票统计 (Get vote statistics)
     */
    public Map<String, Object> getVoteStatistics(String conflictId) {
        List<UserVote> votes = getConflictVotes(conflictId);

        long votesA = votes.stream().filter(v -> "A".equals(v.getChoice())).count();
        long votesB = votes.stream().filter(v -> "B".equals(v.getChoice())).count();

        // 计算加权投票（考虑用户角色）(Calculate weighted votes considering user roles)
        double weightedA = votes.stream()
            .filter(v -> "A".equals(v.getChoice()))
            .mapToDouble(UserVote::getWeight)
            .sum();
        double weightedB = votes.stream()
            .filter(v -> "B".equals(v.getChoice()))
            .mapToDouble(UserVote::getWeight)
            .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVotes", votes.size());
        stats.put("votesA", votesA);
        stats.put("votesB", votesB);
        stats.put("weightedA", weightedA);
        stats.put("weightedB", weightedB);
        stats.put("ratioA", votes.isEmpty() ? 0.0 : (double) votesA / votes.size());
        stats.put("ratioB", votes.isEmpty() ? 0.0 : (double) votesB / votes.size());

        return stats;
    }

    /**
     * 保存投票到磁盘 (Save vote to disk)
     */
    private void saveVote(UserVote vote) {
        try {
            // 按冲突ID创建子目录 (Create subdirectory by conflict ID)
            Path conflictDir = votesPath.resolve(vote.getConflictId());
            Files.createDirectories(conflictDir);

            // 文件名：用户ID_投票ID (Filename: userId_voteId)
            String filename = vote.getUserId() + "_" + vote.getId() + ".json";
            Path file = conflictDir.resolve(filename);

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), vote);
        } catch (IOException e) {
            log.error(I18N.get("log.evolution.vote_save_failed", vote.getId()), e);
        }
    }

    /**
     * 获取投票统计 (Get voting statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalVotes = voteCache.size();

        // 按冲突分组统计 (Group by conflict)
        Map<String, Long> votesByConflict = voteCache.values().stream()
            .collect(Collectors.groupingBy(
                UserVote::getConflictId,
                Collectors.counting()
            ));

        // 按角色分组统计 (Group by role)
        Map<UserVote.UserRole, Long> votesByRole = voteCache.values().stream()
            .collect(Collectors.groupingBy(
                UserVote::getRole,
                Collectors.counting()
            ));

        stats.put("totalVotes", totalVotes);
        stats.put("uniqueConflicts", votesByConflict.size());
        stats.put("uniqueUsers", voteCache.values().stream()
            .map(UserVote::getUserId)
            .distinct()
            .count());
        stats.put("votesByRole", votesByRole);
        stats.put("averageVotesPerConflict", votesByConflict.isEmpty() ? 0.0 :
            votesByConflict.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));

        return stats;
    }
}

