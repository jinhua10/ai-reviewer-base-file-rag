package top.yumbo.ai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带评分的文档（Scored document）
 * 包含文档及其相关性评分（Contains document and its relevance score）
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredDocument {

    /**
     * 文档对象（Document object）
     */
    private Document document;

    /**
     * 相关性评分（Relevance score）
     */
    private float score;

    /**
     * 评分说明（可选，用于调试）（Score explanation (optional, for debugging)）
     */
    private String scoreExplanation;

    /**
     * 高亮片段（可选）（Highlight fragments (optional)）
     */
    private String[] highlights;
}
