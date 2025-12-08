package top.yumbo.ai.rag.spring.boot.llm;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * LLM客户端接口 / LLM Client Interface
 * 用于集成各种大语言模型 / For integrating various large language models
 *
 * <p>支持两种流式输出方式：
 * <ul>
 *   <li>响应式流（Reactive Streams）：使用 {@link Flux} - 推荐用于响应式应用</li>
 *   <li>回调方式（Callback）：使用 Consumer - 兼容非响应式应用</li>
 * </ul>
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

    // ==================== 流式接口（必须由实现类实现真正的流式）====================

    /**
     * 流式生成文本回复（响应式）/ Generate text response with streaming (Reactive)
     *
     * <p><b>⚠️ 此方法必须由实现类提供真正的流式实现，不能使用 generate() 模拟！</b>
     *
     * <p>实现类应该：
     * <ul>
     *   <li>直接调用 LLM 提供商的流式 API（如 OpenAI Stream API、Ollama Stream API）</li>
     *   <li>使用 Flux.create() 或 Flux.generate() 包装流式响应</li>
     *   <li>实时发送每个文本块，而不是等待完整响应</li>
     * </ul>
     *
     * <p>示例实现：
     * <pre>{@code
     * @Override
     * public Flux<String> generateStream(String prompt) {
     *     return Flux.create(sink -> {
     *         // 调用真正的流式 API
     *         openaiAPI.streamChat(prompt, new StreamCallback() {
     *             public void onChunk(String chunk) { sink.next(chunk); }
     *             public void onComplete() { sink.complete(); }
     *             public void onError(Exception e) { sink.error(e); }
     *         });
     *     });
     * }
     * }</pre>
     *
     * <p>使用示例：
     * <pre>{@code
     * llmClient.generateStream("什么是Docker？")
     *     .subscribe(
     *         chunk -> System.out.print(chunk),
     *         error -> System.err.println("错误: " + error),
     *         () -> System.out.println("\n完成")
     *     );
     * }</pre>
     *
     * @param prompt 输入的提示词 / Input prompt
     * @return Flux 流，实时发送文本块 / Flux stream emitting text chunks in real-time
     * @throws UnsupportedOperationException 如果实现类不支持流式 / If implementation doesn't support streaming
     */
    default Flux<String> generateStream(String prompt) {
        throw new UnsupportedOperationException(
            "流式接口未实现！请实现类直接调用 LLM 的流式 API，不要使用 generate() 模拟。\n" +
            "Streaming not implemented! Implementation class should call LLM's native streaming API directly, " +
            "do not simulate with generate()."
        );
    }

    /**
     * 流式生成文本回复（带系统提示，响应式）/ Generate text response with streaming (with system prompt, Reactive)
     *
     * @param prompt 用户提示 / User prompt
     * @param systemPrompt 系统提示 / System prompt
     * @return Flux 流，实时发送文本块 / Flux stream emitting text chunks in real-time
     * @throws UnsupportedOperationException 如果实现类不支持流式 / If implementation doesn't support streaming
     */
    default Flux<String> generateStream(String prompt, String systemPrompt) {
        throw new UnsupportedOperationException(
            "流式接口未实现！请实现类直接调用 LLM 的流式 API。\n" +
            "Streaming not implemented! Implementation class should call LLM's native streaming API."
        );
    }

    // ==================== 回调接口（兼容非响应式应用）====================

    /**
     * 流式生成文本回复（回调方式）/ Generate text response with streaming (Callback style)
     *
     * <p>这是兼容非响应式应用的接口，内部使用 Flux 实现。
     * 如果你的应用支持响应式，推荐直接使用 {@link #generateStream(String)}。
     *
     * @param prompt 输入的提示词 / Input prompt
     * @param onChunk 接收每个文本块的回调 / Callback for each text chunk
     * @param onComplete 完成时的回调 / Callback on completion
     * @param onError 错误时的回调 / Callback on error
     */
    default void generateStreamCallback(String prompt,
                                        java.util.function.Consumer<String> onChunk,
                                        Runnable onComplete,
                                        java.util.function.Consumer<Exception> onError) {
        // 内部调用 Flux 方法
        // (Internally call Flux method)
        generateStream(prompt)
            .subscribe(
                onChunk,
                error -> onError.accept(error instanceof Exception ?
                    (Exception) error : new RuntimeException(error)),
                onComplete
            );
    }

    /**
     * 流式生成文本回复（带系统提示，回调方式）/ Generate text response with streaming (with system prompt, Callback style)
     *
     * @param prompt 用户提示 / User prompt
     * @param systemPrompt 系统提示 / System prompt
     * @param onChunk 接收每个文本块的回调 / Callback for each text chunk
     * @param onComplete 完成时的回调 / Callback on completion
     * @param onError 错误时的回调 / Callback on error
     */
    default void generateStreamCallback(String prompt,
                                        String systemPrompt,
                                        java.util.function.Consumer<String> onChunk,
                                        Runnable onComplete,
                                        java.util.function.Consumer<Exception> onError) {
        // 内部调用 Flux 方法
        // (Internally call Flux method)
        generateStream(prompt, systemPrompt)
            .subscribe(
                onChunk,
                error -> onError.accept(error instanceof Exception ?
                    (Exception) error : new RuntimeException(error)),
                onComplete
            );
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否支持流式输出 / Check if streaming is supported
     *
     * <p>实现类如果支持流式，应该返回 true 并实现 {@link #generateStream(String)} 方法。
     *
     * @return 是否支持流式输出 / Whether streaming is supported
     */
    default boolean supportsStreaming() {
        return false;  // 默认不支持，实现类需要重写
    }
}

