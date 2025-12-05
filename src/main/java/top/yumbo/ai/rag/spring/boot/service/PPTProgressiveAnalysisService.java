package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * PPT 渐进式分析服务
 *
 * 以幻灯片为最小单位，模拟人类阅读PPT的方式：
 * 1. 逐页阅读幻灯片
 * 2. 提取每页的核心观点
 * 3. 维护阅读记忆（前几页的关键点）
 * 4. 根据上下文理解整体结构
 * 5. 生成连贯的总结报告
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Slf4j
@Service
public class PPTProgressiveAnalysisService {

    private final KnowledgeQAService knowledgeQAService;
    private final LLMClient llmClient;

    public PPTProgressiveAnalysisService(KnowledgeQAService knowledgeQAService,
                                         LLMClient llmClient) {
        this.knowledgeQAService = knowledgeQAService;
        this.llmClient = llmClient;
    }

    /**
     * 渐进式分析PPT
     */
    public PPTAnalysisReport analyzeProgressively(File pptFile, String question) {
        PPTAnalysisReport report = new PPTAnalysisReport();
        report.setFileName(pptFile.getName());
        report.setQuestion(question);
        report.setStartTime(System.currentTimeMillis());

        try (FileInputStream fis = new FileInputStream(pptFile);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            List<XSLFSlide> slides = ppt.getSlides();
            int totalSlides = slides.size();

            log.info("📊 开始渐进式分析PPT: {} ({} 张幻灯片)", pptFile.getName(), totalSlides);

            // 初始化记忆管理器（保留最近3张幻灯片的要点）
            SlideMemoryManager memory = new SlideMemoryManager(3);

            // 逐张幻灯片分析
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                int slideNumber = i + 1;

                log.info("🔍 分析幻灯片 {}/{}", slideNumber, totalSlides);

                // 提取幻灯片内容
                SlideContent slideContent = extractSlideContent(slide, slideNumber);

                // 渐进式分析（带记忆上下文）
                String analysis = analyzeSlideWithMemory(
                    slideContent, question, slideNumber, totalSlides, memory
                );

                // 提取关键点
                String keyPoints = extractKeyPointsFromAnalysis(analysis);

                // 保存到记忆
                memory.addMemory(slideNumber, slideContent.getTitle(), keyPoints);

                // 记录结果
                SlideAnalysisResult result = new SlideAnalysisResult();
                result.setSlideNumber(slideNumber);
                result.setTitle(slideContent.getTitle());
                result.setContent(slideContent.getText());
                result.setImageCount(slideContent.getImageCount());
                result.setAnalysis(analysis);
                result.setKeyPoints(keyPoints);

                report.getSlideResults().add(result);

                log.info("✅ 幻灯片 {} 分析完成，关键点: {}", slideNumber,
                    keyPoints.length() > 50 ? keyPoints.substring(0, 50) + "..." : keyPoints);
            }

            // 生成最终总结
            generateComprehensiveSummary(report, memory, question);

            report.setEndTime(System.currentTimeMillis());
            report.setSuccess(true);

            log.info("🎉 PPT渐进式分析完成，耗时: {}ms",
                report.getEndTime() - report.getStartTime());

        } catch (Exception e) {
            log.error("PPT分析失败", e);
            report.setSuccess(false);
            report.setErrorMessage(e.getMessage());
            report.setEndTime(System.currentTimeMillis());
        }

