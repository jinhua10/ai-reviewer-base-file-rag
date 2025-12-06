package top.yumbo.ai.rag.impl.parser.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import top.yumbo.ai.rag.i18n.I18N;

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
     * 图片文本提取模式（Image text extraction mode）
     */
    public enum ExtractionMode {
        /** 精简模式：只提取关键信息，节省 token（Concise mode: key info only, save tokens） */
        CONCISE("concise"),
        /** 详细模式：完整分析图片内容（Detailed mode: full analysis） */
        DETAILED("detailed");

        private final String value;

        ExtractionMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ExtractionMode fromString(String value) {
            if (value == null) return CONCISE;
            for (ExtractionMode mode : values()) {
                if (mode.value.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return CONCISE;
        }
    }

    // 当前提取模式（默认精简）
    private static ExtractionMode currentExtractionMode = ExtractionMode.CONCISE;

    /**
     * 设置全局提取模式（Set global extraction mode）
     */
    public static void setExtractionMode(ExtractionMode mode) {
        currentExtractionMode = mode != null ? mode : ExtractionMode.CONCISE;
        log.info(I18N.get("log.imageproc.extraction_mode", currentExtractionMode.value));
    }

    /**
     * 设置全局提取模式（从字符串）（Set global extraction mode from string）
     */
    public static void setExtractionMode(String mode) {
        setExtractionMode(ExtractionMode.fromString(mode));
    }

    /**
     * 获取当前使用的提示词（Get current prompt based on extraction mode）
     */
    private String getExtractionPrompt() {
        String promptKey = currentExtractionMode == ExtractionMode.CONCISE
                ? "vision_llm.prompt.extract_text_concise"
                : "vision_llm.prompt.extract_text";

        String prompt = I18N.get(promptKey);

        // 如果精简版提示词不存在，回退到详细版（Fallback to detailed if concise not found）
        if ((prompt == null || prompt.isEmpty() || prompt.equals(promptKey))
                && currentExtractionMode == ExtractionMode.CONCISE) {
            prompt = I18N.get("vision_llm.prompt.extract_text");
        }

        return prompt;
    }

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
            log.debug(I18N.get("vision_llm.log.api_format_detected", "Ollama"));
            return ApiFormat.OLLAMA;
        }

        // 检测 OpenAI Chat Completions 格式
        if (lowerEndpoint.contains("/chat/completions") || lowerEndpoint.contains("/v1/")) {
            log.debug(I18N.get("vision_llm.log.api_format_detected", "OpenAI Chat Completions"));
            return ApiFormat.OPENAI_CHAT;
        }

        // 默认使用 OpenAI Chat Completions 格式（最通用）
        log.debug(I18N.get("vision_llm.log.api_format_default"));
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
                    log.warn(I18N.get("vision_llm.log.unavailable_no_apikey"));
                    log.warn(I18N.get("vision_llm.log.hint_set_apikey"));
                    return;
                }
                // 不实际测试连接（避免额外费用），假定配置正确
                available = true;
            }

            if (available) {
                log.info(I18N.get("vision_llm.log.service_available"));
                log.info(I18N.get("vision_llm.log.api_format", apiFormat));
                log.info(I18N.get("vision_llm.log.model", model));
                log.info(I18N.get("vision_llm.log.endpoint", apiEndpoint));
            }

        } catch (Exception e) {
            available = false;
            log.warn(I18N.get("vision_llm.log.service_unavailable", e.getMessage()));
            log.warn(I18N.get("vision_llm.log.check_service"));
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
                log.warn(I18N.get("vision_llm.log.service_response_error", response.code()));
            }
        }
    }

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        if (!available) {
            return I18N.get("vision_llm.error.unavailable", imageName);
        }

        // 检查图片格式是否支持
        if (!isSupportedImageFormat(imageName)) {
            log.warn(I18N.get("vision_llm.log.unsupported_format", imageName, getFileExtension(imageName)));
            return I18N.get("vision_llm.error.unsupported_format", imageName, getFileExtension(imageName));
        }

        try {
            log.debug(I18N.get("vision_llm.log.processing_image", imageName));

            // 1. 读取图片并转为 base64
            byte[] imageBytes = imageStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 2. 调用 Vision API
            String result = callVisionAPI(base64Image, imageName);

            log.info(I18N.get("vision_llm.log.content_extracted", imageName, result.length()));
            return result;

        } catch (Exception e) {
            log.error(I18N.get("vision_llm.log.processing_failed", imageName), e);
            return I18N.get("vision_llm.error.processing_failed", imageName, e.getMessage());
        }
    }

    @Override
    public String extractContent(File imageFile) {
        if (!available) {
            return I18N.get("vision_llm.error.unavailable", imageFile.getName());
        }

        // 检查图片格式是否支持
        if (!isSupportedImageFormat(imageFile.getName())) {
            log.warn(I18N.get("vision_llm.log.unsupported_format", imageFile.getName(), getFileExtension(imageFile.getName())));
            return I18N.get("vision_llm.error.unsupported_format", imageFile.getName(), getFileExtension(imageFile.getName()));
        }

        try {
            log.debug(I18N.get("vision_llm.log.processing_image_file", imageFile.getName()));

            // 读取文件并转为 base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 调用 Vision API
            String result = callVisionAPI(base64Image, imageFile.getName());

            log.info(I18N.get("vision_llm.log.content_extracted", imageFile.getName(), result.length()));
            return result;

        } catch (Exception e) {
            log.error(I18N.get("vision_llm.log.processing_failed", imageFile.getName()), e);
            return I18N.get("vision_llm.error.processing_failed", imageFile.getName(), e.getMessage());
        }
    }

    /**
     * 批量提取多张图片内容（Batch extract content from multiple images）
     * 适用于 PPT 等场景，一次处理多张图片以减少 API 调用次数
     *
     * @param imageDataList 图片数据列表（byte[]）
     * @param imageNames 图片名称列表
     * @return 提取的内容
     */
    public String extractContentBatch(java.util.List<byte[]> imageDataList, java.util.List<String> imageNames) {
        if (!available) {
            return I18N.get("vision_llm.error.unavailable", "batch images");
        }

        if (imageDataList == null || imageDataList.isEmpty()) {
            return "";
        }

        // 过滤掉不支持的格式
        java.util.List<byte[]> validImages = new java.util.ArrayList<>();
        java.util.List<String> validNames = new java.util.ArrayList<>();

        for (int i = 0; i < imageDataList.size(); i++) {
            String imageName = i < imageNames.size() ? imageNames.get(i) : "image_" + i;
            if (isSupportedImageFormat(imageName)) {
                validImages.add(imageDataList.get(i));
                validNames.add(imageName);
            } else {
                log.warn(I18N.get("vision_llm.log.unsupported_format",
                    imageName, getFileExtension(imageName)));
            }
        }

        if (validImages.isEmpty()) {
            return I18N.get("vision_llm.error.no_valid_images");
        }

        try {
            log.info("📦 批量处理 {} 张图片: {}", validImages.size(), String.join(", ", validNames));

            // 转换为 base64
            java.util.List<String> base64Images = new java.util.ArrayList<>();
            for (byte[] imageData : validImages) {
                String base64Image = Base64.getEncoder().encodeToString(imageData);
                base64Images.add(base64Image);
            }

            // 调用批量 Vision API
            String result = callVisionAPIBatch(base64Images, validNames);

            log.info("✅ 批量提取完成: {} 张图片 -> {} 字符", validImages.size(), result.length());
            return result;

        } catch (Exception e) {
            log.error("批量 Vision LLM 处理失败", e);
            return I18N.get("vision_llm.error.batch_processing_failed",
                validImages.size(), e.getMessage());
        }
    }

    /**
     * 调用 Vision LLM API（Call Vision LLM API）- 单张图片
     */
    private String callVisionAPI(String base64Image, String imageName) throws Exception {
        // 根据 API 格式构建不同的请求体
        String requestBody = buildVisionRequest(base64Image);

        log.debug(I18N.get("vision_llm.log.sending_request", model, apiFormat));

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
                String errorBody = response.body() != null ? response.body().string() :
                    I18N.get("vision_llm.error.no_response_body");
                log.error(I18N.get("vision_llm.error.api_error_with_body",
                    response.code(), errorBody));
                throw new Exception(I18N.get("vision_llm.error.api_error", response.code()));
            }

            String responseBody = response.body().string();
            log.debug(I18N.get("vision_llm.log.received_response", elapsed));

            // 根据 API 格式解析响应
            return parseVisionResponse(responseBody);
        }
    }

    /**
     * 调用 Vision LLM API 处理多张图片（Call Vision LLM API with multiple images）
     */
    private String callVisionAPIBatch(java.util.List<String> base64Images, java.util.List<String> imageNames) throws Exception {
        // 根据 API 格式构建不同的请求体
        String requestBody = buildVisionRequest(base64Images);

        log.debug(I18N.get("vision_llm.log.sending_request_batch", model, apiFormat));

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
                String errorBody = response.body() != null ? response.body().string() :
                    I18N.get("vision_llm.error.no_response_body");
                log.error(I18N.get("vision_llm.error.api_error_with_body",
                    response.code(), errorBody));
                throw new Exception(I18N.get("vision_llm.error.api_error", response.code()));
            }

            String responseBody = response.body().string();
            log.debug(I18N.get("vision_llm.log.received_response", elapsed));

            // 根据 API 格式解析响应
            return parseVisionResponse(responseBody);
        }
    }

    /**
     * 批量提取多张图片内容 - 带位置信息（Batch extract content with position info）
     * 适用于 PPT 等场景，保留图片的位置关系有助于理解架构图、流程图
     *
     * @param imagePositions 图片位置信息列表
     * @return 提取的内容
     */
    public String extractContentBatchWithPosition(java.util.List<ImagePositionInfo> imagePositions) {
        if (!available) {
            return I18N.get("vision_llm.error.unavailable", "batch images");
        }

        if (imagePositions == null || imagePositions.isEmpty()) {
            return "";
        }

        // 过滤掉不支持的格式
        java.util.List<ImagePositionInfo> validImages = new java.util.ArrayList<>();

        for (ImagePositionInfo imgPos : imagePositions) {
            if (isSupportedImageFormat(imgPos.getImageName())) {
                validImages.add(imgPos);
            } else {
                log.warn(I18N.get("vision_llm.log.unsupported_format",
                    imgPos.getImageName(), getFileExtension(imgPos.getImageName())));
            }
        }

        if (validImages.isEmpty()) {
            return I18N.get("vision_llm.error.no_valid_images");
        }

        try {
            log.info("📦 批量处理 {} 张图片（含位置信息）", validImages.size());

            // 构建位置信息描述
            StringBuilder positionDesc = new StringBuilder();
            positionDesc.append("幻灯片布局信息：\n");
            for (int i = 0; i < validImages.size(); i++) {
                ImagePositionInfo img = validImages.get(i);
                positionDesc.append("  ").append(img.getPositionDescription()).append("\n");

                // 如果有多张图片，描述它们的相对位置
                if (i > 0) {
                    String relation = ImagePositionInfo.getRelativePosition(validImages.get(i-1), img);
                    positionDesc.append("    -> 相对于图片").append(i).append("在").append(relation).append("\n");
                }
            }

            // 转换为 base64
            java.util.List<String> base64Images = new java.util.ArrayList<>();
            java.util.List<String> imageNames = new java.util.ArrayList<>();
            for (ImagePositionInfo imgPos : validImages) {
                String base64Image = Base64.getEncoder().encodeToString(imgPos.getImageData());
                base64Images.add(base64Image);
                imageNames.add(imgPos.getImageName());
            }

            // 调用批量 Vision API，传入位置信息
            String result = callVisionAPIBatchWithPosition(base64Images, imageNames, positionDesc.toString());

            log.info("✅ 批量提取完成（含位置信息）: {} 张图片 -> {} 字符", validImages.size(), result.length());
            return result;

        } catch (Exception e) {
            log.error("批量 Vision LLM 处理失败（含位置信息）", e);
            return I18N.get("vision_llm.error.batch_processing_failed",
                validImages.size(), e.getMessage());
        }
    }

    /**
     * 调用 Vision LLM API 处理多张图片 - 带位置信息
     */
    private String callVisionAPIBatchWithPosition(java.util.List<String> base64Images,
                                                   java.util.List<String> imageNames,
                                                   String positionDescription) throws Exception {
        // 根据 API 格式构建不同的请求体
        String requestBody = buildVisionRequestWithPosition(base64Images, positionDescription);

        log.debug("发送批量 Vision API 请求（含位置信息）: {} (格式: {})", model, apiFormat);

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
                String errorBody = response.body() != null ? response.body().string() :
                    I18N.get("vision_llm.error.no_response_body");
                log.error(I18N.get("vision_llm.error.api_error_with_body",
                    response.code(), errorBody));
                throw new Exception(I18N.get("vision_llm.error.api_error", response.code()));
            }

            String responseBody = response.body().string();
            log.debug("接收到响应，耗时: {}ms", elapsed);

            // 根据 API 格式解析响应
            return parseVisionResponse(responseBody);
        }
    }

    /**
     * 构建 Vision API 请求体（Build Vision API request body）- 单张图片
     */
    private String buildVisionRequest(String base64Image) throws Exception {
        if (apiFormat == ApiFormat.OLLAMA) {
            return buildOllamaRequest(base64Image);
        } else {
            return buildOpenAIRequest(base64Image);
        }
    }

    /**
     * 构建 Vision API 请求体（Build Vision API request body）- 多张图片（批量）
     */
    private String buildVisionRequest(java.util.List<String> base64Images) throws Exception {
        if (apiFormat == ApiFormat.OLLAMA) {
            return buildOllamaRequestBatch(base64Images);
        } else {
            return buildOpenAIRequestBatch(base64Images);
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

        // 添加文本提示（使用配置的提取模式）
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        String prompt = getExtractionPrompt();
        textContent.put("text", prompt);
        log.debug("Prompt Text (mode={}): {}", currentExtractionMode.getValue(), prompt.substring(0, Math.min(100, prompt.length())));

        // 添加图片
        ObjectNode imageContent = content.addObject();
        imageContent.put("type", "image_url");
        ObjectNode imageUrl = imageContent.putObject("image_url");
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imageUrl.put("detail", "high"); // 使用高清模式以获得更好的 OCR 效果

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建 OpenAI Chat Completions 格式请求 - 多张图片（Build OpenAI Chat Completions format request - Batch）
     */
    private String buildOpenAIRequestBatch(java.util.List<String> base64Images) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 2000); // 多张图片需要更多 tokens

        // 构建 messages 数组
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        // 构建 content 数组（包含文本和多张图片）
        ArrayNode content = message.putArray("content");

        // 添加文本提示（针对多张图片优化，强调按顺序和位置关系）
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        String batchPrompt = String.format(
            "这是一张幻灯片中的 %d 张图片，它们在幻灯片上的排列顺序和相对位置很重要（特别是对于架构图、流程图等）。\n\n" +
            "请注意：\n" +
            "1. 这些图片原本在同一张幻灯片上，它们之间可能有连接关系、布局关系\n" +
            "2. 如果是架构图/流程图，请特别注意组件之间的位置、连接、层次关系\n" +
            "3. 按照图片出现的顺序（从左到右、从上到下）进行分析\n" +
            "4. 如果图片之间有关联，请在分析时说明它们的关系\n\n" +
            "%s",
            base64Images.size(),
            getExtractionPrompt()
        );
        textContent.put("text", batchPrompt);

        // 添加所有图片，并标注序号
        for (int i = 0; i < base64Images.size(); i++) {
            // 先添加图片序号说明
            if (i > 0) { // 第一张图片不需要分隔
                ObjectNode seqContent = content.addObject();
                seqContent.put("type", "text");
                seqContent.put("text", String.format("\n--- 图片 %d ---", i + 1));
            }

            // 添加图片
            ObjectNode imageContent = content.addObject();
            imageContent.put("type", "image_url");
            ObjectNode imageUrl = imageContent.putObject("image_url");
            imageUrl.put("url", "data:image/jpeg;base64," + base64Images.get(i));
            imageUrl.put("detail", "high");
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建 Ollama 格式请求（Build Ollama format request）
     */
    private String buildOllamaRequest(String base64Image) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        String promptText = getExtractionPrompt();

        // 检查端点类型，使用不同的请求格式
        if (apiEndpoint.contains("/api/chat")) {
            // /api/chat 格式：使用 messages 数组（类似 OpenAI，但图片格式不同）
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", promptText);

            // Ollama chat API 使用 images 数组存放 base64 图片
            ArrayNode images = message.putArray("images");
            images.add(base64Image);

            root.put("stream", false);  // 不使用流式输出
        } else {
            // /api/generate 格式：使用 prompt + images
            root.put("prompt", promptText);

            // Ollama 使用 images 数组存放 base64 图片
            ArrayNode images = root.putArray("images");
            images.add(base64Image);

            root.put("stream", false);  // 不使用流式输出
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建 Ollama 格式请求 - 多张图片（Build Ollama format request - Batch）
     */
    private String buildOllamaRequestBatch(java.util.List<String> base64Images) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        String batchPrompt = String.format(
            "这是一张幻灯片中的 %d 张图片，它们在幻灯片上的排列顺序和相对位置很重要（特别是对于架构图、流程图等）。\n\n" +
            "请注意：\n" +
            "1. 这些图片原本在同一张幻灯片上，它们之间可能有连接关系、布局关系\n" +
            "2. 如果是架构图/流程图，请特别注意组件之间的位置、连接、层次关系\n" +
            "3. 按照图片出现的顺序（从左到右、从上到下）进行分析\n" +
            "4. 如果图片之间有关联，请在分析时说明它们的关系\n\n" +
            "%s",
            base64Images.size(),
            getExtractionPrompt()
        );

        // 检查端点类型，使用不同的请求格式
        if (apiEndpoint.contains("/api/chat")) {
            // /api/chat 格式
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", batchPrompt);

            // Ollama chat API 使用 images 数组存放多张 base64 图片
            ArrayNode images = message.putArray("images");
            for (String base64Image : base64Images) {
                images.add(base64Image);
            }

            root.put("stream", false);
        } else {
            // /api/generate 格式
            root.put("prompt", batchPrompt);

            // Ollama 使用 images 数组存放多张 base64 图片
            ArrayNode images = root.putArray("images");
            for (String base64Image : base64Images) {
                images.add(base64Image);
            }

            root.put("stream", false);
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建包含位置信息的 Vision API 请求
     */
    private String buildVisionRequestWithPosition(java.util.List<String> base64Images,
                                                   String positionDescription) throws Exception {
        if (apiFormat == ApiFormat.OLLAMA) {
            return buildOllamaRequestWithPosition(base64Images, positionDescription);
        } else {
            return buildOpenAIRequestWithPosition(base64Images, positionDescription);
        }
    }

    /**
     * 构建 OpenAI 格式请求 - 带位置信息
     */
    private String buildOpenAIRequestWithPosition(java.util.List<String> base64Images,
                                                   String positionDescription) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 2000);

        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");

        ArrayNode content = message.putArray("content");

        // 添加文本提示（包含位置信息）
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        String enhancedPrompt = String.format(
            "这是一张幻灯片中的 %d 张图片。\n\n" +
            "%s\n" +
            "**重要**：这些图片的位置和布局关系已在上面列出，对于理解架构图、流程图非常关键。\n" +
            "请在分析时特别注意：\n" +
            "- 图片之间的空间位置关系（上下左右）\n" +
            "- 可能存在的连接线、箭头等关联\n" +
            "- 整体的布局结构和层次关系\n\n" +
            "%s",
            base64Images.size(),
            positionDescription,
            getExtractionPrompt()
        );
        textContent.put("text", enhancedPrompt);

        // 添加所有图片
        for (int i = 0; i < base64Images.size(); i++) {
            if (i > 0) {
                ObjectNode seqContent = content.addObject();
                seqContent.put("type", "text");
                seqContent.put("text", String.format("\n--- 图片 %d ---", i + 1));
            }

            ObjectNode imageContent = content.addObject();
            imageContent.put("type", "image_url");
            ObjectNode imageUrl = imageContent.putObject("image_url");
            imageUrl.put("url", "data:image/jpeg;base64," + base64Images.get(i));
            imageUrl.put("detail", "high");
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建 Ollama 格式请求 - 带位置信息
     */
    private String buildOllamaRequestWithPosition(java.util.List<String> base64Images,
                                                   String positionDescription) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        String enhancedPrompt = String.format(
            "这是一张幻灯片中的 %d 张图片。\n\n" +
            "%s\n" +
            "**重要**：这些图片的位置和布局关系已在上面列出，对于理解架构图、流程图非常关键。\n" +
            "请在分析时特别注意：\n" +
            "- 图片之间的空间位置关系（上下左右）\n" +
            "- 可能存在的连接线、箭头等关联\n" +
            "- 整体的布局结构和层次关系\n\n" +
            "%s",
            base64Images.size(),
            positionDescription,
            getExtractionPrompt()
        );

        if (apiEndpoint.contains("/api/chat")) {
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", enhancedPrompt);

            ArrayNode images = message.putArray("images");
            for (String base64Image : base64Images) {
                images.add(base64Image);
            }

            root.put("stream", false);
        } else {
            root.put("prompt", enhancedPrompt);

            ArrayNode images = root.putArray("images");
            for (String base64Image : base64Images) {
                images.add(base64Image);
            }

            root.put("stream", false);
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
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
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

                        log.debug(I18N.get("vision_llm.log.token_usage",
                            promptTokens, completionTokens, totalTokens));
                    }

                    return result;
                }
            }
        }

        throw new Exception(I18N.get("vision_llm.error.parse_openai_failed", responseBody));
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
                    log.debug(I18N.get("vision_llm.log.ollama_complete_chat"));
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
                log.debug(I18N.get("vision_llm.log.ollama_complete_generate"));
            }

            return result;
        }

        throw new Exception(I18N.get("vision_llm.error.parse_ollama_failed", responseBody));
    }

    /**
     * 检查图片格式是否被 Vision API 支持
     */
    private boolean isSupportedImageFormat(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        // Vision API 通常支持的格式：jpg, jpeg, png, gif, webp, bmp
        // 不支持的格式：wmf, emf, svg, tiff (某些API), ico 等
        return extension.equals("jpg") || extension.equals("jpeg") ||
               extension.equals("png") || extension.equals("gif") ||
               extension.equals("webp") || extension.equals("bmp");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
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

