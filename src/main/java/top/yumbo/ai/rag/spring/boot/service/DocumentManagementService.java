package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.controller.DocumentManagementController.DocumentInfo;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文档管理服务 / Document Management Service
 * 负责文档的上传、删除、列表管理等功能 / Responsible for document upload, delete, list management, etc.
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Service
public class DocumentManagementService {

    private final KnowledgeQAProperties properties;
    private final FileTrackingService fileTrackingService;
    private final Path documentsPath;

    // 支持的文件格式 / Supported file formats
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "xlsx", "xls", "docx", "doc", "pptx", "ppt", "pdf", "txt", "md", "html", "xml"
    );

    @Autowired
    public DocumentManagementService(KnowledgeQAProperties properties,
                                      FileTrackingService fileTrackingService) {
        this.properties = properties;
        this.fileTrackingService = fileTrackingService;

        // 获取文档路径
        String sourcePath = properties.getKnowledgeBase().getSourcePath();
        Path resolvedPath;

        // 处理 classpath 路径
        if (sourcePath.startsWith("classpath:")) {
            // 从 classpath 获取资源路径
            String resourcePath = sourcePath.substring("classpath:".length());
            try {
                var resource = getClass().getClassLoader().getResource(resourcePath);
                if (resource != null) {
                    Path tempPath = Paths.get(resource.toURI());
                    log.info(I18N.get("log.docs.classpath_resource_found", tempPath.toAbsolutePath()));

                    // 检查是否在 JAR 内
                    if (tempPath.toString().contains(".jar!")) {
                        log.warn(I18N.get("log.docs.classpath_in_jar"));
                        log.warn(I18N.get("log.docs.upload_to_external"));
                        resolvedPath = Paths.get("./data/documents");
                    } else {
                        // 开发环境，使用 classpath 的实际路径
                        resolvedPath = tempPath;
                        log.info(I18N.get("log.docs.classpath_realpath", resolvedPath.toAbsolutePath()));
                    }
                } else {
                    // 如果 classpath 资源不存在，使用默认路径
                    log.warn(I18N.get("log.docs.classpath_not_exists", resourcePath));
                    log.info(I18N.get("log.docs.using_default_path"));
                    resolvedPath = Paths.get("./data/documents");
                }
            } catch (Exception e) {
                log.warn(I18N.get("log.docs.classpath_load_failed", resourcePath, e.getMessage()));
                log.info(I18N.get("log.docs.using_default_path"));
                resolvedPath = Paths.get("./data/documents");
            }
        } else {
            // 使用文件系统路径
            resolvedPath = Paths.get(sourcePath);
            log.info(I18N.get("log.docs.using_filesystem", resolvedPath.toAbsolutePath()));
        }

        this.documentsPath = resolvedPath;

        // 确保目录存在
        try {
            Files.createDirectories(this.documentsPath);
            log.info(I18N.get("log.docs.directory_ready", this.documentsPath.toAbsolutePath()));
        } catch (IOException e) {
            log.error(I18N.get("log.docs.create_failed", e.getMessage()));
            throw new RuntimeException(I18N.get("document_service.error.cannot_create_dir", e.getMessage()), e);
        }
    }

    /**
     * 上传文档 / Upload document
     *
     * @param file 上传的文件 / File to upload
     * @return 文档ID / Document ID
     */
    public String uploadDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException(I18N.get("document_service.error.filename_empty"));
        }

        // 验证文件格式 / Validate file format
        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(I18N.get("document_service.error.unsupported_format", extension));
        }

        // 验证文件大小 / Validate file size
        long maxSize = properties.getDocument().getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            double fileSizeMB = file.getSize() / 1024.0 / 1024.0;
            throw new IllegalArgumentException(
                    I18N.get("document_service.error.file_too_large",
                            String.format("%.2f", fileSizeMB),
                            properties.getDocument().getMaxFileSizeMb())
            );
        }

        // 保存文件
        Path targetPath = documentsPath.resolve(originalFilename);

        // 如果文件已存在，添加时间戳
        if (Files.exists(targetPath)) {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String newFilename = nameWithoutExt + "_" + timestamp + "." + extension;
            targetPath = documentsPath.resolve(newFilename);
            log.info(I18N.get("log.docs.file_exists_renamed", newFilename));
        }

        // 使用 try-with-resources 确保流被正确关闭
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info(I18N.get("log.docs.saved", targetPath.getFileName()));

        return targetPath.getFileName().toString();
    }

    /**
     * 删除文档 / Delete document
     *
     * @param fileName 文件名 / File name
     * @return 是否删除成功 / Whether deletion was successful
     */
    public boolean deleteDocument(String fileName) throws IOException {
        Path filePath = documentsPath.resolve(fileName);

        if (!Files.exists(filePath)) {
            log.warn(I18N.get("log.docs.not_found", fileName));
            return false;
        }

        // 安全检查：确保文件在文档目录内 / Security check: ensure file is within document directory
        if (!filePath.normalize().startsWith(documentsPath.normalize())) {
            throw new SecurityException(I18N.get("document_service.error.illegal_path"));
        }

        Files.delete(filePath);
        log.info(I18N.get("log.docs.deleted", fileName));

        return true;
    }

    /**
     * 获取文档列表 / Get document list
     *
     * @return 文档列表 / Document list
     */
    public List<DocumentInfo> listDocuments() throws IOException {
        List<DocumentInfo> documents = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(documentsPath, 1)) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String extension = getFileExtension(path.getFileName().toString());
                        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
                    })
                    .toList();

            for (Path path : files) {
                DocumentInfo info = new DocumentInfo();
                info.setFileName(path.getFileName().toString());
                info.setFileSize(Files.size(path));
                info.setFileType(getFileExtension(path.getFileName().toString()));

                // 获取创建时间 / Get creation time
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                info.setUploadTime(sdf.format(new Date(attrs.creationTime().toMillis())));

                // 检查是否已索引 / Check if indexed
                // 通过 FileTrackingService 检查文件是否在追踪列表中
                boolean indexed = fileTrackingService.isIndexed(path.toAbsolutePath().toString());
                info.setIndexed(indexed);

                documents.add(info);
            }
        }

        // 按上传时间倒序排列 / Sort by upload time in descending order
        documents.sort((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()));

        return documents;
    }

    /**
     * 获取文件扩展名 / Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 获取文档目录路径 / Get documents directory path
     */
    public String getDocumentsPath() {
        return documentsPath.toAbsolutePath().toString();
    }

    /**
     * 获取指定文件的完整路径 / Get full path of specified file
     *
     * @param fileName 文件名 / File name
     * @return 文件完整路径 / Full file path
     */
    public Path getDocumentPath(String fileName) {
        Path filePath = documentsPath.resolve(fileName);

        // 安全检查：确保文件在文档目录内 / Security check: ensure file is within document directory
        if (!filePath.normalize().startsWith(documentsPath.normalize())) {
            throw new SecurityException(I18N.get("document_service.error.illegal_path"));
        }

        return filePath;
    }

    /**
     * 获取已上传文档的文件类型列表（动态扫描）/ Get list of file types from uploaded documents (dynamic scan)
     *
     * @return 实际已上传的文件扩展名列表（去重且排序）/ List of uploaded file extensions (deduplicated and sorted)
     */
    public List<String> getSupportedTypes() throws IOException {
        List<String> fileTypes = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(documentsPath, 1)) {
            fileTypes = paths
                    .filter(Files::isRegularFile)
                    .map(path -> getFileExtension(path.getFileName().toString()))
                    .filter(ext -> !ext.isEmpty() && SUPPORTED_EXTENSIONS.contains(ext.toLowerCase()))
                    .map(String::toLowerCase)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        log.debug(I18N.get("document_service.log.scanned_types", fileTypes));
        return fileTypes;
    }
}
