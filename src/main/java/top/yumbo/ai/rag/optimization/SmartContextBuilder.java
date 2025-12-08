package top.yumbo.ai.rag.optimization;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.chunking.ChunkingConfig;
import top.yumbo.ai.rag.chunking.ChunkingStrategy;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.DocumentChunker;
import top.yumbo.ai.rag.chunking.DocumentChunkerFactory;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能上下文构建器（Smart context builder）
 * 解决大文档RAG的上下文窗口限制问题（Solve the context window limitation problem for large document RAG）
 *
 * 核心功能：
 * 1. 动态调整文档长度以适应LLM上下文限制（Dynamically adjust document length to fit LLM context limits）
 * 2. 智能分块策略，确保内容不丢失（Smart chunking strategy to ensure no content is lost）
 * 3. 在句子边界处切分，保持语义完整性（Split at sentence boundaries to maintain semantic integrity）
 * 4. 优先保留包含查询关键词的内容（Prioritize content containing query keywords）
 * 5. 当文档过长时，分成多个语义完整的块（When documents are too long, split into multiple semantically complete chunks）
 * 6. 支持可配置的切分策略（SIMPLE/SMART_KEYWORD/AI_SEMANTIC）（Support configurable chunking strategies (SIMPLE/SMART_KEYWORD/AI_SEMANTIC)）
 *
 * 版本历史：
 * v1.0 (2025-11-22) - 初始版本（Initial version）
 * v1.1 (2025-11-26) - 添加可配置切分器支持（Added configurable chunker support）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class SmartContextBuilder {

    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 8000;  // 总上下文限制
    private static final int DEFAULT_MAX_DOC_LENGTH = 2000;      // 单个文档最大长度
    private static final int KEYWORD_WINDOW_SIZE = 500;          // 关键词搜索窗口
    private static final int SENTENCE_BOUNDARY_SEARCH = 100;     // 句子边界搜索范围
    private static final int CHUNK_OVERLAP = 100;                // 分块重叠大小，保证上下文连贯

    private final int maxContextLength;
    private final int maxDocLength;
    private final boolean preserveFullContent;  // 是否保留完整内容
    private final DocumentChunker chunker;       // 文档切分器（新增）
    private ChunkStorageService chunkStorageService;  // 文档块存储服务
    private String currentDocumentId;            // 当前处理的文档ID

    /**
     * Spring 自动装配构造函数
     */
    @Autowired
    public SmartContextBuilder(
            @Autowired(required = false) ChunkStorageService chunkStorageService) {
        this(DEFAULT_MAX_CONTEXT_LENGTH, DEFAULT_MAX_DOC_LENGTH, true, null, null, null, chunkStorageService);
    }

    public SmartContextBuilder() {
        this(DEFAULT_MAX_CONTEXT_LENGTH, DEFAULT_MAX_DOC_LENGTH, true);
    }

    public SmartContextBuilder(int maxContextLength, int maxDocLength) {
        this(maxContextLength, maxDocLength, true);
    }

    public SmartContextBuilder(int maxContextLength, int maxDocLength, boolean preserveFullContent) {
        this(maxContextLength, maxDocLength, preserveFullContent, null, null, null);
    }

    /**
     * 完整构造函数（新增）
     * 支持可配置的切分策略
     *
     * @param maxContextLength 最大上下文长度
     * @param maxDocLength 单文档最大长度
     * @param preserveFullContent 是否保留完整内容（已废弃，由策略控制）
     * @param chunkingConfig 切分配置
     * @param chunkingStrategy 切分策略
     * @param llmClient LLM 客户端（仅 AI_SEMANTIC 策略需要）
     */
    public SmartContextBuilder(int maxContextLength, int maxDocLength,
                              boolean preserveFullContent,
                              ChunkingConfig chunkingConfig,
                              ChunkingStrategy chunkingStrategy,
                              LLMClient llmClient) {
        this(maxContextLength, maxDocLength, preserveFullContent, chunkingConfig, chunkingStrategy, llmClient, null);
    }

    /**
     * 完整构造函数（含存储服务）
     */
    public SmartContextBuilder(int maxContextLength, int maxDocLength,
                              boolean preserveFullContent,
                              ChunkingConfig chunkingConfig,
                              ChunkingStrategy chunkingStrategy,
                              LLMClient llmClient,
                              top.yumbo.ai.rag.chunking.storage.ChunkStorageService chunkStorageService) {
        this.maxContextLength = maxContextLength;
        this.maxDocLength = maxDocLength;
        this.preserveFullContent = preserveFullContent;
        this.chunkStorageService = chunkStorageService;

        // 创建文档切分器
        if (chunkingConfig != null && chunkingStrategy != null) {
            this.chunker = DocumentChunkerFactory.createChunker(
                chunkingStrategy, chunkingConfig, llmClient
            );
            log.info(I18N.get("log.optimization.context.initialized_with_chunker",
                chunkingStrategy, maxContextLength, maxDocLength, chunkStorageService != null ? "enabled" : "disabled"));
        } else {
            this.chunker = null;
            log.info(I18N.get("log.optimization.context.initialized",
                maxContextLength, maxDocLength, preserveFullContent));
        }
    }

    /**
     * 设置当前文档ID（用于保存切分块）
     */
    public void setCurrentDocumentId(String documentId) {
        this.currentDocumentId = documentId;
    }

    /**
     * 构建智能上下文
     *
     * @param query 用户查询
     * @param documents 检索到的文档列表
     * @return 优化后的上下文字符串
     */
    public String buildSmartContext(String query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        log.debug("Building smart context for query: {}, documents: {}",
            query, documents.size());

        StringBuilder context = new StringBuilder();
        int remainingLength = maxContextLength;
        int processedDocs = 0;

        for (Document doc : documents) {
            if (remainingLength <= 0) {
                log.debug("Context length limit reached, processed {} documents", processedDocs);
                break;
            }

            // 计算这个文档可以使用的最大长度
            int allowedLength = Math.min(maxDocLength, remainingLength);

            // 提取最相关的片段
            String relevantPart = extractRelevantPart(
                query,
                doc.getContent(),
                allowedLength
            );

            if (!relevantPart.isEmpty()) {
                // 添加文档标记和内容
                String docSection = formatDocumentSection(doc, relevantPart);
                context.append(docSection);

                // 更新剩余长度（包括格式化字符）
                remainingLength -= docSection.length();
                processedDocs++;

                log.trace("Added document: {}, length: {}, remaining: {}",
                    doc.getTitle(), docSection.length(), remainingLength);
            }
        }

        String result = context.toString();
        log.info(I18N.get("log.optimization.context.built",
            result.length(), processedDocs, result.length() * 100 / maxContextLength));

        return result;
    }

    /**
     * 提取最相关的片段
     *
     * 支持两种模式：
     * 1. 使用新的可配置切分器（如果已配置）
     * 2. 使用原有的智能分块逻辑（向后兼容）
     */
    private String extractRelevantPart(String query, String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 如果内容本身就不超长，直接返回
        if (content.length() <= maxLength) {
            return content;
        }

        // 优先使用新的切分器
        if (chunker != null) {
            return extractWithChunker(query, content, maxLength);
        }

        // 降级到原有逻辑（向后兼容）
        if (preserveFullContent) {
            return extractWithChunking(query, content, maxLength);
        } else {
            return extractMostRelevantPart(query, content, maxLength);
        }
    }

    /**
     * 使用配置的切分器提取内容（新增）
     */
    private String extractWithChunker(String query, String content, int maxLength) {
        try {
            // 使用切分器切分文档
            List<DocumentChunk> chunks = chunker.chunk(content, query);

            if (chunks.isEmpty()) {
                log.warn("Chunker returned no chunks, using fallback");
                return extractWithChunking(query, content, maxLength);
            }

            // 保存切分块到文件系统
            if (chunkStorageService != null && currentDocumentId != null) {
                try {
                    List<top.yumbo.ai.rag.chunking.storage.ChunkStorageInfo> savedChunks =
                        chunkStorageService.saveChunks(currentDocumentId, chunks);
                    log.info("✅ Saved {} chunks for document: {}", savedChunks.size(), currentDocumentId);
                } catch (Exception e) {
                    log.warn("Failed to save chunks for document: {}", currentDocumentId, e);
                }
            }

            // 选择最相关的块
            List<DocumentChunk> selectedChunks = selectBestChunks(chunks, maxLength);

            // 合并块内容
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < selectedChunks.size(); i++) {
                DocumentChunk chunk = selectedChunks.get(i);

                if (i > 0) {
                    result.append("\n...\n");
                }

                // 如果有标题，添加标题
                if (chunk.getTitle() != null && !chunk.getTitle().isEmpty()) {
                    result.append("[").append(chunk.getTitle()).append("]\n");
                }

                result.append(chunk.getContent());
            }

            log.debug("Extracted with chunker: {} chunks, {} chars from {} chars",
                selectedChunks.size(), result.length(), content.length());

            return result.toString();

        } catch (Exception e) {
            log.error("Error using chunker, falling back to default strategy", e);
            return extractWithChunking(query, content, maxLength);
        }
    }

    /**
     * 选择最相关的文档块（新增）
     *
     * 策略：
     * 1. 如果块总大小 <= maxLength，返回所有块
     * 2. 否则，按相关性排序，选择最相关的块直到达到 maxLength
     */
    private List<DocumentChunk> selectBestChunks(List<DocumentChunk> chunks, int maxLength) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        // 计算总大小
        int totalSize = chunks.stream()
            .mapToInt(DocumentChunk::getLength)
            .sum();

        // 如果总大小不超过限制，返回所有块
        if (totalSize <= maxLength) {
            return chunks;
        }

        // 按块的元数据优先级排序（包含关键词的块优先）
        List<DocumentChunk> sortedChunks = chunks.stream()
            .sorted((c1, c2) -> {
                // 优先选择有关键词标记的块
                boolean c1HasKeyword = c1.getMetadata() != null &&
                                      c1.getMetadata().contains("keyword");
                boolean c2HasKeyword = c2.getMetadata() != null &&
                                      c2.getMetadata().contains("keyword");

                if (c1HasKeyword && !c2HasKeyword) return -1;
                if (!c1HasKeyword && c2HasKeyword) return 1;

                // 其次按位置（前面的块优先）
                return Integer.compare(c1.getIndex(), c2.getIndex());
            })
            .collect(Collectors.toList());

        // 贪心选择块直到达到 maxLength
        List<DocumentChunk> selected = new java.util.ArrayList<>();
        int currentLength = 0;

        for (DocumentChunk chunk : sortedChunks) {
            int chunkLength = chunk.getLength();

            // 如果添加这个块会超出限制
            if (currentLength + chunkLength > maxLength) {
                // 如果还没选择任何块，至少选择第一个块（截断）
                if (selected.isEmpty()) {
                    selected.add(chunk);
                }
                break;
            }

            selected.add(chunk);
            currentLength += chunkLength;
        }

        // 按原始顺序排序
        selected.sort(Comparator.comparingInt(DocumentChunk::getIndex));

        return selected;
    }

    /**
     * 智能分块策略 - 保留所有内容
     * 将长文档分成多个语义完整的块，每个块优先包含关键词
     */
    private String extractWithChunking(String query, String content, int maxLength) {
        String[] keywords = extractKeywords(query);
        StringBuilder result = new StringBuilder();

        // 找到所有关键词位置
        List<Integer> keywordPositions = findAllKeywordPositions(content.toLowerCase(), keywords);

        if (keywordPositions.isEmpty()) {
            // 如果没有关键词，按顺序分块
            return chunkBySize(content, maxLength);
        }

        // 按关键词位置分块
        int processedLength = 0;
        for (int i = 0; i < keywordPositions.size() && processedLength < content.length(); i++) {
            int keywordPos = keywordPositions.get(i);

            // 跳过已经处理过的位置
            if (keywordPos < processedLength) {
                continue;
            }

            // 计算这个块的起始和结束位置
            int start = Math.max(processedLength, keywordPos - maxLength / 3);
            int end = Math.min(content.length(), keywordPos + maxLength * 2 / 3);

            // 确保不超过maxLength
            if (end - start > maxLength) {
                end = start + maxLength;
            }

            // 调整到句子边界
            start = adjustToSentenceStart(content, start);
            end = adjustToSentenceEnd(content, end);

            // 提取块
            String chunk = content.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append("\n...\n");
                }
                result.append(chunk);
                processedLength = end;
            }
        }

        // 如果还有未处理的内容，添加剩余部分的摘要
        if (processedLength < content.length()) {
            int remaining = content.length() - processedLength;
            result.append(I18N.get("log.optimization.context.remaining_chars", remaining));
        }

        String extracted = result.toString();
        log.debug("Extracted with chunking: original={}chars, extracted={}chars, chunks={}",
            content.length(), extracted.length(), keywordPositions.size());

        return extracted;
    }

    /**
     * 提取最相关片段策略（原有逻辑）
     */
    private String extractMostRelevantPart(String query, String content, int maxLength) {
        // 提取查询关键词
        String[] keywords = extractKeywords(query);

        // 查找包含最多关键词的位置
        int bestPosition = findBestPosition(content.toLowerCase(), keywords);

        // 以最佳位置为中心提取片段
        int start = Math.max(0, bestPosition - maxLength / 2);
        int end = Math.min(content.length(), start + maxLength);

        // 如果end到达末尾，调整start
        if (end == content.length() && content.length() > maxLength) {
            start = content.length() - maxLength;
        }

        // 调整到句子边界
        start = adjustToSentenceStart(content, start);
        end = adjustToSentenceEnd(content, end);

        // 提取内容
        String extracted = content.substring(start, end).trim();

        // 添加省略标记
        if (start > 0) {
            extracted = "..." + extracted;
        }
        if (end < content.length()) {
            extracted = extracted + "...";
        }

        log.debug("Extracted most relevant part: original={}chars, extracted={}chars, " +
                "bestPos={}, range=[{}, {}]",
            content.length(), extracted.length(), bestPosition, start, end);

        return extracted;
    }

    /**
     * 按大小简单分块（当没有找到关键词时使用）
     */
    private String chunkBySize(String content, int chunkSize) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        int chunkCount = 0;

        while (pos < content.length() && chunkCount < 3) { // 最多3块
            int end = Math.min(content.length(), pos + chunkSize);
            end = adjustToSentenceEnd(content, end);

            String chunk = content.substring(pos, end).trim();
            if (!chunk.isEmpty()) {
                if (!result.isEmpty()) {
                    result.append("\n...\n");
                }
                result.append(chunk);
                chunkCount++;
            }

            pos = end;
        }

        if (pos < content.length()) {
            int remaining = content.length() - pos;
            result.append(I18N.get("log.optimization.context.remaining_chars", remaining));
        }

        return result.toString();
    }

    /**
     * 找到所有关键词出现的位置
     */
    private List<Integer> findAllKeywordPositions(String content, String[] keywords) {
        List<Integer> positions = new java.util.ArrayList<>();

        for (String keyword : keywords) {
            int index = 0;
            while ((index = content.indexOf(keyword, index)) != -1) {
                positions.add(index);
                index += keyword.length();
            }
        }

        // 排序并去重
        return positions.stream()
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 提取关键词（去除停用词）
     */
    private String[] extractKeywords(String query) {
        // 简单的停用词列表
        List<String> stopWords = Arrays.asList(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她",
            "什么", "怎么", "如何", "为什么", "吗", "呢", "啊", "了",
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "what", "how", "why", "when", "where", "who"
        );

        return Arrays.stream(query.toLowerCase().split("[\\s\\p{Punct}]+"))
            .filter(word -> !stopWords.contains(word) && word.length() > 1)
            .toArray(String[]::new);
    }

    /**
     * 查找最佳位置（关键词密度最高的区域）
     */
    private int findBestPosition(String content, String[] keywords) {
        if (keywords.length == 0) {
            return 0;
        }

        int bestPos = 0;
        int maxScore = 0;

        // 使用滑动窗口找到关键词密度最高的区域
        for (int i = 0; i < content.length() - KEYWORD_WINDOW_SIZE; i += KEYWORD_WINDOW_SIZE / 2) {
            int windowEnd = Math.min(i + KEYWORD_WINDOW_SIZE, content.length());
            String window = content.substring(i, windowEnd);

            // 计算这个窗口的得分（关键词出现次数）
            int score = 0;
            for (String keyword : keywords) {
                score += countOccurrences(window, keyword);
            }

            if (score > maxScore) {
                maxScore = score;
                bestPos = i + KEYWORD_WINDOW_SIZE / 2; // 窗口中心
            }
        }

        log.trace("Best position found at {} with score {}", bestPos, maxScore);
        return bestPos;
    }

    /**
     * 统计词出现次数
     */
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }

    /**
     * 调整到句子开始位置
     */
    private int adjustToSentenceStart(String text, int pos) {
        if (pos <= 0) {
            return 0;
        }

        // 向前搜索句子结束符
        int searchStart = Math.max(0, pos - SENTENCE_BOUNDARY_SEARCH);
        for (int i = pos - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                // 找到句子结束符，返回下一个字符位置
                return i + 1;
            }
        }

        // 如果没找到，返回原位置
        return pos;
    }

    /**
     * 调整到句子结束位置
     */
    private int adjustToSentenceEnd(String text, int pos) {
        if (pos >= text.length()) {
            return text.length();
        }

        // 向后搜索句子结束符
        int searchEnd = Math.min(text.length(), pos + SENTENCE_BOUNDARY_SEARCH);
        for (int i = pos; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (isSentenceEnding(c)) {
                // 找到句子结束符，包含这个字符
                return i + 1;
            }
        }

        // 如果没找到，返回原位置
        return pos;
    }

    /**
     * 判断是否是句子结束符
     */
    private boolean isSentenceEnding(char c) {
        return c == '。' || c == '.' || c == '!' || c == '！' ||
               c == '?' || c == '？' || c == '\n' || c == ';' || c == '；';
    }

    /**
     * 格式化文档片段
     */
    private String formatDocumentSection(Document doc, String content) {
        return String.format(
            "\n【文档：%s】\n%s\n",
            doc.getTitle(),
            content
        );
    }

    /**
     * 获取上下文统计信息
     */
    public ContextStats getContextStats(String context) {
        int totalLength = context.length();
        int documentCount = context.split("【文档：").length - 1;
        double utilization = (double) totalLength / maxContextLength * 100;

        return new ContextStats(totalLength, documentCount, utilization);
    }

    /**
     * 上下文统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContextStats {
        private int totalLength;
        private int documentCount;
        private double utilization;

        @Override
        public String toString() {
            return String.format("ContextStats[length=%d, docs=%d, utilization=%.1f%%]",
                totalLength, documentCount, utilization);
        }
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private int maxContextLength = DEFAULT_MAX_CONTEXT_LENGTH;
        private int maxDocLength = DEFAULT_MAX_DOC_LENGTH;
        private boolean preserveFullContent = true;

        public Builder maxContextLength(int length) {
            this.maxContextLength = length;
            return this;
        }

        public Builder maxDocLength(int length) {
            this.maxDocLength = length;
            return this;
        }

        public Builder preserveFullContent(boolean preserve) {
            this.preserveFullContent = preserve;
            return this;
        }

        public SmartContextBuilder build() {
            return new SmartContextBuilder(maxContextLength, maxDocLength, preserveFullContent);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

