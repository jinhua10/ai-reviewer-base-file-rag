package top.yumbo.ai.rag.spring.boot.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.model.document.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档备忘录管理器实现
 *
 * 实现分层记忆模型：
 * - 短期记忆：最近 N 个片段的完整要点
 * - 长期备忘录：所有片段的压缩版本
 */
@Slf4j
@Service
public class DocumentMemoManagerImpl implements DocumentMemoManager {

    private final TokenEstimator tokenEstimator;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    // ==================== 配置 ====================

    @Value("${document-analysis.memo.default-short-term-capacity:3}")
    private int defaultShortTermCapacity;

    @Value("${document-analysis.memo.long-term-max-entries:100}")
    private int longTermMaxEntries;

    @Value("${document-analysis.memo.memo-entry-max-tokens:200}")
    private int memoEntryMaxTokens;

    @Value("${document-analysis.memo.prompt-budget.total:4000}")
    private int totalTokenBudget;

    @Value("${document-analysis.memo.skip-reprocessing.min-tokens-threshold:50}")
    private int minTokensThreshold;

    @Value("${document-analysis.memo.skip-reprocessing.target-compression-ratio:0.3}")
    private double targetCompressionRatio;

    @Value("${document-analysis.memo.skip-reprocessing.max-compression-rounds:2}")
    private int maxCompressionRounds;

    @Value("${document-analysis.memo.aggregation.independent-entry.importance-threshold:0.8}")
    private double independentImportanceThreshold;

    // ==================== 存储 ====================

    /** 短期记忆 */
    private final LinkedList<MemoEntry> shortTermMemory = new LinkedList<>();

    /** 长期备忘录 */
    private final List<MemoEntry> longTermMemo = new ArrayList<>();

    /** 关键词倒排索引 */
    private final Map<String, Set<Integer>> keywordIndex = new HashMap<>();

    /** 当前文档来源 */
    private DocumentSource currentSource;

    /** 分析开始时间 */
    private long analysisStartTime;

    /** 当前分析的片段索引 */
    private int currentSegmentIndex;

