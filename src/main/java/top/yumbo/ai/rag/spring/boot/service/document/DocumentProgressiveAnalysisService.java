package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.model.document.*;
import top.yumbo.ai.rag.spring.boot.service.parser.DocumentParser;
import top.yumbo.ai.rag.spring.boot.service.parser.DocumentParserFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用文档渐进式分析服务
 *
 * 支持多种文档类型的渐进式分析，包括：
 * - PPT/PPTX
 * - PDF
 * - Word/DOCX
 *
 * 使用备忘录系统管理长文档的分析记忆。
 */
@Slf4j
@Service
public class DocumentProgressiveAnalysisService {

    private final LLMClient llmClient;
    private final DocumentMemoManager memoManager;
    private final DocumentParserFactory parserFactory;
    private final StageOutputManager stageOutputManager;

    @Autowired
    public DocumentProgressiveAnalysisService(
            LLMClient llmClient,
            DocumentMemoManager memoManager,
            DocumentParserFactory parserFactory,
            StageOutputManager stageOutputManager) {
        this.llmClient = llmClient;
        this.memoManager = memoManager;
        this.parserFactory = parserFactory;
        this.stageOutputManager = stageOutputManager;
    }

    /**
     * 渐进式分析文档
     *
     * @param documentPath 文档路径
     * @param question 用户问题
     * @return 分析报告
     */
    public DocumentAnalysisReport analyzeProgressively(String documentPath, String question) {
        File file = new File(documentPath);
        return analyzeProgressively(file, question);
    }

    /**
     * 渐进式分析文档
     *
     * @param file 文档文件
     * @param question 用户问题
     * @return 分析报告
     */
    public DocumentAnalysisReport analyzeProgressively(File file, String question) {
        DocumentAnalysisReport report = new DocumentAnalysisReport();
        report.setFileName(file.getName());
        report.setFilePath(file.getAbsolutePath());
        report.setQuestion(question);
        report.setStartTime(System.currentTimeMillis());

        // 获取解析器
        DocumentParser parser = parserFactory.getParser(file.getAbsolutePath());
        if (parser == null) {
            report.setSuccess(false);
            report.setErrorMessage("不支持的文档类型: " + file.getName());
            report.setEndTime(System.currentTimeMillis());
            return report;
        }

        log.info("📚 开始渐进式分析文档: {} (解析器: {})", file.getName(), parser.getParserName());

        try {
            // 解析文档为片段
            List<DocumentSegment> segments = parser.parse(file.getAbsolutePath());
            int totalSegments = segments.size();

            log.info("📄 文档共 {} 个片段", totalSegments);

            // 初始化
            DocumentSource source = segments.isEmpty() ? null : segments.get(0).getSource();
            if (source != null) {
                memoManager.startNewDocument(source);
            }
            stageOutputManager.clear();

            // 逐片段分析
            for (int i = 0; i < segments.size(); i++) {
                DocumentSegment segment = segments.get(i);
                int segmentIndex = i + 1;

                log.info("🔍 分析片段 {}/{}: {}", segmentIndex, totalSegments, segment.getTitle());

                // 渐进式分析
                String analysis = analyzeSegmentWithMemory(segment, question, segmentIndex, totalSegments);

                // 提取关键点
                String keyPoints = extractKeyPoints(analysis);

                // 保存到备忘录
                memoManager.addSegmentAnalysis(segment, analysis, keyPoints);

                // 记录片段完成
                stageOutputManager.recordSegmentCompletion(segmentIndex, keyPoints);

                // 记录分析结果
                SegmentAnalysisResult result = new SegmentAnalysisResult();
                result.setSegmentIndex(segmentIndex);
                result.setSegmentType(segment.getType());
                result.setTitle(segment.getTitle());
                result.setContent(segment.getTextContent());
                result.setAnalysis(analysis);
                result.setKeyPoints(keyPoints);
                report.getSegmentResults().add(result);

                // 检查是否需要生成阶段性输出
                AnalysisProgress progress = memoManager.getProgress();
                if (stageOutputManager.shouldGenerateStageOutput(progress)) {
                    StageOutputManager.StageOutput stageOutput = stageOutputManager.generateStageOutput(
                            progress,
                            memoManager.getShortTermMemory(),
                            memoManager.getLongTermMemo()
                    );
                    if (stageOutput != null) {
                        report.getStageOutputs().add(stageOutput);
                        log.info("📊 生成阶段性输出: {}", stageOutput.getStageName());
                    }
                }

                log.info("✅ 片段 {} 分析完成", segmentIndex);
            }

            // 生成最终总结
            generateComprehensiveSummary(report, question);

            // 导出备忘录
            report.setMemoDocument(memoManager.exportToMarkdown());

            report.setSuccess(true);
            report.setEndTime(System.currentTimeMillis());

            log.info("🎉 文档分析完成，耗时: {}ms", report.getEndTime() - report.getStartTime());

        } catch (Exception e) {
            log.error("文档分析失败: {}", file.getName(), e);
            report.setSuccess(false);
            report.setErrorMessage(e.getMessage());
            report.setEndTime(System.currentTimeMillis());
        }

        return report;
    }

