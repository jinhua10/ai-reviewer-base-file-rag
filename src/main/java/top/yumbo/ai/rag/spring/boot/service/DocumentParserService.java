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
 * 文档解析服务（Document Parser Service）
 *
 * <p>负责解析各种格式的文档内容，支持 Tika 的 60+ 种文档格式</p>
 * <p>Responsible for parsing various document formats, supporting 60+ formats via Tika</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@Service
@Slf4j
public class DocumentParserService {

    @Autowired(required = false)
    private DocumentParserFactory documentParserFactory;

    /**
     * 解析文档（Parse document）
     *
     * <p>将各种格式的文档解析为纯文本内容</p>
     * <p>Parse various document formats into plain text content</p>
     *
     * @param filePath 文件路径（File path）
     * @return 文档文本内容（Document text content）
     * @throws IOException 如果文件不存在或解析失败（If file not found or parsing failed）
     */
    public String parseDocument(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("文件不存在(File not found): " + filePath);
        }

        String fileName = path.getFileName().toString().toLowerCase();

        // 纯文本文件直接读取（Read plain text files directly）
        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        // 使用文档解析器工厂（Use document parser factory）
        if (documentParserFactory != null) {
            try {
                DocumentParser parser = documentParserFactory.getParser(filePath);
                if (parser != null) {
                    List<DocumentSegment> segments = parser.parse(filePath);
                    // 将文档片段合并为单个字符串（Merge document segments into single string）
                    return mergeSegments(segments);
                }
            } catch (Exception e) {
                log.warn("文档解析失败(Document parser failed) {}: {}", fileName, e.getMessage());
            }
        }

        // 尝试作为文本读取（Try to read as text）
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            // 检查是否包含太多非文本字符（Check for too many non-text characters）
            long nonTextChars = content.chars().filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
            if (nonTextChars > content.length() * 0.1) {
                return "二进制文件，无法解析文本内容(Binary file, cannot parse text content)";
            }
            return content;
        } catch (Exception e) {
            return "无法解析文档(Cannot parse document): " + e.getMessage();
        }
    }

    /**
     * 合并文档片段为单个字符串（Merge document segments into a single string）
     *
     * @param segments 文档片段列表（List of document segments）
     * @return 合并后的文本（Merged text）
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
     * 检查是否支持该文件格式（Check if file format is supported）
     *
     * <p>优先使用 DocumentParserFactory 检查，否则使用默认支持列表</p>
     * <p>Prefer DocumentParserFactory check, otherwise use default support list</p>
     *
     * @param fileName 文件名（File name）
     * @return 是否支持（Whether supported）
     */
    public boolean isSupported(String fileName) {
        // 优先使用解析器工厂检查（Prefer parser factory check）
        if (documentParserFactory != null) {
            return documentParserFactory.getParser(fileName) != null;
        }

        // 降级：使用 Tika 支持的常见格式列表（Fallback: use Tika supported formats）
        String lower = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * Tika 支持的常见文档格式（Common document formats supported by Tika）
     *
     * @see <a href="https://tika.apache.org/2.9.1/formats.html">Tika Supported Formats</a>
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            // 文本格式（Text formats）
            ".txt", ".md", ".markdown", ".rst", ".csv", ".tsv", ".log",
            ".xml", ".json", ".yaml", ".yml", ".html", ".htm", ".xhtml",

            // Microsoft Office
            ".doc", ".docx", ".docm",      // Word
            ".xls", ".xlsx", ".xlsm",      // Excel
            ".ppt", ".pptx", ".pptm",      // PowerPoint
            ".msg", ".eml",                 // Outlook/Email
            ".vsd", ".vsdx",                // Visio
            ".pub",                         // Publisher

            // OpenDocument（LibreOffice/OpenOffice）
            ".odt", ".ods", ".odp", ".odg", ".odf",

            // PDF
            ".pdf",

            // 富文本（Rich text）
            ".rtf",

            // 电子书（E-books）
            ".epub", ".mobi", ".azw", ".fb2",

            // 图片 - 可提取元数据和OCR（Images - metadata and OCR）
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".tif",
            ".webp", ".svg", ".ico", ".psd",

            // 压缩包 - 可列出内容（Archives - list contents）
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2",

            // 音视频 - 元数据（Audio/Video - metadata）
            ".mp3", ".mp4", ".avi", ".mkv", ".wav", ".flac",
            ".ogg", ".wma", ".wmv", ".mov",

            // 代码文件（Code files）
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".h",
            ".cs", ".go", ".rb", ".php", ".swift", ".kt", ".rs",
            ".sql", ".sh", ".bat", ".ps1",

            // 配置文件（Config files）
            ".properties", ".ini", ".conf", ".cfg",

            // 其他（Others）
            ".tex", ".latex",               // LaTeX
            ".chm",                          // Windows Help
            ".mbox",                         // Mailbox
            ".ics",                          // Calendar
            ".vcf"                           // vCard
    );
}

