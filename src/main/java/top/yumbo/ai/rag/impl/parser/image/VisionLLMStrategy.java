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
 * Vision LLM 策略
 *
 * 使用多模态大语言模型理解图片内容（支持 OCR）
 *
 * 支持的模型:
 * - gpt-4o (推荐，最新多模态)
 * - gpt-4-turbo (GPT-4 Turbo with vision)
 * - gpt-4-vision-preview (GPT-4 Vision)
 * - 未来的 gpt-5 (发布后自动支持)
 *
 * 使用场景:
 * - OCR 文字识别（包括手写）
 * - 理解图表、图形的语义
 * - 提取结构化信息
 * - 描述图片内容
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
    private boolean available = false;

    // 默认配置
    private static final String DEFAULT_API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final int DEFAULT_TIMEOUT = 60;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 构造函数
     *
     * @param apiKey API密钥
     * @param model 模型名称（如 "gpt-4o"）
     * @param apiEndpoint API端点
     */
    public VisionLLMStrategy(String apiKey, String model, String apiEndpoint) {
        this.apiKey = apiKey;
        this.model = model != null && !model.isEmpty() ? model : DEFAULT_MODEL;
        this.apiEndpoint = apiEndpoint != null && !apiEndpoint.isEmpty() ? apiEndpoint : DEFAULT_API_ENDPOINT;

        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();

        this.objectMapper = new ObjectMapper();

        checkAvailability();
    }

    /**
     * 从环境变量创建
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
        if (apiKey != null && !apiKey.isEmpty()) {
            available = true;
            log.info("✅ Vision LLM 可用 (模型: {})", model);
        } else {
            available = false;
            log.warn("⚠️  Vision LLM 不可用: 未配置 API Key");
            log.warn("💡 提示: 设置环境变量 VISION_LLM_API_KEY 或 OPENAI_API_KEY");
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
     * 调用 Vision LLM API
     */
    private String callVisionAPI(String base64Image, String imageName) throws Exception {
        // 构建请求体
        String requestBody = buildVisionRequest(base64Image);

        log.debug("发送 Vision API 请求: {}", model);

        // 创建 HTTP 请求
        Request request = new Request.Builder()
            .url(apiEndpoint)
            .post(RequestBody.create(requestBody, JSON))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        // 发送请求
        long startTime = System.currentTimeMillis();
        try (Response response = httpClient.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - startTime;

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("Vision API 错误: HTTP {}, Body: {}", response.code(), errorBody);
                throw new Exception("Vision API 错误: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("收到 Vision API 响应，耗时: {}ms", elapsed);

            // 解析响应
            return parseVisionResponse(responseBody);
        }
    }

    /**
     * 构建 Vision API 请求体
     */
    private String buildVisionRequest(String base64Image) throws Exception {
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
     * 解析 Vision API 响应
     */
    private String parseVisionResponse(String responseBody) throws Exception {
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

        throw new Exception("无法解析 Vision API 响应: " + responseBody);
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

