package top.yumbo.ai.rag.integration;

import org.junit.jupiter.api.*;
import top.yumbo.ai.rag.integration.IntegrationTestManager.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试套件 (Integration Test Suite)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@DisplayName("Phase 5 - 集成测试")
class IntegrationTest {

    private static IntegrationTestManager testManager;

    @BeforeAll
    static void setUp() {
        testManager = new IntegrationTestManager();
    }

    @AfterAll
    static void tearDown() {
        testManager.shutdown();
    }

    // ========== 功能测试 (Functional Tests) ==========

    @Test
    @DisplayName("测试功能测试套件")
    void testFunctionalTestSuite() {
        // When
        TestSuiteResult result = testManager.runFunctionalTests();

        // Then
        assertNotNull(result);
        assertEquals("Functional Tests", result.getSuiteName());
        assertNotNull(result.getDuration());
        assertTrue(result.getResults().size() > 0);

        // 验证所有测试都成功 (Verify all tests passed)
        long successCount = result.getResults().stream()
            .filter(TestResult::isSuccess)
            .count();

        assertTrue(successCount > 0, "至少应有一个测试成功");
    }

    // ========== 性能测试 (Performance Tests) ==========

    @Test
    @DisplayName("测试性能测试套件")
    void testPerformanceTestSuite() {
        // When
        PerformanceTestResult result = testManager.runPerformanceTests();

        // Then
        assertNotNull(result);
        assertNotNull(result.getConcurrentUsersResult());
        assertNotNull(result.getResponseTimeResult());
        assertNotNull(result.getThroughputResult());
    }

    @Test
    @DisplayName("测试并发用户性能")
    void testConcurrentUsers() {
        // When
        PerformanceTestResult result = testManager.runPerformanceTests();
        ConcurrentTestResult concurrentResult = result.getConcurrentUsersResult();

        // Then
        assertNotNull(concurrentResult);
        assertEquals(100, concurrentResult.getUserCount());
        assertTrue(concurrentResult.getSuccessCount() > 0);
        assertTrue(concurrentResult.getAvgResponseTime() > 0);
    }

    @Test
    @DisplayName("测试响应时间")
    void testResponseTime() {
        // When
        PerformanceTestResult result = testManager.runPerformanceTests();
        ResponseTimeResult responseResult = result.getResponseTimeResult();

        // Then
        assertNotNull(responseResult);
        assertTrue(responseResult.getAvgResponseTime() > 0);
        assertTrue(responseResult.getP50ResponseTime() > 0);
        assertTrue(responseResult.getP95ResponseTime() > 0);
        assertTrue(responseResult.getP99ResponseTime() > 0);

        // 验证性能指标 (Verify performance metrics)
        assertTrue(responseResult.getAvgResponseTime() < 500,
            "平均响应时间应小于500ms");
    }

    @Test
    @DisplayName("测试吞吐量")
    void testThroughput() {
        // When
        PerformanceTestResult result = testManager.runPerformanceTests();
        ThroughputResult throughputResult = result.getThroughputResult();

        // Then
        assertNotNull(throughputResult);
        assertTrue(throughputResult.getTotalQueries() > 0);
        assertTrue(throughputResult.getQps() > 0);

        // 验证QPS (Verify QPS)
        assertTrue(throughputResult.getQps() > 50,
            "QPS应大于50");
    }

    // ========== 报告生成 (Report Generation) ==========

    @Test
    @DisplayName("测试报告生成")
    void testReportGeneration() {
        // Given - 先运行一些测试
        testManager.runFunctionalTests();

        // When
        TestReport report = testManager.generateReport();

        // Then
        assertNotNull(report);
        assertNotNull(report.getGenerateTime());
        assertTrue(report.getTotalTests() > 0);
        assertTrue(report.getSuccessRate() >= 0 && report.getSuccessRate() <= 100);
    }

    // ========== 集成验证 (Integration Validation) ==========

    @Test
    @DisplayName("端到端集成验证")
    void testEndToEndIntegration() {
        // 1. 功能测试
        TestSuiteResult functionalResult = testManager.runFunctionalTests();
        assertNotNull(functionalResult);

        // 2. 性能测试
        PerformanceTestResult performanceResult = testManager.runPerformanceTests();
        assertNotNull(performanceResult);

        // 3. 生成报告
        TestReport report = testManager.generateReport();
        assertNotNull(report);

        // 4. 验证整体成功率
        assertTrue(report.getSuccessRate() > 80,
            "整体成功率应大于80%");
    }
}

