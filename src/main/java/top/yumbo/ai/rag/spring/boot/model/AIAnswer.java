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

    public AIAnswer(String answer, List<String> sources, long responseTimeMs) {
        this(answer, sources, responseTimeMs, Collections.emptyList(), Collections.emptyList());
    }

    public AIAnswer(String answer, List<String> sources, long responseTimeMs,
                    List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> chunks,
                    List<top.yumbo.ai.rag.image.ImageInfo> images) {
        this.answer = answer;
        this.sources = sources;
        this.responseTimeMs = responseTimeMs;
        this.chunks = chunks != null ? chunks : Collections.emptyList();
        this.images = images != null ? images : Collections.emptyList();
    }

    public String getAnswer() { return answer; }
    public List<String> getSources() { return sources; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> getChunks() { return chunks; }
    public List<top.yumbo.ai.rag.image.ImageInfo> getImages() { return images; }
}
