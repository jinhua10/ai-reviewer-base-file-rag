package top.yumbo.ai.rag.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.config.FeedbackConfig;

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
 * 问答记录存储服务
 * 负责问答记录的存储、查询和管理
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

    @Autowired
    public QARecordService(FeedbackConfig feedbackConfig,
                          DocumentWeightService documentWeightService) {
        this.feedbackConfig = feedbackConfig;
        this.documentWeightService = documentWeightService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.recordsPath = Paths.get(RECORDS_DIR);

        // 确保目录存在
        try {
            Files.createDirectories(recordsPath);
            log.info("QA 记录存储目录: {}", recordsPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create QA records directory", e);
        }
    }

    /**
     * 保存问答记录
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

            log.info("✅ 保存问答记录: {} - {}", record.getId(), recordFile);
            return record.getId();

        } catch (IOException e) {
            log.error("❌ 保存问答记录失败", e);
            return null;
        }
    }

    /**
     * 根据ID获取记录
     */
    public Optional<QARecord> getRecord(String id) {
        try {
            // 遍历所有日期目录查找
            return Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> p.getFileName().toString().contains(id.substring(0, 8)))
                .findFirst()
                .map(this::loadRecord);
        } catch (IOException e) {
            log.error("查找记录失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 更新记录
     */
    public boolean updateRecord(QARecord record) {
        try {
            // 查找现有文件
            Optional<Path> existingFile = Files.walk(recordsPath, 2)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> p.getFileName().toString().contains(record.getId().substring(0, 8)))
                .findFirst();

            if (existingFile.isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(existingFile.get().toFile(), record);
                log.info("✅ 更新问答记录: {}", record.getId());
                return true;
            } else {
                log.warn("⚠️ 记录不存在: {}", record.getId());
                return false;
            }
        } catch (IOException e) {
            log.error("❌ 更新记录失败: {}", record.getId(), e);
            return false;
        }
    }

    /**
     * 添加整体反馈
     */
    public boolean addOverallFeedback(String recordId, int rating, String feedback) {
        Optional<QARecord> recordOpt = getRecord(recordId);
        if (recordOpt.isEmpty()) {
            return false;
        }

        QARecord record = recordOpt.get();
        record.setOverallRating(rating);
        record.setOverallFeedback(feedback);

        log.info("📝 用户反馈 [{}]: 评分={}, 内容={}",
            recordId.substring(0, 8), rating,
            feedback != null && !feedback.isEmpty() ? feedback : "无");

        return updateRecord(record);
    }

    /**
     * 添加文档反馈
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

        // 检查是否已经反馈过
        Optional<QARecord.DocumentFeedback> existing = record.getDocumentFeedbacks().stream()
            .filter(f -> f.getDocumentName().equals(documentName))
            .findFirst();

        if (existing.isPresent()) {
            // 更新现有反馈
            existing.get().setFeedbackType(feedbackType);
            existing.get().setReason(reason);
            existing.get().setFeedbackTime(LocalDateTime.now());
        } else {
            // 添加新反馈
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
        log.info("{} 文档反馈 [{}]: {} - {}",
            emoji, recordId.substring(0, 8), documentName, feedbackType);

        // 根据配置决定是否自动应用反馈
        if (!feedbackConfig.isRequireApproval() && feedbackConfig.isAutoApply()) {
            // 直接应用反馈到文档权重
            documentWeightService.applyFeedback(documentName, feedbackType);
            record.setAppliedToOptimization(true);
            log.info("✅ 反馈已自动应用到文档权重: {}", documentName);
        } else {
            // 设置为待审核
            record.setReviewStatus(QARecord.ReviewStatus.PENDING);
            record.setAppliedToOptimization(false);
            log.info("⏳ 反馈等待审核: {}", documentName);
        }

        return updateRecord(record);
    }

    /**
     * 添加文档星级评价（用户友好接口）
     *
     * 星级到权重调整的映射：
     * 5星 (非常有用) → +0.5 权重
     * 4星 (很有帮助) → +0.2 权重
     * 3星 (一般) → 0 权重（不变）
     * 2星 (帮助不大) → -0.2 权重
     * 1星 (没有帮助) → -0.5 权重
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

        // 将星级转换为反馈类型和权重调整
        QARecord.FeedbackType feedbackType;
        double weightAdjustment;

        switch (rating) {
            case 5:
                feedbackType = QARecord.FeedbackType.LIKE;
                weightAdjustment = 0.5;  // 大幅提升
                break;
            case 4:
                feedbackType = QARecord.FeedbackType.LIKE;
                weightAdjustment = 0.2;  // 提升
                break;
            case 3:
                feedbackType = QARecord.FeedbackType.NEUTRAL;  // 需要在 QARecord 中添加
                weightAdjustment = 0.0;  // 保持不变
                break;
            case 2:
                feedbackType = QARecord.FeedbackType.DISLIKE;
                weightAdjustment = -0.2;  // 降低
                break;
            case 1:
                feedbackType = QARecord.FeedbackType.DISLIKE;
                weightAdjustment = -0.5;  // 大幅降低
                break;
            default:
                return false;
        }

        // 检查是否已经反馈过
        Optional<QARecord.DocumentFeedback> existing = record.getDocumentFeedbacks().stream()
            .filter(f -> f.getDocumentName().equals(documentName))
            .findFirst();

        if (existing.isPresent()) {
            // 更新现有反馈
            existing.get().setFeedbackType(feedbackType);
            existing.get().setReason(comment);
            existing.get().setFeedbackTime(LocalDateTime.now());
        } else {
            // 添加新反馈
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
        log.info("{} 文档星级评价 [{}]: {} - {}星 (权重调整: {})",
            stars, recordId.substring(0, 8), documentName, rating,
            String.format("%+.1f", weightAdjustment));

        // 根据配置决定是否自动应用反馈
        if (!feedbackConfig.isRequireApproval() && feedbackConfig.isAutoApply()) {
            // 直接应用权重调整
            documentWeightService.applyRatingFeedback(documentName, rating, weightAdjustment);
            record.setAppliedToOptimization(true);
            log.info("✅ 星级评价已自动应用到文档权重: {} ({}星 → 权重{})",
                documentName, rating, String.format("%+.1f", weightAdjustment));
        } else {
            // 设置为待审核
            record.setReviewStatus(QARecord.ReviewStatus.PENDING);
            record.setAppliedToOptimization(false);
            log.info("⏳ 星级评价等待审核: {} ({}星)", documentName, rating);
        }

        return updateRecord(record);
    }

    /**
     * 获取最近的记录
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
            log.error("获取最近记录失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取待审核的记录
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
            log.error("获取待审核记录失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取统计信息
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
            log.error("获取统计信息失败", e);
            return new QAStatistics();
        }
    }

    /**
     * 加载记录
     */
    private QARecord loadRecord(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), QARecord.class);
        } catch (IOException e) {
            log.error("加载记录失败: {}", path, e);
            return null;
        }
    }

    /**
     * 统计信息
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

