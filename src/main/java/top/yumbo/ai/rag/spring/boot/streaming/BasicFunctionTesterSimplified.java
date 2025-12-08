package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;
import top.yumbo.ai.rag.spring.boot.streaming.model.StreamingResponse;

/**
 * 基本功能测试器 - P0.2 任务（简化版）
 * (Basic Function Tester - P0.2 Task - Simplified)
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Component
@Profile("!test")
@Order(2)
public class BasicFunctionTesterSimplified implements CommandLineRunner {

    private final HOPEFastQueryService hopeFastQueryService;
    private final HybridStreamingService hybridStreamingService;
    private final StreamingSessionMonitor sessionMonitor;

    @Autowired
    public BasicFunctionTesterSimplified(
            @Autowired(required = false) HOPEFastQueryService hopeFastQueryService,
            @Autowired(required = false) HybridStreamingService hybridStreamingService,
            @Autowired(required = false) StreamingSessionMonitor sessionMonitor) {
        this.hopeFastQueryService = hopeFastQueryService;
        this.hybridStreamingService = hybridStreamingService;
        this.sessionMonitor = sessionMonitor;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("\n========================================");
        log.info("   基本功能测试开始 (P0.2)");
        log.info("========================================\n");

        if (hopeFastQueryService == null || hybridStreamingService == null) {
            log.warn("⚠️  流式响应服务未启用，跳过测试");
            return;
        }

        boolean allPassed = true;

        // 测试 1: HOPE 快速查询
        allPassed &= testHOPEQuery();

        // 测试 2: LLM 流式初始化
        allPassed &= testLLMStreaming();

        // 测试 3: SSE 连接
        allPassed &= testSSEConnection();

        // 测试 4: 综合测试
        allPassed &= testIntegrated();

        log.info("\n========================================");
        log.info("   测试结果: " + (allPassed ? "✅ 全部通过" : "⚠️ 部分通过"));
        log.info("========================================\n");
    }

    private boolean testHOPEQuery() {
        log.info("【测试 1】HOPE 快速查询");

        try {
            long start = System.nanoTime();
            HOPEAnswer answer = hopeFastQueryService.queryFast(
                "什么是Docker？",
                "test-" + System.currentTimeMillis()
            );
            long duration = (System.nanoTime() - start) / 1_000_000;

            log.info("  ✅ HOPE 查询完成: {}ms", duration);
            log.info("  - 找到答案: {}", answer != null);
            if (answer != null) {
                log.info("  - 可直接回答: {}", answer.isCanDirectAnswer());
                log.info("  - 置信度: {}", answer.getConfidence());
            }
            return true;
        } catch (Exception e) {
            log.error("  ❌ HOPE 查询失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean testLLMStreaming() {
        log.info("【测试 2】LLM 流式初始化");

        try {
            long start = System.nanoTime();
            StreamingResponse response = hybridStreamingService.ask(
                "Docker容器技术介绍",
                "test-" + System.currentTimeMillis()
            );
            long duration = (System.nanoTime() - start) / 1_000_000;

            log.info("  ✅ LLM 流式会话创建: {}ms", duration);
            log.info("  - 会话ID: {}", response.getSessionId());
            return true;
        } catch (Exception e) {
            log.error("  ❌ LLM 流式初始化失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean testSSEConnection() {
        log.info("【测试 3】SSE 连接");

        try {
            StreamingResponse response = hybridStreamingService.ask(
                "测试SSE连接",
                "test-" + System.currentTimeMillis()
            );

            log.info("  ✅ SSE 会话创建成功");
            log.info("  - SSE URL: /api/qa/stream/{}", response.getSessionId());
            log.info("  - 可通过 curl 测试: curl -N http://localhost:8080/api/qa/stream/{}",
                response.getSessionId());
            return true;
        } catch (Exception e) {
            log.error("  ❌ SSE 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean testIntegrated() {
        log.info("【测试 4】综合流程");

        try {
            String[] questions = {
                "什么是Kubernetes？",
                "Docker和虚拟机的区别？"
            };

            int success = 0;
            for (String question : questions) {
                try {
                    // HOPE 查询
                    hopeFastQueryService.queryFast(question, "test-" + System.currentTimeMillis());

                    // LLM 流式
                    hybridStreamingService.ask(question, "test-" + System.currentTimeMillis());

                    success++;
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.warn("  ⚠️ 问题失败: {}", question);
                }
            }

            log.info("  ✅ 综合测试完成: {}/{}", success, questions.length);
            return success == questions.length;
        } catch (Exception e) {
            log.error("  ❌ 综合测试失败: {}", e.getMessage());
            return false;
        }
    }
}

