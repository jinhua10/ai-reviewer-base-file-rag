package top.yumbo.ai.rag.optimization;

import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.model.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SmartContextBuilder 测试类
 * 验证内容不丢失的分块策略
 */
class SmartContextBuilderTest {

    @Test
    void testShortContentNotModified() {
        SmartContextBuilder builder = new SmartContextBuilder();

        Document doc = new Document();
        doc.setTitle("短文档");
        doc.setContent("这是一个短文档，不需要分块。");

        String context = builder.buildSmartContext("测试", Arrays.asList(doc));

        assertTrue(context.contains("这是一个短文档，不需要分块。"));
        System.out.println("✅ 短文档测试通过");
    }

    @Test
    void testLongContentWithKeywords() {
        SmartContextBuilder builder = SmartContextBuilder.builder()
            .maxContextLength(1000)
            .maxDocLength(500)
            .preserveFullContent(true)
            .build();

        // 创建一个包含多个关键词的长文档
        StringBuilder longContent = new StringBuilder();
        longContent.append("前言部分：这是文档的开始。");
        for (int i = 0; i < 10; i++) {
            longContent.append("第").append(i).append("段内容，包含一些填充文字。");
        }
        longContent.append("重要关键词在这里。");
        for (int i = 0; i < 10; i++) {
            longContent.append("更多填充内容。");
        }
        longContent.append("另一个重要关键词也在这里。");
        for (int i = 0; i < 10; i++) {
            longContent.append("结尾部分的填充内容。");
        }

        Document doc = new Document();
        doc.setTitle("长文档");
        doc.setContent(longContent.toString());

        String context = builder.buildSmartContext("重要关键词", Arrays.asList(doc));

        // 验证关键词都被保留
        assertTrue(context.contains("重要关键词在这里") || context.contains("重要关键词"),
            "第一个关键词应该被保留");

        System.out.println("原始长度: " + longContent.length());
        System.out.println("提取长度: " + context.length());
        System.out.println("提取内容:\n" + context);
        System.out.println("✅ 长文档关键词提取测试通过");
    }

    @Test
    void testPreserveFullContentMode() {
        SmartContextBuilder builderPreserve = SmartContextBuilder.builder()
            .maxContextLength(2000)
            .maxDocLength(1000)
            .preserveFullContent(true)
            .build();

        SmartContextBuilder builderExtract = SmartContextBuilder.builder()
            .maxContextLength(2000)
            .maxDocLength(1000)
            .preserveFullContent(false)
            .build();

        // 创建一个超长文档
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("段落").append(i).append("：这是一些测试内容。");
            if (i == 50) {
                longContent.append("关键词出现在中间位置。");
            }
        }

        Document doc = new Document();
        doc.setTitle("超长文档");
        doc.setContent(longContent.toString());

        String contextPreserve = builderPreserve.buildSmartContext("关键词", Arrays.asList(doc));
        String contextExtract = builderExtract.buildSmartContext("关键词", Arrays.asList(doc));

        System.out.println("\n=== 保留模式 (preserveFullContent=true) ===");
        System.out.println("长度: " + contextPreserve.length());
        System.out.println("内容:\n" + contextPreserve.substring(0, Math.min(500, contextPreserve.length())));

        System.out.println("\n=== 提取模式 (preserveFullContent=false) ===");
        System.out.println("长度: " + contextExtract.length());
        System.out.println("内容:\n" + contextExtract.substring(0, Math.min(500, contextExtract.length())));

        // 保留模式应该包含更多内容
        assertTrue(contextPreserve.length() >= contextExtract.length() ||
                   contextPreserve.contains("还有") || contextPreserve.contains("..."),
            "保留模式应该包含更多内容或提示");

        System.out.println("✅ 两种模式对比测试通过");
    }

    @Test
    void testMultipleDocuments() {
        SmartContextBuilder builder = SmartContextBuilder.builder()
            .maxContextLength(2000)
            .maxDocLength(800)
            .preserveFullContent(true)
            .build();

        Document doc1 = new Document();
        doc1.setTitle("文档1");
        doc1.setContent("第一个文档的内容，包含查询关键词。".repeat(50));

        Document doc2 = new Document();
        doc2.setTitle("文档2");
        doc2.setContent("第二个文档的内容，也包含查询关键词。".repeat(50));

        List<Document> docs = Arrays.asList(doc1, doc2);
        String context = builder.buildSmartContext("查询关键词", docs);

        assertTrue(context.contains("文档1"));
        assertTrue(context.contains("文档2"));
        assertTrue(context.contains("查询关键词"));

        System.out.println("总长度: " + context.length());
        System.out.println("✅ 多文档测试通过");
    }

    @Test
    void testContextStats() {
        SmartContextBuilder builder = new SmartContextBuilder();

        Document doc = new Document();
        doc.setTitle("测试文档");
        doc.setContent("这是测试内容。");

        String context = builder.buildSmartContext("测试", Arrays.asList(doc));
        SmartContextBuilder.ContextStats stats = builder.getContextStats(context);

        assertNotNull(stats);
        assertTrue(stats.getTotalLength() > 0);
        assertEquals(1, stats.getDocumentCount());

        System.out.println("统计信息: " + stats);
        System.out.println("✅ 统计信息测试通过");
    }
}

