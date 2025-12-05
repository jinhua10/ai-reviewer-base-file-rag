package top.yumbo.ai.rag.spring.boot.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisProgress {
    /** 文档来源 */
    private DocumentSource source;

    /** 当前片段索引 */
    private int currentIndex;

    /** 总片段数 */
    private int totalSegments;

    /** 已分析片段数 */
    private int analyzedCount;

    /** 短期记忆条目数 */
    private int shortTermMemorySize;

    /** 长期备忘录条目数 */
    private int longTermMemoSize;

    /** 独立重要条目数 */
    private int independentEntryCount;

    /** 是否已完成 */
    @Builder.Default
    private boolean completed = false;

    /** 开始时间（毫秒） */
    private long startTimeMs;

    /** 已耗时（毫秒） */
    private long elapsedTimeMs;

    /**
     * 获取进度百分比
     */
    public double getProgressPercent() {
        if (totalSegments <= 0) {
            return 0;
        }
        return (double) analyzedCount * 100 / totalSegments;
    }

    /**
     * 获取格式化的进度字符串
     */
    public String getProgressString() {
        return String.format("%.1f%% (%d/%d)", getProgressPercent(), analyzedCount, totalSegments);
    }

    /**
     * 判断是否达到阶段性输出点
     */
    public boolean isAtStagePoint(int[] stagePoints) {
        double percent = getProgressPercent();
        for (int point : stagePoints) {
            // 允许 1% 的误差
            if (Math.abs(percent - point) < 1.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新已耗时
     */
    public void updateElapsedTime() {
        if (startTimeMs > 0) {
            this.elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
        }
    }

    /**
     * 预估剩余时间（毫秒）
     */
    public long estimateRemainingTimeMs() {
        if (analyzedCount <= 0 || elapsedTimeMs <= 0) {
            return -1;
        }
        double avgTimePerSegment = (double) elapsedTimeMs / analyzedCount;
        int remaining = totalSegments - analyzedCount;
        return (long) (avgTimePerSegment * remaining);
    }
}

