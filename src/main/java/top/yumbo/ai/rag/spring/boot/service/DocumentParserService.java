package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private top.yumbo.ai.rag.spring.boot.service.parser.UniversalDocumentParser universalDocumentParser;

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

        // 使用通用解析器
        if (universalDocumentParser != null) {
            try {
                return universalDocumentParser.parse(filePath);
            } catch (Exception e) {
                log.warn("Universal parser failed for {}: {}", fileName, e.getMessage());
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
     * 检查是否支持该文件格式
     * (Check if file format is supported)
     */
    public boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") ||
               lower.endsWith(".md") ||
               lower.endsWith(".pdf") ||
               lower.endsWith(".doc") ||
               lower.endsWith(".docx") ||
               lower.endsWith(".ppt") ||
               lower.endsWith(".pptx") ||
               lower.endsWith(".xls") ||
               lower.endsWith(".xlsx");
    }
}

