package top.yumbo.ai.rag.spring.boot.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器工厂
 *
 * 根据文档类型自动选择合适的解析器
 */
@Slf4j
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers != null ? parsers : new ArrayList<>();
        log.info("📚 已注册 {} 个文档解析器", this.parsers.size());
        for (DocumentParser parser : this.parsers) {
            log.info("  - {}: {}", parser.getParserName(), parser.getSupportedTypes());
        }
    }

    /**
     * 根据文档路径获取合适的解析器
     *
     * @param documentPath 文档路径
     * @return 解析器，如果没有找到返回 null
     */
    public DocumentParser getParser(String documentPath) {
        return getParser(documentPath, null);
    }

    /**
     * 根据文档路径和 MIME 类型获取合适的解析器
     *
     * @param documentPath 文档路径
     * @param mimeType MIME 类型
     * @return 解析器，如果没有找到返回 null
     */
    public DocumentParser getParser(String documentPath, String mimeType) {
        for (DocumentParser parser : parsers) {
            if (parser.supports(documentPath, mimeType)) {
                log.debug("选择解析器: {} for {}", parser.getParserName(), documentPath);
                return parser;
            }
        }

        log.warn("未找到支持的解析器: {}", documentPath);
        return null;
    }

    /**
     * 检查是否支持该文档类型
     *
     * @param documentPath 文档路径
     * @return 是否支持
     */
    public boolean isSupported(String documentPath) {
        return getParser(documentPath) != null;
    }

    /**
     * 获取所有支持的文档类型
     *
     * @return 文档类型列表
     */
    public List<String> getAllSupportedTypes() {
        List<String> types = new ArrayList<>();
        for (DocumentParser parser : parsers) {
            types.addAll(parser.getSupportedTypes());
        }
        return types;
    }

    /**
     * 注册自定义解析器
     *
     * @param parser 解析器
     */
    public void registerParser(DocumentParser parser) {
        parsers.add(parser);
        log.info("注册解析器: {} - {}", parser.getParserName(), parser.getSupportedTypes());
    }
}

