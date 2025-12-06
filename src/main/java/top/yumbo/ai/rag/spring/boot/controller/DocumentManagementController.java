package top.yumbo.ai.rag.spring.boot.controller;

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
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.service.DocumentManagementService;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
     * 上传文档 / Upload document
     */
    @PostMapping("/upload")
    public UploadResponse uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        log.info(I18N.get("document_management.log.batch_upload", file.getOriginalFilename()));

        UploadResponse response = new UploadResponse();

        try {
            if (file.isEmpty()) {
                response.setSuccess(false);
                response.setMessage(I18N.get("document_management.api.error.file_empty", lang));
                return response;
            }

            String result = documentService.uploadDocument(file);

            response.setSuccess(true);
            response.setMessage(I18N.get("document_management.api.success.upload", lang));
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setDocumentId(result);

            log.info(I18N.get("document_management.log.upload_success", file.getOriginalFilename()));
            return response;

        } catch (Exception e) {
            log.error(I18N.get("document_management.log.upload_failed"), e);
            response.setSuccess(false);
            response.setMessage(I18N.get("document_management.api.error.upload_failed", lang, e.getMessage()));
            return response;
        }
    }

    /**
     * 批量上传文档 / Batch upload documents
     */
    @PostMapping("/upload-batch")
    public BatchUploadResponse uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        log.info(I18N.get("document_management.log.batch_upload", files.length));

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
                log.error(I18N.get("document_management.log.file_upload_failed", file.getOriginalFilename()), e);
                failureCount++;
                response.getFailedFiles().add(file.getOriginalFilename());
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(I18N.get("document_management.api.success.batch_result", lang, successCount, failureCount));

        return response;
    }

    /**
     * 获取文档列表（支持分页、排序、高级搜索）/ Get document list (support pagination, sorting, advanced search)
     *
     * @param page 页码（从1开始），默认1 / Page number (from 1), default 1
     * @param pageSize 每页数量，默认20，-1表示全部 / Items per page, default 20, -1 for all
     * @param sortBy 排序字段：name, size, date, type，默认date / Sort field, default date
     * @param sortOrder 排序方向：asc, desc，默认desc / Sort direction, default desc
     * @param search 搜索关键词（简单搜索），默认空 / Search keyword, default empty
     * @param searchMode 搜索模式：contains(包含), exact(精确), regex(正则)，默认contains / Search mode, default contains
     * @param fileTypes 文件类型过滤，逗号分隔，如 "pdf,docx,xlsx"，空表示全部 / File type filter
     * @param minSize 最小文件大小（字节），默认0 / Min file size (bytes), default 0
     * @param maxSize 最大文件大小（字节），默认Long.MAX_VALUE / Max file size (bytes)
     * @param indexed 是否已索引：true, false, all，默认all / Indexed status
     * @param startDate 开始日期，格式：yyyy-MM-dd，默认空 / Start date, format: yyyy-MM-dd
     * @param endDate 结束日期，格式：yyyy-MM-dd，默认空 / End date, format: yyyy-MM-dd
     * @param lang 语言参数，默认zh / Language parameter, default zh
     */
    @GetMapping("/list")
    public ListResponse listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "contains") String searchMode,
            @RequestParam(defaultValue = "") String fileTypes,
            @RequestParam(defaultValue = "0") long minSize,
            @RequestParam(defaultValue = "9223372036854775807") long maxSize,
            @RequestParam(defaultValue = "all") String indexed,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate,
            @RequestParam(defaultValue = "zh") String lang) {

        log.info(I18N.get("document_management.log.list_documents",
            page, pageSize, sortBy, sortOrder));

        try {
            // 获取所有文档 / Get all documents
            List<DocumentInfo> allDocuments = documentService.listDocuments();

            // 1. 高级过滤 / Advanced filtering
            List<DocumentInfo> filteredDocuments = advancedFilter(
                    allDocuments, search, searchMode, fileTypes,
                    minSize, maxSize, indexed, startDate, endDate
            );

            // 2. 排序 / Sorting
            filteredDocuments = sortDocuments(filteredDocuments, sortBy, sortOrder);

            // 3. 分页 / Pagination
            int totalCount = filteredDocuments.size();
            List<DocumentInfo> paginatedDocuments;
            int totalPages;

            if (pageSize == -1) {
                // 显示全部 / Show all
                paginatedDocuments = filteredDocuments;
                totalPages = 1;
            } else {
                // 计算分页 / Calculate pagination
                totalPages = (int) Math.ceil((double) totalCount / pageSize);
                int startIndex = (page - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, totalCount);

                if (startIndex >= totalCount) {
                    paginatedDocuments = new java.util.ArrayList<>();
                } else {
                    paginatedDocuments = filteredDocuments.subList(startIndex, endIndex);
                }

                log.debug(I18N.get("doc_management.log.pagination",
                        page, pageSize, totalPages, paginatedDocuments.size()));
            }

            ListResponse response = new ListResponse();
            response.setSuccess(true);
            response.setTotal(totalCount);
            response.setDocuments(paginatedDocuments);
            response.setPage(page);
            response.setPageSize(pageSize);
            response.setTotalPages(totalPages);

            log.info(I18N.get("document_management.api.success.list_loaded",
                paginatedDocuments.size(), totalCount));
            return response;

        } catch (Exception e) {
            log.error(I18N.get("document_management.log.list_failed"), e);

            ListResponse response = new ListResponse();
            response.setSuccess(false);
            response.setMessage(I18N.get("document_management.api.error.list_failed", lang, e.getMessage()));

            return response;
        }
    }

    /**
     * 对文档列表进行排序
     */
    private List<DocumentInfo> sortDocuments(List<DocumentInfo> documents, String sortBy, String sortOrder) {
        List<DocumentInfo> sorted = new java.util.ArrayList<>(documents);

        java.util.Comparator<DocumentInfo> comparator;

        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = java.util.Comparator.comparing(DocumentInfo::getFileName,
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "size":
                comparator = java.util.Comparator.comparingLong(DocumentInfo::getFileSize);
                break;
            case "type":
                comparator = java.util.Comparator.comparing(DocumentInfo::getFileType,
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case "date":
            default:
                comparator = java.util.Comparator.comparing(DocumentInfo::getUploadTime);
                break;
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        sorted.sort(comparator);
        log.debug(I18N.get("doc_management.log.sort_complete", sortBy, sortOrder));

        return sorted;
    }

    /**
     * 高级过滤方法 - 支持多条件组合搜索
     */
    private List<DocumentInfo> advancedFilter(
            List<DocumentInfo> documents,
            String search,
            String searchMode,
            String fileTypes,
            long minSize,
            long maxSize,
            String indexed,
            String startDate,
            String endDate) {

        return documents.stream().filter(doc -> {
            // 1. 文件名搜索过滤
            if (search != null && !search.trim().isEmpty()) {
                boolean matchName = false;
                try {
                    switch (searchMode.toLowerCase()) {
                        case "exact":
                            // 精确匹配
                            matchName = doc.getFileName().equals(search);
                            break;
                        case "regex":
                            // 正则表达式匹配
                            matchName = doc.getFileName().matches(search);
                            break;
                        case "contains":
                        default:
                            // 包含匹配（默认）
                            matchName = doc.getFileName().toLowerCase().contains(search.toLowerCase());
                            break;
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("doc_management.log.search_mode_failed", searchMode, e.getMessage()));
                    matchName = doc.getFileName().toLowerCase().contains(search.toLowerCase());
                }
                if (!matchName) {
                    return false;
                }
            }

            // 2. 文件类型过滤
            if (fileTypes != null && !fileTypes.trim().isEmpty()) {
                String[] types = fileTypes.toLowerCase().split(",");
                boolean matchType = false;
                for (String type : types) {
                    if (doc.getFileType().toLowerCase().equals(type.trim())) {
                        matchType = true;
                        break;
                    }
                }
                if (!matchType) {
                    return false;
                }
            }

            // 3. 文件大小过滤
            if (doc.getFileSize() < minSize || doc.getFileSize() > maxSize) {
                return false;
            }

            // 4. 索引状态过滤
            if (!"all".equalsIgnoreCase(indexed)) {
                boolean isIndexed = Boolean.parseBoolean(indexed);
                if (doc.isIndexed() != isIndexed) {
                    return false;
                }
            }

            // 5. 日期范围过滤
            if ((startDate != null && !startDate.trim().isEmpty()) ||
                (endDate != null && !endDate.trim().isEmpty())) {
                try {
                    String docDate = doc.getUploadTime().substring(0, 10); // 提取 yyyy-MM-dd

                    if (startDate != null && !startDate.trim().isEmpty()) {
                        if (docDate.compareTo(startDate) < 0) {
                            return false;
                        }
                    }

                    if (endDate != null && !endDate.trim().isEmpty()) {
                        if (docDate.compareTo(endDate) > 0) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    log.warn(I18N.get("doc_management.log.date_filter_failed", e.getMessage()));
                }
            }

            return true;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 删除文档（Delete document）
     */
    @DeleteMapping("/{fileName}")
    public DeleteResponse deleteDocument(
            @PathVariable String fileName,
            @RequestParam(value = "lang", defaultValue = "zh") String lang) {
        log.info(I18N.get("doc_management.log.delete_document", fileName));

        DeleteResponse response = new DeleteResponse();

        try {
            boolean deleted = documentService.deleteDocument(fileName);

            if (deleted) {
                response.setSuccess(true);
                response.setMessage(I18N.get("document_management.api.success.delete", lang));
                response.setFileName(fileName);
            } else {
                response.setSuccess(false);
                response.setMessage(I18N.get("document_management.api.error.not_found", lang));
            }

            return response;
        } catch (Exception e) {
            log.error(I18N.get("doc_management.log.delete_failed"), e);
            response.setSuccess(false);
            response.setMessage(I18N.get("document_management.api.error.delete_failed", lang, e.getMessage()));
            return response;
        }
    }

    /**
     * 批量删除文档 / Batch delete documents
     */
    @DeleteMapping("/batch")
    public BatchDeleteResponse deleteBatch(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> fileNames = (List<String>) request.get("fileNames");
        String lang = (String) request.getOrDefault("lang", "zh"); // 获取语言参数 / Get language parameter

        log.info(I18N.get("document_management.log.batch_upload", fileNames.size()));

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
                log.error(I18N.get("document_management.log.file_upload_failed", fileName), e);
                failureCount++;
                response.getFailedFiles().add(fileName);
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(I18N.get("document_management.api.success.batch_result", lang, successCount, failureCount));

        return response;
    }

    /**
     * 下载单个文档（Download single document）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadDocument(@RequestParam("fileName") String fileName) {
        log.info(I18N.get("doc_management.log.download_document", fileName));
        log.debug(I18N.get("doc_management.log.filename_bytes", java.util.Arrays.toString(fileName.getBytes(StandardCharsets.UTF_8))));

        try {
            // URL解码已由Spring自动处理（URL decoding is handled by Spring automatically）
            Path filePath = documentService.getDocumentPath(fileName);
            log.debug(I18N.get("doc_management.log.find_path", filePath.toAbsolutePath()));

            if (!Files.exists(filePath)) {
                log.warn(I18N.get("doc_management.log.file_not_exists", fileName, filePath.toAbsolutePath()));
                log.warn(I18N.get("doc_management.log.possible_reasons"));
                log.warn(I18N.get("doc_management.log.reason_deleted"));
                log.warn(I18N.get("doc_management.log.reason_special_chars"));
                log.warn(I18N.get("doc_management.log.reason_not_uploaded"));
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn(I18N.get("doc_management.log.file_not_readable", fileName));
                return ResponseEntity.notFound().build();
            }

            // 设置响应头 - 使用RFC 5987编码方式（Set response header - using RFC 5987 encoding）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(resource);

        } catch (Exception e) {
            log.error(I18N.get("doc_management.log.download_failed", fileName), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 批量下载文档（打包为ZIP）（Batch download documents as ZIP）
     */
    @PostMapping("/download-batch")
    public ResponseEntity<Resource> downloadBatch(@RequestBody List<String> fileNames) {
        log.info(I18N.get("doc_management.log.batch_download", fileNames.size()));
        log.debug(I18N.get("doc_management.log.filename_list", fileNames));

        try {
            // 创建临时ZIP文件（Create temp ZIP file）
            Path tempZipFile = Files.createTempFile("documents_", ".zip");
            
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(tempZipFile.toFile()))) {
                for (String fileName : fileNames) {
                    try {
                        Path filePath = documentService.getDocumentPath(fileName);
                        log.debug(I18N.get("doc_management.log.find_file", fileName, filePath.toAbsolutePath()));

                        if (Files.exists(filePath)) {
                            ZipEntry zipEntry = new ZipEntry(fileName);
                            zipOut.putNextEntry(zipEntry);
                            
                            Files.copy(filePath, zipOut);
                            zipOut.closeEntry();
                            
                            log.debug(I18N.get("doc_management.log.added_to_zip", fileName));
                        } else {
                            log.warn(I18N.get("doc_management.log.file_not_exists_skip", fileName, filePath.toAbsolutePath()));
                        }
                    } catch (Exception e) {
                        log.error(I18N.get("doc_management.log.add_to_zip_failed", fileName), e);
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
                                        log.debug(I18N.get("doc_management.log.temp_zip_deleted", tempZipFile));
                                    } catch (IOException e) {
                                        log.warn(I18N.get("doc_management.log.delete_temp_zip_failed", tempZipFile), e);
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
            log.error(I18N.get("doc_management.log.batch_download_failed"), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取已上传文档的文件类型列表（动态扫描）（Get uploaded document file types list）
     */
    @GetMapping("/supported-types")
    public SupportedTypesResponse getSupportedTypes() {
        log.info(I18N.get("doc_management.log.get_file_types"));

        SupportedTypesResponse response = new SupportedTypesResponse();

        try {
            List<String> types = documentService.getSupportedTypes();
            response.setSuccess(true);
            response.setTypes(types);
            response.setMessage(I18N.get("doc_management.log.get_file_types_success", types.size()));
            response.setCount(types.size());
        } catch (Exception e) {
            log.error(I18N.get("doc_management.log.get_file_types_failed"), e);
            response.setSuccess(false);
            response.setMessage(I18N.get("doc_management.log.get_file_types_error", e.getMessage()));
            response.setTypes(new java.util.ArrayList<>());
            response.setCount(0);
        }

        return response;
    }

    // ========== DTO 类（DTO Classes）==========

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
        private int total;              // 总文档数（过滤后）
        private List<DocumentInfo> documents;
        private int page;               // 当前页码
        private int pageSize;           // 每页数量
        private int totalPages;         // 总页数
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

    @Data
    public static class SupportedTypesResponse {
        private boolean success;
        private String message;
        private List<String> types;
        private int count;  // 文件类型数量
    }
}


