package top.yumbo.ai.rag.example.simple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.spring.boot.autoconfigure.SimpleRAGService;

import java.io.File;
import java.util.List;

/**
 * SimpleRAGService.indexFile() 功能演示
 *
 * 演示如何使用 SimpleRAGService 索引文件
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@SpringBootApplication
public class FileIndexExample {

    public static void main(String[] args) {
        SpringApplication.run(FileIndexExample.class, args);
    }

    @Bean
    public CommandLineRunner demo(SimpleRAGService ragService) {
        return args -> {
            log.info("=".repeat(80));
            log.info("📁 SimpleRAGService 文件索引功能演示");
            log.info("=".repeat(80));

            // 示例1: 索引单个文件
            log.info("\n【示例1】索引单个文件");
            File singleFile = new File("./data/test.txt");
            if (singleFile.exists()) {
                String docId = ragService.indexFile(singleFile);
                log.info("✅ 文件已索引: {} -> {}", singleFile.getName(), docId);
            } else {
                log.warn("⚠️  测试文件不存在: {}", singleFile.getAbsolutePath());
            }

            // 示例2: 批量索引文件
            log.info("\n【示例2】批量索引文件");
            File dir = new File("./data/documents");
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) ->
                    name.endsWith(".txt") ||
                    name.endsWith(".pdf") ||
                    name.endsWith(".docx")
                );

                if (files != null && files.length > 0) {
                    int count = ragService.indexFiles(List.of(files));
                    log.info("✅ 批量索引完成: {} 个文件", count);
                } else {
                    log.warn("⚠️  目录中没有文件");
                }
            } else {
                log.warn("⚠️  目录不存在: {}", dir.getAbsolutePath());
            }

            // 示例3: 递归索引目录
            log.info("\n【示例3】递归索引目录");
            File recursiveDir = new File("./data");
            if (recursiveDir.exists()) {
                int count = ragService.indexDirectory(recursiveDir, true);
                log.info("✅ 递归索引完成: {} 个文件", count);
            }

            // 提交更改
            ragService.commit();

            // 示例4: 搜索已索引的内容
            log.info("\n【示例4】搜索已索引的内容");
            List<Document> results = ragService.search("测试", 5);
            log.info("找到 {} 个相关文档:", results.size());
            results.forEach(doc ->
                log.info("  - {}: {}",
                    doc.getTitle(),
                    doc.getContent().substring(0, Math.min(50, doc.getContent().length())) + "..."
                )
            );

            // 统计信息
            log.info("\n【统计信息】");
            var stats = ragService.getStatistics();
            log.info("  - 文档总数: {}", stats.getDocumentCount());
            log.info("  - 索引文档数: {}", stats.getIndexedDocumentCount());

            log.info("\n" + "=".repeat(80));
            log.info("✅ 演示完成！");
            log.info("=".repeat(80));
        };
    }
}

