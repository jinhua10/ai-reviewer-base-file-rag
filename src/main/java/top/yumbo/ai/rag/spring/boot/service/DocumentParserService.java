package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.service.parser.DocumentParser;
import top.yumbo.ai.rag.spring.boot.service.parser.DocumentParserFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档解析服务
 * (Document Parser Service)
 *
 * 负责解析各种格式的文档内容
 */
@Service
@Slf4j
public class DocumentParserService {

    @Autowired(required = false)
    private DocumentParserFactory documentParserFactory;

    /**
     * 解析文档
     * (Parse document)
     *
     * @param filePath 文件路径
     * @return 文档文本内容
     */
    public String parseDocument(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        String fileName = path.getFileName().toString().toLowerCase();

        // 纯文本文件直接读取
        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        // 使用文档解析器工厂
        if (documentParserFactory != null) {
            try {
                DocumentParser parser = documentParserFactory.getParser(filePath);
                if (parser != null) {
                    List<DocumentSegment> segments = parser.parse(filePath);
                    // 将文档片段合并为单个字符串
                    return mergeSegments(segments);
                }
            } catch (Exception e) {
                log.warn("Document parser failed for {}: {}", fileName, e.getMessage());
            }
        }

        // 尝试作为文本读取
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            // 检查是否包含太多非文本字符
            long nonTextChars = content.chars().filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
            if (nonTextChars > content.length() * 0.1) {
                return "二进制文件，无法解析文本内容";
            }
            return content;
        } catch (Exception e) {
            return "无法解析文档: " + e.getMessage();
        }
    }

    /**
     * 合并文档片段为单个字符串
     * (Merge document segments into a single string)
     */
    private String mergeSegments(List<DocumentSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }

        return segments.stream()
                .map(DocumentSegment::getTextContent)
                .filter(content -> content != null && !content.trim().isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 检查是否支持该文件格式
     * (Check if file format is supported)
     *
     * 优先使用 DocumentParserFactory 检查，否则使用默认支持列表
     * (Prefer DocumentParserFactory check, otherwise use default support list)
     */
    public boolean isSupported(String fileName) {
        // 优先使用解析器工厂检查
        if (documentParserFactory != null) {
            return documentParserFactory.getParser(fileName) != null;
        }

        // 降级：使用 Tika 支持的常见格式列表
        String lower = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * Tika 支持的常见文档格式
     * (Common document formats supported by Tika)
     *
     * 参考: https://tika.apache.org/2.9.1/formats.html
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            // 文本格式 (Text formats)
            ".txt", ".md", ".markdown", ".rst", ".csv", ".tsv", ".log",
            ".xml", ".json", ".yaml", ".yml", ".html", ".htm", ".xhtml",

            // Microsoft Office
            ".doc", ".docx", ".docm",      // Word
            ".xls", ".xlsx", ".xlsm",      // Excel
            ".ppt", ".pptx", ".pptm",      // PowerPoint
            ".msg", ".eml",                 // Outlook/Email
            ".vsd", ".vsdx",                // Visio
            ".pub",                         // Publisher

            // OpenDocument (LibreOffice/OpenOffice)
            ".odt", ".ods", ".odp", ".odg", ".odf",

            // PDF
            ".pdf",

            // 富文本 (Rich text)
            ".rtf",

            // 电子书 (E-books)
            ".epub", ".mobi", ".azw", ".fb2",

            // 图片（可提取元数据和OCR）(Images - metadata and OCR)
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".tif",
            ".webp", ".svg", ".ico", ".psd",

            // 压缩包（可列出内容）(Archives - list contents)
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2",

            // 音视频（元数据）(Audio/Video - metadata)
            ".mp3", ".mp4", ".avi", ".mkv", ".wav", ".flac",
            ".ogg", ".wma", ".wmv", ".mov",

            // 代码文件 (Code files)
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".h",
            ".cs", ".go", ".rb", ".php", ".swift", ".kt", ".rs",
            ".sql", ".sh", ".bat", ".ps1",

            // 配置文件 (Config files)
            ".properties", ".ini", ".conf", ".cfg",

            // 其他 (Others)
            ".tex", ".latex",               // LaTeX
            ".chm",                          // Windows Help
            ".mbox",                         // Mailbox
            ".ics",                          // Calendar
            ".vcf"                           // vCard
    );
}

