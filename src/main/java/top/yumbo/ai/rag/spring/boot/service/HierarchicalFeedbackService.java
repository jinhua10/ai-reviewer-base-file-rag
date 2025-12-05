package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.feedback.DocumentWeightService;
import top.yumbo.ai.rag.feedback.HierarchicalFeedback;
import top.yumbo.ai.rag.feedback.HierarchicalFeedback.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分层反馈服务
 *
 * 支持文档级、段落级、句子级的精细反馈
 *
 * 📈 优化说明（2025-12-05）：
 * 分层反馈机制可减少 2-3 次反馈交互，提升反馈精度
 * 详见: md/20251205140000-RAG系统收敛性分析.md
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Service
public class HierarchicalFeedbackService {

    private final DocumentWeightService documentWeightService;
    private final ObjectMapper objectMapper;

    /** 反馈存储路径 */
    private final Path feedbackStoragePath;

    /** 内存缓存 */
    private final Map<String, HierarchicalFeedback> feedbackCache = new ConcurrentHashMap<>();

    /** 段落权重因子 */
    private static final double PARAGRAPH_WEIGHT_FACTOR = 0.1;

    /** 句子权重因子 */
    private static final double SENTENCE_WEIGHT_FACTOR = 0.05;

    @Autowired
    public HierarchicalFeedbackService(
            @Autowired(required = false) DocumentWeightService documentWeightService) {
        this.documentWeightService = documentWeightService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.feedbackStoragePath = Paths.get("./data/feedback");

        // 确保目录存在
        try {
            Files.createDirectories(feedbackStoragePath);
        } catch (IOException e) {
            log.warn("无法创建反馈存储目录: {}", e.getMessage());
        }

        // 加载已有反馈
        loadExistingFeedbacks();
    }

    /**
     * 提交文档级反馈
     */
    public HierarchicalFeedback submitDocumentFeedback(String qaRecordId, String documentName,
            String documentId, DocumentLevelFeedback feedback) {

        HierarchicalFeedback hierarchicalFeedback = getOrCreateFeedback(qaRecordId, documentName, documentId);
        hierarchicalFeedback.setDocumentFeedback(feedback);
        hierarchicalFeedback.setLevel(FeedbackLevel.DOCUMENT);
        hierarchicalFeedback.setUpdatedAt(LocalDateTime.now());

        // 应用到文档权重
        applyDocumentFeedbackToWeight(documentName, feedback);

        // 保存
        saveFeedback(hierarchicalFeedback);

        log.info("📄 文档级反馈: {} -> 评分={}, 相关性={}",
                documentName, feedback.getRating(), feedback.getRelevance());

        return hierarchicalFeedback;
    }

    /**
     * 提交段落级反馈
     */
    public HierarchicalFeedback submitParagraphFeedback(String qaRecordId, String documentName,
            String documentId, ParagraphFeedback paragraphFeedback) {

        HierarchicalFeedback hierarchicalFeedback = getOrCreateFeedback(qaRecordId, documentName, documentId);

        if (hierarchicalFeedback.getParagraphFeedbacks() == null) {
            hierarchicalFeedback.setParagraphFeedbacks(new ArrayList<>());
        }

        // 更新或添加段落反馈
        boolean updated = false;
        for (int i = 0; i < hierarchicalFeedback.getParagraphFeedbacks().size(); i++) {
            if (hierarchicalFeedback.getParagraphFeedbacks().get(i).getParagraphIndex()
                    == paragraphFeedback.getParagraphIndex()) {
                hierarchicalFeedback.getParagraphFeedbacks().set(i, paragraphFeedback);
                updated = true;
                break;
            }
        }
        if (!updated) {
            hierarchicalFeedback.getParagraphFeedbacks().add(paragraphFeedback);
        }

        hierarchicalFeedback.setLevel(FeedbackLevel.PARAGRAPH);
        hierarchicalFeedback.setUpdatedAt(LocalDateTime.now());

        // 应用到文档权重
        applyParagraphFeedbackToWeight(documentName, paragraphFeedback);

        // 保存
        saveFeedback(hierarchicalFeedback);

        log.info("📝 段落级反馈: {} 段落#{} -> 有帮助={}, 类型={}",
                documentName, paragraphFeedback.getParagraphIndex(),
                paragraphFeedback.isHelpful(), paragraphFeedback.getFeedbackType());

        return hierarchicalFeedback;
    }