    /**
     * 检查是否支持该文档类型
     */
    public boolean isSupported(String documentPath) {
        return parserFactory.isSupported(documentPath);
    }

    /**
     * 获取支持的文档类型
     */
    public List<String> getSupportedTypes() {
        return parserFactory.getAllSupportedTypes();
    }

    // ==================== 私有方法 ====================

    /**
     * 带记忆上下文分析片段
     */
    private String analyzeSegmentWithMemory(DocumentSegment segment, String question,
                                            int segmentIndex, int totalSegments) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 文档渐进式分析\n\n");
        prompt.append("你正在帮助用户逐步分析一份文档，需要**像人类一样渐进式理解内容**。\n\n");

        prompt.append("## 用户问题\n");
        prompt.append(question).append("\n\n");

        prompt.append("## 当前进度\n");
        prompt.append("- 当前: 第 ").append(segmentIndex).append(" 部分 / 共 ").append(totalSegments).append(" 部分\n");
        prompt.append("- 完成度: ").append(String.format("%.1f%%", segmentIndex * 100.0 / totalSegments)).append("\n");
        prompt.append("- 片段类型: ").append(segment.getType().getDisplayName()).append("\n\n");

        // 添加短期记忆
        List<MemoEntry> shortTermMemory = memoManager.getShortTermMemory();
        if (!shortTermMemory.isEmpty()) {
            prompt.append("## 📚 最近的内容要点\n\n");
            for (MemoEntry mem : shortTermMemory) {
                prompt.append("**第 ").append(mem.getSegmentIndex()).append(" 部分");
                if (mem.getTitle() != null && !mem.getTitle().isEmpty()) {
                    prompt.append(" - ").append(mem.getTitle());
                }
                prompt.append("**:\n");
                prompt.append(mem.getEffectiveContent()).append("\n\n");
            }
        }

        // 添加召回的相关备忘录
        List<MemoEntry> recalledMemos = memoManager.recallRelevantMemos(segment, 500);
        if (!recalledMemos.isEmpty()) {
            prompt.append("## 📋 相关历史内容\n\n");
            for (MemoEntry mem : recalledMemos) {
                prompt.append("【第 ").append(mem.getSegmentIndex()).append(" 部分");
                if (mem.getTitle() != null && !mem.getTitle().isEmpty()) {
                    prompt.append(" - ").append(mem.getTitle());
                }
                prompt.append("】\n");
                prompt.append("> ").append(mem.getEffectiveContent().replace("\n", "\n> ")).append("\n\n");
            }
        }

        // 当前片段内容
        prompt.append("## 📄 当前片段\n\n");
        prompt.append("**标题**: ").append(segment.getTitle()).append("\n\n");

        if (segment.getTextContent() != null && !segment.getTextContent().isEmpty()) {
            prompt.append("**内容**:\n");
            // 限制内容长度
            String content = segment.getTextContent();
            if (content.length() > 3000) {
                content = content.substring(0, 3000) + "\n...[内容过长已截断]";
            }
            prompt.append(content).append("\n\n");
        }

        if (segment.getImages() != null && !segment.getImages().isEmpty()) {
            prompt.append("**包含图片**: ").append(segment.getImages().size()).append(" 张\n\n");
        }

        if (segment.getTables() != null && !segment.getTables().isEmpty()) {
            prompt.append("**包含表格**: ").append(segment.getTables().size()).append(" 个\n\n");
        }

        // 分析指导
        prompt.append("## 🎯 分析指导\n\n");
        prompt.append("1. **理解当前内容**: 这部分讲了什么？核心观点是什么？\n");
        prompt.append("2. **承接前文**: 与前面的内容有什么联系？\n");
        prompt.append("3. **提炼要点**: 找出2-3个最重要的信息点\n");
        prompt.append("4. **关注问题**: 重点关注与用户问题相关的内容\n\n");

        prompt.append("## 📝 请提供分析\n\n");
        prompt.append("请按以下格式输出:\n\n");
        prompt.append("### 本部分分析\n");
        prompt.append("[你对这部分内容的理解和分析]\n\n");
        prompt.append("### 核心要点 (KEY_POINTS)\n");
        prompt.append("- [要点1]\n");
        prompt.append("- [要点2]\n");
        prompt.append("- [要点3]\n");
        prompt.append("(END_KEY_POINTS)\n");

