package top.yumbo.ai.rag.core;

import java.io.File;

/**
 * 文档解析器接口
 * 负责将各种格式的文件解析为文本内容
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public interface DocumentParser {

    /**
     * 解析文件
     *
     * @param file 文件对象
     * @return 解析后的文本内容
     */
    String parse(File file);

    /**
     * 解析字节数组
     *
     * @param bytes 文件字节数组
     * @param mimeType MIME类型
     * @return 解析后的文本内容
     */
    String parse(byte[] bytes, String mimeType);

    /**
     * 检查是否支持该MIME类型
     *
     * @param mimeType MIME类型
     * @return 是否支持
     */
    boolean supports(String mimeType);

    /**
     * 检查是否支持该文件扩展名
     *
     * @param extension 文件扩展名（如 .pdf）
     * @return 是否支持
     */
    boolean supportsExtension(String extension);
}

