package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.QARecord;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.config.QAArchiveProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答归档服务（QA archive service）
 * 将高质量问答转化为新的知识文档并索引（Convert high-quality Q&A into new knowledge documents and index them）
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Slf4j
@Service
public class QAArchiveService {

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final QAArchiveProperties archiveProperties;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;
    private final Path archivePath;

    @Autowired
    public QAArchiveService(QAArchiveProperties archiveProperties,
                            KnowledgeBaseService knowledgeBaseService) {
        this.archiveProperties = archiveProperties;
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.archivePath = Paths.get(archiveProperties.getArchivePath());

        // 初始化目录结构
        initDirectories();
    }

    /**
     * 初始化目录结构（Initialize directory structure）
     */
    private void initDirectories() {
        try {
            Files.createDirectories(archivePath.resolve("approved/concept"));
            Files.createDirectories(archivePath.resolve("approved/howto"));
            Files.createDirectories(archivePath.resolve("approved/troubleshooting"));
            Files.createDirectories(archivePath.resolve("approved/other"));
            Files.createDirectories(archivePath.resolve("temp"));
            Files.createDirectories(archivePath.resolve("rejected"));

            log.info(LogMessageProvider.getMessage("log.qa.archive.init", archivePath.toAbsolutePath()));
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.archive.init_failed"), e);
        }
    }

    /**
     * 判断是否应该归档（Determine if should archive）
     */
    public boolean shouldArchive(QARecord record) {
        if (!archiveProperties.isEnabled()) {
            return false;
        }

        String strategy = archiveProperties.getStrategy();

        switch (strategy) {
            case "auto":
                return shouldArchiveAuto(record);
            case "feedback-based":
                return shouldArchiveFeedbackBased(record);
            case "manual":
                return false; // 手动模式不自动归档（Manual mode does not auto-archive）
            default:
                log.warn(LogMessageProvider.getMessage("log.qa.archive.unknown_strategy", strategy));
                return false;
        }
    }

    /**
     * 自动归档策略（Auto archive strategy）
     */
    private boolean shouldArchiveAuto(QARecord record) {
        // 检查问题长度（Check question length）
        if (record.getQuestion() == null ||
                record.getQuestion().length() < archiveProperties.getMinQuestionLength()) {
            return false;
        }

        // 检查回答长度（Check answer length）
        if (record.getAnswer() == null ||
                record.getAnswer().length() < archiveProperties.getMinAnswerLength()) {
            return false;
        }

        // 检查是否包含 "无法回答" 等关键词（Check if contains keywords like "unable to answer"）
        String answer = record.getAnswer().toLowerCase();
        if (answer.contains("无法回答") ||
                answer.contains("没有相关信息") ||
                answer.contains("抱歉") ||
                answer.contains("无法找到")) {
            return false;
        }

        return true;
    }

    /**
     * 基于反馈的归档策略（Feedback-based archive strategy）
     */
    private boolean shouldArchiveFeedbackBased(QARecord record) {
        // 必须有评分（Must have rating）
        if (record.getOverallRating() == null) {
            return false;
        }

        // 评分必须 >= 阈值（Rating must >= threshold）
        if (record.getOverallRating() < archiveProperties.getMinRating()) {
            return false;
        }

        // 同时满足自动归档的基本条件（Also meet basic auto-archive conditions）
        return shouldArchiveAuto(record);
    }

    /**
     * 归档问答为新文档（Archive Q&A as new document）
     */
    public String archiveQA(QARecord record) {
        try {
            // 1. 构建文档内容（Build document content）
            String content = buildDocumentContent(record);

            // 2. 确定分类和路径（Determine category and path）
            String category = detectCategory(record.getQuestion(), record.getAnswer());
            String status = determineStatus(record);

            // 3. 生成文件名（使用系统默认时区）（Generate filename (using system default timezone)）
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String questionPrefix = sanitizeFileName(
                    record.getQuestion().substring(0, Math.min(30, record.getQuestion().length()))
            );
            String fileName = String.format("%s-QA-%s.md", timestamp, questionPrefix);

            // 4. 确定保存路径（Determine save path）
            Path targetPath;
            if ("approved".equals(status)) {
                targetPath = archivePath.resolve("approved").resolve(category).resolve(fileName);
            } else {
                targetPath = archivePath.resolve("temp").resolve(fileName);
            }

            // 5. 保存文件（Save file）
            Files.writeString(targetPath, content);
            log.info(LogMessageProvider.getMessage("log.qa.archive.saved", targetPath.getFileName()));

            // 6. 自动索引（如果配置了）（Auto index if configured）
            if (archiveProperties.isAutoIndex()) {
                try {
                    knowledgeBaseService.incrementalIndexFile(targetPath);
                    log.info(LogMessageProvider.getMessage("log.qa.archive.indexed", fileName));
                } catch (Exception e) {
                    log.warn(LogMessageProvider.getMessage("log.qa.archive.index_failed", fileName, e.getMessage()));
                }
            }

            return targetPath.toString();

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.qa.archive.failed", record.getId()), e);
            return null;
        }
    }