    /**
     * 提交句子级反馈（高亮标记）
     */
    public HierarchicalFeedback submitSentenceFeedback(String qaRecordId, String documentName,
            String documentId, SentenceFeedback sentenceFeedback) {

        HierarchicalFeedback hierarchicalFeedback = getOrCreateFeedback(qaRecordId, documentName, documentId);

        if (hierarchicalFeedback.getSentenceFeedbacks() == null) {
            hierarchicalFeedback.setSentenceFeedbacks(new ArrayList<>());
        }

        // 更新或添加句子反馈
        boolean updated = false;
        for (int i = 0; i < hierarchicalFeedback.getSentenceFeedbacks().size(); i++) {
            if (hierarchicalFeedback.getSentenceFeedbacks().get(i).getStartOffset()
                    == sentenceFeedback.getStartOffset()) {
                hierarchicalFeedback.getSentenceFeedbacks().set(i, sentenceFeedback);
                updated = true;
                break;
            }
        }
        if (!updated) {
            hierarchicalFeedback.getSentenceFeedbacks().add(sentenceFeedback);
        }

        hierarchicalFeedback.setLevel(FeedbackLevel.SENTENCE);
        hierarchicalFeedback.setUpdatedAt(LocalDateTime.now());

        // 应用到文档权重
        applySentenceFeedbackToWeight(documentName, sentenceFeedback);

        // 保存
        saveFeedback(hierarchicalFeedback);

        log.info("✨ 句子级反馈: {} 位置[{}-{}] -> 类型={}, 关键信息={}",
                documentName, sentenceFeedback.getStartOffset(), sentenceFeedback.getEndOffset(),
                sentenceFeedback.getHighlightType(), sentenceFeedback.isKeyInformation());

        return hierarchicalFeedback;
    }

    /**
     * 批量提交高亮标记
     */
    public HierarchicalFeedback submitHighlights(String qaRecordId, String documentName,
            String documentId, List<SentenceFeedback> highlights) {

        HierarchicalFeedback hierarchicalFeedback = getOrCreateFeedback(qaRecordId, documentName, documentId);
        hierarchicalFeedback.setSentenceFeedbacks(highlights);
        hierarchicalFeedback.setLevel(FeedbackLevel.SENTENCE);
        hierarchicalFeedback.setUpdatedAt(LocalDateTime.now());

        // 应用所有高亮到权重
        for (SentenceFeedback highlight : highlights) {
            applySentenceFeedbackToWeight(documentName, highlight);
        }

        // 保存
        saveFeedback(hierarchicalFeedback);

        log.info("✨ 批量高亮反馈: {} -> {} 个高亮", documentName, highlights.size());

        return hierarchicalFeedback;
    }

    /**
     * 获取文档的分层反馈
     */
    public Optional<HierarchicalFeedback> getFeedback(String qaRecordId, String documentName) {
        String key = buildCacheKey(qaRecordId, documentName);
        return Optional.ofNullable(feedbackCache.get(key));
    }

    /**
     * 获取问答记录的所有反馈
     */
    public List<HierarchicalFeedback> getFeedbacksByQARecord(String qaRecordId) {
        return feedbackCache.values().stream()
                .filter(f -> qaRecordId.equals(f.getQaRecordId()))
                .toList();
    }

