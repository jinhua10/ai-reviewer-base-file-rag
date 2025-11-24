package top.yumbo.ai.rag.optimization;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 用于在 Maven Surefire 下检查 JVM 编码与控制台显示的测试。
 */
public class EncodingCheckTest {

    private static final Logger log = LoggerFactory.getLogger(EncodingCheckTest.class);

    @Test
    void printEncoding() {
        System.out.println("=== EncodingCheckTest START ===");
        System.out.println("file.encoding=" + System.getProperty("file.encoding"));
        System.out.println("defaultCharset=" + Charset.defaultCharset());
        System.out.println("sun.stdout.encoding=" + System.getProperty("sun.stdout.encoding"));
        System.out.println("sun.stderr.encoding=" + System.getProperty("sun.stderr.encoding"));
        System.out.println("console.encoding=" + System.getProperty("console.encoding"));
        System.out.println("中文测试：这是一个中文测试行。");
        log.info("log.info 中文测试：这是 log.info 的中文输出");
        System.out.println("=== EncodingCheckTest END ===");
    }
}

