package top.yumbo.ai.rag.spring.boot.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.DocumentQAService;
import top.yumbo.ai.rag.spring.boot.service.PPTProgressiveAnalysisService;

import java.io.File;

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
    private final PPTProgressiveAnalysisService pptAnalysisService;
    private final String storagePath;
    private final String documentsPath;

    public DocumentQAController(DocumentQAService documentQAService,
                               PPTProgressiveAnalysisService pptAnalysisService,
                               @Value("${knowledge.qa.storage-path:./knowledge-base}")
                               String storagePath,
                               @Value("${knowledge.qa.knowledge-base.source-path:./data/documents}")
                               String documentsPath) {
        this.documentQAService = documentQAService;
        this.pptAnalysisService = pptAnalysisService;
        this.storagePath = storagePath;
        this.documentsPath = documentsPath;
    }

    /**
     * 根据文件名解析完整路径
     * 如果传入的是文件名，则拼接文档路径；如果是完整路径，则直接返回
     */
    private String resolveFilePath(String fileNameOrPath) {
        File file = new File(fileNameOrPath);

        // 如果文件存在，说明是完整路径
        if (file.exists()) {
            return fileNameOrPath;
        }

        // 否则认为是文件名，拼接文档路径
        File documentFile = new File(documentsPath, fileNameOrPath);
        if (documentFile.exists()) {
            return documentFile.getAbsolutePath();
        }

        // 如果都不存在，返回拼接后的路径（让后续错误处理统一处理）
        return documentFile.getAbsolutePath();
    }

    /**
     * 对完整文档进行AI问答
     */
    @PostMapping("/query")
    public ResponseEntity<DocumentQAService.DocumentQAReport> queryDocument(
            @RequestBody DocumentQARequest request) {

        try {
            String filePath = resolveFilePath(request.getDocumentPath());
            log.info("收到文档问答请求: 文档={}, 解析路径={}, 问题={}",
                request.getDocumentPath(), filePath, request.getQuestion());

            DocumentQAService.DocumentQAReport report = documentQAService.queryDocument(
                filePath,
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
     * PPT渐进式分析（推荐用于PPT文档）
     */
    @PostMapping("/analyze-ppt")
    public ResponseEntity<PPTProgressiveAnalysisService.PPTAnalysisReport> analyzePPT(
            @RequestBody DocumentQARequest request) {

        try {
            String filePath = resolveFilePath(request.getDocumentPath());
            log.info("收到PPT渐进式分析请求: 文档={}, 解析路径={}, 问题={}",
                request.getDocumentPath(), filePath, request.getQuestion());

            File pptFile = new File(filePath);

            PPTProgressiveAnalysisService.PPTAnalysisReport report =
                pptAnalysisService.analyzeProgressively(pptFile, request.getQuestion());

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("PPT分析失败", e);

            PPTProgressiveAnalysisService.PPTAnalysisReport errorReport =
                new PPTProgressiveAnalysisService.PPTAnalysisReport();
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

