package top.yumbo.ai.rag.chunking;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.impl.AiSemanticChunker;

import java.util.List;
import top.yumbo.ai.rag.chunking.impl.SimpleDocumentChunker;
import top.yumbo.ai.rag.chunking.impl.SmartKeywordChunker;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * 文档切分器工厂 (Document chunker factory)
 * 根据策略创建相应的切分器 (Creates appropriate chunker based on strategy)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class DocumentChunkerFactory {

    /**
     * 创建切分器 (Creates chunker)
     *
     * @param strategy 切分策略 (chunking strategy)
     * @param config 切分配置 (chunking config)
     * @param llmClient LLM 客户端（仅 AI_SEMANTIC 策略需要） (LLM client, required only for AI_SEMANTIC strategy)
     * @return 文档切分器 (document chunker)
     */
    public static DocumentChunker createChunker(ChunkingStrategy strategy,
                                                 ChunkingConfig config,
                                                 LLMClient llmClient) {

        log.info(LogMessageProvider.getMessage("log.chunker.creating", strategy, config.getChunkSize(), config.getChunkOverlap()));

        switch (strategy) {
            case NONE:
                return new NoneChunker();

            case SIMPLE:
                return new SimpleDocumentChunker(config);

            case SMART_KEYWORD:
                return new SmartKeywordChunker(config);

            case AI_SEMANTIC:
                if (llmClient == null) {
                    log.warn(LogMessageProvider.getMessage("log.chunker.llm_null"));
                    return new SmartKeywordChunker(config);
                }
                if (!config.getAiChunking().isEnabled()) {
                    log.warn(LogMessageProvider.getMessage("log.chunker.ai_disabled"));
                    return new SmartKeywordChunker(config);
                }
                return new AiSemanticChunker(config, llmClient);

            default:
                log.warn(LogMessageProvider.getMessage("log.chunker.unknown_strategy", strategy));
                return new SmartKeywordChunker(config);
        }
    }

    /**
     * 创建切分器（从字符串解析策略） (Creates chunker from string strategy name)
     */
    @SuppressWarnings("unused")
    public static DocumentChunker createChunker(String strategyName,
                                                 ChunkingConfig config,
                                                 LLMClient llmClient) {
        ChunkingStrategy strategy = ChunkingStrategy.fromString(strategyName);
        return createChunker(strategy, config, llmClient);
    }

    /**
     * 不切分器（NONE 策略） (None chunker for NONE strategy)
     */
    private static class NoneChunker implements DocumentChunker {

        @Override
        public List<DocumentChunk> chunk(String content, String query) {
            if (content == null || content.isEmpty()) {
                return List.of();
            }

            return List.of(DocumentChunk.builder()
                    .content(content)
                    .index(0)
                    .totalChunks(1)
                    .startPosition(0)
                    .endPosition(content.length())
                    .build());
        }

        @Override
        public String getName() {
            return "None Chunker";
        }

        @Override
        public String getDescription() {
            return "不切分，直接使用完整内容 (No chunking: use full content)";
        }
    }
}