    @Autowired
    public DocumentMemoManagerImpl(TokenEstimator tokenEstimator,
                                   @Autowired(required = false) LLMClient llmClient) {
        this.tokenEstimator = tokenEstimator;
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void startNewDocument(DocumentSource source) {
        clear();
        this.currentSource = source;
        this.analysisStartTime = System.currentTimeMillis();
        this.currentSegmentIndex = 0;
        log.info("📄 开始分析文档: {} (共 {} 个片段)",
                source.getDocumentName(), source.getTotalSegments());
    }

    @Override
    public void addSegmentAnalysis(DocumentSegment segment, String analysis, String keyPoints) {
        log.debug("📝 添加片段分析: {} - {}", segment.getIndex(), segment.getTitle());

        // 估算 Token 数
        int tokens = tokenEstimator.estimate(keyPoints);

        // 创建备忘录条目
        MemoEntry entry = MemoEntry.fromSegment(segment, keyPoints, tokens);

        // 提取关键词（简单实现：按空格和标点分词）
        List<String> keywords = extractKeywords(keyPoints);
        entry.setKeywords(keywords);

        // 评估重要性
        double importance = evaluateImportance(keyPoints, segment);
        entry.setImportance(importance);

        // 检查是否为独立重要条目
        if (importance >= independentImportanceThreshold || containsCriticalData(keyPoints)) {
            entry.setIndependent(true);
            log.debug("⭐ 标记为独立重要条目: {}", segment.getTitle());
        }

        // 添加到短期记忆
        shortTermMemory.addLast(entry);
        currentSegmentIndex = segment.getIndex();

        // 检查短期记忆容量
        int capacity = getShortTermCapacity(segment.getType());
        while (shortTermMemory.size() > capacity) {
            // 弹出最旧的条目
            MemoEntry oldEntry = shortTermMemory.removeFirst();

            // 压缩后存入长期备忘录
            moveToLongTermMemo(oldEntry);
        }

        // 更新关键词索引
        updateKeywordIndex(entry);
    }

    @Override
    public void markAsImportant(int segmentIndex) {
        // 在短期记忆中查找
        for (MemoEntry entry : shortTermMemory) {
            if (entry.getSegmentIndex() == segmentIndex) {
                entry.setUserMarked(true);
                entry.setIndependent(true);
                log.info("⭐ 用户标记片段 {} 为重要", segmentIndex);
                return;
            }
        }

        // 在长期备忘录中查找
        for (MemoEntry entry : longTermMemo) {
            if (entry.getSegmentIndex() == segmentIndex) {
                entry.setUserMarked(true);
                entry.setIndependent(true);
                log.info("⭐ 用户标记片段 {} 为重要", segmentIndex);
                return;
            }
        }
    }

    @Override
    public List<MemoEntry> getShortTermMemory() {
        return new ArrayList<>(shortTermMemory);
    }

    @Override
    public List<MemoEntry> getLongTermMemo() {
        return new ArrayList<>(longTermMemo);
    }

    @Override
    public List<MemoEntry> recallRelevantMemos(DocumentSegment currentSegment, int maxTokens) {
        if (longTermMemo.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取当前片段的关键词
        String content = currentSegment.getFullContent();
        List<String> currentKeywords = extractKeywords(content);

        // 计算每个备忘录条目的相关性得分
        List<ScoredEntry> scoredEntries = new ArrayList<>();
        for (MemoEntry entry : longTermMemo) {
            double score = calculateRelevanceScore(entry, currentKeywords, currentSegment.getIndex());
            if (score > 0) {
                scoredEntries.add(new ScoredEntry(entry, score));
            }
        }

        // 按得分排序
        scoredEntries.sort((a, b) -> Double.compare(b.score, a.score));

        // 选择 Top-K，同时控制 Token 预算
        List<MemoEntry> result = new ArrayList<>();
        int totalTokens = 0;

        for (ScoredEntry scored : scoredEntries) {
            int entryTokens = scored.entry.getEffectiveTokens();
            if (totalTokens + entryTokens <= maxTokens) {
                scored.entry.touch(); // 更新访问时间
                result.add(scored.entry);
                totalTokens += entryTokens;
            }

            if (result.size() >= 3) { // 最多召回 3 条
                break;
            }
        }

        log.debug("📋 召回 {} 条相关备忘录，共 {} tokens", result.size(), totalTokens);
        return result;
    }

    @Override
    public String getAllMemosSummary() {
        StringBuilder sb = new StringBuilder();

        // 添加短期记忆
        if (!shortTermMemory.isEmpty()) {
            sb.append("## 📚 最近分析的片段\n\n");
            for (MemoEntry entry : shortTermMemory) {
                sb.append(entry.formatForPrompt()).append("\n\n");
            }
        }

        // 添加长期备忘录（按重要性排序）
        if (!longTermMemo.isEmpty()) {
            sb.append("## 📋 历史分析摘要\n\n");

            List<MemoEntry> sorted = longTermMemo.stream()
                    .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
                    .collect(Collectors.toList());

            for (MemoEntry entry : sorted) {
                sb.append(entry.formatForPrompt()).append("\n\n");
            }
        }

        return sb.toString();
    }

    @Override
    public List<MemoEntry> getIndependentEntries() {
        List<MemoEntry> result = new ArrayList<>();

        for (MemoEntry entry : shortTermMemory) {
            if (entry.isIndependent()) {
                result.add(entry);
            }
        }

        for (MemoEntry entry : longTermMemo) {
            if (entry.isIndependent()) {
                result.add(entry);
            }
        }

        return result;
    }

    @Override
    public String compressEntry(MemoEntry entry, int targetTokens) {
        if (entry.canSkipCompression(minTokensThreshold, targetCompressionRatio, maxCompressionRounds)) {
            log.debug("⏭️ 跳过压缩: {} (已足够精简)", entry.getTitle());
            return entry.getEffectiveContent();
        }

        String content = entry.getEffectiveContent();

        if (llmClient != null) {
            try {
                String compressed = compressWithLLM(content, targetTokens, entry.getTitle());
                entry.setCompressedContent(compressed);
                entry.setCompressedTokens(tokenEstimator.estimate(compressed));
                entry.setCompressed(true);
                entry.setCompressionCount(entry.getCompressionCount() + 1);
                entry.setContentForm(ContentForm.STRUCTURED_BULLETS);
                log.debug("✅ LLM 压缩完成: {} -> {} tokens",
                        entry.getOriginalTokens(), entry.getCompressedTokens());
                return compressed;
            } catch (Exception e) {
                log.warn("⚠️ LLM 压缩失败，使用截断: {}", e.getMessage());
            }
        }

        // 降级：简单截断
        String truncated = tokenEstimator.truncateToTokens(content, targetTokens);
        entry.setCompressedContent(truncated);
        entry.setCompressedTokens(tokenEstimator.estimate(truncated));
        entry.setCompressed(true);
        entry.setCompressionCount(entry.getCompressionCount() + 1);
        return truncated;
    }

    @Override
    public int estimateTokens(String text) {
        return tokenEstimator.estimate(text);
    }

    @Override
    public boolean hasTokenBudget(int requiredTokens) {
        return getRemainingTokenBudget() >= requiredTokens;
    }

    @Override
    public int getRemainingTokenBudget() {
        int used = 0;
        for (MemoEntry entry : shortTermMemory) {
            used += entry.getEffectiveTokens();
        }
        for (MemoEntry entry : longTermMemo) {
            used += entry.getEffectiveTokens();
        }
        return Math.max(0, totalTokenBudget - used);
    }

    @Override
    public void clear() {
        shortTermMemory.clear();
        longTermMemo.clear();
        keywordIndex.clear();
        currentSource = null;
        analysisStartTime = 0;
        currentSegmentIndex = 0;
        log.debug("🧹 已清空备忘录");
    }

    @Override
    public AnalysisProgress getProgress() {
        int totalSegments = currentSource != null ? currentSource.getTotalSegments() : 0;
        int analyzedCount = shortTermMemory.size() + longTermMemo.size();

        return AnalysisProgress.builder()
                .source(currentSource)
                .currentIndex(currentSegmentIndex)
                .totalSegments(totalSegments)
                .analyzedCount(analyzedCount)
                .shortTermMemorySize(shortTermMemory.size())
                .longTermMemoSize(longTermMemo.size())
                .independentEntryCount((int) getIndependentEntries().size())
                .completed(totalSegments > 0 && currentSegmentIndex >= totalSegments)
                .startTimeMs(analysisStartTime)
                .elapsedTimeMs(System.currentTimeMillis() - analysisStartTime)
                .build();
    }

    @Override
    public String exportToMarkdown() {
        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("# 📋 文档分析备忘录\n\n");

        // 文档信息
        if (currentSource != null) {
            sb.append("> **文档**: ").append(currentSource.getDocumentName()).append("  \n");
            sb.append("> **类型**: ").append(currentSource.getDocumentType()).append("  \n");
            sb.append("> **分析时间**: ").append(formatTimestamp(Instant.now())).append("  \n");
            sb.append("> **片段数**: ").append(currentSource.getTotalSegments()).append("\n\n");
        }

        sb.append("---\n\n");

        // 整体概览
        sb.append("## 📊 整体概览\n\n");
        sb.append("- **已分析片段**: ").append(shortTermMemory.size() + longTermMemo.size()).append("\n");
        sb.append("- **独立重要条目**: ").append(getIndependentEntries().size()).append("\n");
        sb.append("- **短期记忆**: ").append(shortTermMemory.size()).append(" 条\n");
        sb.append("- **长期备忘录**: ").append(longTermMemo.size()).append(" 条\n\n");

        sb.append("---\n\n");

        // 独立重要条目
        List<MemoEntry> independentEntries = getIndependentEntries();
        if (!independentEntries.isEmpty()) {
            sb.append("## ⭐ 独立重要条目\n\n");
            sb.append("> 以下内容独立性强，建议单独关注\n\n");

            for (MemoEntry entry : independentEntries) {
                sb.append(entry.formatForDocument()).append("\n");
            }

            sb.append("---\n\n");
        }

        // 短期记忆（最近分析）
        if (!shortTermMemory.isEmpty()) {
            sb.append("## 📚 最近分析的片段\n\n");
            for (MemoEntry entry : shortTermMemory) {
                if (!entry.isIndependent()) { // 避免重复
                    sb.append(entry.formatForDocument()).append("\n");
                }
            }
            sb.append("---\n\n");
        }

        // 长期备忘录
        if (!longTermMemo.isEmpty()) {
            sb.append("## 📋 历史分析摘要\n\n");
            sb.append("<details>\n");
            sb.append("<summary>点击展开完整备忘录（").append(longTermMemo.size()).append(" 条）</summary>\n\n");

            for (MemoEntry entry : longTermMemo) {
                if (!entry.isIndependent()) { // 避免重复
                    sb.append(entry.formatForDocument()).append("\n");
                }
            }

            sb.append("</details>\n\n");
        }

        sb.append("---\n\n");
        sb.append("*备忘录导出完成*\n");

        return sb.toString();
    }

    @Override
    public String exportToJson() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("source", currentSource);
            data.put("exportTime", Instant.now().toString());
            data.put("progress", getProgress());
            data.put("shortTermMemory", shortTermMemory);
            data.put("longTermMemo", longTermMemo);
            data.put("independentEntries", getIndependentEntries());

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            log.error("导出 JSON 失败", e);
            return "{}";
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 获取指定类型的短期记忆容量
     */
    private int getShortTermCapacity(SegmentType type) {
        // 可以根据类型返回不同容量，这里使用默认值
        return defaultShortTermCapacity;
    }

    /**
     * 将条目移入长期备忘录
     */
    private void moveToLongTermMemo(MemoEntry entry) {
        // 检查是否需要压缩
        if (entry.getEffectiveTokens() > memoEntryMaxTokens) {
            compressEntry(entry, memoEntryMaxTokens);
        }

        longTermMemo.add(entry);
        log.debug("📦 移入长期备忘录: {} ({} tokens)",
                entry.getTitle(), entry.getEffectiveTokens());

        // 检查长期备忘录容量
        if (longTermMemo.size() > longTermMaxEntries) {
            // 移除最旧且非重要的条目
            MemoEntry toRemove = null;
            for (MemoEntry e : longTermMemo) {
                if (!e.isIndependent() && !e.isUserMarked()) {
                    toRemove = e;
                    break;
                }
            }
            if (toRemove != null) {
                longTermMemo.remove(toRemove);
                log.debug("🗑️ 移除旧条目: {}", toRemove.getTitle());
            }
        }
    }

    /**
     * 提取关键词（简单实现）
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        // 简单分词：按空格、中英文标点分割
        // 使用 Unicode 范围匹配中文标点，避免编码问题
        // \u3000-\u303F: 中文标点
        // \uFF00-\uFFEF: 全角字符
        String[] tokens = text.split("[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF]+");

        Set<String> keywords = new LinkedHashSet<>();
        for (String token : tokens) {
            token = token.trim();
            // 过滤：长度 >= 2，非纯数字
            if (token.length() >= 2 && !token.matches("\\d+")) {
                keywords.add(token);
            }
            if (keywords.size() >= 10) {
                break;
            }
        }

        return new ArrayList<>(keywords);
    }

    /**
     * 评估内容重要性
     */
    private double evaluateImportance(String content, DocumentSegment segment) {
        double score = 0.5; // 基础分

        if (content == null || content.isEmpty()) {
            return 0.2;
        }

        // 1. 包含数字/数据
        if (content.matches(".*\\d+.*")) {
            score += 0.1;
        }

        // 2. 包含结论性关键词
        String[] conclusionKeywords = {"总结", "结论", "关键", "重要", "核心", "决策", "建议", "风险",
                "summary", "conclusion", "key", "important", "critical"};
        for (String keyword : conclusionKeywords) {
            if (content.toLowerCase().contains(keyword)) {
                score += 0.1;
                break;
            }
        }

        // 3. 第一张和最后一张通常重要
        if (segment.getSource() != null) {
            int total = segment.getSource().getTotalSegments();
            if (segment.getIndex() == 1 || segment.getIndex() == total) {
                score += 0.15;
            }
        }

        // 4. 内容长度适中（太短可能信息不足，太长可能是堆砌）
        int length = content.length();
        if (length >= 100 && length <= 500) {
            score += 0.05;
        }

        return Math.min(1.0, score);
    }

    /**
     * 检查是否包含关键数据
     */
    private boolean containsCriticalData(String content) {
        if (content == null) {
            return false;
        }

        // 包含金额
        if (content.matches(".*[¥$€]\\s*[\\d,]+.*")) {
            return true;
        }

        // 包含百分比
        if (content.matches(".*\\d+\\.?\\d*\\s*%.*")) {
            return true;
        }

        // 包含日期
        if (content.matches(".*\\d{4}[-/年]\\d{1,2}[-/月].*")) {
            return true;
        }

        return false;
    }

    /**
     * 更新关键词索引
     */
    private void updateKeywordIndex(MemoEntry entry) {
        for (String keyword : entry.getKeywords()) {
            keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
                    .add(entry.getSegmentIndex());
        }
    }

    /**
     * 计算相关性得分
     */
    private double calculateRelevanceScore(MemoEntry entry, List<String> currentKeywords, int currentIndex) {
        double score = 0;

        // 1. 关键词匹配 (权重 0.4)
        int matchCount = 0;
        for (String keyword : currentKeywords) {
            if (entry.getKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(keyword))) {
                matchCount++;
            }
        }
        if (!currentKeywords.isEmpty()) {
            score += 0.4 * matchCount / currentKeywords.size();
        }

        // 2. 重要性 (权重 0.3)
        score += 0.3 * entry.getImportance();

        // 3. 时间衰减 (权重 0.2)
        int distance = Math.abs(currentIndex - entry.getSegmentIndex());
        double recencyFactor = 1.0 / (1.0 + 0.1 * distance);
        score += 0.2 * recencyFactor;

        // 4. 独立条目加分 (权重 0.1)
        if (entry.isIndependent()) {
            score += 0.1;
        }

        return score;
    }

    /**
     * 使用 LLM 压缩内容
     */
    private String compressWithLLM(String content, int targetTokens, String title) {
        int targetChars = tokenEstimator.calculateTargetLength(
                tokenEstimator.estimate(content), targetTokens, content.length());

        String prompt = String.format(
                "请将以下内容压缩为简洁的摘要，保留最关键的信息。\n\n" +
                "## 原始内容\n【%s】\n%s\n\n" +
                "## 压缩要求\n" +
                "1. 保留核心观点和关键数据\n" +
                "2. 使用简洁的要点形式\n" +
                "3. 目标长度：约 %d 字符\n\n" +
                "## 输出\n直接输出压缩后的内容：",
                title != null ? title : "内容",
                content,
                targetChars
        );

        return llmClient.generate(prompt);
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * 带分数的条目
     */
    private static class ScoredEntry {
        final MemoEntry entry;
        final double score;

        ScoredEntry(MemoEntry entry, double score) {
            this.entry = entry;
            this.score = score;
        }
    }
}

