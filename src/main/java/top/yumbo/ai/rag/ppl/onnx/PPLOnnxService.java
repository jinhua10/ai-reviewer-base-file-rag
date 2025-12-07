package top.yumbo.ai.rag.ppl.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.PPLMetrics;
import top.yumbo.ai.rag.ppl.PPLProviderType;
import top.yumbo.ai.rag.ppl.PPLService;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.ppl.config.RerankConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 ONNX Runtime 的 PPL 服务实现
 * (ONNX Runtime based PPL Service Implementation)
 *
 * 特点 (Features):
 * - 本地嵌入式推理，无网络开销 (Local embedded inference, no network overhead)
 * - 速度快（30-150ms）(Fast speed: 30-150ms)
 * - 成本低（完全免费）(Low cost: completely free)
 * - 支持 GPU 加速 (Supports GPU acceleration)
 * - 支持中英文混合文档 (Supports Chinese-English mixed documents)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
@Slf4j
@ConditionalOnProperty(prefix = "knowledge.qa.ppl.onnx", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PPLOnnxService implements PPLService {

    private final PPLConfig config;
    private final PPLMetrics metrics;

    // ONNX Runtime 组件
    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    // PPL 缓存
    private Cache<String, Double> pplCache;

    public PPLOnnxService(PPLConfig config) {
        this.config = config;
        this.metrics = new PPLMetrics();
    }

    @PostConstruct
    public void init() {
        log.info(I18N.get("ppl_onnx.log.init_start"));

        try {
            PPLConfig.OnnxConfig onnxConfig = config.getOnnx();

            log.info(I18N.get("ppl_onnx.log.model_path", onnxConfig.getModelPath()));
            log.info(I18N.get("ppl_onnx.log.tokenizer_path", onnxConfig.getTokenizerPath()));

            // 1. 初始化 ONNX Runtime 环境
            this.env = OrtEnvironment.getEnvironment();
            log.info(I18N.get("ppl_onnx.log.env_created"));

            // 2. 加载 ONNX 模型
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            this.session = env.createSession(onnxConfig.getModelPath(), sessionOptions);
            log.info(I18N.get("ppl_onnx.log.model_loaded", onnxConfig.getModelPath()));

            // 3. 加载 Tokenizer
            this.tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(onnxConfig.getTokenizerPath()));
            log.info(I18N.get("ppl_onnx.log.tokenizer_loaded", onnxConfig.getTokenizerPath()));

            // 4. 初始化缓存
            if (onnxConfig.isUseCache()) {
                this.pplCache = Caffeine.newBuilder()
                        .maximumSize(onnxConfig.getCacheSize())
                        .expireAfterWrite(Duration.ofSeconds(onnxConfig.getCacheTtl()))
                        .recordStats()
                        .build();
                log.info(I18N.get("ppl_onnx.log.cache_init",
                        onnxConfig.getCacheSize(), onnxConfig.getCacheTtl()));
            }

            log.info(I18N.get("ppl_onnx.log.init_success"));

        } catch (Exception e) {
            log.error(I18N.get("ppl_onnx.log.init_failed"), e);
            throw new RuntimeException(I18N.get("ppl_onnx.error.init_failed"), e);
        }
    }

    /**
     * 计算文本的困惑度
     * (Calculate perplexity for text)
     *
     * 困惑度是衡量语言模型对文本预测能力的指标，值越低表示文本越"流畅"
     * (Perplexity measures how well the language model predicts the text, lower is more "fluent")
     *
     * @param text 文本 (text)
     * @return 困惑度值 (perplexity value)
     * @throws PPLException 计算失败时抛出 (thrown when calculation fails)
     */
    @Override
    public double calculatePerplexity(String text) throws PPLException {
        if (text == null || text.trim().isEmpty()) {
            return Double.MAX_VALUE;
        }

        // 检查缓存 (Check cache)
        if (pplCache != null) {
            Double cached = pplCache.getIfPresent(text);
            if (cached != null) {
                metrics.recordCacheHit();
                return cached;
            }
            metrics.recordCacheMiss();
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. Tokenize - 将文本转换为 Token IDs (Convert text to Token IDs)
            Encoding encoding = tokenizer.encode(text);
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();

            if (inputIds.length == 0) {
                return Double.MAX_VALUE;
            }

            // 2. 准备 ONNX 输入 (Prepare ONNX inputs)
            Map<String, OnnxTensor> inputs = new HashMap<>();

            // 将 inputIds 转换为 [1, seq_len] 的张量 (Convert to [1, seq_len] tensor)
            long[][] inputIdsArray = new long[1][inputIds.length];
            inputIdsArray[0] = inputIds;

            long[][] attentionMaskArray = new long[1][attentionMask.length];
            attentionMaskArray[0] = attentionMask;

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsArray);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskArray);

            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // 3. 模型推理 (Model inference)
            try (OrtSession.Result results = session.run(inputs)) {
                // 获取 logits（模型输出）(Get logits - model output)
                OnnxValue logitsValue = results.get(0);
                float[][][] logits = (float[][][]) logitsValue.getValue();

                // 4. 计算困惑度 (Calculate perplexity)
                double totalLoss = 0.0;
                int validTokens = 0;

                // 对每个位置计算 cross-entropy loss (Calculate cross-entropy loss for each position)
                for (int i = 0; i < inputIds.length - 1; i++) {
                    int targetId = (int) inputIds[i + 1];
                    float[] probs = logits[0][i];

                    // Softmax 归一化 (Softmax normalization)
                    float maxLogit = Float.NEGATIVE_INFINITY;
                    for (float logit : probs) {
                        maxLogit = Math.max(maxLogit, logit);
                    }

                    double sumExp = 0.0;
                    for (float logit : probs) {
                        sumExp += Math.exp(logit - maxLogit);
                    }

                    double logProb = probs[targetId] - maxLogit - Math.log(sumExp);
                    totalLoss -= logProb;  // 等价于 += -logProb (equivalent to += -logProb)
                    validTokens++;
                }

                // PPL = exp(average loss)
                double ppl = validTokens > 0 ? Math.exp(totalLoss / validTokens) : Double.MAX_VALUE;

                // 清理资源 (Clean up resources)
                inputIdsTensor.close();
                attentionMaskTensor.close();

                // 缓存结果 (Cache result)
                if (pplCache != null) {
                    pplCache.put(text, ppl);
                }

                metrics.recordSuccess(System.currentTimeMillis() - startTime);
                return ppl;
            }

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            log.error(I18N.get("ppl_onnx.log.calc_ppl_failed",
                    text.substring(0, Math.min(50, text.length()))), e);
            throw new PPLException(PPLProviderType.ONNX,
                    I18N.get("ppl_onnx.error.calc_ppl_failed"), e);
        }
    }

    /**
     * 基于 PPL 的文档分块
     * (PPL-based document chunking)
     *
     * 使用困惑度检测语义边界，实现智能分块
     * (Use perplexity to detect semantic boundaries for intelligent chunking)
     *
     * @param content 文档内容 (document content)
     * @param query 查询（可选，用于查询感知分块）(query, optional, for query-aware chunking)
     * @param config 分块配置 (chunk configuration)
     * @return 文档块列表 (list of document chunks)
     * @throws PPLException PPL计算异常 (PPL计算异常)
     */
    @Override
    public List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();

        try {
            List<DocumentChunk> chunks = new ArrayList<>();

            // 1. 分句 - 按标点符号分割（支持中英文）
            // (Split into sentences by punctuation - supports Chinese and English)
            List<String> sentences = splitIntoSentences(content);

            if (sentences.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 如果启用粗分块，先按段落粗分
            // (If coarse chunking is enabled, first chunk by paragraphs)
            List<List<String>> coarseChunks = new ArrayList<>();
            if (config.isEnableCoarseChunking()) {
                coarseChunks = coarseChunk(sentences, config.getMaxChunkSize());
            } else {
                coarseChunks.add(sentences);
            }

            // 3. 对每个粗块进行 PPL 精细切分
            // (Fine-chunk each coarse chunk using PPL)
            int chunkIndex = 0;
            for (List<String> coarseChunk : coarseChunks) {
                List<DocumentChunk> fineChunks = pplBasedChunk(coarseChunk, config);

                // 设置索引 (Set index)
                for (DocumentChunk chunk : fineChunks) {
                    chunk.setIndex(chunkIndex++);
                }

                chunks.addAll(fineChunks);
            }

            metrics.recordSuccess(System.currentTimeMillis() - startTime);
            return chunks;

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            throw new PPLException(PPLProviderType.ONNX,
                    I18N.get("ppl_onnx.error.chunk_failed"), e);
        }
    }

    /**
     * 分句 - 按标点符号分割（支持中英文）
     * (Split into sentences - by punctuation marks, supports Chinese and English)
     *
     * 支持的分句符号 (Supported sentence delimiters):
     * - 中文：。！？ (Chinese: period, exclamation, question)
     * - 英文：. ! ? (English: period, exclamation, question)
     * - 保留句末标点 (Preserves trailing punctuation)
     *
     * @param content 文本内容 (text content)
     * @return 句子列表 (list of sentences)
     */
    private List<String> splitIntoSentences(String content) {
        List<String> sentences = new ArrayList<>();

        // 按中英文句号、问号、感叹号分割，保留分隔符
        // Split by Chinese/English periods, question marks, exclamation marks
        String[] parts = content.split("(?<=[。！？.!?])\\s*");

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part.trim());
            }
        }

        return sentences;
    }

    /**
     * 粗分块 - 语义感知的分割（优化版）
     * (Coarse chunking - semantic-aware splitting, optimized version)
     *
     * 改进点 (Improvements):
     * 1. 在语义边界切分，而不是固定字数 (Split at semantic boundaries, not fixed size)
     * 2. 支持上下文重叠，避免信息丢失 (Support context overlap to avoid information loss)
     * 3. 软限制 + 硬限制，保证块大小在合理范围 (Soft + hard limits for reasonable chunk size)
     *
     * @param sentences 句子列表 (list of sentences)
     * @param maxChunkSize 最大块大小 (maximum chunk size)
     * @return 粗分块列表 (list of coarse chunks)
     */
    private List<List<String>> coarseChunk(List<String> sentences, int maxChunkSize) {
        return semanticCoarseChunk(sentences, maxChunkSize, true, 2);
    }

    /**
     * 语义感知的粗分块
     * (Semantic-aware coarse chunking)
     *
     * @param sentences 句子列表 (list of sentences)
     * @param maxChunkSize 最大块大小 (maximum chunk size)
     * @param semanticAware 是否启用语义感知 (whether to enable semantic awareness)
     * @param overlapSentences 重叠句子数 (number of overlapping sentences)
     * @return 粗分块列表 (list of coarse chunks)
     */
    private List<List<String>> semanticCoarseChunk(List<String> sentences,
            int maxChunkSize, boolean semanticAware, int overlapSentences) {

        List<List<String>> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        List<String> overlapBuffer = new ArrayList<>();  // 重叠缓冲区 (overlap buffer)
        int currentSize = 0;

        // 目标大小为最大大小的 60%，软限制 (Target size is 60% of max, soft limit)
        int targetSize = (int) (maxChunkSize * 0.6);
        // 硬性上限为最大大小的 125% (Hard limit is 125% of max)
        int hardLimit = (int) (maxChunkSize * 1.25);

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int sentenceLength = sentence.length();

            boolean shouldSplit = false;

            if (semanticAware) {
                // 语义感知模式：到达目标大小后，在语义边界切分
                String prevSentence = i > 0 ? sentences.get(i - 1) : null;
                boolean isSemanticBoundary = isSemanticBoundary(sentence, prevSentence);

                if (currentSize >= targetSize && isSemanticBoundary) {
                    shouldSplit = true;
                }
            }

            // 硬性上限：超过则强制切分
            if (currentSize + sentenceLength > hardLimit && !currentChunk.isEmpty()) {
                shouldSplit = true;
            }

            if (shouldSplit && !currentChunk.isEmpty()) {
                // 添加当前块（包含前一块的尾部作为上下文）
                List<String> chunkWithContext = new ArrayList<>();
                if (!overlapBuffer.isEmpty()) {
                    chunkWithContext.addAll(overlapBuffer);
                }
                chunkWithContext.addAll(currentChunk);
                chunks.add(chunkWithContext);

                // 更新重叠缓冲区（保留最后 N 个句子）
                overlapBuffer.clear();
                if (overlapSentences > 0) {
                    int overlapStart = Math.max(0, currentChunk.size() - overlapSentences);
                    for (int j = overlapStart; j < currentChunk.size(); j++) {
                        overlapBuffer.add(currentChunk.get(j));
                    }
                }

                currentChunk.clear();
                currentSize = 0;
            }

            currentChunk.add(sentence);
            currentSize += sentenceLength;
        }

        // 处理最后一块
        if (!currentChunk.isEmpty()) {
            List<String> chunkWithContext = new ArrayList<>();
            if (!overlapBuffer.isEmpty()) {
                chunkWithContext.addAll(overlapBuffer);
            }
            chunkWithContext.addAll(currentChunk);
            chunks.add(chunkWithContext);
        }

        return chunks;
    }

    /**
     * 检测语义边界（支持中英文）
     * (Detect semantic boundary - supports Chinese and English)
     *
     * 检测规则 (Detection rules):
     * 1. 中文章节标题 (Chinese chapter titles): 第一章、第1节等
     * 2. 英文章节标题 (English chapter titles): Chapter 1, Section 2等
     * 3. Markdown 标题 (Markdown headings): # ## ###等
     * 4. 数字编号标题 (Numbered headings): 1. 1.1 等
     * 5. 中文过渡词 (Chinese transition words): 首先、其次、总之等
     * 6. 英文过渡词 (English transition words): First, Second, In conclusion等
     * 7. 列表结束 (End of list)
     * 8. 段落分隔标记 (Paragraph separators)
     *
     * @param current 当前句子 (current sentence)
     * @param previous 前一句子 (previous sentence)
     * @return 是否是语义边界 (whether it's a semantic boundary)
     */
    private boolean isSemanticBoundary(String current, String previous) {
        if (current == null || current.isEmpty()) {
            return false;
        }

        String trimmed = current.trim();
        String lowerTrimmed = trimmed.toLowerCase();

        // 1. 中文章节标题 (Chinese chapter titles)
        if (trimmed.matches("^第[一二三四五六七八九十百千零\\d]+[章节篇部条款项].*")) {
            return true;
        }

        // 2. 英文章节标题 (English chapter titles)
        if (lowerTrimmed.matches("^(chapter|section|part|article|appendix)\\s*\\d+.*")) {
            return true;
        }

        // 3. Markdown 标题 (Markdown headings)
        if (trimmed.matches("^#{1,6}\\s+.*")) {
            return true;
        }

        // 4. 数字编号标题 (Numbered headings): "1. xxx", "1.1 xxx", "(1) xxx"
        if (trimmed.matches("^\\d+(\\.\\d+)*[.、\\s].*") && trimmed.length() < 100) {
            return true;
        }
        if (trimmed.matches("^\\([\\d]+\\)\\s+.*") && trimmed.length() < 100) {
            return true;
        }

        // 5. 中文过渡词/段落开头词 (Chinese transition words)
        if (trimmed.matches("^(首先|其次|再次|然后|接着|最后|另外|此外|" +
                "综上|总之|因此|所以|总结|结论|概述|简介|背景|目的|" +
                "一方面|另一方面|与此同时|需要注意|值得一提|特别是|" +
                "第一|第二|第三|第四|第五|最终|总而言之|归纳起来|" +
                "换句话说|换言之|也就是说|具体来说|例如|比如).*")) {
            return true;
        }

        // 6. 英文过渡词 (English transition words)
        if (lowerTrimmed.matches("^(first(ly)?|second(ly)?|third(ly)?|fourth(ly)?|finally|lastly|" +
                "in conclusion|to summarize|in summary|to sum up|overall|" +
                "furthermore|moreover|additionally|besides|however|nevertheless|" +
                "on the other hand|in contrast|meanwhile|consequently|therefore|" +
                "for example|for instance|specifically|in particular|namely|" +
                "introduction|background|purpose|objective|conclusion|summary|" +
                "note that|it is worth noting|importantly|significantly)[,:\\s].*")) {
            return true;
        }

        // 7. 前一句是列表项结尾，当前不是列表项（列表结束）
        // (Previous sentence is list item, current is not - end of list)
        if (previous != null) {
            // 中文列表项 (Chinese list items)
            boolean prevIsChineseList = previous.trim().matches("^[\\d一二三四五六七八九十]+[.、）)].*");
            // 英文列表项 (English list items)
            boolean prevIsEnglishList = previous.trim().matches("^[-*•]\\s.*") ||
                    previous.trim().matches("^\\(?[a-zA-Z\\d]+[.):]\\s.*");
            boolean prevIsList = prevIsChineseList || prevIsEnglishList;

            boolean currIsChineseList = trimmed.matches("^[\\d一二三四五六七八九十]+[.、）)].*");
            boolean currIsEnglishList = trimmed.matches("^[-*•]\\s.*") ||
                    trimmed.matches("^\\(?[a-zA-Z\\d]+[.):]\\s.*");
            boolean currIsList = currIsChineseList || currIsEnglishList;

            if (prevIsList && !currIsList) {
                return true;
            }
        }

        // 8. 段落分隔标记 (Paragraph separators)
        if (trimmed.startsWith("[PARA]") || trimmed.startsWith("---") || trimmed.startsWith("***")) {
            return true;
        }

        return false;
    }

    /**
     * 基于 PPL 的精细切分
     * (PPL-based fine chunking)
     *
     * 使用困惑度(Perplexity)检测语义突变点，在突变点处切分
     * (Use perplexity to detect semantic transition points and split at those points)
     *
     * @param sentences 句子列表 (list of sentences)
     * @param config 分块配置 (chunk configuration)
     * @return 文档块列表 (list of document chunks)
     * @throws PPLException PPL计算异常 (PPL计算异常)
     */
    private List<DocumentChunk> pplBasedChunk(List<String> sentences, ChunkConfig config) throws PPLException {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        // 检测图片标记位置
        Set<Integer> imagePositions = detectImageMarkers(sentences);

        if (!imagePositions.isEmpty()) {
            log.debug("   🖼️ 检测到 {} 个图片位置标记", imagePositions.size());
        }

        // 计算每个句子的 PPL
        List<Double> pplScores = new ArrayList<>();
        for (String sentence : sentences) {
            double ppl = calculatePerplexity(sentence);
            pplScores.add(ppl);
        }

        // 找到 PPL 突变点（考虑图片位置）
        List<Integer> splitPoints = new ArrayList<>();
        splitPoints.add(0); // 起始点

        for (int i = 1; i < pplScores.size(); i++) {
            double currentPPL = pplScores.get(i);
            double prevPPL = pplScores.get(i - 1);

            // 计算 PPL 变化
            double pplDelta = Math.abs(currentPPL - prevPPL);

            // 如果附近有图片标记，降低切分权重
            if (isNearImagePosition(i, imagePositions)) {
                pplDelta *= 0.3;  // 大幅降低图片附近的切分概率
                log.debug("   📍 位置 {} 靠近图片，PPL 权重降低至 {}", i, pplDelta);
            }

            // PPL 变化超过阈值，且当前块不为空
            if (pplDelta > config.getPplThreshold()) {
                splitPoints.add(i);
            }
        }

        splitPoints.add(sentences.size()); // 结束点

        // 根据切分点生成块
        for (int i = 0; i < splitPoints.size() - 1; i++) {
            int start = splitPoints.get(i);
            int end = splitPoints.get(i + 1);

            StringBuilder chunkContent = new StringBuilder();
            for (int j = start; j < end; j++) {
                chunkContent.append(sentences.get(j));
                if (j < end - 1) {
                    chunkContent.append(" ");
                }
            }

            String content = chunkContent.toString();

            // 应用大小限制
            if (content.length() >= config.getMinChunkSize() &&
                content.length() <= config.getMaxChunkSize()) {

                DocumentChunk chunk = DocumentChunk.builder()
                        .content(content)
                        .build();
                chunks.add(chunk);
            } else if (content.length() > config.getMaxChunkSize()) {
                // 太大，需要进一步分割
                List<DocumentChunk> subChunks = splitLargeChunk(content, config);
                chunks.addAll(subChunks);
            } else if (content.length() < config.getMinChunkSize() && !chunks.isEmpty()) {
                // 太小，合并到前一个块
                DocumentChunk lastChunk = chunks.getLast();
                lastChunk.setContent(lastChunk.getContent() + " " + content);
            } else if (!content.trim().isEmpty()) {
                // 第一个块，即使小也保留
                DocumentChunk chunk = DocumentChunk.builder()
                        .content(content)
                        .build();
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * 分割过大的块
     * (Split chunks that are too large)
     *
     * 使用滑动窗口方式分割，支持重叠以保持上下文连贯性
     * (Use sliding window approach with overlap to maintain context continuity)
     *
     * @param content 内容 (content)
     * @param config 分块配置 (chunk configuration)
     * @return 分割后的块列表 (list of split chunks)
     */
    private List<DocumentChunk> splitLargeChunk(String content, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int maxSize = config.getMaxChunkSize();
        int overlapSize = config.getOverlapSize();

        for (int i = 0; i < content.length(); i += maxSize - overlapSize) {
            int end = Math.min(i + maxSize, content.length());
            String chunkContent = content.substring(i, end);

            DocumentChunk chunk = DocumentChunk.builder()
                    .content(chunkContent)
                    .build();
            chunks.add(chunk);

            if (end >= content.length()) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 检测文本中的图片标记位置
     * Detect image markers in text
     *
     * 图片标记格式：[图片-xxx：描述] 或 [图片-xxx.png：描述]
     *
     * @param sentences 句子列表
     * @return 包含图片标记的句子索引集合
     */
    private Set<Integer> detectImageMarkers(List<String> sentences) {
        Set<Integer> imagePositions = new HashSet<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            // 检测图片标记格式：[图片-xxx：
            if (sentence.contains("[图片-") || sentence.contains("[Image-")) {
                imagePositions.add(i);
                log.debug("   🖼️ 句子 {} 包含图片标记", i);
            }
        }

        return imagePositions;
    }

    /**
     * 判断位置是否靠近图片标记
     * Check if position is near image markers
     *
     * 策略：图片前后各2个句子范围内都认为是"靠近"
     *
     * @param position 当前位置
     * @param imagePositions 图片位置集合
     * @return 是否靠近图片
     */
    private boolean isNearImagePosition(int position, Set<Integer> imagePositions) {
        if (imagePositions.isEmpty()) {
            return false;
        }

        // 检查前后2个句子范围
        int range = 2;
        for (int offset = -range; offset <= range; offset++) {
            if (imagePositions.contains(position + offset)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 基于 PPL 的文档重排序
     * (PPL-based document reranking)
     *
     * 使用困惑度对候选文档重新排序，PPL 越低的文档排名越靠前
     * (Rerank candidate documents using perplexity, lower PPL ranks higher)
     *
     * @param question 用户问题 (user question)
     * @param candidates 候选文档列表 (list of candidate documents)
     * @param config 重排序配置 (rerank configuration)
     * @return 重排序后的文档列表 (reranked document list)
     * @throws PPLException PPL计算异常 (PPL计算异常)
     */
    @Override
    public List<Document> rerank(String question, List<Document> candidates, RerankConfig config) throws PPLException {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. 选择前 K 个文档进行重排序 (Select top K documents for reranking)
            int topK = Math.min(config.getTopK(), candidates.size());
            List<Document> toRerank = candidates.subList(0, topK);
            List<Document> remaining = candidates.subList(topK, candidates.size());

            // 2. 计算每个文档的 PPL 分数 (Calculate PPL score for each document)
            List<DocumentWithScore> scoredDocs = new ArrayList<>();

            for (Document doc : toRerank) {
                String content = doc.getContent();

                // 截断内容以控制成本 (Truncate content to control cost)
                if (content.length() > config.getContentTruncateLength()) {
                    content = content.substring(0, config.getContentTruncateLength());
                }

                // 计算 PPL (Calculate PPL)
                double ppl = calculatePerplexity(content);

                // PPL 转换为分数：分数越高越好，PPL 越低越好
                // (Convert PPL to score: higher score is better, lower PPL is better)
                double pplScore = 1.0 / (1.0 + ppl);

                // 获取原始检索分数（从混合检索传递过来）
                // (Get original retrieval score, passed from hybrid search)
                double originalScore = doc.getScore() != null ? doc.getScore() : 1.0;

                // 混合评分：final = (1-weight) * original + weight * ppl_score
                // (Hybrid scoring: final = (1-weight) * original + weight * ppl_score)
                double weight = config.getWeight();
                double finalScore = (1 - weight) * originalScore + weight * pplScore;

                // 记录日志，方便调试 (Log for debugging)
                log.debug(I18N.get("ppl_onnx.log.rerank_detail",
                    doc.getTitle(),
                    String.format("%.3f", originalScore),
                    String.format("%.2f", ppl),
                    String.format("%.3f", pplScore),
                    String.format("%.3f", finalScore)));

                scoredDocs.add(new DocumentWithScore(doc, finalScore));
            }

            // 3. 重新排序 (Re-sort)
            scoredDocs.sort((a, b) -> Double.compare(b.score, a.score));

            // 4. 合并结果 (Merge results)
            List<Document> reranked = scoredDocs.stream()
                    .map(ds -> ds.document)
                    .collect(Collectors.toList());
            reranked.addAll(remaining);

            metrics.recordSuccess(System.currentTimeMillis() - startTime);
            return reranked;

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            throw new PPLException(PPLProviderType.ONNX,
                    I18N.get("ppl_onnx.error.rerank_failed"), e);
        }
    }

    /**
     * 文档和分数的包装类
     * (Wrapper class for document and score)
     */
    private static class DocumentWithScore {
        final Document document;  // 文档 (document)
        final double score;       // 分数 (score)

        DocumentWithScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }

    /**
     * 获取服务提供者类型
     * (Get service provider type)
     */
    @Override
    public PPLProviderType getProviderType() {
        return PPLProviderType.ONNX;
    }

    /**
     * 健康检查
     * (Health check)
     *
     * 验证 ONNX 会话和分词器是否正常工作
     * (Verify ONNX session and tokenizer are working properly)
     *
     * @return 是否健康 (whether healthy)
     */
    @Override
    public boolean isHealthy() {
        try {
            // 检查关键组件是否已初始化 (Check if key components are initialized)
            if (session == null || tokenizer == null) {
                return false;
            }

            // 尝试计算一个简单文本的 PPL (Try to calculate PPL for simple text)
            String testText = "Hello";
            double ppl = calculatePerplexity(testText);

            // PPL 应该是一个合理的正数 (PPL should be a reasonable positive number)
            return ppl > 0 && ppl < 10000;

        } catch (Exception e) {
            log.warn(I18N.get("ppl_onnx.log.health_check_failed"), e);
            return false;
        }
    }

    /**
     * 获取性能指标
     * (Get performance metrics)
     */
    @Override
    public PPLMetrics getMetrics() {
        return metrics;
    }

    /**
     * 销毁服务，释放资源
     * (Destroy service and release resources)
     */
    @PreDestroy
    public void destroy() {
        log.info(I18N.get("ppl_onnx.log.shutdown_start"));

        try {
            // 释放 ONNX Session (Release ONNX Session)
            if (session != null) {
                session.close();
                log.info(I18N.get("ppl_onnx.log.session_closed"));
            }

            // 关闭 Tokenizer (Close Tokenizer)
            if (tokenizer != null) {
                tokenizer.close();
                log.info(I18N.get("ppl_onnx.log.tokenizer_closed"));
            }

            // 清理缓存 (Clear cache)
            if (pplCache != null) {
                pplCache.invalidateAll();
                log.info(I18N.get("ppl_onnx.log.cache_cleared"));
            }

            log.info(I18N.get("ppl_onnx.log.shutdown_success"));

        } catch (Exception e) {
            log.error(I18N.get("ppl_onnx.log.shutdown_error"), e);
        }
    }
}

