package top.yumbo.ai.rag.spring.boot.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分析上下文
 * (Analysis Context)
 *
 * 包含执行分析所需的所有信息
 * (Contains all information needed for analysis)
 */
@Data
@Builder
public class AnalysisContext {

    /**
     * 文档路径列表
     * (List of document paths)
     */
    private List<String> documentPaths;

    /**
     * 文档内容列表（预加载的）
     * (List of document contents - preloaded)
     */
    private List<DocumentContent> documentContents;

    /**
     * 用户问题/分析请求
     * (User question/analysis request)
     */
    private String question;

    /**
     * 分析目标ID
     * (Analysis goal ID)
     */
    private String goalId;

    /**
     * 使用的策略列表
     * (List of strategies to use)
     */
    private List<String> strategies;

    /**
     * 高级参数
     * (Advanced parameters)
     */
    private Map<String, Object> advancedParams;

    /**
     * 语言
     * (Language)
     */
    private String language;

    /**
     * 最大Token限制
     * (Maximum token limit)
     */
    @Builder.Default
    private int maxTokens = 4000;

    /**
     * 是否使用知识库
     * (Whether to use knowledge base)
     */
    @Builder.Default
    private boolean useKnowledgeBase = false;

    /**
     * 上下文窗口大小
     * (Context window size)
     */
    @Builder.Default
    private int contextWindowSize = 8000;

    /**
     * 获取文档数量
     * (Get document count)
     */
    public int getDocumentCount() {
        return documentPaths != null ? documentPaths.size() : 0;
    }

    /**
     * 获取总内容长度
     * (Get total content length)
     */
    public long getTotalContentLength() {
        if (documentContents == null) return 0;
        return documentContents.stream()
                .mapToLong(d -> d.getContent() != null ? d.getContent().length() : 0)
                .sum();
    }

    /**
     * 检查内容是否超出上下文窗口
     * (Check if content exceeds context window)
     */
    public boolean exceedsContextWindow() {
        return getTotalContentLength() > contextWindowSize * 0.7;
    }

    /**
     * 文档内容
     * (Document Content)
     */
    @Data
    @Builder
    public static class DocumentContent {
        private String path;
        private String name;
        private String content;
        private String type;
        private long size;
        private Map<String, Object> metadata;
    }
}

