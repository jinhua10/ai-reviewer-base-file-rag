package top.yumbo.ai.rag.impl.parser.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import top.yumbo.ai.rag.spring.boot.llm.OpenAILLMClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMClient Vision 策略单元测试
 * 验证通过主 LLM 客户端（OpenAILLMClient）进行图片识别的功能
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
class LLMClientVisionStrategyTest {

    /**
     * 测试使用 OpenAILLMClient 进行图片识别
     * 需要设置环境变量 QW_API_KEY 或 AI_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testLLMClientVisionWithQianwen() throws Exception {
        System.out.println("\n=== 测试 LLMClient Vision 策略（千问模型） ===");

        // 创建 OpenAI LLM 客户端（配置为千问）
        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";
        String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        OpenAILLMClient llmClient = new OpenAILLMClient(apiKey, model, apiUrl);

        // 验证客户端可用且支持图片
        assertTrue(llmClient.isAvailable(), "LLM 客户端应该可用");
        assertTrue(llmClient.supportsImageInput(), "模型应该支持图片输入");

        System.out.println("✅ LLM 客户端创建成功");
        System.out.println("   - 模型: " + llmClient.getModelName());
        System.out.println("   - 支持图片: " + llmClient.supportsImageInput());

        // 创建 LLMClient Vision 策略
        LLMClientVisionStrategy strategy = new LLMClientVisionStrategy(llmClient);

        // 验证策略可用
        assertTrue(strategy.isAvailable(), "LLM Vision 策略应该可用");
        System.out.println("✅ LLM Vision 策略创建成功: " + strategy.getStrategyName());

        // 测试图片
        String testImagePath = "E:\\excel1\\1.jpg";
        Path imagePath = Paths.get(testImagePath);

        if (!Files.exists(imagePath)) {
            System.out.println("⚠️  测试图片不存在: " + testImagePath);
            System.out.println("💡 跳过实际图片识别测试");
            return;
        }

        // 测试图片提取
        System.out.println("\n=== 提取图片内容 ===");
        File imageFile = imagePath.toFile();
        String result = strategy.extractContent(imageFile);

        // 验证结果
        assertNotNull(result, "提取结果不应为空");
        assertFalse(result.isEmpty(), "提取结果不应为空字符串");
        assertFalse(result.contains("不可用"), "LLM Vision 应该可用");
        assertFalse(result.contains("失败"), "处理不应失败");

        System.out.println("✅ 提取成功！");
        System.out.println("📄 提取内容（前300字符）:");
        System.out.println(result.substring(0, Math.min(300, result.length())));
        if (result.length() > 300) {
            System.out.println("...");
        }
        System.out.println("📊 总字符数: " + result.length());
    }

    /**
     * 测试与 SmartImageExtractor 的集成
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testIntegrationWithSmartExtractor() {
        System.out.println("\n=== 测试与 SmartImageExtractor 的集成 ===");

        // 创建 LLM 客户端
        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";
        String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        OpenAILLMClient llmClient = new OpenAILLMClient(apiKey, model, apiUrl);

        // 创建智能提取器并添加 LLMClient Vision 策略
        SmartImageExtractor extractor = new SmartImageExtractor();
        LLMClientVisionStrategy visionStrategy = new LLMClientVisionStrategy(llmClient);
        extractor.addStrategy(visionStrategy);

        // 验证策略已激活
        assertNotNull(extractor.getActiveStrategy(), "应该有激活的策略");
        assertTrue(extractor.getActiveStrategy() instanceof LLMClientVisionStrategy,
                   "激活的策略应该是 LLMClientVisionStrategy");

        System.out.println("✅ SmartImageExtractor 成功集成 LLMClient Vision");
        System.out.println("📌 当前激活策略: " + extractor.getActiveStrategy().getStrategyName());

        // 测试图片
        String testImagePath = "E:\\excel1\\1.jpg";
        Path imagePath = Paths.get(testImagePath);

        if (!Files.exists(imagePath)) {
            System.out.println("⚠️  测试图片不存在，跳过实际提取测试");
            return;
        }

        System.out.println("\n=== 通过 SmartImageExtractor 提取图片 ===");
        File imageFile = imagePath.toFile();
        String result = extractor.extractContent(imageFile);

        assertNotNull(result, "提取结果不应为空");
        System.out.println("✅ 提取成功！");
        System.out.println("📄 提取内容（前200字符）:");
        System.out.println(result.substring(0, Math.min(200, result.length())));
        System.out.println("📊 总字符数: " + result.length());
    }

    /**
     * 测试混合模式：LLMClient Vision + OCR
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testHybridModeWithLLMClient() {
        System.out.println("\n=== 测试混合模式（LLMClient Vision + OCR） ===");

        // 创建 LLM 客户端
        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";
        String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        OpenAILLMClient llmClient = new OpenAILLMClient(apiKey, model, apiUrl);

        // 创建混合模式提取器
        SmartImageExtractor extractor = new SmartImageExtractor();

        // 添加 LLMClient Vision 策略
        extractor.addStrategy(new LLMClientVisionStrategy(llmClient));


        // 验证策略
        assertTrue(extractor.getStrategies().size() >= 1, "应该至少有一个策略");

        System.out.println("✅ 混合模式提取器创建成功");
        System.out.println("📌 策略列表:");
        for (int i = 0; i < extractor.getStrategies().size(); i++) {
            ImageContentExtractorStrategy strategy = extractor.getStrategies().get(i);
            System.out.println("   " + (i + 1) + ". " + strategy.getStrategyName() +
                             " (可用: " + strategy.isAvailable() + ")");
        }

        if (extractor.getActiveStrategy() != null) {
            System.out.println("📌 当前激活策略: " + extractor.getActiveStrategy().getStrategyName());

            // 注意：SmartImageExtractor 按添加顺序选择第一个可用的策略
            // 如果 OCR 和 LLM Vision 都可用，会选择第一个添加的
            assertTrue(extractor.getActiveStrategy().isAvailable(),
                       "激活的策略应该是可用的");
        }
    }

    /**
     * 测试不支持图片的 LLM 客户端
     */
    @Test
    void testUnsupportedLLMClient() {
        System.out.println("\n=== 测试不支持图片的 LLM 客户端 ===");

        // 创建一个不支持图片的 LLM 客户端（如 deepseek-chat）
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️  未设置 AI_API_KEY，跳过测试");
            return;
        }

