package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;

/**
 * 向量检索功能测试
 * 验证模型加载是否正常
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class VectorSearchTest {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info("🧪 向量检索功能测试");
        log.info("=".repeat(80));

        String indexPath = "./data/test-vector-index";

        log.info("📍 向量索引路径: {}", indexPath);
        log.info("");

        LocalEmbeddingEngine embeddingEngine = null;
        SimpleVectorIndexEngine vectorIndexEngine = null;

        try {
            log.info("🚀 初始化向量嵌入引擎...");
            embeddingEngine = new LocalEmbeddingEngine();

            log.info("✅ 向量嵌入引擎初始化成功");
            log.info("   - 模型: {}", embeddingEngine.getModelName());
            log.info("   - 维度: {}", embeddingEngine.getEmbeddingDim());
            log.info("");

            log.info("🚀 初始化向量索引引擎...");
            vectorIndexEngine = new SimpleVectorIndexEngine(
                indexPath,
                embeddingEngine.getEmbeddingDim()
            );

            log.info("✅ 向量索引引擎初始化成功");
            log.info("   - 索引路径: {}", indexPath);
            log.info("   - 向量数量: {}", vectorIndexEngine.size());
            log.info("");

            log.info("✅ 测试成功！向量检索引擎初始化正常");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("❌ 测试失败", e);
            log.error("");
            log.error("💡 可能的原因：");
            log.error("   1. 模型文件不存在");
            log.error("   2. 模型文件路径不正确");
            log.error("   3. ONNX Runtime 依赖问题");
            log.error("");
            log.error("🔧 解决方法：");
            log.error("   1. 将模型文件放到 src/main/resources/models/ 目录");
            log.error("   2. 支持的模型: bge-m3, paraphrase-multilingual, 等");
            log.error("   3. 检查日志中的详细错误信息");
            log.error("=".repeat(80));
            System.exit(1);
        } finally {
            // 清理资源
            if (embeddingEngine != null) {
                embeddingEngine.close();
                log.info("🔄 向量嵌入引擎已关闭");
            }
        }
    }
}

