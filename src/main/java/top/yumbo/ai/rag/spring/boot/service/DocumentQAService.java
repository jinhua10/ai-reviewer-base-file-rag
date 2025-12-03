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
     * 初始化临时目录
     */
    private void initTempDir(String storagePath) {
        this.tempDir = storagePath + File.separator + ".doc_qa_temp";
        Path tempDirPath = Paths.get(tempDir);

        try {
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
                log.info("创建文档问答临时目录: {}", tempDir);
            }
        } catch (IOException e) {
            log.error("创建临时目录失败", e);
        }
    }

    /**
     * 对完整文档进行AI问答
     *
     * @param documentPath 文档路径
     * @param question 问题
     * @param storagePath 知识库存储路径
     * @return 问答报告
     */
    public DocumentQAReport queryDocument(String documentPath, String question, String storagePath) {
        initTempDir(storagePath);

        File docFile = new File(documentPath);
        if (!docFile.exists()) {
            throw new IllegalArgumentException("文档不存在: " + documentPath);
        }

        String sessionId = UUID.randomUUID().toString();
        log.info("📄 开始文档问答: {} (会话ID: {})", docFile.getName(), sessionId);
        log.info("❓ 问题: {}", question);

        DocumentQAReport report = new DocumentQAReport();
        report.setSessionId(sessionId);
        report.setDocumentName(docFile.getName());
        report.setQuestion(question);
        report.setStartTime(System.currentTimeMillis());

        try {
            // 1. 检查文档大小并决定是否分批
            long fileSize = docFile.length();
            int maxChunkSize = properties.getDocument().getMaxIndexContentLength();

            boolean needsChunking = shouldChunkDocument(docFile, maxChunkSize);

            if (needsChunking) {
                log.info("📦 文档较大，启用分批处理模式");
                processInChunks(docFile, question, sessionId, report);
            } else {
                log.info("📝 文档较小，直接处理");
                processDirectly(docFile, question, sessionId, report);
            }

            // 2. 生成最终报告
            generateFinalReport(report);

            report.setEndTime(System.currentTimeMillis());
            report.setSuccess(true);

            log.info("✅ 文档问答完成: {} (耗时: {}ms)",
                docFile.getName(), report.getEndTime() - report.getStartTime());

        } catch (Exception e) {
            log.error("❌ 文档问答失败", e);
            report.setSuccess(false);
            report.setErrorMessage(e.getMessage());
            report.setEndTime(System.currentTimeMillis());
        }

        return report;
    }

    /**
     * 判断是否需要分块处理
     */
    private boolean shouldChunkDocument(File docFile, int maxContentLength) {
        // 简单估算：假设1KB文件约产生1-2字符的内容
        long estimatedContentLength = docFile.length() * 2;
        return estimatedContentLength > maxContentLength;
    }

    /**
     * 直接处理整个文档
     */
    private void processDirectly(File docFile, String question, String sessionId, DocumentQAReport report) {
        try {
            // 使用知识库服务进行问答
            AIAnswer aiAnswer = knowledgeQAService.ask(question);
            String answer = aiAnswer.getAnswer();

            BatchResult result = new BatchResult();
            result.setBatchId(1);
            result.setTotalBatches(1);
            result.setQuestion(question);
            result.setAnswer(answer);
            result.setTimestamp(System.currentTimeMillis());

            report.getBatchResults().add(result);

            // 保存临时结果
            saveBatchResult(sessionId, result);

        } catch (Exception e) {
            log.error("直接处理文档失败", e);
            throw new RuntimeException("处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分批处理文档
     */
    private void processInChunks(File docFile, String question, String sessionId, DocumentQAReport report) {
        try {
            // 读取文档内容
            String content = readDocumentContent(docFile);

            // 分割成多个批次
            int maxChunkSize = properties.getDocument().getMaxIndexContentLength() / 2; // 保守估计
            List<String> chunks = splitContent(content, maxChunkSize);

            log.info("📦 文档已分割为 {} 个批次", chunks.size());

            // 逐批处理
            for (int i = 0; i < chunks.size(); i++) {
                int batchId = i + 1;
                String chunk = chunks.get(i);

                log.info("🔄 处理批次 {}/{} (大小: {} 字符)", batchId, chunks.size(), chunk.length());

                // 构建批次特定的问题
                String batchQuestion = String.format(
                    "%s\n\n【处理范围】这是文档的第 %d/%d 部分。\n\n【文档片段】\n%s",
                    question, batchId, chunks.size(), chunk
                );

                // 调用AI问答
                AIAnswer aiAnswer = knowledgeQAService.ask(batchQuestion);
                String answer = aiAnswer.getAnswer();

                // 保存批次结果
                BatchResult batchResult = new BatchResult();
                batchResult.setBatchId(batchId);
                batchResult.setTotalBatches(chunks.size());
                batchResult.setQuestion(question);
                batchResult.setContentChunk(chunk);
                batchResult.setAnswer(answer);
                batchResult.setTimestamp(System.currentTimeMillis());

                report.getBatchResults().add(batchResult);

                // 临时持久化
                saveBatchResult(sessionId, batchResult);

                log.info("✅ 批次 {}/{} 处理完成", batchId, chunks.size());
            }

        } catch (Exception e) {
            log.error("分批处理文档失败", e);
            throw new RuntimeException("分批处理失败: " + e.getMessage(), e);
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
     * 保存批次结果到临时文件
     */
    private void saveBatchResult(String sessionId, BatchResult result) {
        try {
            String fileName = String.format("%s_batch_%d.json", sessionId, result.getBatchId());
            Path filePath = Paths.get(tempDir, fileName);

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(filePath.toFile(), result);

            log.debug("💾 批次结果已保存: {}", fileName);

        } catch (IOException e) {
            log.error("保存批次结果失败", e);
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
     * 保存最终报告
     */
    private void saveFinalReport(DocumentQAReport report) {
        try {
            String fileName = String.format("%s_final_report.json", report.getSessionId());
            Path filePath = Paths.get(tempDir, fileName);

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(filePath.toFile(), report);

            log.info("📊 最终报告已保存: {}", fileName);

        } catch (IOException e) {
            log.error("保存最终报告失败", e);
        }
    }

    /**
     * 清理临时文件
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
                             log.debug("🗑️ 已删除临时文件: {}", path.getFileName());
                         } catch (IOException e) {
                             log.warn("删除临时文件失败: {}", path.getFileName());
                         }
                     });
            }
        } catch (IOException e) {
            log.error("清理会话失败", e);
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
        private long timestamp;
    }
}

