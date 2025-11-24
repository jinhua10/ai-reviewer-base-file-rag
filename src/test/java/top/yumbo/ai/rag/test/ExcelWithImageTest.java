package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;

import java.io.File;

/**
 * Excel 图片处理测试
 * 测试包含图片的 Excel 文件是否能正常解析
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class ExcelWithImageTest {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info("📊 Excel 图片处理测试");
        log.info("=".repeat(80));
        log.info("");

        // 测试文件路径
        String testFilePath = "E:\\excel\\l0810.xls"; // 使用你的实际 Excel 文件
        File testFile = new File(testFilePath);

        if (!testFile.exists()) {
            log.error("❌ 测试文件不存在: {}", testFilePath);
            log.info("💡 请修改 testFilePath 为实际的 Excel 文件路径");
            return;
        }

        log.info("📁 测试文件: {}", testFile.getAbsolutePath());
        log.info("📏 文件大小: {} KB", testFile.length() / 1024);
        log.info("");

        // 测试 1: 默认配置解析
        log.info("📋 测试 1: 默认配置解析");
        log.info("-".repeat(80));
        testParse(testFile, "默认配置");

        // 测试 2: 禁用图片元数据提取
        log.info("");
        log.info("📋 测试 2: 禁用图片元数据提取");
        log.info("-".repeat(80));
        TikaDocumentParser parser2 = new TikaDocumentParser(
            10 * 1024 * 1024,  // 10MB
            false,              // 不提取图片元数据
            false               // 不包含图片占位符
        );
        testParseWithParser(testFile, parser2, "禁用图片处理");

        // 测试 3: 启用图片元数据提取
        log.info("");
        log.info("📋 测试 3: 启用图片元数据提取");
        log.info("-".repeat(80));
        TikaDocumentParser parser3 = new TikaDocumentParser(
            10 * 1024 * 1024,  // 10MB
            true,               // 提取图片元数据
            true                // 包含图片占位符
        );
        testParseWithParser(testFile, parser3, "启用图片处理");

        log.info("");
        log.info("=".repeat(80));
        log.info("✅ 测试完成");
        log.info("=".repeat(80));
    }

    private static void testParse(File file, String configName) {
        TikaDocumentParser parser = new TikaDocumentParser();
        testParseWithParser(file, parser, configName);
    }

    private static void testParseWithParser(File file, TikaDocumentParser parser, String configName) {
        try {
            log.info("⏳ 开始解析...");
            long startTime = System.currentTimeMillis();

            String content = parser.parse(file);
            long endTime = System.currentTimeMillis();

            log.info("✅ 解析成功！");
            log.info("   - 配置: {}", configName);
            log.info("   - 内容长度: {} 字符", content.length());
            log.info("   - 耗时: {} ms", endTime - startTime);

            // 检查内容是否为空
            if (content == null || content.trim().isEmpty()) {
                log.warn("⚠️ 警告: 解析内容为空！");
                log.info("   可能原因:");
                log.info("     1. Excel 文件是空的");
                log.info("     2. Excel 文件格式不支持");
                log.info("     3. 文件包含图片导致解析失败");
                log.info("     4. Apache POI/Tika 依赖问题");
            } else {
                // 显示内容预览
                String preview = content.length() > 200
                    ? content.substring(0, 200) + "..."
                    : content;
                log.info("   - 内容预览: {}", preview.replace("\n", " ").replace("\t", " "));

                // 检查是否包含图片相关信息
                if (content.contains("图片") || content.contains("嵌入资源") || content.contains("[Image")) {
                    log.info("   📷 检测到图片相关信息");
                }
            }

        } catch (Exception e) {
            log.error("❌ 解析失败: {}", e.getMessage());
            log.error("   异常类型: {}", e.getClass().getSimpleName());

            if (e.getCause() != null) {
                log.error("   根本原因: {}", e.getCause().getMessage());
            }

            // 检查是否是图片导致的问题
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("image") || errorMsg.contains("picture") || errorMsg.contains("graphic")) {
                log.error("   💡 可能是图片导致的问题！");
            }

            log.debug("完整堆栈:", e);
        }
    }
}

