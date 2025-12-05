package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.model.document.AnalysisProgress;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 阶段性输出管理器实现
 */
@Slf4j
@Service
public class StageOutputManagerImpl implements StageOutputManager {

    private final LLMClient llmClient;

    @Value("${document-analysis.memo.stage-output.trigger-points:25,50,75}")
    private int[] triggerPoints;

    @Value("${document-analysis.memo.stage-output.enabled:true}")
    private boolean enabled;

    /** 已生成的阶段性输出 */
    private final List<StageOutput> stageOutputs = new ArrayList<>();

    /** 已触发的进度点 */
    private final Set<Integer> triggeredPoints = new HashSet<>();

    /** 片段完成记录 */
    private final Map<Integer, String> segmentKeyPoints = new LinkedHashMap<>();

    @Autowired
    public StageOutputManagerImpl(@Autowired(required = false) LLMClient llmClient) {
        this.llmClient = llmClient;
        // 默认触发点
        if (triggerPoints == null || triggerPoints.length == 0) {
            triggerPoints = new int[]{25, 50, 75};
        }
    }

    @Override
    public boolean shouldGenerateStageOutput(AnalysisProgress progress) {
        if (!enabled || progress == null) {
            return false;
        }

        double percent = progress.getProgressPercent();

        for (int point : triggerPoints) {
            // 检查是否刚好到达或刚刚越过某个触发点
            if (!triggeredPoints.contains(point) && percent >= point) {
                return true;
            }
        }

        return false;
    }

    @Override
    public StageOutput generateStageOutput(AnalysisProgress progress,
                                           List<MemoEntry> shortTermMemory,
                                           List<MemoEntry> longTermMemo) {
        if (progress == null) {
            return null;
        }

        double percent = progress.getProgressPercent();

        // 确定当前触发的阶段点
        int currentPoint = 0;
        for (int point : triggerPoints) {
            if (!triggeredPoints.contains(point) && percent >= point) {
                currentPoint = point;
                break;
            }
        }

        if (currentPoint == 0) {
            return null;
        }

        log.info("📊 生成阶段性输出 ({}%)", currentPoint);

        // 标记为已触发
        triggeredPoints.add(currentPoint);

        // 创建阶段输出
        StageOutput output = new StageOutput();
        output.setStageName(getStageName(currentPoint));
        output.setProgressPercent(percent);
        output.setAnalyzedCount(progress.getAnalyzedCount());
        output.setTotalCount(progress.getTotalSegments());
        output.setTimestamp(System.currentTimeMillis());
        output.setFinal(false);

        // 提取关键发现
        List<String> keyFindings = extractKeyFindings(shortTermMemory, longTermMemo);
        output.setKeyFindings(keyFindings);

        // 生成阶段总结
        String summary = generateStageSummary(progress, shortTermMemory, longTermMemo, currentPoint);
        output.setSummary(summary);

        stageOutputs.add(output);

        log.info("✅ 阶段性输出生成完成: {}", output.getStageName());

        return output;
    }

    @Override
    public void recordSegmentCompletion(int segmentIndex, String keyPoints) {
        segmentKeyPoints.put(segmentIndex, keyPoints);
    }

    @Override
    public List<StageOutput> getAllStageOutputs() {
        return new ArrayList<>(stageOutputs);
    }

    @Override
    public StageOutput getLatestStageOutput() {
        if (stageOutputs.isEmpty()) {
            return null;
        }
        return stageOutputs.get(stageOutputs.size() - 1);
    }

    @Override
    public void clear() {
        stageOutputs.clear();
        triggeredPoints.clear();
        segmentKeyPoints.clear();
        log.debug("🧹 阶段性输出管理器已清空");
    }

    // ==================== 私有方法 ====================

    /**
     * 获取阶段名称
     */
    private String getStageName(int point) {
        if (point <= 25) {
            return "📘 初始阶段 (25%)";
        } else if (point <= 50) {
            return "📗 中期阶段 (50%)";
        } else if (point <= 75) {
            return "📙 后期阶段 (75%)";
        } else {
            return "📕 最终阶段";
        }
    }

