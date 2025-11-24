package top.yumbo.ai.rag.spring.boot.llm;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock LLM 客户端
 * 用于测试和演示，返回固定的模拟回答，不调用真实的 AI API
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class MockLLMClient implements LLMClient {

    public MockLLMClient() {
        log.info("✅ Mock LLM 客户端初始化完成（仅用于测试）");
    }

    @Override
    public String generate(String prompt) {
        log.debug("Mock LLM 收到请求，prompt 长度: {}", prompt.length());
        return generateMockResponse(prompt);
    }

    /**
     * 生成模拟回答
     */
    private String generateMockResponse(String prompt) {
        log.info("📝 Mock LLM 返回模拟回答");

        // 根据 prompt 的内容返回不同的模拟回答
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("总人口") || lowerPrompt.contains("population")) {
            return "根据文档内容，中国总人口约为14亿人。\n\n" +
                   "（注意：这是 Mock LLM 的模拟回答，实际数据请参考文档内容）";
        }

        if (lowerPrompt.contains("婚配") || lowerPrompt.contains("marriage")) {
            return "根据文档内容，婚配情况统计数据包括未婚、已婚、离婚、丧偶等状态的人数分布。\n\n" +
                   "（注意：这是 Mock LLM 的模拟回答，实际数据请参考文档内容）";
        }

        if (lowerPrompt.contains("民族") || lowerPrompt.contains("ethnic")) {
            return "根据文档内容，中国有56个民族，包括汉族、蒙古族、回族、藏族等。\n\n" +
                   "（注意：这是 Mock LLM 的模拟回答，实际数据请参考文档内容）";
        }

        // 默认回答
        return "根据提供的文档内容，我为您总结如下：\n\n" +
               "这是一个模拟回答示例。在实际使用中，您需要配置真实的 LLM API Key 来获取准确的答案。\n\n" +
               "**配置方法：**\n" +
               "1. 使用 DeepSeek：设置环境变量 AI_API_KEY\n" +
               "2. 使用 OpenAI：配置 knowledge.qa.llm.provider=openai 并设置 OPENAI_API_KEY\n\n" +
               "（注意：这是 Mock LLM 的模拟回答）";
    }
}

