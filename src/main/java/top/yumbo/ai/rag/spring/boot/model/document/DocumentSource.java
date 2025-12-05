package top.yumbo.ai.rag.spring.boot.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档来源信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSource {
    /** 文档类型 (ppt, pdf, word, markdown, code) */
    private String documentType;

    /** 文档名称 */
    private String documentName;

    /** 文档路径 */
    private String documentPath;

    /** 总片段数 */
    private int totalSegments;

    /** 文档大小（字节） */
    private long fileSize;

    /** MIME 类型 */
    private String mimeType;

    /**
     * 从文件路径创建 DocumentSource
     */
    public static DocumentSource fromPath(String path, String documentType, int totalSegments) {
        String fileName = path;
        int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSep >= 0) {
            fileName = path.substring(lastSep + 1);
        }

        return DocumentSource.builder()
                .documentPath(path)
                .documentName(fileName)
                .documentType(documentType)
                .totalSegments(totalSegments)
                .build();
    }
}

