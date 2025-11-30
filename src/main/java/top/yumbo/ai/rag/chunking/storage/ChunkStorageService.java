package top.yumbo.ai.rag.chunking.storage;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 文档块存储服务
 * 负责将切分后的文档块持久化到文件系统
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class ChunkStorageService {

    private final String storageBasePath;
    private static final String CHUNK_DIR = "chunks";
    private static final String METADATA_SUFFIX = ".meta.json";
    private static final String CONTENT_SUFFIX = ".md";

    public ChunkStorageService(String storageBasePath) {
        this.storageBasePath = storageBasePath;
        initializeStorage();
    }

    /**
     * 初始化存储目录
     */
    private void initializeStorage() {
        try {
            Path chunkPath = Paths.get(storageBasePath, CHUNK_DIR);
            if (!Files.exists(chunkPath)) {
                Files.createDirectories(chunkPath);
                log.info(LogMessageProvider.getMessage("log.chunk.storage.created", chunkPath.toString()));
            }
        } catch (IOException e) {
            log.error(LogMessageProvider.getMessage("log.chunk.storage.init_failed"), e);
            throw new RuntimeException("Failed to initialize chunk storage", e);
        }
    }

    /**
     * 保存文档块
     *
     * @param documentId 文档ID（原始文档名）
     * @param chunks 文档块列表
     * @return 保存的文件路径列表
     */
    public List<ChunkStorageInfo> saveChunks(String documentId, List<DocumentChunk> chunks) {
        List<ChunkStorageInfo> storageInfos = new ArrayList<>();

        // 清理文档ID，移除特殊字符
        String cleanDocId = sanitizeFilename(documentId);

        for (DocumentChunk chunk : chunks) {
            try {
                ChunkStorageInfo info = saveChunk(cleanDocId, chunk);
                storageInfos.add(info);
            } catch (Exception e) {
                log.error(LogMessageProvider.getMessage("log.chunk.save_failed", documentId, chunk.getIndex()), e);
            }
        }

        log.info(LogMessageProvider.getMessage("log.chunk.saved", storageInfos.size(), documentId));
        return storageInfos;
    }

    /**
     * 保存单个文档块
     */
    private ChunkStorageInfo saveChunk(String documentId, DocumentChunk chunk) throws IOException {
        // 生成文件名
        String chunkId = generateChunkId(documentId, chunk);
        String contentFile = chunkId + CONTENT_SUFFIX;
        String metadataFile = chunkId + METADATA_SUFFIX;

        Path chunkDir = Paths.get(storageBasePath, CHUNK_DIR, documentId);
        if (!Files.exists(chunkDir)) {
            Files.createDirectories(chunkDir);
        }

        Path contentPath = chunkDir.resolve(contentFile);
        Path metadataPath = chunkDir.resolve(metadataFile);

        // 保存内容（Markdown格式）
        String content = formatChunkContent(chunk);
        Files.writeString(contentPath, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 保存元数据（JSON格式）
        String metadata = formatChunkMetadata(documentId, chunk);
        Files.writeString(metadataPath, metadata, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return ChunkStorageInfo.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .chunkIndex(chunk.getIndex())
                .title(chunk.getTitle())
                .contentPath(contentPath.toString())
                .metadataPath(metadataPath.toString())
                .contentLength(chunk.getLength())
                .build();
    }

    /**
     * 生成文档块ID
     */
    private String generateChunkId(String documentId, DocumentChunk chunk) {
        // 格式：文档ID_块序号_标题（如果有）
        StringBuilder id = new StringBuilder();
        id.append(documentId);
        id.append("_chunk_");
        id.append(String.format("%03d", chunk.getIndex() + 1)); // 从001开始

        if (chunk.getTitle() != null && !chunk.getTitle().isEmpty()) {
            String cleanTitle = sanitizeFilename(chunk.getTitle());
            if (cleanTitle.length() > 30) {
                cleanTitle = cleanTitle.substring(0, 30);
            }
            id.append("_").append(cleanTitle);
        }

        return id.toString();
    }

    /**
     * 格式化文档块内容为 Markdown
     */
    private String formatChunkContent(DocumentChunk chunk) {
        StringBuilder md = new StringBuilder();

        // 添加标题
        if (chunk.getTitle() != null && !chunk.getTitle().isEmpty()) {
            md.append("# ").append(chunk.getTitle()).append("\n\n");
        } else {
            md.append("# 文档块 ").append(chunk.getIndex() + 1).append("\n\n");
        }

        // 添加元信息
        md.append("> **块信息**: ");
        md.append("第 ").append(chunk.getIndex() + 1);
        md.append("/").append(chunk.getTotalChunks()).append(" 块");
        if (chunk.getMetadata() != null) {
            md.append(" | 标签: ").append(chunk.getMetadata());
        }
        md.append("\n\n");
        md.append("---\n\n");

        // 添加内容
        md.append(chunk.getContent());

        return md.toString();
    }

    /**
     * 格式化文档块元数据为 JSON
     */
    private String formatChunkMetadata(String documentId, DocumentChunk chunk) {
        return String.format("""
            {
              "documentId": "%s",
              "chunkIndex": %d,
              "totalChunks": %d,
              "title": "%s",
              "startPosition": %d,
              "endPosition": %d,
              "contentLength": %d,
              "metadata": "%s",
              "createdAt": "%s"
            }
            """,
            documentId,
            chunk.getIndex(),
            chunk.getTotalChunks(),
            escapeJson(chunk.getTitle() != null ? chunk.getTitle() : ""),
            chunk.getStartPosition(),
            chunk.getEndPosition(),
            chunk.getLength(),
            escapeJson(chunk.getMetadata() != null ? chunk.getMetadata() : ""),
            java.time.LocalDateTime.now()
        );
    }

    /**
     * 读取文档块内容
     */
    public String readChunkContent(String chunkId, String documentId) throws IOException {
        Path contentPath = Paths.get(storageBasePath, CHUNK_DIR, documentId, chunkId + CONTENT_SUFFIX);

        if (!Files.exists(contentPath)) {
            throw new IOException("Chunk not found: " + chunkId);
        }

        return Files.readString(contentPath, StandardCharsets.UTF_8);
    }

    /**
     * 列出文档的所有块
     */
    public List<ChunkStorageInfo> listChunks(String documentId) throws IOException {
        Path docDir = Paths.get(storageBasePath, CHUNK_DIR, sanitizeFilename(documentId));

        if (!Files.exists(docDir)) {
            return List.of();
        }

        List<ChunkStorageInfo> chunks = new ArrayList<>();

        try (Stream<Path> stream = Files.list(docDir)) {
            stream
                .filter(p -> p.toString().endsWith(METADATA_SUFFIX))
                .forEach(metaPath -> {
                    try {
                        String metadata = Files.readString(metaPath, StandardCharsets.UTF_8);
                        // 简单解析（实际应使用 JSON 库）
                        ChunkStorageInfo info = parseMetadata(metadata);
                        chunks.add(info);
                    } catch (Exception e) {
                        log.warn(LogMessageProvider.getMessage("log.chunk.read_meta_failed", metaPath.toString()), e);
                    }
                });
        }

        chunks.sort(Comparator.comparingInt(ChunkStorageInfo::getChunkIndex));
        return chunks;
    }

    /**
     * 删除文档的所有块
     */
    @SuppressWarnings("unused")
    public void deleteChunks(String documentId) throws IOException {
        Path docDir = Paths.get(storageBasePath, CHUNK_DIR, sanitizeFilename(documentId));

        if (Files.exists(docDir)) {
            try (Stream<Path> stream = Files.walk(docDir)) {
                stream
                    .sorted(Comparator.reverseOrder()) // 反向排序，先删除文件再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn(LogMessageProvider.getMessage("log.chunk.delete_failed", path.toString()), e);
                        }
                    });
            }

            log.info(LogMessageProvider.getMessage("log.chunk.deleted_all", documentId));
        }
    }

    /**
     * 清理文件名，移除特殊字符
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }

        // 移除或替换特殊字符
        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .trim();
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * 解析元数据（简化版）
     */
    private ChunkStorageInfo parseMetadata(String json) {
        // 简化的 JSON 解析（实际应使用 Jackson）
        ChunkStorageInfo.ChunkStorageInfoBuilder builder = ChunkStorageInfo.builder();

        // 提取 documentId
        int docIdStart = json.indexOf("\"documentId\": \"") + 15;
        int docIdEnd = json.indexOf("\"", docIdStart);
        if (docIdStart > 14 && docIdEnd > docIdStart) {
            builder.documentId(json.substring(docIdStart, docIdEnd));
        }

        // 提取 chunkIndex
        int indexStart = json.indexOf("\"chunkIndex\": ") + 14;
        int indexEnd = json.indexOf(",", indexStart);
        if (indexStart > 13 && indexEnd > indexStart) {
            try {
                builder.chunkIndex(Integer.parseInt(json.substring(indexStart, indexEnd).trim()));
            } catch (NumberFormatException e) {
                builder.chunkIndex(0);
            }
        }

        // 提取 title
        int titleStart = json.indexOf("\"title\": \"") + 10;
        int titleEnd = json.indexOf("\"", titleStart);
        if (titleStart > 9 && titleEnd > titleStart) {
            builder.title(json.substring(titleStart, titleEnd));
        }

        return builder.build();
    }
}
