package top.yumbo.ai.rag.index;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.ScoredDocument;
import top.yumbo.ai.rag.model.SearchResult;
import top.yumbo.ai.rag.role.Role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 角色专属向量索引 (Role-specific Vector Index)
 *
 * 为每个角色维护独立的向量索引，实现分角色知识库
 * (Maintains independent vector index for each role to implement role-based knowledge base)
 *
 * 核心功能 (Core Features):
 * - 独立索引空间 (Independent index space)
 * - 按需加载/卸载 (On-demand load/unload)
 * - 索引持久化 (Index persistence)
 * - 增量更新 (Incremental update)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Getter
public class RoleVectorIndex {

    /**
     * 所属角色 (Belonging role)
     */
    private final Role role;

    /**
     * 索引存储路径 (Index storage path)
     */
    private final Path indexPath;

    /**
     * 向量索引引擎 (Vector index engine)
     */
    private SimpleVectorIndexEngine indexEngine;

    /**
     * 索引状态 (Index status)
     */
    private IndexStatus status;

    /**
     * 文档数量 (Document count)
     */
    private final AtomicInteger documentCount;

    /**
     * 创建时间 (Creation time)
     */
    private final Instant createdAt;

    /**
     * 最后加载时间 (Last load time)
     */
    private Instant lastLoadTime;

    /**
     * 最后访问时间 (Last access time)
     */
    private Instant lastAccessTime;

    /**
     * 构造函数 (Constructor)
     *
     * @param role 角色 (Role)
     * @param indexPath 索引路径 (Index path)
     */
    public RoleVectorIndex(Role role, String indexPath) {
        this.role = role;
        this.indexPath = Paths.get(indexPath);
        this.status = IndexStatus.UNLOADED;
        this.documentCount = new AtomicInteger(0);
        this.createdAt = Instant.now();

        log.info(I18N.get("index.role.created", role.getId(), indexPath));
    }

