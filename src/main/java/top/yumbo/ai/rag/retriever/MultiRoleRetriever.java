package top.yumbo.ai.rag.retriever;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.index.RoleVectorIndex;
import top.yumbo.ai.rag.loader.KnowledgeBaseLoader;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;
import top.yumbo.ai.rag.role.detector.RoleDetectionResult;
import top.yumbo.ai.rag.role.detector.RoleDetector;
import top.yumbo.ai.rag.role.detector.RoleMatchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 多角色检索器 (Multi-Role Retriever)
 *
 * 综合多个角色知识库进行并行检索和结果融合
 * (Performs parallel retrieval across multiple role knowledge bases and fuses results)
 *
 * 工作流程 (Workflow):
 * 1. 角色识别 (Role detection)
 * 2. 并行检索 (Parallel retrieval)
 * 3. 结果融合 (Result fusion)
 * 4. 排序返回 (Sort and return)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class MultiRoleRetriever {

    @Autowired
    private RoleDetector roleDetector;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private KnowledgeBaseLoader knowledgeBaseLoader;

    @Autowired
    private ResultFusion resultFusion;

    /**
     * 并行检索线程池 (Parallel retrieval thread pool)
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(3,
            r -> new Thread(r, "MultiRoleRetriever"));

    /**
     * 最大角色数 (Maximum role count)
     */
    private static final int MAX_ROLES = 3;

    /**
     * 最小角色置信度 (Minimum role confidence)
     */
    private static final double MIN_ROLE_CONFIDENCE = 0.3;

    /**
     * 主检索方法 (Main retrieval method)
     *
     * @param question 问题 (Question)
     * @param userId 用户ID (User ID)
     * @param topK 返回前K个结果 (Return top K results)
     * @return 检索结果 (Retrieval results)
     */
    public List<Document> retrieve(String question, String userId, int topK) {
        log.info(I18N.get("retriever.multi.start", question, topK));
        long startTime = System.currentTimeMillis();

        try {
            // 1. 角色检测 (Role detection)
            RoleDetectionResult detectionResult = roleDetector.detect(question, userId);

            // 2. 选择Top角色（最多3个） (Select top roles, max 3)
            List<RoleMatchResult> topRoles = selectTopRoles(detectionResult);

            if (topRoles.isEmpty()) {
                log.warn(I18N.get("retriever.multi.no_roles", question));
                // 使用默认角色 (Use default role)
                return retrieveWithDefaultRole(question, topK);
            }

            log.info(I18N.get("retriever.multi.roles_selected",
                    topRoles.stream()
                            .map(r -> String.format("%s(%.2f)", r.getRoleId(), r.getConfidence()))
                            .collect(Collectors.joining(", "))));

            // 3. 并行检索 (Parallel retrieval)
            List<RoleSearchResult> searchResults = parallelSearch(question, topRoles, topK);

            // 4. 结果融合 (Result fusion)
            List<Document> fusedResults = resultFusion.fuseResults(searchResults, topK);

            long duration = System.currentTimeMillis() - startTime;
            log.info(I18N.get("retriever.multi.complete", fusedResults.size(), duration));

            return fusedResults;

        } catch (Exception e) {
            log.error(I18N.get("retriever.multi.failed", question, e.getMessage()), e);
            // 降级：使用默认角色 (Fallback: use default role)
            return retrieveWithDefaultRole(question, topK);
        }
    }

    /**
     * 带查询向量的检索方法 (Retrieval method with query vector)
     *
     * @param question 问题 (Question)
     * @param queryVector 查询向量 (Query vector)
     * @param userId 用户ID (User ID)
     * @param topK 返回前K个结果 (Return top K results)
     * @return 检索结果 (Retrieval results)
     */
    public List<Document> retrieveWithVector(String question, float[] queryVector,
                                            String userId, int topK) {
        log.info(I18N.get("retriever.multi.start_with_vector", question, topK));
        long startTime = System.currentTimeMillis();

        try {
            // 1. 角色检测 (Role detection)
            RoleDetectionResult detectionResult = roleDetector.detect(question, userId);

            // 2. 选择Top角色 (Select top roles)
            List<RoleMatchResult> topRoles = selectTopRoles(detectionResult);

            if (topRoles.isEmpty()) {
                return retrieveWithDefaultRole(question, topK);
            }

            // 3. 并行检索（使用向量） (Parallel retrieval with vector)
            List<RoleSearchResult> searchResults = parallelSearchWithVector(
                    queryVector, topRoles, topK);

            // 4. 结果融合 (Result fusion)
            List<Document> fusedResults = resultFusion.fuseResults(searchResults, topK);

            long duration = System.currentTimeMillis() - startTime;
            log.info(I18N.get("retriever.multi.complete", fusedResults.size(), duration));

            return fusedResults;

        } catch (Exception e) {
            log.error(I18N.get("retriever.multi.failed", question, e.getMessage()), e);
            return retrieveWithDefaultRole(question, topK);
        }
    }

    /**
     * 选择Top角色 (Select top roles)
     *
     * @param detectionResult 检测结果 (Detection result)
     * @return Top角色列表 (Top role list)
     */
    private List<RoleMatchResult> selectTopRoles(RoleDetectionResult detectionResult) {
        if (detectionResult == null || detectionResult.getAllCandidates() == null) {
            return Collections.emptyList();
        }

        return detectionResult.getAllCandidates().stream()
                .filter(r -> r.getConfidence() >= MIN_ROLE_CONFIDENCE)
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(MAX_ROLES)
                .collect(Collectors.toList());
    }

    /**
     * 并行检索 (Parallel retrieval)
     *
     * @param question 问题 (Question)
     * @param topRoles Top角色列表 (Top role list)
     * @param topK 每个角色返回的结果数 (Results per role)
     * @return 角色搜索结果列表 (List of role search results)
     */
    private List<RoleSearchResult> parallelSearch(String question,
                                                  List<RoleMatchResult> topRoles,
                                                  int topK) {
        // 创建异步搜索任务 (Create async search tasks)
        List<CompletableFuture<RoleSearchResult>> futures = topRoles.stream()
                .map(roleMatch -> CompletableFuture.supplyAsync(() ->
                        searchInRole(question, roleMatch, topK), executorService))
                .collect(Collectors.toList());

        // 等待所有搜索完成 (Wait for all searches to complete)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果 (Collect results)
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null && result.hasResults())
                .collect(Collectors.toList());
    }

    /**
     * 并行检索（使用向量） (Parallel retrieval with vector)
     *
     * @param queryVector 查询向量 (Query vector)
     * @param topRoles Top角色列表 (Top role list)
     * @param topK 每个角色返回的结果数 (Results per role)
     * @return 角色搜索结果列表 (List of role search results)
     */
    private List<RoleSearchResult> parallelSearchWithVector(float[] queryVector,
                                                            List<RoleMatchResult> topRoles,
                                                            int topK) {
        List<CompletableFuture<RoleSearchResult>> futures = topRoles.stream()
                .map(roleMatch -> CompletableFuture.supplyAsync(() ->
                        searchInRoleWithVector(queryVector, roleMatch, topK), executorService))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null && result.hasResults())
                .collect(Collectors.toList());
    }

    /**
     * 在单个角色中搜索 (Search in a single role)
     *
     * @param question 问题 (Question)
     * @param roleMatch 角色匹配结果 (Role match result)
     * @param topK 返回结果数 (Result count)
     * @return 角色搜索结果 (Role search result)
     */
    private RoleSearchResult searchInRole(String question, RoleMatchResult roleMatch, int topK) {
        long startTime = System.currentTimeMillis();

        try {
            String roleId = roleMatch.getRoleId();

            // 获取角色索引 (Get role index)
            RoleVectorIndex index = knowledgeBaseLoader.getIndex(roleId);

            // TODO: 这里需要先将question转换为向量
            // 目前简化处理，直接返回空结果
            // (Need to convert question to vector first, simplified here)

            long duration = System.currentTimeMillis() - startTime;

            log.debug(I18N.get("retriever.multi.role_search_complete",
                    roleId, 0, duration));

            // 获取角色信息 (Get role info)
            Role role = roleManager.getRole(roleId);
            String roleName = role != null ? role.getName() : roleId;

            RoleSearchResult result = new RoleSearchResult();
            result.setRoleId(roleId);
            result.setRoleName(roleName);
            result.setRoleWeight(roleMatch.getConfidence());
            result.setDocuments(new ArrayList<>());
            result.setSearchTimeMs(duration);

            return result;

        } catch (Exception e) {
            log.error(I18N.get("retriever.multi.role_search_failed",
                    roleMatch.getRoleId(), e.getMessage()), e);
            return new RoleSearchResult(roleMatch.getRoleId(), roleMatch.getConfidence(),
                    new ArrayList<>());
        }
    }

    /**
     * 在单个角色中搜索（使用向量） (Search in a single role with vector)
     *
     * @param queryVector 查询向量 (Query vector)
     * @param roleMatch 角色匹配结果 (Role match result)
     * @param topK 返回结果数 (Result count)
     * @return 角色搜索结果 (Role search result)
     */
    private RoleSearchResult searchInRoleWithVector(float[] queryVector,
                                                   RoleMatchResult roleMatch,
                                                   int topK) {
        long startTime = System.currentTimeMillis();

        try {
            String roleId = roleMatch.getRoleId();

            // 获取角色索引 (Get role index)
            RoleVectorIndex index = knowledgeBaseLoader.getIndex(roleId);

            // 使用向量搜索 (Search with vector)
            SearchResult searchResult = index.search(queryVector, topK);

            long duration = System.currentTimeMillis() - startTime;

            log.debug(I18N.get("retriever.multi.role_search_complete",
                    roleId, searchResult.size(), duration));

            // 获取角色信息 (Get role info)
            Role role = roleManager.getRole(roleId);
            String roleName = role != null ? role.getName() : roleId;

            RoleSearchResult result = new RoleSearchResult();
            result.setRoleId(roleId);
            result.setRoleName(roleName);
            result.setRoleWeight(roleMatch.getConfidence());
            result.setDocuments(searchResult.getDocuments());
            result.setSearchTimeMs(duration);

            return result;

        } catch (Exception e) {
            log.error(I18N.get("retriever.multi.role_search_failed",
                    roleMatch.getRoleId(), e.getMessage()), e);
            return new RoleSearchResult(roleMatch.getRoleId(), roleMatch.getConfidence(),
                    new ArrayList<>());
        }
    }

    /**
     * 使用默认角色检索 (Retrieve with default role)
     *
     * @param question 问题 (Question)
     * @param topK 返回结果数 (Result count)
     * @return 检索结果 (Retrieval results)
     */
    private List<Document> retrieveWithDefaultRole(String question, int topK) {
        try {
            log.info(I18N.get("retriever.multi.fallback_default"));

            // 使用默认的开发者角色 (Use default developer role)
            RoleVectorIndex index = knowledgeBaseLoader.getIndex("developer");

            // TODO: 需要先将question转换为向量
            // (Need to convert question to vector first)

            return Collections.emptyList();

        } catch (Exception e) {
            log.error(I18N.get("retriever.multi.default_failed", e.getMessage()), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取检索统计信息 (Get retrieval statistics)
     *
     * @param question 问题 (Question)
     * @param userId 用户ID (User ID)
     * @param topK 返回结果数 (Result count)
     * @return 检索统计 (Retrieval statistics)
     */
    public RetrievalStatistics getStatistics(String question, String userId, int topK) {
        RetrievalStatistics stats = new RetrievalStatistics();

        try {
            // 角色检测 (Role detection)
            RoleDetectionResult detectionResult = roleDetector.detect(question, userId);
            List<RoleMatchResult> topRoles = selectTopRoles(detectionResult);

            stats.setRoleCount(topRoles.size());
            stats.setRoleWeights(topRoles.stream()
                    .collect(Collectors.toMap(
                            RoleMatchResult::getRoleId,
                            RoleMatchResult::getConfidence)));

        } catch (Exception e) {
            log.error("Failed to get statistics", e);
        }

        return stats;
    }

    /**
     * 检索统计信息 (Retrieval Statistics)
     */
    @lombok.Data
    public static class RetrievalStatistics {
        /**
         * 角色数量 (Role count)
         */
        private int roleCount;

        /**
         * 角色权重映射 (Role weight map)
         */
        private java.util.Map<String, Double> roleWeights;

        /**
         * 总检索时间 (Total retrieval time)
         */
        private long totalTimeMs;

        /**
         * 融合后文档数 (Fused document count)
         */
        private int fusedDocumentCount;
    }
}