    /**
     * 分析段落内容，提取段落信息
     */
    public List<ParagraphInfo> analyzeDocumentParagraphs(String documentContent) {
        List<ParagraphInfo> paragraphs = new ArrayList<>();

        // 按换行符分割段落
        String[] parts = documentContent.split("\n\n+");
        int offset = 0;

        for (int i = 0; i < parts.length; i++) {
            String paragraph = parts[i].trim();
            if (paragraph.isEmpty()) {
                offset += 2; // 空行
                continue;
            }

            ParagraphInfo info = new ParagraphInfo();
            info.setIndex(i);
            info.setContent(paragraph);
            info.setPreview(paragraph.length() > 100
                    ? paragraph.substring(0, 100) + "..."
                    : paragraph);
            info.setStartOffset(offset);
            info.setEndOffset(offset + paragraph.length());
            info.setWordCount(paragraph.length());

            paragraphs.add(info);
            offset += paragraph.length() + 2;
        }

        return paragraphs;
    }

    /**
     * 获取反馈统计
     */
    public FeedbackStatistics getStatistics() {
        FeedbackStatistics stats = new FeedbackStatistics();

        int totalFeedbacks = feedbackCache.size();
        int documentLevel = 0;
        int paragraphLevel = 0;
        int sentenceLevel = 0;
        int totalHighlights = 0;

        for (HierarchicalFeedback feedback : feedbackCache.values()) {
            if (feedback.getDocumentFeedback() != null) documentLevel++;
            if (feedback.getParagraphFeedbacks() != null && !feedback.getParagraphFeedbacks().isEmpty()) {
                paragraphLevel++;
            }
            if (feedback.getSentenceFeedbacks() != null && !feedback.getSentenceFeedbacks().isEmpty()) {
                sentenceLevel++;
                totalHighlights += feedback.getSentenceFeedbacks().size();
            }
        }

        stats.setTotalFeedbacks(totalFeedbacks);
        stats.setDocumentLevelCount(documentLevel);
        stats.setParagraphLevelCount(paragraphLevel);
        stats.setSentenceLevelCount(sentenceLevel);
        stats.setTotalHighlights(totalHighlights);

        return stats;
    }

    // ==================== 私有方法 ====================

    private HierarchicalFeedback getOrCreateFeedback(String qaRecordId, String documentName, String documentId) {
        String key = buildCacheKey(qaRecordId, documentName);
        return feedbackCache.computeIfAbsent(key, k -> {
            HierarchicalFeedback feedback = new HierarchicalFeedback();
            feedback.setId(UUID.randomUUID().toString());
            feedback.setQaRecordId(qaRecordId);
            feedback.setDocumentName(documentName);
            feedback.setDocumentId(documentId);
            feedback.setCreatedAt(LocalDateTime.now());
            feedback.setUpdatedAt(LocalDateTime.now());
            return feedback;
        });
    }

    private String buildCacheKey(String qaRecordId, String documentName) {
        return qaRecordId + ":" + documentName;
    }

    private void applyDocumentFeedbackToWeight(String documentName, DocumentLevelFeedback feedback) {
        if (documentWeightService == null || feedback.getRating() == null) {
            return;
        }

        double adjustment = (feedback.getRating() - 3) * 0.2; // -0.4 到 +0.4

        if (feedback.getRelevance() != null) {
            switch (feedback.getRelevance()) {
                case HIGHLY_RELEVANT -> adjustment += 0.3;
                case RELEVANT -> adjustment += 0.1;
                case PARTIALLY_RELEVANT -> adjustment += 0.0;
                case NOT_RELEVANT -> adjustment -= 0.2;
                case MISLEADING -> adjustment -= 0.5;
            }
        }

        documentWeightService.applyRatingFeedback(documentName, feedback.getRating(), adjustment);
    }

