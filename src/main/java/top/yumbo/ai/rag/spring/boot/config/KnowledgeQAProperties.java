package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 知识库问答系统配置（Knowledge QA system configuration）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Data
@ConfigurationProperties(prefix = "knowledge.qa")
public class KnowledgeQAProperties {

    /**
     * 知识库配置
     */
    private KnowledgeBaseConfig knowledgeBase = new KnowledgeBaseConfig();

    /**
     * 向量检索配置
     */
    private VectorSearchConfig vectorSearch = new VectorSearchConfig();

    /**
     * LLM配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * 文档处理配置
     */
    private DocumentConfig document = new DocumentConfig();

    /**
     * 图片处理配置
     */
    private ImageProcessingConfig imageProcessing = new ImageProcessingConfig();

    /**
     * 搜索配置（停用词等）
     */
    private SearchConfig search = new SearchConfig();

    @Data
    public static class KnowledgeBaseConfig {
        /**
         * 知识库存储路径
         */
        private String storagePath = "./data/knowledge-base";

        /**
         * 文档源路径（支持文件夹或单个文件）
         */
        private String sourcePath = "./data/documents";

        /**
         * 启动时是否重建知识库
         */
        private boolean rebuildOnStartup = false;

        /**
         * 是否启用缓存
         */
        private boolean enableCache = true;
    }

    @Data
    public static class VectorSearchConfig {
        /**
         * 是否启用向量检索
         */
        private boolean enabled = true;

        /**
         * 模型配置
         */
        private ModelConfig model = new ModelConfig();

        /**
         * 向量索引存储路径
         */
        private String indexPath = "./data/vector-index";

        /**
         * 检索相似度阈值 (0.0-1.0)
         */
        private float similarityThreshold = 0.5f;

        /**
         * Lucene 检索返回的候选文档数量
         * 用于第一步粗筛，通常设置为 hybridTopK 的 2-3 倍
         */
        private int luceneTopK = 40;

        /**
         * 向量检索返回的候选文档数量
         * 用于语义精排，通常设置为 hybridTopK 的 2-3 倍
         */
        private int vectorTopK = 40;

        /**
         * 混合检索最终返回的文档数量（去重后）
         * 这些文档会按评分排序，用于后续的分批引用
         */
        private int hybridTopK = 20;

        /**
         * 每次问答引用的文档数量
         * 用户可以通过 next 接口获取剩余文档
         */
        private int documentsPerQuery = 5;

        /**
         * 检索返回的文档数量上限（已废弃，请使用 hybridTopK）
         *
         * @deprecated 使用 hybridTopK 替代
         */
        @Deprecated
        private int topK = 20;

        /**
         * 文档相关性评分阈值（Lucene + 向量混合评分）
         * 低于此分数的文档会被过滤
         */
        private float minScoreThreshold = 0.10f;
    }

    @Data
    public static class ModelConfig {
        /**
         * 模型名称
         */
        private String name = "paraphrase-multilingual";

        /**
         * 模型路径（相对于 resources）
         */
        private String path = "/models/paraphrase-multilingual/model.onnx";

        /**
         * 支持的模型目录列表（自动查找顺序）
         */
        private List<String> searchPaths = List.of(
                "bge-m3",
                "multilingual-e5-large",
                "bge-large-zh",
                "paraphrase-multilingual",
                "text2vec-base-chinese"
        );

        /**
         * 模型文件名列表（自动查找顺序）
         */
        private List<String> fileNames = List.of(
                "model.onnx",
                "model_O2.onnx",
                "model_quantized.onnx",
                "model_quint8_avx2.onnx"
        );
    }

    @Data
    public static class LlmConfig {
        /**
         * LLM提供商
         * openai: OpenAI 兼容 API（默认，支持 OpenAI、DeepSeek 等所有兼容服务）
         * mock: Mock 模式（测试用）
         * <p>
         * 说明：通过配置不同的 apiUrl 和 model 即可切换不同的服务
         */
        private String provider = "openai";

        /**
         * API Key（从环境变量读取）
         */
        private String apiKey = "${AI_API_KEY:}";

        /**
         * API 端点
         */
        private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

        /**
         * 模型名称
         */
        private String model = "deepseek-chat";

