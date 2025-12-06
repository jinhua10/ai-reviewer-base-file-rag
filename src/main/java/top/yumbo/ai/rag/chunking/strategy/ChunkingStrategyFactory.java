package top.yumbo.ai.rag.chunking.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.ppl.PPLService;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;


/**
 * 分块策略工厂 (Chunking strategy factory)
 * 
 * 根据配置创建和选择合适的分块策略 (Creates and selects appropriate chunking strategy based on configuration)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Component
public class ChunkingStrategyFactory {

    private final PPLService pplService;
    private final LLMClient llmClient;

    @Value("${knowledge.qa.chunking.llm-chunking.enabled:false}")
    private boolean llmChunkingEnabled;

    @Value("${knowledge.qa.chunking.llm-chunking.prompt-template:}")
    private String llmPromptTemplate;

    // 策略实例缓存
    private ChunkingStrategy pplStrategy;
    private ChunkingStrategy llmStrategy;

    public ChunkingStrategyFactory(
            @Autowired(required = false) PPLService pplService,
            @Autowired(required = false) LLMClient llmClient) {
        this.pplService = pplService;
        this.llmClient = llmClient;

        log.info(I18N.get("chunking_strategy.factory.initialized"));
        log.info(I18N.get(pplService != null ? "chunking_strategy.factory.ppl_available" : "chunking_strategy.factory.ppl_unavailable"));
        log.info(I18N.get(llmClient != null ? "chunking_strategy.factory.llm_available" : "chunking_strategy.factory.llm_unavailable"));
    }

    /**
     * 获取策略
     *
     * @param strategyType 策略类型：ppl, llm, auto
     * @return 分块策略
     */
    public ChunkingStrategy getStrategy(String strategyType) {
        return switch (strategyType.toLowerCase()) {
            case "ppl" -> getPPLStrategy();
            case "llm" -> getLLMStrategy();
            case "auto" -> getAutoStrategy();
            default -> {
                log.warn(I18N.get("chunking_strategy.factory.unknown_strategy", strategyType));
                yield getDefaultStrategy();
            }
        };
    }

    /**
     * 获取 PPL 策略
     */
    public ChunkingStrategy getPPLStrategy() {
        if (pplStrategy == null) {
            pplStrategy = new PPLChunkingStrategy(pplService);
        }
        return pplStrategy;
    }

    /**
     * 获取 LLM 策略
     */
    public ChunkingStrategy getLLMStrategy() {
        if (llmStrategy == null) {
            llmStrategy = new LLMChunkingStrategy(
                llmClient,
                llmChunkingEnabled && llmClient != null,
                llmPromptTemplate
            );
        }
        return llmStrategy;
    }

    /**
     * 自动选择策略
     *
     * 优先级：
     * 1. LLM（如果可用且配置启用）
     * 2. PPL（如果可用）
     * 3. 简单分块（降级）
     */
    public ChunkingStrategy getAutoStrategy() {
        // 优先 LLM (Priority: LLM)
        ChunkingStrategy llm = getLLMStrategy();
        if (llm.isAvailable()) {
            log.info(I18N.get("chunking_strategy.factory.auto_select_llm"));
            return llm;
        }

        // 其次 PPL (Next: PPL)
        ChunkingStrategy ppl = getPPLStrategy();
        if (ppl.isAvailable()) {
            log.info(I18N.get("chunking_strategy.factory.auto_select_ppl"));
            return ppl;
        }

        // 降级：简单分块 (Fallback: Simple chunking)
        log.warn(I18N.get("chunking_strategy.factory.fallback_simple"));
        return new SimpleChunkingStrategy();
    }

    /**
     * 获取默认策略
     */
    public ChunkingStrategy getDefaultStrategy() {
        // 默认使用 PPL
        ChunkingStrategy ppl = getPPLStrategy();
        if (ppl.isAvailable()) {
            return ppl;
        }

        // 降级
        return new SimpleChunkingStrategy();
    }
}

