package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分析上下文（Analysis Context）
 *
 * <p>包含执行分析所需的所有信息，如文档列表、问题、参数等</p>
 * <p>Contains all information needed for analysis, such as document list, question, parameters, etc.</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Data
@Builder
public class AnalysisContext {

    /**
     * 文档路径列表（List of document paths）
     */
    private List<String> documentPaths;

    /**
     * 文档内容列表 - 预加载的（List of document contents - preloaded）
     */
    private List<DocumentContent> documentContents;

    /**
     * 用户问题/分析请求（User question/analysis request）
     */
    private String question;

    /**
     * 分析目标ID（Analysis goal ID）
     */
    private String goalId;

    /**
     * 使用的策略列表（List of strategies to use）
     */
    private List<String> strategies;

    /**
     * 高级参数（Advanced parameters）
     */
    private Map<String, Object> advancedParams;

    /**
     * 语言（Language）
     */
    private String language;

    /**
     * 最大Token限制（Maximum token limit）
     */
    @Builder.Default
    private int maxTokens = 4000;

    /**
     * 是否使用知识库（Whether to use knowledge base）
     */
    @Builder.Default
    private boolean useKnowledgeBase = false;

    /**
     * 上下文窗口大小（Context window size）
     */
    @Builder.Default
    private int contextWindowSize = 8000;

    /**
     * 获取文档数量（Get document count）
     *
     * @return 文档数量（Document count）
     */
    public int getDocumentCount() {
        return documentPaths != null ? documentPaths.size() : 0;
    }

    /**
     * 获取总内容长度（Get total content length）
     *
     * @return 总字符数（Total character count）
     */
    public long getTotalContentLength() {
        if (documentContents == null) return 0;
        return documentContents.stream()
                .mapToLong(d -> d.getContent() != null ? d.getContent().length() : 0)
                .sum();
    }

    /**
     * 检查内容是否超出上下文窗口（Check if content exceeds context window）
     *
     * <p>使用 70% 阈值预留空间给提示词和回答</p>
     * <p>Uses 70% threshold to reserve space for prompts and responses</p>
     *
     * @return 是否超出（Whether exceeds）
     */
    public boolean exceedsContextWindow() {
        return getTotalContentLength() > contextWindowSize * 0.7;
    }

    /**
     * 文档内容（Document Content）
     *
     * <p>表示单个文档的内容和元数据</p>
     * <p>Represents content and metadata of a single document</p>
     */
    @Data
    @Builder
    public static class DocumentContent {
        /** 文件路径（File path） */
        private String path;

        /** 文件名（File name） */
        private String name;

        /** 文本内容（Text content） */
        private String content;

        /** 文件类型（File type） */
        private String type;

        /** 文件大小（File size） */
        private long size;

        /** 元数据（Metadata） */
        private Map<String, Object> metadata;
    }
}

