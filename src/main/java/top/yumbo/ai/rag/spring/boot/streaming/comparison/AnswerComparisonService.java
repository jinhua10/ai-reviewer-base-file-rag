package top.yumbo.ai.rag.spring.boot.streaming.comparison;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.model.RecentQA;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 答案对比服务
 * (Answer Comparison Service)
 *
 * 核心功能：
 * 1. 记录用户对 HOPE vs LLM 答案的选择
 * 2. 使用 LLM 进行差异分析
 * 3. 触发知识更新机制（投票、学习等）
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Service
public class AnswerComparisonService {

    private final HOPEKnowledgeManager hopeManager;
    private final LLMClient llmClient;

    // 对比记录存储（内存）
    private final Map<String, AnswerComparison> comparisons = new ConcurrentHashMap<>();

    // 投票统计（按问题聚合）
    private final Map<String, VoteStatistics> voteStats = new ConcurrentHashMap<>();

    @Autowired
    public AnswerComparisonService(
            @Autowired(required = false) HOPEKnowledgeManager hopeManager,
            LLMClient llmClient) {
        this.hopeManager = hopeManager;
        this.llmClient = llmClient;
    }

    /**
     * 提交对比反馈
     * (Submit comparison feedback)
     *
     * @param comparison 对比记录
     * @return 处理结果
     */
    public ComparisonResult submitFeedback(AnswerComparison comparison) {
        // 设置时间戳
        if (comparison.getCreatedAt() == null) {
            comparison.setCreatedAt(LocalDateTime.now());
        }
        comparison.setFeedbackAt(LocalDateTime.now());

        // 生成ID
        if (comparison.getComparisonId() == null) {
            comparison.setComparisonId(UUID.randomUUID().toString());
        }

        log.info(I18N.get("comparison.feedback.received",
            comparison.getComparisonId(),
            comparison.getUserChoice()));

        // 保存对比记录
        comparisons.put(comparison.getComparisonId(), comparison);

        // 更新投票统计
        updateVoteStatistics(comparison);

        // 异步处理
        CompletableFuture<Void> processFuture = CompletableFuture.runAsync(() -> {
            try {
                // 1. 差异分析（如果需要）
                if (comparison.needsDifferenceAnalysis()) {
                    String analysis = analyzeDifference(comparison);
                    comparison.setDifferenceAnalysis(analysis);
                    log.debug("差异分析完成: {}", comparison.getComparisonId());
                }

                // 2. 触发知识更新
                triggerKnowledgeUpdate(comparison);

                // 3. 标记为已处理
                comparison.setProcessed(true);

            } catch (Exception e) {
                log.error("处理对比反馈失败: {}", e.getMessage(), e);
            }
        });

        return ComparisonResult.builder()
            .comparisonId(comparison.getComparisonId())
            .success(true)
            .message(I18N.get("comparison.feedback.success"))
            .processingAsync(true)
            .build();
    }

    /**
     * 差异分析（使用 LLM）
     * (Analyze difference using LLM)
     */
    private String analyzeDifference(AnswerComparison comparison) {
        try {
            String prompt = buildDifferenceAnalysisPrompt(comparison);

            log.debug("开始差异分析: {}", comparison.getComparisonId());
            String analysis = llmClient.generate(prompt);

            log.info("差异分析完成: {} 字符", analysis.length());
            return analysis;

        } catch (Exception e) {
            log.error("差异分析失败: {}", e.getMessage());
            return I18N.get("comparison.analysis.failed") + ": " + e.getMessage();
        }
    }

    /**
     * 构建差异分析提示词
     */
    private String buildDifferenceAnalysisPrompt(AnswerComparison comparison) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 答案对比分析任务\n\n");
        prompt.append("## 原始问题\n");
        prompt.append(comparison.getQuestion()).append("\n\n");

        prompt.append("## HOPE 答案（知识库快速答案）\n");
        prompt.append("**置信度**: ").append(comparison.getHopeConfidence()).append("\n");
        prompt.append("**来源**: ").append(comparison.getHopeSource()).append("\n");
        prompt.append("**内容**:\n").append(comparison.getHopeAnswer()).append("\n\n");

