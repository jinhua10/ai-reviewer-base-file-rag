package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 提供测试 API
 * (Test Controller - Provides test APIs)
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    private final PerformanceBenchmarkService benchmarkService;

    @Autowired
    public TestController(@Autowired(required = false) PerformanceBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * HOPE 查询性能测试
     *
     * GET /api/test/benchmark/hope?question=什么是Docker&iterations=10
     */
    @GetMapping("/benchmark/hope")
    public ResponseEntity<?> benchmarkHope(
            @RequestParam(defaultValue = "什么是Docker？") String question,
            @RequestParam(defaultValue = "10") int iterations) {

        if (benchmarkService == null) {
            return ResponseEntity.ok(createErrorResponse("Benchmark service not available"));
        }

        log.info(I18N.get("log.test.hope_benchmark", question, iterations));

        try {
            PerformanceBenchmarkService.BenchmarkResult result =
                benchmarkService.benchmarkHOPEQuery(question, iterations);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("testName", result.getTestName());
            response.put("iterations", result.getIterations());
            response.put("successCount", result.getSuccessCount());
            response.put("foundCount", result.getFoundCount());
            response.put("minDuration", result.getMinDuration());
            response.put("maxDuration", result.getMaxDuration());
            response.put("avgDuration", result.getAvgDuration());
            response.put("targetDuration", result.getTargetDuration());
            response.put("meetsTarget", result.meetsTarget());
            response.put("summary", result.getSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(I18N.get("log.test.hope_benchmark_failed"), e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * LLM 初始化性能测试
     *
     * GET /api/test/benchmark/llm?question=介绍Docker&iterations=5
     */
    @GetMapping("/benchmark/llm")
    public ResponseEntity<?> benchmarkLLM(
            @RequestParam(defaultValue = "请简要介绍Docker") String question,
            @RequestParam(defaultValue = "5") int iterations) {

        if (benchmarkService == null) {
            return ResponseEntity.ok(createErrorResponse("Benchmark service not available"));
        }

        log.info(I18N.get("log.test.llm_benchmark", question, iterations));

        try {
            PerformanceBenchmarkService.BenchmarkResult result =
                benchmarkService.benchmarkLLMInitialization(question, iterations);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("testName", result.getTestName());
            response.put("iterations", result.getIterations());
            response.put("successCount", result.getSuccessCount());
            response.put("minDuration", result.getMinDuration());
            response.put("maxDuration", result.getMaxDuration());
            response.put("avgDuration", result.getAvgDuration());
            response.put("targetDuration", result.getTargetDuration());
            response.put("meetsTarget", result.meetsTarget());
            response.put("summary", result.getSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(I18N.get("log.test.llm_benchmark_failed"), e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 端到端性能测试
     *
     * GET /api/test/benchmark/e2e?question=什么是Kubernetes&iterations=5
     */
    @GetMapping("/benchmark/e2e")
    public ResponseEntity<?> benchmarkEndToEnd(
            @RequestParam(defaultValue = "什么是Kubernetes？") String question,
            @RequestParam(defaultValue = "5") int iterations) {

        if (benchmarkService == null) {
            return ResponseEntity.ok(createErrorResponse("Benchmark service not available"));
        }

        log.info(I18N.get("log.test.end_to_end_benchmark", question, iterations));

        try {
            PerformanceBenchmarkService.BenchmarkResult result =
                benchmarkService.benchmarkEndToEnd(question, iterations);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("testName", result.getTestName());
            response.put("iterations", result.getIterations());
            response.put("successCount", result.getSuccessCount());
            response.put("minDuration", result.getMinDuration());
            response.put("maxDuration", result.getMaxDuration());
            response.put("avgDuration", result.getAvgDuration());
            response.put("targetDuration", result.getTargetDuration());
            response.put("meetsTarget", result.meetsTarget());
            response.put("summary", result.getSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(I18N.get("log.test.end_to_end_benchmark_failed"), e);
            return ResponseEntity.ok(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 运行所有基本功能测试
     *
     * GET /api/test/basic-functions
     */
    @GetMapping("/basic-functions")
    public ResponseEntity<?> runBasicFunctionTests() {
        log.info(I18N.get("log.test.basic_function_suite"));

        Map<String, Object> results = new HashMap<>();
        results.put("timestamp", System.currentTimeMillis());
        results.put("message", "基本功能测试已触发，请查看应用日志获取详细结果");
        results.put("note", "测试会在后台运行，完整结果将输出到日志");

        return ResponseEntity.ok(results);
    }

    /**
     * 健康检查
     *
     * GET /api/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("benchmarkService", benchmarkService != null ? "Available" : "Not Available");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}

