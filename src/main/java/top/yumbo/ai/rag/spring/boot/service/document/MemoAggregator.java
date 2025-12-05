package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 备忘录聚合器
 *
 * 负责将多个备忘录条目聚合为更高层次的摘要
 */
@Slf4j
@Service
public class MemoAggregator {

    private final LLMClient llmClient;
    private final TokenEstimator tokenEstimator;

    @Autowired
    public MemoAggregator(@Autowired(required = false) LLMClient llmClient,
                          TokenEstimator tokenEstimator) {
        this.llmClient = llmClient;
        this.tokenEstimator = tokenEstimator;
    }

    /**
     * 按主题聚合备忘录
     *
     * @param memos 备忘录列表
     * @return 聚合后的主题组
     */
    public List<TopicGroup> aggregateByTopic(List<MemoEntry> memos) {
        if (memos == null || memos.isEmpty()) {
            return Collections.emptyList();
        }

        // 简单的基于关键词的主题聚类
        Map<String, List<MemoEntry>> topicGroups = new LinkedHashMap<>();
        Set<String> assigned = new HashSet<>();

        // 按关键词分组
        for (MemoEntry memo : memos) {
            if (assigned.contains(memo.getId())) {
                continue;
            }

            String primaryKeyword = getPrimaryKeyword(memo);
            if (primaryKeyword != null) {
                topicGroups.computeIfAbsent(primaryKeyword, k -> new ArrayList<>()).add(memo);
                assigned.add(memo.getId());
            }
        }

        // 处理未分配的条目
        for (MemoEntry memo : memos) {
            if (!assigned.contains(memo.getId())) {
                topicGroups.computeIfAbsent("其他", k -> new ArrayList<>()).add(memo);
            }
        }

        // 转换为 TopicGroup
        List<TopicGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<MemoEntry>> entry : topicGroups.entrySet()) {
            TopicGroup group = new TopicGroup();
            group.setTopic(entry.getKey());
            group.setEntries(entry.getValue());
            group.setEntryCount(entry.getValue().size());

            // 计算组的重要性（取最高）
            double maxImportance = entry.getValue().stream()
                    .mapToDouble(MemoEntry::getImportance)
                    .max()
                    .orElse(0.5);
            group.setImportance(maxImportance);

            result.add(group);
        }

