package top.yumbo.ai.rag.spring.boot.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.service.LocalFileRAG;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.optimization.DocumentChunker;
import top.yumbo.ai.rag.optimization.MemoryMonitor;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

import java.io.IOException;

/**
 * 文档处理优化服务
 * 提供内存管理、批处理、自动分块等优化功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Service
public class DocumentProcessingOptimizer {

    private final KnowledgeQAProperties properties;
    @Getter
    private final MemoryMonitor memoryMonitor;

    // 批处理内存阈值
    private static final long BATCH_MEMORY_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final double GC_TRIGGER_THRESHOLD = 80.0; // 80%触发GC

    private long currentBatchMemory = 0;

    public DocumentProcessingOptimizer(KnowledgeQAProperties properties) {
        this.properties = properties;
        this.memoryMonitor = new MemoryMonitor();
    }

    /**
     * 检查是否需要进行批处理
     *
     * @param estimatedMemory 预估的内存使用
     * @return 是否需要批处理
     */
    public boolean shouldBatch(long estimatedMemory) {
        return (currentBatchMemory + estimatedMemory) > BATCH_MEMORY_THRESHOLD;
    }

    /**
     * 记录批处理内存使用
     */
    public void addBatchMemory(long memory) {
        currentBatchMemory += memory;
    }

    /**
     * 重置批处理计数器
     */
    public void resetBatchMemory() {
        currentBatchMemory = 0;
    }

    /**
     * 检查并触发GC
     */
    public void checkAndTriggerGC() {
        double memoryUsage = memoryMonitor.getMemoryUsagePercent();

        if (memoryUsage > GC_TRIGGER_THRESHOLD) {
            log.warn(LogMessageProvider.getMessage("log.memory.gc_trigger", String.format("%.1f", memoryUsage)));
            System.gc();

            try {
                Thread.sleep(100); // 给GC一些时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            double afterGC = memoryMonitor.getMemoryUsagePercent();
            log.info(LogMessageProvider.getMessage("log.memory.gc_done", String.format("%.1f", memoryUsage), String.format("%.1f", afterGC)));
        }
    }

    /**
     * 判断是否需要自动分块
     *
     * @param contentSize 内容大小（字节）
     * @return 是否需要分块
     */
    public boolean shouldAutoChunk(long contentSize) {
        long autoChunkThreshold = properties.getDocument().getAutoChunkThresholdMb() * 1024L * 1024L;
        return contentSize > autoChunkThreshold;
    }

    /**
     * 创建文档分块器
     */
    public DocumentChunker createChunker() {
        return top.yumbo.ai.rag.optimization.DocumentChunker.builder()
            .chunkSize(properties.getDocument().getChunkSize())
            .chunkOverlap(properties.getDocument().getChunkOverlap())
            .smartSplit(true)
            .maxContentLength(properties.getDocument().getMaxChunkContentLength())
            .maxChunks(properties.getDocument().getMaxChunksPerDocument())
            .build();
    }

    /**
     * 检查文件大小是否在限制内
     *
     * @param fileSize 文件大小
     * @return 是否通过检查
     */
    public boolean checkFileSize(long fileSize) {
        long maxFileSize = properties.getDocument().getMaxFileSizeMb() * 1024L * 1024L;
        return fileSize <= maxFileSize;
    }

    /**
     * 检查内容大小是否需要强制分块
     *
     * @param contentSize 内容大小
     * @return 是否需要强制分块
     */
    public boolean needsForceChunking(long contentSize) {
        long maxContentSize = properties.getDocument().getMaxContentSizeMb() * 1024L * 1024L;
        return contentSize > maxContentSize;
    }

    /**
     * 估算文档内存占用
     *
     * @param contentLength 内容长度
     * @return 估算的内存大小（字节）
     */
    public long estimateMemoryUsage(int contentLength) {
        // 粗略估算：内容大小 * 2（考虑对象开销和索引）
        return (long) contentLength * 2;
    }

    /**
     * 保存向量索引
     */
    public void saveVectorIndex(SimpleVectorIndexEngine vectorIndexEngine) {
        if (vectorIndexEngine != null) {
            try {
                log.info(LogMessageProvider.getMessage("log.optimizer.saving_vectors"));
                vectorIndexEngine.saveIndex();
                log.info(LogMessageProvider.getMessage("log.optimizer.vectors_saved"));
            } catch (IOException e) {
                log.error(LogMessageProvider.getMessage("log.optimizer.save_failed"), e);
            }
        }
    }

    /**
     * 关闭嵌入引擎
     */
    public void closeEmbeddingEngine(LocalEmbeddingEngine embeddingEngine) {
        if (embeddingEngine != null) {
            embeddingEngine.close();
            log.info(LogMessageProvider.getMessage("log.optimizer.embedding_closed"));
        }
    }

    /**
     * 提交RAG更改并优化
     */
    public void commitAndOptimize(LocalFileRAG rag) {
        log.info(LogMessageProvider.getMessage("log.optimizer.commit"));
        rag.commit();

        log.info(LogMessageProvider.getMessage("log.optimizer.optimize"));
        rag.optimizeIndex();

        log.info(LogMessageProvider.getMessage("log.optimizer.done"));
    }

    /**
     * 打印内存使用情况
     */
    public void logMemoryUsage(String context) {
        double usage = memoryMonitor.getMemoryUsagePercent();
        long usedMB = memoryMonitor.getUsedMemoryMB();
        long maxMB = memoryMonitor.getMaxMemoryMB();

        log.info(LogMessageProvider.getMessage("log.memory.usage_phase", context, usedMB, maxMB, String.format("%.1f", usage)));
    }
}
