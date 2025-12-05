package top.yumbo.ai.rag.spring.boot.service.document;

import top.yumbo.ai.rag.spring.boot.model.document.AnalysisProgress;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;

import java.util.List;

/**
 * 阶段性输出管理器接口
 *
 * 负责在文档分析过程中管理阶段性输出：
 * - 在特定进度点（25%、50%、75%）生成中间总结
 * - 支持实时片段输出
 * - 自动导出备忘录文档
 */
public interface StageOutputManager {

    /**
     * 检查是否应该生成阶段性输出
     *
     * @param progress 当前分析进度
     * @return 是否应该输出
     */
    boolean shouldGenerateStageOutput(AnalysisProgress progress);

    /**
     * 生成阶段性总结
     *
     * @param progress 当前分析进度
     * @param shortTermMemory 短期记忆
     * @param longTermMemo 长期备忘录
     * @return 阶段性总结
     */
    StageOutput generateStageOutput(AnalysisProgress progress,
                                    List<MemoEntry> shortTermMemory,
                                    List<MemoEntry> longTermMemo);

    /**
     * 记录片段分析完成
     *
     * @param segmentIndex 片段索引
     * @param keyPoints 关键点
     */
    void recordSegmentCompletion(int segmentIndex, String keyPoints);

    /**
     * 获取所有阶段性输出
     *
     * @return 阶段性输出列表
     */
    List<StageOutput> getAllStageOutputs();

    /**
     * 获取最新的阶段性输出
     *
     * @return 最新的阶段性输出，如果没有则返回 null
     */
    StageOutput getLatestStageOutput();

    /**
     * 清空所有输出记录
     */
    void clear();

    /**
     * 阶段性输出
     */
    class StageOutput {
        /** 阶段名称 */
        private String stageName;

        /** 进度百分比 */
        private double progressPercent;

        /** 已分析片段数 */
        private int analyzedCount;

        /** 总片段数 */
        private int totalCount;

        /** 阶段总结 */
        private String summary;

        /** 关键发现 */
        private List<String> keyFindings;

        /** 生成时间戳 */
        private long timestamp;

        /** 是否为最终输出 */
        private boolean isFinal;

        // Getters and Setters

        public String getStageName() {
            return stageName;
        }

        public void setStageName(String stageName) {
            this.stageName = stageName;
        }

        public double getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(double progressPercent) {
            this.progressPercent = progressPercent;
        }

        public int getAnalyzedCount() {
            return analyzedCount;
        }

        public void setAnalyzedCount(int analyzedCount) {
            this.analyzedCount = analyzedCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getKeyFindings() {
            return keyFindings;
        }

        public void setKeyFindings(List<String> keyFindings) {
            this.keyFindings = keyFindings;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public void setFinal(boolean aFinal) {
            isFinal = aFinal;
        }

        /**
         * 格式化为 Markdown
         */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();

            sb.append("## ").append(stageName).append("\n\n");
            sb.append("> **进度**: ").append(String.format("%.1f%%", progressPercent));
            sb.append(" (").append(analyzedCount).append("/").append(totalCount).append(")\n\n");

            if (keyFindings != null && !keyFindings.isEmpty()) {
                sb.append("### 关键发现\n\n");
                for (String finding : keyFindings) {
                    sb.append("- ").append(finding).append("\n");
                }
                sb.append("\n");
            }

            if (summary != null && !summary.isEmpty()) {
                sb.append("### 阶段总结\n\n");
                sb.append(summary).append("\n\n");
            }

            return sb.toString();
        }
    }
}

