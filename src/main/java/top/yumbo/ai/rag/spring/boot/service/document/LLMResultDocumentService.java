package top.yumbo.ai.rag.spring.boot.service.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 结果文档化服务
 *
 * 职责：
 * 1. 将 LLM 分析结果持久化为文档（Markdown/PDF）
 * 2. 支持在线预览和下载
 * 3. 自动添加到知识库（可选）
 * 4. 图片处理：在线预览用链接，下载时转 Base64
 * 5. ✅ 持久化历史记录，服务重启后自动恢复
 */
@Slf4j
@Service
public class LLMResultDocumentService {

    @Value("${knowledge.qa.llm-result.storage-path:./data/llm-results}")
    private String storagePath;

    @Value("${knowledge.qa.llm-result.auto-add-to-knowledge-base:false}")
    private boolean autoAddToKnowledgeBase;

    @Value("${knowledge.qa.llm-result.max-history:100}")
    private int maxHistory;

    /** 结果历史记录 */
    private final LinkedList<LLMResultDocument> resultHistory = new LinkedList<>();

    /** 元数据文件名 */
    private static final String METADATA_FILE = "history-metadata.json";

    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;

    /** 图片 URL 匹配正则 */
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "!\\[([^\\]]*)\\]\\((https?://[^)]+)\\)"
    );

    /** 本地图片路径匹配正则 */
    private static final Pattern IMAGE_LOCAL_PATTERN = Pattern.compile(
            "!\\[([^\\]]*)\\]\\(([^)]+)\\)"
    );

    public LLMResultDocumentService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 服务启动时加载历史记录
     */
    @PostConstruct
    public void init() {
        loadHistoryFromDisk();
    }

    /**
     * 从磁盘加载历史记录
     */
    private void loadHistoryFromDisk() {
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("📁 创建 LLM 结果存储目录: {}", storageDir.toAbsolutePath());
                return;
            }

            Path metadataPath = storageDir.resolve(METADATA_FILE);

            if (Files.exists(metadataPath)) {
                // 从元数据文件加载
                String json = Files.readString(metadataPath);
                List<LLMResultDocument> loaded = objectMapper.readValue(json,
                        new TypeReference<List<LLMResultDocument>>() {});

                synchronized (resultHistory) {
                    resultHistory.clear();

                    // 验证文件是否存在，只加载有效记录
                    for (LLMResultDocument doc : loaded) {
                        if (doc.getFilePath() != null && Files.exists(Paths.get(doc.getFilePath()))) {
                            resultHistory.add(doc);
                        } else {
                            log.warn("⚠️ 跳过无效记录（文件不存在）: {}", doc.getFileName());
                        }
                    }
                }

                log.info("✅ 从磁盘加载了 {} 条 LLM 分析历史记录", resultHistory.size());
            } else {
                // 元数据文件不存在，尝试从现有 .md 文件恢复
                rebuildHistoryFromFiles(storageDir);
            }

        } catch (Exception e) {
            log.error("❌ 加载 LLM 分析历史失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从现有 .md 文件重建历史记录
     */
    private void rebuildHistoryFromFiles(Path storageDir) {
        try {
            List<Path> mdFiles = Files.list(storageDir)
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .limit(maxHistory)
                    .toList();

            synchronized (resultHistory) {
                resultHistory.clear();

                for (Path mdFile : mdFiles) {
                    try {
                        LLMResultDocument doc = parseDocumentFromFile(mdFile);
                        if (doc != null) {
                            resultHistory.add(doc);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ 解析文件失败: {}", mdFile.getFileName());
                    }
                }
            }

            if (!resultHistory.isEmpty()) {
                log.info("📂 从现有文件重建了 {} 条历史记录", resultHistory.size());
                saveHistoryToDisk(); // 保存元数据
            }

        } catch (IOException e) {
            log.warn("⚠️ 扫描存储目录失败: {}", e.getMessage());
        }
    }

    /**
     * 从 Markdown 文件解析文档信息
     */
    private LLMResultDocument parseDocumentFromFile(Path mdFile) throws IOException {
        String content = Files.readString(mdFile);
        String fileName = mdFile.getFileName().toString().replace(".md", "");

        // 提取元信息
        String sourceDocument = extractMetaValue(content, "源文档");
        String question = extractMetaValue(content, "分析问题");
        String analysisType = extractMetaValue(content, "分析类型");

        // 生成 ID
        String docId = "llm-" + fileName.hashCode() + "-" +
                       UUID.randomUUID().toString().substring(0, 4);

        return LLMResultDocument.builder()
                .id(docId)
                .fileName(fileName)
                .filePath(mdFile.toAbsolutePath().toString())
                .sourceDocument(sourceDocument)
                .question(question)
                .analysisType(analysisType != null ? analysisType : "未知")
                .summary(extractSummary(content, 200))
                .createdAt(LocalDateTime.now()) // 可以从文件名解析
                .contentLength(content.length())
                .hasImages(containsImages(content))
                .build();
    }

    /**
     * 从内容中提取元信息值
     */
    private String extractMetaValue(String content, String key) {
        Pattern pattern = Pattern.compile("\\*\\*" + key + "\\*\\*:\\s*(.+?)\\s*(?:\\n|$)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 保存历史记录到磁盘
     */
    private void saveHistoryToDisk() {
        try {
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            Path metadataPath = storageDir.resolve(METADATA_FILE);

            synchronized (resultHistory) {
                String json = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(new ArrayList<>(resultHistory));
                Files.writeString(metadataPath, json);
            }

            log.debug("💾 历史记录已保存到磁盘: {} 条", resultHistory.size());

        } catch (Exception e) {
            log.error("❌ 保存历史记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存 LLM 分析结果
     *
     * @param result LLM 分析结果
     * @return 文档信息
     */
    public LLMResultDocument saveResult(LLMAnalysisResult result) {
        try {
            // 生成文档ID和文件名
            String docId = generateDocId();
            String fileName = generateFileName(result);

            // 确保存储目录存在
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            // 生成 Markdown 内容
            String markdownContent = generateMarkdown(result, false);

            // 保存文件
            Path filePath = storageDir.resolve(fileName + ".md");
            Files.writeString(filePath, markdownContent);

            // 创建文档对象
            LLMResultDocument document = LLMResultDocument.builder()
                    .id(docId)
                    .fileName(fileName)
                    .filePath(filePath.toString())
                    .sourceDocument(result.getSourceDocument())
                    .question(result.getQuestion())
                    .analysisType(result.getAnalysisType())
                    .summary(extractSummary(result.getContent(), 200))
                    .createdAt(LocalDateTime.now())
                    .contentLength(markdownContent.length())
                    .hasImages(containsImages(result.getContent()))
                    .build();

            // 添加到历史记录
            addToHistory(document);

            log.info(I18N.get("llm_result.log.result_saved", docId, filePath));

            // 可选：自动添加到知识库
            if (autoAddToKnowledgeBase) {
                addToKnowledgeBase(document, markdownContent);
            }

            return document;

        } catch (Exception e) {
            log.error(I18N.get("llm_result.log.save_failed"), e);
            throw new RuntimeException(I18N.get("llm_result.error.save_failed", e.getMessage()), e);
        }
    }

    /**
     * 获取 Markdown 内容（用于在线预览，图片保持链接形式）
     */
    public String getMarkdownForPreview(String docId) {
        LLMResultDocument doc = findById(docId);
        if (doc == null) {
            return null;
        }

        try {
            return Files.readString(Paths.get(doc.getFilePath()));
        } catch (IOException e) {
            log.error(I18N.get("llm_result.log.read_failed", docId), e);
            return null;
        }
    }

    /**
     * 获取 Markdown 内容（用于下载，图片转 Base64）
     */
    public String getMarkdownForDownload(String docId) {
        String content = getMarkdownForPreview(docId);
        if (content == null) {
            return null;
        }

        // 将图片链接转换为 Base64
        return convertImagesToBase64(content);
    }

    /**
     * 获取 PDF 内容
     *
     * 注意：PDF 生成已改为前端处理（使用 html2pdf.js）
     * 此方法保留用于向后兼容，返回 Markdown 内容的字节数组
     * 前端获取后可自行转换为 PDF
     *
     * @deprecated 推荐使用前端 html2pdf.js 生成 PDF
     */
    @Deprecated
    public byte[] getPdfForDownload(String docId) {
        String markdown = getMarkdownForDownload(docId);
        if (markdown == null) {
            return null;
        }
        // 返回 Markdown 内容，前端负责转换为 PDF
        return markdown.getBytes();
    }

    /**
     * 获取历史记录
     */
    public List<LLMResultDocument> getHistory(int limit) {
        synchronized (resultHistory) {
            int size = Math.min(limit, resultHistory.size());
            return new ArrayList<>(resultHistory.subList(0, size));
        }
    }

    /**
     * 根据ID查找文档
     */
    public LLMResultDocument findById(String docId) {
        synchronized (resultHistory) {
            return resultHistory.stream()
                    .filter(doc -> doc.getId().equals(docId))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        LLMResultDocument doc = findById(docId);
        if (doc == null) {
            return false;
        }

        try {
            // 删除文件
            Files.deleteIfExists(Paths.get(doc.getFilePath()));

            // 从历史记录移除
            synchronized (resultHistory) {
                resultHistory.removeIf(d -> d.getId().equals(docId));
            }

            // 持久化到磁盘
            saveHistoryToDisk();

            log.info(I18N.get("llm_result.log.document_deleted", docId));
            return true;

        } catch (IOException e) {
            log.error(I18N.get("llm_result.log.delete_failed", docId), e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 Markdown 文档
     */
    private String generateMarkdown(LLMAnalysisResult result, boolean embedImages) {
        StringBuilder md = new StringBuilder();

        // 标题
        md.append("# ").append(result.getTitle()).append("\n\n");

        // 元信息
        md.append("> **生成时间**: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("  \n");

        if (result.getSourceDocument() != null) {
            md.append("> **源文档**: ").append(result.getSourceDocument()).append("  \n");
        }

        if (result.getQuestion() != null) {
            md.append("> **分析问题**: ").append(result.getQuestion()).append("  \n");
        }

        md.append("> **分析类型**: ").append(result.getAnalysisType()).append("\n\n");

        md.append("---\n\n");

        // 主体内容
        String content = result.getContent();
        if (embedImages) {
            content = convertImagesToBase64(content);
        }
        md.append(content).append("\n\n");

        // 如果有关键点
        if (result.getKeyPoints() != null && !result.getKeyPoints().isEmpty()) {
            md.append("---\n\n");
            md.append("## 📌 关键要点\n\n");
            for (String point : result.getKeyPoints()) {
                md.append("- ").append(point).append("\n");
            }
            md.append("\n");
        }

        // 如果有图片列表
        if (result.getImages() != null && !result.getImages().isEmpty()) {
            md.append("---\n\n");
            md.append("## 🖼️ 相关图片\n\n");
            for (int i = 0; i < result.getImages().size(); i++) {
                ImageInfo img = result.getImages().get(i);
                md.append("### 图片 ").append(i + 1);
                if (img.getCaption() != null) {
                    md.append(": ").append(img.getCaption());
                }
                md.append("\n\n");

                if (embedImages && img.getBase64() != null) {
                    md.append("![").append(img.getCaption() != null ? img.getCaption() : "图片")
                      .append("](data:").append(img.getMimeType()).append(";base64,")
                      .append(img.getBase64()).append(")\n\n");
                } else if (img.getUrl() != null) {
                    md.append("![").append(img.getCaption() != null ? img.getCaption() : "图片")
                      .append("](").append(img.getUrl()).append(")\n\n");
                }

                if (img.getDescription() != null) {
                    md.append("*").append(img.getDescription()).append("*\n\n");
                }
            }
        }

        // 页脚
        md.append("---\n\n");
        md.append("*本文档由 AI 智能分析生成*\n");

        return md.toString();
    }

    /**
     * 将图片链接转换为 Base64
     */
    private String convertImagesToBase64(String markdown) {
        // 处理网络图片
        Matcher urlMatcher = IMAGE_URL_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();

        while (urlMatcher.find()) {
            String altText = urlMatcher.group(1);
            String imageUrl = urlMatcher.group(2);

            try {
                String base64 = downloadImageAsBase64(imageUrl);
                String mimeType = guessMimeType(imageUrl);
                String replacement = "![" + altText + "](data:" + mimeType + ";base64," + base64 + ")";
                urlMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                log.warn(I18N.get("llm_result.log.download_image_failed", imageUrl), e);
                // 保持原样
                urlMatcher.appendReplacement(sb, Matcher.quoteReplacement(urlMatcher.group()));
            }
        }
        urlMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 下载图片并转为 Base64
     */
    private String downloadImageAsBase64(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        byte[] imageBytes = url.openStream().readAllBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 猜测 MIME 类型
     */
    private String guessMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        if (lower.contains(".svg")) return "image/svg+xml";
        return "image/jpeg"; // 默认
    }

    /**
     * 生成文档ID
     */
    private String generateDocId() {
        return "llm-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成文件名
     */
    private String generateFileName(LLMAnalysisResult result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String prefix = result.getAnalysisType().toLowerCase().replace(" ", "-");
        return timestamp + "-" + prefix;
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 移除 Markdown 标记
        String plain = content.replaceAll("#+ ", "")
                              .replaceAll("\\*+", "")
                              .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                              .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]")
                              .replaceAll("\n+", " ")
                              .trim();

        if (plain.length() <= maxLength) {
            return plain;
        }

        return plain.substring(0, maxLength) + "...";
    }

    /**
     * 检查内容是否包含图片
     */
    private boolean containsImages(String content) {
        return content != null && IMAGE_LOCAL_PATTERN.matcher(content).find();
    }

    /**
     * 添加到历史记录
     */
    private void addToHistory(LLMResultDocument document) {
        synchronized (resultHistory) {
            resultHistory.addFirst(document);

            // 保持历史记录在限制内
            while (resultHistory.size() > maxHistory) {
                resultHistory.removeLast();
            }
        }

        // 持久化到磁盘
        saveHistoryToDisk();
    }

    /**
     * 添加到知识库
     *
     * 将 LLM 分析结果添加到知识库，使其可被后续检索使用
     */
    private void addToKnowledgeBase(LLMResultDocument document, String content) {
        try {
            // 知识库服务会自动发现新文件并索引
            // 因为文件已保存在 storagePath 目录下
            // 这里只需要记录日志
            log.info("📚 LLM 分析结果已保存，下次增量索引时将自动添加到知识库: {}", document.getFileName());

            // 如果需要立即索引，可以注入 KnowledgeBaseService 并调用：
            // knowledgeBaseService.incrementalIndexFile(Paths.get(document.getFilePath()));

        } catch (Exception e) {
            log.warn("⚠️ 添加到知识库失败（不影响保存）: {}", e.getMessage());
        }
    }

    // ==================== 数据类 ====================

    /**
     * LLM 分析结果
     */
    @Data
    @Builder
    public static class LLMAnalysisResult {
        /** 标题 */
        private String title;

        /** 源文档名称 */
        private String sourceDocument;

        /** 用户问题 */
        private String question;

        /** 分析类型 */
        private String analysisType;

        /** 主体内容（Markdown 格式） */
        private String content;

        /** 关键要点 */
        private List<String> keyPoints;

        /** 相关图片 */
        private List<ImageInfo> images;

        /** 额外元数据 */
        private Map<String, Object> metadata;
    }

    /**
     * 图片信息
     */
    @Data
    @Builder
    public static class ImageInfo {
        /** 图片 URL */
        private String url;

        /** Base64 编码（可选） */
        private String base64;

        /** MIME 类型 */
        private String mimeType;

        /** 图片标题 */
        private String caption;

        /** AI 生成的描述 */
        private String description;
    }

    /**
     * LLM 结果文档
     */
    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LLMResultDocument {
        /** 文档ID */
        private String id;

        /** 文件名 */
        private String fileName;

        /** 文件路径 */
        private String filePath;

        /** 源文档 */
        private String sourceDocument;

        /** 分析问题 */
        private String question;

        /** 分析类型 */
        private String analysisType;

        /** 摘要 */
        private String summary;

        /** 创建时间 */
        private LocalDateTime createdAt;

        /** 内容长度 */
        private int contentLength;

        /** 是否包含图片 */
        private boolean hasImages;
    }
}

