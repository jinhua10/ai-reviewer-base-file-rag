package top.yumbo.ai.rag.test;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;
import top.yumbo.ai.rag.i18n.I18N;

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
        log.info(I18N.get("log.vector.test.start"));
        log.info("=".repeat(80));

        String indexPath = "./data/test-vector-index";

        log.info(I18N.get("log.vector.index_path", indexPath));
        log.info("");

        LocalEmbeddingEngine embeddingEngine = null;
        SimpleVectorIndexEngine vectorIndexEngine = null;

        try {
            log.info(I18N.get("log.vector.init_embedding"));
            embeddingEngine = new LocalEmbeddingEngine();

            log.info(I18N.get("log.vector.embedding_init_success"));
            log.info(I18N.get("log.vector.embedding_model", embeddingEngine.getModelName()));
            log.info(I18N.get("log.vector.embedding_dim", embeddingEngine.getEmbeddingDim()));
            log.info("");

            log.info(I18N.get("log.vector.init_index"));
            vectorIndexEngine = new SimpleVectorIndexEngine(
                indexPath,
                embeddingEngine.getEmbeddingDim()
            );

            log.info(I18N.get("log.vector.index_init_success"));
            log.info(I18N.get("log.vector.index_path", indexPath));
            log.info(I18N.get("log.vector.vector_count", vectorIndexEngine.size()));
            log.info("");

            log.info(I18N.get("log.vector.test_success"));
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error(I18N.get("log.vector.test_failed"), e);
            log.error("");
            log.error(I18N.get("log.vector.possible_reasons"));
            log.error(I18N.get("log.vector.reason_model_missing"));
            log.error(I18N.get("log.vector.reason_model_path"));
            log.error(I18N.get("log.vector.reason_onnx"));
            log.error("");
            log.error(I18N.get("log.vector.possible_fixes"));
            log.error(I18N.get("log.vector.fix_place_model"));
            log.error(I18N.get("log.vector.fix_supported_models"));
            log.error(I18N.get("log.vector.fix_check_logs"));
            log.error("=".repeat(80));
            System.exit(1);
        } finally {
            // 清理资源 (Cleanup resources)
            if (embeddingEngine != null) {
                embeddingEngine.close();
                log.info(I18N.get("log.vector.embedding_closed"));
            }
        }
    }
}
