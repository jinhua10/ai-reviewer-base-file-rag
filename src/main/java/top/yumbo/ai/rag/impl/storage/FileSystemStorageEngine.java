package top.yumbo.ai.rag.impl.storage;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.RAGConfiguration;
import top.yumbo.ai.rag.core.StorageEngine;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 文件系统存储引擎实现
 * 将文档存储在本地文件系统中，按日期分区组织
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class FileSystemStorageEngine implements StorageEngine {

    private final String basePath;
    private final boolean compressionEnabled;
    private final SHA256DocumentHasher hasher;
    private final SQLiteMetadataManager metadataManager;

    public FileSystemStorageEngine(RAGConfiguration.StorageConfig config) {
        this.basePath = config.getBasePath();
        this.compressionEnabled = config.isCompression();
        this.hasher = new SHA256DocumentHasher();

        // 初始化目录结构
        initializeDirectories();

        // 初始化元数据管理器
        String dbPath = Paths.get(basePath, "metadata", "metadata.db").toString();
        this.metadataManager = new SQLiteMetadataManager(dbPath);

        log.info(I18N.get("storage_engine.log.initialized", basePath));
    }

    /**
     * 初始化目录结构
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(basePath, "documents"));
            Files.createDirectories(Paths.get(basePath, "metadata"));
            Files.createDirectories(Paths.get(basePath, "cache"));
        } catch (IOException e) {
            log.error(I18N.get("storage_engine.log.failed_init_dirs"), e);
            throw new RuntimeException(I18N.get("storage_engine.error.failed_init_storage_dirs"), e);
        }
    }

    @Override
    public String store(Document document) {
        try {
            // 1. 生成文档ID（如果没有）
            if (document.getId() == null) {
                document.setId(UUID.randomUUID().toString());
            }

            // 2. 计算内容哈希
            String hash = hasher.computeHash(document.getContent());
            document.setContentHash(hash);

            // 3. 检查是否已存在相同内容的文档
            List<Document> duplicates = metadataManager.findByHash(hash);
            if (!duplicates.isEmpty()) {
                log.info(I18N.get("storage_engine.log.document_with_same_content", duplicates.get(0).getId()));
                return duplicates.get(0).getId();
            }

            // 4. 确定存储路径
            String storagePath = getStoragePath(document);
            Path filePath = Paths.get(basePath, "documents", storagePath);
            Files.createDirectories(filePath.getParent());

            // 5. 写入文件内容
            if (compressionEnabled) {
                writeCompressed(filePath, document.getContent());
            } else {
                Files.writeString(filePath, document.getContent());
            }

            // 6. 更新文档信息
            document.setFilePath(storagePath);
            document.setFileSize((long) document.getContent().length());
            document.setUpdatedAt(Instant.now());

            // 7. 保存元数据
            metadataManager.save(document);

            log.debug(I18N.get("storage_engine.log.document_stored", document.getId()));
            return document.getId();

        } catch (Exception e) {
            log.error(I18N.get("storage_engine.log.failed_store"), e);
            throw new RuntimeException(I18N.get("storage_engine.error.failed_store_document"), e);
        }
    }

    @Override
    public int storeBatch(List<Document> documents) {
        int count = 0;
        for (Document doc : documents) {
            try {
                store(doc);
                count++;
            } catch (Exception e) {
                log.error(I18N.get("storage_engine.log.failed_store_batch", doc.getId()), e);
            }
        }
        return count;
    }

    @Override
    public Document retrieve(String id) {
        try {
            // 1. 从元数据获取文档信息
            Document document = metadataManager.get(id);
            if (document == null) {
                return null;
            }

            // 2. 读取文件内容
            Path filePath = Paths.get(basePath, "documents", document.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn(I18N.get("storage_engine.log.document_file_not_found", filePath));
                return null;
            }

            String content;
            if (compressionEnabled) {
                content = readCompressed(filePath);
            } else {
                content = Files.readString(filePath);
            }

            document.setContent(content);
            return document;

        } catch (Exception e) {
            log.error(I18N.get("storage_engine.log.failed_retrieve", id), e);
            throw new RuntimeException(I18N.get("storage_engine.error.failed_retrieve_document", id), e);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            // 1. 获取文档信息
            Document document = metadataManager.get(id);
            if (document == null) {
                return false;
            }

            // 2. 删除文件
            Path filePath = Paths.get(basePath, "documents", document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // 3. 删除元数据
            metadataManager.delete(id);

            log.debug(I18N.get("storage_engine.log.document_deleted", id));
            return true;

        } catch (Exception e) {
            log.error(I18N.get("storage_engine.log.failed_delete", id), e);
            return false;
        }
    }

    @Override
    public boolean update(String id, Document document) {
        try {
            // 1. 检查文档是否存在
            if (!exists(id)) {
                return false;
            }

            // 2. 删除旧文档
            delete(id);

            // 3. 存储新文档（保持原ID）
            document.setId(id);
            document.setUpdatedAt(Instant.now());
            store(document);

            log.debug(I18N.get("storage_engine.log.document_updated", id));
            return true;

        } catch (Exception e) {
            log.error(I18N.get("storage_engine.error.failed_delete_document", id), e);
            return false;
        }
    }

    @Override
    public Stream<Document> listAll() {
        List<Document> documents = metadataManager.listAll();
        return documents.stream().map(doc -> {
            try {
                return retrieve(doc.getId());
            } catch (Exception e) {
                log.error(I18N.get("storage_engine.log.failed_retrieve", doc.getId()), e);
                return null;
            }
        }).filter(doc -> doc != null);
    }

    @Override
    public Stream<Document> list(Query query) {
        // 简单实现：先获取所有文档，然后根据查询条件过滤
        return listAll()
                .filter(doc -> matchesQuery(doc, query))
                .limit(query.getLimit());
    }

    @Override
    public long count() {
        return metadataManager.count();
    }

    @Override
    public List<String> getAllDocumentIds() {
        return metadataManager.getAllDocumentIds();
    }

    @Override
    public boolean exists(String id) {
        return metadataManager.exists(id);
    }

    @Override
    public void clear() {
        try {
            // 1. 清空元数据
            metadataManager.clear();

            // 2. 清空文档目录
            Path documentsPath = Paths.get(basePath, "documents");
            if (Files.exists(documentsPath)) {
                Files.walk(documentsPath)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error(I18N.get("storage_engine.log.failed_delete", path.toString()), e);
                            }
                        });
            }

            log.info(I18N.get("storage_engine.log.storage_cleared"));
        } catch (Exception e) {
            log.error(I18N.get("storage_engine.log.clear_storage_failed"), e);
            throw new RuntimeException(I18N.get("storage_engine.error.clear_storage"), e);
        }
    }

    /**
     * 获取存储路径（按日期分区）
     */
    private String getStoragePath(Document document) {
        LocalDate date = document.getCreatedAt() != null ?
                LocalDate.ofInstant(document.getCreatedAt(), java.time.ZoneId.systemDefault()) :
                LocalDate.now();

        String extension = compressionEnabled ? ".txt.gz" : ".txt";
        return String.format("%d/%02d/%02d/%s%s",
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                document.getId(),
                extension);
    }

    /**
     * 写入压缩文件
     */
    private void writeCompressed(Path path, String content) throws IOException {
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(path))) {
            gzipOut.write(content.getBytes());
        }
    }

    /**
     * 读取压缩文件
     */
    private String readCompressed(Path path) throws IOException {
        try (GZIPInputStream gzipIn = new GZIPInputStream(Files.newInputStream(path))) {
            return new String(gzipIn.readAllBytes());
        }
    }

    /**
     * 检查文档是否匹配查询条件
     */
    private boolean matchesQuery(Document doc, Query query) {
        // 简单的过滤逻辑
        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (var entry : query.getFilters().entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();

                if ("category".equals(field) && !value.equals(doc.getCategory())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 关闭资源
     */
    public void close() {
        metadataManager.close();
    }
}

