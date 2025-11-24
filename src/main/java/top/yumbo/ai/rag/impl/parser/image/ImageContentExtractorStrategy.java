package top.yumbo.ai.rag.impl.parser.image;

import java.io.File;
import java.io.InputStream;

/**
 * 图片内容提取策略接口
 *
 * 支持多种图片处理方式：
 * - 占位符（默认）
 * - OCR 文字识别
 * - Vision LLM 语义理解
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
public interface ImageContentExtractorStrategy {

    /**
     * 从图片提取文本内容
     *
     * @param imageStream 图片输入流
     * @param imageName 图片名称（用于日志）
     * @return 提取的文本内容
     */
    String extractContent(InputStream imageStream, String imageName);

    /**
     * 从图片文件提取文本内容
     *
     * @param imageFile 图片文件
     * @return 提取的文本内容
     */
    String extractContent(File imageFile);

    /**
     * 获取策略名称
     */
    String getStrategyName();

    /**
     * 是否可用
     */
    boolean isAvailable();
}