        /**
         * 最大上下文长度
         */
        private int maxContextLength = 20000;

        /**
         * 单文档最大长度
         */
        private int maxDocLength = 5000;

        /**
         * 单次问答最大处理文档数
         * 用于防止内存溢出和控制响应时间
         */
        private int maxDocumentsPerQuery = 10;

        /**
         * 是否启用分批处理模式
         */
        private boolean enableBatchProcessing = true;

        /**
         * 文档切分策略
         */
        private String chunkingStrategy = "SMART_KEYWORD";

        /**
         * 文档切分配置
         */
        private top.yumbo.ai.rag.chunking.ChunkingConfig chunking = new top.yumbo.ai.rag.chunking.ChunkingConfig();

        /**
         * Prompt 模板
         * 支持两个占位符：
         * - {question}: 用户问题
         * - {context}: 相关文档内容
         */
        private String promptTemplate = """
                你是一个专业的知识助手。请基于文档内容回答用户问题。
                
                # 回答要求
                1. 必须基于文档内容回答，不要编造信息
                2. 如果文档中没有相关信息，明确告知用户
                3. 回答要清晰、准确、有条理
                4. 可以引用文档名称作为信息来源
                5. 保持专业友好的语气
                
                # 用户问题
                {question}
                
                # 相关文档
                {context}
                
                # 请提供你的回答：
                """;
    }

    @Data
    public static class DocumentConfig {
        /**
         * 支持的文件格式
         */
        private List<String> supportedFormats = List.of(
                "xlsx", "xls",      // Excel
                "docx", "doc",      // Word
                "pptx", "ppt",      // PowerPoint
                "pdf",              // PDF
                "txt", "md",        // 文本
                "html", "xml"       // 其他
        );

        /**
         * 最大文件大小（MB）
         */
        private int maxFileSizeMb = 200;

        /**
         * 最大内容大小（MB）
         */
        private int maxContentSizeMb = 50;

        /**
         * 自动分块阈值（MB）
         */
        private int autoChunkThresholdMb = 2;

        /**
         * 文档分块大小（字符数）
         */
        private int chunkSize = 2000;

        /**
         * 文档分块重叠（字符数）
         */
        private int chunkOverlap = 400;

        /**
         * 是否启用并行处理
         */
        private boolean parallelProcessing = true;

        /**
         * 并行处理线程数（0=自动，使用 CPU 核心数）
         */
        private int parallelThreads = 0;

        /**
         * 批处理大小（文档数）
         */
        private int batchSize = 10;

        /**
         * 索引时单个文档最大内容长度（字符数）
         * 超过此长度会被截断，防止后续处理内存溢出
         * <p>
         * 建议值：
         * - 内存充足（8GB+）: 100000 (约 200KB)
         * - 内存一般（4GB）: 50000 (约 100KB)
         * - 内存有限（2GB）: 30000 (约 60KB)
         * <p>
         * 影响：
         * - 过大：可能导致内存溢出、索引慢
         * - 过小：可能丢失文档尾部内容
         */
        private int maxIndexContentLength = 50000;

        /**
         * 问答时文档切分最大内容长度（字符数）
         * 在切分文档时，如果内容超过此长度会被截断
         * <p>
         * 建议值：
         * - 通常设置为 maxIndexContentLength 的 2 倍
         * - 因为问答时只处理少量文档（5-10个）
         */
        private int maxChunkContentLength = 100000;

        /**
         * 问答时单次切分最大块数
         * 防止切分产生过多块导致内存溢出
         * <p>
         * 建议值：30-100
         * <p>
         * 影响：
         * - 过大：可能内存溢出
         * - 过小：可能丢失内容
         */
        private int maxChunksPerDocument = 50;
    }

    /**
     * 图片处理配置
     */
    @Data
    public static class ImageProcessingConfig {
        /**
         * 图片处理策略: placeholder, ocr, vision-llm, hybrid
         */
        private String strategy = "placeholder";

        /**
         * 是否启用 OCR
         */
        private boolean enableOcr = false;

        /**
         * OCR 配置
         */
        private OcrConfig ocr = new OcrConfig();

        /**
         * Vision LLM 配置
         */
        private VisionLlmConfig visionLlm = new VisionLlmConfig();
    }

