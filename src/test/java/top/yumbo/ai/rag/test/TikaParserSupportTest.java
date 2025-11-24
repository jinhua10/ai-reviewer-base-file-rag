package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * TikaDocumentParser 支持格式测试
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class TikaParserSupportTest {

    public static void main(String[] args) {
        TikaDocumentParser parser = new TikaDocumentParser();

        log.info("=".repeat(80));
        log.info("📚 TikaDocumentParser 支持的文件格式");
        log.info("=".repeat(80));

        // 测试支持的扩展名
        testSupportedExtensions(parser);

        // 测试支持的 MIME 类型
        testSupportedMimeTypes(parser);

        // 测试实际文件解析
        testActualFileParsing(parser);
    }

    /**
     * 测试支持的文件扩展名
     */
    private static void testSupportedExtensions(TikaDocumentParser parser) {
        log.info("\n【1】支持的文件扩展名");
        log.info("-".repeat(80));

        List<String> extensions = Arrays.asList(
            // 文本文件
            "txt", "md", "html", "xml", "json",
            // Office 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // 代码文件
            "java", "py", "js", "ts", "c", "cpp", "h", "go", "rs",
            // 其他
            "rtf", "odt", "csv"
        );

        log.info("\n文本文件:");
        testExtensions(parser, Arrays.asList("txt", "md", "html", "xml", "json"));

        log.info("\nOffice 文档:");
        testExtensions(parser, Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"));

        log.info("\n代码文件:");
        testExtensions(parser, Arrays.asList("java", "py", "js", "ts", "c", "cpp", "h", "go", "rs"));

        log.info("\n其他格式:");
        testExtensions(parser, Arrays.asList("rtf", "odt", "csv", "log"));
    }

    private static void testExtensions(TikaDocumentParser parser, List<String> extensions) {
        for (String ext : extensions) {
            boolean supported = parser.supportsExtension(ext);
            String status = supported ? "✅" : "❌";
            log.info("  {} .{}", status, ext);
        }
    }

    /**
     * 测试支持的 MIME 类型
     */
    private static void testSupportedMimeTypes(TikaDocumentParser parser) {
        log.info("\n【2】支持的 MIME 类型");
        log.info("-".repeat(80));

        List<String[]> mimeTypes = Arrays.asList(
            // 文本
            new String[]{"text/plain", "纯文本"},
            new String[]{"text/html", "HTML"},
            new String[]{"text/xml", "XML"},
            new String[]{"text/markdown", "Markdown"},

            // 文档
            new String[]{"application/pdf", "PDF"},
            new String[]{"application/msword", "Word (doc)"},
            new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (docx)"},
            new String[]{"application/vnd.ms-excel", "Excel (xls)"},
            new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel (xlsx)"},
            new String[]{"application/vnd.ms-powerpoint", "PowerPoint (ppt)"},
            new String[]{"application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint (pptx)"},

            // 代码
            new String[]{"text/x-java-source", "Java"},
            new String[]{"text/x-python", "Python"},
            new String[]{"text/x-c", "C/C++"},
            new String[]{"application/javascript", "JavaScript"},
            new String[]{"application/json", "JSON"},
            new String[]{"application/xml", "XML"}
        );

        for (String[] mimeType : mimeTypes) {
            boolean supported = parser.supports(mimeType[0]);
            String status = supported ? "✅" : "❌";
            log.info("  {} {} - {}", status, mimeType[1], mimeType[0]);
        }
    }

    /**
     * 测试实际文件解析
     */
    private static void testActualFileParsing(TikaDocumentParser parser) {
        log.info("\n【3】实际文件解析测试");
        log.info("-".repeat(80));

        // 测试文件路径
        String[] testFiles = {
            "./data/test.txt",
            "./data/test.pdf",
            "./data/test.docx",
            "./data/test.xlsx"
        };

        for (String filePath : testFiles) {
            File file = new File(filePath);
            if (file.exists()) {
                testFileParsing(parser, file);
            } else {
                log.info("  ⚠️  测试文件不存在: {}", filePath);
            }
        }
    }

    private static void testFileParsing(TikaDocumentParser parser, File file) {
        try {
            long startTime = System.currentTimeMillis();

            // 检测 MIME 类型
            String mimeType = parser.detectMimeType(file);

            // 解析内容
            String content = parser.parse(file);

            long elapsed = System.currentTimeMillis() - startTime;

            log.info("\n  文件: {}", file.getName());
            log.info("    - MIME 类型: {}", mimeType);
            log.info("    - 内容长度: {} 字符", content.length());
            log.info("    - 解析耗时: {}ms", elapsed);
            log.info("    - 内容预览: {}",
                content.substring(0, Math.min(100, content.length())) + "...");

        } catch (Exception e) {
            log.error("  ❌ 解析失败: {}", file.getName(), e);
        }
    }
}

