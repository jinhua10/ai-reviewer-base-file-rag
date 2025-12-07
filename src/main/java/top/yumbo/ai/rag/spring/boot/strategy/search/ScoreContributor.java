package top.yumbo.ai.rag.spring.boot.strategy.search;

import java.util.Map;

/**
 * 评分贡献者接口（Score Contributor Interface）
 *
 * <p>用于可扩展的评分融合，每个贡献者提供部分评分</p>
 * <p>For extensible score fusion, each contributor provides partial scores</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
public interface ScoreContributor {

    /**
     * 获取贡献者名称（Get contributor name）
     *
     * @return 名称（Name）
     */
    String getName();

    /**
     * 获取贡献者描述（Get contributor description）
     *
     * @return 描述（Description）
     */
    String getDescription();

    /**
     * 计算评分贡献（Calculate score contribution）
     *
     * @param context 检索上下文（Search context）
     * @return 文档ID到评分的映射（Map of document ID to score）
     */
    Map<String, Double> contribute(SearchContext context);

    /**
     * 获取权重（Get weight）
     * <p>用于融合时的加权计算（Used for weighted calculation in fusion）</p>
     *
     * @return 权重（Weight）
     */
    double getWeight();

    /**
     * 设置权重（Set weight）
     *
     * @param weight 权重（Weight）
     */
    void setWeight(double weight);

    /**
     * 是否启用（Whether enabled）
     *
     * @return 是否启用（Whether enabled）
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取优先级（Get priority）
     * <p>数字越小优先级越高，先执行（Lower number means higher priority）</p>
     *
     * @return 优先级（Priority）
     */
    default int getPriority() {
        return 100;
    }
}

