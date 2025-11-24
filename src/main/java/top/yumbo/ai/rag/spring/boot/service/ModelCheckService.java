package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;

import java.io.File;
import java.io.InputStream;

/**
 * 模型检查服务
 * 在应用启动时检查向量嵌入模型是否存在
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Component
@Order(1)  // 最先执行
public class ModelCheckService {

    private final KnowledgeQAProperties properties;

    public ModelCheckService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 应用启动时检查模型
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkModelOnStartup() {
        // 如果未启用向量检索，跳过检查
        if (!properties.getVectorSearch().isEnabled()) {
            log.info("⚠️  向量检索已禁用，跳过模型检查");
            return;
        }

        log.info("=".repeat(80));
        log.info("🔍 检查向量嵌入模型...");
        log.info("=".repeat(80));

        boolean modelFound = checkModel();

        if (!modelFound) {
            printModelDownloadInstructions();

            log.error("=".repeat(80));
            log.error("❌ 模型文件不存在，应用将退出");
            log.error("=".repeat(80));
            log.error("");
            log.error("💡 解决方法:");
            log.error("   1. 按照上述说明下载模型文件");
            log.error("   2. 将模型文件放到 src/main/resources/models/ 目录");
            log.error("   3. 重新启动应用");
            log.error("");
            log.error("   或者在 application.yml 中设置:");
            log.error("   knowledge.qa.vector-search.enabled: false");
            log.error("   以禁用向量检索功能（将使用纯关键词检索）");
            log.error("");

            // 退出应用
            System.exit(1);
        }

        log.info("=".repeat(80));
        log.info("✅ 模型检查通过");
        log.info("=".repeat(80));
    }

    /**
     * 检查模型是否存在
     */
    private boolean checkModel() {
        var modelConfig = properties.getVectorSearch().getModel();

        // 检查所有可能的模型目录
        for (String modelDir : modelConfig.getSearchPaths()) {
            for (String fileName : modelConfig.getFileNames()) {

                // 1. 检查 resources 中是否存在
                String resourcePath = "/models/" + modelDir + "/" + fileName;
                InputStream resourceStream = getClass().getResourceAsStream(resourcePath);

                if (resourceStream != null) {
                    try {
                        resourceStream.close();
                        log.info("✅ 找到模型: {}", resourcePath);
                        log.info("   - 模型目录: models/{}", modelDir);
                        log.info("   - 模型文件: {}", fileName);
                        return true;
                    } catch (Exception e) {
                        // 忽略
                    }
                }

                // 2. 检查文件系统中是否存在
                String fileSystemPath = "./models/" + modelDir + "/" + fileName;
                File file = new File(fileSystemPath);
                if (file.exists()) {
                    log.info("✅ 找到模型: {}", file.getAbsolutePath());
                    log.info("   - 模型目录: models/{}", modelDir);
                    log.info("   - 模型文件: {}", fileName);
                    return true;
                }

                // 3. 检查 src/main/resources 中是否存在
                String srcResourcePath = "src/main/resources/models/" + modelDir + "/" + fileName;
                File srcFile = new File(srcResourcePath);
                if (srcFile.exists()) {
                    log.info("✅ 找到模型: {}", srcFile.getAbsolutePath());
                    log.info("   - 模型目录: models/{}", modelDir);
                    log.info("   - 模型文件: {}", fileName);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 打印模型下载说明
     */
    private void printModelDownloadInstructions() {
        log.error("");
        log.error("❌ 未找到向量嵌入模型文件！");
        log.error("");
        log.error("=".repeat(80));
        log.error("📥 推荐的模型（按性能排序）");
        log.error("=".repeat(80));
        log.error("");
        log.error("1️⃣  BGE-M3 ⭐⭐⭐⭐⭐ （2024最新，性能最佳）");
        log.error("   https://huggingface.co/BAAI/bge-m3");
        log.error("   目录: src/main/resources/models/bge-m3/model.onnx");
        log.error("");
        log.error("2️⃣  Multilingual-E5-Large ⭐⭐⭐⭐ （微软出品，平衡）");
        log.error("   https://huggingface.co/intfloat/multilingual-e5-large");
        log.error("   目录: src/main/resources/models/multilingual-e5-large/model.onnx");
        log.error("");
        log.error("3️⃣  BGE-Large-ZH ⭐⭐⭐⭐ （中文最佳）");
        log.error("   https://huggingface.co/BAAI/bge-large-zh-v1.5");
        log.error("   目录: src/main/resources/models/bge-large-zh/model.onnx");
        log.error("");
        log.error("4️⃣  Paraphrase-Multilingual ⭐⭐⭐ （轻量兼容）");
        log.error("   https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2");
        log.error("   目录: src/main/resources/models/paraphrase-multilingual/model.onnx");
        log.error("");
        log.error("=".repeat(80));
        log.error("📖 快速下载方法");
        log.error("=".repeat(80));
        log.error("");
        log.error("方法1: 使用 Python 脚本（推荐）");
        log.error("```bash");
        log.error("pip install optimum[onnxruntime] transformers");
        log.error("");
        log.error("python -c \"");
        log.error("from optimum.onnxruntime import ORTModelForFeatureExtraction");
        log.error("from transformers import AutoTokenizer");
        log.error("");
        log.error("model = ORTModelForFeatureExtraction.from_pretrained('BAAI/bge-m3', export=True)");
        log.error("tokenizer = AutoTokenizer.from_pretrained('BAAI/bge-m3')");
        log.error("");
        log.error("model.save_pretrained('src/main/resources/models/bge-m3')");
        log.error("tokenizer.save_pretrained('src/main/resources/models/bge-m3')");
        log.error("\"");
        log.error("```");
        log.error("");
        log.error("方法2: 手动下载");
        log.error("1. 访问上述 HuggingFace 链接");
        log.error("2. 下载 model.onnx 文件");
        log.error("3. 放到 src/main/resources/models/[模型名称]/ 目录");
        log.error("");
        log.error("=".repeat(80));
        log.error("📁 已搜索的位置");
        log.error("=".repeat(80));

        var modelConfig = properties.getVectorSearch().getModel();
        for (String modelDir : modelConfig.getSearchPaths()) {
            log.error("   - src/main/resources/models/{}/", modelDir);
        }

        log.error("");
        log.error("📝 详细文档: 模型下载说明.md");
        log.error("");
    }
}

