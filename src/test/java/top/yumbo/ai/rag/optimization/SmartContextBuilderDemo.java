package top.yumbo.ai.rag.optimization;

import top.yumbo.ai.rag.model.Document;

import java.util.Arrays;
import java.util.List;

/**
 * SmartContextBuilder 使用示例
 * 演示如何避免内容丢失
 */
public class SmartContextBuilderDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("SmartContextBuilder 内容不丢失演示");
        System.out.println("=".repeat(80));

        // 示例1：默认模式（内容保留）
        demo1_DefaultMode();

        // 示例2：对比两种模式
        demo2_CompareModels();

        // 示例3：处理超长文档
        demo3_VeryLongDocument();
    }

    /**
     * 示例1：默认模式（内容保留）
     */
    private static void demo1_DefaultMode() {
        System.out.println("\n【示例1】默认模式 - 内容保留");
        System.out.println("-".repeat(80));

        SmartContextBuilder builder = new SmartContextBuilder();

        Document doc = createLongDocument();
        String context = builder.buildSmartContext("API 设计", Arrays.asList(doc));

        System.out.println("原文长度: " + doc.getContent().length() + " 字符");
        System.out.println("提取长度: " + context.length() + " 字符");
        System.out.println("保留率: " + String.format("%.1f%%",
            (double)context.length() / doc.getContent().length() * 100));
        System.out.println("\n提取内容（前500字符）:");
        System.out.println(context.substring(0, Math.min(500, context.length())));
        System.out.println("...");
    }

    /**
     * 示例2：对比两种模式
     */
    private static void demo2_CompareModels() {
        System.out.println("\n【示例2】模式对比");
        System.out.println("-".repeat(80));

        // 保留模式
        SmartContextBuilder preserveBuilder = SmartContextBuilder.builder()
            .maxContextLength(2000)
            .maxDocLength(1000)
            .preserveFullContent(true)
            .build();

        // 提取模式
        SmartContextBuilder extractBuilder = SmartContextBuilder.builder()
            .maxContextLength(2000)
            .maxDocLength(1000)
            .preserveFullContent(false)
            .build();

        Document doc = createLongDocument();

        String preserveContext = preserveBuilder.buildSmartContext("API 设计", Arrays.asList(doc));
        String extractContext = extractBuilder.buildSmartContext("API 设计", Arrays.asList(doc));

        System.out.println("原文长度: " + doc.getContent().length() + " 字符");
        System.out.println("\n保留模式:");
        System.out.println("  - 提取长度: " + preserveContext.length() + " 字符");
        System.out.println("  - 包含关键词: " + countKeyword(preserveContext, "API"));
        System.out.println("  - 包含分块标记: " + preserveContext.contains("..."));

        System.out.println("\n提取模式:");
        System.out.println("  - 提取长度: " + extractContext.length() + " 字符");
        System.out.println("  - 包含关键词: " + countKeyword(extractContext, "API"));
        System.out.println("  - 包含省略标记: " + extractContext.contains("..."));

        System.out.println("\n结论: 保留模式可以提取更多包含关键词的片段");
    }

    /**
     * 示例3：处理超长文档
     */
    private static void demo3_VeryLongDocument() {
        System.out.println("\n【示例3】超长文档处理");
        System.out.println("-".repeat(80));

        SmartContextBuilder builder = SmartContextBuilder.builder()
            .maxContextLength(3000)
            .maxDocLength(1500)
            .preserveFullContent(true)
            .build();

        // 创建一个包含多个关键词位置的超长文档
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            content.append("章节").append(i + 1).append("：");
            if (i % 5 == 0) {
                content.append("这里讨论了重要的API设计原则。");
            } else {
                content.append("这是一些其他的填充内容。");
            }
            content.append("详细内容省略...".repeat(20));
        }

        Document doc = new Document();
        doc.setTitle("API设计指南");
        doc.setContent(content.toString());

        String context = builder.buildSmartContext("API设计", Arrays.asList(doc));

        System.out.println("原文长度: " + doc.getContent().length() + " 字符");
        System.out.println("提取长度: " + context.length() + " 字符");
        System.out.println("关键词出现次数: " + countKeyword(context, "API"));

        // 统计信息
        SmartContextBuilder.ContextStats stats = builder.getContextStats(context);
        System.out.println("\n上下文统计: " + stats);
    }

    /**
     * 创建一个长文档示例
     */
    private static Document createLongDocument() {
        StringBuilder content = new StringBuilder();

        content.append("第一章：引言\n");
        content.append("本文档介绍了系统的整体架构和设计原则。");
        content.append("填充内容...".repeat(50));
        content.append("\n\n");

        content.append("第二章：API设计\n");
        content.append("API设计是系统架构的核心部分。良好的API设计应该遵循以下原则：\n");
        content.append("1. 简洁性：API接口应该简单明了\n");
        content.append("2. 一致性：保持命名和使用模式的一致性\n");
        content.append("3. 可扩展性：预留扩展空间\n");
        content.append("填充内容...".repeat(50));
        content.append("\n\n");

        content.append("第三章：实现细节\n");
        content.append("在实现API设计时，需要注意性能和安全性。");
        content.append("填充内容...".repeat(50));
        content.append("\n\n");

        content.append("第四章：最佳实践\n");
        content.append("基于多年的API设计经验，我们总结了以下最佳实践。");
        content.append("填充内容...".repeat(50));

        Document doc = new Document();
        doc.setTitle("系统架构设计文档");
        doc.setContent(content.toString());

        return doc;
    }

    /**
     * 统计关键词出现次数
     */
    private static int countKeyword(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}

