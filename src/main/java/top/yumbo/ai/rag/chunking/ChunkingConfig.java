package top.yumbo.ai.rag.chunking;

import lombok.Data;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 文档切分配置 (Document chunking configuration)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
public class ChunkingConfig {

    /**
     * 切分块大小（字符数） (Chunk size in characters)
     * 建议设置为 max-doc-length 的 80%，留出重叠空间 (Recommended to set to 80% of max-doc-length, leaving space for overlap)
     */
    private int chunkSize = 4000;

    /**
     * 切分重叠大小（字符数） (Chunk overlap size in characters)
     * 用于保持上下文连贯性，建议 10-20% 的 chunk-size (Used to maintain context coherence, recommended 10-20% of chunk-size)
     */
    private int chunkOverlap = 400;

    /**
     * 是否在句子边界切分（强烈推荐） (Whether to split on sentence boundaries (strongly recommended))
     */
    private boolean splitOnSentence = true;

    /**
     * AI 语义切分配置 (AI semantic chunking configuration)
     */
    private AiChunkingConfig aiChunking = new AiChunkingConfig();

    /**
     * AI 语义切分配置 (AI semantic chunking configuration)
     */
    @Data
    public static class AiChunkingConfig {
        /**
         * 是否启用 (Whether to enable)
         */
        private boolean enabled = false;

        /**
         * 用于语义切分的模型 (Model used for semantic chunking)
         */
        private String model = "deepseek-chat";

        /**
         * 语义切分的 Prompt 模板 (Prompt template for semantic chunking)
         */
        private String prompt = """
            请将以下文档智能切分成多个语义完整的段落。
            
            要求：
            1. 每个段落应该是一个完整的主题或概念
            2. 保持段落之间的逻辑连贯性
            3. 每个段落大小在 {chunk_size} 字符左右
            4. 返回 JSON 格式：[{"content": "段落1内容", "title": "段落1标题"}, ...]
            
            文档内容：
            {content}
            """;
    }

    /**
     * 问答时文档切分最大内容长度（字符数） (Maximum content length for document chunking during Q&A in characters)
     * 在切分文档时，如果内容超过此长度会被截断 (Content exceeding this length will be truncated when chunking documents)
     * 默认值：100000（约 200KB） (Default value: 100000 (approx. 200KB))
     */
    private int maxContentLength = 100000;

    /**
     * 问答时单次切分最大块数 (Maximum number of chunks per single chunking during Q&A)
     * 防止切分产生过多块导致内存溢出 (Prevent chunking from producing too many chunks causing memory overflow)
     * 默认值：50 (Default value: 50)
     */
    private int maxChunks = 50;

    /**
     * 验证配置 (Validate configuration)
     */
    public void validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException(I18N.get("error.chunk.chunk_size_positive"));
        }

        if (chunkOverlap < 0) {
            throw new IllegalArgumentException(I18N.get("error.chunk.overlap_non_negative"));
        }

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(I18N.get("error.chunk.overlap_less_than_size"));
        }
    }
}
