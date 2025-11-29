package top.yumbo.ai.rag.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.config.FeedbackConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档权重管理服务
 *
 * 根据用户反馈动态调整文档在检索中的权重
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */
@Slf4j
@Service
public class DocumentWeightService {

    private final FeedbackConfig feedbackConfig;
    private final ObjectMapper objectMapper;

    // 文档权重映射表 <文档名, 权重>
    private final Map<String, DocumentWeight> documentWeights = new ConcurrentHashMap<>();

    // 权重文件路径
    private static final String WEIGHTS_FILE = "data/document-weights.json";

    public DocumentWeightService(FeedbackConfig feedbackConfig) {
        this.feedbackConfig = feedbackConfig;
        this.objectMapper = new ObjectMapper();
        loadWeights();
    }

    /**
     * 文档权重信息
     */
    @Data
    public static class DocumentWeight {
        private String documentName;
        private double weight = 1.0;           // 当前权重，默认 1.0
        private int likeCount = 0;             // 点赞次数
        private int dislikeCount = 0;          // 踩的次数
        private double originalWeight = 1.0;   // 原始权重
        private long lastUpdated = System.currentTimeMillis();
    }

    /**
     * 应用用户反馈到文档权重
     */
    public void applyFeedback(String documentName, QARecord.FeedbackType feedbackType) {
        if (!feedbackConfig.isEnableDynamicWeighting()) {
            log.debug("动态权重调整已禁用");
            return;
        }

        DocumentWeight docWeight = documentWeights.computeIfAbsent(
            documentName,
            k -> new DocumentWeight()
        );
        docWeight.setDocumentName(documentName);

        // 更新计数
        if (feedbackType == QARecord.FeedbackType.LIKE) {
            docWeight.setLikeCount(docWeight.getLikeCount() + 1);
            adjustWeight(docWeight, feedbackConfig.getLikeWeightIncrement());
        } else if (feedbackType == QARecord.FeedbackType.DISLIKE) {
            docWeight.setDislikeCount(docWeight.getDislikeCount() + 1);
            adjustWeight(docWeight, feedbackConfig.getDislikeWeightDecrement());
        }

        docWeight.setLastUpdated(System.currentTimeMillis());

        // 保存权重
        saveWeights();

        log.info("📊 文档权重更新: {} -> 权重={} (👍{} 👎{})",
            documentName,
            String.format("%.2f", docWeight.getWeight()),
            docWeight.getLikeCount(),
            docWeight.getDislikeCount()
        );
    }

    /**
     * 调整权重
     */
    private void adjustWeight(DocumentWeight docWeight, double delta) {
        double newWeight = docWeight.getWeight() + delta;

        // 应用限制
        newWeight = Math.max(feedbackConfig.getMinWeight(), newWeight);
        newWeight = Math.min(feedbackConfig.getMaxWeight(), newWeight);

        docWeight.setWeight(newWeight);
    }

    /**
     * 获取文档权重
     */
    public double getDocumentWeight(String documentName) {
        DocumentWeight docWeight = documentWeights.get(documentName);
        if (docWeight == null) {
            return 1.0; // 默认权重
        }
        return docWeight.getWeight();
    }

    /**
     * 获取所有文档权重
     */
    public Map<String, DocumentWeight> getAllWeights() {
        return new HashMap<>(documentWeights);
    }

    /**
     * 重置文档权重
     */
    public void resetWeight(String documentName) {
        DocumentWeight docWeight = documentWeights.get(documentName);
        if (docWeight != null) {
            docWeight.setWeight(docWeight.getOriginalWeight());
            docWeight.setLikeCount(0);
            docWeight.setDislikeCount(0);
            docWeight.setLastUpdated(System.currentTimeMillis());
            saveWeights();
            log.info("🔄 重置文档权重: {} -> {}", documentName, docWeight.getWeight());
        }
    }

