package top.yumbo.ai.rag.impl.parser.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vision LLM 策略单元测试
 * 用于验证 Vision LLM（如千问VL模型）的图片识别功能是否有效
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
class VisionLLMStrategyTest {

    /**
     * 测试 Vision LLM 基本功能
     * 需要设置环境变量 QW_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testVisionLLMWithQianwenModel() throws Exception {
        // 从环境变量读取配置
        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";  // 千问VL Plus模型
        String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // 创建 Vision LLM 策略
        VisionLLMStrategy strategy = new VisionLLMStrategy(apiKey, model, endpoint);

        // 验证策略可用
        assertTrue(strategy.isAvailable(), "Vision LLM 策略应该可用");
        assertEquals("Vision LLM (qwen-vl-plus)", strategy.getStrategyName());

        // 测试图片文件路径（可以根据实际情况修改）
        String testImagePath = "E:\\excel1\\1.jpg";
        Path imagePath = Paths.get(testImagePath);

        // 如果测试图片不存在，跳过测试
        if (!Files.exists(imagePath)) {
            System.out.println("⚠️  测试图片不存在: " + testImagePath);
            System.out.println("💡 请修改测试图片路径或放置测试图片到该路径");
            return;
        }

        // 方式1：使用文件路径提取
        System.out.println("\n=== 测试方式1：从文件路径提取 ===");
        File imageFile = imagePath.toFile();
        String result1 = strategy.extractContent(imageFile);

        assertNotNull(result1, "提取结果不应为空");
        assertFalse(result1.isEmpty(), "提取结果不应为空字符串");
        assertFalse(result1.contains("Vision LLM不可用"), "Vision LLM应该可用");
        assertFalse(result1.contains("处理失败"), "处理不应失败");

        System.out.println("✅ 提取成功！");
        System.out.println("📄 提取内容（前200字符）:");
        System.out.println(result1.substring(0, Math.min(200, result1.length())));
        System.out.println("...");
        System.out.println("📊 总字符数: " + result1.length());

        // 方式2：使用输入流提取
        System.out.println("\n=== 测试方式2：从输入流提取 ===");
        try (InputStream imageStream = new FileInputStream(imageFile)) {
            String result2 = strategy.extractContent(imageStream, imageFile.getName());

            assertNotNull(result2, "提取结果不应为空");
            assertFalse(result2.isEmpty(), "提取结果不应为空字符串");

            System.out.println("✅ 提取成功！");
            System.out.println("📄 提取内容（前200字符）:");
            System.out.println(result2.substring(0, Math.min(200, result2.length())));
            System.out.println("...");
            System.out.println("📊 总字符数: " + result2.length());
        }
    }

    /**
     * 测试 Vision LLM 在 SmartImageExtractor 中的集成
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testVisionLLMInSmartExtractor() throws Exception {
        String apiKey = System.getenv("QW_API_KEY");
        String model = "qwen-vl-plus";
        String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        // 创建使用 Vision LLM 的智能提取器
        SmartImageExtractor extractor = SmartImageExtractor.withVisionLLM(apiKey, model, endpoint);

        // 验证策略已添加
        assertNotNull(extractor.getActiveStrategy(), "应该有激活的策略");
        assertTrue(extractor.getActiveStrategy() instanceof VisionLLMStrategy,
                   "激活的策略应该是 VisionLLMStrategy");

        System.out.println("✅ SmartImageExtractor 成功集成 Vision LLM");
        System.out.println("📌 当前激活策略: " + extractor.getActiveStrategy().getStrategyName());

        // 测试图片提取
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
        assertFalse(result.isEmpty(), "提取结果不应为空字符串");

        System.out.println("✅ 提取成功！");
        System.out.println("📄 提取内容（前200字符）:");
        System.out.println(result.substring(0, Math.min(200, result.length())));
        System.out.println("📊 总字符数: " + result.length());
    }

    /**
     * 测试混合模式（Vision LLM + OCR）
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "QW_API_KEY", matches = ".+")
    void testHybridMode() {
        String apiKey = System.getenv("QW_API_KEY");

        // 创建混合模式提取器（会优先使用 Vision LLM）
        SmartImageExtractor extractor = SmartImageExtractor.withHybrid(apiKey);

        // 验证有多个策略
        assertTrue(extractor.getStrategies().size() >= 1, "应该至少有一个策略");

        System.out.println("✅ 混合模式提取器创建成功");
        System.out.println("📌 策略数量: " + extractor.getStrategies().size());
        for (int i = 0; i < extractor.getStrategies().size(); i++) {
            ImageContentExtractorStrategy strategy = extractor.getStrategies().get(i);
            System.out.println("   " + (i + 1) + ". " + strategy.getStrategyName() +
                             " (可用: " + strategy.isAvailable() + ")");
        }

        if (extractor.getActiveStrategy() != null) {
            System.out.println("📌 当前激活策略: " + extractor.getActiveStrategy().getStrategyName());
        }
    }

    /**
     * 测试 Vision LLM 不可用的情况
     */
    @Test
    void testVisionLLMUnavailable() {
        // 使用空 API Key 创建策略
        VisionLLMStrategy strategy = new VisionLLMStrategy(null, null, null);

        // 应该不可用
        assertFalse(strategy.isAvailable(), "没有 API Key 时应该不可用");

        // 尝试提取应该返回错误信息
        String result = strategy.extractContent(new File("test.jpg"));
        assertTrue(result.contains("Vision LLM不可用"), "应该提示 Vision LLM 不可用");

        System.out.println("✅ Vision LLM 不可用测试通过");
        System.out.println("📄 返回信息: " + result);
    }

    /**
     * 测试配置信息输出
     */
    @Test
    void testConfigurationInfo() {
        System.out.println("\n=== Vision LLM 配置信息 ===");
        System.out.println("🔧 环境变量检查:");

        String qwApiKey = System.getenv("QW_API_KEY");
        System.out.println("   QW_API_KEY: " + (qwApiKey != null ? "✅ 已设置" : "❌ 未设置"));

        String visionApiKey = System.getenv("VISION_LLM_API_KEY");
        System.out.println("   VISION_LLM_API_KEY: " + (visionApiKey != null ? "✅ 已设置" : "❌ 未设置"));

        String aiApiKey = System.getenv("AI_API_KEY");
        System.out.println("   AI_API_KEY: " + (aiApiKey != null ? "✅ 已设置" : "❌ 未设置"));

        System.out.println("\n💡 配置建议:");
        if (qwApiKey == null && visionApiKey == null && aiApiKey == null) {
            System.out.println("   ⚠️  请设置以下环境变量之一来启用 Vision LLM:");
            System.out.println("      export QW_API_KEY=your-qianwen-api-key");
            System.out.println("      或");
            System.out.println("      export VISION_LLM_API_KEY=your-api-key");
        } else {
            System.out.println("   ✅ 环境变量配置正常");
        }

        System.out.println("\n📋 application.yml 配置示例:");
        System.out.println("   image-processing:");
        System.out.println("     vision-llm:");
        System.out.println("       enabled: true");
        System.out.println("       api-key: ${QW_API_KEY:}");
        System.out.println("       model: qwen-vl-plus");
        System.out.println("       endpoint: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
    }
}

