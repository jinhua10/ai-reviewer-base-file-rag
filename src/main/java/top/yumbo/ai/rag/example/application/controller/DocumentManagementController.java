package top.yumbo.ai.rag.example.application.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.example.application.service.DocumentManagementService;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文档管理 REST API 控制器
 * 支持文档的上传、删除、列表查询等操作
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentManagementController {

    private final DocumentManagementService documentService;

    public DocumentManagementController(DocumentManagementService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public UploadResponse uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("收到文档上传请求: {}", file.getOriginalFilename());

        UploadResponse response = new UploadResponse();

        try {
            if (file.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("文件为空");
                return response;
            }

            String result = documentService.uploadDocument(file);

            response.setSuccess(true);
            response.setMessage("文档上传成功");
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setDocumentId(result);

            log.info("文档上传成功: {}", file.getOriginalFilename());
            return response;

        } catch (Exception e) {
            log.error("文档上传失败", e);
            response.setSuccess(false);
            response.setMessage("上传失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/upload-batch")
    public BatchUploadResponse uploadBatch(@RequestParam("files") MultipartFile[] files) {
        log.info("收到批量上传请求: {} 个文件", files.length);

        BatchUploadResponse response = new BatchUploadResponse();
        response.setTotal(files.length);

        int successCount = 0;
        int failureCount = 0;

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    documentService.uploadDocument(file);
                    successCount++;
                    response.getSuccessFiles().add(file.getOriginalFilename());
                }
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                failureCount++;
                response.getFailedFiles().add(file.getOriginalFilename());
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(String.format("成功: %d, 失败: %d", successCount, failureCount));

        return response;
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/list")
    public ListResponse listDocuments() {
        log.info("获取文档列表");

        try {
            List<DocumentInfo> documents = documentService.listDocuments();

            ListResponse response = new ListResponse();
            response.setSuccess(true);
            response.setTotal(documents.size());
            response.setDocuments(documents);

            return response;
        } catch (Exception e) {
            log.error("获取文档列表失败", e);

            ListResponse response = new ListResponse();
            response.setSuccess(false);
            response.setMessage("获取列表失败: " + e.getMessage());

            return response;
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{fileName}")
    public DeleteResponse deleteDocument(@PathVariable String fileName) {
        log.info("删除文档: {}", fileName);

        DeleteResponse response = new DeleteResponse();

        try {
            boolean deleted = documentService.deleteDocument(fileName);

            if (deleted) {
                response.setSuccess(true);
                response.setMessage("文档删除成功");
                response.setFileName(fileName);
            } else {
                response.setSuccess(false);
                response.setMessage("文档不存在");
            }

            return response;
        } catch (Exception e) {
            log.error("删除文档失败", e);
            response.setSuccess(false);
            response.setMessage("删除失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/batch")
    public BatchDeleteResponse deleteBatch(@RequestBody List<String> fileNames) {
        log.info("批量删除文档: {} 个", fileNames.size());

        BatchDeleteResponse response = new BatchDeleteResponse();
        response.setTotal(fileNames.size());

        int successCount = 0;
        int failureCount = 0;

        for (String fileName : fileNames) {
            try {
                if (documentService.deleteDocument(fileName)) {
                    successCount++;
                    response.getSuccessFiles().add(fileName);
                } else {
                    failureCount++;
                    response.getFailedFiles().add(fileName);
                }
            } catch (Exception e) {
                log.error("删除文档失败: {}", fileName, e);
                failureCount++;
                response.getFailedFiles().add(fileName);
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(String.format("成功: %d, 失败: %d", successCount, failureCount));

        return response;
    }

    /**
     * 下载单个文档
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String fileName) {
        log.info("下载文档: {}", fileName);

        try {
            Path filePath = documentService.getDocumentPath(fileName);
            
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("文件不可读: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(resource);

        } catch (Exception e) {
            log.error("下载文档失败: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 批量下载文档（打包为ZIP）
     */
    @PostMapping("/download-batch")
    public ResponseEntity<Resource> downloadBatch(@RequestBody List<String> fileNames) {
        log.info("批量下载文档: {} 个", fileNames.size());

        try {
            // 创建临时ZIP文件
            Path tempZipFile = Files.createTempFile("documents_", ".zip");
            
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZipFile.toFile()))) {
                for (String fileName : fileNames) {
                    try {
                        Path filePath = documentService.getDocumentPath(fileName);
                        
                        if (Files.exists(filePath)) {
                            ZipEntry zipEntry = new ZipEntry(fileName);
                            zipOut.putNextEntry(zipEntry);
                            
                            Files.copy(filePath, zipOut);
                            zipOut.closeEntry();
                            
                            log.debug("已添加到ZIP: {}", fileName);
                        } else {
                            log.warn("文件不存在，跳过: {}", fileName);
                        }
                    } catch (Exception e) {
                        log.error("添加文件到ZIP失败: {}", fileName, e);
                    }
                }
            }

            Resource resource = new UrlResource(tempZipFile.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String zipFileName = "documents_" + System.currentTimeMillis() + ".zip";
            String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            // 返回ZIP文件，并在传输完成后删除临时文件
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(new Resource() {
                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new FileInputStream(tempZipFile.toFile()) {
                                @Override
                                public void close() throws IOException {
                                    super.close();
                                    try {
                                        Files.deleteIfExists(tempZipFile);
                                        log.debug("临时ZIP文件已删除: {}", tempZipFile);
                                    } catch (IOException e) {
                                        log.warn("删除临时ZIP文件失败: {}", tempZipFile, e);
                                    }
                                }
                            };
                        }

                        @Override
                        public boolean exists() { return resource.exists(); }
                        @Override
                        public boolean isReadable() { return resource.isReadable(); }
                        @Override
                        public boolean isOpen() { return resource.isOpen(); }
                        @Override
                        public boolean isFile() { return resource.isFile(); }
                        @Override
                        public java.net.URL getURL() throws IOException { return resource.getURL(); }
                        @Override
                        public java.net.URI getURI() throws IOException { return resource.getURI(); }
                        @Override
                        public File getFile() throws IOException { return resource.getFile(); }
                        @Override
                        public long contentLength() throws IOException { return resource.contentLength(); }
                        @Override
                        public long lastModified() throws IOException { return resource.lastModified(); }
                        @Override
                        public Resource createRelative(String relativePath) throws IOException { return resource.createRelative(relativePath); }
                        @Override
                        public String getFilename() { return zipFileName; }
                        @Override
                        public String getDescription() { return resource.getDescription(); }
                    });

        } catch (Exception e) {
            log.error("批量下载失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== DTO 类 ==========

    @Data
    public static class UploadResponse {
        private boolean success;
        private String message;
        private String fileName;
        private long fileSize;
        private String documentId;
    }

    @Data
    public static class BatchUploadResponse {
        private int total;
        private int successCount;
        private int failureCount;
        private String message;
        private List<String> successFiles = new java.util.ArrayList<>();
        private List<String> failedFiles = new java.util.ArrayList<>();
    }

    @Data
    public static class ListResponse {
        private boolean success = true;
        private String message;
        private int total;
        private List<DocumentInfo> documents;
    }

    @Data
    public static class DocumentInfo {
        private String fileName;
        private long fileSize;
        private String fileType;
        private String uploadTime;
        private boolean indexed;
    }

    @Data
    public static class DeleteResponse {
        private boolean success;
        private String message;
        private String fileName;
    }

    @Data
    public static class BatchDeleteResponse {
        private int total;
        private int successCount;
        private int failureCount;
        private String message;
        private List<String> successFiles = new java.util.ArrayList<>();
        private List<String> failedFiles = new java.util.ArrayList<>();
    }
}