    /**
     * 构建文档内容（Markdown 格式）（Build document content (Markdown format)）
     */
    private String buildDocumentContent(QARecord record) {
        StringBuilder content = new StringBuilder();

        // YAML Front Matter
        content.append("---\n");
        content.append("id: \"").append(record.getId()).append("\"\n");
        content.append("question: \"").append(escapeYaml(record.getQuestion())).append("\"\n");
        content.append("timestamp: \"").append(record.getTimestamp()).append("\"\n");
        if (record.getOverallRating() != null) {
            content.append("rating: ").append(record.getOverallRating()).append("\n");
        }
        content.append("status: \"").append(determineStatus(record)).append("\"\n");
        content.append("category: \"").append(detectCategory(record.getQuestion(), record.getAnswer())).append("\"\n");

        // 关键词
        List<String> keywords = extractKeywords(record.getQuestion(), record.getAnswer());
        content.append("keywords: [");
        content.append(keywords.stream()
                .map(k -> "\"" + k + "\"")
                .collect(Collectors.joining(", ")));
        content.append("]\n");

        // 来源文档
        if (record.getUsedDocuments() != null && !record.getUsedDocuments().isEmpty()) {
            content.append("sourceDocuments: [");
            content.append(record.getUsedDocuments().stream()
                    .map(d -> "\"" + d + "\"")
                    .collect(Collectors.joining(", ")));
            content.append("]\n");
        }

        content.append("usageCount: 0\n");
        content.append("---\n\n");

        // 主标题
        content.append("# ").append(record.getQuestion()).append("\n\n");

        // 元数据摘要
        content.append("> **类型**: ").append(getCategoryDisplayName(detectCategory(record.getQuestion(), record.getAnswer()))).append("  \n");
        if (record.getUsedDocuments() != null) {
            content.append("> **来源**: 基于 ").append(record.getUsedDocuments().size()).append(" 个文档生成  \n");
        }
        if (record.getOverallRating() != null) {
            content.append("> **评分**: ").append("⭐".repeat(record.getOverallRating())).append(" (").append(record.getOverallRating()).append(".0)  \n");
        }
        content.append("\n");

        // 回答内容
        content.append("## 回答\n\n");
        content.append(record.getAnswer()).append("\n\n");

        // 来源文档详情
        if (record.getUsedDocuments() != null && !record.getUsedDocuments().isEmpty()) {
            content.append("## 来源文档\n\n");
            content.append("本回答综合了以下文档的内容：\n\n");
            for (int i = 0; i < record.getUsedDocuments().size(); i++) {
                content.append(i + 1).append(". **").append(record.getUsedDocuments().get(i)).append("**\n");
            }
            content.append("\n");
        }

        // 用户反馈
        if (record.getDocumentFeedbacks() != null && !record.getDocumentFeedbacks().isEmpty()) {
            content.append("## 用户反馈\n\n");
            long likeCount = record.getDocumentFeedbacks().stream()
                    .filter(f -> f.getFeedbackType() == QARecord.FeedbackType.LIKE)
                    .count();
            content.append("- 👍 **").append(likeCount).append(" 个文档被标记为有帮助**\n");

            if (record.getOverallFeedback() != null) {
                content.append("- 💬 用户评价：\"").append(record.getOverallFeedback()).append("\"\n");
            }
            content.append("\n");
        }

        // 页脚
        content.append("---\n\n");
        content.append("**生成时间**: ").append(record.getTimestamp()).append("  \n");
        content.append("**文档版本**: 1.0\n");

        return content.toString();
    }

    /**
     * 检测问题类别（Detect question category）
     */
    private String detectCategory(String question, String answer) {
        question = question.toLowerCase();

        if (question.contains("什么是") || question.contains("是什么") ||
                question.contains("定义") || question.contains("解释")) {
            return "concept";
        }

        if (question.contains("如何") || question.contains("怎么") ||
                question.contains("怎样") || question.contains("怎么样")) {
            return "howto";
        }

        if (question.contains("失败") || question.contains("错误") ||
                question.contains("问题") || question.contains("为什么不")) {
            return "troubleshooting";
        }

        return "other";
    }

    /**
     * 获取分类显示名称（Get category display name）
     */
    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "concept":
                return "概念解释";
            case "howto":
                return "操作指南";
            case "troubleshooting":
                return "问题排查";
            default:
                return "其他";
        }
    }

    /**
     * 确定文档状态（Determine document status）
     */
    private String determineStatus(QARecord record) {
        if ("feedback-based".equals(archiveProperties.getStrategy())) {
            return record.getOverallRating() != null &&
                    record.getOverallRating() >= archiveProperties.getMinRating()
                    ? "approved" : "temp";
        }
        return "temp"; // 自动归档默认为临时状态（Auto archive defaults to temp status）
    }

    /**
     * 提取关键词（Extract keywords）
     */
    private List<String> extractKeywords(String question, String answer) {
        // 简单实现：提取问题中的名词（Simple implementation: extract nouns from question）
        List<String> stopWords = List.of(
                "的", "是", "在", "了", "和", "有", "我", "你", "他", "她",
                "什么", "怎么", "如何", "为什么", "吗", "呢", "啊"
        );

        return java.util.Arrays.stream(question.split("[\\s，。！？、；：]"))
                .filter(word -> word.length() > 1)
                .filter(word -> !stopWords.contains(word))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 清理文件名（移除非法字符）（Clean filename (remove illegal characters)）
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 转义 YAML 特殊字符（Escape YAML special characters）
     */
    private String escapeYaml(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * 获取归档统计信息（Get archive statistics）
     */
    public ArchiveStatistics getStatistics() {
        try {
            long approvedCount = countFilesInDirectory(archivePath.resolve("approved"));
            long tempCount = countFilesInDirectory(archivePath.resolve("temp"));
            long rejectedCount = countFilesInDirectory(archivePath.resolve("rejected"));

            return ArchiveStatistics.builder()
                    .totalArchived(approvedCount + tempCount)
                    .approvedCount(approvedCount)
                    .tempCount(tempCount)
                    .rejectedCount(rejectedCount)
                    .build();
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.qa.archive.stats_failed"), e);
            return ArchiveStatistics.builder().build();
        }
    }

    private long countFilesInDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".md"))
                .count();
    }

    /**
     * 归档统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ArchiveStatistics {
        private long totalArchived;
        private long approvedCount;
        private long tempCount;
        private long rejectedCount;
    }
}
