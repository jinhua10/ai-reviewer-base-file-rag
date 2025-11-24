package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import top.yumbo.ai.rag.core.DocumentParser;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Apache Tika文档解析器实现
 * 支持多种文档格式的解析
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class TikaDocumentParser implements DocumentParser {

    private final Tika tika;
    private final Parser parser;

    // 解析配置
    private final boolean extractImageMetadata;     // 是否提取图片元数据
    private final boolean includeImagePlaceholders; // 是否包含图片占位符
    private final int maxContentLength;             // 最大内容长度（防止内存溢出）
    private final top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor imageExtractor; // 智能图片提取器

    // 默认配置
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 10 * 1024 * 1024; // 10MB
    private static final boolean DEFAULT_EXTRACT_IMAGE_METADATA = true;
    private static final boolean DEFAULT_INCLUDE_IMAGE_PLACEHOLDERS = true;

    // 支持的MIME类型
    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList(
            // 文本
            "text/plain",
            "text/html",
            "text/xml",
            "text/markdown",

            // 文档
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",

            // 代码
            "text/x-java-source",
            "text/x-python",
            "text/x-c",
            "application/javascript",
            "application/json",
            "application/xml"
    ));

    // 支持的文件扩展名
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".txt", ".md", ".html", ".xml", ".json",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h", ".go", ".rs"
    ));

    public TikaDocumentParser() {
        this(DEFAULT_MAX_CONTENT_LENGTH, DEFAULT_EXTRACT_IMAGE_METADATA, DEFAULT_INCLUDE_IMAGE_PLACEHOLDERS);
    }

    /**
     * 带配置的构造函数
     *
     * @param maxContentLength 最大内容长度（字符数）
     * @param extractImageMetadata 是否提取图片元数据
     * @param includeImagePlaceholders 是否包含图片占位符
     */
    public TikaDocumentParser(int maxContentLength, boolean extractImageMetadata, boolean includeImagePlaceholders) {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        this.maxContentLength = maxContentLength;
        this.extractImageMetadata = extractImageMetadata;
        this.includeImagePlaceholders = includeImagePlaceholders;

        // 初始化智能图片提取器（从环境变量自动配置）
        this.imageExtractor = top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor.fromEnv();

        // 显示OCR配置详情
        String enableOCR = System.getenv("ENABLE_OCR");
        String tessdataPrefix = System.getenv("TESSDATA_PREFIX");
        String ocrLanguage = System.getenv("OCR_LANGUAGE");

        log.info("📊 TikaDocumentParser 初始化完成:");
        log.info("  ├─ 最大内容长度: {}MB", maxContentLength / 1024 / 1024);
        log.info("  ├─ 提取图片元数据: {}", extractImageMetadata);
        log.info("  ├─ 图片占位符: {}", includeImagePlaceholders);
        log.info("  └─ 图片处理策略: {}", imageExtractor.getActiveStrategy().getStrategyName());

        if ("true".equalsIgnoreCase(enableOCR)) {
            log.info("🔍 OCR配置:");
            log.info("  ├─ ENABLE_OCR: {}", enableOCR);
            log.info("  ├─ TESSDATA_PREFIX: {}", tessdataPrefix != null ? tessdataPrefix : "未设置");
            log.info("  └─ OCR_LANGUAGE: {}", ocrLanguage != null ? ocrLanguage : "未设置");
        } else {
            log.info("⚠️  OCR未启用 (ENABLE_OCR={})", enableOCR);
        }
    }

    @Override
    public String parse(File file) {
        if (file == null || !file.exists()) {
            log.warn("File does not exist: {}", file);
            return "";
        }

        try {
            // 检测MIME类型
            String mimeType = tika.detect(file);
            log.debug("Detected MIME type: {} for file: {}", mimeType, file.getName());

            // 对于Office文档，使用专门的图片提取器
            String filename = file.getName().toLowerCase();
            if (filename.endsWith(".pptx") || filename.endsWith(".docx") || filename.endsWith(".xlsx")) {
                OfficeImageExtractor officeExtractor = new OfficeImageExtractor(imageExtractor);

                String content = "";
                if (filename.endsWith(".pptx")) {
                    log.info("使用Office图片提取器处理PPTX: {}", file.getName());
                    content = officeExtractor.extractFromPPTX(file);
                } else if (filename.endsWith(".docx")) {
                    log.info("使用Office图片提取器处理DOCX: {}", file.getName());
                    content = officeExtractor.extractFromDOCX(file);
                } else if (filename.endsWith(".xlsx")) {
                    log.info("使用Office图片提取器处理XLSX: {}", file.getName());
                    content = officeExtractor.extractFromXLSX(file);
                }

                if (content != null && !content.trim().isEmpty()) {
                    log.info("✅ Office文档处理完成: {}, 内容长度: {}", file.getName(), content.length());
                    return content;
                }
            }

            // 默认使用Tika解析
            try (InputStream stream = Files.newInputStream(file.toPath())) {
                String content = parseWithMetadata(stream, file.getName(), mimeType);
                log.debug("Parsed file: {}, content length: {}", file.getName(), content.length());
                return content;
            }

        } catch (IOException | TikaException | SAXException e) {
            log.error("Failed to parse file: {}", file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * 增强的解析方法，支持图片元数据提取
     */
    private String parseWithMetadata(InputStream stream, String filename, String mimeType)
            throws IOException, TikaException, SAXException {

        // 创建元数据对象
        Metadata metadata = new Metadata();
        metadata.set("resourceName", filename);
        metadata.set("Content-Type", mimeType);

        // 创建内容处理器（限制最大长度）
        ContentHandler handler = new BodyContentHandler(maxContentLength);

        // 创建解析上下文
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        // 图片处理说明：
        // 不使用 EnhancedContentHandler，因为它可能导致内容丢失（特别是 Excel）
        // 图片处理由 SmartImageExtractor 统一管理，支持多种策略：
        // - 占位符（默认，零依赖）
        // - Tesseract OCR（文字识别，本地）
        // - Vision LLM（语义理解，云端）
        // 可通过环境变量配置：ENABLE_OCR=true, VISION_LLM_API_KEY=xxx

        // 执行解析
        parser.parse(stream, handler, metadata, context);

        // 获取文本内容
        String textContent = handler.toString();

        // 如果启用了图片元数据提取，添加图片信息
        if (extractImageMetadata) {
            textContent = enrichWithImageMetadata(textContent, metadata);
        }

        return textContent;
    }

    /**
     * 使用图片元数据丰富文本内容
     */
    private String enrichWithImageMetadata(String textContent, Metadata metadata) {
        StringBuilder enriched = new StringBuilder(textContent);

        // 提取图片相关的元数据
        String[] metadataNames = metadata.names();
        int imageCount = 0;

        for (String name : metadataNames) {
            // 检查是否是图片相关的元数据
            if (name.toLowerCase().contains("image") ||
                name.toLowerCase().contains("picture") ||
                name.toLowerCase().contains("photo")) {

                String value = metadata.get(name);
                if (value != null && !value.isEmpty()) {
                    imageCount++;

                    // 添加图片元数据信息
                    if (imageCount == 1) {
                        enriched.append("\n\n--- 图片信息 ---\n");
                    }
                    enriched.append(String.format("[图片%d] %s: %s\n", imageCount, name, value));
                }
            }
        }

        // 如果发现图片但没有详细元数据，添加占位符
        if (includeImagePlaceholders && imageCount == 0 &&
            (metadata.get("X-TIKA:embedded_resource_count") != null)) {

            String embeddedCount = metadata.get("X-TIKA:embedded_resource_count");
            if (embeddedCount != null && Integer.parseInt(embeddedCount) > 0) {
                enriched.append("\n\n--- 嵌入资源 ---\n");
                enriched.append(String.format("[文档包含 %s 个嵌入资源（图片/图表等）]\n", embeddedCount));
            }
        }

        return enriched.toString();
    }

    /**
     * 增强的内容处理器，用于捕获图片占位符
     */
    private static class EnhancedContentHandler extends BodyContentHandler {
        private final boolean includeImagePlaceholders;
        private int imageCounter = 0;

        public EnhancedContentHandler(ContentHandler handler, Metadata metadata, boolean includeImagePlaceholders) {
            super(handler);
            this.includeImagePlaceholders = includeImagePlaceholders;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // 检测图片标记
            String text = new String(ch, start, length);

            if (includeImagePlaceholders &&
                (text.contains("[embedded]") || text.contains("[image]"))) {
                imageCounter++;
                // 替换为更友好的占位符
                String placeholder = String.format("[图片%d: 无法提取文字内容]", imageCounter);
                super.characters(placeholder.toCharArray(), 0, placeholder.length());
            } else {
                super.characters(ch, start, length);
            }
        }
    }

    @Override
    public String parse(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            log.warn("Empty byte array provided");
            return "";
        }

        try {
            // 使用Tika解析
            String content = tika.parseToString(new java.io.ByteArrayInputStream(bytes));

            log.debug("Parsed bytes: mimeType={}, content length: {}", mimeType, content.length());
            return content;

        } catch (IOException | TikaException e) {
            log.error("Failed to parse bytes: mimeType={}", mimeType, e);
            return "";
        }
    }

    @Override
    public boolean supports(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        // 检查是否在支持列表中
        if (SUPPORTED_MIME_TYPES.contains(mimeType)) {
            return true;
        }

        // 检查通配符匹配
        return mimeType.startsWith("text/");
    }

    @Override
    public boolean supportsExtension(String extension) {
        if (extension == null) {
            return false;
        }

        // 确保扩展名以点开头
        String ext = extension.startsWith(".") ? extension : "." + extension;
        return SUPPORTED_EXTENSIONS.contains(ext.toLowerCase());
    }

    /**
     * 检测文件的MIME类型
     */
    public String detectMimeType(File file) {
        try {
            return tika.detect(file);
        } catch (IOException e) {
            log.error("Failed to detect MIME type: {}", file.getAbsolutePath(), e);
            return "application/octet-stream";
        }
    }

    /**
     * 检测字节数组的MIME类型
     */
    public String detectMimeType(byte[] bytes) {
        return tika.detect(bytes);
    }

    /**
     * 根据文件扩展名检测MIME类型
     */
    public String detectMimeType(String filename) {
        return tika.detect(filename);
    }
}

