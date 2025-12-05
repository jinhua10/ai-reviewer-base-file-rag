package top.yumbo.ai.rag.spring.boot.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSource;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PDF 文档解析器
 *
 * 将 PDF 文件解析为页面片段列表
 */
@Slf4j
@Component
public class PDFDocumentParser implements DocumentParser {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList("pdf");

    /** 每页最大字符数（避免单页内容过长） */
    private static final int MAX_CHARS_PER_PAGE = 5000;

    @Override
    public boolean supports(String documentPath, String mimeType) {
        if (documentPath == null) {
            return false;
        }

        String lowerPath = documentPath.toLowerCase();
        return lowerPath.endsWith(".pdf")
                || "application/pdf".equals(mimeType);
    }

    @Override
    public List<DocumentSegment> parse(String documentPath) throws IOException {
        File file = new File(documentPath);
        if (!file.exists()) {
            throw new IOException(LogMessageProvider.getMessage("pdf_parser.error.file_not_found", documentPath));
        }

        List<DocumentSegment> segments = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file)) {
            int totalPages = document.getNumberOfPages();

            log.info(LogMessageProvider.getMessage("pdf_parser.log.parse_file", file.getName(), totalPages));

            // 创建文档来源信息
            DocumentSource source = DocumentSource.builder()
                    .documentType("pdf")
                    .documentName(file.getName())
                    .documentPath(documentPath)
                    .totalSegments(totalPages)
                    .fileSize(file.length())
                    .mimeType("application/pdf")
                    .build();

            PDFTextStripper stripper = new PDFTextStripper();

            // 解析每一页
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String pageText = stripper.getText(document);

                // 提取标题（取第一行非空文本）
                String title = extractPageTitle(pageText, pageNum);

                // 截断过长的内容
                if (pageText.length() > MAX_CHARS_PER_PAGE) {
                    pageText = pageText.substring(0, MAX_CHARS_PER_PAGE) + "\n" +
                            LogMessageProvider.getMessage("pdf_parser.error.content_truncated");
                }

                DocumentSegment segment = DocumentSegment.builder()
                        .id("page-" + pageNum)
                        .index(pageNum)
                        .type(SegmentType.PAGE)
                        .title(title)
                        .textContent(pageText.trim())
                        .source(source)
                        .build();

                segments.add(segment);

                log.debug(LogMessageProvider.getMessage("pdf_parser.log.parse_page", pageNum, title, pageText.length()));
            }

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("pdf_parser.log.parse_failed", documentPath), e);
            throw new IOException(LogMessageProvider.getMessage("pdf_parser.log.parse_failed", e.getMessage()), e);
        }

        return segments;
    }

    @Override
    public List<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String getParserName() {
        return "PDF 文档解析器";
    }

    /**
     * 提取页面标题
     */
    private String extractPageTitle(String pageText, int pageNum) {
        if (pageText == null || pageText.trim().isEmpty()) {
            return LogMessageProvider.getMessage("pdf_parser.title.default_page", pageNum);
        }

        // 取第一行非空文本作为标题
        String[] lines = pageText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && line.length() <= 100) {
                return line;
            }
        }

        return LogMessageProvider.getMessage("pdf_parser.title.default_page", pageNum);
    }
}

