package top.yumbo.ai.rag.role;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.RoleKnowledgeAggregator.AggregatedKnowledge;

import java.util.*;

/**
 * 知识缺口分析器 (Knowledge Gap Analyzer)
 *
 * 功能 (Features):
 * 1. 识别角色知识覆盖不足的领域 (Identify areas with insufficient coverage)
 * 2. 对比不同角色的知识完整度 (Compare knowledge completeness across roles)
 * 3. 生成改进建议 (Generate improvement suggestions)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class KnowledgeGapAnalyzer {

    /**
     * 角色预期知识领域 (Expected knowledge domains for roles)
     */
    private final Map<String, Set<String>> expectedDomains = new HashMap<>();

    /**
     * 最小知识数量阈值 (Minimum knowledge count threshold)
     */
    private int minKnowledgeThreshold = 10;

    // ========== 初始化 (Initialization) ==========

    public KnowledgeGapAnalyzer() {
        initializeExpectedDomains();
        log.info(I18N.get("role.gap.initialized"));
    }

    /**
     * 初始化预期领域 (Initialize expected domains)
     */
    private void initializeExpectedDomains() {
        // 开发者预期领域 (Developer expected domains)
        expectedDomains.put("developer", Set.of(
            "代码规范", "设计模式", "算法", "数据结构",
            "框架使用", "调试技巧", "性能优化", "单元测试"
        ));

        // 架构师预期领域 (Architect expected domains)
        expectedDomains.put("architect", Set.of(
            "系统设计", "架构模式", "技术选型", "性能优化",
            "高可用", "扩展性", "安全设计", "微服务"
        ));

        // 测试预期领域 (Tester expected domains)
        expectedDomains.put("tester", Set.of(
            "测试策略", "自动化测试", "性能测试", "测试工具",
            "缺陷管理", "测试用例", "质量保证", "回归测试"
        ));

        log.debug(I18N.get("role.gap.domains_loaded"), expectedDomains.size());
    }

    // ========== 缺口分析 (Gap Analysis) ==========

    /**
     * 分析角色知识缺口 (Analyze role knowledge gaps)
     *
     * @param roleId 角色ID (Role ID)
     * @param knowledgeList 知识列表 (Knowledge list)
     * @return 缺口分析结果 (Gap analysis result)
     */
    public GapAnalysisResult analyze(String roleId, List<AggregatedKnowledge> knowledgeList) {
        try {
            log.info(I18N.get("role.gap.analyzing"), roleId, knowledgeList.size());

            GapAnalysisResult result = new GapAnalysisResult();
            result.setRoleId(roleId);
            result.setTotalKnowledge(knowledgeList.size());

            // 1. 获取预期领域 (Get expected domains)
            Set<String> expected = expectedDomains.getOrDefault(roleId, new HashSet<>());
            result.setExpectedDomains(new ArrayList<>(expected));

            // 2. 识别已覆盖领域 (Identify covered domains)
            Set<String> covered = identifyCoveredDomains(knowledgeList, expected);
            result.setCoveredDomains(new ArrayList<>(covered));

            // 3. 识别缺口领域 (Identify gap domains)
            Set<String> gaps = new HashSet<>(expected);
            gaps.removeAll(covered);
            result.setGapDomains(new ArrayList<>(gaps));

            // 4. 计算完整度 (Calculate completeness)
            if (!expected.isEmpty()) {
                result.setCompletenessRate((double) covered.size() / expected.size());
            }

            // 5. 评估知识数量 (Evaluate knowledge count)
            if (knowledgeList.size() < minKnowledgeThreshold) {
                result.setNeedsMoreKnowledge(true);
                result.setRecommendedKnowledgeCount(minKnowledgeThreshold - knowledgeList.size());
            }

            log.info(I18N.get("role.gap.analyzed"),
                roleId, covered.size(), expected.size());

            return result;

        } catch (Exception e) {
            log.error(I18N.get("role.gap.analyze_failed"), e.getMessage(), e);
            return new GapAnalysisResult();
        }
    }

    /**
     * 识别已覆盖的领域 (Identify covered domains)
     */
    private Set<String> identifyCoveredDomains(
            List<AggregatedKnowledge> knowledgeList,
            Set<String> expectedDomains) {

        Set<String> covered = new HashSet<>();

        for (String domain : expectedDomains) {
            // 检查是否有知识覆盖该领域 (Check if any knowledge covers this domain)
            boolean hasCoverage = knowledgeList.stream()
                .anyMatch(k -> containsDomain(k, domain));

            if (hasCoverage) {
                covered.add(domain);
            }
        }

        return covered;
    }

    /**
     * 检查知识是否包含特定领域 (Check if knowledge contains domain)
     */
    private boolean containsDomain(AggregatedKnowledge knowledge, String domain) {
        String content = knowledge.getQuestion() + " " + knowledge.getAnswer();
        return content.toLowerCase().contains(domain.toLowerCase());
    }

    // ========== 批量分析 (Batch Analysis) ==========

    /**
     * 批量分析多个角色 (Batch analyze multiple roles)
     */
    public Map<String, GapAnalysisResult> batchAnalyze(
            Map<String, List<AggregatedKnowledge>> aggregatedMap) {

        log.info(I18N.get("role.gap.batch_analyzing"), aggregatedMap.size());

        Map<String, GapAnalysisResult> results = new HashMap<>();

        for (var entry : aggregatedMap.entrySet()) {
            String roleId = entry.getKey();
            List<AggregatedKnowledge> knowledgeList = entry.getValue();

            GapAnalysisResult result = analyze(roleId, knowledgeList);
            results.put(roleId, result);
        }

        return results;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 缺口分析结果 (Gap Analysis Result)
     */
    @Data
    public static class GapAnalysisResult {
        private String roleId;                      // 角色ID
        private int totalKnowledge;                 // 总知识数
        private List<String> expectedDomains;       // 预期领域
        private List<String> coveredDomains;        // 已覆盖领域
        private List<String> gapDomains;            // 缺口领域
        private double completenessRate;            // 完整度
        private boolean needsMoreKnowledge;         // 是否需要更多知识
        private int recommendedKnowledgeCount;      // 建议知识数量
    }
}

