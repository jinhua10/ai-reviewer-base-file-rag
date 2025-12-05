package top.yumbo.ai.rag.spring.boot.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;
import top.yumbo.ai.rag.spring.boot.model.document.DocumentSource;
import top.yumbo.ai.rag.spring.boot.model.document.SegmentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Word 文档解析器
 *
 * 将 Word 文件解析为段落/章节片段列表
 */
@Slf4j
@Component
public class WordDocumentParser implements DocumentParser {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList("doc", "docx");

    /** 最小段落长度（避免过短的段落） */
    private static final int MIN_PARAGRAPH_LENGTH = 50;

    /** 合并后的最大段落长度 */
    private static final int MAX_MERGED_LENGTH = 2000;

    @Override
    public boolean supports(String documentPath, String mimeType) {
        if (documentPath == null) {
            return false;
        }

        String lowerPath = documentPath.toLowerCase();
        return lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")
                || "application/msword".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType);
    }

    @Override
    public List<DocumentSegment> parse(String documentPath) throws IOException {
        File file = new File(documentPath);
        if (!file.exists()) {
            throw new IOException(LogMessageProvider.getMessage("word_parser.error.file_not_found", documentPath));
        }

        List<DocumentSegment> segments = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            log.info(LogMessageProvider.getMessage("word_parser.log.parse_file", file.getName()));

            // 提取所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            // 按章节分组
            List<ChapterContent> chapters = groupByChapters(paragraphs);

            // 创建文档来源信息
            DocumentSource source = DocumentSource.builder()
                    .documentType("word")
                    .documentName(file.getName())
                    .documentPath(documentPath)
                    .totalSegments(chapters.size())
                    .fileSize(file.length())
                    .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .build();

            // 转换为 DocumentSegment
            for (int i = 0; i < chapters.size(); i++) {
                ChapterContent chapter = chapters.get(i);
                int index = i + 1;

                DocumentSegment segment = DocumentSegment.builder()
                        .id("chapter-" + index)
                        .index(index)
                        .type(chapter.isHeading ? SegmentType.CHAPTER : SegmentType.PARAGRAPH)
                        .title(chapter.title)
                        .textContent(chapter.content)
                        .source(source)
                        .build();

                segments.add(segment);

                log.debug(LogMessageProvider.getMessage("word_parser.log.parse_chapter", index, chapter.title, chapter.content.length()));
            }

            // 更新总片段数
            source.setTotalSegments(segments.size());

            log.info(LogMessageProvider.getMessage("word_parser.log.parse_complete", segments.size()));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("word_parser.log.parse_failed", documentPath), e);
            throw new IOException(LogMessageProvider.getMessage("word_parser.log.parse_failed", e.getMessage()), e);
        }

        return segments;
    }

    @Override
    public List<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String getParserName() {
        return "Word 文档解析器";
    }

    /**
     * 按章节分组段落
     */
    private List<ChapterContent> groupByChapters(List<XWPFParagraph> paragraphs) {
        List<ChapterContent> chapters = new ArrayList<>();

        ChapterContent currentChapter = null;
        StringBuilder contentBuilder = new StringBuilder();

        for (XWPFParagraph para : paragraphs) {
            String text = para.getText();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            text = text.trim();

            // 检查是否是标题
            boolean isHeading = isHeadingParagraph(para);

            if (isHeading) {
                // 保存之前的章节
                if (currentChapter != null) {
                    currentChapter.content = contentBuilder.toString().trim();
                    if (!currentChapter.content.isEmpty()) {
                        chapters.add(currentChapter);
                    }
                }

                // 开始新章节
                currentChapter = new ChapterContent();
                currentChapter.title = text;
                currentChapter.isHeading = true;
                contentBuilder = new StringBuilder();
            } else {
                // 普通段落
                if (currentChapter == null) {
                    currentChapter = new ChapterContent();
                    currentChapter.title = text.length() > 50 ? text.substring(0, 50) + "..." : text;
                    currentChapter.isHeading = false;
                }

                contentBuilder.append(text).append("\n\n");

                // 如果内容过长，拆分
                if (contentBuilder.length() > MAX_MERGED_LENGTH) {
                    currentChapter.content = contentBuilder.toString().trim();
                    chapters.add(currentChapter);

                    currentChapter = new ChapterContent();
                    currentChapter.title = LogMessageProvider.getMessage("word_parser.title.continued");
                    currentChapter.isHeading = false;
                    contentBuilder = new StringBuilder();
                }
            }
        }

        // 保存最后一个章节
        if (currentChapter != null) {
            currentChapter.content = contentBuilder.toString().trim();
            if (!currentChapter.content.isEmpty()) {
                chapters.add(currentChapter);
            }
        }

        // 如果没有章节，创建一个默认的
        if (chapters.isEmpty()) {
            ChapterContent defaultChapter = new ChapterContent();
            defaultChapter.title = LogMessageProvider.getMessage("word_parser.title.default_content");
            defaultChapter.content = "";
            defaultChapter.isHeading = false;
            chapters.add(defaultChapter);
        }

        return chapters;
    }

    /**
     * 判断是否是标题段落
     */
    private boolean isHeadingParagraph(XWPFParagraph para) {
        // 检查样式
        String styleId = para.getStyleID();
        if (styleId != null) {
            String styleLower = styleId.toLowerCase();
            if (styleLower.contains("heading") || styleLower.contains("title")
                    || styleLower.contains("标题")) {
                return true;
            }
        }

        // 检查大纲级别
        if (para.getNumIlvl() != null) {
            return true;
        }

        // 检查字体大小（通常标题字号更大）
        for (XWPFRun run : para.getRuns()) {
            int fontSize = run.getFontSizeAsDouble() != null ? run.getFontSizeAsDouble().intValue() : 0;
            if (fontSize >= 14) { // 14pt 或更大通常是标题
                return true;
            }
            if (run.isBold() && para.getText().length() < 100) { // 短且加粗
                return true;
            }
        }

        return false;
    }

    /**
     * 章节内容
     */
    private static class ChapterContent {
        String title;
        String content;
        boolean isHeading;
    }
}

