package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.DocumentQAService;

/**
 * 完整文档AI问答 API
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Slf4j
@RestController
@RequestMapping("/api/document-qa")
public class DocumentQAController {

    private final DocumentQAService documentQAService;
    private final String storagePath;

    public DocumentQAController(DocumentQAService documentQAService,
                               @org.springframework.beans.factory.annotation.Value("${knowledge.qa.storage-path:./knowledge-base}")
                               String storagePath) {
        this.documentQAService = documentQAService;
        this.storagePath = storagePath;
    }

    /**
     * 对完整文档进行AI问答
     */
    @PostMapping("/query")
    public ResponseEntity<DocumentQAService.DocumentQAReport> queryDocument(
            @RequestBody DocumentQARequest request) {

        try {
            log.info("收到文档问答请求: 文档={}, 问题={}", request.getDocumentPath(), request.getQuestion());

            DocumentQAService.DocumentQAReport report = documentQAService.queryDocument(
                request.getDocumentPath(),
                request.getQuestion(),
                storagePath
            );

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("文档问答失败", e);

            DocumentQAService.DocumentQAReport errorReport = new DocumentQAService.DocumentQAReport();
            errorReport.setSuccess(false);
            errorReport.setErrorMessage(e.getMessage());

            return ResponseEntity.internalServerError().body(errorReport);
        }
    }

    /**
     * 清理会话临时文件
     */
    @DeleteMapping("/cleanup/{sessionId}")
    public ResponseEntity<String> cleanupSession(@PathVariable String sessionId) {
        try {
            documentQAService.cleanupSession(sessionId);
            return ResponseEntity.ok("会话临时文件已清理: " + sessionId);
        } catch (Exception e) {
            log.error("清理会话失败", e);
            return ResponseEntity.internalServerError().body("清理失败: " + e.getMessage());
        }
    }

    /**
     * 文档问答请求
     */
    @Data
    public static class DocumentQARequest {
        /**
         * 文档路径
         */
        private String documentPath;

        /**
         * 问题
         */
        private String question;
    }
}

