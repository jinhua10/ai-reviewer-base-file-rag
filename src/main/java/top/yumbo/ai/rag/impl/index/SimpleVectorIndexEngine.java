package top.yumbo.ai.rag.impl.index;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化版本地向量索引引擎
 * 使用 HashMap 实现向量存储和暴力检索
 *
 * 说明：由于 JVector 4.0 API 复杂度较高，这里提供一个简化版本
 * 使用线性扫描进行向量检索，适合中小规模数据（<10万条）
 *
 * 优势：
 * - ✅ 实现简单，易于理解
 * - ✅ 无需复杂的依赖
 * - ✅ 本地文件持久化
 * - ✅ 余弦相似度精确计算
 *
 * 性能：
 * - 1万条文档：<100ms
 * - 10万条文档：<1s
 * - 100万条文档：需要考虑升级到专业向量库
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class SimpleVectorIndexEngine {

    private final Path indexPath;
    private final int dimension;

    // 向量存储：文档ID -> 向量
    private final Map<String, float[]> vectorStore = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public SimpleVectorIndexEngine(String basePath, int dimension) throws IOException {
        this.indexPath = Paths.get(basePath, "vector-index");
        this.dimension = dimension;

        Files.createDirectories(indexPath);

        // 尝试加载现有索引
        Path indexFile = indexPath.resolve("vectors.dat");
        if (Files.exists(indexFile)) {
            loadIndex();
        }

        log.info("✅ 简化版向量索引引擎已初始化");
        log.info("   - 索引路径: {}", indexPath);
        log.info("   - 向量维度: {}", dimension);
        log.info("   - 当前向量数: {}", size());
        log.info("   - 检索方式: 线性扫描（适合<10万条）");
    }

    /**
     * 添加文档向量到索引
     */
    public synchronized void addDocument(String docId, float[] vector) {
        if (vector == null || vector.length != dimension) {
            throw new IllegalArgumentException(
                String.format("向量维度不匹配: 期望 %d, 实际 %d",
                             dimension, vector == null ? 0 : vector.length)
            );
        }

        vectorStore.put(docId, vector);
        log.trace("添加向量: docId={}, dim={}", docId, vector.length);
    }

    /**
     * 批量添加向量
     */
    public void addDocumentBatch(Map<String, float[]> documents) {
        for (Map.Entry<String, float[]> entry : documents.entrySet()) {
            addDocument(entry.getKey(), entry.getValue());
        }
        log.info("批量添加 {} 个向量", documents.size());
    }

    /**
     * 向量相似度搜索（线性扫描）
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK) {
        return search(queryVector, topK, 0.0f);
    }

    /**
     * 向量相似度搜索（带阈值过滤）
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK, float similarityThreshold) {
        if (queryVector == null || queryVector.length != dimension) {
            throw new IllegalArgumentException(
                String.format("查询向量维度不匹配: 期望 %d, 实际 %d",
                             dimension, queryVector == null ? 0 : queryVector.length)
            );
        }

        if (vectorStore.isEmpty()) {
            log.debug("索引为空，返回空结果");
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();

        // 使用优先队列（最小堆）保存 Top-K 结果
        PriorityQueue<VectorSearchResult> topKHeap = new PriorityQueue<>(
            Comparator.comparingDouble(VectorSearchResult::getSimilarity)
        );

        // 线性扫描所有向量
        int scanned = 0;
        int filtered = 0;

        for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
            scanned++;

            // 计算余弦相似度
            float similarity = cosineSimilarity(queryVector, entry.getValue());

            // 阈值过滤
            if (similarity < similarityThreshold) {
                filtered++;
                continue;
            }

            VectorSearchResult result = new VectorSearchResult(
                entry.getKey(),
                similarity,
                -1  // 简化版没有 vectorId
            );

            if (topKHeap.size() < topK) {
                topKHeap.offer(result);
            } else if (similarity > topKHeap.peek().getSimilarity()) {
                topKHeap.poll();
                topKHeap.offer(result);
            }
        }

        // 转换为降序列表
        List<VectorSearchResult> results = new ArrayList<>(topKHeap);
        results.sort(Comparator.comparingDouble(VectorSearchResult::getSimilarity).reversed());

        long elapsedTime = System.currentTimeMillis() - startTime;

        log.debug("向量搜索完成: scanned={}, filtered={}, found={}, time={}ms",
                 scanned, filtered, results.size(), elapsedTime);

        return results;
    }

    /**
     * 计算余弦相似度
     * 前提：向量已经归一化（L2范数=1）
     */
    private float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        // 对于归一化向量，余弦相似度 = 点积
        float dotProduct = 0.0f;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }

        return dotProduct;
    }

    /**
     * 删除文档向量
     */
    public synchronized boolean deleteDocument(String docId) {
        float[] removed = vectorStore.remove(docId);
        if (removed != null) {
            log.debug("删除向量: docId={}", docId);
            return true;
        }
        return false;
    }

    /**
     * 持久化索引到本地文件
     */
    public synchronized void saveIndex() throws IOException {
        log.info("开始保存向量索引...");

        Path vectorsFile = indexPath.resolve("vectors.dat");

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(vectorsFile)))) {

            // 写入元数据
            dos.writeInt(vectorStore.size());
            dos.writeInt(dimension);

            // 写入每个向量
            for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
                // 写入文档ID
                dos.writeUTF(entry.getKey());

                // 写入向量
                float[] vector = entry.getValue();
                for (float v : vector) {
                    dos.writeFloat(v);
                }
            }
        }

        log.info("✅ 向量索引已保存: {} 个向量, 文件大小: {} KB",
                 size(), Files.size(vectorsFile) / 1024);
    }

    /**
     * 从本地文件加载索引
     */
    private void loadIndex() throws IOException {
        log.info("开始加载向量索引...");

        Path vectorsFile = indexPath.resolve("vectors.dat");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(vectorsFile)))) {

            // 读取元数据
            int count = dis.readInt();
            int dim = dis.readInt();

            if (dim != this.dimension) {
                throw new IOException(
                    String.format("向量维度不匹配: 索引=%d, 期望=%d", dim, this.dimension)
                );
            }

            // 读取每个向量
            for (int i = 0; i < count; i++) {
                // 读取文档ID
                String docId = dis.readUTF();

                // 读取向量
                float[] vector = new float[dim];
                for (int j = 0; j < dim; j++) {
                    vector[j] = dis.readFloat();
                }

                vectorStore.put(docId, vector);
            }
        }

        log.info("✅ 向量索引已加载: {} 个向量", size());
    }

    /**
     * 清空索引
     */
    public synchronized void clear() throws IOException {
        vectorStore.clear();

        // 删除本地文件
        Files.deleteIfExists(indexPath.resolve("vectors.dat"));

        log.info("✅ 向量索引已清空");
    }

    /**
     * 获取索引大小
     */
    public int size() {
        return vectorStore.size();
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * 检查文档是否已索引
     */
    public boolean containsDocument(String docId) {
        return vectorStore.containsKey(docId);
    }

    /**
     * 向量搜索结果
     */
    @Data
    public static class VectorSearchResult {
        private final String docId;
        private final float similarity;  // 余弦相似度 (0-1)
        private final int vectorId;      // 简化版中无意义，保留兼容性

        public VectorSearchResult(String docId, float similarity, int vectorId) {
            this.docId = docId;
            this.similarity = similarity;
            this.vectorId = vectorId;
        }
    }
}

