package top.yumbo.ai.rag.chunking.strategy;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.PPLProviderType;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于大语言模型的智能分块策略
 * LLM-based intelligent chunking strategy
 *
 * 优势：
 * - 理解文档语义和结构
 * - 在最佳位置切分（章节、段落、主题边界）
 * - 保持每个块的语义完整性
 * - 支持复杂文档结构（如技术文档、论文）
 *
 * 适用场景：
 * - 大型文档的一次性索引（分块成本可接受）
 * - 对分块质量要求高的场景
 * - 复杂结构的技术文档
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
public class LLMChunkingStrategy implements ChunkingStrategy {

    private final LLMClient llmClient;
    private final boolean enabled;
    private final String promptTemplate;

    // 默认提示词模板（当配置未提供时使用）
    private static final String DEFAULT_PROMPT_TEMPLATE = """
            你是一个文档分块专家。请将以下文档智能地分割成多个语义完整的块。
            
            # 分块要求
            1. 每个块应该是一个完整的语义单元（如：一个章节、一个主题、一个完整的论述）
            2. 在自然的边界处切分（章节标题、段落分隔、主题转换）
            3. 每个块大小控制在 {minSize}-{maxSize} 字符之间
            4. 保持图片标记（[图片-xxx：...]）与相关文本在同一块中
            5. 不要破坏列表、表格、代码块等结构
            
            # 输出格式
            请使用以下格式标记切分点：
            
            [CHUNK_SPLIT]
            
            在需要切分的位置插入 [CHUNK_SPLIT] 标记。不要修改原文内容，只添加标记。
            
            # 文档内容
            {content}
            
            # 请在合适的位置插入 [CHUNK_SPLIT] 标记：
            """;

    public LLMChunkingStrategy(LLMClient llmClient, boolean enabled) {
        this(llmClient, enabled, null);
    }

