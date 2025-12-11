package top.yumbo.ai.rag.index;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 索引构建器 (Index Builder)
 *
 * 负责构建和管理所有角色的向量索引
 * (Responsible for building and managing vector indices for all roles)
 *
 * 核心功能 (Core Features):
 * - 批量构建索引 (Batch build indices)
 * - 增量更新索引 (Incremental update indices)
 * - 索引重建 (Rebuild indices)
 * - 索引优化 (Optimize indices)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class IndexBuilder {

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private DocumentClassifier classifier;

    @Autowired(required = false)
    private LocalEmbeddingEngine embeddingEngine;

    @Value("${rag.index.base-path:data/vector-index}")
    private String indexBasePath;

    /**
     * 角色索引缓存 (Role index cache)
     */
    private final Map<String, RoleVectorIndex> indexCache = new ConcurrentHashMap<>();

    /**
     * 构建所有角色的索引 (Build indices for all roles)
     *
     * @param documents 文档列表 (Document list)
     * @return 构建结果 (Build result)
     */
    public BuildResult buildAll(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn(I18N.get("index.builder.documents.empty"));
            return BuildResult.empty();
        }

        log.info(I18N.get("index.builder.start", documents.size()));
        long startTime = System.currentTimeMillis();

        BuildResult result = new BuildResult();

        try {
            // 1. 检查 embedding 引擎 (Check embedding engine)
            if (embeddingEngine == null) {
                throw new IllegalStateException("Embedding engine not available");
            }

            // 2. 文档分类 (Classify documents)
            Map<Role, List<Document>> groupedDocs = classifier.groupByRole(documents);
            result.setTotalDocuments(documents.size());
            result.setRoleCount(groupedDocs.size());

            // 3. 为每个角色构建索引 (Build index for each role)
            for (Map.Entry<Role, List<Document>> entry : groupedDocs.entrySet()) {
                Role role = entry.getKey();
                List<Document> roleDocs = entry.getValue();

                try {
                    BuildRoleResult roleResult = buildForRole(role, roleDocs);
                    result.addRoleResult(role.getId(), roleResult);

                } catch (Exception e) {
                    log.error(I18N.get("index.builder.role_failed", role.getId(), e.getMessage()), e);
                    result.addError(role.getId(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            result.setBuildTimeMs(duration);
            result.setSuccess(result.getErrorCount() == 0);

            log.info(I18N.get("index.builder.complete",
                    result.getSuccessCount(), result.getTotalDocuments(), duration));

            return result;

        } catch (Exception e) {
            log.error(I18N.get("index.builder.failed", e.getMessage()), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 为单个角色构建索引 (Build index for single role)
     *
     * @param role 角色 (Role)
     * @param documents 文档列表 (Document list)
     * @return 构建结果 (Build result)
     * @throws IOException 如果构建失败 (If build fails)
     */
    public BuildRoleResult buildForRole(Role role, List<Document> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            log.warn(I18N.get("index.builder.role_empty", role.getId()));
            return BuildRoleResult.empty(role.getId());
        }

        log.info(I18N.get("index.builder.role_start", role.getId(), documents.size()));
        long startTime = System.currentTimeMillis();

        BuildRoleResult result = new BuildRoleResult();
        result.setRoleId(role.getId());
        result.setRoleName(role.getName());
        result.setTotalDocuments(documents.size());

        try {
            // 1. 获取或创建角色索引 (Get or create role index)
            RoleVectorIndex index = getOrCreateIndex(role);

            // 2. 加载索引 (Load index)
            index.load();

            // 3. 生成向量并添加到索引 (Generate vectors and add to index)
            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (Document doc : documents) {
                try {
                    // 生成向量 (Generate vector)
                    float[] vector = embeddingEngine.embed(doc.getContent());

                    // 添加到索引 (Add to index)
                    index.addDocument(doc, vector);
                    successCount++;

                } catch (Exception e) {
                    log.warn(I18N.get("index.builder.doc_failed", doc.getId(), e.getMessage()));
                    errors.add(String.format("%s: %s", doc.getId(), e.getMessage()));
                }
            }

            // 4. 保存索引 (Save index)
            index.save();

            // 5. 记录结果 (Record result)
            result.setSuccessCount(successCount);
            result.setFailureCount(documents.size() - successCount);
            result.setErrors(errors);

            long duration = System.currentTimeMillis() - startTime;
            result.setBuildTimeMs(duration);
            result.setSuccess(errors.isEmpty());

            log.info(I18N.get("index.builder.role_complete",
                    role.getId(), successCount, documents.size(), duration));

            return result;

        } catch (Exception e) {
            log.error(I18N.get("index.builder.role_error", role.getId(), e.getMessage()), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw new IOException("Failed to build index for role: " + role.getId(), e);
        }
    }

    /**
     * 增量更新索引 (Incremental update index)
     *
     * @param documents 新文档列表 (New document list)
     * @return 更新结果 (Update result)
     */
    public BuildResult incrementalUpdate(List<Document> documents) {
        log.info(I18N.get("index.builder.incremental.start", documents.size()));
        return buildAll(documents);
    }

    /**
     * 重建所有索引 (Rebuild all indices)
     *
     * @param documents 文档列表 (Document list)
     * @return 重建结果 (Rebuild result)
     * @throws IOException 如果重建失败 (If rebuild fails)
     */
    public BuildResult rebuildAll(List<Document> documents) throws IOException {
        log.info(I18N.get("index.builder.rebuild.start"));

        // 1. 清空所有现有索引 (Clear all existing indices)
        for (Role role : roleManager.getEnabledRoles()) {
            try {
                RoleVectorIndex index = getOrCreateIndex(role);
                index.load();
                index.clear();
                log.info(I18N.get("index.builder.role_cleared", role.getId()));
            } catch (Exception e) {
                log.warn(I18N.get("index.builder.clear_failed", role.getId(), e.getMessage()));
            }
        }

        // 2. 重新构建 (Rebuild)
        return buildAll(documents);
    }

    /**
     * 获取或创建角色索引 (Get or create role index)
     *
     * @param role 角色 (Role)
     * @return 角色索引 (Role index)
     */
    public RoleVectorIndex getOrCreateIndex(Role role) {
        return indexCache.computeIfAbsent(role.getId(), k -> {
            String indexPath = buildIndexPath(role);
            return new RoleVectorIndex(role, indexPath);
        });
    }

    /**
     * 获取角色索引 (Get role index)
     *
     * @param roleId 角色ID (Role ID)
     * @return 角色索引，如果不存在返回null (Role index, null if not exists)
     */
    public RoleVectorIndex getIndex(String roleId) {
        return indexCache.get(roleId);
    }

    /**
     * 获取所有索引统计信息 (Get all index statistics)
     *
     * @return 统计信息列表 (Statistics list)
     */
    public List<IndexStatistics> getAllStatistics() {
        return indexCache.values().stream()
                .map(RoleVectorIndex::getStatistics)
                .collect(Collectors.toList());
    }

    /**
     * 卸载所有索引 (Unload all indices)
     */
    public void unloadAll() {
        log.info(I18N.get("index.builder.unload.all"));

        for (RoleVectorIndex index : indexCache.values()) {
            try {
                index.unload();
            } catch (Exception e) {
                log.warn(I18N.get("index.builder.unload_failed",
                        index.getRole().getId(), e.getMessage()));
            }
        }

        indexCache.clear();
        log.info(I18N.get("index.builder.unload.complete"));
    }

    /**
     * 构建索引路径 (Build index path)
     *
     * @param role 角色 (Role)
     * @return 索引路径 (Index path)
     */
    private String buildIndexPath(Role role) {
        // 如果角色配置中指定了索引路径，使用它 (Use if specified in role config)
        if (role.getIndexPath() != null && !role.getIndexPath().isEmpty()) {
            return role.getIndexPath();
        }

        // 否则使用默认路径 (Otherwise use default path)
        Path basePath = Paths.get(indexBasePath);
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn(I18N.get("index.builder.create_dir_failed", basePath, e.getMessage()));
        }

        return basePath.resolve("role_" + role.getId() + ".index").toString();
    }

    /**
     * 构建结果 (Build Result)
     */
    @lombok.Data
    public static class BuildResult {
        private boolean success;
        private int totalDocuments;
        private int roleCount;
        private long buildTimeMs;
        private Map<String, BuildRoleResult> roleResults = new HashMap<>();
        private String errorMessage;

        public void addRoleResult(String roleId, BuildRoleResult result) {
            roleResults.put(roleId, result);
        }

        public void addError(String roleId, String error) {
            BuildRoleResult result = roleResults.getOrDefault(roleId, new BuildRoleResult());
            result.setRoleId(roleId);
            result.setSuccess(false);
            result.setErrorMessage(error);
            roleResults.put(roleId, result);
        }

        public int getSuccessCount() {
            return roleResults.values().stream()
                    .mapToInt(BuildRoleResult::getSuccessCount)
                    .sum();
        }

        public int getErrorCount() {
            return roleResults.values().stream()
                    .filter(r -> !r.isSuccess())
                    .mapToInt(r -> 1)
                    .sum();
        }

        public static BuildResult empty() {
            BuildResult result = new BuildResult();
            result.setSuccess(true);
            result.setTotalDocuments(0);
            result.setRoleCount(0);
            return result;
        }
    }

    /**
     * 角色构建结果 (Role Build Result)
     */
    @lombok.Data
    public static class BuildRoleResult {
        private String roleId;
        private String roleName;
        private boolean success;
        private int totalDocuments;
        private int successCount;
        private int failureCount;
        private long buildTimeMs;
        private List<String> errors = new ArrayList<>();
        private String errorMessage;

        public static BuildRoleResult empty(String roleId) {
            BuildRoleResult result = new BuildRoleResult();
            result.setRoleId(roleId);
            result.setSuccess(true);
            result.setTotalDocuments(0);
            result.setSuccessCount(0);
            result.setFailureCount(0);
            return result;
        }
    }
}

