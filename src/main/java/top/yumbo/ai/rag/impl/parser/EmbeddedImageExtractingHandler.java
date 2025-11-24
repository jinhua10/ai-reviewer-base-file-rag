package top.yumbo.ai.rag.impl.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import top.yumbo.ai.rag.impl.parser.image.SmartImageExtractor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 增强的内容处理器，用于提取嵌入图片中的文字
 *
 * 在解析文档时，会拦截嵌入的图片资源，使用OCR提取文字内容
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
public class EmbeddedImageExtractingHandler extends ContentHandlerDecorator {

    private final SmartImageExtractor imageExtractor;
    private final StringBuilder content;
    private int imageCounter = 0;

    public EmbeddedImageExtractingHandler(org.xml.sax.ContentHandler handler, SmartImageExtractor imageExtractor) {
        super(handler);
        this.imageExtractor = imageExtractor;
        this.content = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        // 检测图片元素
        if ("img".equalsIgnoreCase(localName) || "image".equalsIgnoreCase(localName)) {
            imageCounter++;
            log.debug("检测到图片元素: {}", localName);
        }
        super.startElement(uri, localName, qName, atts);
    }

    /**
     * 处理嵌入资源
     */
    public void handleEmbeddedResource(InputStream stream, Metadata metadata, ParseContext context) {
        try {
            String contentType = metadata.get(Metadata.CONTENT_TYPE);
            String resourceName = metadata.get("resourceName");

            // 检查是否是图片
            if (contentType != null && contentType.startsWith("image/")) {
                imageCounter++;

                log.info("处理嵌入图片 #{}: {} ({})", imageCounter, resourceName, contentType);

                // 读取图片数据
                byte[] imageData = readAllBytes(stream);

                if (imageData.length > 0) {
                    // 使用智能提取器处理图片
                    String extractedText = imageExtractor.extractContent(
                        new ByteArrayInputStream(imageData),
                        resourceName != null ? resourceName : "image_" + imageCounter
                    );

                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        // 将提取的文本添加到内容中
                        content.append("\n").append(extractedText).append("\n");

                        // 也发送到内容处理器
                        char[] chars = extractedText.toCharArray();
                        super.characters(chars, 0, chars.length);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理嵌入图片失败", e);
        }
    }

    /**
     * 读取所有字节
     */
    private byte[] readAllBytes(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    /**
     * 获取提取的内容
     */
    public String getExtractedContent() {
        return content.toString();
    }

    /**
     * 获取图片计数
     */
    public int getImageCount() {
        return imageCounter;
    }
}

