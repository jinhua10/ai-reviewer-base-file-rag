package top.yumbo.ai.rag.spring.boot.llm;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * Mock LLM 客户端 / Mock LLM Client
 * 用于测试和演示，返回固定的模拟回答，不调用真实的 AI API
 * For testing and demonstration, returns fixed simulated answers without calling real AI APIs
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class MockLLMClient implements LLMClient {

    public MockLLMClient() {
        log.info(I18N.get("llm.log.mock_init"));
    }

    @Override
    public String generate(String prompt) {
        log.debug(I18N.get("llm.log.mock_request", prompt.length()));
        return generateMockResponse(prompt);
    }

    /**
     * 生成模拟回答 / Generate mock response
     */
    private String generateMockResponse(String prompt) {
        log.info(I18N.get("llm.log.mock_response"));

        // 根据 prompt 的内容返回不同的模拟回答 / Return different mock responses based on prompt content
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("总人口") || lowerPrompt.contains("population")) {
            return I18N.get("llm.mock.population_answer");
        }

        if (lowerPrompt.contains("婚配") || lowerPrompt.contains("marriage")) {
            return I18N.get("llm.mock.marriage_answer");
        }

        if (lowerPrompt.contains("民族") || lowerPrompt.contains("ethnic")) {
            return I18N.get("llm.mock.population_answer"); // 可复用或添加新key / Can reuse or add new key
        }

        // 默认回答 / Default answer
        return I18N.get("llm.mock.default_answer");
    }
}

