package top.yumbo.ai.rag.spring.boot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.rag.i18n.I18N;

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

    // 可配置参数 / Configurable parameters
    private final double temperature;
    private final int maxTokens;

    // 默认 API 端点 / Default API endpoint
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT = 60;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 默认参数 / Default parameters
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 2000;

    /**
     * 构造函数 / Constructor
     *
     * @param apiKey API Key
     * @param model 模型名称 (如 "gpt-4o", "gpt-4-turbo") / Model name (e.g. "gpt-4o", "gpt-4-turbo")
     * @param apiUrl API 端点 (null 则使用默认) / API endpoint (use default if null)
     */
    public OpenAILLMClient(String apiKey, String model, String apiUrl) {
        this(apiKey, model, apiUrl, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
    }

    /**
     * 完整构造函数 / Full constructor
     *
     * @param apiKey API Key
     * @param model 模型名称 / Model name
     * @param apiUrl API 端点 / API endpoint
     * @param temperature 温度参数 (0.0-2.0，越高越随机) / Temperature (0.0-2.0, higher = more random)
     * @param maxTokens 最大生成令牌数 / Maximum tokens to generate
     */
    public OpenAILLMClient(String apiKey, String model, String apiUrl, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "gpt-4o";
        this.apiUrl = apiUrl != null && !apiUrl.isEmpty() ? apiUrl : DEFAULT_API_URL;
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();

        this.objectMapper = new ObjectMapper();

        log.info(I18N.get("llm.log.openai_init"));
        log.info("   - Model: {}", this.model);
        log.info("   - Endpoint: {}", this.apiUrl);
        log.info("   - Temperature: {}", this.temperature);
        log.info("   - MaxTokens: {}", this.maxTokens);
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

    @Override
    public String generate(String prompt, String systemPrompt) {
        try {
            log.info(prompt);
            // 构建请求消息
            List<Map<String, String>> messages = buildMessages(systemPrompt, prompt);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 发送请求 / Send request
            String response = sendRequest(requestBody);

            // 解析响应 / Parse response
            return parseResponse(response);

        } catch (Exception e) {
            log.error(I18N.get("llm.log.openai_failed"), e);
            throw new RuntimeException(I18N.get("llm.error.openai_failed", e.getMessage()), e);
        }
    }

    @Override
    public String generateWithImage(String prompt, String imageUrl, String systemPrompt) {
        try {
            log.info("Generating with image: {}", prompt);

            // 构建包含图片的消息
            List<Map<String, Object>> messages = buildMessagesWithImage(systemPrompt, prompt, imageUrl);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 发送请求 / Send request
            String response = sendRequest(requestBody);

            // 解析响应 / Parse response
            return parseResponse(response);

        } catch (Exception e) {
            log.error(I18N.get("llm.log.openai_failed"), e);
            throw new RuntimeException(I18N.get("llm.error.openai_failed", e.getMessage()), e);
        }
    }

    @Override
    public String generateWithImages(String prompt, List<String> imageUrls, String systemPrompt) {
        try {
            log.info("Generating with {} images: {}", imageUrls.size(), prompt);

            // 构建包含多张图片的消息
            List<Map<String, Object>> messages = buildMessagesWithImages(systemPrompt, prompt, imageUrls);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // 发送请求 / Send request
            String response = sendRequest(requestBody);

            // 解析响应 / Parse response
            return parseResponse(response);

        } catch (Exception e) {
            log.error(I18N.get("llm.log.openai_failed"), e);
            throw new RuntimeException(I18N.get("llm.error.openai_failed", e.getMessage()), e);
        }
    }

    @Override
    public boolean supportsImageInput() {
        // 支持图片的模型列表 / Models that support images
        return model != null && (
            model.contains("gpt-4o") ||
            model.contains("gpt-4-vision") ||
            model.contains("qwen-vl") ||
            model.contains("qwen3-vl") ||
            model.contains("claude-3")
        );
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getModelName() {
        return model;
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
     * 构建包含单张图片的消息列表 / Build message list with single image
     */
    private List<Map<String, Object>> buildMessagesWithImage(String systemPrompt, String userPrompt, String imageUrl) {
        List<Map<String, Object>> messages = new java.util.ArrayList<>();

        // 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }

        // 添加用户提示（包含文本和图片）
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> contentParts = new java.util.ArrayList<>();

        // 添加文本部分
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userPrompt);
        contentParts.add(textPart);

        // 添加图片部分
        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrlObj = new HashMap<>();
        imageUrlObj.put("url", imageUrl);
        imagePart.put("image_url", imageUrlObj);
        contentParts.add(imagePart);

        userMessage.put("content", contentParts);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 构建包含多张图片的消息列表 / Build message list with multiple images
     */
    private List<Map<String, Object>> buildMessagesWithImages(String systemPrompt, String userPrompt, List<String> imageUrls) {
        List<Map<String, Object>> messages = new java.util.ArrayList<>();

        // 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }

        // 添加用户提示（包含文本和多张图片）
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Map<String, Object>> contentParts = new java.util.ArrayList<>();

        // 添加文本部分
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userPrompt);
        contentParts.add(textPart);

        // 添加图片部分
        for (String imageUrl : imageUrls) {
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> imageUrlObj = new HashMap<>();
            imageUrlObj.put("url", imageUrl);
            imagePart.put("image_url", imageUrlObj);
            contentParts.add(imagePart);
        }

        userMessage.put("content", contentParts);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 发送 HTTP 请求 / Send HTTP request
     */
    private String sendRequest(Map<String, Object> requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        log.debug(I18N.get("llm.log.openai_request", model));
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
                log.error(I18N.get("llm.log.openai_error", response.code(), errorBody));
                throw new IOException(I18N.get("llm.error.openai_http_error", response.code(), errorBody));
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
                    log.debug(I18N.get("llm.log.openai_response",
                        result.substring(0, Math.min(200, result.length()))));
                    return result;
                }
            }
        }

        throw new IOException(I18N.get("llm.error.parse_failed", responseBody));
    }
}