        prompt.append("## LLM 答案（AI 生成答案）\n");
        prompt.append(comparison.getLlmAnswer()).append("\n\n");

        prompt.append("## 用户选择\n");
        prompt.append(comparison.getUserChoice().getDescription()).append("\n\n");

        if (comparison.getUserComment() != null && !comparison.getUserComment().isEmpty()) {
            prompt.append("## 用户评论\n");
            prompt.append(comparison.getUserComment()).append("\n\n");
        }

        prompt.append("## 分析要求\n");
        prompt.append("请分析这两个答案的差异，包括：\n");
        prompt.append("1. **准确性**：哪个答案更准确？\n");
        prompt.append("2. **完整性**：哪个答案更完整？\n");
        prompt.append("3. **实用性**：哪个答案更实用？\n");
        prompt.append("4. **差异原因**：为什么会有差异？\n");
        prompt.append("5. **改进建议**：如何改进较弱的答案？\n\n");
        prompt.append("请用简洁的语言（200-300字）进行分析。\n");

        return prompt.toString();
    }

    /**
     * 触发知识更新
     * (Trigger knowledge update)
     */
    private void triggerKnowledgeUpdate(AnswerComparison comparison) {
        if (hopeManager == null) {
            log.debug("HOPE 管理器未启用，跳过知识更新");
            return;
        }

        switch (comparison.getUserChoice()) {
            case HOPE_BETTER:
                // HOPE 答案更好 → 提升 HOPE 答案置信度
                promoteHOPEAnswer(comparison);
                break;

            case LLM_BETTER:
                // LLM 答案更好 → 保存 LLM 答案到 HOPE
                saveLLMAnswerToHOPE(comparison);
                break;

            case BOTH_GOOD:
                // 两者都好 → 记录为高质量问答
                saveBothToHOPE(comparison);
                break;

            case NEITHER_GOOD:
                // 都不好 → 标记为需要改进
                markAsNeedsImprovement(comparison);
                break;
        }
    }

    /**
     * 提升 HOPE 答案
     */
    private void promoteHOPEAnswer(AnswerComparison comparison) {
        try {
            // 查找原始 HOPE 答案
            RecentQA qa = hopeManager.getOrdinaryLayer()
                .findSimilarQA(comparison.getQuestion(), 0.7);

            if (qa != null) {
                // 增加评分
                qa.setRating(Math.min(qa.getRating() + 1, 5));
                qa.setTotalRating(qa.getTotalRating() + 5); // 用户选择 HOPE，给高分
                qa.setRatingCount(qa.getRatingCount() + 1);
                qa.recordAccess();

                // 保存更新
                hopeManager.getOrdinaryLayer().save(qa);

                log.info("HOPE 答案已提升: questionId={}, newRating={}",
                    qa.getId(), qa.getRating());
            }

        } catch (Exception e) {
            log.error("提升 HOPE 答案失败: {}", e.getMessage());
        }
    }

    /**
     * 保存 LLM 答案到 HOPE
     */
    private void saveLLMAnswerToHOPE(AnswerComparison comparison) {
        try {
            RecentQA qa = RecentQA.builder()
                .id(UUID.randomUUID().toString())
                .question(comparison.getQuestion())
                .answer(comparison.getLlmAnswer())
                .sessionId(comparison.getSessionId())
                .createdAt(LocalDateTime.now())
                .rating(5) // 用户选择 LLM，给高分
                .totalRating(5)
                .ratingCount(1)
                .accessCount(1)
                .sourceDocuments("LLM_COMPARISON_CHOSEN")
                .build();

            hopeManager.getOrdinaryLayer().save(qa);

            log.info("LLM 答案已保存到 HOPE: questionId={}", qa.getId());

        } catch (Exception e) {
            log.error("保存 LLM 答案失败: {}", e.getMessage());
        }
    }

    /**
     * 保存两个答案到 HOPE
     */
    private void saveBothToHOPE(AnswerComparison comparison) {
        try {
            // 合并两个答案
            String combinedAnswer = String.format(
                "**HOPE 答案（置信度 %.1f%%）**:\n%s\n\n**LLM 详细回答**:\n%s",
                comparison.getHopeConfidence() * 100,
                comparison.getHopeAnswer(),
                comparison.getLlmAnswer()
            );

            RecentQA qa = RecentQA.builder()
                .id(UUID.randomUUID().toString())
                .question(comparison.getQuestion())
                .answer(combinedAnswer)
                .sessionId(comparison.getSessionId())
                .createdAt(LocalDateTime.now())
                .rating(5)
                .totalRating(5)
                .ratingCount(1)
                .accessCount(1)
                .sourceDocuments("BOTH_ANSWERS_COMBINED")
                .build();

            hopeManager.getOrdinaryLayer().save(qa);

            log.info("组合答案已保存到 HOPE: questionId={}", qa.getId());

        } catch (Exception e) {
            log.error("保存组合答案失败: {}", e.getMessage());
        }
    }

    /**
     * 标记为需要改进
     */
    private void markAsNeedsImprovement(AnswerComparison comparison) {
        log.warn("问题需要改进: question={}, reason={}",
            comparison.getQuestion(),
            comparison.getUserComment());

        // TODO: 可以将这些问题收集起来，定期人工审核或重新生成
    }

    /**
     * 更新投票统计
     */
    private void updateVoteStatistics(AnswerComparison comparison) {
        String questionKey = normalizeQuestion(comparison.getQuestion());

        VoteStatistics stats = voteStats.computeIfAbsent(questionKey,
            k -> new VoteStatistics(comparison.getQuestion()));

        stats.addVote(comparison.getUserChoice());
        stats.setLastUpdated(LocalDateTime.now());

        log.debug("投票统计更新: question={}, hopeVotes={}, llmVotes={}",
            questionKey, stats.getHopeVotes(), stats.getLlmVotes());
    }

    /**
     * 标准化问题（用于聚合）
     */
    private String normalizeQuestion(String question) {
        return question.toLowerCase()
            .replaceAll("[？?！!。.]", "")
            .trim();
    }

    /**
     * 获取对比记录
     */
    public Optional<AnswerComparison> getComparison(String comparisonId) {
        return Optional.ofNullable(comparisons.get(comparisonId));
    }

    /**
     * 获取投票统计
     */
    public Optional<VoteStatistics> getVoteStatistics(String question) {
        String key = normalizeQuestion(question);
        return Optional.ofNullable(voteStats.get(key));
    }

    /**
     * 获取所有对比记录
     */
    public List<AnswerComparison> getAllComparisons() {
        return new ArrayList<>(comparisons.values());
    }

    /**
     * 获取最近的对比记录
     */
    public List<AnswerComparison> getRecentComparisons(int limit) {
        return comparisons.values().stream()
            .sorted(Comparator.comparing(AnswerComparison::getFeedbackAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 投票统计内部类
     */
    @lombok.Data
    public static class VoteStatistics {
        private String question;
        private int hopeVotes = 0;
        private int llmVotes = 0;
        private int bothVotes = 0;
        private int neitherVotes = 0;
        private LocalDateTime lastUpdated;

        public VoteStatistics(String question) {
            this.question = question;
            this.lastUpdated = LocalDateTime.now();
        }

        public void addVote(AnswerComparison.UserChoice choice) {
            switch (choice) {
                case HOPE_BETTER:
                    hopeVotes++;
                    break;
                case LLM_BETTER:
                    llmVotes++;
                    break;
                case BOTH_GOOD:
                    bothVotes++;
                    break;
                case NEITHER_GOOD:
                    neitherVotes++;
                    break;
            }
        }

        public int getTotalVotes() {
            return hopeVotes + llmVotes + bothVotes + neitherVotes;
        }

        public double getHopeWinRate() {
            int total = getTotalVotes();
            return total > 0 ? (double) hopeVotes / total : 0.0;
        }

        public double getLlmWinRate() {
            int total = getTotalVotes();
            return total > 0 ? (double) llmVotes / total : 0.0;
        }

        public String getWinner() {
            if (hopeVotes > llmVotes) {
                return "HOPE";
            } else if (llmVotes > hopeVotes) {
                return "LLM";
            } else {
                return "TIE";
            }
        }
    }

    /**
     * 对比结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ComparisonResult {
        private String comparisonId;
        private boolean success;
        private String message;
        private boolean processingAsync;
        private String differenceAnalysis;
    }
}

