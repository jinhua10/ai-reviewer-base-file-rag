package top.yumbo.ai.rag.spring.boot.strategy.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索策略调度器（Search Strategy Dispatcher）
 *
 * <p>根据查询上下文自动选择最合适的检索策略</p>
 * <p>Automatically selects the most suitable search strategy based on query context</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class SearchStrategyDispatcher {

    private final List<SearchStrategy> strategies;

    // 默认策略ID (Default strategy ID)
    private String defaultStrategyId = "hybrid";

    // 适用性阈值 (Suitability threshold)
    private int suitabilityThreshold = 50;

    @Autowired
    public SearchStrategyDispatcher(@Autowired(required = false) List<SearchStrategy> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        if (!strategies.isEmpty()) {
            // 按优先级排序 (Sort by priority)
            strategies.sort(Comparator.comparingInt(SearchStrategy::getPriority));
            log.info(I18N.get("log.search_dispatcher.init", strategies.size()));
            for (SearchStrategy strategy : strategies) {
                log.info(I18N.get("log.search_dispatcher.strategy_registered",
                    strategy.getId(), strategy.getName(), strategy.getPriority()));
            }
        } else {
            log.info(I18N.get("log.search_dispatcher.no_strategies"));
        }
    }

    /**
     * 执行检索（Execute search）
     *
     * <p>自动选择最合适的策略执行检索</p>
     * <p>Automatically selects the most suitable strategy to execute search</p>
     *
     * @param context 检索上下文（Search context）
     * @return 检索到的文档列表（Retrieved document list）
     */
    public List<Document> search(SearchContext context) {
        // 选择最佳策略 (Select best strategy)
        SearchStrategy bestStrategy = selectBestStrategy(context);

        if (bestStrategy == null) {
            log.warn(I18N.get("log.search_dispatcher.no_suitable_strategy"));
            return Collections.emptyList();
        }

        log.info(I18N.get("log.search_dispatcher.selected_strategy",
            bestStrategy.getId(), bestStrategy.getName()));

        // 执行检索 (Execute search)
        long startTime = System.currentTimeMillis();
        try {
            List<Document> results = bestStrategy.search(context);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info(I18N.get("log.search_dispatcher.search_completed",
                bestStrategy.getId(), results.size(), elapsed));
            return results;
        } catch (Exception e) {
            log.error(I18N.get("log.search_dispatcher.search_failed", bestStrategy.getId()), e);

            // 尝试降级到默认策略 (Try fallback to default strategy)
            if (!bestStrategy.getId().equals(defaultStrategyId)) {
                return fallbackSearch(context);
            }
            return Collections.emptyList();
        }
    }

    /**
     * 使用指定策略执行检索（Execute search with specified strategy）
     *
     * @param strategyId 策略ID（Strategy ID）
     * @param context 检索上下文（Search context）
     * @return 检索到的文档列表（Retrieved document list）
     */
    public List<Document> searchWithStrategy(String strategyId, SearchContext context) {
        SearchStrategy strategy = getStrategy(strategyId);
        if (strategy == null) {
            log.warn(I18N.get("log.search_dispatcher.strategy_not_found", strategyId));
            return search(context); // 降级到自动选择 (Fallback to auto selection)
        }

        log.info(I18N.get("log.search_dispatcher.using_strategy", strategyId));
        return strategy.search(context);
    }

    /**
     * 选择最佳策略（Select best strategy）
     */
    private SearchStrategy selectBestStrategy(SearchContext context) {
        if (strategies.isEmpty()) {
            return null;
        }

        SearchStrategy bestStrategy = null;
        int bestScore = -1;

        // 评估每个启用的策略 (Evaluate each enabled strategy)
        for (SearchStrategy strategy : strategies) {
            if (!strategy.isEnabled()) {
                continue;
            }

            try {
                int score = strategy.evaluateSuitability(context);
                log.debug(I18N.get("log.search_dispatcher.strategy_score",
                    strategy.getId(), score));

                if (score > bestScore && score >= suitabilityThreshold) {
                    bestScore = score;
                    bestStrategy = strategy;
                }
            } catch (Exception e) {
                log.warn(I18N.get("log.search_dispatcher.evaluate_failed", strategy.getId()), e);
            }
        }

        // 如果没有找到合适的策略，使用默认策略 (If no suitable strategy found, use default)
        if (bestStrategy == null) {
            bestStrategy = getStrategy(defaultStrategyId);
            if (bestStrategy == null && !strategies.isEmpty()) {
                bestStrategy = strategies.get(0); // 使用第一个策略作为兜底 (Use first strategy as fallback)
            }
        }

        return bestStrategy;
    }

    /**
     * 降级检索（Fallback search）
     */
    private List<Document> fallbackSearch(SearchContext context) {
        SearchStrategy defaultStrategy = getStrategy(defaultStrategyId);
        if (defaultStrategy != null) {
            log.warn(I18N.get("log.search_dispatcher.fallback", defaultStrategyId));
            try {
                return defaultStrategy.search(context);
            } catch (Exception e) {
                log.error(I18N.get("log.search_dispatcher.fallback_failed"), e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 获取指定策略（Get specified strategy）
     */
    public SearchStrategy getStrategy(String strategyId) {
        return strategies.stream()
            .filter(s -> s.getId().equals(strategyId))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有策略（Get all strategies）
     */
    public List<SearchStrategy> getAllStrategies() {
        return Collections.unmodifiableList(strategies);
    }

    /**
     * 获取所有策略信息（Get all strategy info）
     */
    public List<Map<String, Object>> getStrategiesInfo() {
        return strategies.stream()
            .map(s -> {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", s.getId());
                info.put("name", s.getName());
                info.put("description", s.getDescription());
                info.put("priority", s.getPriority());
                info.put("enabled", s.isEnabled());
                return info;
            })
            .collect(Collectors.toList());
    }

    /**
     * 设置默认策略（Set default strategy）
     */
    public void setDefaultStrategyId(String defaultStrategyId) {
        this.defaultStrategyId = defaultStrategyId;
        log.info(I18N.get("log.search_dispatcher.default_changed", defaultStrategyId));
    }

    /**
     * 设置适用性阈值（Set suitability threshold）
     */
    public void setSuitabilityThreshold(int threshold) {
        this.suitabilityThreshold = threshold;
        log.info(I18N.get("log.search_dispatcher.threshold_changed", threshold));
    }

    /**
     * 动态注册策略（Dynamically register strategy）
     */
    public void registerStrategy(SearchStrategy strategy) {
        strategies.add(strategy);
        strategies.sort(Comparator.comparingInt(SearchStrategy::getPriority));
        log.info(I18N.get("log.search_dispatcher.strategy_added", strategy.getId()));
    }
}

