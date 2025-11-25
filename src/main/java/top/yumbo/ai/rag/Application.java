package top.yumbo.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

/**
 * AI Reviewer RAG 系统 - Spring Boot 主入口
 *
 * 功能特性：
 * - 支持多种文件格式：Excel, Word, PowerPoint, PDF, TXT等
 * - 向量检索增强：使用本地嵌入模型进行语义检索
 * - 配置化管理：通过application.yml配置所有参数
 * - REST API：提供问答、搜索、统计等接口
 * - 模型检查：启动时自动检查模型文件
 *
 * 使用方法：
 * 1. 配置 application.yml 文件
 * 2. 下载向量嵌入模型到 resources/models/ 目录
 * 3. 将文档放到配置的文档路径
 * 4. 运行应用
 *
 * API 接口：
 * - POST /api/qa/ask - 问答接口
 * - GET  /api/qa/search - 搜索文档
 * - GET  /api/qa/statistics - 获取统计信息
 * - GET  /api/qa/health - 健康检查
 *
 * @author AI Reviewer Team
 * @since 2025-11-24
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(KnowledgeQAProperties.class)
public class Application {

    public static void main(String[] args) {
        printBanner();

        try {
            // 创建 SpringApplication 实例并显式设置为 Web 应用
            SpringApplication app = new SpringApplication(Application.class);
            // 强制设置为 Servlet Web 应用类型，防止应用启动后立即退出
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);
            app.run(args);

            log.info("✅ 应用启动成功，默认端口：http://localhost:8080");
        } catch (Exception e) {
            log.error("❌ 应用启动失败", e);
            System.exit(1);
        }
    }

    /**
     * 打印启动横幅
     */
    private static void printBanner() {
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("   _    ___   ____                _                           ____      _    ____  ");
        System.out.println("  / \\  |_ _| |  _ \\ _____   _____ (_) _____      _____ _ __  |  _ \\    / \\  / ___| ");
        System.out.println(" / _ \\  | |  | |_) / _ \\ \\ / / _ \\| |/ _ \\ \\ /\\ / / _ \\ '__| | |_) |  / _ \\| |  _  ");
        System.out.println("/ ___ \\ | |  |  _ <  __/\\ V /  __/| |  __/\\ V  V /  __/ |    |  _ <  / ___ \\ |_| | ");
        System.out.println("\\_/ \\_\\|___| |_| \\_\\___| \\_/ \\___|_|\\___| \\_/\\_/ \\___|_|    |_| \\_\\/_/   \\_\\____| ");
        System.out.println();
        System.out.println("  AI Reviewer - 知识库智能问答系统 (Knowledge QA System)");
        System.out.println("  版本: 1.0.0");
        System.out.println("  支持: Excel, Word, PowerPoint, PDF, TXT 等多种格式");
        System.out.println("=".repeat(80));
        System.out.println();
    }
}

