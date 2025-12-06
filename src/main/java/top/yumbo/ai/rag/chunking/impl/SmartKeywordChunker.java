package top.yumbo.ai.rag.chunking.impl;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.ChunkingConfig;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.chunking.DocumentChunker;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能关键词文档切分器 (Smart keyword document chunker)
 * 优先提取包含查询关键词的内容，平衡效果和性能
 * (Extracts keyword-containing portions first to balance quality and performance)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class SmartKeywordChunker implements DocumentChunker {

    private final ChunkingConfig config;

    // 停用词列表 (stop words)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她", "它",
            "什么", "怎么", "如何", "为什么", "吗", "呢", "啊", "了", "着", "过",
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "what", "how", "why", "when", "where", "who", "which", "this", "that"
    ));

    public SmartKeywordChunker(ChunkingConfig config) {
        this.config = config;
        config.validate();
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        // 提取关键词
        List<String> keywords = extractKeywords(query);

        // 如果没有关键词或内容不长，使用简单切分
        if (keywords.isEmpty() || content.length() <= config.getChunkSize()) {
            return new SimpleDocumentChunker(config).chunk(content, query);
        }

        // 找到所有关键词位置
        List<KeywordPosition> positions = findKeywordPositions(content, keywords);

        if (positions.isEmpty()) {
            // 没有找到关键词，使用简单切分
            log.debug(I18N.get("log.chunk.no_keywords_fallback"));
            return new SimpleDocumentChunker(config).chunk(content, query);
        }

        // 基于关键词位置生成切分块
        List<DocumentChunk> chunks = generateChunksAroundKeywords(content, positions);

        log.debug(I18N.get("log.chunk.smart_summary", content.length(), chunks.size(), keywords.size()));

        return chunks;
    }

    /**
     * 提取关键词 (Extract keywords)
     */
    private List<String> extractKeywords(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }

        List<String> keywords = new ArrayList<>();

        // 中文分词：按字符提取（简单但有效）
        // 提取 2-4 个字的词组
        String trimmed = query.trim();
        for (int len = 4; len >= 2; len--) {
            for (int i = 0; i <= trimmed.length() - len; i++) {
                String word = trimmed.substring(i, i + len);
                // 过滤停用词和标点
                if (!STOP_WORDS.contains(word) && !word.matches(".*[\\p{Punct}\\s]+.*")) {
                    keywords.add(word);
                }
            }
        }

        // 英文分词
        Arrays.stream(query.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(word -> !STOP_WORDS.contains(word) && word.length() > 2)
                .forEach(keywords::add);

        // 去重并限制数量
        List<String> result = keywords.stream()
                .distinct()
                .limit(20) // 最多 20 个关键词
                .collect(Collectors.toList());

        if (!result.isEmpty()) {
            log.debug(I18N.get("log.chunk.keywords_extracted", result.size(), query));
        }

        return result;
    }

    /**
     * 找到所有关键词位置 (Find keyword positions)
     */
    private List<KeywordPosition> findKeywordPositions(String content, List<String> keywords) {
        List<KeywordPosition> positions = new ArrayList<>();
        String lowerContent = content.toLowerCase();

        for (String keyword : keywords) {
            int index = 0;
            while ((index = lowerContent.indexOf(keyword, index)) != -1) {
                positions.add(new KeywordPosition(index, keyword));
                index += keyword.length();
            }
        }

        // 按位置排序
        positions.sort(Comparator.comparingInt(p -> p.position));

        return positions;
    }

    /**
     * 基于关键词位置生成切分块 (Generate chunks around keywords)
     */
    private List<DocumentChunk> generateChunksAroundKeywords(String content, List<KeywordPosition> positions) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        Set<Integer> coveredRanges = new HashSet<>();
        int index = 0;

        // 为每个关键词位置生成一个块
        for (KeywordPosition kp : positions) {
            int center = kp.position;

            // 如果这个位置已经被覆盖，跳过
            if (coveredRanges.contains(center / 100)) {
                continue;
            }

            // 计算块的起始和结束位置（以关键词为中心）
            int start = Math.max(0, center - chunkSize / 2);
            int end = Math.min(content.length(), start + chunkSize);

            // 如果end到达末尾，调整start
            if (end == content.length() && content.length() > chunkSize) {
                start = Math.max(0, content.length() - chunkSize);
            }

            // 调整到句子边界
            if (config.isSplitOnSentence()) {
                start = adjustToSentenceStart(content, start);
                end = adjustToSentenceEnd(content, end);
            }

            // 提取内容
            String chunkContent = content.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                chunks.add(DocumentChunk.builder()
                        .content(chunkContent)
                        .index(index)
                        .totalChunks(-1) // 后面更新
                        .startPosition(start)
                        .endPosition(end)
                        .metadata("keyword:" + kp.keyword)
                        .build());

                // 标记已覆盖的范围（以100字符为单位）
                for (int i = start; i < end; i += 100) {
                    coveredRanges.add(i / 100);
                }

                index++;
            }
        }

        // 如果切分后覆盖不全，补充剩余部分
        if (chunks.isEmpty() || getTotalCoverage(chunks) < content.length() * 0.5) {
            log.debug(I18N.get("log.chunk.coverage_low"));
            chunks.addAll(addSequentialChunks(content, chunks, chunkSize, overlap));
        }

        // 按位置排序并更新索引
        chunks.sort(Comparator.comparingInt(DocumentChunk::getStartPosition));
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setIndex(i);
            chunks.get(i).setTotalChunks(chunks.size());
        }

        return chunks;
    }

    /**
     * 添加顺序块以覆盖未覆盖的部分 (Add sequential chunks)
     */
    private List<DocumentChunk> addSequentialChunks(String content, List<DocumentChunk> existingChunks,
                                                     int chunkSize, int overlap) {
        List<DocumentChunk> additionalChunks = new ArrayList<>();
        Set<Integer> coveredRanges = new HashSet<>();

        // 标记已覆盖的范围
        for (DocumentChunk chunk : existingChunks) {
            for (int i = chunk.getStartPosition(); i < chunk.getEndPosition(); i += 100) {
                coveredRanges.add(i / 100);
            }
        }

        // 补充未覆盖的部分
        int position = 0;
        int index = existingChunks.size();

        while (position < content.length()) {
            // 检查这个位置是否已被覆盖
            if (!coveredRanges.contains(position / 100)) {
                int end = Math.min(position + chunkSize, content.length());

                if (config.isSplitOnSentence()) {
                    end = adjustToSentenceEnd(content, end);
                }

                String chunkContent = content.substring(position, end).trim();

                if (!chunkContent.isEmpty()) {
                    additionalChunks.add(DocumentChunk.builder()
                            .content(chunkContent)
                            .index(index++)
                            .totalChunks(-1)
                            .startPosition(position)
                            .endPosition(end)
                            .metadata("sequential")
                            .build());
                }
            }

            position += chunkSize - overlap;
        }

        return additionalChunks;
    }

    /**
     * 计算总覆盖率 (Compute total coverage)
     */
    private int getTotalCoverage(List<DocumentChunk> chunks) {
        Set<Integer> covered = new HashSet<>();
        for (DocumentChunk chunk : chunks) {
            for (int i = chunk.getStartPosition(); i < chunk.getEndPosition(); i++) {
                covered.add(i);
            }
        }
        return covered.size();
    }

    /**
     * 调整到句子开始位置 (Adjust to sentence start)
     */
    private int adjustToSentenceStart(String text, int pos) {
        if (pos <= 0) return 0;

        int searchRange = Math.min(100, pos);
        int lowerBound = pos - searchRange;
        for (int i = pos - 1; i >= lowerBound; i--) {
            if (isSentenceEnding(text.charAt(i))) {
                return i + 1;
            }
        }
        return pos;
    }

    /**
     * 调整到句子结束位置 (Adjust to sentence end)
     */
    private int adjustToSentenceEnd(String text, int pos) {
        if (pos >= text.length()) return text.length();

        int searchRange = Math.min(100, text.length() - pos);
        for (int i = pos; i < pos + searchRange && i < text.length(); i++) {
            if (isSentenceEnding(text.charAt(i))) {
                return i + 1;
            }
        }
        return pos;
    }

    /**
     * 判断是否是句子结束符 (Check sentence ending)
     */
    private boolean isSentenceEnding(char c) {
        return c == '。' || c == '！' || c == '？' ||
               c == '.' || c == '!' || c == '?' ||
               c == '\n' || c == '\r';
    }

    @Override
    public String getName() {
        return "Smart Keyword Chunker";
    }

    @Override
    public String getDescription() {
        return "智能关键词切分，优先保留相关内容";
    }

    /**
     * 关键词位置 (Keyword position)
     */
    private static class KeywordPosition {
        int position;
        String keyword;

        KeywordPosition(int position, String keyword) {
            this.position = position;
            this.keyword = keyword;
        }
    }
}

