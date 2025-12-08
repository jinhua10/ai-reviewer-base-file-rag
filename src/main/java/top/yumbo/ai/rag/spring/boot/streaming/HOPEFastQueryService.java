package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.layer.OrdinaryLayerService;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.RecentQA;
import top.yumbo.ai.rag.spring.boot.streaming.model.HOPEAnswer;


/**
 * HOPE 快速查询服务
 * (HOPE Fast Query Service)
 *
 * 目标：<300ms 返回快速答案
 *
 * @author AI Reviewer Team
 * @since 2025-12-08
 */
@Slf4j
@Service
public class HOPEFastQueryService {

    private final HOPEKnowledgeManager hopeManager;

    @Autowired
    public HOPEFastQueryService(@Autowired(required = false) HOPEKnowledgeManager hopeManager) {
        this.hopeManager = hopeManager;
    }

    /**
     * 快速查询（优化后 <300ms）
     * (Fast query, optimized to <300ms)
     *
     * @param question 用户问题
     * @param sessionId 会话ID
     * @return HOPE答案，如果无法快速回答则返回 null
     */
    public HOPEAnswer queryFast(String question, String sessionId) {
        if (hopeManager == null) {
            log.debug("HOPE 管理器未启用 (HOPE manager not enabled)");
            return buildEmptyAnswer();
        }

        long startTime = System.currentTimeMillis();

        try {
            // 优先级1：低频层确定性知识（最快，50-100ms）
            // (Priority 1: Permanent layer factual knowledge, fastest)
            HOPEAnswer permanentAnswer = queryPermanentLayer(question);
            if (permanentAnswer != null && permanentAnswer.isCanDirectAnswer()) {
                log.info("⚡ HOPE 低频层直接回答 (HOPE permanent layer direct answer): {}ms",
                    System.currentTimeMillis() - startTime);
                return permanentAnswer;
            }

            // 优先级2：中频层近期问答（较快，150-250ms）
            // (Priority 2: Ordinary layer recent Q&A, fast)
            HOPEAnswer ordinaryAnswer = queryOrdinaryLayer(question);
            if (ordinaryAnswer != null && ordinaryAnswer.getConfidence() >= 0.8) {
                log.info("⚡ HOPE 中频层相似答案 (HOPE ordinary layer similar answer): {}ms",
                    System.currentTimeMillis() - startTime);
                return ordinaryAnswer;
            }

            // 无法快速回答
            // (Cannot answer quickly)
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("HOPE 无法快速回答 (HOPE cannot answer quickly): {}ms", responseTime);

            return HOPEAnswer.builder()
                .canDirectAnswer(false)
                .source(HOPEAnswer.SourceType.NONE)
                .responseTime(responseTime)
                .build();

        } catch (Exception e) {
            log.warn("HOPE 快速查询失败 (HOPE fast query failed): {}", e.getMessage());
            return buildEmptyAnswer();
        }
    }

    /**
     * 查询低频层
     * (Query permanent layer)
     */
    private HOPEAnswer queryPermanentLayer(String question) {
        try {
            PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
            if (permanentLayer == null) {
                return null;
            }

            long startTime = System.currentTimeMillis();

            // 查找确定性知识
            // (Find factual knowledge)
            FactualKnowledge fact = permanentLayer.findDirectAnswer(question);

            if (fact != null && fact.getConfidence() >= 0.9) {
                return HOPEAnswer.builder()
                    .answer(fact.getAnswer())
                    .confidence(fact.getConfidence())
                    .source(HOPEAnswer.SourceType.HOPE_PERMANENT)
                    .canDirectAnswer(true)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .build();
            }

        } catch (Exception e) {
            log.debug("查询 HOPE 低频层失败 (Query HOPE permanent layer failed): {}", e.getMessage());
        }

        return null;
    }

    /**
     * 查询中频层
     * (Query ordinary layer)
     */
    private HOPEAnswer queryOrdinaryLayer(String question) {
        try {
            OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
            if (ordinaryLayer == null) {
                return null;
            }

            long startTime = System.currentTimeMillis();

            // 查找相似问答（相似度阈值 0.85）
            // (Find similar Q&A with similarity threshold 0.85)
            RecentQA recentQA = ordinaryLayer.findSimilarQA(question, 0.85);

            if (recentQA != null && recentQA.getRating() >= 4.0) {
                return HOPEAnswer.builder()
                    .answer(recentQA.getAnswer())
                    .confidence(recentQA.getRating() / 5.0)  // 转换为 0-1
                    .source(HOPEAnswer.SourceType.HOPE_ORDINARY)
                    .canDirectAnswer(false)  // 相似度不是100%
                    .responseTime(System.currentTimeMillis() - startTime)
                    .similarityScore(recentQA.getSimilarityScore())
                    .build();
            }

        } catch (Exception e) {
            log.debug("查询 HOPE 中频层失败 (Query HOPE ordinary layer failed): {}", e.getMessage());
        }

        return null;
    }

    /**
     * 构建空答案
     * (Build empty answer)
     */
    private HOPEAnswer buildEmptyAnswer() {
        return HOPEAnswer.builder()
            .canDirectAnswer(false)
            .source(HOPEAnswer.SourceType.NONE)
            .responseTime(0)
            .build();
    }
}