    /**
     * 加载索引 (Load index)
     *
     * @throws IOException 如果加载失败 (If load fails)
     */
    public synchronized void load() throws IOException {
        if (status == IndexStatus.LOADED) {
            log.debug(I18N.get("index.role.already_loaded", role.getId()));
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info(I18N.get("index.role.loading", role.getId()));

        try {
            // 确保目录存在 (Ensure directory exists)
            Files.createDirectories(indexPath.getParent());

            // 创建向量索引引擎 (Create vector index engine)
            // BGE模型默认维度768 (BGE model default dimension 768)
            // 传递父目录，SimpleVectorIndexEngine会在里面创建vector-index子目录
            // (Pass parent directory, SimpleVectorIndexEngine will create vector-index subdirectory inside)
            this.indexEngine = new SimpleVectorIndexEngine(indexPath.getParent().toString(), 768);

            // 更新状态 (Update status)
            this.status = IndexStatus.LOADED;
            this.lastLoadTime = Instant.now();
            this.lastAccessTime = Instant.now();
            this.documentCount.set(indexEngine.size());

            long loadTime = System.currentTimeMillis() - startTime;
            log.info(I18N.get("index.role.loaded", role.getId(), documentCount.get(), loadTime));

        } catch (Exception e) {
            this.status = IndexStatus.ERROR;
            log.error(I18N.get("index.role.load_failed", role.getId(), e.getMessage()), e);
            throw new IOException("Failed to load index for role: " + role.getId(), e);
        }
    }

    /**
     * 卸载索引 (Unload index)
     *
     * @throws IOException 如果卸载失败 (If unload fails)
     */
    public synchronized void unload() throws IOException {
        if (status != IndexStatus.LOADED) {
            log.debug(I18N.get("index.role.not_loaded", role.getId()));
            return;
        }

        log.info(I18N.get("index.role.unloading", role.getId()));

        try {
            // 保存索引 (Save index)
            if (indexEngine != null) {
                indexEngine.saveIndex();
                indexEngine = null;
            }

            // 更新状态 (Update status)
            this.status = IndexStatus.UNLOADED;

            log.info(I18N.get("index.role.unloaded", role.getId()));

        } catch (Exception e) {
            this.status = IndexStatus.ERROR;
            log.error(I18N.get("index.role.unload_failed", role.getId(), e.getMessage()), e);
            throw new IOException("Failed to unload index for role: " + role.getId(), e);
        }
    }

    /**
     * 添加文档到索引 (Add document to index)
     *
     * @param document 文档 (Document)
     * @param vector 向量 (Vector)
     * @throws IOException 如果添加失败 (If add fails)
     */
    public void addDocument(Document document, float[] vector) throws IOException {
        ensureLoaded();

        try {
            indexEngine.addDocument(document.getId(), vector);
            documentCount.incrementAndGet();
            updateAccessTime();

            log.debug(I18N.get("index.role.document_added", role.getId(), document.getId()));

        } catch (Exception e) {
            log.error(I18N.get("index.role.add_failed", role.getId(), document.getId(), e.getMessage()), e);
            throw new IOException("Failed to add document to role index", e);
        }
    }

    /**
     * 批量添加文档 (Batch add documents)
     *
     * @param documents 文档列表 (Document list)
     * @param vectors 向量列表 (Vector list)
     * @throws IOException 如果添加失败 (If add fails)
     */
    public void addDocuments(List<Document> documents, List<float[]> vectors) throws IOException {
        ensureLoaded();

        if (documents.size() != vectors.size()) {
            throw new IllegalArgumentException("Documents and vectors size mismatch");
        }

        log.info(I18N.get("index.role.batch_adding", role.getId(), documents.size()));
        long startTime = System.currentTimeMillis();

        int successCount = 0;
        for (int i = 0; i < documents.size(); i++) {
            try {
                addDocument(documents.get(i), vectors.get(i));
                successCount++;
            } catch (Exception e) {
                log.warn(I18N.get("index.role.add_failed", role.getId(), documents.get(i).getId(), e.getMessage()));
            }
        }

        long batchTime = System.currentTimeMillis() - startTime;
        log.info(I18N.get("index.role.batch_added", role.getId(), successCount, documents.size(), batchTime));
    }

    /**
     * 搜索文档 (Search documents)
     *
     * @param queryVector 查询向量 (Query vector)
     * @param topK 返回数量 (Number to return)
     * @return 搜索结果列表 (Search result list)
     * @throws IOException 如果搜索失败 (If search fails)
     */
    public SearchResult search(float[] queryVector, int topK) throws IOException {
        ensureLoaded();
        updateAccessTime();

        try {
            long startTime = System.currentTimeMillis();
            var vectorResults = indexEngine.search(queryVector, topK);

            // 转换为 ScoredDocument 列表 (Convert to ScoredDocument list)
            List<ScoredDocument> scoredDocs = new ArrayList<>();
            for (var vr : vectorResults) {
                // 注意：这里只有文档ID和分数，实际的Document对象需要从存储层获取
                // (Note: only docId and score here, actual Document object needs to be fetched from storage)
                ScoredDocument scoredDoc = ScoredDocument.builder()
                        .document(Document.builder().id(vr.getDocId()).build())
                        .score(vr.getSimilarity())
                        .build();
                scoredDocs.add(scoredDoc);
            }

            long queryTime = System.currentTimeMillis() - startTime;

            // 构建 SearchResult (Build SearchResult)
            SearchResult result = SearchResult.builder()
                    .documents(scoredDocs)
                    .totalHits(scoredDocs.size())
                    .queryTimeMs(queryTime)
                    .hasMore(false)
                    .build();

            log.debug(I18N.get("index.role.searched", role.getId(), scoredDocs.size(), topK));
            return result;

        } catch (Exception e) {
            log.error(I18N.get("index.role.search_failed", role.getId(), e.getMessage()), e);
            throw new IOException("Failed to search in role index", e);
        }
    }

    /**
     * 删除文档 (Delete document)
     *
     * @param docId 文档ID (Document ID)
     * @return 是否成功 (Whether successful)
     */
    public boolean deleteDocument(String docId) {
        if (status != IndexStatus.LOADED) {
            log.warn(I18N.get("index.role.not_loaded", role.getId()));
            return false;
        }

        try {
            boolean deleted = indexEngine.deleteDocument(docId);
            if (deleted) {
                documentCount.decrementAndGet();
                updateAccessTime();
                log.debug(I18N.get("index.role.document_deleted", role.getId(), docId));
            }
            return deleted;

        } catch (Exception e) {
            log.error(I18N.get("index.role.delete_failed", role.getId(), docId, e.getMessage()), e);
            return false;
        }
    }

    /**
     * 保存索引 (Save index)
     *
     * @throws IOException 如果保存失败 (If save fails)
     */
    public void save() throws IOException {
        if (status != IndexStatus.LOADED) {
            log.debug(I18N.get("index.role.not_loaded", role.getId()));
            return;
        }

        try {
            indexEngine.saveIndex();
            log.info(I18N.get("index.role.saved", role.getId()));

        } catch (Exception e) {
            log.error(I18N.get("index.role.save_failed", role.getId(), e.getMessage()), e);
            throw new IOException("Failed to save role index", e);
        }
    }

    /**
     * 清空索引 (Clear index)
     *
     * @throws IOException 如果清空失败 (If clear fails)
     */
    public void clear() throws IOException {
        ensureLoaded();

        try {
            indexEngine.clear();
            documentCount.set(0);
            log.info(I18N.get("index.role.cleared", role.getId()));

        } catch (Exception e) {
            log.error(I18N.get("index.role.clear_failed", role.getId(), e.getMessage()), e);
            throw new IOException("Failed to clear role index", e);
        }
    }

    /**
     * 获取索引统计信息 (Get index statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public IndexStatistics getStatistics() {
        return IndexStatistics.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .status(status)
                .documentCount(documentCount.get())
                .indexPath(indexPath.toString())
                .createdAt(createdAt)
                .lastLoadTime(lastLoadTime)
                .lastAccessTime(lastAccessTime)
                .build();
    }

    /**
     * 确保索引已加载 (Ensure index is loaded)
     *
     * @throws IOException 如果索引未加载 (If index is not loaded)
     */
    private void ensureLoaded() throws IOException {
        if (status != IndexStatus.LOADED) {
            throw new IOException("Index not loaded for role: " + role.getId());
        }
    }

    /**
     * 更新访问时间 (Update access time)
     */
    private void updateAccessTime() {
        this.lastAccessTime = Instant.now();
    }

    /**
     * 索引状态枚举 (Index status enum)
     */
    public enum IndexStatus {
        /**
         * 未加载 (Unloaded)
         */
        UNLOADED,

        /**
         * 已加载 (Loaded)
         */
        LOADED,

        /**
         * 错误 (Error)
         */
        ERROR
    }
}

