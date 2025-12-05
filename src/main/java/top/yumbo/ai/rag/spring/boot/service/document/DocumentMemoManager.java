package top.yumbo.ai.rag.spring.boot.service.document;

import top.yumbo.ai.rag.spring.boot.model.document.AnalysisProgress;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSource;
import top.yumbo.ai.rag.spring.boot.model.document.MemoEntry;

import java.util.List;

/**
 * 文档备忘录管理器接口
 *
 * 职责：
 * 1. 管理短期记忆和长期备忘录
 * 2. 控制内容压缩时机
 * 3. 实现智能召回
 * 4. Token 预算管理
 * 5. 支持多种文档类型
 */
public interface DocumentMemoManager {

    // ==================== 初始化 ====================

    /**
     * 开始新文档的分析
     *
     * @param source 文档来源信息
     */
    void startNewDocument(DocumentSource source);

    // ==================== 写入操作 ====================

    /**
     * 添加片段分析结果到备忘录
     *
     * @param segment 文档片段
     * @param analysis 分析结果
     * @param keyPoints 关键点
     */
    void addSegmentAnalysis(DocumentSegment segment, String analysis, String keyPoints);

    /**
     * 标记条目为重要（用户手动标记）
     *
     * @param segmentIndex 片段索引
     */
    void markAsImportant(int segmentIndex);

    // ==================== 读取操作 ====================

    /**
     * 获取短期记忆
     *
     * @return 短期记忆条目列表
     */
    List<MemoEntry> getShortTermMemory();

    /**
     * 获取长期备忘录
     *
     * @return 长期备忘录条目列表
     */
    List<MemoEntry> getLongTermMemo();

    /**
     * 根据当前内容召回相关的长期备忘录条目
     *
     * @param currentSegment 当前片段
     * @param maxTokens 最大返回 token 数
     * @return 召回的备忘录条目
     */
    List<MemoEntry> recallRelevantMemos(DocumentSegment currentSegment, int maxTokens);

    /**
     * 获取所有备忘录的摘要（用于最终总结）
     *
     * @return 完整摘要
     */
    String getAllMemosSummary();

    /**
     * 获取独立重要条目
     *
     * @return 独立重要条目列表
     */
    List<MemoEntry> getIndependentEntries();

    // ==================== 压缩操作 ====================

    /**
     * 压缩指定条目
     *
     * @param entry 备忘录条目
     * @param targetTokens 目标 token 数
     * @return 压缩后的内容
     */
    String compressEntry(MemoEntry entry, int targetTokens);

    // ==================== 工具方法 ====================

    /**
     * 估算文本的 token 数量
     *
     * @param text 文本
     * @return Token 数
     */
    int estimateTokens(String text);

    /**
     * 检查是否有足够的 token 预算
     *
     * @param requiredTokens 需要的 Token 数
     * @return 是否有足够预算
     */
    boolean hasTokenBudget(int requiredTokens);

    /**
     * 获取剩余 Token 预算
     *
     * @return 剩余 Token 数
     */
    int getRemainingTokenBudget();

    /**
     * 清空所有记忆（用于新的文档分析）
     */
    void clear();

    /**
     * 获取当前文档的分析进度
     *
     * @return 分析进度
     */
    AnalysisProgress getProgress();

    // ==================== 导出 ====================

    /**
     * 导出备忘录为 Markdown 文档
     *
     * @return Markdown 格式的备忘录文档
     */
    String exportToMarkdown();

    /**
     * 导出备忘录为 JSON
     *
     * @return JSON 格式的备忘录
     */
    String exportToJson();
}

