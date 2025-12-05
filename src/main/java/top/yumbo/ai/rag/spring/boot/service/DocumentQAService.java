package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 完整文档AI问答服务
 *
 * 功能：
 * 1. 对完整文档进行AI问答
 * 2. 支持大文档分批处理
 * 3. 临时持久化每批结果
 * 4. 合并生成最终报告
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Slf4j
@Service
public class DocumentQAService {

    private final KnowledgeQAService knowledgeQAService;
    private final KnowledgeQAProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String tempDir;

    public DocumentQAService(KnowledgeQAService knowledgeQAService,
                            KnowledgeQAProperties properties) {
        this.knowledgeQAService = knowledgeQAService;
        this.properties = properties;
    }

    /**
     * 初始化临时目录（Initialize temp directory）
     */
    private void initTempDir(String storagePath) {
        this.tempDir = storagePath + File.separator + ".doc_qa_temp";
        Path tempDirPath = Paths.get(tempDir);

        try {
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
                log.info(LogMessageProvider.getMessage("doc_qa.log.create_temp_dir", tempDir));
            }
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.create_temp_dir_failed"), e);
        }
    }

    /**
     * 对完整文档进行AI问答（Query document with AI）
     *
     * @param documentPath 文档路径（Document path）
     * @param question 问题（Question）
     * @param storagePath 知识库存储路径（Knowledge base storage path）
     * @return 问答报告（QA Report）
     */
    public DocumentQAReport queryDocument(String documentPath, String question, String storagePath) {
        initTempDir(storagePath);

        File docFile = new File(documentPath);
        if (!docFile.exists()) {
            throw new IllegalArgumentException("Document not found: " + documentPath);
        }

        String sessionId = UUID.randomUUID().toString();
        log.info(LogMessageProvider.getMessage("doc_qa.log.start_qa", docFile.getName(), sessionId));
        log.info(LogMessageProvider.getMessage("doc_qa.log.question", question));

        DocumentQAReport report = new DocumentQAReport();
        report.setSessionId(sessionId);
        report.setDocumentName(docFile.getName());
        report.setQuestion(question);
        report.setStartTime(System.currentTimeMillis());

        try {
            // 1. 检查文档大小并决定是否分批（Check document size and decide whether to batch）
            long fileSize = docFile.length();
            int maxChunkSize = properties.getDocument().getMaxIndexContentLength();

            boolean needsChunking = shouldChunkDocument(docFile, maxChunkSize);

            if (needsChunking) {
                log.info(LogMessageProvider.getMessage("doc_qa.log.batch_mode"));
                processInChunks(docFile, question, sessionId, report);
            } else {
                log.info(LogMessageProvider.getMessage("doc_qa.log.direct_mode"));
                processDirectly(docFile, question, sessionId, report);
            }

            // 2. 生成最终报告（Generate final report）
            generateFinalReport(report);

            report.setEndTime(System.currentTimeMillis());
            report.setSuccess(true);

            log.info(LogMessageProvider.getMessage("doc_qa.log.qa_complete",
                docFile.getName(), report.getEndTime() - report.getStartTime()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.qa_error"), e);
            report.setSuccess(false);
            report.setErrorMessage(e.getMessage());
            report.setEndTime(System.currentTimeMillis());
        }

        return report;
    }

    /**
     * 判断是否需要分块处理（Check if chunking is needed）
     */
    private boolean shouldChunkDocument(File docFile, int maxContentLength) {
        // 简单估算：假设1KB文件约产生1-2字符的内容（Simple estimation）
        long estimatedContentLength = docFile.length() * 2;
        return estimatedContentLength > maxContentLength;
    }

    /**
     * 直接处理整个文档（Process document directly）
     */
    private void processDirectly(File docFile, String question, String sessionId, DocumentQAReport report) {
        try {
            // 使用知识库服务进行问答（Use knowledge base service for QA）
            AIAnswer aiAnswer = knowledgeQAService.ask(question);
            String answer = aiAnswer.getAnswer();

            BatchResult result = new BatchResult();
            result.setBatchId(1);
            result.setTotalBatches(1);
            result.setQuestion(question);
            result.setAnswer(answer);
            result.setTimestamp(System.currentTimeMillis());

            report.getBatchResults().add(result);

            // 保存临时结果（Save temp result）
            saveBatchResult(sessionId, result);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.direct_process_failed"), e);
            throw new RuntimeException("Processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 分批处理文档（带记忆的渐进式分析）
     * 模拟人类阅读方式：
     * 1. 逐批次分析内容
     * 2. 提取关键信息到记忆中
     * 3. 后续批次带上之前的关键记忆
     * 4. 适当遗忘细节，聚焦重点
     */
    private void processInChunks(File docFile, String question, String sessionId, DocumentQAReport report) {
        try {
            // 读取文档内容（Read document content）
            String content = readDocumentContent(docFile);

            // 分割成多个批次（Split into batches）
            int maxChunkSize = properties.getDocument().getMaxIndexContentLength() / 2;
            List<String> chunks = splitContent(content, maxChunkSize);

            log.info(LogMessageProvider.getMessage("doc_qa.log.split_batches", chunks.size()));

            // 初始化记忆管理器（Initialize memory manager）
            ProgressiveMemory memory = new ProgressiveMemory(3); // 保留最近3个批次的关键信息

            // 逐批处理（Process each batch）
            for (int i = 0; i < chunks.size(); i++) {
                int batchId = i + 1;
                String chunk = chunks.get(i);

                log.info(LogMessageProvider.getMessage("doc_qa.log.process_batch", batchId, chunks.size(), chunk.length()));

                // 构建带记忆的提示词（Build prompt with memory）
                String batchPrompt = buildProgressivePrompt(
                    question, chunk, batchId, chunks.size(), memory
                );

                // 调用AI问答（Call AI QA）
                AIAnswer aiAnswer = knowledgeQAService.ask(batchPrompt);
                String answer = aiAnswer.getAnswer();

                // 提取本批次的关键信息并加入记忆（Extract key points and add to memory）
                String keyPoints = extractKeyPoints(aiAnswer, chunk, batchId);
                memory.addMemory(batchId, keyPoints);

                log.info(LogMessageProvider.getMessage("doc_qa.log.batch_key_points", batchId, keyPoints.length()));

                // 保存批次结果（Save batch result）
                BatchResult batchResult = new BatchResult();
                batchResult.setBatchId(batchId);
                batchResult.setTotalBatches(chunks.size());
                batchResult.setQuestion(question);
                batchResult.setContentChunk(chunk);
                batchResult.setAnswer(answer);
                batchResult.setKeyPoints(keyPoints);
                batchResult.setTimestamp(System.currentTimeMillis());

                report.getBatchResults().add(batchResult);

                // 临时持久化（Persist temporarily）
                saveBatchResult(sessionId, batchResult);

                log.info(LogMessageProvider.getMessage("doc_qa.log.batch_complete", batchId, chunks.size()));
            }

            // 最后，使用所有关键记忆生成总结（Finally, generate summary using all key memories）
            generateFinalSummary(report, memory, question);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.batch_process_failed"), e);
            throw new RuntimeException("Batch processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建渐进式提示词（带记忆上下文）
     */
    private String buildProgressivePrompt(String question, String currentChunk,
                                         int batchId, int totalBatches,
                                         ProgressiveMemory memory) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 文档渐进式分析任务\n\n");
        prompt.append("你正在帮助用户分析一份文档，需要**像人类一样渐进式理解内容**。\n\n");

        prompt.append("## 用户问题\n");
        prompt.append(question).append("\n\n");

        prompt.append("## 当前进度\n");
        prompt.append("- 当前批次: ").append(batchId).append("/").append(totalBatches).append("\n");
        prompt.append("- 已完成: ").append(String.format("%.1f%%", (batchId - 1) * 100.0 / totalBatches)).append("\n\n");

        // 添加之前的关键记忆
        if (batchId > 1 && !memory.isEmpty()) {
            prompt.append("## 📝 之前批次的关键要点\n");
            prompt.append("*(这些是你分析前面内容时提取的重点，帮助你保持上下文连贯)*\n\n");

            List<String> recentMemories = memory.getRecentMemories();
            for (int i = 0; i < recentMemories.size(); i++) {
                prompt.append("**批次 ").append(batchId - recentMemories.size() + i)
                      .append("的关键点**:\n");
                prompt.append(recentMemories.get(i)).append("\n\n");
            }
        }

        prompt.append("## 📄 当前批次内容\n");
        prompt.append(currentChunk).append("\n\n");

        prompt.append("## 🎯 分析要求\n");
        prompt.append("1. **理解当前内容**: 仔细分析当前批次的内容\n");
        prompt.append("2. **关联前文**: 结合之前的关键要点，理解整体脉络\n");
        prompt.append("3. **提取重点**: 识别最重要的3-5个关键信息\n");
        prompt.append("4. **聚焦问题**: 重点关注与用户问题相关的内容\n");

        if (batchId < totalBatches) {
            prompt.append("5. **保持开放**: 这不是最后一部分，保留进一步分析的空间\n");
        } else {
            prompt.append("5. **总结全文**: 这是最后一部分，可以做出完整结论\n");
        }

        prompt.append("\n## 💡 请提供你的分析\n");
        prompt.append("请按以下格式输出：\n\n");
        prompt.append("### 本批次分析\n");
        prompt.append("[你对当前内容的分析和理解]\n\n");
        prompt.append("### 关键要点 (KEY_POINTS_START)\n");
        prompt.append("[提取3-5个最重要的关键信息，每个一行，用 - 开头]\n");
        prompt.append("(KEY_POINTS_END)\n");

        return prompt.toString();
    }

    /**
     * 从AI回答中提取关键点
     */
    private String extractKeyPoints(AIAnswer aiAnswer, String chunk, int batchId) {
        String answer = aiAnswer.getAnswer();

        // 尝试从答案中提取 KEY_POINTS 标记的内容
        int startIdx = answer.indexOf("KEY_POINTS_START");
        int endIdx = answer.indexOf("KEY_POINTS_END");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            String keyPointsSection = answer.substring(startIdx + 16, endIdx).trim();
            // 清理并格式化
            String cleaned = keyPointsSection.replaceAll("(?m)^\\s*#+\\s*.*$", "") // 移除标题
                                           .replaceAll("(?m)^\\s*\\(.*\\)\\s*$", "") // 移除括号注释
                                           .trim();

            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }

        // 如果没有找到标记，尝试智能提取（取答案的前500字符作为关键点）
        String keyPoints = answer.length() > 500 ? answer.substring(0, 500) + "..." : answer;

        // 简单格式化为要点形式
        return "批次 " + batchId + " 关键内容:\n" + keyPoints;
    }

    /**
     * 生成最终总结（基于所有关键记忆）（Generate final summary based on all key memories）
     */
    private void generateFinalSummary(DocumentQAReport report, ProgressiveMemory memory, String question) {
        try {
            log.info(LogMessageProvider.getMessage("doc_qa.log.generate_summary"));

            // 构建总结提示词（Build summary prompt）
            StringBuilder summaryPrompt = new StringBuilder();

            summaryPrompt.append("# 文档完整总结任务\n\n");
            summaryPrompt.append("你已经完成了对一份文档的逐批次分析。现在需要基于所有批次的关键要点，生成一个完整、连贯的总结。\n\n");

            summaryPrompt.append("## 用户问题\n");
            summaryPrompt.append(question).append("\n\n");

            summaryPrompt.append("## 所有批次的关键要点\n\n");

            List<BatchResult> batchResults = report.getBatchResults();
            for (BatchResult result : batchResults) {
                summaryPrompt.append("**批次 ").append(result.getBatchId())
                            .append("/").append(result.getTotalBatches()).append("**:\n");
                summaryPrompt.append(result.getKeyPoints()).append("\n\n");
            }

            summaryPrompt.append("## 总结要求\n");
            summaryPrompt.append("1. **综合所有要点**: 整合各批次的关键信息\n");
            summaryPrompt.append("2. **逻辑连贯**: 形成完整的分析思路\n");
            summaryPrompt.append("3. **突出重点**: 强调最重要的发现\n");
            summaryPrompt.append("4. **回应问题**: 直接回答用户的问题\n");
            summaryPrompt.append("5. **结构清晰**: 使用标题、列表等组织内容\n\n");

            summaryPrompt.append("请生成最终总结报告：\n");

            // 调用AI生成总结（Call AI to generate summary）
            AIAnswer summaryAnswer = knowledgeQAService.ask(summaryPrompt.toString());

            // 保存到报告（Save to report）
            report.setFinalReport(summaryAnswer.getAnswer());

            log.info(LogMessageProvider.getMessage("doc_qa.log.summary_complete", summaryAnswer.getAnswer().length()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.generate_summary_failed"), e);
            // 使用默认合并方式（Use default merge method）
            report.setFinalReport(generateDefaultSummary(report));
        }
    }

    /**
     * 生成默认总结（降级方案）
     */
    private String generateDefaultSummary(DocumentQAReport report) {
        StringBuilder summary = new StringBuilder();

        summary.append("# ").append(report.getDocumentName()).append(" - 分析报告\n\n");
        summary.append("**问题**: ").append(report.getQuestion()).append("\n\n");
        summary.append("---\n\n");

        summary.append("## 综合分析\n\n");
        summary.append("文档已分 ").append(report.getBatchResults().size())
              .append(" 个部分进行渐进式分析，以下是各部分的关键要点：\n\n");

        for (BatchResult batch : report.getBatchResults()) {
            summary.append("### 第 ").append(batch.getBatchId())
                  .append("/").append(batch.getTotalBatches())
                  .append(" 部分\n\n");

            if (batch.getKeyPoints() != null && !batch.getKeyPoints().isEmpty()) {
                summary.append("**关键要点**:\n");
                summary.append(batch.getKeyPoints()).append("\n\n");
            }

            summary.append("**详细分析**:\n");
            summary.append(batch.getAnswer()).append("\n\n");
        }

        return summary.toString();
    }

    /**
     * 渐进式记忆管理器
     * 模拟人类的记忆机制：保留重点，遗忘细节
     */
    private static class ProgressiveMemory {
        private final int maxMemorySize; // 最多保留多少批次的记忆
        private final LinkedHashMap<Integer, String> memories; // 批次ID -> 关键信息

        public ProgressiveMemory(int maxMemorySize) {
            this.maxMemorySize = maxMemorySize;
            this.memories = new LinkedHashMap<>();
        }

        /**
         * 添加新的记忆
         */
        public void addMemory(int batchId, String keyPoints) {
            memories.put(batchId, keyPoints);

            // 如果超过容量，移除最旧的记忆
            if (memories.size() > maxMemorySize) {
                Integer oldestKey = memories.keySet().iterator().next();
                memories.remove(oldestKey);
            }
        }

        /**
         * 获取最近的记忆
         */
        public List<String> getRecentMemories() {
            return new ArrayList<>(memories.values());
        }

        /**
         * 获取所有记忆
         */
        public Map<Integer, String> getAllMemories() {
            return new LinkedHashMap<>(memories);
        }

        public boolean isEmpty() {
            return memories.isEmpty();
        }
    }

    /**
     * 读取文档内容
     */
    private String readDocumentContent(File docFile) throws Exception {
        // 这里应该使用 TikaDocumentParser 解析文档
        // 为简化，假设直接读取文本
        return new String(Files.readAllBytes(docFile.toPath()));
    }

    /**
     * 分割内容为多个块
     */
    private List<String> splitContent(String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxChunkSize, content.length());

            // 尝试在段落边界分割
            if (end < content.length()) {
                int lastNewLine = content.lastIndexOf('\n', end);
                if (lastNewLine > start) {
                    end = lastNewLine;
                }
            }

            chunks.add(content.substring(start, end));
            start = end;
        }

        return chunks;
    }

    /**
     * 保存批次结果到临时文件（Save batch result to temp file）
     */
    private void saveBatchResult(String sessionId, BatchResult result) {
        try {
            String fileName = String.format("%s_batch_%d.json", sessionId, result.getBatchId());
            Path filePath = Paths.get(tempDir, fileName);

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(filePath.toFile(), result);

            log.debug(LogMessageProvider.getMessage("doc_qa.log.batch_result_saved", fileName));

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.save_batch_failed"), e);
        }
    }

    /**
     * 生成最终报告
     */
    private void generateFinalReport(DocumentQAReport report) {
        if (report.getBatchResults().isEmpty()) {
            report.setFinalReport("无结果");
            return;
        }

        StringBuilder finalReport = new StringBuilder();

        finalReport.append("# ").append(report.getDocumentName()).append(" - AI问答报告\n\n");
        finalReport.append("**问题**: ").append(report.getQuestion()).append("\n\n");
        finalReport.append("---\n\n");

        if (report.getBatchResults().size() == 1) {
            // 单批次，直接使用答案
            finalReport.append("## 回答\n\n");
            finalReport.append(report.getBatchResults().get(0).getAnswer());
        } else {
            // 多批次，需要合并
            finalReport.append("## 综合分析\n\n");
            finalReport.append("文档已分 ").append(report.getBatchResults().size())
                      .append(" 个部分进行分析，以下是各部分的分析结果：\n\n");

            for (BatchResult batch : report.getBatchResults()) {
                finalReport.append("### 第 ").append(batch.getBatchId())
                          .append("/").append(batch.getTotalBatches())
                          .append(" 部分\n\n");
                finalReport.append(batch.getAnswer()).append("\n\n");
            }

            // 生成总结（可以调用AI进行总结）
            finalReport.append("---\n\n");
            finalReport.append("## 总结\n\n");
            finalReport.append("以上是对文档 ").append(report.getDocumentName())
                      .append(" 的分").append(report.getBatchResults().size())
                      .append("次分析结果。");
        }

        report.setFinalReport(finalReport.toString());

        // 保存最终报告
        saveFinalReport(report);
    }

    /**
     * 保存最终报告（Save final report）
     */
    private void saveFinalReport(DocumentQAReport report) {
        try {
            String fileName = String.format("%s_final_report.json", report.getSessionId());
            Path filePath = Paths.get(tempDir, fileName);

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(filePath.toFile(), report);

            log.info(LogMessageProvider.getMessage("doc_qa.log.report_saved", fileName));

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.save_report_failed"), e);
        }
    }

    /**
     * 清理临时文件（Cleanup temp files）
     */
    public void cleanupSession(String sessionId) {
        try {
            Path tempDirPath = Paths.get(tempDir);
            if (Files.exists(tempDirPath)) {
                Files.list(tempDirPath)
                     .filter(path -> path.getFileName().toString().startsWith(sessionId))
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                             log.debug(LogMessageProvider.getMessage("doc_qa.log.temp_file_deleted", path.getFileName()));
                         } catch (IOException e) {
                             log.warn(LogMessageProvider.getMessage("doc_qa.log.delete_temp_failed", path.getFileName()));
                         }
                     });
            }
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("doc_qa.log.cleanup_failed"), e);
        }
    }

    /**
     * 文档问答报告
     */
    @Data
    public static class DocumentQAReport {
        private String sessionId;
        private String documentName;
        private String question;
        private long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;
        private List<BatchResult> batchResults = new ArrayList<>();
        private String finalReport;
    }

    /**
     * 批次结果
     */
    @Data
    public static class BatchResult {
        private int batchId;
        private int totalBatches;
        private String question;
        private String contentChunk;
        private String answer;
        private String keyPoints;  // 本批次提取的关键点
        private long timestamp;
    }
}

