package top.yumbo.ai.rag.retriever;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.ScoredDocument;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 结果融合器 (Result Fusion)
 *
 * 将多个角色的检索结果融合为统一的排序列表
 * (Fuses retrieval results from multiple roles into a unified sorted list)
 *
 * 融合策略 (Fusion Strategy):
 * 1. 相同文档的分数加权累加 (Weighted sum of scores for same document)
 * 2. 考虑角色权重 (Consider role weights)
 * 3. 考虑文档在结果中的位置 (Consider document position in results)
 * 4. 多角色共识加成 (Bonus for multi-role consensus)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class ResultFusion {

    /**
     * 位置衰减因子 (Position decay factor)
     */
    private static final double POSITION_DECAY = 0.9;

    /**
     * 多角色加成因子 (Multi-role bonus factor)
     */
    private static final double MULTI_ROLE_BONUS = 1.2;

    /**
     * 融合多个角色的搜索结果 (Fuse search results from multiple roles)
     *
     * @param results 角色搜索结果列表 (List of role search results)
     * @param topK 返回前K个结果 (Return top K results)
     * @return 融合后的文档列表 (Fused document list)
     */
    public List<Document> fuseResults(List<RoleSearchResult> results, int topK) {
        if (results == null || results.isEmpty()) {
            log.warn(I18N.get("retriever.fusion.empty_input"));
            return Collections.emptyList();
        }

        log.info(I18N.get("retriever.fusion.start", results.size(), topK));
        long startTime = System.currentTimeMillis();

        // 1. 收集所有文档并计算融合分数 (Collect all documents and calculate fused scores)
        Map<String, FusedDocument> fusedMap = new HashMap<>();

        for (RoleSearchResult result : results) {
            if (!result.hasResults()) {
                continue;
            }

            double roleWeight = result.getRoleWeight();
            List<ScoredDocument> docs = result.getDocuments();

            for (int i = 0; i < docs.size(); i++) {
                ScoredDocument scoredDoc = docs.get(i);
                Document doc = scoredDoc.getDocument();

                if (doc == null || doc.getId() == null) {
                    continue;
                }

                // 计算综合分数 (Calculate comprehensive score)
                double positionWeight = Math.pow(POSITION_DECAY, i);
                double finalScore = roleWeight * scoredDoc.getScore() * positionWeight;

                String docId = doc.getId();
                FusedDocument existing = fusedMap.get(docId);

                if (existing == null) {
                    fusedMap.put(docId, new FusedDocument(doc, finalScore, result.getRoleId()));
                } else {
                    // 同一文档在多个角色中出现，累加分数 (Same document appears in multiple roles, add scores)
                    existing.addRoleScore(finalScore, result.getRoleId());
                }
            }
        }

        // 2. 应用多角色加成 (Apply multi-role bonus)
        for (FusedDocument fusedDoc : fusedMap.values()) {
            if (fusedDoc.isFromMultipleRoles()) {
                double bonus = (fusedDoc.getSourceRoleCount() - 1) * MULTI_ROLE_BONUS;
                fusedDoc.setTotalScore(fusedDoc.getTotalScore() * (1.0 + bonus));
                log.debug(I18N.get("retriever.fusion.multi_role_bonus",
                        fusedDoc.getDocument().getId(),
                        fusedDoc.getSourceRoleCount(),
                        bonus));
            }
        }

        // 3. 排序并返回Top K (Sort and return top K)
        List<Document> finalResults = fusedMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .limit(topK)
                .map(fusedDoc -> {
                    Document doc = fusedDoc.getDocument();
                    doc.setScore(fusedDoc.getTotalScore());
                    return doc;
                })
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info(I18N.get("retriever.fusion.complete",
                finalResults.size(), fusedMap.size(), duration));

        return finalResults;
    }

    /**
     * 融合并生成详细结果 (Fuse and generate detailed results)
     *
     * @param results 角色搜索结果列表 (List of role search results)
     * @param topK 返回前K个结果 (Return top K results)
     * @return 融合文档列表（包含详细信息） (Fused document list with details)
     */
    public List<FusedDocument> fuseWithDetails(List<RoleSearchResult> results, int topK) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, FusedDocument> fusedMap = new HashMap<>();

        for (RoleSearchResult result : results) {
            if (!result.hasResults()) {
                continue;
            }

            double roleWeight = result.getRoleWeight();
            List<ScoredDocument> docs = result.getDocuments();

            for (int i = 0; i < docs.size(); i++) {
                ScoredDocument scoredDoc = docs.get(i);
                Document doc = scoredDoc.getDocument();

                if (doc == null || doc.getId() == null) {
                    continue;
                }

                double positionWeight = Math.pow(POSITION_DECAY, i);
                double finalScore = roleWeight * scoredDoc.getScore() * positionWeight;

                String docId = doc.getId();
                FusedDocument existing = fusedMap.get(docId);

                if (existing == null) {
                    fusedMap.put(docId, new FusedDocument(doc, finalScore, result.getRoleId()));
                } else {
                    existing.addRoleScore(finalScore, result.getRoleId());
                }
            }
        }

        // 应用多角色加成并排序 (Apply multi-role bonus and sort)
        return fusedMap.values().stream()
                .peek(fusedDoc -> {
                    if (fusedDoc.isFromMultipleRoles()) {
                        double bonus = (fusedDoc.getSourceRoleCount() - 1) * MULTI_ROLE_BONUS;
                        fusedDoc.setTotalScore(fusedDoc.getTotalScore() * (1.0 + bonus));
                    }
                })
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算融合统计信息 (Calculate fusion statistics)
     *
     * @param results 角色搜索结果列表 (List of role search results)
     * @return 统计信息 (Statistics)
     */
    public FusionStatistics calculateStatistics(List<RoleSearchResult> results) {
        FusionStatistics stats = new FusionStatistics();

        if (results == null || results.isEmpty()) {
            return stats;
        }

        stats.setRoleCount(results.size());
        stats.setTotalDocuments(results.stream()
                .mapToInt(RoleSearchResult::getDocumentCount)
                .sum());

        Map<String, Integer> docFrequency = new HashMap<>();
        for (RoleSearchResult result : results) {
            if (result.hasResults()) {
                for (ScoredDocument doc : result.getDocuments()) {
                    if (doc.getDocument() != null && doc.getDocument().getId() != null) {
                        String docId = doc.getDocument().getId();
                        docFrequency.put(docId, docFrequency.getOrDefault(docId, 0) + 1);
                    }
                }
            }
        }

        stats.setUniqueDocuments(docFrequency.size());
        stats.setOverlapDocuments((int) docFrequency.values().stream()
                .filter(count -> count > 1)
                .count());

        return stats;
    }

    /**
     * 融合统计信息 (Fusion Statistics)
     */
    @lombok.Data
    public static class FusionStatistics {
        /**
         * 角色数量 (Role count)
         */
        private int roleCount;

        /**
         * 总文档数 (Total documents)
         */
        private int totalDocuments;

        /**
         * 唯一文档数 (Unique documents)
         */
        private int uniqueDocuments;

        /**
         * 重叠文档数 (Overlap documents)
         */
        private int overlapDocuments;

        /**
         * 获取重叠率 (Get overlap rate)
         *
         * @return 重叠率 (Overlap rate)
         */
        public double getOverlapRate() {
            return uniqueDocuments == 0 ? 0.0 : (double) overlapDocuments / uniqueDocuments;
        }
    }
}

