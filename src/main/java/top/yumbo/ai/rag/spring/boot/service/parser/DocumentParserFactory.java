package top.yumbo.ai.rag.spring.boot.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器工厂（Document parser factory）
 *
 * 根据文档类型自动选择合适的解析器（Automatically select appropriate parser based on document type）
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers != null ? parsers : new ArrayList<>();
        log.info(LogMessageProvider.getMessage("parser_factory.log.registered_parsers", this.parsers.size()));
        for (DocumentParser parser : this.parsers) {
            log.info("  - {}: {}", parser.getParserName(), parser.getSupportedTypes());
        }
    }

    /**
     * 根据文档路径获取合适的解析器（Get appropriate parser based on document path）
     *
     * @param documentPath 文档路径（Document path）
     * @return 解析器，如果没有找到返回 null（Parser, returns null if not found）
     */
    public DocumentParser getParser(String documentPath) {
        return getParser(documentPath, null);
    }

    /**
     * 根据文档路径和 MIME 类型获取合适的解析器（Get appropriate parser based on document path and MIME type）
     *
     * @param documentPath 文档路径（Document path）
     * @param mimeType MIME 类型（MIME type）
     * @return 解析器，如果没有找到返回 null（Parser, returns null if not found）
     */
    public DocumentParser getParser(String documentPath, String mimeType) {
        for (DocumentParser parser : parsers) {
            if (parser.supports(documentPath, mimeType)) {
                log.debug(LogMessageProvider.getMessage("parser_factory.log.select_parser", parser.getParserName(), documentPath));
                return parser;
            }
        }

        log.warn(LogMessageProvider.getMessage("parser_factory.log.no_parser_found", documentPath));
        return null;
    }

    /**
     * 检查是否支持该文档类型（Check if document type is supported）
     *
     * @param documentPath 文档路径（Document path）
     * @return 是否支持（Whether supported）
     */
    public boolean isSupported(String documentPath) {
        return getParser(documentPath) != null;
    }

    /**
     * 获取所有支持的文档类型（Get all supported document types）
     *
     * @return 文档类型列表（Document type list）
     */
    public List<String> getAllSupportedTypes() {
        List<String> types = new ArrayList<>();
        for (DocumentParser parser : parsers) {
            types.addAll(parser.getSupportedTypes());
        }
        return types;
    }

    /**
     * 注册自定义解析器（Register custom parser）
     *
     * @param parser 解析器（Parser）
     */
    public void registerParser(DocumentParser parser) {
        parsers.add(parser);
        log.info(LogMessageProvider.getMessage("parser_factory.log.register_parser", parser.getParserName(), parser.getSupportedTypes()));
    }
}
