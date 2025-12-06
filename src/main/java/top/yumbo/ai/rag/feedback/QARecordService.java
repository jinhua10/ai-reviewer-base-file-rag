package top.yumbo.ai.rag.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.config.FeedbackConfig;
import top.yumbo.ai.rag.spring.boot.service.QAArchiveService;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 问答记录存储服务（QA Record Storage Service）
 * 负责问答记录的存储、查询和管理（Responsible for storing, querying, and managing QA records）
 *
 * @author AI Reviewer Team
 * @since 2025-11-27
 */
@Slf4j
@Service
public class QARecordService {

    private static final String RECORDS_DIR = "./data/qa-records";
    private static final DateTimeFormatter FILE_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper;
    private final Path recordsPath;
    private final FeedbackConfig feedbackConfig;
    private final DocumentWeightService documentWeightService;
    private QAArchiveService qaArchiveService; // 延迟注入，避免循环依赖（Lazy injection to avoid circular dependency）

    @Autowired
    public QARecordService(FeedbackConfig feedbackConfig,
                          DocumentWeightService documentWeightService) {
        this.feedbackConfig = feedbackConfig;
        this.documentWeightService = documentWeightService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.recordsPath = Paths.get(RECORDS_DIR);

        // 初始化存储目录（Initialize storage directory）
        initStorageDirectory();
    }

    /**
     * 设置问答归档服务（延迟注入）（Set QA archive service (lazy injection)）
     */
    @Autowired(required = false)
    public void setQaArchiveService(QAArchiveService qaArchiveService) {
        this.qaArchiveService = qaArchiveService;
    }

    /**
     * 保存问答记录（Save QA record）
     */
    public String saveRecord(QARecord record) {
        try {
            // 生成唯一ID
            if (record.getId() == null) {
                record.setId(UUID.randomUUID().toString());
            }

            // 设置时间戳
            if (record.getTimestamp() == null) {
                record.setTimestamp(LocalDateTime.now());
            }

            // 设置初始审核状态
            if (record.getReviewStatus() == null) {
                record.setReviewStatus(QARecord.ReviewStatus.PENDING);
            }

            // 按日期组织文件
            String dateStr = record.getTimestamp().format(FILE_DATE_FORMATTER);
            Path dateDir = recordsPath.resolve(dateStr);
            Files.createDirectories(dateDir);

            // 保存为 JSON 文件
            String fileName = String.format("%s_%s.json",
                record.getTimestamp().format(DateTimeFormatter.ofPattern("HHmmss")),
                record.getId().substring(0, 8));
            Path recordFile = dateDir.resolve(fileName);

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(recordFile.toFile(), record);

            log.info(LogMessageProvider.getMessage("log.qa.record_saved", record.getId(), recordFile));
            return record.getId();

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.record_save_failed"), e);
            return null;
        }
    }

