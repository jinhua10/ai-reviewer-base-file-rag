package top.yumbo.ai.rag.spring.boot.llm;

import java.util.List;

/**
 * LLM客户端接口 / LLM Client Interface
 * 用于集成各种大语言模型 / For integrating various large language models
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
public interface LLMClient {
    /**
     * 生成文本回复 / Generate text response
     *
     * @param prompt 输入的提示词 / Input prompt
     * @return LLM生成的回复 / LLM generated response
     */
    String generate(String prompt);

    /**
     * 生成文本回复（带系统提示） / Generate text response (with system prompt)
     *
     * @param prompt 用户提示 / User prompt
     * @param systemPrompt 系统提示（可选） / System prompt (optional)
     * @return LLM生成的回复 / LLM generated response
     */
    default String generate(String prompt, String systemPrompt) {
        return generate(prompt);
    }

    /**
     * 生成回复（支持单张图片） / Generate response (with single image support)
     * 适用于视觉模型如 gpt-4o, qwen-vl 等 / For vision models like gpt-4o, qwen-vl, etc.
     *
     * @param prompt 用户提示 / User prompt
     * @param imageUrl 图片 URL（base64 或 http(s) URL） / Image URL (base64 or http(s) URL)
     * @param systemPrompt 系统提示（可选） / System prompt (optional)
     * @return 生成的文本 / Generated text
     * @throws UnsupportedOperationException 如果模型不支持图片 / If model doesn't support images
     */
    default String generateWithImage(String prompt, String imageUrl, String systemPrompt) {
        throw new UnsupportedOperationException("此模型不支持图片输入 / This model doesn't support image input");
    }

    /**
     * 生成回复（支持多张图片） / Generate response (with multiple images support)
     *
     * @param prompt 用户提示 / User prompt
     * @param imageUrls 图片 URL 列表 / List of image URLs
     * @param systemPrompt 系统提示（可选） / System prompt (optional)
     * @return 生成的文本 / Generated text
     * @throws UnsupportedOperationException 如果模型不支持图片 / If model doesn't support images
     */
    default String generateWithImages(String prompt, List<String> imageUrls, String systemPrompt) {
        throw new UnsupportedOperationException("此模型不支持多图片输入 / This model doesn't support multiple images input");
    }

    /**
     * 检查是否支持图片输入 / Check if image input is supported
     *
     * @return 是否支持图片 / Whether images are supported
     */
    default boolean supportsImageInput() {
        return false;
    }

    /**
     * 检查客户端是否可用 / Check if client is available
     *
     * @return 是否可用 / Whether available
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取模型名称 / Get model name
     *
     * @return 模型名称 / Model name
     */
    default String getModelName() {
        return "unknown";
    }
}

