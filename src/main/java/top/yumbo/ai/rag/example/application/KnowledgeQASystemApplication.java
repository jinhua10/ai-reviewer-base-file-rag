package top.yumbo.ai.rag.example.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.yumbo.ai.rag.example.application.config.KnowledgeQAProperties;

/**
 * 知识库问答系统 Spring Boot 应用
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
 * @since 2025-11-22
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(KnowledgeQAProperties.class)
public class KnowledgeQASystemApplication {

    public static void main(String[] args) {
        printBanner();

        try {
            SpringApplication.run(KnowledgeQASystemApplication.class, args);
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
        System.out.println("  ____  _   _  ___        _      ____    _    ____  _   _ ____  ");
        System.out.println(" |  _ \\| \\ | |/ _ \\      / \\    / ___|  / \\  / ___|| | | / ___| ");
        System.out.println(" | |_) |  \\| | | | |    / _ \\  | |     / _ \\ \\___ \\| |_| \\___ \\ ");
        System.out.println(" |  __/| |\\  | |_| |   / ___ \\ | |___ / ___ \\ ___) |  _  |___) |");
        System.out.println(" |_|   |_| \\_|\\___/   /_/   \\_\\ \\____/_/   \\_\\____/|_| |_|____/ ");
        System.out.println();
        System.out.println("  知识库智能问答系统 (Knowledge QA System)");
        System.out.println("  版本: 1.0.0");
        System.out.println("  支持: Excel, Word, PowerPoint, PDF, TXT 等多种格式");
        System.out.println("=".repeat(80));
        System.out.println();
    }
}