    /**
     * OCR 配置
     */
    @Data
    public static class OcrConfig {
        /**
         * Tesseract 数据路径
         */
        private String tessdataPath = "${TESSDATA_PREFIX:}";

        /**
         * 识别语言 (chi_sim=简体中文, eng=英文)
         */
        private String language = "chi_sim+eng";
    }

    /**
     * Vision LLM 配置
     */
    @Data
    public static class VisionLlmConfig {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * API Key
         */
        private String apiKey = "${VISION_LLM_API_KEY:}";

        /**
         * 模型名称
         */
        private String model = "gpt-4-vision-preview";

        /**
         * API 端点
         */
        private String endpoint = "${VISION_LLM_ENDPOINT:}";

        /**
         * 批量处理配置
         */
        private BatchConfig batch = new BatchConfig();
    }

    /**
     * Vision LLM 批量处理配置
     */
    @Data
    public static class BatchConfig {
        /**
         * 是否启用批量处理
         */
        private boolean enabled = true;

        /**
         * 批量大小（一次处理多少张幻灯片）
         */
        private int size = 4;

        /**
         * 图片压缩配置
         */
        private CompressConfig compress = new CompressConfig();
    }

    /**
     * 图片压缩配置
     */
    @Data
    public static class CompressConfig {
        /**
         * 是否启用压缩
         */
        private boolean enabled = true;

        /**
         * 压缩质量 (0.1-1.0)
         */
        private double quality = 0.8;

        /**
         * 单张图片最大尺寸（KB）
         */
        private int maxSizeKb = 300;

        /**
         * 图片最大宽度
         */
        private int maxWidth = 1920;

        /**
         * 图片最大高度
         */
        private int maxHeight = 1080;
    }

    /**
     * 搜索配置（停用词等）
     */
    @Data
    public static class SearchConfig {
        /**
         * 中文停用词列表
         * 在提取关键词时会过滤这些词
         */
        private List<String> chineseStopWords = List.of(
            "的", "是", "在", "了", "和", "有", "我", "你", "他", "她", "它",
            "们", "这", "那", "之", "与", "及", "或", "等", "也", "就", "都",
            "而", "及", "着", "把", "被", "让", "给", "向", "往", "从", "自",
            "什么", "怎么", "如何", "为什么", "哪", "哪些", "哪个", "哪里",
            "谁", "多少", "几", "吗", "呢", "啊", "吧", "呀", "哦", "嗯",
            "一个", "一些", "这个", "那个", "这些", "那些", "某", "某些",
            "可以", "可能", "应该", "需要", "必须", "能够", "能", "会",
            "请", "请问", "想", "要", "想要", "希望", "可否", "是否",
            "非常", "很", "太", "最", "更", "比较", "相当", "十分"
        );

        /**
         * 英文停用词列表
         * 在提取关键词时会过滤这些词
         */
        private List<String> englishStopWords = List.of(
            // 冠词
            "a", "an", "the",
            // be 动词
            "is", "are", "was", "were", "be", "been", "being", "am",
            // 代词
            "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their", "mine", "yours", "hers", "ours", "theirs",
            "this", "that", "these", "those", "who", "whom", "which", "what", "whose",
            // 介词
            "in", "on", "at", "to", "for", "of", "with", "by", "from", "up", "down",
            "into", "onto", "out", "off", "over", "under", "about", "between", "through",
            // 连词
            "and", "or", "but", "if", "then", "else", "when", "while", "because", "although",
            "however", "therefore", "so", "yet", "nor", "either", "neither",
            // 助动词
            "do", "does", "did", "have", "has", "had", "will", "would", "shall", "should",
            "can", "could", "may", "might", "must",
            // 疑问词
            "how", "why", "where", "when", "what", "which", "who",
            // 其他常见词
            "not", "no", "yes", "all", "any", "some", "each", "every", "both", "few", "more",
            "most", "other", "such", "only", "same", "than", "too", "very", "just", "also"
        );

        /**
         * 最小关键词长度
         * 小于此长度的词会被过滤（对英文生效）
         */
        private int minKeywordLength = 2;

        /**
         * 是否启用停用词过滤
         */
        private boolean enableStopWordsFilter = true;
    }
}
