package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.document.LLMResultDocumentService;
import top.yumbo.ai.rag.spring.boot.service.document.LLMResultDocumentService.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 结果文档控制器
 *
 * 提供 LLM 分析结果的保存、查看、下载等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/llm-results")
public class LLMResultDocumentController {

    private final LLMResultDocumentService documentService;

    @Autowired
    public LLMResultDocumentController(LLMResultDocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 保存 LLM 分析结果
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveResult(@RequestBody SaveResultRequest request) {
        try {
            LLMAnalysisResult result = LLMAnalysisResult.builder()
                    .title(request.getTitle())
                    .sourceDocument(request.getSourceDocument())
                    .question(request.getQuestion())
                    .analysisType(request.getAnalysisType())
                    .content(request.getContent())
                    .keyPoints(request.getKeyPoints())
                    .build();

            LLMResultDocument document = documentService.saveResult(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("document", document);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("保存结果失败", e);
            return ResponseEntity.internalServerError().body(errorResponse(e.getMessage()));
        }
    }

    /**
     * 获取历史记录
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(defaultValue = "20") int limit) {

        List<LLMResultDocument> history = documentService.getHistory(limit);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("total", history.size());
        response.put("documents", history);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取文档详情（用于预览）
     */
    @GetMapping("/{docId}")
    public ResponseEntity<?> getDocument(@PathVariable String docId) {
        LLMResultDocument document = documentService.findById(docId);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        String content = documentService.getMarkdownForPreview(docId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("document", document);
        response.put("content", content);

        return ResponseEntity.ok(response);
    }

    /**
     * 预览 Markdown（返回原始 Markdown，图片为链接）
     */
    @GetMapping("/{docId}/preview")
    public ResponseEntity<?> previewMarkdown(@PathVariable String docId) {
        String content = documentService.getMarkdownForPreview(docId);

        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    /**
     * 下载 Markdown（图片转 Base64 嵌入）
     */
    @GetMapping("/{docId}/download/markdown")
    public ResponseEntity<?> downloadMarkdown(@PathVariable String docId) {
        LLMResultDocument document = documentService.findById(docId);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        String content = documentService.getMarkdownForDownload(docId);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        String fileName = document.getFileName() + ".md";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodeFileName(fileName) + "\"")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 下载 PDF
     */
    @GetMapping("/{docId}/download/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable String docId) {
        LLMResultDocument document = documentService.findById(docId);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfContent = documentService.getPdfForDownload(docId);
        if (pdfContent == null) {
            return ResponseEntity.notFound().build();
        }

        String fileName = document.getFileName() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodeFileName(fileName) + "\"")
                .body(pdfContent);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{docId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String docId) {
        boolean deleted = documentService.deleteDocument(docId);

        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文档已删除");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量保存（用于文档分析完成后一次性保存）
     */
    @PostMapping("/batch-save")
    public ResponseEntity<?> batchSave(@RequestBody BatchSaveRequest request) {
        try {
            List<LLMResultDocument> savedDocs = request.getResults().stream()
                    .map(r -> {
                        LLMAnalysisResult result = LLMAnalysisResult.builder()
                                .title(r.getTitle())
                                .sourceDocument(r.getSourceDocument())
                                .question(r.getQuestion())
                                .analysisType(r.getAnalysisType())
                                .content(r.getContent())
                                .keyPoints(r.getKeyPoints())
                                .build();
                        return documentService.saveResult(result);
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("savedCount", savedDocs.size());
            response.put("documents", savedDocs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量保存失败", e);
            return ResponseEntity.internalServerError().body(errorResponse(e.getMessage()));
        }
    }

    // ==================== 辅助方法 ====================

    private String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    // ==================== 请求体类 ====================

    @Data
    public static class SaveResultRequest {
        private String title;
        private String sourceDocument;
        private String question;
        private String analysisType;
        private String content;
        private List<String> keyPoints;
    }

    @Data
    public static class BatchSaveRequest {
        private List<SaveResultRequest> results;
    }
}

