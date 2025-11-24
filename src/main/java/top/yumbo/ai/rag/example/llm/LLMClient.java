package top.yumbo.ai.rag.example.llm;

/**
 * LLM客户端接口
 * 用于集成各种大语言模型
 */
public interface LLMClient {
    /**
     * 生成文本回复
     *
     * @param prompt 输入的提示词
     * @return LLM生成的回复
     */
    String generate(String prompt);
}