        return report;
    }

    /**
     * 提取幻灯片内容
     */
    private SlideContent extractSlideContent(XSLFSlide slide, int slideNumber) {
        SlideContent content = new SlideContent();
        content.setSlideNumber(slideNumber);

        StringBuilder text = new StringBuilder();
        String title = "";
        int imageCount = 0;

        // 提取标题和文本
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape) shape;
                String shapeText = textShape.getText();

                if (shapeText != null && !shapeText.trim().isEmpty()) {
                    // 第一个文本框通常是标题
                    if (title.isEmpty() && textShape instanceof XSLFTextBox) {
                        title = shapeText.trim();
                    }
                    text.append(shapeText).append("\n");
                }
            } else if (shape instanceof XSLFPictureShape) {
                imageCount++;
            }
        }

        content.setTitle(title.isEmpty() ? "幻灯片 " + slideNumber : title);
        content.setText(text.toString().trim());
        content.setImageCount(imageCount);

        return content;
    }

    /**
     * 带记忆上下文的幻灯片分析
     */
    private String analyzeSlideWithMemory(SlideContent slideContent, String question,
                                         int slideNumber, int totalSlides,
                                         SlideMemoryManager memory) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# PPT幻灯片渐进式分析\n\n");
        prompt.append("你正在帮助用户逐张分析一份PPT，需要**像人类一样渐进式理解演示内容**。\n\n");

        prompt.append("## 用户问题\n");
        prompt.append(question).append("\n\n");

        prompt.append("## 当前进度\n");
        prompt.append("- 当前: 第 ").append(slideNumber).append(" 张 / 共 ").append(totalSlides).append(" 张\n");
        prompt.append("- 完成度: ").append(String.format("%.1f%%", slideNumber * 100.0 / totalSlides)).append("\n\n");

        // 添加记忆上下文
        if (slideNumber > 1 && !memory.isEmpty()) {
            prompt.append("## 📚 前面幻灯片的核心要点\n");
            prompt.append("*(这些是你看过的前面幻灯片的关键信息)*\n\n");

            List<SlideMemory> recentMemories = memory.getRecentMemories();
            for (SlideMemory mem : recentMemories) {
                prompt.append("**第 ").append(mem.getSlideNumber()).append(" 张");
                if (!mem.getTitle().isEmpty()) {
                    prompt.append(" - ").append(mem.getTitle());
                }
                prompt.append("**:\n");
                prompt.append(mem.getKeyPoints()).append("\n\n");
            }
        }

        // 当前幻灯片内容
        prompt.append("## 📄 当前幻灯片\n\n");
        prompt.append("**标题**: ").append(slideContent.getTitle()).append("\n\n");

        if (!slideContent.getText().isEmpty()) {
            prompt.append("**文字内容**:\n");
            prompt.append(slideContent.getText()).append("\n\n");
        }

        if (slideContent.getImageCount() > 0) {
            prompt.append("**包含图片**: ").append(slideContent.getImageCount()).append(" 张\n\n");
        }

        // 分析指导
        prompt.append("## 🎯 分析指导\n\n");
        prompt.append("1. **理解当前页**: 这张幻灯片讲了什么？核心观点是什么？\n");
        prompt.append("2. **承接前文**: 与前面的内容有什么联系？是递进、转折还是并列？\n");
        prompt.append("3. **提炼要点**: 找出2-3个最重要的信息点\n");
        prompt.append("4. **关注问题**: 重点关注与用户问题相关的内容\n");

        if (slideNumber == 1) {
            prompt.append("5. **开篇分析**: 这是第一张，通常包含主题或总览\n");
        } else if (slideNumber == totalSlides) {
            prompt.append("5. **收尾总结**: 这是最后一张，通常包含总结或结论\n");
        } else {
            prompt.append("5. **中间分析**: 这是中间部分，注意内容的连贯性\n");
        }

        prompt.append("\n## 📝 请提供分析\n\n");
        prompt.append("请按以下格式输出:\n\n");
        prompt.append("### 本页分析\n");
        prompt.append("[你对这张幻灯片的理解和分析]\n\n");
        prompt.append("### 核心要点 (KEY_POINTS)\n");
        prompt.append("- [要点1]\n");
        prompt.append("- [要点2]\n");
        prompt.append("- [要点3]\n");
        prompt.append("(END_KEY_POINTS)\n");

        try {
            // 直接调用 LLM，不需要通过 RAG 搜索
            // PPT 分析是基于当前幻灯片内容，不需要检索知识库
            return llmClient.generate(prompt.toString());
        } catch (Exception e) {
            log.error("幻灯片 {} 分析失败", slideNumber, e);
            return "处理问答时发生错误: " + e.getMessage();
        }
    }

    /**
     * 从分析中提取关键点
     */
    private String extractKeyPointsFromAnalysis(String analysis) {
        int startIdx = analysis.indexOf("KEY_POINTS");
        int endIdx = analysis.indexOf("END_KEY_POINTS");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            String keyPoints = analysis.substring(startIdx + 10, endIdx).trim();
            // 清理格式
            return keyPoints.replaceAll("(?m)^\\s*#+\\s*", "")
                           .replaceAll("(?m)^\\s*\\(.*\\)\\s*$", "")
                           .trim();
        }

        // 降级：取前200字符
        return analysis.length() > 200 ? analysis.substring(0, 200) + "..." : analysis;
    }

    /**
     * 生成综合总结
     */
    private void generateComprehensiveSummary(PPTAnalysisReport report,
                                             SlideMemoryManager memory,
                                             String question) {
        try {
            log.info("📊 生成PPT综合总结...");

            StringBuilder summaryPrompt = new StringBuilder();

            summaryPrompt.append("# PPT完整总结任务\n\n");
            summaryPrompt.append("你已经逐张分析完一份PPT的所有幻灯片。现在需要生成一个完整、连贯的总结报告。\n\n");

            summaryPrompt.append("## 用户问题\n");
            summaryPrompt.append(question).append("\n\n");

            summaryPrompt.append("## PPT结构与要点\n\n");

            for (SlideAnalysisResult result : report.getSlideResults()) {
                summaryPrompt.append("### 第 ").append(result.getSlideNumber())
                            .append(" 张: ").append(result.getTitle()).append("\n");
                if (result.getKeyPoints() != null && !result.getKeyPoints().isEmpty()) {
                    summaryPrompt.append(result.getKeyPoints()).append("\n");
                }
                summaryPrompt.append("\n");
            }

            summaryPrompt.append("## 总结要求\n\n");
            summaryPrompt.append("1. **整体把握**: 理解PPT的整体结构和逻辑脉络\n");
            summaryPrompt.append("2. **要点提炼**: 突出最核心的3-5个观点\n");
            summaryPrompt.append("3. **回答问题**: 直接、清晰地回答用户的问题\n");
            summaryPrompt.append("4. **结构清晰**: 使用标题、列表等组织内容\n");
            summaryPrompt.append("5. **连贯表达**: 确保内容前后连贯，逻辑通顺\n\n");

            summaryPrompt.append("请生成最终总结报告:\n");

            // 直接调用 LLM 生成总结，不进行文档检索
            // 这样可以避免将包含特殊字符的长 prompt 传递给 Lucene 导致解析错误
            log.info("📝 直接调用 LLM 生成最终总结（跳过文档检索）");
            String summary = llmClient.generate(summaryPrompt.toString());
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
    private String generateDefaultSummary(PPTAnalysisReport report) {
        StringBuilder summary = new StringBuilder();

        summary.append("# ").append(report.getFileName()).append(" - PPT分析报告\n\n");
        summary.append("**问题**: ").append(report.getQuestion()).append("\n\n");
        summary.append("**幻灯片数**: ").append(report.getSlideResults().size()).append(" 张\n\n");
        summary.append("---\n\n");

        summary.append("## 逐页要点\n\n");

        for (SlideAnalysisResult result : report.getSlideResults()) {
            summary.append("### ").append(result.getSlideNumber())
                  .append(". ").append(result.getTitle()).append("\n\n");

            if (result.getKeyPoints() != null) {
                summary.append(result.getKeyPoints()).append("\n\n");
            }
        }

        return summary.toString();
    }

    /**
     * 幻灯片内容
     */
    @Data
    private static class SlideContent {
        private int slideNumber;
        private String title;
        private String text;
        private int imageCount;
    }

    /**
     * 幻灯片记忆管理器
     */
    private static class SlideMemoryManager {
        private final int maxMemorySize;
        private final LinkedList<SlideMemory> memories;

        public SlideMemoryManager(int maxMemorySize) {
            this.maxMemorySize = maxMemorySize;
            this.memories = new LinkedList<>();
        }

        public void addMemory(int slideNumber, String title, String keyPoints) {
            SlideMemory memory = new SlideMemory();
            memory.setSlideNumber(slideNumber);
            memory.setTitle(title);
            memory.setKeyPoints(keyPoints);

            memories.add(memory);

            // 保持固定大小
            while (memories.size() > maxMemorySize) {
                memories.removeFirst();
            }
        }

        public List<SlideMemory> getRecentMemories() {
            return new ArrayList<>(memories);
        }

        public boolean isEmpty() {
            return memories.isEmpty();
        }
    }

    /**
     * 幻灯片记忆
     */
    @Data
    private static class SlideMemory {
        private int slideNumber;
        private String title;
        private String keyPoints;
    }

    /**
     * PPT分析报告
     */
    @Data
    public static class PPTAnalysisReport {
        private String fileName;
        private String question;
        private long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;
        private List<SlideAnalysisResult> slideResults = new ArrayList<>();
        private String comprehensiveSummary;
    }

    /**
     * 幻灯片分析结果
     */
    @Data
    public static class SlideAnalysisResult {
        private int slideNumber;
        private String title;
        private String content;
        private int imageCount;
        private String analysis;      // 本页分析
        private String keyPoints;     // 关键点
    }
}