        // 按重要性排序
        result.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));

        log.debug("按主题聚合: {} 个条目 -> {} 个主题组", memos.size(), result.size());

        return result;
    }

    /**
     * 按片段类型聚合
     */
    public Map<SegmentType, List<MemoEntry>> aggregateByType(List<MemoEntry> memos) {
        if (memos == null || memos.isEmpty()) {
            return Collections.emptyMap();
        }

        return memos.stream()
                .filter(m -> m.getSegmentType() != null)
                .collect(Collectors.groupingBy(
                        MemoEntry::getSegmentType,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * 生成聚合摘要
     *
     * @param topicGroups 主题组列表
     * @param maxTokens 最大 token 数
     * @return 聚合摘要
     */
    public String generateAggregatedSummary(List<TopicGroup> topicGroups, int maxTokens) {
        if (topicGroups == null || topicGroups.isEmpty()) {
            return "";
        }

        // 如果没有 LLM，使用简单聚合
        if (llmClient == null) {
            return generateSimpleSummary(topicGroups, maxTokens);
        }

        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("# 备忘录聚合任务\n\n");
            prompt.append("以下是按主题分组的文档分析备忘录，请生成一个简洁的聚合摘要。\n\n");

            for (TopicGroup group : topicGroups) {
                prompt.append("## 主题: ").append(group.getTopic()).append("\n\n");

                for (MemoEntry entry : group.getEntries()) {
                    prompt.append("- **第").append(entry.getSegmentIndex()).append("部分**");
                    if (entry.getTitle() != null) {
                        prompt.append(" (").append(entry.getTitle()).append(")");
                    }
                    prompt.append(": ");
                    prompt.append(entry.getEffectiveContent()).append("\n");
                }
                prompt.append("\n");
            }

            prompt.append("## 要求\n\n");
            prompt.append("1. 为每个主题生成 1-2 句话的摘要\n");
            prompt.append("2. 突出最重要的信息\n");
            prompt.append("3. 保持简洁，总长度不超过 ").append(maxTokens).append(" 字\n\n");
            prompt.append("请生成聚合摘要：\n");

            return llmClient.generate(prompt.toString());

        } catch (Exception e) {
            log.warn("LLM 聚合失败，使用简单聚合: {}", e.getMessage());
            return generateSimpleSummary(topicGroups, maxTokens);
        }
    }

    /**
     * 合并相似条目
     *
     * @param memos 备忘录列表
     * @param similarityThreshold 相似度阈值
     * @return 合并后的列表
     */
    public List<MemoEntry> mergeSimilarEntries(List<MemoEntry> memos, double similarityThreshold) {
        if (memos == null || memos.size() <= 1) {
            return memos != null ? new ArrayList<>(memos) : Collections.emptyList();
        }

        List<MemoEntry> result = new ArrayList<>();
        Set<String> merged = new HashSet<>();

        for (int i = 0; i < memos.size(); i++) {
            MemoEntry memo1 = memos.get(i);
            if (merged.contains(memo1.getId())) {
                continue;
            }

            List<MemoEntry> similar = new ArrayList<>();
            similar.add(memo1);

            for (int j = i + 1; j < memos.size(); j++) {
                MemoEntry memo2 = memos.get(j);
                if (merged.contains(memo2.getId())) {
                    continue;
                }

                double similarity = calculateSimilarity(memo1, memo2);
                if (similarity >= similarityThreshold) {
                    similar.add(memo2);
                    merged.add(memo2.getId());
                }
            }

            if (similar.size() > 1) {
                // 合并相似条目
                MemoEntry mergedEntry = mergeEntries(similar);
                result.add(mergedEntry);
            } else {
                result.add(memo1);
            }

            merged.add(memo1.getId());
        }

        log.debug("合并相似条目: {} -> {}", memos.size(), result.size());

        return result;
    }

    /**
     * 分层压缩
     *
     * @param memos 备忘录列表
     * @param targetTokens 目标 token 数
     * @return 压缩后的摘要
     */
    public String hierarchicalCompress(List<MemoEntry> memos, int targetTokens) {
        if (memos == null || memos.isEmpty()) {
            return "";
        }

        // 第一层：按主题聚合
        List<TopicGroup> topicGroups = aggregateByTopic(memos);

        // 第二层：生成主题摘要
        StringBuilder summary = new StringBuilder();
        int currentTokens = 0;

        for (TopicGroup group : topicGroups) {
            String topicSummary = summarizeTopicGroup(group);
            int topicTokens = tokenEstimator.estimate(topicSummary);

            if (currentTokens + topicTokens > targetTokens) {
                break;
            }

            summary.append("**").append(group.getTopic()).append("**\n");
            summary.append(topicSummary).append("\n\n");
            currentTokens += topicTokens;
        }

        return summary.toString().trim();
    }

    // ==================== 私有方法 ====================

    private String getPrimaryKeyword(MemoEntry memo) {
        List<String> keywords = memo.getKeywords();
        if (keywords != null && !keywords.isEmpty()) {
            return keywords.get(0);
        }

        // 从标题提取
        if (memo.getTitle() != null && !memo.getTitle().isEmpty()) {
            String[] words = memo.getTitle().split("[\\s\\p{Punct}]+");
            for (String word : words) {
                if (word.length() >= 2) {
                    return word;
                }
            }
        }

        return null;
    }

    private String generateSimpleSummary(List<TopicGroup> topicGroups, int maxTokens) {
        StringBuilder summary = new StringBuilder();
        int currentTokens = 0;

        for (TopicGroup group : topicGroups) {
            String line = "**" + group.getTopic() + "**: " + group.getEntryCount() + " 个相关内容\n";
            int lineTokens = tokenEstimator.estimate(line);

            if (currentTokens + lineTokens > maxTokens) {
                break;
            }

            summary.append(line);
            currentTokens += lineTokens;
        }

        return summary.toString();
    }

    private double calculateSimilarity(MemoEntry memo1, MemoEntry memo2) {
        List<String> keywords1 = memo1.getKeywords();
        List<String> keywords2 = memo2.getKeywords();

        if (keywords1 == null || keywords1.isEmpty() || keywords2 == null || keywords2.isEmpty()) {
            return 0.0;
        }

        Set<String> set1 = new HashSet<>(keywords1);
        Set<String> set2 = new HashSet<>(keywords2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private MemoEntry mergeEntries(List<MemoEntry> entries) {
        if (entries.isEmpty()) {
            return null;
        }

        MemoEntry first = entries.get(0);

        // 合并内容
        StringBuilder content = new StringBuilder();
        Set<String> allKeywords = new LinkedHashSet<>();
        double maxImportance = 0;

        for (MemoEntry entry : entries) {
            if (entry.getEffectiveContent() != null) {
                content.append(entry.getEffectiveContent()).append("\n");
            }
            if (entry.getKeywords() != null) {
                allKeywords.addAll(entry.getKeywords());
            }
            maxImportance = Math.max(maxImportance, entry.getImportance());
        }

        return MemoEntry.builder()
                .id("merged-" + first.getId())
                .segmentIndex(first.getSegmentIndex())
                .segmentType(first.getSegmentType())
                .title(first.getTitle() + " (合并)")
                .originalContent(content.toString().trim())
                .keywords(new ArrayList<>(allKeywords))
                .importance(maxImportance)
                .compressed(true)
                .build();
    }

    private String summarizeTopicGroup(TopicGroup group) {
        StringBuilder sb = new StringBuilder();

        int count = Math.min(3, group.getEntries().size());
        for (int i = 0; i < count; i++) {
            MemoEntry entry = group.getEntries().get(i);
            String content = entry.getEffectiveContent();
            if (content != null && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            sb.append("- ").append(content).append("\n");
        }

        if (group.getEntries().size() > 3) {
            sb.append("- ... 还有 ").append(group.getEntries().size() - 3).append(" 项\n");
        }

        return sb.toString().trim();
    }

    // ==================== 数据类 ====================

    /**
     * 主题组
     */
    @Data
    public static class TopicGroup {
        private String topic;
        private List<MemoEntry> entries;
        private int entryCount;
        private double importance;
    }
}

