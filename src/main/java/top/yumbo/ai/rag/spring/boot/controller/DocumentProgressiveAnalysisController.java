package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.spring.boot.service.document.DocumentProgressiveAnalysisService;
import top.yumbo.ai.rag.spring.boot.service.document.StageOutputManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档渐进式分析控制器
 *
 * 提供通用文档分析 API
 */
@Slf4j
@RestController
@RequestMapping("/api/document/progressive")
public class DocumentProgressiveAnalysisController {

    private final DocumentProgressiveAnalysisService analysisService;

    @Autowired
    public DocumentProgressiveAnalysisController(DocumentProgressiveAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 分析上传的文档
     *
     * @param file 文档文件
     * @param question 用户问题
     * @return 分析报告
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("请上传文件"));
        }

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("请提供分析问题"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "unknown";
        }

        // 检查是否支持该文件类型
        if (!analysisService.isSupported(originalFilename)) {
            return ResponseEntity.badRequest().body(
                    errorResponse("不支持的文件类型: " + originalFilename +
                            "，支持的类型: " + analysisService.getSupportedTypes()));
        }

        try {
            // 保存临时文件
            Path tempFile = Files.createTempFile("doc_analysis_", "_" + originalFilename);
            file.transferTo(tempFile);

            log.info("📄 收到文档分析请求: {}, 问题: {}", originalFilename, question);

            // 执行分析
            DocumentProgressiveAnalysisService.DocumentAnalysisReport report =
                    analysisService.analyzeProgressively(tempFile.toFile(), question);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            if (!report.isSuccess()) {
                return ResponseEntity.internalServerError().body(
                        errorResponse("分析失败: " + report.getErrorMessage()));
            }

            return ResponseEntity.ok(successResponse(report));

        } catch (IOException e) {
            log.error("文件处理失败", e);
            return ResponseEntity.internalServerError().body(
                    errorResponse("文件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 分析指定路径的文档
     *
     * @param request 请求体
     * @return 分析报告
     */
    @PostMapping("/analyze/path")
    public ResponseEntity<?> analyzeDocumentByPath(@RequestBody AnalyzeRequest request) {
        if (request.getPath() == null || request.getPath().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("请提供文档路径"));
        }

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("请提供分析问题"));
        }

        File file = new File(request.getPath());
        if (!file.exists()) {
            return ResponseEntity.badRequest().body(errorResponse("文件不存在: " + request.getPath()));
        }

        if (!analysisService.isSupported(request.getPath())) {
            return ResponseEntity.badRequest().body(
                    errorResponse("不支持的文件类型，支持的类型: " + analysisService.getSupportedTypes()));
        }

        log.info("📄 收到文档分析请求: {}, 问题: {}", file.getName(), request.getQuestion());

        DocumentProgressiveAnalysisService.DocumentAnalysisReport report =
                analysisService.analyzeProgressively(file, request.getQuestion());

        if (!report.isSuccess()) {
            return ResponseEntity.internalServerError().body(
                    errorResponse("分析失败: " + report.getErrorMessage()));
        }

        return ResponseEntity.ok(successResponse(report));
    }

    /**
     * 获取支持的文档类型
     */
    @GetMapping("/supported-types")
    public ResponseEntity<?> getSupportedTypes() {
        List<String> types = analysisService.getSupportedTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("supportedTypes", types);

        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> successResponse(DocumentProgressiveAnalysisService.DocumentAnalysisReport report) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fileName", report.getFileName());
        response.put("question", report.getQuestion());
        response.put("duration", report.getDuration());
        response.put("segmentCount", report.getSegmentResults().size());
        response.put("comprehensiveSummary", report.getComprehensiveSummary());
        response.put("stageOutputs", report.getStageOutputs());
        response.put("memoDocument", report.getMemoDocument());

        // 简化的片段结果
        response.put("segments", report.getSegmentResults().stream()
                .map(seg -> {
                    Map<String, Object> segMap = new HashMap<>();
                    segMap.put("index", seg.getSegmentIndex());
                    segMap.put("title", seg.getTitle());
                    segMap.put("keyPoints", seg.getKeyPoints());
                    return segMap;
                })
                .toList());

        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    // ==================== 请求体类 ====================

    @Data
    public static class AnalyzeRequest {
        private String path;
        private String question;
    }
}

