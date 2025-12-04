package top.yumbo.ai.rag.ppl;

/**
 * PPL 提供商类型枚举
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public enum PPLProviderType {

    /**
     * Hugging Face ONNX - 本地嵌入式推理
     * 优势：速度快、成本低、完全离线
     * 适用场景：PPL Chunking、PPL Rerank
     */
    ONNX("Hugging Face ONNX", "本地嵌入式推理引擎"),

    /**
     * Ollama - 本地 LLM 服务
     * 优势：支持大模型、易于部署、社区活跃
     * 适用场景：本地大模型部署、隐私敏感场景
     */
    OLLAMA("Ollama", "本地 LLM 服务器"),

    /**
     * OpenAI API - 云端 API 服务
     * 优势：能力最强、无需部署、按需付费
     * 适用场景：高质量需求、云端服务
     */
    OPENAI("OpenAI API", "云端 API 服务");

    private final String displayName;
    private final String description;

    PPLProviderType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析提供商类型（忽略大小写）
     */
    public static PPLProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ONNX; // 默认使用 ONNX
        }

        for (PPLProviderType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown PPL provider type: " + value +
            ". Valid values: onnx, ollama, openai");
    }
}

