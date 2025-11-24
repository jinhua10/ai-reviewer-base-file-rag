package top.yumbo.ai.rag.api.model;
import lombok.Data;
import java.util.Map;
@Data
public class DocumentRequest {
    private String title;
    private String content;
    private String category;
    private Map<String, Object> metadata;
}
