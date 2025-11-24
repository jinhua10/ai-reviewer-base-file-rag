package top.yumbo.ai.rag.impl.parser.image;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;

/**
 * 占位符策略（默认实现）
 *
 * 不进行实际的图片处理，只返回占位符文本
 * 优点：零依赖、零成本、快速
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class PlaceholderImageStrategy implements ImageContentExtractorStrategy {

    private int imageCounter = 0;

    @Override
    public String extractContent(InputStream imageStream, String imageName) {
        imageCounter++;
        String placeholder = String.format("[图片%d: %s - 未提取内容]", imageCounter, imageName);
        log.debug("使用占位符策略: {}", placeholder);
        return placeholder;
    }

    @Override
    public String extractContent(File imageFile) {
        return extractContent(null, imageFile.getName());
    }

    @Override
    public String getStrategyName() {
        return "Placeholder";
    }

    @Override
    public boolean isAvailable() {
        return true; // 始终可用
    }
}