    /**
     * 清除所有权重
     */
    public void clearAllWeights() {
        documentWeights.clear();
        saveWeights();
        log.info("🧹 清除所有文档权重");
    }

    /**
     * 保存权重到文件
     */
    private void saveWeights() {
        try {
            Path weightsPath = Paths.get(WEIGHTS_FILE);
            Files.createDirectories(weightsPath.getParent());

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(weightsPath.toFile(), documentWeights);

            log.debug("💾 保存文档权重: {} 个文档", documentWeights.size());
        } catch (IOException e) {
            log.error("保存文档权重失败", e);
        }
    }

    /**
     * 从文件加载权重
     */
    private void loadWeights() {
        try {
            File weightsFile = new File(WEIGHTS_FILE);
            if (!weightsFile.exists()) {
                log.info("📂 文档权重文件不存在，使用默认权重");
                return;
            }

            Map<String, DocumentWeight> loaded = objectMapper.readValue(
                weightsFile,
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, DocumentWeight.class
                )
            );

            documentWeights.putAll(loaded);
            log.info("📂 加载文档权重: {} 个文档", documentWeights.size());

        } catch (IOException e) {
            log.error("加载文档权重失败", e);
        }
    }

    /**
     * 获取权重统计信息
     */
    public Map<String, Object> getStatistics() {
        int totalDocs = documentWeights.size();
        int highWeightDocs = 0;
        int lowWeightDocs = 0;
        double avgWeight = 0.0;

        for (DocumentWeight dw : documentWeights.values()) {
            avgWeight += dw.getWeight();
            if (dw.getWeight() > 1.2) highWeightDocs++;
            if (dw.getWeight() < 0.8) lowWeightDocs++;
        }

        if (totalDocs > 0) {
            avgWeight /= totalDocs;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", totalDocs);
        stats.put("highWeightDocuments", highWeightDocs);
        stats.put("lowWeightDocuments", lowWeightDocs);
        stats.put("averageWeight", String.format("%.2f", avgWeight));

        return stats;
    }

    /**
     * 应用星级评价到文档权重（用户友好接口）
     *
     * @param documentName 文档名称
     * @param rating 星级评分 (1-5)
     * @param weightAdjustment 权重调整值
     */
    public void applyRatingFeedback(String documentName, int rating, double weightAdjustment) {
        if (!feedbackConfig.isEnableDynamicWeighting()) {
            log.debug("动态权重调整已禁用");
            return;
        }

        DocumentWeight docWeight = documentWeights.computeIfAbsent(
            documentName,
            k -> {
                DocumentWeight dw = new DocumentWeight();
                dw.setDocumentName(documentName);
                return dw;
            }
        );

        // 直接应用指定的权重调整
        adjustWeightDirect(docWeight, weightAdjustment);

        // 更新计数（根据星级）
        if (rating >= 4) {
            docWeight.setLikeCount(docWeight.getLikeCount() + 1);
        } else if (rating <= 2) {
            docWeight.setDislikeCount(docWeight.getDislikeCount() + 1);
        }

        docWeight.setLastUpdated(System.currentTimeMillis());

        // 保存权重
        saveWeights();

        String stars = "⭐".repeat(rating);
        log.info("📊 文档权重更新(星级): {} -> 权重={} ({}星, 调整{}, 👍{} 👎{})",
            documentName,
            String.format("%.2f", docWeight.getWeight()),
            stars,
            String.format("%+.1f", weightAdjustment),
            docWeight.getLikeCount(),
            docWeight.getDislikeCount()
        );
    }

    /**
     * 直接调整权重（用于星级评价）
     */
    private void adjustWeightDirect(DocumentWeight docWeight, double delta) {
        double newWeight = docWeight.getWeight() + delta;

        // 应用限制
        newWeight = Math.max(feedbackConfig.getMinWeight(), newWeight);
        newWeight = Math.min(feedbackConfig.getMaxWeight(), newWeight);

        docWeight.setWeight(newWeight);
    }
}
