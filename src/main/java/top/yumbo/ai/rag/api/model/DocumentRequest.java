package top.yumbo.ai.rag.api.model;
import lombok.Data;
import java.util.Map;

/**
 * 文档请求 (Document request)
 * 用于 API 接口接收文档创建或更新的请求数据 (Used by API interfaces to receive document creation or update request data)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
public class DocumentRequest {

    /**
     * 标题 (title)
     */
    private String title;

    /**
     * 内容 (content)
     */
    private String content;

    /**
     * 分类 (category)
     */
    private String category;

    /**
     * 元数据 (metadata)
     */
    private Map<String, Object> metadata;
}
