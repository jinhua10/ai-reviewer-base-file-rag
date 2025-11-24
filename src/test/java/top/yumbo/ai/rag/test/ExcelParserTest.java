package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;

import java.io.File;

/**
 * Excel解析器测试
 * 用于诊断Excel文件解析问题
 */
@Slf4j
public class ExcelParserTest {

    public static void main(String[] args) {
        // 测试路径
        String testFilePath = "E:\\excel\\月度数据.xls";

        if (args.length > 0) {
            testFilePath = args[0];
        }

        log.info("=".repeat(80));
        log.info("Excel解析器诊断测试");
        log.info("=".repeat(80));
        log.info("测试文件: {}", testFilePath);

        File file = new File(testFilePath);

        // 检查文件
        log.info("\n📂 文件检查:");
        log.info("  - 文件存在: {}", file.exists());
        log.info("  - 是文件: {}", file.isFile());
        log.info("  - 可读: {}", file.canRead());
        log.info("  - 文件大小: {} bytes ({} KB)", file.length(), file.length() / 1024);
        log.info("  - 绝对路径: {}", file.getAbsolutePath());

        if (!file.exists()) {
            log.error("❌ 文件不存在！");
            log.info("\n💡 提示:");
            log.info("  1. 检查路径是否正确");
            log.info("  2. 检查文件是否真的存在");
            log.info("  3. 注意Windows路径使用双反斜杠 \\\\ 或单正斜杠 /");
            log.info("  4. 中文路径可能需要特殊处理");
            return;
        }

        // 解析测试
        log.info("\n⏳ 开始解析...");
        try {
            TikaDocumentParser parser = new TikaDocumentParser();
            String content = parser.parse(file);

            log.info("\n✅ 解析完成!");
            log.info("  - 内容长度: {} 字符", content.length());

            if (content.length() == 0) {
                log.warn("⚠️ 警告: 解析内容为空！");
                log.info("  可能原因:");
                log.info("    1. Excel文件是空的");
                log.info("    2. Excel文件格式不支持");
                log.info("    3. 文件损坏");
                log.info("    4. Apache POI依赖缺失");
            } else {
                log.info("\n📄 内容预览（前500字符）:");
                log.info("----------------------------------------");
                log.info(content.substring(0, Math.min(500, content.length())));
                log.info("----------------------------------------");
            }

        } catch (Exception e) {
            log.error("❌ 解析失败:", e);
            log.error("  异常类型: {}", e.getClass().getName());
            log.error("  异常消息: {}", e.getMessage());

            // 检查是否是依赖问题
            if (e.getMessage() != null && e.getMessage().contains("NoClassDefFoundError")) {
                log.error("\n💡 这可能是依赖缺失问题，请检查pom.xml中是否包含:");
                log.error("  - org.apache.tika");
                log.error("  - org.apache.poi (for Excel support)");
            }
        }

        log.info("\n" + "=".repeat(80));
    }
}

