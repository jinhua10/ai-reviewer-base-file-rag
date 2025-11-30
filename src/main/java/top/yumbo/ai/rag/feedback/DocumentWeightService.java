package top.yumbo.ai.rag.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.config.FeedbackConfig;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档权重管理服务（Document Weight Management Service）
 *
 * 根据用户反馈动态调整文档在检索中的权重（Dynamically adjust document weights in retrieval based on user feedback）
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */
@Slf4j
@Service
public class DocumentWeightService {

    private final FeedbackConfig feedbackConfig;
    private final ObjectMapper objectMapper;

    // 文档权重映射表 <文档名, 权重>（Document weight mapping table <document name, weight>）
    private final Map<String, DocumentWeight> documentWeights = new ConcurrentHashMap<>();

    // 权重文件路径（Weight file path）
    private static final String WEIGHTS_FILE = "data/document-weights.json";

    public DocumentWeightService(FeedbackConfig feedbackConfig) {
        this.feedbackConfig = feedbackConfig;
        this.objectMapper = new ObjectMapper();
        loadWeights();
    }

    /**
     * 文档权重信息（Document weight information）
     */
    @Data
    public static class DocumentWeight {
        private String documentName;
        private double weight = 1.0;           // 当前权重，默认 1.0（Current weight, default 1.0）
        private int likeCount = 0;             // 点赞次数（Number of likes）
        private int dislikeCount = 0;          // 踩的次数（Number of dislikes）
        private double originalWeight = 1.0;   // 原始权重（Original weight）
        private long lastUpdated = System.currentTimeMillis();
    }

    /**
     * 应用用户反馈到文档权重（Apply user feedback to document weights）
     */
    public void applyFeedback(String documentName, QARecord.FeedbackType feedbackType) {
        if (!feedbackConfig.isEnableDynamicWeighting()) {
            log.debug(LogMessageProvider.getMessage("log.feedback.weight_disabled"));
            return;
        }

        DocumentWeight docWeight = documentWeights.computeIfAbsent(
            documentName,
            k -> new DocumentWeight()
        );
        docWeight.setDocumentName(documentName);

        // 更新计数（Update counts）
        if (feedbackType == QARecord.FeedbackType.LIKE) {
            docWeight.setLikeCount(docWeight.getLikeCount() + 1);
            adjustWeight(docWeight, feedbackConfig.getLikeWeightIncrement());
        } else if (feedbackType == QARecord.FeedbackType.DISLIKE) {
            docWeight.setDislikeCount(docWeight.getDislikeCount() + 1);
            adjustWeight(docWeight, feedbackConfig.getDislikeWeightDecrement());
        }

        docWeight.setLastUpdated(System.currentTimeMillis());

        // 保存权重（Save weights）
        saveWeights();

        log.info(LogMessageProvider.getMessage("log.feedback.weight_updated",
            documentName,
            String.format("%.2f", docWeight.getWeight()),
            docWeight.getLikeCount(),
            docWeight.getDislikeCount()
        ));
    }

    /**
     * 调整权重（Adjust weight）
     */
    private void adjustWeight(DocumentWeight docWeight, double delta) {
        double newWeight = docWeight.getWeight() + delta;

        // 应用限制（Apply limits）
        newWeight = Math.max(feedbackConfig.getMinWeight(), newWeight);
        newWeight = Math.min(feedbackConfig.getMaxWeight(), newWeight);

        docWeight.setWeight(newWeight);
    }

    /**
     * 获取文档权重（Get document weight）
     */
    public double getDocumentWeight(String documentName) {
        DocumentWeight docWeight = documentWeights.get(documentName);
        if (docWeight == null) {
            return 1.0; // 默认权重（Default weight）
        }
        return docWeight.getWeight();
    }

    /**
     * 获取所有文档权重（Get all document weights）
     */
    public Map<String, DocumentWeight> getAllWeights() {
        return new HashMap<>(documentWeights);
    }

