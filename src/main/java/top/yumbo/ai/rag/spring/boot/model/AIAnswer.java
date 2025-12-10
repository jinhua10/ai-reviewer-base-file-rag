package top.yumbo.ai.rag.spring.boot.model;

import top.yumbo.ai.rag.spring.boot.service.SimilarQAService;

import java.util.Collections;
import java.util.List;

/**
 * AI答案封装类 / AI Answer Wrapper Class
 * 
 * 封装AI生成的答案及其相关信息，包括源文档、响应时间等
 * (Wraps AI-generated answers and their related information, including source documents, response time, etc.)
 * 
 * @author AI Reviewer Team
 * @since 2.0.0
 */
public class AIAnswer {
    private final String answer;
    private final List<String> sources;
    private final long responseTimeMs;
    private final List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks;
    private final List<top.yumbo.ai.rag.image.ImageInfo> images;

    // 新增：文档使用情况 / New: Document usage
    private final List<String> usedDocuments;  // 本次实际使用的文档 / Documents actually used this time
    private final int totalRetrieved;          // 检索到的总文档数 / Total number of documents retrieved
    private final boolean hasMoreDocuments;     // 是否还有更多文档未处理 / Whether there are more unprocessed documents

    // 新增：问答记录ID（用于反馈）/ New: QA record ID (for feedback)
    private String recordId;

    // 新增：会话ID（用于分页引用）/ New: Session ID (for paginated referencing)
    private String sessionId;

    // 新增：相似问题推荐 / New: Similar question recommendations
    private List<SimilarQAService.SimilarQA> similarQuestions;

    // 新增：HOPE 相关字段 / New: HOPE related fields
    private String hopeSource;       // HOPE 来源层：permanent/ordinary/high_frequency/null
    private boolean directAnswer;    // 是否为直接回答（未调用 LLM）
    private String strategyUsed;     // 使用的策略：DIRECT_ANSWER/TEMPLATE_ANSWER/REFERENCE_ANSWER/FULL_RAG
    private double hopeConfidence;   // HOPE 查询置信度

    public AIAnswer(String answer, List<String> sources, long responseTimeMs) {
        this(answer, sources, responseTimeMs, Collections.emptyList(), Collections.emptyList(),
             Collections.emptyList(), 0, false);
    }

    public AIAnswer(String answer, List<String> sources, long responseTimeMs,
                    List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks,
                    List<top.yumbo.ai.rag.image.ImageInfo> images) {
        this(answer, sources, responseTimeMs, chunks, images,
             Collections.emptyList(), 0, false);
    }

    public AIAnswer(String answer, List<String> sources, long responseTimeMs,
                    List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks,
                    List<top.yumbo.ai.rag.image.ImageInfo> images,
                    List<String> usedDocuments, int totalRetrieved, boolean hasMoreDocuments) {
        this.answer = answer;
        this.sources = sources;
        this.responseTimeMs = responseTimeMs;
        this.chunks = chunks != null ? chunks : Collections.emptyList();
        this.images = images != null ? images : Collections.emptyList();
        this.usedDocuments = usedDocuments != null ? usedDocuments : Collections.emptyList();
        this.totalRetrieved = totalRetrieved;
        this.hasMoreDocuments = hasMoreDocuments;
    }

    public String getAnswer() { return answer; }
    public List<String> getSources() { return sources; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> getChunks() { return chunks; }
    public List<top.yumbo.ai.rag.image.ImageInfo> getImages() { return images; }
    public List<String> getUsedDocuments() { return usedDocuments; }
    public int getTotalRetrieved() { return totalRetrieved; }
    public boolean isHasMoreDocuments() { return hasMoreDocuments; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<SimilarQAService.SimilarQA> getSimilarQuestions() { return similarQuestions; }
    public void setSimilarQuestions(List<SimilarQAService.SimilarQA> similarQuestions) { this.similarQuestions = similarQuestions; }

    // HOPE 相关 getter/setter
    public String getHopeSource() { return hopeSource; }
    public void setHopeSource(String hopeSource) { this.hopeSource = hopeSource; }
    public boolean isDirectAnswer() { return directAnswer; }
    public void setDirectAnswer(boolean directAnswer) { this.directAnswer = directAnswer; }
    public String getStrategyUsed() { return strategyUsed; }
    public void setStrategyUsed(String strategyUsed) { this.strategyUsed = strategyUsed; }
    public double getHopeConfidence() { return hopeConfidence; }
    public void setHopeConfidence(double hopeConfidence) { this.hopeConfidence = hopeConfidence; }
}
