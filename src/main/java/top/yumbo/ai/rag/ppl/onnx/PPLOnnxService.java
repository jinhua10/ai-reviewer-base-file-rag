package top.yumbo.ai.rag.ppl.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.yumbo.ai.rag.chunking.DocumentChunk;
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
 *
 * 特点：
 * - 本地嵌入式推理，无网络开销
 * - 速度快（30-150ms）
 * - 成本低（完全免费）
 * - 支持 GPU 加速
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
        log.info("🚀 Initializing ONNX PPL Service...");

        try {
            PPLConfig.OnnxConfig onnxConfig = config.getOnnx();

            log.info("📦 Model path: {}", onnxConfig.getModelPath());
            log.info("📦 Tokenizer path: {}", onnxConfig.getTokenizerPath());

            // 1. 初始化 ONNX Runtime 环境
            this.env = OrtEnvironment.getEnvironment();
            log.info("✅ ONNX Runtime environment created");

            // 2. 加载 ONNX 模型
            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

            this.session = env.createSession(onnxConfig.getModelPath(), sessionOptions);
            log.info("✅ ONNX model loaded from: {}", onnxConfig.getModelPath());

            // 3. 加载 Tokenizer
            this.tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(onnxConfig.getTokenizerPath()));
            log.info("✅ Tokenizer loaded from: {}", onnxConfig.getTokenizerPath());

            // 4. 初始化缓存
            if (onnxConfig.isUseCache()) {
                this.pplCache = Caffeine.newBuilder()
                        .maximumSize(onnxConfig.getCacheSize())
                        .expireAfterWrite(Duration.ofSeconds(onnxConfig.getCacheTtl()))
                        .recordStats()
                        .build();
                log.info("✅ PPL cache initialized (size: {}, TTL: {}s)",
                        onnxConfig.getCacheSize(), onnxConfig.getCacheTtl());
            }

            log.info("✅ ONNX PPL Service initialized successfully");

        } catch (Exception e) {
            log.error("❌ Failed to initialize ONNX PPL Service", e);
            throw new RuntimeException("ONNX initialization failed", e);
        }
    }

    @Override
    public double calculatePerplexity(String text) throws PPLException {
        if (text == null || text.trim().isEmpty()) {
            return Double.MAX_VALUE;
        }

        // 检查缓存
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
            // 1. Tokenize - 将文本转换为 Token IDs
            Encoding encoding = tokenizer.encode(text);
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();

            if (inputIds.length == 0) {
                return Double.MAX_VALUE;
            }

            // 2. 准备 ONNX 输入
            Map<String, OnnxTensor> inputs = new HashMap<>();

            // 将 inputIds 转换为 [1, seq_len] 的张量
            long[][] inputIdsArray = new long[1][inputIds.length];
            inputIdsArray[0] = inputIds;

            long[][] attentionMaskArray = new long[1][attentionMask.length];
            attentionMaskArray[0] = attentionMask;

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsArray);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskArray);

            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            // 3. 模型推理
            try (OrtSession.Result results = session.run(inputs)) {
                // 获取 logits（模型输出）
                OnnxValue logitsValue = results.get(0);
                float[][][] logits = (float[][][]) logitsValue.getValue();

                // 4. 计算困惑度
                double totalLoss = 0.0;
                int validTokens = 0;

                // 对每个位置计算 cross-entropy loss
                for (int i = 0; i < inputIds.length - 1; i++) {
                    int targetId = (int) inputIds[i + 1];
                    float[] probs = logits[0][i];

                    // Softmax 归一化
                    float maxLogit = Float.NEGATIVE_INFINITY;
                    for (float logit : probs) {
                        maxLogit = Math.max(maxLogit, logit);
                    }

                    double sumExp = 0.0;
                    for (float logit : probs) {
                        sumExp += Math.exp(logit - maxLogit);
                    }

                    double logProb = probs[targetId] - maxLogit - Math.log(sumExp);
                    totalLoss -= logProb;  // 等价于 += -logProb
                    validTokens++;
                }

                // PPL = exp(average loss)
                double ppl = validTokens > 0 ? Math.exp(totalLoss / validTokens) : Double.MAX_VALUE;

                // 清理资源
                inputIdsTensor.close();
                attentionMaskTensor.close();

                // 缓存结果
                if (pplCache != null) {
                    pplCache.put(text, ppl);
                }

                metrics.recordSuccess(System.currentTimeMillis() - startTime);
                return ppl;
            }

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            log.error("Failed to calculate perplexity for text: {}", text.substring(0, Math.min(50, text.length())), e);
            throw new PPLException(PPLProviderType.ONNX, "Failed to calculate perplexity", e);
        }
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();

        try {
            List<DocumentChunk> chunks = new ArrayList<>();

            // 1. 分句 - 按标点符号分割
            List<String> sentences = splitIntoSentences(content);

            if (sentences.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 如果启用粗分块，先按段落粗分
            List<List<String>> coarseChunks = new ArrayList<>();
            if (config.isEnableCoarseChunking()) {
                coarseChunks = coarseChunk(sentences, config.getMaxChunkSize());
            } else {
                coarseChunks.add(sentences);
            }

            // 3. 对每个粗块进行 PPL 精细切分
            int chunkIndex = 0;
            for (List<String> coarseChunk : coarseChunks) {
                List<DocumentChunk> fineChunks = pplBasedChunk(coarseChunk, config);

                // 设置索引
                for (DocumentChunk chunk : fineChunks) {
                    chunk.setIndex(chunkIndex++);
                }

                chunks.addAll(fineChunks);
            }

            metrics.recordSuccess(System.currentTimeMillis() - startTime);
            return chunks;

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            throw new PPLException(PPLProviderType.ONNX, "Failed to chunk document", e);
        }
    }

    /**
     * 分句 - 按标点符号分割
     */
    private List<String> splitIntoSentences(String content) {
        List<String> sentences = new ArrayList<>();

        // 按中英文句号、问号、感叹号分割
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
     *
     * 改进点：
     * 1. 在语义边界切分，而不是固定字数
     * 2. 支持上下文重叠，避免信息丢失
     * 3. 软限制 + 硬限制，保证块大小在合理范围
     */
    private List<List<String>> coarseChunk(List<String> sentences, int maxChunkSize) {
        return semanticCoarseChunk(sentences, maxChunkSize, true, 2);
    }

    /**
     * 语义感知的粗分块
     *
     * @param sentences 句子列表
     * @param maxChunkSize 最大块大小
     * @param semanticAware 是否启用语义感知
     * @param overlapSentences 重叠句子数
     */
    private List<List<String>> semanticCoarseChunk(List<String> sentences,
            int maxChunkSize, boolean semanticAware, int overlapSentences) {

        List<List<String>> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        List<String> overlapBuffer = new ArrayList<>();  // 重叠缓冲区
        int currentSize = 0;

        // 目标大小为最大大小的 60%，软限制
        int targetSize = (int) (maxChunkSize * 0.6);
        // 硬性上限为最大大小的 125%
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
     * 检测语义边界
     *
     * @param current 当前句子
     * @param previous 前一句子
     * @return 是否是语义边界
     */
    private boolean isSemanticBoundary(String current, String previous) {
        if (current == null || current.isEmpty()) {
            return false;
        }

        String trimmed = current.trim();

        // 1. 章节标题（中文）
        if (trimmed.matches("^第[一二三四五六七八九十百千零\\d]+[章节篇部条款项].*")) {
            return true;
        }

        // 2. Markdown 标题
        if (trimmed.matches("^#{1,6}\\s+.*")) {
            return true;
        }

        // 3. 数字编号标题（如 "1. xxx", "1.1 xxx"）
        if (trimmed.matches("^\\d+(\\.\\d+)*[.、\\s].*") && trimmed.length() < 100) {
            return true;
        }

        // 4. 段落开头词（表示新主题）
        if (trimmed.matches("^(首先|其次|再次|然后|接着|最后|另外|此外|" +
                "综上|总之|因此|所以|总结|结论|概述|简介|背景|目的|" +
                "一方面|另一方面|与此同时|需要注意|值得一提|特别是).*")) {
            return true;
        }

        // 5. 前一句是列表项结尾，当前不是列表项（列表结束）
        if (previous != null) {
            boolean prevIsList = previous.trim().matches("^[\\d一二三四五六七八九十]+[.、）)].*")
                    || previous.trim().matches("^[-*•]\\s.*");
            boolean currIsList = trimmed.matches("^[\\d一二三四五六七八九十]+[.、）)].*")
                    || trimmed.matches("^[-*•]\\s.*");
            if (prevIsList && !currIsList) {
                return true;
            }
        }

        // 6. 段落分隔标记（如果在分句时保留了）
        if (trimmed.startsWith("[PARA]") || trimmed.startsWith("---") || trimmed.startsWith("***")) {
            return true;
        }

        return false;
    }

    /**
     * 基于 PPL 的精细切分
     */
    private List<DocumentChunk> pplBasedChunk(List<String> sentences, ChunkConfig config) throws PPLException {
        List<DocumentChunk> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        // 计算每个句子的 PPL
        List<Double> pplScores = new ArrayList<>();
        for (String sentence : sentences) {
            double ppl = calculatePerplexity(sentence);
            pplScores.add(ppl);
        }

        // 找到 PPL 突变点
        List<Integer> splitPoints = new ArrayList<>();
        splitPoints.add(0); // 起始点

        for (int i = 1; i < pplScores.size(); i++) {
            double currentPPL = pplScores.get(i);
            double prevPPL = pplScores.get(i - 1);

            // PPL 变化超过阈值，且当前块不为空
            if (Math.abs(currentPPL - prevPPL) > config.getPplThreshold()) {
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

    @Override
    public List<Document> rerank(String question, List<Document> candidates, RerankConfig config) throws PPLException {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. 选择前 K 个文档进行重排序
            int topK = Math.min(config.getTopK(), candidates.size());
            List<Document> toRerank = candidates.subList(0, topK);
            List<Document> remaining = candidates.subList(topK, candidates.size());

            // 2. 计算每个文档的 PPL 分数
            List<DocumentWithScore> scoredDocs = new ArrayList<>();

            for (Document doc : toRerank) {
                String content = doc.getContent();

                // 截断内容以控制成本
                if (content.length() > config.getContentTruncateLength()) {
                    content = content.substring(0, config.getContentTruncateLength());
                }

                // 计算 PPL
                double ppl = calculatePerplexity(content);

                // PPL 转换为分数：分数越高越好，PPL 越低越好
                double pplScore = 1.0 / (1.0 + ppl);

                // 获取原始分数（如果有的话）
                double originalScore = 1.0; // 默认分数

                // 混合评分：final = (1-weight) * original + weight * ppl_score
                double weight = config.getWeight();
                double finalScore = (1 - weight) * originalScore + weight * pplScore;

                scoredDocs.add(new DocumentWithScore(doc, finalScore));
            }

            // 3. 重新排序
            scoredDocs.sort((a, b) -> Double.compare(b.score, a.score));

            // 4. 合并结果
            List<Document> reranked = scoredDocs.stream()
                    .map(ds -> ds.document)
                    .collect(Collectors.toList());
            reranked.addAll(remaining);

            metrics.recordSuccess(System.currentTimeMillis() - startTime);
            return reranked;

        } catch (Exception e) {
            metrics.recordFailure(System.currentTimeMillis() - startTime);
            throw new PPLException(PPLProviderType.ONNX, "Failed to rerank documents", e);
        }
    }

    /**
     * 文档和分数的包装类
     */
    private static class DocumentWithScore {
        final Document document;
        final double score;

        DocumentWithScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }

    @Override
    public PPLProviderType getProviderType() {
        return PPLProviderType.ONNX;
    }

    @Override
    public boolean isHealthy() {
        try {
            // 检查关键组件是否已初始化
            if (session == null || tokenizer == null) {
                return false;
            }

            // 尝试计算一个简单文本的 PPL
            String testText = "Hello";
            double ppl = calculatePerplexity(testText);

            // PPL 应该是一个合理的正数
            return ppl > 0 && ppl < 10000;

        } catch (Exception e) {
            log.warn("Health check failed", e);
            return false;
        }
    }

    @Override
    public PPLMetrics getMetrics() {
        return metrics;
    }

    @PreDestroy
    public void destroy() {
        log.info("🛑 Shutting down ONNX PPL Service...");

        try {
            // 释放 ONNX Session
            if (session != null) {
                session.close();
                log.info("✅ ONNX session closed");
            }

            // 关闭 Tokenizer
            if (tokenizer != null) {
                tokenizer.close();
                log.info("✅ Tokenizer closed");
            }

            // 清理缓存
            if (pplCache != null) {
                pplCache.invalidateAll();
                log.info("✅ PPL cache cleared");
            }

            log.info("✅ ONNX PPL Service shut down successfully");

        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }
}

