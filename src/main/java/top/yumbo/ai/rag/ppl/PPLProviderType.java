package top.yumbo.ai.rag.ppl;

/**
 * PPL 提供商类型枚举 (PPL Provider Type Enumeration)
 * 
 * 定义了 PPL 服务支持的多种提供商类型
 * (Defines multiple provider types supported by PPL service)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public enum PPLProviderType {

    /**
     * Hugging Face ONNX - 本地嵌入式推理
     * 优势：速度快、成本低、完全离线 (Advantages: Fast speed, low cost, completely offline)
     * 适用场景：PPL Chunking、PPL Rerank (Applicable scenarios: PPL Chunking, PPL Rerank)
     */
    ONNX("Hugging Face ONNX", "本地嵌入式推理引擎"),

    /**
     * Ollama - 本地 LLM 服务
     * 优势：支持大模型、易于部署、社区活跃 (Advantages: Supports large models, easy deployment, active community)
     * 适用场景：本地大模型部署、隐私敏感场景 (Applicable scenarios: Local LLM deployment, privacy-sensitive scenarios)
     */
    OLLAMA("Ollama", "本地 LLM 服务器"),

    /**
     * OpenAI API - 云端 API 服务
     * 优势：能力最强、无需部署、按需付费 (Advantages: Strongest capabilities, no deployment needed, pay-as-you-go)
     * 适用场景：高质量需求、云端服务 (Applicable scenarios: High-quality requirements, cloud services)
     */
    OPENAI("OpenAI API", "云端 API 服务");

    /**
     * 显示名称 (Display name)
     */
    private final String displayName;
    
    /**
     * 描述信息 (Description)
     */
    private final String description;

    /**
     * 构造函数 (Constructor)
     * 
     * @param displayName 显示名称 (Display name)
     * @param description 描述信息 (Description)
     */
    PPLProviderType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取显示名称 (Get display name)
     * 
     * @return 显示名称 (Display name)
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取描述信息 (Get description)
     * 
     * @return 描述信息 (Description)
     */
    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析提供商类型（忽略大小写）(Parse provider type from string, case-insensitive)
     * 
     * @param value 输入字符串 (Input string)
     * @return PPL提供商类型 (PPL provider type)
     */
    public static PPLProviderType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ONNX; // 默认使用 ONNX (Default to ONNX)
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

