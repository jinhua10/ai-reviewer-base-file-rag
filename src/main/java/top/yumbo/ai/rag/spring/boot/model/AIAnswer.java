package top.yumbo.ai.rag.spring.boot.model;

import java.util.List;

/**
 * AI答案封装类
 */
public class AIAnswer {
    private final String answer;
    private final List<String> sources;
    private final long responseTimeMs;

    public AIAnswer(String answer, List<String> sources, long responseTimeMs) {
        this.answer = answer;
        this.sources = sources;
        this.responseTimeMs = responseTimeMs;
    }

    public String getAnswer() { return answer; }
    public List<String> getSources() { return sources; }
    public long getResponseTimeMs() { return responseTimeMs; }
}