    public LLMChunkingStrategy(LLMClient llmClient, boolean enabled, String promptTemplate) {
        this.llmClient = llmClient;
        this.enabled = enabled;
        this.promptTemplate = (promptTemplate != null && !promptTemplate.trim().isEmpty())
                ? promptTemplate
                : DEFAULT_PROMPT_TEMPLATE;

        if (enabled && llmClient != null) {
            log.info("✅ LLM Chunking Strategy 已启用");
            if (promptTemplate != null && !promptTemplate.trim().isEmpty()) {
                log.info("   使用自定义提示词模板");
            } else {
                log.info("   使用默认提示词模板");
            }
        }
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException {
        if (!isAvailable()) {
            throw new PPLException(PPLProviderType.ONNX, "LLM Chunking Strategy 不可用");
        }

        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();
        log.info("🤖 开始 LLM 智能分块，文档长度: {} 字符", content.length());

        try {
            // 1. 如果文档较小，直接返回
            if (content.length() < config.getMaxChunkSize()) {
                log.info("📄 文档较小，无需分块");
                return List.of(DocumentChunk.builder()
                        .content(content)
                        .index(0)
                        .build());
            }

            // 2. 对于大文档，分段处理
            List<DocumentChunk> chunks = new ArrayList<>();

            if (content.length() > config.getMaxChunkSize() * 3) {
                // 超大文档：先粗分，再让 LLM 精细分块
                log.info("📚 超大文档，采用分段策略");
                chunks = chunkLargeDocument(content, config);
            } else {
                // 中等文档：直接用 LLM 分块
                log.info("📖 中等文档，直接 LLM 分块");
                chunks = chunkWithLLM(content, config);
            }

            // 3. 设置索引
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).setIndex(i);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ LLM 分块完成：{} 块，耗时: {}ms", chunks.size(), duration);

            return chunks;

        } catch (Exception e) {
            log.error("❌ LLM 分块失败", e);
            throw new PPLException(PPLProviderType.ONNX,
                    "LLM chunking failed: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 LLM 对文档进行智能分块
     */
    private List<DocumentChunk> chunkWithLLM(String content, ChunkConfig config) {
        try {
            // 构建提示词（使用配置的模板）
            String prompt = promptTemplate
                    .replace("{minSize}", String.valueOf(config.getMinChunkSize()))
                    .replace("{maxSize}", String.valueOf(config.getMaxChunkSize()))
                    .replace("{content}", content);

            // 调用 LLM
            log.debug("🤖 调用 LLM 进行分块分析...");
            String response = llmClient.generate(prompt);

            // 解析 LLM 返回的结果
            List<DocumentChunk> chunks = parseChunkResponse(response, content, config);

            if (chunks.isEmpty()) {
                // LLM 未返回有效分块，使用原文
                log.warn("⚠️ LLM 未返回有效分块，使用原文");
                return List.of(DocumentChunk.builder()
                        .content(content)
                        .build());
            }

            return chunks;

        } catch (Exception e) {
            log.warn("⚠️ LLM 分块失败，降级为简单分块: {}", e.getMessage());
            return fallbackChunk(content, config);
        }
    }

    /**
     * 超大文档的分段处理策略
     */
    private List<DocumentChunk> chunkLargeDocument(String content, ChunkConfig config) {
        List<DocumentChunk> allChunks = new ArrayList<>();

        // 1. 先按段落粗分
        List<String> coarseChunks = coarseChunkByParagraph(content, config.getMaxChunkSize() * 2);

        log.info("📑 超大文档粗分为 {} 段", coarseChunks.size());

        // 2. 对每段进行 LLM 精细分块
        for (int i = 0; i < coarseChunks.size(); i++) {
            String chunk = coarseChunks.get(i);
            log.debug("   处理第 {}/{} 段，长度: {}", i + 1, coarseChunks.size(), chunk.length());

            List<DocumentChunk> subChunks = chunkWithLLM(chunk, config);
            allChunks.addAll(subChunks);
        }

        return allChunks;
    }

    /**
     * 按段落进行粗分
     */
    private List<String> coarseChunkByParagraph(String content, int maxSize) {
        List<String> chunks = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = content.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() > maxSize && currentChunk.length() > 0) {
                // 当前块已满，保存并开始新块
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }

            currentChunk.append(para).append("\n\n");
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 解析 LLM 返回的分块结果
     */
    private List<DocumentChunk> parseChunkResponse(String response, String originalContent, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // 检查响应中是否包含 [CHUNK_SPLIT] 标记
        if (!response.contains("[CHUNK_SPLIT]")) {
            // LLM 可能直接返回了分段的内容，尝试其他分隔符
            return parseAlternativeFormat(response, config);
        }

        // 按 [CHUNK_SPLIT] 分割
        String[] parts = response.split("\\[CHUNK_SPLIT\\]");

        for (String part : parts) {
            String trimmed = part.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // 过滤掉提示词中的内容
            if (trimmed.contains("你是一个文档分块专家") ||
                trimmed.contains("# 分块要求") ||
                trimmed.contains("# 输出格式")) {
                continue;
            }

            // 检查大小限制
            if (trimmed.length() >= config.getMinChunkSize() &&
                trimmed.length() <= config.getMaxChunkSize() * 1.5) {

                chunks.add(DocumentChunk.builder()
                        .content(trimmed)
                        .build());
            } else if (trimmed.length() > config.getMaxChunkSize() * 1.5) {
                // 太大，进一步分割
                List<DocumentChunk> subChunks = splitLargeChunk(trimmed, config);
                chunks.addAll(subChunks);
            }
        }

        return chunks;
    }

    /**
     * 解析其他格式的 LLM 响应
     */
    private List<DocumentChunk> parseAlternativeFormat(String response, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // 尝试按编号分割（如：1. 2. 3.）
        Pattern pattern = Pattern.compile("(?:^|\\n)\\d+\\.\\s*(.+?)(?=\\n\\d+\\.|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            String content = matcher.group(1).trim();

            if (content.length() >= config.getMinChunkSize()) {
                chunks.add(DocumentChunk.builder()
                        .content(content)
                        .build());
            }
        }

        // 如果没有找到编号格式，返回空（降级到 fallback）
        return chunks;
    }

    /**
     * 分割过大的块
     */
    private List<DocumentChunk> splitLargeChunk(String content, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int maxSize = config.getMaxChunkSize();
        int overlapSize = config.getOverlapSize();

        for (int i = 0; i < content.length(); i += maxSize - overlapSize) {
            int end = Math.min(i + maxSize, content.length());
            String chunkContent = content.substring(i, end);

            chunks.add(DocumentChunk.builder()
                    .content(chunkContent)
                    .build());

            if (end >= content.length()) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 降级分块策略（简单按段落分割）
     */
    private List<DocumentChunk> fallbackChunk(String content, ChunkConfig config) {
        log.info("📝 使用降级分块策略");

        List<DocumentChunk> chunks = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = content.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() > config.getMaxChunkSize() &&
                currentChunk.length() > 0) {

                chunks.add(DocumentChunk.builder()
                        .content(currentChunk.toString().trim())
                        .build());
                currentChunk = new StringBuilder();
            }

            currentChunk.append(para).append("\n\n");
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                    .content(currentChunk.toString().trim())
                    .build());
        }

        return chunks;
    }

    @Override
    public String getStrategyName() {
        return "LLM-based Chunking";
    }

    @Override
    public boolean isAvailable() {
        return enabled && llmClient != null;
    }
}

