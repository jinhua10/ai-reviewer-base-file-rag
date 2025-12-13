package top.yumbo.ai.rag.spring.boot.llm;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 流式生成模拟回答 / Generate mock response with streaming
     *
     * @param prompt 输入的提示词 / Input prompt
     * @return Flux 流，实时发送文本块 / Flux stream emitting text chunks in real-time
     */
    @Override
    public Flux<String> generateStream(String prompt) {
        log.debug(I18N.get("llm.log.mock_request", prompt.length()) + " [Streaming]");

        // 生成完整的模拟回答
        String fullResponse = generateMockResponse(prompt);

        // 将完整回答分割成多个块，模拟流式输出
        // Split full response into chunks to simulate streaming
        List<String> chunks = new ArrayList<>();
        int chunkSize = 5; // 每次发送5个字符

        for (int i = 0; i < fullResponse.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, fullResponse.length());
            chunks.add(fullResponse.substring(i, end));
        }

        // 使用 Flux.fromIterable + delayElements 实现流式输出，避免阻塞
        // Use Flux.fromIterable + delayElements for streaming output without blocking
        return Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(50)); // 每个块延迟 50ms
    }

    /**
     * 流式生成文本回复（带系统提示）/ Generate text response with streaming (with system prompt)
     */
    @Override
    public Flux<String> generateStream(String prompt, String systemPrompt) {
        log.debug("Mock streaming with system prompt: " + (systemPrompt != null ? systemPrompt.substring(0, Math.min(50, systemPrompt.length())) : "null"));
        return generateStream(prompt);
    }
}

