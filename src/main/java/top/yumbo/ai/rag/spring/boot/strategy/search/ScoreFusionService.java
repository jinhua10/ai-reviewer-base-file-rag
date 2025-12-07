package top.yumbo.ai.rag.spring.boot.strategy.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评分融合服务（Score Fusion Service）
 *
 * <p>融合多个评分贡献者的评分，生成最终的文档排序</p>
 * <p>Fuses scores from multiple contributors to generate final document ranking</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class ScoreFusionService {

    private final List<ScoreContributor> contributors;

    @Autowired
    public ScoreFusionService(@Autowired(required = false) List<ScoreContributor> contributors) {
        this.contributors = contributors != null ? contributors : new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        if (!contributors.isEmpty()) {
            // 按优先级排序 (Sort by priority)
            contributors.sort(Comparator.comparingInt(ScoreContributor::getPriority));
            log.info(I18N.get("log.score_fusion.init", contributors.size()));
            for (ScoreContributor contributor : contributors) {
                log.info(I18N.get("log.score_fusion.contributor_registered",
                    contributor.getName(), contributor.getWeight(), contributor.getPriority()));
            }
        } else {
            log.info(I18N.get("log.score_fusion.no_contributors"));
        }
    }

    /**
     * 融合评分（Fuse scores）
     *
     * @param context 检索上下文（Search context）
     * @return 文档ID到最终评分的映射，按评分降序排序
     *         (Map of document ID to final score, sorted by score descending)
     */
    public Map<String, Double> fuse(SearchContext context) {
        Map<String, Double> finalScores = new HashMap<>();

        // 获取启用的贡献者 (Get enabled contributors)
        List<ScoreContributor> enabledContributors = contributors.stream()
            .filter(ScoreContributor::isEnabled)
            .collect(Collectors.toList());

        if (enabledContributors.isEmpty()) {
            log.warn(I18N.get("log.score_fusion.no_enabled_contributors"));
            return finalScores;
        }

        // 计算总权重 (Calculate total weight)
        double totalWeight = enabledContributors.stream()
            .mapToDouble(ScoreContributor::getWeight)
            .sum();

        if (totalWeight <= 0) {
            log.warn(I18N.get("log.score_fusion.zero_weight"));
            return finalScores;
        }

        // 收集各贡献者的评分 (Collect scores from each contributor)
        for (ScoreContributor contributor : enabledContributors) {
            try {
                long startTime = System.currentTimeMillis();
                Map<String, Double> scores = contributor.contribute(context);
                long elapsed = System.currentTimeMillis() - startTime;

                // 归一化权重 (Normalize weight)
                double normalizedWeight = contributor.getWeight() / totalWeight;

                // 加权融合 (Weighted fusion)
                for (Map.Entry<String, Double> entry : scores.entrySet()) {
                    String docId = entry.getKey();
                    double score = entry.getValue() * normalizedWeight;
                    finalScores.merge(docId, score, Double::sum);
                }

                log.debug(I18N.get("log.score_fusion.contributor_done",
                    contributor.getName(), scores.size(), elapsed));

            } catch (Exception e) {
                log.error(I18N.get("log.score_fusion.contributor_failed", contributor.getName()), e);
            }
        }

        // 按评分降序排序 (Sort by score descending)
        return finalScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * 获取所有贡献者（Get all contributors）
     */
    public List<ScoreContributor> getContributors() {
        return Collections.unmodifiableList(contributors);
    }

    /**
     * 获取启用的贡献者数量（Get enabled contributor count）
     */
    public int getEnabledContributorCount() {
        return (int) contributors.stream().filter(ScoreContributor::isEnabled).count();
    }

    /**
     * 动态添加贡献者（Dynamically add contributor）
     */
    public void addContributor(ScoreContributor contributor) {
        contributors.add(contributor);
        contributors.sort(Comparator.comparingInt(ScoreContributor::getPriority));
        log.info(I18N.get("log.score_fusion.contributor_added", contributor.getName()));
    }

    /**
     * 动态移除贡献者（Dynamically remove contributor）
     */
    public boolean removeContributor(String name) {
        boolean removed = contributors.removeIf(c -> c.getName().equals(name));
        if (removed) {
            log.info(I18N.get("log.score_fusion.contributor_removed", name));
        }
        return removed;
    }
}

