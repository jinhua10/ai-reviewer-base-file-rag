package top.yumbo.ai.rag.spring.boot.strategy.search;

import top.yumbo.ai.rag.model.Document;

import java.util.List;
import java.util.Map;

/**
 * 检索策略接口（Search Strategy Interface）
 *
 * <p>所有检索策略必须实现此接口，支持可插拔的检索方式</p>
 * <p>All search strategies must implement this interface for pluggable search methods</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
public interface SearchStrategy {

    /**
     * 获取策略唯一标识（Get strategy unique identifier）
     *
     * @return 策略ID（Strategy ID）
     */
    String getId();

    /**
     * 获取策略名称（Get strategy name）
     *
     * @return 策略名称（Strategy name）
     */
    String getName();

    /**
     * 获取策略描述（Get strategy description）
     *
     * @return 策略描述（Strategy description）
     */
    String getDescription();

    /**
     * 评估策略对当前查询的适用性（Evaluate strategy suitability for current query）
     *
     * <p>返回 0-100 的分数，分数越高表示越适合当前查询</p>
     * <p>Returns a score from 0-100, higher score means more suitable for current query</p>
     *
     * @param context 检索上下文（Search context）
     * @return 适用性评分 0-100（Suitability score 0-100）
     */
    int evaluateSuitability(SearchContext context);

    /**
     * 执行检索（Execute search）
     *
     * @param context 检索上下文（Search context）
     * @return 检索到的文档列表（Retrieved document list）
     */
    List<Document> search(SearchContext context);

    /**
     * 获取策略优先级（Get strategy priority）
     * <p>数字越小优先级越高（Lower number means higher priority）</p>
     *
     * @return 优先级（Priority）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否启用（Whether enabled）
     *
     * @return 是否启用（Whether enabled）
     */
    default boolean isEnabled() {
        return true;
    }
}

