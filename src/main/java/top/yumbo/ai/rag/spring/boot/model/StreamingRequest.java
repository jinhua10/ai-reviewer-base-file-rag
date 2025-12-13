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

    /**
     * 知识库模式 (Knowledge base mode)
     * 可选值 (Options):
     * - "none": 不使用RAG，直接LLM (Direct LLM without RAG)
     * - "rag": 使用传统RAG (Traditional RAG)
     * - "role": 使用角色知识库 (Role-based knowledge base)
     * null 或空表示使用传统RAG (null or empty means traditional RAG)
     */
    private String knowledgeMode;

    /**
     * 角色名称 (Role name)
     * 当 knowledgeMode="role" 时使用
     * (Used when knowledgeMode="role")
     * 例如: developer, devops, architect, general 等
     */
    private String roleName;
}