    /**
     * 根据ID获取记录（Get record by ID）
     */
    public Optional<QARecord> getRecord(String id) {
        try {
            // 遍历所有日期目录查找（Traverse all date directories to find）
            return Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> p.getFileName().toString().contains(id.substring(0, 8)))
                .findFirst()
                .map(this::loadRecord);
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.find_failed", id), e);
            return Optional.empty();
        }
    }

    /**
     * 更新记录（Update record）
     */
    public boolean updateRecord(QARecord record) {
        try {
            // 查找现有文件（Find existing file）
            Optional<Path> existingFile = Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> p.getFileName().toString().contains(record.getId().substring(0, 8)))
                .findFirst();

            if (existingFile.isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(existingFile.get().toFile(), record);
                log.info(LogMessageProvider.getMessage("log.qa.record_updated", record.getId()));
                return true;
            } else {
                log.warn(LogMessageProvider.getMessage("log.qa.record_notfound", record.getId()));
                return false;
            }
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.record_update_failed", record.getId()), e);
            return false;
        }
    }

    /**
     * 添加整体反馈（Add overall feedback）
     */
    public boolean addOverallFeedback(String recordId, int rating, String feedback) {
        Optional<QARecord> recordOpt = getRecord(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        QARecord record = recordOpt.get();
        record.setOverallRating(rating);
        record.setOverallFeedback(feedback);

        log.info(LogMessageProvider.getMessage("log.qa.user_feedback", recordId.substring(0, 8), rating,
            feedback != null && !feedback.isEmpty() ? feedback : "无"));

        boolean updated = updateRecord(record);

        // ✨ 新增：高评分自动归档（New feature: High rating auto-archive）
        if (updated && rating >= 4 && qaArchiveService != null) {
            try {
                if (qaArchiveService.shouldArchive(record)) {
                    String archivePath = qaArchiveService.archiveQA(record);
                    if (archivePath != null) {
                        log.info(LogMessageProvider.getMessage("log.qa.archived", rating, archivePath));
                    }
                }
            } catch (Exception e) {
                log.error(LogMessageProvider.getMessage("log.qa.archive_failed"), e);
            }
        }

        return updated;
    }

    /**
     * 添加文档反馈（Add document feedback）
     */
    public boolean addDocumentFeedback(String recordId, String documentName,
                                      QARecord.FeedbackType feedbackType, String reason) {
        Optional<QARecord> recordOpt = getRecord(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        QARecord record = recordOpt.get();
        if (record.getDocumentFeedbacks() == null) {
            record.setDocumentFeedbacks(new ArrayList<>());
        }

        // 检查是否已经反馈过（Check if feedback already exists）
        Optional<QARecord.DocumentFeedback> existing = record.getDocumentFeedbacks().stream()
            .filter(f -> f.getDocumentName().equals(documentName))
            .findFirst();

        if (existing.isPresent()) {
            // 更新现有反馈（Update existing feedback）
            existing.get().setFeedbackType(feedbackType);
            existing.get().setReason(reason);
            existing.get().setFeedbackTime(LocalDateTime.now());
        } else {
            // 添加新反馈（Add new feedback）
            record.getDocumentFeedbacks().add(
                QARecord.DocumentFeedback.builder()
                    .documentName(documentName)
                    .feedbackType(feedbackType)
                    .reason(reason)
                    .feedbackTime(LocalDateTime.now())
                    .build()
            );
        }

        String emoji = feedbackType == QARecord.FeedbackType.LIKE ? "👍" : "👎";
        log.info(LogMessageProvider.getMessage("log.qa.document_feedback", emoji, recordId.substring(0, 8), documentName, feedbackType));

        // 根据配置决定是否自动应用反馈（Decide whether to apply feedback automatically based on configuration）
        if (!feedbackConfig.isRequireApproval() && feedbackConfig.isAutoApply()) {
            // 直接应用反馈到文档权重（Apply feedback directly to document weight）
            documentWeightService.applyFeedback(documentName, feedbackType);
            record.setAppliedToOptimization(true);
            log.info(LogMessageProvider.getMessage("log.qa.feedback_applied", documentName));
        } else {
            // 设置为待审核（Set status to pending review）
            record.setReviewStatus(QARecord.ReviewStatus.PENDING);
            record.setAppliedToOptimization(false);
            log.info(LogMessageProvider.getMessage("log.qa.feedback_pending", documentName));
        }

        return updateRecord(record);
    }

    /**
     * 添加文档星级评价（用户友好接口）（Add document rating (user-friendly interface)）
     *
     * 星级到权重调整的映射：（Star rating to weight adjustment mapping:）
     * 5星 (非常有用) → +0.5 权重（5 stars (very useful) → +0.5 weight）
     * 4星 (很有帮助) → +0.2 权重（4 stars (very helpful) → +0.2 weight）
     * 3星 (一般) → 0 权重（不变）（3 stars (average) → 0 weight (unchanged)）
     * 2星 (帮助不大) → -0.2 权重（2 stars (not very helpful) → -0.2 weight）
     * 1星 (没有帮助) → -0.5 权重（1 star (not helpful) → -0.5 weight）
     */
    public boolean addDocumentRating(String recordId, String documentName, int rating, String comment) {
        Optional<QARecord> recordOpt = getRecord(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        QARecord record = recordOpt.get();
        if (record.getDocumentFeedbacks() == null) {
            record.setDocumentFeedbacks(new ArrayList<>());
        }

        // 将星级转换为反馈类型和权重调整（Convert star rating to feedback type and weight adjustment）
        QARecord.FeedbackType feedbackType;
        double weightAdjustment;

        switch (rating) {
            case 5:
                feedbackType = QARecord.FeedbackType.LIKE;
                weightAdjustment = 0.5;  // 大幅提升（Significantly increase）
                break;
            case 4:
                feedbackType = QARecord.FeedbackType.LIKE;
                weightAdjustment = 0.2;  // 提升（Increase）
                break;
            case 3:
                feedbackType = QARecord.FeedbackType.NEUTRAL;  // 需要在 QARecord 中添加（Need to add in QARecord）
                weightAdjustment = 0.0;  // 保持不变（Keep unchanged）
                break;
            case 2:
                feedbackType = QARecord.FeedbackType.DISLIKE;
                weightAdjustment = -0.2;  // 降低（Decrease）
                break;
            case 1:
                feedbackType = QARecord.FeedbackType.DISLIKE;
                weightAdjustment = -0.5;  // 大幅降低（Significantly decrease）
                break;
            default:
                return false;
        }

        // 检查是否已经反馈过（Check if feedback already exists）
        Optional<QARecord.DocumentFeedback> existing = record.getDocumentFeedbacks().stream()
            .filter(f -> f.getDocumentName().equals(documentName))
            .findFirst();

        if (existing.isPresent()) {
            // 更新现有反馈（Update existing feedback）
            existing.get().setFeedbackType(feedbackType);
            existing.get().setReason(comment);
            existing.get().setFeedbackTime(LocalDateTime.now());
        } else {
            // 添加新反馈（Add new feedback）
            record.getDocumentFeedbacks().add(
                QARecord.DocumentFeedback.builder()
                    .documentName(documentName)
                    .feedbackType(feedbackType)
                    .reason(comment)
                    .feedbackTime(LocalDateTime.now())
                    .build()
            );
        }

        String stars = "⭐".repeat(rating);
        log.info(LogMessageProvider.getMessage("log.qa.rating_submitted", stars, recordId.substring(0, 8), documentName, rating, String.format("%+.1f", weightAdjustment)));

        // 根据配置决定是否自动应用反馈（Decide whether to apply feedback automatically based on configuration）
        if (!feedbackConfig.isRequireApproval() && feedbackConfig.isAutoApply()) {
            // 直接应用权重调整（Apply weight adjustment directly）
            documentWeightService.applyRatingFeedback(documentName, rating, weightAdjustment);
            record.setAppliedToOptimization(true);
            log.info(LogMessageProvider.getMessage("log.qa.rating_applied", documentName, rating, String.format("%+.1f", weightAdjustment)));
        } else {
            // 设置为待审核（Set status to pending review）
            record.setReviewStatus(QARecord.ReviewStatus.PENDING);
            record.setAppliedToOptimization(false);
            log.info(LogMessageProvider.getMessage("log.qa.rating_pending", documentName, rating));
        }

        return updateRecord(record);
    }

    /**
     * 添加整体评价（表情评分）/ Add overall rating (emoji rating)
     *
     * @param recordId 记录ID / Record ID
     * @param rating 评分 (1-5) / Rating (1-5)
     * @return 是否成功 / Whether successful
     */
    public boolean addOverallRating(String recordId, int rating) {
        Optional<QARecord> recordOpt = getRecord(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        QARecord record = recordOpt.get();

        // 更新整体评分 / Update overall rating
        record.setOverallRating(rating);
        record.setOverallFeedback(getEmojiDescription(rating));

        // 记录日志 / Log the rating
        String emojiText = getEmojiDescription(rating);
        log.info(LogMessageProvider.getMessage("log.qa.overall_rating_submitted",
            emojiText, recordId.substring(0, Math.min(8, recordId.length())), rating));

        // 如果评分较高（4-5星），可以将此问答记录标记为优质内容
        // If rating is high (4-5 stars), mark this QA as high-quality content
        if (rating >= 4) {
            record.setReviewStatus(QARecord.ReviewStatus.APPROVED);
            log.info(LogMessageProvider.getMessage("log.qa.marked_as_quality", recordId.substring(0, Math.min(8, recordId.length()))));
        }

        return updateRecord(record);
    }

    /**
     * 获取表情描述 / Get emoji description
     */
    private String getEmojiDescription(int rating) {
        switch (rating) {
            case 5:
                return "🤩 非常有用";
            case 4:
                return "😊 很有用";
            case 3:
                return "😐 一般般";
            case 2:
                return "🙁 帮助不大";
            case 1:
                return "😞 完全没用";
            default:
                return "";
        }
    }

    /**
     * 获取最近的记录（Get recent records）
     */
    public List<QARecord> getRecentRecords(int limit) {
        try {
            return Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::getFileName).reversed())
                .limit(limit)
                .map(this::loadRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.recent_failed"), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取待审核的记录（Get pending review records）
     */
    public List<QARecord> getPendingRecords() {
        try {
            return Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .map(this::loadRecord)
                .filter(Objects::nonNull)
                .filter(r -> r.getReviewStatus() == QARecord.ReviewStatus.PENDING)
                .filter(r -> r.getOverallRating() != null ||
                           (r.getDocumentFeedbacks() != null && !r.getDocumentFeedbacks().isEmpty()))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.pending_failed"), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取统计信息（Get statistics）
     */
    public QAStatistics getStatistics() {
        try {
            List<QARecord> allRecords = Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .map(this::loadRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            long totalCount = allRecords.size();
            long withFeedback = allRecords.stream()
                .filter(r -> r.getOverallRating() != null)
                .count();
            double avgRating = allRecords.stream()
                .filter(r -> r.getOverallRating() != null)
                .mapToInt(QARecord::getOverallRating)
                .average()
                .orElse(0.0);
            long pendingReview = allRecords.stream()
                .filter(r -> r.getReviewStatus() == QARecord.ReviewStatus.PENDING)
                .count();

            return QAStatistics.builder()
                .totalRecords(totalCount)
                .recordsWithFeedback(withFeedback)
                .averageRating(avgRating)
                .pendingReview(pendingReview)
                .build();

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.stats_failed"), e);
            return new QAStatistics();
        }
    }

    /**
     * 加载记录（Load record）
     */
    private QARecord loadRecord(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), QARecord.class);
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.load_failed", path.toString()), e);
            return null;
        }
    }

    /**
     * 初始化存储目录（Initialize storage directory）
     */
    private void initStorageDirectory() {
        try {
            Files.createDirectories(recordsPath);
            log.info(LogMessageProvider.getMessage("log.qa.records_dir", recordsPath.toAbsolutePath()));
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.qa.records_dir_failed", recordsPath), e);
        }
    }

    /**
     * 获取高赞提示词推荐（Get highly rated prompt recommendations）
     * 根据策略类型获取评分高的历史提示词
     *
     * @param strategyType 策略类型（可选，如"快速总结"、"深度分析"等）
     * @param limit 返回数量限制
     * @return 高赞提示词列表
     */
    public List<PromptRecommendation> getTopRatedPrompts(String strategyType, int limit) {
        try {
            List<PromptRecommendation> recommendations = new ArrayList<>();
            
            // 遍历所有记录
            Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        QARecord record = loadRecord(path);
                        if (record != null && record.getOverallRating() != null && record.getOverallRating() >= 4) {
                            // 高评分记录
                            PromptRecommendation rec = new PromptRecommendation();
                            rec.setPrompt(record.getQuestion());
                            rec.setRating(record.getOverallRating());
                            rec.setUsageCount(1); // 简化版，后续可以统计实际使用次数
                            rec.setStrategy(detectStrategy(record.getQuestion()));
                            rec.setTimestamp(record.getTimestamp());
                            
                            // 如果指定了策略类型，进行筛选
                            if (strategyType == null || strategyType.isEmpty() || 
                                rec.getStrategy().contains(strategyType) || strategyType.equals("all")) {
                                recommendations.add(rec);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to process record: {}", path, e);
                    }
                });

            // 按评分和使用次数排序
            return recommendations.stream()
                .sorted((a, b) -> {
                    int ratingCompare = Integer.compare(b.getRating(), a.getRating());
                    if (ratingCompare != 0) return ratingCompare;
                    return Integer.compare(b.getUsageCount(), a.getUsageCount());
                })
                .limit(limit)
                .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Failed to get top rated prompts", e);
            return new ArrayList<>();
        }
    }

    /**
     * 检测提示词的策略类型（简单的关键词匹配）
     */
    private String detectStrategy(String question) {
        if (question == null) return "通用";
        
        String lower = question.toLowerCase();
        if (lower.contains("总结") || lower.contains("概括") || lower.contains("summarize")) {
            return "快速总结";
        } else if (lower.contains("分析") || lower.contains("analyze") || lower.contains("详细")) {
            return "深度分析";
        } else if (lower.contains("对比") || lower.contains("比较") || lower.contains("compare")) {
            return "对比分析";
        } else if (lower.contains("提取") || lower.contains("extract") || lower.contains("列出")) {
            return "信息提取";
        } else if (lower.contains("什么") || lower.contains("哪个") || lower.contains("what") || lower.contains("which")) {
            return "精确查找";
        } else {
            return "通用";
        }
    }

    /**
     * 提示词推荐数据结构
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PromptRecommendation {
        private String prompt;
        private Integer rating;
        private Integer usageCount;
        private String strategy;
        private LocalDateTime timestamp;
    }

    /**
     * 统计信息（Statistics）
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QAStatistics {
        private long totalRecords;
        private long recordsWithFeedback;
        private double averageRating;
        private long pendingReview;
    }
}
