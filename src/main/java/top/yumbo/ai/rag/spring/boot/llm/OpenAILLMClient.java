package top.yumbo.ai.rag.spring.boot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI LLM 客户端 / OpenAI LLM Client
 *
 * 支持的模型 / Supported models:
 * - gpt-4o (最新多模态模型 / Latest multimodal model)
 * - gpt-4-turbo (GPT-4 Turbo)
 * - gpt-4 (GPT-4)
 * - gpt-3.5-turbo (GPT-3.5)
 * - 未来的 GPT-5 等 / Future GPT-5, etc.
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class OpenAILLMClient implements LLMClient {

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // 默认 API 端点 / Default API endpoint
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT = 60;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 构造函数 / Constructor
     *
     * @param apiKey API Key
     * @param model 模型名称 (如 "gpt-4o", "gpt-4-turbo") / Model name (e.g. "gpt-4o", "gpt-4-turbo")
     * @param apiUrl API 端点 (null 则使用默认) / API endpoint (use default if null)
     */
    public OpenAILLMClient(String apiKey, String model, String apiUrl) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "gpt-4o";
        this.apiUrl = apiUrl != null && !apiUrl.isEmpty() ? apiUrl : DEFAULT_API_URL;

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();

        this.objectMapper = new ObjectMapper();

        log.info(LogMessageProvider.getMessage("llm.log.openai_init"));
        log.info("   - Model: {}", this.model);
        log.info("   - Endpoint: {}", this.apiUrl);
    }

    /**
     * 从环境变量创建 / Create from environment variables
     */
    public static OpenAILLMClient fromEnv() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_API_KEY");
        }

        String model = System.getenv("OPENAI_MODEL");
        if (model == null || model.isEmpty()) {
            model = "gpt-4o";
        }

        String apiUrl = System.getenv("OPENAI_API_URL");

        return new OpenAILLMClient(apiKey, model, apiUrl);
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, null);
    }

    public String generate(String prompt, String systemPrompt) {
        try {
            log.info(prompt);
            // 构建请求消息
            List<Map<String, String>> messages = buildMessages(systemPrompt, prompt);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 发送请求 / Send request
            String response = sendRequest(requestBody);

            // 解析响应 / Parse response
            return parseResponse(response);

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("llm.log.openai_failed"), e);
            throw new RuntimeException(LogMessageProvider.getMessage("llm.error.openai_failed", e.getMessage()), e);
        }
    }

    /**
     * 构建消息列表 / Build message list
     */
    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new java.util.ArrayList<>();

        // 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }

        // 添加用户提示
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 发送 HTTP 请求 / Send HTTP request
     */
    private String sendRequest(Map<String, Object> requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        log.debug(LogMessageProvider.getMessage("llm.log.openai_request", model));
        log.debug("Request body: {}", jsonBody);

        Request request = new Request.Builder()
            .url(apiUrl)
            .post(RequestBody.create(jsonBody, JSON))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                log.error(LogMessageProvider.getMessage("llm.log.openai_error", response.code(), errorBody));
                throw new IOException(LogMessageProvider.getMessage("llm.error.openai_http_error", response.code(), errorBody));
            }

            String responseBody = response.body().string();
            log.debug("Received response: {}", responseBody);

            return responseBody;
        }
    }

    /**
     * 解析响应 / Parse response
     */
    private String parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // 提取回复内容 / Extract reply content
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    String result = content.asText();
                    log.debug(LogMessageProvider.getMessage("llm.log.openai_response",
                        result.substring(0, Math.min(200, result.length()))));
                    return result;
                }
            }
        }

        throw new IOException(LogMessageProvider.getMessage("llm.error.parse_failed", responseBody));
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String getModelName() {
        return model;
    }
}

