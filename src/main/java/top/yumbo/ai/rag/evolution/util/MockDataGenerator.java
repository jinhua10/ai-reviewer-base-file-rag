package top.yumbo.ai.rag.evolution.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.evolution.model.ConceptConflict;
import top.yumbo.ai.rag.evolution.service.ConceptConflictService;
import top.yumbo.ai.rag.evolution.service.ConceptEvolutionService;
import top.yumbo.ai.rag.evolution.service.VotingService;

import java.util.Random;

/**
 * Mock 数据生成器 (Mock Data Generator)
 *
 * 用于生成测试用的冲突、投票和演化数据
 * (Used to generate test conflict, vote, and evolution data)
 *
 * 启用方式：在 application.yml 中添加 (Enable by adding to application.yml):
 * evolution:
 *   mock-data:
 *     enabled: true
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "evolution.mock-data", name = "enabled", havingValue = "true")
public class MockDataGenerator implements CommandLineRunner {

    private final ConceptConflictService conflictService;
    private final ConceptEvolutionService evolutionService;
    private final VotingService votingService;
    private final Random random = new Random();

    @Autowired
    public MockDataGenerator(ConceptConflictService conflictService,
                            ConceptEvolutionService evolutionService,
                            VotingService votingService) {
        this.conflictService = conflictService;
        this.evolutionService = evolutionService;
        this.votingService = votingService;
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("开始生成 Mock 数据...");
        log.info("========================================");

        generateMockConflicts();

        log.info("========================================");
        log.info("Mock 数据生成完成！");
        log.info("========================================");
    }

    /**
     * 生成模拟冲突数据 (Generate mock conflict data)
     */
    private void generateMockConflicts() {
        log.info("正在生成模拟冲突...");

        // 冲突 1: 微服务架构定义
        ConceptConflict conflict1 = conflictService.createConflict(
            "什么是微服务架构？",
            "微服务是一种将应用程序构建为一系列小型、独立服务的架构风格。每个服务运行在自己的进程中，服务之间使用轻量级通信机制（通常是HTTP API）进行通信。",
            "微服务架构是一种分布式系统设计模式，每个服务负责单一业务功能，可以独立开发、部署和扩展。服务之间通过明确定义的接口进行交互。",
            "微服务设计模式.pdf",
            "分布式系统架构.pdf"
        );
        addRandomVotes(conflict1);

        // 冲突 2: 数据库优化方法
        ConceptConflict conflict2 = conflictService.createConflict(
            "如何优化数据库查询性能？",
            "通过添加索引和优化SQL语句来提升查询速度。合理使用 EXPLAIN 分析查询计划，避免全表扫描。",
            "使用缓存、读写分离和分库分表等技术优化性能。同时结合连接池管理、慢查询日志分析等手段进行综合优化。",
            "SQL优化指南.pdf",
            "数据库架构设计.pdf"
        );
        addRandomVotes(conflict2);

        // 冲突 3: RESTful API (已解决)
        ConceptConflict conflict3 = conflictService.createConflict(
            "什么是RESTful API？",
            "RESTful API是基于REST架构风格的Web服务接口，使用HTTP协议的GET、POST、PUT、DELETE等方法对资源进行操作。",
            "REST API是一种无状态的接口设计规范，使用标准HTTP方法实现CRUD操作，资源通过URI进行标识，数据通常采用JSON格式传输。",
            "REST架构原理.pdf",
            "Web API设计指南.pdf"
        );
        addManyVotes(conflict3, "B"); // B 获胜

        // 冲突 4: Docker容器化
        ConceptConflict conflict4 = conflictService.createConflict(
            "Docker容器化的核心优势是什么？",
            "Docker提供轻量级虚拟化，实现应用程序及其依赖的打包和隔离，确保在不同环境中一致运行。",
            "Docker容器化技术实现了环境一致性、快速部署、资源高效利用和版本控制，是现代DevOps的核心工具。",
            "Docker入门教程.pdf",
            "容器化最佳实践.pdf"
        );
        addRandomVotes(conflict4);

        // 冲突 5: CI/CD流程
        ConceptConflict conflict5 = conflictService.createConflict(
            "CI/CD的完整流程包括哪些步骤？",
            "持续集成（CI）包括代码提交、自动构建、单元测试；持续部署（CD）包括集成测试、预发布环境部署、生产环境部署。",
            "CI/CD流程涵盖源代码管理、自动化构建、测试（单元测试、集成测试、端到端测试）、制品管理、自动化部署、监控告警等全生命周期。",
            "CI_CD实践指南.pdf",
            "DevOps工程师手册.pdf"
        );
        addManyVotes(conflict5, "B"); // B 获胜

        log.info("已生成 5 个模拟冲突");

        // 记录演化历史
        recordEvolutionHistory();
    }

    /**
     * 添加随机投票 (Add random votes)
     */
    private void addRandomVotes(ConceptConflict conflict) {
        int voteCount = random.nextInt(8) + 3; // 3-10 票

        for (int i = 0; i < voteCount; i++) {
            String choice = random.nextBoolean() ? "A" : "B";
            String userId = "user-" + random.nextInt(1000);
            String reason = generateRandomReason(choice);

            votingService.submitVote(
                conflict.getId(),
                userId,
                choice,
                reason,
                "127.0.0." + random.nextInt(255)
            );
        }

        log.info("为冲突 {} 添加了 {} 个随机投票", conflict.getId(), voteCount);
    }

    /**
     * 添加大量投票（用于已解决的冲突）(Add many votes for resolved conflicts)
     */
    private void addManyVotes(ConceptConflict conflict, String winningChoice) {
        int winningVotes = random.nextInt(10) + 20; // 20-29 票
        int losingVotes = random.nextInt(8) + 5;   // 5-12 票
        String losingChoice = "A".equals(winningChoice) ? "B" : "A";

        // 添加获胜选项的投票 (Add winning votes)
        for (int i = 0; i < winningVotes; i++) {
            votingService.submitVote(
                conflict.getId(),
                "user-" + random.nextInt(1000),
                winningChoice,
                generateRandomReason(winningChoice),
                "127.0.0." + random.nextInt(255)
            );
        }

        // 添加失败选项的投票 (Add losing votes)
        for (int i = 0; i < losingVotes; i++) {
            votingService.submitVote(
                conflict.getId(),
                "user-" + random.nextInt(1000),
                losingChoice,
                generateRandomReason(losingChoice),
                "127.0.0." + random.nextInt(255)
            );
        }

        log.info("为冲突 {} 添加了投票: {} 获胜({}票), {} 失败({}票)",
            conflict.getId(), winningChoice, winningVotes, losingChoice, losingVotes);
    }

    /**
     * 生成随机投票原因 (Generate random vote reason)
     */
    private String generateRandomReason(@SuppressWarnings("unused") String choice) {
        String[] reasons = {
            "定义更准确清晰",
            "覆盖更全面",
            "更符合实际应用场景",
            "技术描述更专业",
            "易于理解和实施",
            "包含更多实践经验",
            "更贴近行业标准"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    /**
     * 记录演化历史 (Record evolution history)
     */
    private void recordEvolutionHistory() {
        log.info("正在生成演化历史...");

        // 为每个概念创建演化记录
        String conceptId1 = "concept-microservices";
        evolutionService.recordCreation(
            conceptId1,
            "微服务是一种架构风格",
            "system"
        );
        evolutionService.recordUpdate(
            conceptId1,
            "微服务是一种将应用程序构建为小型独立服务的架构风格",
            "admin",
            "根据用户反馈优化定义"
        );

        String conceptId2 = "concept-restful";
        evolutionService.recordCreation(
            conceptId2,
            "RESTful API是基于REST架构风格的Web服务接口",
            "system"
        );
        evolutionService.recordConflictResolution(
            conceptId2,
            "conflict-restful-api",
            "REST API是一种无状态的接口设计规范，使用标准HTTP方法实现CRUD操作",
            "RESTful API是基于REST架构风格的Web服务接口",
            "community",
            "社区投票决定，选择B"
        );

        log.info("已生成演化历史记录");
    }
}

