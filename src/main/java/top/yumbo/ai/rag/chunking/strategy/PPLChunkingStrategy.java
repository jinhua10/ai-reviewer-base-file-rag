package top.yumbo.ai.rag.chunking.strategy;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.chunking.DocumentChunk;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.ppl.PPLException;
import top.yumbo.ai.rag.ppl.PPLService;
import top.yumbo.ai.rag.ppl.config.ChunkConfig;

import java.util.List;

/**
 * 基于 PPL（困惑度）的分块策略 (PPL-based Chunking Strategy)
 *
 * 优势 (Advantages):
 * - 本地推理，无需网络调用 (Local inference, no network calls)
 * - 速度快（30-150ms）(Fast: 30-150ms)
 * - 成本低（完全免费）(Low cost: completely free)
 * - 稳定可靠 (Stable and reliable)
 *
 * 适用场景 (Use Cases):
 * - 大批量文档索引 (Bulk document indexing)
 * - 对成本敏感的场景 (Cost-sensitive scenarios)
 * - 离线环境 (Offline environments)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
public class PPLChunkingStrategy implements ChunkingStrategy {

    private final PPLService pplService;

    public PPLChunkingStrategy(PPLService pplService) {
        this.pplService = pplService;

        if (pplService != null && pplService.isHealthy()) {
            log.info(I18N.get("chunking_strategy.ppl.initialized"));
        }
    }

    @Override
    public List<DocumentChunk> chunk(String content, String query, ChunkConfig config) throws PPLException {
        if (!isAvailable()) {
            throw new PPLException(null, I18N.get("chunking_strategy.ppl.unavailable"));
        }

        // 直接委托给 PPLService
        return pplService.chunk(content, query, config);
    }

    @Override
    public String getStrategyName() {
        return "PPL-based Chunking (Local)";
    }

    @Override
    public boolean isAvailable() {
        return pplService != null && pplService.isHealthy();
    }
}

