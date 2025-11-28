package top.yumbo.ai.rag.spring.boot.model;

import java.util.Collections;
import java.util.List;

/**
 * AI答案封装类
 */
public class AIAnswer {
    private final String answer;
    private final List<String> sources;
    private final long responseTimeMs;
    private final List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks;
    private final List<top.yumbo.ai.rag.image.ImageInfo> images;

    // 新增：文档使用情况
    private final List<String> usedDocuments;  // 本次实际使用的文档
    private final int totalRetrieved;          // 检索到的总文档数
    private final boolean hasMoreDocuments;     // 是否还有更多文档未处理

    // 新增：问答记录ID（用于反馈）
    private String recordId;

    // 新增：会话ID（用于分页引用）
    private String sessionId;

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
}
