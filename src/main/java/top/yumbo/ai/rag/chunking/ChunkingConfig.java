package top.yumbo.ai.rag.chunking;

import lombok.Data;

/**
 * 文档切分配置
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
public class ChunkingConfig {

    /**
     * 切分块大小（字符数）
     * 建议设置为 max-doc-length 的 80%，留出重叠空间
     */
    private int chunkSize = 4000;

    /**
     * 切分重叠大小（字符数）
     * 用于保持上下文连贯性，建议 10-20% 的 chunk-size
     */
    private int chunkOverlap = 400;

    /**
     * 是否在句子边界切分（强烈推荐）
     */
    private boolean splitOnSentence = true;

    /**
     * AI 语义切分配置
     */
    private AiChunkingConfig aiChunking = new AiChunkingConfig();

    /**
     * AI 语义切分配置
     */
    @Data
    public static class AiChunkingConfig {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 用于语义切分的模型
         */
        private String model = "deepseek-chat";

        /**
         * 语义切分的 Prompt 模板
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
     * 验证配置
     */
    public void validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunk-size must be positive");
        }

        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("chunk-overlap must be non-negative");
        }

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunk-overlap must be less than chunk-size");
        }
    }
}

