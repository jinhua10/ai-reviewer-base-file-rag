package top.yumbo.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.i18n.LogLocaleProperties;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * AI Reviewer RAG 系统 - Spring Boot 主入口 (AI Reviewer RAG System - Spring Boot Main Entry)
 *
 * 功能特性： (Features:)
 * - 支持多种文件格式：Excel, Word, PowerPoint, PDF, TXT等 (Supports multiple file formats: Excel, Word, PowerPoint, PDF, TXT, etc.)
 * - 向量检索增强：使用本地嵌入模型进行语义检索 (Vector search enhancement: uses local embedding models for semantic search)
 * - 配置化管理：通过application.yml配置所有参数 (Configuration management: configure all parameters via application.yml)
 * - REST API：提供问答、搜索、统计等接口 (REST API: provides Q&A, search, statistics, etc. interfaces)
 * - 模型检查：启动时自动检查模型文件 (Model check: automatically checks model files on startup)
 *
 * 使用方法： (Usage:)
 * 1. 配置 application.yml 文件 (1. Configure application.yml file)
 * 2. 下载向量嵌入模型到 resources/models/ 目录 (2. Download vector embedding models to resources/models/ directory)
 * 3. 将文档放到配置的文档路径 (3. Put documents into the configured document path)
 * 4. 运行应用 (4. Run the application)
 *
 * API 接口： (API Interfaces:)
 * - POST /api/qa/ask - 问答接口 (Q&A interface)
 * - GET  /api/qa/search - 搜索文档 (Search documents)
 * - GET  /api/qa/statistics - 获取统计信息 (Get statistics)
 * - GET  /api/qa/health - 健康检查 (Health check)
 *
 * @author AI Reviewer Team
 * @since 2025-11-24
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({KnowledgeQAProperties.class, LogLocaleProperties.class})
public class Application {

    public static void main(String[] args) {
        printBanner();

        try {
            // 创建 SpringApplication 实例并显式设置为 Web 应用 (Create SpringApplication instance and explicitly set as Web application)
            SpringApplication app = new SpringApplication(Application.class);
            // 强制设置为 Servlet Web 应用类型，防止应用启动后立即退出 (Force set as Servlet Web application type to prevent app from exiting immediately after startup)
            app.setWebApplicationType(WebApplicationType.SERVLET);
            app.run(args);

            log.info(I18N.get("log.app.started", "http://localhost:8080"));
        } catch (Exception e) {
            log.error(I18N.get("log.app.start_failed"), e);
            System.exit(1);
        }
    }

    /**
     * 打印启动横幅 (Print startup banner)
     */
    private static void printBanner() {
        System.out.println();
        System.out.println("     _     ___   ____               _                             ____      _      ____  ");
        System.out.println("    / \\   |_ _| |  _ \\  ___ __   __(_) ___ __      __ ___   ____ |  _ \\    / \\    / ___|");
        System.out.println("   / _ \\   | |  | |_) |/ _ \\\\ \\ / /| |/ _ \\\\ \\ /\\ / // _ \\ | '__|| |_) |  / _ \\  | |  _ ");
        System.out.println("  / ___ \\  | |  |  _ <|  __/ \\ V / | |  __/ \\ V  V /|  __/ | |   |  _ <  / ___ \\ | |_| |");
        System.out.println(" /_/   \\_\\|___| |_| \\_\\\\___|  \\_/  |_|\\___|  \\_/\\_/  \\___| |_|   |_| \\_\\/_/   \\_\\ \\____|");
        System.out.println();
        System.out.println(" :: AI Reviewer RAG ::                                               (v1.0.0)");
        System.out.println();
        System.out.println(I18N.get("banner.title"));
        System.out.println(I18N.get("banner.version", "1.0.0"));
        System.out.println(I18N.get("banner.supports", "Excel, Word, PowerPoint, PDF, TXT"));
        System.out.println();
    }
}
