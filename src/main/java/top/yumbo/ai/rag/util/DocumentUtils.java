package top.yumbo.ai.rag.util;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.parser.TikaDocumentParser;
import top.yumbo.ai.rag.model.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * 文档工具类
 * 提供文档创建和处理的便捷方法
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class DocumentUtils {

    private static final TikaDocumentParser parser = new TikaDocumentParser();

    /**
     * 从文件创建文档
     */
    public static Document fromFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        try {
            // 解析文件内容
            String content = parser.parse(file);

            // 检测MIME类型
            String mimeType = parser.detectMimeType(file);

            // 获取文件大小
            long fileSize = file.length();

            // 构建文档
            return Document.builder()
                    .title(file.getName())
                    .content(content)
                    .filePath(file.getAbsolutePath())
                    .fileSize(fileSize)
                    .mimeType(mimeType)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create document from file: {}", file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to create document from file", e);
        }
    }

    /**
     * 从Path创建文档
     */
    public static Document fromPath(Path path) {
        return fromFile(path.toFile());
    }

    /**
     * 从文本内容创建文档
     */
    public static Document fromText(String title, String content) {
        return Document.builder()
                .title(title)
                .content(content)
                .mimeType("text/plain")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 从字节数组创建文档
     */
    public static Document fromBytes(String title, byte[] bytes, String mimeType) {
        String content = parser.parse(bytes, mimeType);

        return Document.builder()
                .title(title)
                .content(content)
                .fileSize((long) bytes.length)
                .mimeType(mimeType)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 批量从目录创建文档
     */
    public static java.util.List<Document> fromDirectory(File directory) {
        return fromDirectory(directory, true);
    }

    /**
     * 批量从目录创建文档
     *
     * @param directory 目录
     * @param recursive 是否递归
     */
    public static java.util.List<Document> fromDirectory(File directory, boolean recursive) {
        java.util.List<Document> documents = new java.util.ArrayList<>();

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            log.warn("Invalid directory: {}", directory);
            return documents;
        }

        try {
            if (recursive) {
                Files.walk(directory.toPath())
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Document doc = fromPath(path);
                                documents.add(doc);
                            } catch (Exception e) {
                                log.error("Failed to process file: {}", path, e);
                            }
                        });
            } else {
                File[] files = directory.listFiles(File::isFile);
                if (files != null) {
                    for (File file : files) {
                        try {
                            Document doc = fromFile(file);
                            documents.add(doc);
                        } catch (Exception e) {
                            log.error("Failed to process file: {}", file, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to walk directory: {}", directory, e);
        }

        return documents;
    }

    /**
     * 检查文件是否支持解析
     */
    public static boolean isSupported(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String extension = getFileExtension(file.getName());
        return parser.supportsExtension(extension);
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }
}

