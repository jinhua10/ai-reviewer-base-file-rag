package top.yumbo.ai.rag.spring.boot.streaming.model;

import lombok.Builder;
import lombok.Data;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.model.Document;

import java.util.List;

/**
 * HOPE 快速答案
 * (HOPE Fast Answer)
 *
 * 用于双轨响应架构中的快速答案展示
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Data
@Builder
public class HOPEAnswer {

    /**
     * 答案内容
     * (Answer content)
     */
    private String answer;

    /**
     * 置信度 (0-1)
     * (Confidence score)
     */
    private double confidence;

    /**
     * 来源类型
     * (Source type)
     */
    private String source;

    /**
     * 能否直接回答
     * (Can answer directly)
     */
    private boolean canDirectAnswer;

    /**
     * 响应时间（毫秒）
     * (Response time in milliseconds)
     */
    private long responseTime;

    /**
     * 关联概念ID
     * (Related concept ID)
     */
    private String conceptId;

    /**
     * 相关概念列表
     * (Related concepts)
     */
    private List<Document> relatedConcepts;

    /**
     * 相似度评分
     * (Similarity score)
     */
    private double similarityScore;

    /**
     * 来源类型常量
     * (Source type constants)
     */
    public static class SourceType {
        public static final String HOPE_PERMANENT = "HOPE_PERMANENT";
        public static final String HOPE_ORDINARY = "HOPE_ORDINARY";
        public static final String CONCEPT_LIBRARY = "CONCEPT_LIBRARY";
        public static final String NONE = "NONE";
    }
}

