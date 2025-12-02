package top.yumbo.ai.rag.impl.parser.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Vision LLM 策略（Vision LLM Strategy）
 * <p>
 * 通用的多模态视觉语言模型接口，支持任何兼容的 API 格式：
 * Universal multimodal vision language model interface, supports any compatible API format:
 * <p>
 * 支持的 API 格式（Supported API Formats）：
 * 1. **OpenAI Chat Completions 格式**（标准格式，大多数服务兼容）
 *    - OpenAI GPT-4o / GPT-4 Vision
 *    - DeepSeek VL
 *    - 其他 OpenAI 兼容服务
 * <p>
 * 2. **Ollama 格式**（本地部署）
 *    - Ollama LLaVA
 *    - Ollama MiniCPM-V
 *    - Ollama Qwen-VL
 * <p>
 * 3. **自定义格式**（通过配置适配）
 *    - 任何提供 HTTP API 的视觉模型服务
 * <p>
 * 配置示例（Configuration Examples）：
 * <pre>
 * # OpenAI / DeepSeek (在线)
 * endpoint: https://api.openai.com/v1/chat/completions
 * model: gpt-4o
 * api-key: sk-xxx
 *
 * # Ollama (本地)
 * endpoint: http://localhost:11434/api/generate
 * model: llava:7b
 * api-key: "" (可选)
 *
 * # 自定义服务
 * endpoint: http://your-server:8080/v1/vision
 * model: your-model
 * api-key: your-key
 * </pre>
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class VisionLLMStrategy implements ImageContentExtractorStrategy {

    private final String apiKey;
    private final String model;
    private final String apiEndpoint;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiFormat apiFormat;  // API 格式类型
    private boolean available = false;

    // 默认配置（Default Configuration）
    private static final String DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final int DEFAULT_TIMEOUT = 120;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * API 格式枚举（API Format Enum）
     */
    private enum ApiFormat {
        OPENAI_CHAT,    // OpenAI Chat Completions 格式（标准格式）
        OLLAMA,         // Ollama 格式
        AUTO            // 自动检测
    }

    /**
     * 构造函数（Constructor）
     *
     * @param apiKey API密钥（可选，某些本地服务不需要）
     * @param model 模型名称
     * @param apiEndpoint API端点
     */
    public VisionLLMStrategy(String apiKey, String model, String apiEndpoint) {
        this.apiKey = apiKey;
        this.model = model != null && !model.isEmpty() ? model : DEFAULT_MODEL;
        this.apiEndpoint = apiEndpoint != null && !apiEndpoint.isEmpty() ? apiEndpoint : DEFAULT_API_ENDPOINT;

        // 自动检测 API 格式（Auto-detect API format）
        this.apiFormat = detectApiFormat(this.apiEndpoint);

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();

        this.objectMapper = new ObjectMapper();

        checkAvailability();
    }

    /**
     * 自动检测 API 格式（Auto-detect API format based on endpoint）
     */
    private ApiFormat detectApiFormat(String endpoint) {
        String lowerEndpoint = endpoint.toLowerCase();

        // 检测 Ollama 格式（支持 /api/generate 和 /api/chat）
        if (lowerEndpoint.contains("/api/generate") ||
            lowerEndpoint.contains("/api/chat") ||
            lowerEndpoint.contains(":11434")) {
            log.debug("检测到 Ollama API 格式（Detected Ollama API format）");
            return ApiFormat.OLLAMA;
        }

        // 检测 OpenAI Chat Completions 格式
        if (lowerEndpoint.contains("/chat/completions") || lowerEndpoint.contains("/v1/")) {
            log.debug("检测到 OpenAI Chat Completions API 格式（Detected OpenAI Chat Completions API format）");
            return ApiFormat.OPENAI_CHAT;
        }

        // 默认使用 OpenAI Chat Completions 格式（最通用）
        log.debug("使用默认 OpenAI Chat Completions API 格式（Using default OpenAI Chat Completions API format）");
        return ApiFormat.OPENAI_CHAT;
    }

    /**
     * 从环境变量创建（Create from environment variables）
     */
    public static VisionLLMStrategy fromEnv() {
        String apiKey = System.getenv("VISION_LLM_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("AI_API_KEY");
        }

        String model = System.getenv("VISION_LLM_MODEL");
        String endpoint = System.getenv("VISION_LLM_ENDPOINT");

        return new VisionLLMStrategy(apiKey, model, endpoint);
    }

    private void checkAvailability() {
        try {
            // 尝试测试连接（根据 API 格式选择不同的测试方法）
            if (apiFormat == ApiFormat.OLLAMA) {
                // Ollama：测试 /api/tags 端点
                String baseUrl = apiEndpoint.replace("/api/generate", "").replace("/api/chat", "");
                String testUrl = baseUrl + "/api/tags";
                testConnection(testUrl, false);  // Ollama 不需要认证
            } else {
                // OpenAI Chat Completions：检查 API Key
                if (apiKey == null || apiKey.isEmpty()) {
                    available = false;
                    log.warn("⚠️  Vision LLM 不可用: 未配置 API Key（Vision LLM unavailable: API Key not configured）");
                    log.warn("💡 提示（Hint）: 在配置文件中设置 vision-llm.api-key（Set vision-llm.api-key in config）");
                    return;
                }
                // 不实际测试连接（避免额外费用），假定配置正确
                available = true;
            }

            if (available) {
                log.info("✅ Vision LLM 可用（Vision LLM available）");
                log.info("   - API 格式（API Format）: {}", apiFormat);
                log.info("   - 模型（Model）: {}", model);
                log.info("   - 端点（Endpoint）: {}", apiEndpoint);
            }

        } catch (Exception e) {
            available = false;
            log.warn("⚠️  Vision LLM 服务不可用（Vision LLM service unavailable）: {}", e.getMessage());
            log.warn("💡 提示（Hint）: 请检查服务是否正常运行（Please check if service is running）");
        }
    }

    /**
     * 测试连接（Test connection to the service）
     */
    private void testConnection(String url, boolean requireAuth) throws Exception {
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .get();

        if (requireAuth && apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (response.isSuccessful() || response.code() == 404) {  // 404 也认为服务可用
                available = true;
            } else {
                available = false;
                log.warn("服务响应异常: HTTP {}", response.code());
            }
        }
    }

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        if (!available) {
            return String.format("[图片: %s - Vision LLM不可用]", imageName);
        }

        try {
            log.debug("使用 Vision LLM 处理图片: {}", imageName);

            // 1. 读取图片并转为 base64
            byte[] imageBytes = imageStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 2. 调用 Vision API
            String result = callVisionAPI(base64Image, imageName);

            log.info("Vision LLM 提取内容 [{}]: {} 字符", imageName, result.length());
            return result;

        } catch (Exception e) {
            log.error("Vision LLM 处理失败: {}", imageName, e);
            return String.format("[图片: %s - Vision LLM处理失败: %s]", imageName, e.getMessage());
        }
    }

    @Override
    public String extractContent(File imageFile) {
        if (!available) {
            return String.format("[图片: %s - Vision LLM不可用]", imageFile.getName());
        }

        try {
            log.debug("使用 Vision LLM 处理图片文件: {}", imageFile.getName());

            // 读取文件并转为 base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 调用 Vision API
            String result = callVisionAPI(base64Image, imageFile.getName());

            log.info("Vision LLM 提取内容 [{}]: {} 字符", imageFile.getName(), result.length());
            return result;

        } catch (Exception e) {
            log.error("Vision LLM 处理失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - Vision LLM处理失败: %s]", imageFile.getName(), e.getMessage());
        }
    }

    /**
     * 调用 Vision LLM API（Call Vision LLM API）
     */
    private String callVisionAPI(String base64Image, String imageName) throws Exception {
        // 根据 API 格式构建不同的请求体
        String requestBody = buildVisionRequest(base64Image);

        log.debug("发送 Vision API 请求: {} (格式: {})", model, apiFormat);

        // 创建 HTTP 请求
        Request.Builder requestBuilder = new Request.Builder()
            .url(apiEndpoint)
            .post(RequestBody.create(requestBody, JSON))
            .addHeader("Content-Type", "application/json");

        // OpenAI 格式需要 Authorization header
        if (apiFormat == ApiFormat.OPENAI_CHAT && apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }

        // 发送请求
        long startTime = System.currentTimeMillis();
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("Vision API 错误: HTTP {}, Body: {}", response.code(), errorBody);
                throw new Exception("Vision API 错误: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("收到 Vision API 响应，耗时: {}ms", elapsed);

            // 根据 API 格式解析响应
            return parseVisionResponse(responseBody);
        }
    }

    /**
     * 构建 Vision API 请求体（Build Vision API request body）
     */
    private String buildVisionRequest(String base64Image) throws Exception {
        if (apiFormat == ApiFormat.OLLAMA) {
            return buildOllamaRequest(base64Image);
        } else {
            return buildOpenAIRequest(base64Image);
        }
    }

    /**
     * 构建 OpenAI Chat Completions 格式请求（Build OpenAI Chat Completions format request）
     */
    private String buildOpenAIRequest(String base64Image) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 1000);

        // 构建 messages 数组
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // 构建 content 数组（包含文本和图片）
        ArrayNode content = message.putArray("content");

        // 添加文本提示
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text",
            "请识别并提取这张图片中的所有文字内容。" +
            "如果图片包含表格、图表或其他结构化数据，请详细描述。" +
            "直接返回识别的内容，不需要额外的解释。");

        // 添加图片
        ObjectNode imageContent = content.addObject();
        imageContent.put("type", "image_url");
        ObjectNode imageUrl = imageContent.putObject("image_url");
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imageUrl.put("detail", "high"); // 使用高清模式以获得更好的 OCR 效果

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建 Ollama 格式请求（Build Ollama format request）
     */
    private String buildOllamaRequest(String base64Image) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        // 检查端点类型，使用不同的请求格式
        if (apiEndpoint.contains("/api/chat")) {
            // /api/chat 格式：使用 messages 数组（类似 OpenAI，但图片格式不同）
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content",
                "请识别并提取这张图片中的所有文字内容。" +
                "如果图片包含表格、图表或其他结构化数据，请详细描述。" +
                "直接返回识别的内容，不需要额外的解释。");

            // Ollama chat API 使用 images 数组存放 base64 图片
            ArrayNode images = message.putArray("images");
            images.add(base64Image);

            root.put("stream", false);  // 不使用流式输出
        } else {
            // /api/generate 格式：使用 prompt + images
            root.put("prompt",
                "请识别并提取这张图片中的所有文字内容。" +
                "如果图片包含表格、图表或其他结构化数据，请详细描述。" +
                "直接返回识别的内容，不需要额外的解释。");

            // Ollama 使用 images 数组存放 base64 图片
            ArrayNode images = root.putArray("images");
            images.add(base64Image);

            root.put("stream", false);  // 不使用流式输出
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析 Vision API 响应（Parse Vision API response）
     */
    private String parseVisionResponse(String responseBody) throws Exception {
        if (apiFormat == ApiFormat.OLLAMA) {
            return parseOllamaResponse(responseBody);
        } else {
            return parseOpenAIResponse(responseBody);
        }
    }

    /**
     * 解析 OpenAI Chat Completions 格式响应（Parse OpenAI Chat Completions format response）
     */
    private String parseOpenAIResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 提取内容
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    String result = content.asText();

                    // 记录 token 使用情况
                    JsonNode usage = root.get("usage");
                    if (usage != null) {
                        int promptTokens = usage.path("prompt_tokens").asInt(0);
                        int completionTokens = usage.path("completion_tokens").asInt(0);
                        int totalTokens = usage.path("total_tokens").asInt(0);

                        log.debug("Token 使用 - Prompt: {}, Completion: {}, Total: {}",
                            promptTokens, completionTokens, totalTokens);
                    }

                    return result;
                }
            }
        }

        throw new Exception("无法解析 OpenAI API 响应: " + responseBody);
    }

    /**
     * 解析 Ollama 格式响应（Parse Ollama format response）
     */
    private String parseOllamaResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 尝试 /api/chat 格式: { "message": { "content": "..." }, "done": true }
        JsonNode message = root.get("message");
        if (message != null) {
            JsonNode content = message.get("content");
            if (content != null) {
                String result = content.asText();

                // 记录处理时间等信息
                JsonNode done = root.get("done");
                if (done != null && done.asBoolean()) {
                    log.debug("Ollama 处理完成（/api/chat 格式）");
                }

                return result;
            }
        }

        // 尝试 /api/generate 格式: { "response": "...", "done": true }
        JsonNode response = root.get("response");
        if (response != null) {
            String result = response.asText();

            // 记录处理时间等信息
            JsonNode done = root.get("done");
            if (done != null && done.asBoolean()) {
                log.debug("Ollama 处理完成（/api/generate 格式）");
            }

            return result;
        }

        throw new Exception("无法解析 Ollama API 响应: " + responseBody);
    }

    @Override
    public String getStrategyName() {
        return "Vision LLM (" + model + ")";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}