    /**
     * 重置文档权重（Reset document weight）
     */
    public void resetWeight(String documentName) {
        DocumentWeight docWeight = documentWeights.get(documentName);
        if (docWeight != null) {
            docWeight.setWeight(docWeight.getOriginalWeight());
            docWeight.setLikeCount(0);
            docWeight.setDislikeCount(0);
            docWeight.setLastUpdated(System.currentTimeMillis());
            saveWeights();
            log.info(LogMessageProvider.getMessage("log.feedback.weight_reset", documentName, docWeight.getWeight()));
        }
    }

    /**
     * 清除所有权重（Clear all weights）
     */
    public void clearAllWeights() {
        documentWeights.clear();
        saveWeights();
        log.info(LogMessageProvider.getMessage("log.feedback.weights_cleared"));
    }

    /**
     * 保存权重到文件（Save weights to file）
     */
    private void saveWeights() {
        try {
            Path weightsPath = Paths.get(WEIGHTS_FILE);
            Files.createDirectories(weightsPath.getParent());

            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(weightsPath.toFile(), documentWeights);

            log.debug(LogMessageProvider.getMessage("log.feedback.weights_saved", documentWeights.size()));
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.feedback.save_failed"), e);
        }
    }

    /**
     * 从文件加载权重（Load weights from file）
     */
    private void loadWeights() {
        try {
            File weightsFile = new File(WEIGHTS_FILE);
            if (!weightsFile.exists()) {
                log.info(LogMessageProvider.getMessage("log.feedback.weights_file_not_exists"));
                return;
            }

            Map<String, DocumentWeight> loaded = objectMapper.readValue(
                weightsFile,
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, DocumentWeight.class
                )
            );

            documentWeights.putAll(loaded);
            log.info(LogMessageProvider.getMessage("log.feedback.weights_loaded", documentWeights.size()));

        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.feedback.load_failed"), e);
        }
    }

    /**
     * 获取权重统计信息（Get weight statistics）
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
     * 应用星级评价到文档权重（用户友好接口）（Apply star rating to document weights (user-friendly interface)）
     *
     * @param documentName 文档名称（Document name）
     * @param rating 星级评分 (1-5)（Star rating (1-5)）
     * @param weightAdjustment 权重调整值（Weight adjustment value）
     */
    public void applyRatingFeedback(String documentName, int rating, double weightAdjustment) {
        if (!feedbackConfig.isEnableDynamicWeighting()) {
            log.debug(LogMessageProvider.getMessage("log.feedback.weight_disabled"));
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

        // 直接应用指定的权重调整（Directly apply the specified weight adjustment）
        adjustWeightDirect(docWeight, weightAdjustment);

        // 更新计数（根据星级）（Update counts (based on rating)）
        if (rating >= 4) {
            docWeight.setLikeCount(docWeight.getLikeCount() + 1);
        } else if (rating <= 2) {
            docWeight.setDislikeCount(docWeight.getDislikeCount() + 1);
        }

        docWeight.setLastUpdated(System.currentTimeMillis());

        // 保存权重（Save weights）
        saveWeights();

        String stars = "⭐".repeat(rating);
        log.info(LogMessageProvider.getMessage("log.feedback.rating_updated",
            documentName,
            String.format("%.2f", docWeight.getWeight()),
            stars,
            String.format("%+.1f", weightAdjustment),
            docWeight.getLikeCount(),
            docWeight.getDislikeCount()
        ));
    }

    /**
     * 直接调整权重（用于星级评价）（Directly adjust weight (for star rating)）
     */
    private void adjustWeightDirect(DocumentWeight docWeight, double delta) {
        double newWeight = docWeight.getWeight() + delta;

        // 应用限制（Apply limits）
        newWeight = Math.max(feedbackConfig.getMinWeight(), newWeight);
        newWeight = Math.min(feedbackConfig.getMaxWeight(), newWeight);

        docWeight.setWeight(newWeight);
    }
}
