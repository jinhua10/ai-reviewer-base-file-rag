package top.yumbo.ai.rag.spring.boot.model;

import lombok.Data;

/**
 * 流式请求 DTO (Streaming Request DTO)
 *
 * @author AI Reviewer Team
 * @since 2025-12-10
 */
@Data
public class StreamingRequest {

    /**
     * 用户问题 (User question)
     */
    private String question;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 语言偏好 (Language preference)
     * 可选：zh/en (Optional: zh/en)
     */
    private String language;

    /**
     * 是否使用知识库 RAG (Whether to use knowledge base RAG)
     * true: RAG模式, false: 直接LLM, null: 默认RAG
     */
    private Boolean useKnowledgeBase;
}

