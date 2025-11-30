package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * 向量检索功能测试 (Vector Search Functionality Test)
 * 验证模型加载是否正常 (Verify model loading is correct)
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class VectorSearchTest {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info(LogMessageProvider.getMessage("log.vector.test.start"));
        log.info("=".repeat(80));

        String indexPath = "./data/test-vector-index";

        log.info(LogMessageProvider.getMessage("log.vector.index_path", indexPath));
        log.info("");

        LocalEmbeddingEngine embeddingEngine = null;
        SimpleVectorIndexEngine vectorIndexEngine = null;

        try {
            log.info(LogMessageProvider.getMessage("log.vector.init_embedding"));
            embeddingEngine = new LocalEmbeddingEngine();

            log.info(LogMessageProvider.getMessage("log.vector.embedding_init_success"));
            log.info(LogMessageProvider.getMessage("log.vector.embedding_model", embeddingEngine.getModelName()));
            log.info(LogMessageProvider.getMessage("log.vector.embedding_dim", embeddingEngine.getEmbeddingDim()));
            log.info("");

            log.info(LogMessageProvider.getMessage("log.vector.init_index"));
            vectorIndexEngine = new SimpleVectorIndexEngine(
                indexPath,
                embeddingEngine.getEmbeddingDim()
            );

            log.info(LogMessageProvider.getMessage("log.vector.index_init_success"));
            log.info(LogMessageProvider.getMessage("log.vector.index_path", indexPath));
            log.info(LogMessageProvider.getMessage("log.vector.vector_count", vectorIndexEngine.size()));
            log.info("");

            log.info(LogMessageProvider.getMessage("log.vector.test_success"));
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.vector.test_failed"), e);
            log.error("");
            log.error(LogMessageProvider.getMessage("log.vector.possible_reasons"));
            log.error(LogMessageProvider.getMessage("log.vector.reason_model_missing"));
            log.error(LogMessageProvider.getMessage("log.vector.reason_model_path"));
            log.error(LogMessageProvider.getMessage("log.vector.reason_onnx"));
            log.error("");
            log.error(LogMessageProvider.getMessage("log.vector.possible_fixes"));
            log.error(LogMessageProvider.getMessage("log.vector.fix_place_model"));
            log.error(LogMessageProvider.getMessage("log.vector.fix_supported_models"));
            log.error(LogMessageProvider.getMessage("log.vector.fix_check_logs"));
            log.error("=".repeat(80));
            System.exit(1);
        } finally {
            // 清理资源 (Cleanup resources)
            if (embeddingEngine != null) {
                embeddingEngine.close();
                log.info(LogMessageProvider.getMessage("log.vector.embedding_closed"));
            }
        }
    }
}