    /**
     * 提取关键发现
     */
    private List<String> extractKeyFindings(List<MemoEntry> shortTermMemory,
                                            List<MemoEntry> longTermMemo) {
        List<String> findings = new ArrayList<>();

        // 从短期记忆中提取
        for (MemoEntry entry : shortTermMemory) {
            if (entry.getImportance() >= 0.7) {
                String finding = formatFinding(entry);
                if (finding != null) {
                    findings.add(finding);
                }
            }
        }

        // 从长期备忘录中提取重要条目
        List<MemoEntry> importantMemos = longTermMemo.stream()
                .filter(e -> e.isIndependent() || e.getImportance() >= 0.8)
                .sorted((a, b) -> Double.compare(b.getImportance(), a.getImportance()))
                .limit(3)
                .collect(Collectors.toList());

        for (MemoEntry entry : importantMemos) {
            String finding = formatFinding(entry);
            if (finding != null && !findings.contains(finding)) {
                findings.add(finding);
            }
        }

        // 限制数量
        if (findings.size() > 5) {
            findings = findings.subList(0, 5);
        }

        return findings;
    }

    /**
     * 格式化发现
     */
    private String formatFinding(MemoEntry entry) {
        String content = entry.getEffectiveContent();
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 取第一行或前100字符
        String[] lines = content.split("\n");
        String firstLine = lines[0].trim();

        if (firstLine.length() > 100) {
            firstLine = firstLine.substring(0, 100) + "...";
        }

        return String.format("[第%d%s] %s",
                entry.getSegmentIndex(),
                entry.getSegmentType() != null ? entry.getSegmentType().getDisplayName() : "",
                firstLine);
    }

    /**
     * 生成阶段总结
     */
    private String generateStageSummary(AnalysisProgress progress,
                                        List<MemoEntry> shortTermMemory,
                                        List<MemoEntry> longTermMemo,
                                        int currentPoint) {
        if (llmClient == null) {
            return generateSimpleSummary(progress, shortTermMemory, longTermMemo);
        }

        try {
            StringBuilder prompt = new StringBuilder();

            prompt.append("# 文档分析阶段性总结\n\n");
            prompt.append("你正在帮助用户分析一份文档，目前已完成 ")
                  .append(currentPoint).append("% 的分析。\n\n");

            prompt.append("## 已分析内容概要\n\n");

            // 添加已分析的内容
            for (MemoEntry entry : longTermMemo) {
                prompt.append("### 第 ").append(entry.getSegmentIndex()).append(" 部分");
                if (entry.getTitle() != null) {
                    prompt.append(": ").append(entry.getTitle());
                }
                prompt.append("\n");
                prompt.append(entry.getEffectiveContent()).append("\n\n");
            }

            for (MemoEntry entry : shortTermMemory) {
                prompt.append("### 第 ").append(entry.getSegmentIndex()).append(" 部分");
                if (entry.getTitle() != null) {
                    prompt.append(": ").append(entry.getTitle());
                }
                prompt.append("\n");
                prompt.append(entry.getEffectiveContent()).append("\n\n");
            }

            prompt.append("## 任务\n\n");
            prompt.append("请生成一个简洁的阶段性总结（100-200字），包括：\n");
            prompt.append("1. 目前为止的主要内容\n");
            prompt.append("2. 关键信息点\n");
            prompt.append("3. 可能的后续内容预测\n\n");
            prompt.append("直接输出总结内容：\n");

            return llmClient.generate(prompt.toString());

        } catch (Exception e) {
            log.warn("生成阶段总结失败，使用简单总结: {}", e.getMessage());
            return generateSimpleSummary(progress, shortTermMemory, longTermMemo);
        }
    }

    /**
     * 生成简单总结（无 LLM 时使用）
     */
    private String generateSimpleSummary(AnalysisProgress progress,
                                         List<MemoEntry> shortTermMemory,
                                         List<MemoEntry> longTermMemo) {
        StringBuilder summary = new StringBuilder();

        summary.append("已分析 ").append(progress.getAnalyzedCount())
               .append("/").append(progress.getTotalSegments()).append(" 个片段。\n\n");

        summary.append("**最近分析的内容**：\n");
        for (MemoEntry entry : shortTermMemory) {
            summary.append("- 第 ").append(entry.getSegmentIndex()).append(" 部分");
            if (entry.getTitle() != null) {
                summary.append(": ").append(entry.getTitle());
            }
            summary.append("\n");
        }

        if (!longTermMemo.isEmpty()) {
            summary.append("\n**重要条目数**: ").append(
                    longTermMemo.stream().filter(MemoEntry::isIndependent).count()
            ).append(" 个\n");
        }

        return summary.toString();
    }
}

