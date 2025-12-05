package top.yumbo.ai.rag.spring.boot.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 备忘录条目 - 存储单个文档片段的分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoEntry {
    /** 片段编号/索引 */
    private int segmentIndex;

    /** 片段类型 */
    private SegmentType segmentType;

    /** 片段标题 */
    private String title;

    /** 原始内容（完整版） */
    private String originalContent;

    /** 压缩后内容（摘要版） */
    private String compressedContent;

    /** 关键词列表（用于召回匹配） */
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    /** 命名实体（人名、地名、组织等） */
    @Builder.Default
    private List<String> namedEntities = new ArrayList<>();

    /** 重要性评分 (0.0 - 1.0) */
    @Builder.Default
    private double importance = 0.5;

    /** 是否已压缩 */
    @Builder.Default
    private boolean compressed = false;

    /** 原始 Token 数 */
    private int originalTokens;

    /** 压缩后 Token 数 */
    private int compressedTokens;

    /** 创建时间 */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** 最后访问时间（用于 LRU） */
    @Builder.Default
    private Instant lastAccessedAt = Instant.now();

    /** 来源文档信息 */
    private DocumentSource source;

    // ==================== 分层处理支持 ====================

    /** 压缩次数（用于判断是否跳过再次处理） */
    @Builder.Default
    private int compressionCount = 0;

    /** 是否为独立重要条目（不参与聚合） */
    @Builder.Default
    private boolean independent = false;

    /** 用户是否标记为重要 */
    @Builder.Default
    private boolean userMarked = false;

    /** 所属聚合组ID（如果已被聚合） */
    private String aggregationGroupId;

    /** 聚合层级 (0=原始, 1=章节摘要, 2=主题摘要) */
    @Builder.Default
    private int aggregationLevel = 0;

    /** 内容形式标记 */
    @Builder.Default
    private ContentForm contentForm = ContentForm.RAW_TEXT;

    /**
     * 获取当前有效内容（压缩后优先）
     */
    public String getEffectiveContent() {
        if (compressed && compressedContent != null && !compressedContent.isEmpty()) {
            return compressedContent;
        }
        return originalContent;
    }

    /**
     * 获取当前有效 Token 数
     */
    public int getEffectiveTokens() {
        if (compressed && compressedTokens > 0) {
            return compressedTokens;
        }
        return originalTokens;
    }

    /**
     * 更新访问时间
     */
    public void touch() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * 判断是否可以跳过再次压缩
     */
    public boolean canSkipCompression(int minTokensThreshold, double targetCompressionRatio, int maxCompressionRounds) {
        // 1. Token 数已低于阈值
        if (getEffectiveTokens() < minTokensThreshold) {
            return true;
        }

        // 2. 压缩比已达到目标
        if (originalTokens > 0) {
            double ratio = (double) getEffectiveTokens() / originalTokens;
            if (ratio <= targetCompressionRatio) {
                return true;
            }
        }

        // 3. 内容形式已是精简形式
        if (contentForm != ContentForm.RAW_TEXT) {
            return true;
        }

        // 4. 已经过多轮压缩
        if (compressionCount >= maxCompressionRounds) {
            return true;
        }

        return false;
    }

    /**
     * 从 DocumentSegment 创建备忘录条目
     */
    public static MemoEntry fromSegment(DocumentSegment segment, String keyPoints, int tokens) {
        return MemoEntry.builder()
                .segmentIndex(segment.getIndex())
                .segmentType(segment.getType())
                .title(segment.getTitle())
                .originalContent(keyPoints)
                .originalTokens(tokens)
                .source(segment.getSource())
                .createdAt(Instant.now())
                .lastAccessedAt(Instant.now())
                .build();
    }

    /**
     * 获取简要描述
     */
    public String getBriefDescription() {
        String desc = segmentType.getDisplayName() + " " + segmentIndex;
        if (title != null && !title.isEmpty()) {
            desc += " - " + title;
        }
        return desc;
    }

    /**
     * 格式化输出（用于 Prompt）
     */
    public String formatForPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(getBriefDescription()).append("\n");
        sb.append(getEffectiveContent());
        return sb.toString();
    }

    /**
     * 格式化输出（用于备忘录文档）
     */
    public String formatForDocument() {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(segmentType.getDisplayName()).append(" ").append(segmentIndex);
        if (title != null && !title.isEmpty()) {
            sb.append(": ").append(title);
        }
        sb.append("\n");

        if (independent) {
            sb.append("> ⭐ **独立重要条目**\n\n");
        }

        sb.append(getEffectiveContent()).append("\n");

        if (!keywords.isEmpty()) {
            sb.append("\n**关键词**: ").append(String.join(", ", keywords)).append("\n");
        }

        return sb.toString();
    }
}

