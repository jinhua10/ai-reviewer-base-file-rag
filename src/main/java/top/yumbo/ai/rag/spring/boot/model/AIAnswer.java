package top.yumbo.ai.rag.spring.boot.model;

import lombok.Data;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo;
import top.yumbo.ai.rag.image.ImageInfo;
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
@Data
public class AIAnswer {
    private final String answer;
    private final List<String> sources;
    private final long responseTimeMs;
    private final List<ChunkStorageInfo> chunks;
    private final List<ImageInfo> images;

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


}
