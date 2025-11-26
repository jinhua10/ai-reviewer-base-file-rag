package top.yumbo.ai.rag.chunking.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.ChunkingConfig;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.DocumentChunker;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 语义文档切分器
 * 使用 AI 模型进行智能语义切分，效果最好但成本较高
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class AiSemanticChunker implements DocumentChunker {

    private final ChunkingConfig config;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public AiSemanticChunker(ChunkingConfig config, LLMClient llmClient) {
        this.config = config;
        this.llmClient = llmClient;
        this.objectMapper = new ObjectMapper();
        config.validate();

        if (!config.getAiChunking().isEnabled()) {
            log.warn("AI chunking is not enabled in config, but AiSemanticChunker is being used");
        }
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        // 如果内容不长，不需要AI切分
        if (content.length() <= config.getChunkSize()) {
            return List.of(DocumentChunk.builder()
                    .content(content)
                    .index(0)
                    .totalChunks(1)
                    .startPosition(0)
                    .endPosition(content.length())
                    .build());
        }

        try {
            log.info("🤖 Starting AI semantic chunking for {} chars", content.length());
            long startTime = System.currentTimeMillis();

            // 构建 Prompt
            String prompt = buildChunkingPrompt(content, query);

            // 调用 LLM
            String response = llmClient.generate(prompt);

            // 解析响应
            List<DocumentChunk> chunks = parseChunkingResponse(response, content);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ AI semantic chunking completed: {} chars -> {} chunks in {}ms",
                    content.length(), chunks.size(), duration);

            return chunks;

        } catch (Exception e) {
            log.error("❌ AI semantic chunking failed, falling back to smart keyword chunking", e);
            // 失败时降级到智能关键词切分
            return new SmartKeywordChunker(config).chunk(content, query);
        }
    }

    /**
     * 构建切分 Prompt
     */
    private String buildChunkingPrompt(String content, String query) {
        String promptTemplate = config.getAiChunking().getPrompt();
        int chunkSize = config.getChunkSize();

        // 替换占位符
        String prompt = promptTemplate
                .replace("{chunk_size}", String.valueOf(chunkSize))
                .replace("{content}", truncateIfNeeded(content));

        // 如果有查询上下文，添加到 Prompt 中
        if (query != null && !query.isEmpty()) {
            prompt = "用户问题：" + query + "\n\n" + prompt;
        }

        return prompt;
    }

    /**
     * 如果内容太长，截断到合理长度
     * AI 切分本身也有上下文限制
     */
    private String truncateIfNeeded(String content) {
        int maxLength = config.getChunkSize() * 10; // 最多处理10倍块大小

        if (content.length() <= maxLength) {
            return content;
        }

        log.warn("Content too long ({} chars), truncating to {} chars for AI chunking",
                content.length(), maxLength);
        return content.substring(0, maxLength) + "\n\n[内容过长已截断...]";
    }

    /**
     * 解析 AI 返回的切分结果
     */
    private List<DocumentChunk> parseChunkingResponse(String response, String originalContent) {
        try {
            // 尝试从响应中提取 JSON
            String jsonContent = extractJson(response);

            // 解析 JSON 数组
            JsonNode root = objectMapper.readTree(jsonContent);

            if (!root.isArray()) {
                throw new IllegalArgumentException("Expected JSON array, got: " + root.getNodeType());
            }

            List<DocumentChunk> chunks = new ArrayList<>();
            int totalChunks = root.size();

            for (int i = 0; i < totalChunks; i++) {
                JsonNode chunkNode = root.get(i);

                String content = chunkNode.has("content")
                        ? chunkNode.get("content").asText()
                        : "";

                String title = chunkNode.has("title")
                        ? chunkNode.get("title").asText()
                        : null;

                if (!content.isEmpty()) {
                    // 在原文中查找位置
                    int startPos = originalContent.indexOf(content.substring(0, Math.min(100, content.length())));
                    int endPos = startPos + content.length();

                    chunks.add(DocumentChunk.builder()
                            .content(content)
                            .title(title)
                            .index(i)
                            .totalChunks(totalChunks)
                            .startPosition(Math.max(0, startPos))
                            .endPosition(Math.min(originalContent.length(), endPos))
                            .metadata("ai_semantic")
                            .build());
                }
            }

            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("No valid chunks found in AI response");
            }

            return chunks;

        } catch (Exception e) {
            log.error("Failed to parse AI chunking response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI chunking response", e);
        }
    }

    /**
     * 从响应中提取 JSON
     * AI 可能返回带有解释文字的响应，需要提取纯 JSON 部分
     */
    private String extractJson(String response) {
        // 尝试找到 JSON 数组的开始和结束
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start == -1 || end == -1 || start >= end) {
            // 如果没有找到，尝试 JSON 对象格式
            start = response.indexOf('{');
            end = response.lastIndexOf('}');

            if (start == -1 || end == -1 || start >= end) {
                throw new IllegalArgumentException("No valid JSON found in response");
            }
        }

        return response.substring(start, end + 1);
    }

    @Override
    public String getName() {
        return "AI Semantic Chunker";
    }

    @Override
    public String getDescription() {
        return "使用AI模型智能语义切分，效果最好";
    }
}

