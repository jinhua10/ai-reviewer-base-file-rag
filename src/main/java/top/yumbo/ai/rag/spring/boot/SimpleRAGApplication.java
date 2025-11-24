package top.yumbo.ai.rag.spring.boot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import top.yumbo.ai.rag.spring.boot.autoconfigure.SimpleRAGService;

/**
 * 极简 RAG 应用示例
 *
 * 只需 3 步：
 * 1. 添加依赖
 * 2. 配置 application.yml
 * 3. 注入使用
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@SpringBootApplication
public class SimpleRAGApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleRAGApplication.class, args);
    }

    /**
     * 示例：启动时索引一些文档并测试搜索
     */
    @Bean
    public CommandLineRunner demo(SimpleRAGService ragService) {
        return args -> {
            log.info("=".repeat(80));
            log.info("🚀 极简 RAG 应用示例");
            log.info("=".repeat(80));

            // 1. 索引文档（只需一行代码）
            log.info("\n📝 索引文档...");
            ragService.index("Java教程", "Java是一种面向对象的编程语言，由Sun公司开发...");
            ragService.index("Python教程", "Python是一种解释型、面向对象的高级编程语言...");
            ragService.index("Spring框架", "Spring是一个开源的Java企业级应用开发框架...");
            ragService.commit();

            // 2. 搜索文档（只需一行代码）
            log.info("\n🔍 搜索文档...");
            var results = ragService.search("编程语言");

            log.info("找到 {} 个相关文档:", results.size());
            results.forEach(doc ->
                log.info("  - {}: {}", doc.getTitle(),
                    doc.getContent().substring(0, Math.min(50, doc.getContent().length())) + "...")
            );

            // 3. 查看统计
            log.info("\n📊 统计信息:");
            var stats = ragService.getStatistics();
            log.info("  - 文档总数: {}", stats.getDocumentCount());
            log.info("  - 索引文档数: {}", stats.getIndexedDocumentCount());

            log.info("\n" + "=".repeat(80));
            log.info("✅ 示例完成！");
            log.info("=".repeat(80));
        };
    }
}

