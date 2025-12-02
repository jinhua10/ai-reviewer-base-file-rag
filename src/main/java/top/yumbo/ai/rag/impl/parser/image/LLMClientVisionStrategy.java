package top.yumbo.ai.rag.impl.parser.image;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;

/**
 * 基于 LLMClient 的 Vision 策略
 * <p>
 * 复用主 LLM 客户端（如 OpenAILLMClient）来处理图片识别
 * 这样可以统一配置、统一管理、避免重复代码
 *
 * <p>
 * 优势：
 * - 复用 OpenAILLMClient 的配置（API Key、模型、端点等）
 * - 统一的错误处理和日志记录
 * - 支持所有 LLMClient 支持的视觉模型
 * - 更好的可维护性
 *
 * <p>
 * 支持的模型：
 * - OpenAI: gpt-4o, gpt-4-vision-preview
 * - Qianwen: qwen-vl-plus, qwen-vl-max
 * - Claude: claude-3-opus, claude-3-sonnet
 * - 其他实现了 generateWithImage() 的 LLMClient
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Slf4j
public class LLMClientVisionStrategy implements ImageContentExtractorStrategy {

    private final LLMClient llmClient;
    private final String strategyName;
    private boolean available = false;

    /**
     * 构造函数
     *
     * @param llmClient LLM 客户端（必须支持图片输入）
     */
    public LLMClientVisionStrategy(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.strategyName = "LLM Vision (" + llmClient.getModelName() + ")";
        checkAvailability();
    }

    /**
     * 检查可用性
     */
    private void checkAvailability() {
        if (llmClient == null) {
            available = false;
            log.warn("⚠️  LLM Vision 不可用: LLMClient 为空");
            return;
        }

        if (!llmClient.isAvailable()) {
            available = false;
            log.warn("⚠️  LLM Vision 不可用: LLMClient 不可用");
            return;
        }

        if (!llmClient.supportsImageInput()) {
            available = false;
            log.warn("⚠️  LLM Vision 不可用: 模型 {} 不支持图片输入", llmClient.getModelName());
            return;
        }

        available = true;
        log.info("✅ LLM Vision 可用");
        log.info("   - 模型: {}", llmClient.getModelName());
        log.info("   - 客户端: {}", llmClient.getClass().getSimpleName());
    }

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        if (!available) {
            return String.format("[图片: %s - LLM Vision不可用]", imageName);
        }

        try {
            log.debug("使用 LLM Vision 处理图片: {}", imageName);

            // 1. 读取图片并转为 base64
            byte[] imageBytes = imageStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            // 2. 构建提示词
            String prompt = buildPrompt();

            // 3. 调用 LLM 的图片识别功能
            String result = llmClient.generateWithImage(prompt, imageUrl, null);

            log.info("LLM Vision 提取内容 [{}]: {} 字符", imageName, result.length());
            return result;

        } catch (Exception e) {
            log.error("LLM Vision 处理失败: {}", imageName, e);
            return String.format("[图片: %s - LLM Vision处理失败: %s]", imageName, e.getMessage());
        }
    }

    @Override
    public String extractContent(File imageFile) {
        if (!available) {
            return String.format("[图片: %s - LLM Vision不可用]", imageFile.getName());
        }

        try {
            log.debug("使用 LLM Vision 处理图片文件: {}", imageFile.getName());

            // 读取文件并转为 base64
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            // 构建提示词
            String prompt = buildPrompt();

            // 调用 LLM 的图片识别功能
            String result = llmClient.generateWithImage(prompt, imageUrl, null);

            log.info("LLM Vision 提取内容 [{}]: {} 字符", imageFile.getName(), result.length());
            return result;

        } catch (Exception e) {
            log.error("LLM Vision 处理失败: {}", imageFile.getName(), e);
            return String.format("[图片: %s - LLM Vision处理失败: %s]", imageFile.getName(), e.getMessage());
        }
    }

    /**
     * 构建图片识别提示词
     */
    private String buildPrompt() {
        return "请识别并提取这张图片中的所有文字内容。" +
               "如果图片包含表格、图表或其他结构化数据，请详细描述。" +
               "直接返回识别的内容，不需要额外的解释。";
    }

    @Override
    public String getStrategyName() {
        return strategyName;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取使用的 LLM 客户端
     */
    public LLMClient getLlmClient() {
        return llmClient;
    }
}

