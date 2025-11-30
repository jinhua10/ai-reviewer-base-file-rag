package top.yumbo.ai.rag.spring.boot.llm;

/**
 * LLM客户端接口 / LLM Client Interface
 * 用于集成各种大语言模型 / For integrating various large language models
 */
public interface LLMClient {
    /**
     * 生成文本回复 / Generate text response
     *
     * @param prompt 输入的提示词 / Input prompt
     * @return LLM生成的回复 / LLM generated response
     */
    String generate(String prompt);
}