    private void applyParagraphFeedbackToWeight(String documentName, ParagraphFeedback feedback) {
        if (documentWeightService == null) {
            return;
        }

        double adjustment = 0;

        if (feedback.isHelpful()) {
            adjustment += PARAGRAPH_WEIGHT_FACTOR;
        } else {
            adjustment -= PARAGRAPH_WEIGHT_FACTOR;
        }

        if (feedback.getFeedbackType() != null) {
            switch (feedback.getFeedbackType()) {
                case KEY_POINT -> adjustment += PARAGRAPH_WEIGHT_FACTOR * 2;
                case SUPPORTING_DETAIL -> adjustment += PARAGRAPH_WEIGHT_FACTOR;
                case BACKGROUND -> adjustment += PARAGRAPH_WEIGHT_FACTOR * 0.5;
                case IRRELEVANT -> adjustment -= PARAGRAPH_WEIGHT_FACTOR;
                case WRONG_INFO -> adjustment -= PARAGRAPH_WEIGHT_FACTOR * 2;
                case OUTDATED -> adjustment -= PARAGRAPH_WEIGHT_FACTOR * 1.5;
            }
        }

        int rating = feedback.isHelpful() ? 4 : 2;
        documentWeightService.applyRatingFeedback(documentName, rating, adjustment);
    }

    private void applySentenceFeedbackToWeight(String documentName, SentenceFeedback feedback) {
        if (documentWeightService == null) {
            return;
        }

        double adjustment = 0;

        if (feedback.isKeyInformation()) {
            adjustment += SENTENCE_WEIGHT_FACTOR * 2;
        }

        if (feedback.getHighlightType() != null) {
            switch (feedback.getHighlightType()) {
                case ANSWER -> adjustment += SENTENCE_WEIGHT_FACTOR * 3;
                case KEY_FACT -> adjustment += SENTENCE_WEIGHT_FACTOR * 2;
                case IMPORTANT -> adjustment += SENTENCE_WEIGHT_FACTOR * 1.5;
                case EXAMPLE -> adjustment += SENTENCE_WEIGHT_FACTOR;
                case DEFINITION -> adjustment += SENTENCE_WEIGHT_FACTOR;
                case WRONG -> adjustment -= SENTENCE_WEIGHT_FACTOR * 2;
                case UNCERTAIN -> adjustment += 0;
            }
        }

        if (adjustment != 0) {
            int rating = adjustment > 0 ? 4 : 2;
            documentWeightService.applyRatingFeedback(documentName, rating, adjustment);
        }
    }

    private void saveFeedback(HierarchicalFeedback feedback) {
        String key = buildCacheKey(feedback.getQaRecordId(), feedback.getDocumentName());
        feedbackCache.put(key, feedback);

        // 异步保存到文件
        try {
            Path filePath = feedbackStoragePath.resolve(feedback.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), feedback);
        } catch (IOException e) {
            log.warn("保存反馈失败: {}", e.getMessage());
        }
    }

    private void loadExistingFeedbacks() {
        try {
            File[] files = feedbackStoragePath.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        HierarchicalFeedback feedback = objectMapper.readValue(file, HierarchicalFeedback.class);
                        String key = buildCacheKey(feedback.getQaRecordId(), feedback.getDocumentName());
                        feedbackCache.put(key, feedback);
                    } catch (IOException e) {
                        log.warn("加载反馈文件失败: {}", file.getName());
                    }
                }
                log.info("📂 加载了 {} 个分层反馈记录", feedbackCache.size());
            }
        } catch (Exception e) {
            log.warn("加载反馈目录失败: {}", e.getMessage());
        }
    }

    // ==================== 数据类 ====================

    @lombok.Data
    public static class ParagraphInfo {
        private int index;
        private String content;
        private String preview;
        private int startOffset;
        private int endOffset;
        private int wordCount;
    }

    @lombok.Data
    public static class FeedbackStatistics {
        private int totalFeedbacks;
        private int documentLevelCount;
        private int paragraphLevelCount;
        private int sentenceLevelCount;
        private int totalHighlights;
    }
}

