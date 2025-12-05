package top.yumbo.ai.rag.spring.boot.service.parser;

import top.yumbo.ai.rag.spring.boot.model.document.DocumentSegment;

import java.io.IOException;
import java.util.List;

/**
 * 文档解析器接口
 *
 * 将各种文档类型解析为统一的 DocumentSegment 列表
 */
public interface DocumentParser {

    /**
     * 检查是否支持该文档类型
     *
     * @param documentPath 文档路径
     * @param mimeType MIME 类型（可选）
     * @return 是否支持
     */
    boolean supports(String documentPath, String mimeType);

    /**
     * 解析文档为片段列表
     *
     * @param documentPath 文档路径
     * @return 文档片段列表
     * @throws IOException 如果解析失败
     */
    List<DocumentSegment> parse(String documentPath) throws IOException;

    /**
     * 获取支持的文档类型
     *
     * @return 支持的文档类型列表（扩展名）
     */
    List<String> getSupportedTypes();

    /**
     * 获取解析器名称
     *
     * @return 解析器名称
     */
    String getParserName();
}

