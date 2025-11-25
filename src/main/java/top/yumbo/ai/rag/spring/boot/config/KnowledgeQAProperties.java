package top.yumbo.ai.rag.spring.boot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 知识库问答系统配置
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
        private float similarityThreshold = 0.4f;

        /**
         * 检索返回的文档数量
         */
        private int topK = 20;
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
         *
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
    }
}

