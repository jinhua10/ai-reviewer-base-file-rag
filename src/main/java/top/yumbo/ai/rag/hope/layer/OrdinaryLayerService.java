package top.yumbo.ai.rag.hope.layer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.model.RecentQA;
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
 * 中频层服务 - 管理近期知识
 * (Ordinary Layer Service - Manages recent knowledge)
 *
 * 特点：
 * - 存储近期高分问答
 * - 定期清理过期数据
 * - 支持知识晋升到低频层
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class OrdinaryLayerService {

    private final HOPEConfig config;
    private final PermanentLayerService permanentLayerService;

    // 近期问答存储
    private final Map<String, RecentQA> recentQAs = new ConcurrentHashMap<>();

    // 关键词到问答ID的索引
    private final Map<String, Set<String>> keywordIndex = new ConcurrentHashMap<>();

    @Autowired
    public OrdinaryLayerService(HOPEConfig config,
                                 PermanentLayerService permanentLayerService) {
        this.config = config;
        this.permanentLayerService = permanentLayerService;
    }

    @PostConstruct
    public void init() {
        // 1. 检查HOPE是否启用 (Check if HOPE is enabled)
        if (!config.isEnabled()) {
            log.info(I18N.get("hope.ordinary.disabled"));
            return;
        }

        try {
            // 2. 确保存储目录存在 (Ensure storage directory exists)
            Path storagePath = Paths.get(config.getOrdinary().getStoragePath());
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // 3. 加载已保存的数据 (Load saved data)
            loadData();

            // 4. 记录初始化成功日志 (Log successful initialization)
            log.info(I18N.get("hope.ordinary.init_success", recentQAs.size()));

        } catch (IOException e) {
            // 5. 记录初始化失败日志 (Log initialization failure)
            log.error(I18N.get("hope.ordinary.init_failed"), e);
        }
    }

    /**
     * 查询中频层
     */
    public OrdinaryQueryResult query(String question) {
        long startTime = System.currentTimeMillis();
        OrdinaryQueryResult result = new OrdinaryQueryResult();

        String normalizedQuestion = question.toLowerCase().trim();

        // 查找相似问答
        List<SimilarMatch> matches = findSimilarQAs(normalizedQuestion);

        if (!matches.isEmpty()) {
            SimilarMatch bestMatch = matches.get(0);
            result.setFound(true);
            result.setBestMatch(bestMatch.getQa());
            result.setSimilarity(bestMatch.getSimilarity());
            result.setAllMatches(matches);

            // 记录访问
            bestMatch.getQa().recordAccess();

            // 判断是否可直接使用
            if (bestMatch.getSimilarity() >= config.getOrdinary().getSimilarityThreshold()) {
                result.setDirectUsable(true);
                log.debug(I18N.get("hope.ordinary.direct_hit", bestMatch.getQa().getId(), bestMatch.getSimilarity()));
            } else if (bestMatch.getSimilarity() >= config.getOrdinary().getReferenceThreshold()) {
                result.setAsReference(true);
                log.debug(I18N.get("hope.ordinary.reference_hit", bestMatch.getQa().getId(), bestMatch.getSimilarity()));
            }
        }

        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 查找相似问答（公开方法）
     * (Find similar Q&A - public method)
     *
     * @param question 用户问题
     * @param minSimilarity 最小相似度阈值
     * @return 最相似的问答，如果未找到或相似度低于阈值返回 null
     */
    public RecentQA findSimilarQA(String question, double minSimilarity) {
        List<SimilarMatch> matches = findSimilarQAs(question);

        if (!matches.isEmpty()) {
            SimilarMatch bestMatch = matches.get(0);
            if (bestMatch.getSimilarity() >= minSimilarity) {
                RecentQA qa = bestMatch.getQa();
                // 设置相似度评分
                qa.setSimilarityScore(bestMatch.getSimilarity());
                // 记录访问
                qa.recordAccess();
                return qa;
            }
        }

        return null;
    }

    /**
     * 查找相似问答（内部方法）
     */
    private List<SimilarMatch> findSimilarQAs(String question) {
        List<SimilarMatch> matches = new ArrayList<>();
        String[] queryWords = extractKeywords(question);

        // 1. 通过关键词索引获取候选
        Set<String> candidateIds = new HashSet<>();
        for (String word : queryWords) {
            Set<String> ids = keywordIndex.get(word.toLowerCase());
            if (ids != null) {
                candidateIds.addAll(ids);
            }
        }

        // 2. 计算相似度
        for (String id : candidateIds) {
            RecentQA qa = recentQAs.get(id);
            if (qa == null || qa.isPromoted()) {
                continue;
            }

            double similarity = calculateSimilarity(question, qa.getQuestion(), queryWords, qa.getKeywords());
            if (similarity >= 0.5) {
                matches.add(new SimilarMatch(qa, similarity));
            }
        }

        // 3. 如果关键词索引没找到足够的，遍历所有
        if (matches.size() < 3) {
            for (RecentQA qa : recentQAs.values()) {
                if (qa.isPromoted() || candidateIds.contains(qa.getId())) {
                    continue;
                }

                double similarity = calculateSimilarity(question, qa.getQuestion(), queryWords, qa.getKeywords());
                if (similarity >= 0.5) {
                    matches.add(new SimilarMatch(qa, similarity));
                }
            }
        }

        // 4. 按相似度排序
        matches.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        return matches.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 计算问题相似度
     */
    private double calculateSimilarity(String question1, String question2,
                                        String[] keywords1, String[] keywords2) {
        if (question1 == null || question2 == null) {
            return 0.0;
        }

        // 1. 精确匹配
        if (question1.equalsIgnoreCase(question2)) {
            return 1.0;
        }

        // 2. 关键词 Jaccard 相似度
        Set<String> set1 = new HashSet<>();
        Set<String> set2 = new HashSet<>();

        if (keywords1 != null) {
            for (String k : keywords1) {
                set1.add(k.toLowerCase());
            }
        } else {
            for (String k : extractKeywords(question1)) {
                set1.add(k.toLowerCase());
            }
        }

        if (keywords2 != null) {
            for (String k : keywords2) {
                set2.add(k.toLowerCase());
            }
        } else {
            for (String k : extractKeywords(question2)) {
                set2.add(k.toLowerCase());
            }
        }

        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 提取关键词
     */
    private String[] extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // 简单的停用词过滤
        Set<String> stopWords = Set.of(
            "的", "是", "在", "了", "和", "有", "我", "你", "这", "那",
            "什么", "怎么", "如何", "为什么", "哪", "吗", "呢", "啊",
            "a", "an", "the", "is", "are", "what", "how", "why", "which"
        );

        return Arrays.stream(text.toLowerCase().split("[\\s,.;:?!，。；：？！]+"))
            .filter(w -> w.length() >= 2 && !stopWords.contains(w))
            .toArray(String[]::new);
    }

    /**
     * 保存 RecentQA 对象到中频层
     * (Save RecentQA object to ordinary layer)
     *
     * @param qa 问答对象
     */
    public void save(RecentQA qa) {
        if (qa == null) {
            return;
        }

        // 如果没有 ID，生成一个
        if (qa.getId() == null || qa.getId().isEmpty()) {
            qa.setId("qa_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4));
        }

        // 如果没有关键词，提取关键词
        if (qa.getKeywords() == null || qa.getKeywords().length == 0) {
            qa.setKeywords(extractKeywords(qa.getQuestion()));
        }

        // 设置创建时间（如果未设置）
        if (qa.getCreatedAt() == null) {
            qa.setCreatedAt(LocalDateTime.now());
        }

        // 设置访问时间
        if (qa.getLastAccessedAt() == null) {
            qa.setLastAccessedAt(LocalDateTime.now());
        }

        // 保存到内存
        recentQAs.put(qa.getId(), qa);

        // 更新关键词索引
        for (String keyword : qa.getKeywords()) {
            keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>()).add(qa.getId());
        }

        // 持久化
        persistData();

        log.debug(I18N.get("hope.ordinary.saved", qa.getId()));
    }

    /**
     * 保存问答到中频层
     */
    public void save(String question, String answer, int rating) {
        save(question, answer, rating, null);
    }

    /**
     * 保存问答到中频层（带来源文档）
     */
    public void save(String question, String answer, int rating, String sourceDocuments) {
        // 检查是否已存在相似问题
        OrdinaryQueryResult existingResult = query(question);
        if (existingResult.isFound() && existingResult.getSimilarity() > 0.9) {
            // 更新现有记录的评分
            RecentQA existing = existingResult.getBestMatch();
            existing.recordRating(rating);
            persistData();
            log.debug(I18N.get("hope.ordinary.updated", existing.getId()));
            return;
        }

        // 创建新记录
        String id = "qa_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);
        String[] keywords = extractKeywords(question);

        RecentQA qa = RecentQA.builder()
            .id(id)
            .question(question)
            .answer(answer)
            .keywords(keywords)
            .rating(rating)
            .totalRating(rating)
            .ratingCount(1)
            .accessCount(1)
            .sourceDocuments(sourceDocuments)
            .createdAt(LocalDateTime.now())
            .lastAccessedAt(LocalDateTime.now())
            .promoted(false)
            .build();

        recentQAs.put(id, qa);

        // 更新关键词索引
        for (String keyword : keywords) {
            keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>()).add(id);
        }

        // 检查是否超过最大条目数
        enforceMaxEntries();

        persistData();
        log.info(I18N.get("hope.ordinary.saved", id, rating));
    }

    /**
     * 检查并晋升符合条件的知识到低频层
     */
    public List<RecentQA> checkAndPromote() {
        List<RecentQA> promoted = new ArrayList<>();

        int minAccess = config.getPermanent().getPromotionMinAccessCount();
        double minRating = config.getPermanent().getPromotionMinAvgRating();

        for (RecentQA qa : recentQAs.values()) {
            if (qa.isEligibleForPromotion(minAccess, minRating)) {
                // 尝试晋升
                if (promoteToPeranent(qa)) {
                    qa.setPromoted(true);
                    promoted.add(qa);
                    log.info(I18N.get("hope.learn.promoted", qa.getId()));
                }
            }
        }

        if (!promoted.isEmpty()) {
            persistData();
        }

        return promoted;
    }

    /**
     * 晋升到低频层
     */
    private boolean promoteToPeranent(RecentQA qa) {
        try {
            // 保存为确定性知识
            permanentLayerService.saveFactualKnowledge(
                top.yumbo.ai.rag.hope.model.FactualKnowledge.builder()
                    .questionPattern(generatePattern(qa.getQuestion()))
                    .keywords(qa.getKeywords())
                    .answer(qa.getAnswer())
                    .source("HOPE:Ordinary:" + qa.getId())
                    .confidence(Math.min(qa.getAverageRating() / 5.0, 1.0))
                    .build()
            );
            return true;
        } catch (Exception e) {
            log.error(I18N.get("hope.ordinary.promote_failed", qa.getId()), e);
            return false;
        }
    }

    /**
     * 生成问题匹配模式
     */
    private String generatePattern(String question) {
        // 简单实现：将问题转为正则模式
        String pattern = question.toLowerCase()
            .replaceAll("[\\s]+", ".*")
            .replaceAll("[?？]", ".*");
        return ".*" + pattern + ".*";
    }

    /**
     * 清理过期数据
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void cleanupExpired() {
        if (!config.isEnabled()) {
            return;
        }

        int retentionDays = config.getOrdinary().getRetentionDays();
        List<String> expiredIds = new ArrayList<>();

        for (RecentQA qa : recentQAs.values()) {
            if (qa.isExpired(retentionDays) || qa.isPromoted()) {
                expiredIds.add(qa.getId());
            }
        }

        for (String id : expiredIds) {
            RecentQA removed = recentQAs.remove(id);
            if (removed != null && removed.getKeywords() != null) {
                // 清理关键词索引
                for (String keyword : removed.getKeywords()) {
                    Set<String> ids = keywordIndex.get(keyword.toLowerCase());
                    if (ids != null) {
                        ids.remove(id);
                    }
                }
            }
        }

        if (!expiredIds.isEmpty()) {
            persistData();
            log.info(I18N.get("hope.ordinary.cleaned", expiredIds.size()));
        }
    }

    /**
     * 强制执行最大条目数限制
     */
    private void enforceMaxEntries() {
        int maxEntries = config.getOrdinary().getMaxEntries();
        if (recentQAs.size() <= maxEntries) {
            return;
        }

        // 按最后访问时间排序，删除最老的
        List<RecentQA> sortedByAccess = recentQAs.values().stream()
            .sorted((a, b) -> {
                LocalDateTime timeA = a.getLastAccessedAt() != null ? a.getLastAccessedAt() : a.getCreatedAt();
                LocalDateTime timeB = b.getLastAccessedAt() != null ? b.getLastAccessedAt() : b.getCreatedAt();
                if (timeA == null) return -1;
                if (timeB == null) return 1;
                return timeA.compareTo(timeB);
            })
            .collect(Collectors.toList());

        int toRemove = recentQAs.size() - maxEntries;
        for (int i = 0; i < toRemove && i < sortedByAccess.size(); i++) {
            RecentQA qa = sortedByAccess.get(i);
            recentQAs.remove(qa.getId());
            // 清理关键词索引
            if (qa.getKeywords() != null) {
                for (String keyword : qa.getKeywords()) {
                    Set<String> ids = keywordIndex.get(keyword.toLowerCase());
                    if (ids != null) {
                        ids.remove(qa.getId());
                    }
                }
            }
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", recentQAs.size());
        stats.put("promotedCount", recentQAs.values().stream().filter(RecentQA::isPromoted).count());
        stats.put("avgRating", recentQAs.values().stream()
            .mapToDouble(RecentQA::getAverageRating).average().orElse(0.0));
        stats.put("totalAccess", recentQAs.values().stream()
            .mapToLong(RecentQA::getAccessCount).sum());
        return stats;
    }

    /**
     * 获取访问次数
     */
    public long getAccessCount(String questionId) {
        RecentQA qa = recentQAs.get(questionId);
        return qa != null ? qa.getAccessCount() : 0;
    }

    /**
     * 获取平均评分
     */
    public double getAverageRating(String questionId) {
        RecentQA qa = recentQAs.get(questionId);
        return qa != null ? qa.getAverageRating() : 0.0;
    }

    /**
     * 持久化数据
     */
    private void persistData() {
        try {
            Path storagePath = Paths.get(config.getOrdinary().getStoragePath());
            Path dataPath = storagePath.resolve("recent_qa.json");
            Files.writeString(dataPath, JSON.toJSONString(recentQAs.values(),
                JSONWriter.Feature.PrettyFormat));
        } catch (IOException e) {
            log.error(I18N.get("hope.ordinary.persist_failed"), e);
        }
    }

    /**
     * 加载数据
     */
    private void loadData() {
        try {
            Path storagePath = Paths.get(config.getOrdinary().getStoragePath());
            Path dataPath = storagePath.resolve("recent_qa.json");

            if (Files.exists(dataPath)) {
                String json = Files.readString(dataPath);
                List<RecentQA> qas = JSON.parseArray(json, RecentQA.class);
                for (RecentQA qa : qas) {
                    recentQAs.put(qa.getId(), qa);
                    // 重建关键词索引
                    if (qa.getKeywords() != null) {
                        for (String keyword : qa.getKeywords()) {
                            keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
                                .add(qa.getId());
                        }
                    }
                }
                log.info(I18N.get("hope.ordinary.loaded", qas.size()));
            }
        } catch (IOException e) {
            log.error(I18N.get("hope.ordinary.load_failed"), e);
        }
    }

    /**
     * 查询结果
     */
    @Data
    public static class OrdinaryQueryResult {
        private boolean found;
        private RecentQA bestMatch;
        private double similarity;
        private boolean directUsable;
        private boolean asReference;
        private List<SimilarMatch> allMatches;
        private long processingTimeMs;
    }

    /**
     * 相似匹配结果
     */
    @Data
    public static class SimilarMatch {
        private final RecentQA qa;
        private final double similarity;

        public SimilarMatch(RecentQA qa, double similarity) {
            this.qa = qa;
            this.similarity = similarity;
        }
    }
}

