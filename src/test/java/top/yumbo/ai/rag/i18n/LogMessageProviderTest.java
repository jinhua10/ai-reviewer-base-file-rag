package top.yumbo.ai.rag.i18n;

/**
 * 测试 LogMessageProvider 是否能正确加载 YAML 格式的国际化文件
 */
public class LogMessageProviderTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("测试 LogMessageProvider YAML 加载");
        System.out.println("Testing LogMessageProvider YAML Loading");
        System.out.println("========================================");
        System.out.println();

        // 测试中文
        System.setProperty("log.locale", "zh");
        testKey("banner.title", "Banner 标题");
        testKey("log.kqa.init_start", "知识库问答初始化");
        testKey("log.kb.found_files", "找到文件", 10);
        testKey("error.file.not_exists", "文件不存在错误", "/path/to/file");

        System.out.println();

        // 测试英文
        System.setProperty("log.locale", "en");
        testKey("banner.title", "Banner Title");
        testKey("log.kqa.init_start", "Knowledge QA Init");
        testKey("log.kb.found_files", "Found Files", 10);
        testKey("error.file.not_exists", "File Not Exists", "/path/to/file");

        System.out.println();

        // 测试不存在的 key
        System.setProperty("log.locale", "zh");
        testKey("non.existent.key", "不存在的Key（应该返回 [non.existent.key]）");

        System.out.println();
        System.out.println("========================================");
        System.out.println("测试完成！");
        System.out.println("Test Completed!");
        System.out.println("========================================");
    }

    private static void testKey(String key, String description, Object... args) {
        String result = LogMessageProvider.getMessage(key, args);
        System.out.println("✓ " + description + ":");
        System.out.println("  Key: " + key);
        System.out.println("  Result: " + result);
        System.out.println();
    }
}

