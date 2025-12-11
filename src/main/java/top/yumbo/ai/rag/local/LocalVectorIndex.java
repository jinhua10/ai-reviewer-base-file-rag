package top.yumbo.ai.rag.local;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地向量索引 (Local Vector Index)
 *
 * 功能 (Features):
 * 1. 本地向量存储和检索 (Local vector storage and retrieval)
 * 2. 快速相似度计算 (Fast similarity calculation)
 * 3. 角色过滤 (Role filtering)
 * 4. 内存优化 (Memory optimization)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class LocalVectorIndex {

    /**
     * 向量索引存储 (Vector index storage)
     * Key: documentId, Value: VectorEntry
     */
    private final Map<String, VectorEntry> vectorIndex = new ConcurrentHashMap<>();

    /**
     * 角色索引 (Role index)
     * Key: roleId, Value: List of documentIds
     */
    private final Map<String, List<String>> roleIndex = new ConcurrentHashMap<>();

    /**
     * 向量维度 (Vector dimension)
     */
    private int dimension = 768;  // 默认 768 维 (Default 768 dimensions)

    // ========== 索引构建 (Index Building) ==========

    /**
     * 添加向量到索引 (Add vector to index)
     *
     * @param entry 向量条目 (Vector entry)
     */
    public void addVector(VectorEntry entry) {
        try {
            // 添加到向量索引 (Add to vector index)
            vectorIndex.put(entry.getDocumentId(), entry);

            // 添加到角色索引 (Add to role index)
            if (entry.getRoleId() != null) {
                roleIndex.computeIfAbsent(entry.getRoleId(), k -> new ArrayList<>())
                    .add(entry.getDocumentId());
            }

            log.debug(I18N.get("local.vector.added"), entry.getDocumentId());

        } catch (Exception e) {
            log.error(I18N.get("local.vector.add_failed"), entry.getDocumentId(), e.getMessage(), e);
        }
    }

    /**
     * 批量添加向量 (Batch add vectors)
     *
     * @param entries 向量条目列表 (Vector entry list)
     */
    public void batchAddVectors(List<VectorEntry> entries) {
        int successCount = 0;
        for (VectorEntry entry : entries) {
            try {
                addVector(entry);
                successCount++;
            } catch (Exception e) {
                log.warn(I18N.get("local.vector.add_failed"), entry.getDocumentId(), e.getMessage());
            }
        }

        log.info(I18N.get("local.vector.batch_added"), successCount, entries.size());
    }

    // ========== 向量检索 (Vector Search) ==========

    /**
     * 搜索相似向量 (Search similar vectors)
     *
     * @param queryVector 查询向量 (Query vector)
     * @param topK 返回前K个结果 (Return top K results)
     * @return 相似度排序的结果列表 (Similarity-sorted result list)
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, null);
    }

    /**
     * 搜索相似向量（带角色过滤） (Search similar vectors with role filter)
     *
     * @param queryVector 查询向量 (Query vector)
     * @param topK 返回前K个结果 (Return top K results)
     * @param roleId 角色ID过滤（可选） (Role ID filter, optional)
     * @return 相似度排序的结果列表 (Similarity-sorted result list)
     */
    public List<SearchResult> search(float[] queryVector, int topK, String roleId) {
        try {
            log.debug(I18N.get("local.vector.searching"), topK, roleId != null ? roleId : "all");

            // 获取候选文档 (Get candidate documents)
            List<String> candidates = getCandidateDocuments(roleId);

            // 计算相似度 (Calculate similarity)
            List<SearchResult> results = new ArrayList<>();
            for (String docId : candidates) {
                VectorEntry entry = vectorIndex.get(docId);
                if (entry != null) {
                    double similarity = cosineSimilarity(queryVector, entry.getVector());
                    results.add(new SearchResult(docId, similarity, entry));
                }
            }

            // 按相似度降序排序 (Sort by similarity descending)
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            // 返回 Top K (Return top K)
            return results.stream()
                .limit(topK)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error(I18N.get("local.vector.search_failed"), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取候选文档列表 (Get candidate documents)
     */
    private List<String> getCandidateDocuments(String roleId) {
        if (roleId != null && roleIndex.containsKey(roleId)) {
            // 返回特定角色的文档 (Return documents of specific role)
            return roleIndex.get(roleId);
        } else {
            // 返回所有文档 (Return all documents)
            return new ArrayList<>(vectorIndex.keySet());
        }
    }

    // ========== 相似度计算 (Similarity Calculation) ==========

    /**
     * 计算余弦相似度 (Calculate cosine similarity)
     *
     * @param vec1 向量1 (Vector 1)
     * @param vec2 向量2 (Vector 2)
     * @return 相似度 [0, 1] (Similarity [0, 1])
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException(
                I18N.get("local.vector.dimension_mismatch", vec1.length, vec2.length)
            );
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ========== 索引管理 (Index Management) ==========

    /**
     * 删除向量 (Delete vector)
     *
     * @param documentId 文档ID (Document ID)
     */
    public void deleteVector(String documentId) {
        VectorEntry entry = vectorIndex.remove(documentId);
        if (entry != null) {
            // 从角色索引中删除 (Remove from role index)
            if (entry.getRoleId() != null) {
                List<String> roleDocuments = roleIndex.get(entry.getRoleId());
                if (roleDocuments != null) {
                    roleDocuments.remove(documentId);
                }
            }
            log.info(I18N.get("local.vector.deleted"), documentId);
        }
    }

    /**
     * 清空索引 (Clear index)
     */
    public void clear() {
        vectorIndex.clear();
        roleIndex.clear();
        log.info(I18N.get("local.vector.cleared"));
    }

    /**
     * 获取索引统计 (Get index statistics)
     */
    public IndexStats getStats() {
        IndexStats stats = new IndexStats();
        stats.setTotalVectors(vectorIndex.size());
        stats.setTotalRoles(roleIndex.size());
        stats.setDimension(dimension);

        // 计算内存使用（估算） (Calculate memory usage, estimated)
        long memoryBytes = (long) vectorIndex.size() * dimension * 4; // float = 4 bytes
        stats.setEstimatedMemoryBytes(memoryBytes);

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 向量条目 (Vector Entry)
     */
    @Data
    public static class VectorEntry {
        private String documentId;       // 文档ID
        private String roleId;           // 角色ID
        private float[] vector;          // 向量数据
        private String content;          // 原始内容（可选）
        private Map<String, Object> metadata;  // 元数据
    }

    /**
     * 搜索结果 (Search Result)
     */
    @Data
    public static class SearchResult {
        private final String documentId;
        private final double similarity;
        private final VectorEntry entry;
    }

    /**
     * 索引统计 (Index Statistics)
     */
    @Data
    public static class IndexStats {
        private int totalVectors;         // 向量总数
        private int totalRoles;           // 角色总数
        private int dimension;            // 向量维度
        private long estimatedMemoryBytes; // 估算内存使用（字节）

        /**
         * 获取格式化的内存大小 (Get formatted memory size)
         */
        public String getFormattedMemory() {
            if (estimatedMemoryBytes < 1024) {
                return estimatedMemoryBytes + " B";
            } else if (estimatedMemoryBytes < 1024 * 1024) {
                return String.format("%.2f KB", estimatedMemoryBytes / 1024.0);
            } else if (estimatedMemoryBytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", estimatedMemoryBytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", estimatedMemoryBytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}

