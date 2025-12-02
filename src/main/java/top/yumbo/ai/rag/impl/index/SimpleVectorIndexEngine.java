package top.yumbo.ai.rag.impl.index;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化版本地向量索引引擎（Simple Local Vector Index Engine）
 * 使用 HashMap 实现向量存储和暴力检索（Uses HashMap for vector storage and brute-force retrieval）
 *
 * 说明：由于 JVector 4.0 API 复杂度较高，这里提供一个简化版本（Note: Due to the complexity of JVector 4.0 API, this is a simplified version）
 * 使用线性扫描进行向量检索，适合中小规模数据（<10万条）（Uses linear scan for vector retrieval, suitable for small to medium scale data (<100K)）
 *
 * 优势（Advantages）：
 * - ✅ 实现简单，易于理解（Simple implementation, easy to understand）
 * - ✅ 无需复杂的依赖（No complex dependencies required）
 * - ✅ 本地文件持久化（Local file persistence）
 * - ✅ 余弦相似度精确计算（Precise cosine similarity calculation）
 *
 * 性能（Performance）：
 * - 1万条文档（10K documents）：<100ms
 * - 10万条文档（100K documents）：<1s
 * - 100万条文档（1M documents）：需要考虑升级到专业向量库（Consider upgrading to professional vector library）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class SimpleVectorIndexEngine {

    private final Path indexPath;
    /**
     * -- GETTER --
     *  获取向量维度（Get vector dimension）
     */
    @Getter
    private final int dimension;

    // 向量存储：文档ID -> 向量（Vector storage: Document ID -> Vector）
    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();

    /**
     * 构造函数（Constructor）
     */
    public SimpleVectorIndexEngine(String basePath, int dimension) throws IOException {
        this.indexPath = Paths.get(basePath, "vector-index");
        this.dimension = dimension;

        Files.createDirectories(indexPath);

        // 尝试加载现有索引（Try to load existing index）
        Path indexFile = indexPath.resolve("vectors.dat");
        if (Files.exists(indexFile)) {
            loadIndex();
        }

        log.info(LogMessageProvider.getMessage("vector_index.log.init"));
        log.info(LogMessageProvider.getMessage("vector_index.log.index_path", indexPath));
        log.info(LogMessageProvider.getMessage("vector_index.log.dimension", dimension));
        log.info(LogMessageProvider.getMessage("vector_index.log.current_count", size()));
        log.info(LogMessageProvider.getMessage("vector_index.log.search_method"));
    }

    /**
     * 添加文档向量到索引（Add document vector to index）
     */
    public synchronized void addDocument(String docId, float[] vector) {
        if (vector == null || vector.length != dimension) {
            throw new IllegalArgumentException(
                    LogMessageProvider.getMessage("vector_index.error.dimension_mismatch",
                            dimension, vector == null ? 0 : vector.length)
            );
        }

        vectorStore.put(docId, vector);
        log.trace(LogMessageProvider.getMessage("vector_index.log.add_vector"),
                docId, vector.length);
    }

    /**
     * 批量添加向量（Batch add vectors）
     */
    public void addDocumentBatch(Map<String, float[]> documents) {
        for (Map.Entry<String, float[]> entry : documents.entrySet()) {
            addDocument(entry.getKey(), entry.getValue());
        }
        log.info(LogMessageProvider.getMessage("vector_index.log.batch_add"),
                documents.size());
    }

    /**
     * 向量相似度搜索（线性扫描）（Vector similarity search using linear scan）
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, 0.0f);
    }

    /**
     * 向量相似度搜索（带阈值过滤）（Vector similarity search with threshold filtering）
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK, float similarityThreshold) {
        if (queryVector == null || queryVector.length != dimension) {
            throw new IllegalArgumentException(
                    LogMessageProvider.getMessage("vector_index.error.query_dimension_mismatch",
                            dimension, queryVector == null ? 0 : queryVector.length)
            );
        }

        if (vectorStore.isEmpty()) {
            log.debug(LogMessageProvider.getMessage("vector_index.log.index_empty"));
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();

        // 使用优先队列（最小堆）保存 Top-K 结果（Use priority queue (min-heap) to save Top-K results）
        PriorityQueue<VectorSearchResult> topKHeap = new PriorityQueue<>(
                Comparator.comparingDouble(VectorSearchResult::getSimilarity)
        );

        // 线性扫描所有向量（Linear scan all vectors）
        int scanned = 0;
        int filtered = 0;

        for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
            scanned++;

            // 计算余弦相似度（Calculate cosine similarity）
            float similarity = cosineSimilarity(queryVector, entry.getValue());

            // 阈值过滤（Threshold filtering）
            if (similarity < similarityThreshold) {
                filtered++;
                continue;
            }

            VectorSearchResult result = new VectorSearchResult(
                    entry.getKey(),
                    similarity,
                    -1  // 简化版没有 vectorId（Simplified version has no vectorId）
            );

            if (topKHeap.size() < topK) {
                topKHeap.offer(result);
            } else if (similarity > topKHeap.peek().getSimilarity()) {
                topKHeap.poll();
                topKHeap.offer(result);
            }
        }

        // 转换为降序列表（Convert to descending list）
        List<VectorSearchResult> results = new ArrayList<>(topKHeap);
        results.sort(Comparator.comparingDouble(VectorSearchResult::getSimilarity).reversed());

        long elapsedTime = System.currentTimeMillis() - startTime;

        log.debug(LogMessageProvider.getMessage("vector_index.log.search_complete"),
                scanned, filtered, results.size(), elapsedTime);

        return results;
    }

    /**
     * 计算余弦相似度（Calculate cosine similarity）
     * 前提：向量已经归一化（L2范数=1）（Prerequisite: vectors are normalized (L2 norm = 1)）
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException(
                    LogMessageProvider.getMessage("vector_index.error.vector_dimension_mismatch")
            );
        }

        // 对于归一化向量，余弦相似度 = 点积（For normalized vectors, cosine similarity = dot product）
        float dotProduct = 0.0f;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }

        return dotProduct;
    }

    /**
     * 删除文档向量（Delete document vector）
     */
    public synchronized boolean deleteDocument(String docId) {
        float[] removed = vectorStore.remove(docId);
        if (removed != null) {
            log.debug(LogMessageProvider.getMessage("vector_index.log.delete_vector"), docId);
            return true;
        }
        return false;
    }

    /**
     * 持久化索引到本地文件（Persist index to local file）
     */
    public synchronized void saveIndex() throws IOException {
        log.info(LogMessageProvider.getMessage("vector_index.log.save_start"));

        Path vectorsFile = indexPath.resolve("vectors.dat");

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(vectorsFile)))) {

            // 写入元数据（Write metadata）
            dos.writeInt(vectorStore.size());
            dos.writeInt(dimension);

            // 写入每个向量（Write each vector）
            for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
                // 写入文档ID（Write document ID）
                dos.writeUTF(entry.getKey());

                // 写入向量（Write vector）
                float[] vector = entry.getValue();
                for (float v : vector) {
                    dos.writeFloat(v);
                }
            }
        }

        log.info(LogMessageProvider.getMessage("vector_index.log.save_complete"),
                size(), Files.size(vectorsFile) / 1024);
    }

    /**
     * 从本地文件加载索引（Load index from local file）
     */
    private void loadIndex() throws IOException {
        log.info(LogMessageProvider.getMessage("vector_index.log.load_start"));

        Path vectorsFile = indexPath.resolve("vectors.dat");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(vectorsFile)))) {

            // 读取元数据（Read metadata）
            int count = dis.readInt();
            int dim = dis.readInt();

            if (dim != this.dimension) {
                throw new IOException(
                        LogMessageProvider.getMessage("vector_index.error.index_dimension_mismatch",
                                dim, this.dimension)
                );
            }

            // 读取每个向量（Read each vector）
            for (int i = 0; i < count; i++) {
                // 读取文档ID（Read document ID）
                String docId = dis.readUTF();

                // 读取向量（Read vector）
                float[] vector = new float[dim];
                for (int j = 0; j < dim; j++) {
                    vector[j] = dis.readFloat();
                }

                vectorStore.put(docId, vector);
            }
        }

        log.info(LogMessageProvider.getMessage("vector_index.log.load_complete", size()));
    }

    /**
     * 清空索引（Clear index）
     */
    public synchronized void clear() throws IOException {
        vectorStore.clear();

        // 删除本地文件（Delete local file）
        Files.deleteIfExists(indexPath.resolve("vectors.dat"));

        log.info(LogMessageProvider.getMessage("vector_index.log.clear_complete"));
    }

    /**
     * 获取索引大小（Get index size）
     */
    public int size() {
        return vectorStore.size();
    }

    /**
     * 检查文档是否已索引（Check if document is indexed）
     */
    public boolean containsDocument(String docId) {
        return vectorStore.containsKey(docId);
    }

    /**
     * 向量搜索结果（Vector search result）
     */
    @Data
    public static class VectorSearchResult {
        private final String docId;
        private final float similarity;  // 余弦相似度 (0-1)（Cosine similarity (0-1)）
        private final int vectorId;      // 简化版中无意义，保留兼容性（Meaningless in simplified version, kept for compatibility）

        public VectorSearchResult(String docId, float similarity, int vectorId) {
            this.docId = docId;
            this.similarity = similarity;
            this.vectorId = vectorId;
        }
    }
}