        try {
            return llmClient.generate(prompt.toString());
        } catch (Exception e) {
            log.error("片段 {} 分析失败", segmentIndex, e);
            return "分析失败: " + e.getMessage();
        }
    }

    /**
     * 提取关键点
     */
    private String extractKeyPoints(String analysis) {
        int startIdx = analysis.indexOf("KEY_POINTS");
        int endIdx = analysis.indexOf("END_KEY_POINTS");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            String keyPoints = analysis.substring(startIdx + 10, endIdx).trim();
            return keyPoints.replaceAll("(?m)^\\s*#+\\s*", "")
                           .replaceAll("(?m)^\\s*\\(.*\\)\\s*$", "")
                           .trim();
        }

        return analysis.length() > 200 ? analysis.substring(0, 200) + "..." : analysis;
    }

    /**
     * 生成综合总结
     */
    private void generateComprehensiveSummary(DocumentAnalysisReport report, String question) {
        try {
            log.info("📊 生成文档综合总结...");

            StringBuilder prompt = new StringBuilder();

            prompt.append("# 文档完整总结任务\n\n");
            prompt.append("你已经逐步分析完一份文档的所有内容。现在需要生成一个完整、连贯的总结报告。\n\n");

            prompt.append("## 用户问题\n");
            prompt.append(question).append("\n\n");

            // 使用备忘录摘要
            prompt.append(memoManager.getAllMemosSummary());

            // 添加独立重要条目
            List<MemoEntry> independentEntries = memoManager.getIndependentEntries();
            if (!independentEntries.isEmpty()) {
                prompt.append("## ⭐ 独立重要条目\n\n");
                for (MemoEntry entry : independentEntries) {
                    prompt.append("### 第 ").append(entry.getSegmentIndex())
                          .append(" 部分: ").append(entry.getTitle()).append("\n");
                    prompt.append(entry.getEffectiveContent()).append("\n\n");
                }
            }

            prompt.append("## 总结要求\n\n");
            prompt.append("1. **整体把握**: 理解文档的整体结构和逻辑脉络\n");
            prompt.append("2. **要点提炼**: 突出最核心的3-5个观点\n");
            prompt.append("3. **回答问题**: 直接、清晰地回答用户的问题\n");
            prompt.append("4. **结构清晰**: 使用标题、列表等组织内容\n");
            prompt.append("5. **连贯表达**: 确保内容前后连贯，逻辑通顺\n\n");

            prompt.append("请生成最终总结报告:\n");

            String summary = llmClient.generate(prompt.toString());
            report.setComprehensiveSummary(summary);

            log.info("✅ 综合总结生成完成");

        } catch (Exception e) {
            log.error("生成综合总结失败", e);
            report.setComprehensiveSummary(generateDefaultSummary(report));
        }
    }

    /**
     * 生成默认总结
     */
    private String generateDefaultSummary(DocumentAnalysisReport report) {
        StringBuilder summary = new StringBuilder();

        summary.append("# ").append(report.getFileName()).append(" - 文档分析报告\n\n");
        summary.append("**问题**: ").append(report.getQuestion()).append("\n\n");
        summary.append("**片段数**: ").append(report.getSegmentResults().size()).append("\n\n");
        summary.append("---\n\n");

        summary.append("## 逐部分要点\n\n");

        for (SegmentAnalysisResult result : report.getSegmentResults()) {
            summary.append("### ").append(result.getSegmentIndex())
                   .append(". ").append(result.getTitle()).append("\n\n");

            if (result.getKeyPoints() != null) {
                summary.append(result.getKeyPoints()).append("\n\n");
            }
        }

        return summary.toString();
    }

    // ==================== 数据类 ====================

    /**
     * 文档分析报告
     */
    @Data
    public static class DocumentAnalysisReport {
        private String fileName;
        private String filePath;
        private String question;
        private long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;
        private List<SegmentAnalysisResult> segmentResults = new ArrayList<>();
        private List<StageOutputManager.StageOutput> stageOutputs = new ArrayList<>();
        private String comprehensiveSummary;
        private String memoDocument;

        /**
         * 获取耗时（毫秒）
         */
        public long getDuration() {
            return endTime - startTime;
        }
    }

    /**
     * 片段分析结果
     */
    @Data
    public static class SegmentAnalysisResult {
        private int segmentIndex;
        private SegmentType segmentType;
        private String title;
        private String content;
        private String analysis;
        private String keyPoints;
    }
}

