package top.yumbo.ai.rag.hope;

import lombok.Getter;

/**
 * 响应策略枚举
 * (Response Strategy Enum)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
public enum ResponseStrategy {

    /**
     * 直接回答 - 不调用 LLM，直接返回低频层的确定性答案
     * (Direct Answer - No LLM call, return deterministic answer from permanent layer)
     *
     * 适用：简单事实型问题 + 高置信度命中
     * (Use case: Simple factual questions + high confidence match)
     *
     * 响应时间：< 100ms
     * (Response time: < 100ms)
     */
    DIRECT_ANSWER("direct_answer", "直接回答", false),

    /**
     * 模板增强回答 - 使用技能模板优化 Prompt 后调用 LLM
     * (Template Answer - Use skill template to optimize prompt then call LLM)
     *
     * 适用：有匹配的技能模板 + 中等复杂度
     * (Use case: Matching skill template + medium complexity)
     *
     * 响应时间：1-2s
     * (Response time: 1-2s)
     */
    TEMPLATE_ANSWER("template_answer", "模板增强", true),

    /**
     * 参考增强回答 - 使用相似问答作为参考后调用 LLM
     * (Reference Answer - Use similar QA as reference then call LLM)
     *
     * 适用：中频层有相似问答参考
     * (Use case: Similar QA reference available in ordinary layer)
     *
     * 响应时间：2-3s
     * (Response time: 2-3s)
     */
    REFERENCE_ANSWER("reference_answer", "参考增强", true),

    /**
     * 完整 RAG 流程 - 执行完整的检索增强生成
     * (Full RAG - Execute full Retrieval-Augmented Generation)
     *
     * 适用：复杂问题、无命中、需要推理
     * (Use case: Complex questions, no match, requires reasoning)
     *
     * 响应时间：2-5s
     * (Response time: 2-5s)
     */
    FULL_RAG("full_rag", "完整RAG", true);

    @Getter
    private final String code;
    @Getter
    private final String name;
    private final boolean requiresLLM;

    ResponseStrategy(String code, String name, boolean requiresLLM) {
        this.code = code;
        this.name = name;
        this.requiresLLM = requiresLLM;
    }

    public boolean requiresLLM() {
        return requiresLLM;
    }
}

