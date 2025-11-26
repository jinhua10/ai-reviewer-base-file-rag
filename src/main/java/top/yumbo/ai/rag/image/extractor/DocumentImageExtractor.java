package top.yumbo.ai.rag.image.extractor;

import top.yumbo.ai.rag.image.ImageInfo;

import java.io.InputStream;
import java.util.List;

/**
 * 文档图片提取器接口
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
public interface DocumentImageExtractor {

    /**
     * 从文档中提取图片
     *
     * @param documentStream 文档输入流
     * @param documentName 文档名称
     * @return 提取的图片信息列表
     * @throws Exception 提取失败时抛出异常
     */
    List<ExtractedImage> extractImages(InputStream documentStream, String documentName) throws Exception;

    /**
     * 判断是否支持该文档类型
     *
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean supports(String fileName);

    /**
     * 获取提取器名称
     */
    String getName();
}

