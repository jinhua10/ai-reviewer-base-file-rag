package top.yumbo.ai.rag.image.extractor;

import top.yumbo.ai.rag.image.ImageInfo;

import java.io.InputStream;
import java.util.List;

/**
 * 文档图片提取器接口（Document image extractor interface）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public interface DocumentImageExtractor {

    /**
     * 从文档中提取图片（Extract images from document）
     *
     * @param documentStream 文档输入流（Document input stream）
     * @param documentName 文档名称（Document name）
     * @return 提取的图片信息列表（List of extracted image information）
     * @throws Exception 提取失败时抛出异常（Throws exception when extraction fails）
     */
    List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception;

    /**
     * 判断是否支持该文档类型（Check if this document type is supported）
     *
     * @param fileName 文件名（Filename）
     * @return 是否支持（Whether supported）
     */
    boolean supports(String fileName);

    /**
     * 获取提取器名称（Get extractor name）
     */
    String getName();
}