        String model = "deepseek-chat";  // 不支持图片
        String apiUrl = "https://api.deepseek.com/v1/chat/completions";
        OpenAILLMClient llmClient = new OpenAILLMClient(apiKey, model, apiUrl);

        System.out.println("📌 LLM 客户端信息:");
        System.out.println("   - 模型: " + llmClient.getModelName());
        System.out.println("   - 支持图片: " + llmClient.supportsImageInput());

        // 创建策略
        LLMClientVisionStrategy strategy = new LLMClientVisionStrategy(llmClient);

        // 应该不可用
        assertFalse(strategy.isAvailable(), "不支持图片的模型应该不可用");
        System.out.println("✅ 正确识别：该模型不支持图片");

        // 尝试提取应该返回错误信息
        String result = strategy.extractContent(new File("test.jpg"));
        assertTrue(result.contains("不可用"), "应该提示 LLM Vision 不可用");
        System.out.println("📄 返回信息: " + result);
    }

    /**
     * 对比测试：VisionLLMStrategy vs LLMClientVisionStrategy
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testCompareStrategies() {
        System.out.println("\n=== 对比测试：VisionLLMStrategy vs LLMClientVisionStrategy ===");

        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";
        String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // 方式1：独立的 VisionLLMStrategy
        VisionLLMStrategy visionStrategy = new VisionLLMStrategy(apiKey, model, endpoint);
        System.out.println("\n方式1 - VisionLLMStrategy:");
        System.out.println("   - 策略名: " + visionStrategy.getStrategyName());
        System.out.println("   - 可用: " + visionStrategy.isAvailable());
        System.out.println("   - 特点: 独立配置，需要单独的 API Key");

        // 方式2：基于 LLMClient 的策略
        OpenAILLMClient llmClient = new OpenAILLMClient(apiKey, model, endpoint);
        LLMClientVisionStrategy llmVisionStrategy = new LLMClientVisionStrategy(llmClient);
        System.out.println("\n方式2 - LLMClientVisionStrategy:");
        System.out.println("   - 策略名: " + llmVisionStrategy.getStrategyName());
        System.out.println("   - 可用: " + llmVisionStrategy.isAvailable());
        System.out.println("   - 特点: 复用主 LLM 配置，统一管理");

        System.out.println("\n💡 推荐：");
        System.out.println("   - 如果主 LLM 支持图片，建议使用 LLMClientVisionStrategy");
        System.out.println("   - 可以避免重复配置，统一管理 API Key 和模型");
        System.out.println("   - 在 application.yml 中设置 strategy: llm-vision");
    }
}

