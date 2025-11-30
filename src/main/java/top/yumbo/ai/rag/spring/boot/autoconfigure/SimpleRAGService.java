package top.yumbo.ai.rag.spring.boot.autoconfigure;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.Query;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简易 RAG 服务（Simple RAG service）
 * 提供常用的 RAG 操作（Provides common RAG operations）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class SimpleRAGService {

    /**
     * -- GETTER --
     *  获取底层 RAG 实例（高级用法）
     */
    @Getter
    private final LocalFileRAG rag;
    private final LocalFileRAGProperties properties;

    public SimpleRAGService(LocalFileRAG rag, LocalFileRAGProperties properties) {
        this.rag = rag;
        this.properties = properties;
    }

    /**
     * 索引文本内容
     */
    public String index(String title, String content) {
        return index(title, content, Map.of());
    }

    /**
     * 索引文本内容（带元数据）
     */
    public String index(String title, String content, Map<String, Object> metadata) {
        Document doc = Document.builder()
            .title(title)
            .content(content)
            .metadata(metadata)
            .build();

        String docId = rag.index(doc);
        log.debug(LogMessageProvider.getMessage("log.simple.doc_indexed", title, docId));

        return docId;
    }

    /**
     * 索引文件
     */
    public String indexFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.file.not_exists", String.valueOf(file)));
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.file.not_a_file", String.valueOf(file)));
        }

        try {
            // 使用 Tika 解析文件内容
            TikaDocumentParser parser = new TikaDocumentParser();
            String content = parser.parse(file);

            if (content == null || content.trim().isEmpty()) {
                log.warn(LogMessageProvider.getMessage("log.simple.file_empty", file.getName()));
                throw new IllegalArgumentException(LogMessageProvider.getMessage("error.file.empty", file.getName()));
            }

            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("file_path", file.getAbsolutePath());
            metadata.put("file_name", file.getName());
            metadata.put("file_size", file.length());
            metadata.put("file_type", getFileExtension(file));
            metadata.put("last_modified", new Date(file.lastModified()));

            // 索引文档
            String docId = index(file.getName(), content, metadata);
            log.info(LogMessageProvider.getMessage("log.simple.index_file", file.getName(), docId, file.length()));

            return docId;

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.simple.index_file_failed", file.getName()), e);
            throw new RuntimeException(LogMessageProvider.getMessage("error.file.index_failed", file.getName()), e);
        }
    }

    /**
     * 批量索引文件
     */
    public int indexFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        for (File file : files) {
            try {
                indexFile(file);
                successCount++;
            } catch (Exception e) {
                log.error(LogMessageProvider.getMessage("log.simple.index_file_failed", file.getName()), e);
                failCount++;
            }
        }

        log.info(LogMessageProvider.getMessage("log.simple.batch_index_files_complete", successCount, failCount));
        return successCount;
    }

    /**
     * 索引目录下的所有文件
     */
    public int indexDirectory(File directory, boolean recursive) {
        if (directory == null || !directory.exists()) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.file.directory_not_exists", String.valueOf(directory)));
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(LogMessageProvider.getMessage("error.file.not_a_directory", String.valueOf(directory)));
        }

        List<File> files = new ArrayList<>();
        collectFiles(directory, files, recursive);

        log.info(LogMessageProvider.getMessage("log.simple.scanned_files", files.size()));
        return indexFiles(files);
    }

    /**
     * 递归收集文件
     */
    private void collectFiles(File directory, List<File> files, boolean recursive) {
        File[] listFiles = directory.listFiles();
        if (listFiles == null) {
            return;
        }

        for (File file : listFiles) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory() && recursive) {
                collectFiles(file, files, recursive);
            }
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 批量索引
     */
    public int indexBatch(List<Document> documents) {
        int count = rag.indexBatch(documents);
        log.info(LogMessageProvider.getMessage("log.simple.batch_indexed", count));
        return count;
    }

    /**
     * 搜索
     */
    public List<Document> search(String queryText) {
        return search(queryText, properties.getSearch().getDefaultLimit());
    }

    /**
     * 搜索（指定数量）
     */
    public List<Document> search(String queryText, int limit) {
        SearchResult result = rag.search(Query.builder()
            .queryText(queryText)
            .limit(Math.min(limit, properties.getSearch().getMaxLimit()))
            .build());

        log.debug(LogMessageProvider.getMessage("log.simple.search_results", queryText, result.getDocuments().size()));

        // 从ScoredDocument中提取Document
        return result.getDocuments().stream()
            .map(ScoredDocument::getDocument)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取文档
     */
    public Document getDocument(String docId) {
        return rag.getDocument(docId);
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        return rag.deleteDocument(docId);
    }

    /**
     * 提交更改
     */
    public void commit() {
        rag.commit();
        log.debug(LogMessageProvider.getMessage("log.simple.commit"));
    }

    /**
     * 优化索引
     */
    public void optimize() {
        rag.optimizeIndex();
        log.info(LogMessageProvider.getMessage("log.simple.optimized"));
    }

    /**
     * 获取统计信息
     */
    public LocalFileRAG.Statistics getStatistics() {
        return rag.getStatistics();
    }

    @PreDestroy
    public void cleanup() {
        log.info(LogMessageProvider.getMessage("log.simple.shutdown"));
        rag.close();
    }
}
