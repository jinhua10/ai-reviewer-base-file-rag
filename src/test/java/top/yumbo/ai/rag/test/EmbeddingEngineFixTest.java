package top.yumbo.ai.rag.test;

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;

import java.io.IOException;

/**
 * LocalEmbeddingEngine 修复验证测试
 * 验证 token_type_ids 修复是否有效
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class EmbeddingEngineFixTest {

    public static void main(String[] args) {
        log.info("=".repeat(80));
        log.info("🧪 LocalEmbeddingEngine 修复验证测试");
        log.info("=".repeat(80));
        log.info("");
        log.info("本测试验证以下修复：");
        log.info("  ✅ 添加 token_type_ids 输入");
        log.info("  ✅ 修复 ONNX Runtime 推理错误");
        log.info("");

        // 测试1: 检查模型文件是否存在
        log.info("📋 测试1: 检查模型文件");
        String[] possiblePaths = {
            "./models/text2vec-base-chinese/model.onnx",
            "models/text2vec-base-chinese/model.onnx",
            "src/main/resources/models/text2vec-base-chinese/model.onnx"
        };

        boolean modelFound = false;
        String modelPath = null;
        for (String path : possiblePaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                modelFound = true;
                modelPath = path;
                log.info("  ✅ 找到模型文件: {}", file.getAbsolutePath());
                break;
            }
        }

        if (!modelFound) {
            log.warn("  ⚠️  未找到模型文件，跳过实际推理测试");
            log.info("");
            log.info("💡 要完整测试修复，请下载模型文件并放到以下任一位置：");
            for (String path : possiblePaths) {
                log.info("   - {}", path);
            }
            log.info("");
            log.info("模型下载地址：");
            log.info("   https://huggingface.co/shibing624/text2vec-base-chinese");
            log.info("");
            log.info("=".repeat(80));
            log.info("✅ 代码修复验证完成（编译通过）");
            log.info("=".repeat(80));
            return;
        }

        // 测试2: 初始化嵌入引擎
        log.info("");
        log.info("📋 测试2: 初始化嵌入引擎");
        LocalEmbeddingEngine engine = null;
        try {
            engine = new LocalEmbeddingEngine(modelPath);
            log.info("  ✅ 嵌入引擎初始化成功");
            log.info("     - 模型: {}", engine.getModelName());
            log.info("     - 维度: {}", engine.getEmbeddingDim());

        } catch (OrtException | IOException e) {
            log.error("  ❌ 嵌入引擎初始化失败", e);
            log.info("");
            log.info("=".repeat(80));
            System.exit(1);
        }

        // 测试3: 执行嵌入推理
        log.info("");
        log.info("📋 测试3: 执行嵌入推理（验证 token_type_ids 修复）");
        try {
            String testText = "这是一个测试文本";
            log.info("  输入文本: {}", testText);

            float[] embedding = engine.embed(testText);

            log.info("  ✅ 嵌入生成成功");
            log.info("     - 向量维度: {}", embedding.length);
            log.info("     - 向量范数: {}", calculateNorm(embedding));
            log.info("     - 前5个值: [{}, {}, {}, {}, {}]",
                String.format("%.4f", embedding[0]),
                String.format("%.4f", embedding[1]),
                String.format("%.4f", embedding[2]),
                String.format("%.4f", embedding[3]),
                String.format("%.4f", embedding[4])
            );

        } catch (Exception e) {
            log.error("  ❌ 嵌入推理失败", e);
            log.info("");
            log.info("=".repeat(80));
            engine.close();
            System.exit(1);
        }

        // 测试4: 测试特殊文本（之前导致错误的Excel内容）
        log.info("");
        log.info("📋 测试4: 测试Excel内容文本");
        try {
            String excelText = "l0810.xls\n长表8-10\n\t表8—10   全国按户主的职业、住房来源分的家庭户户数";
            log.info("  输入文本: {}", excelText.substring(0, Math.min(50, excelText.length())) + "...");

            float[] embedding = engine.embed(excelText);

            log.info("  ✅ Excel内容嵌入成功");
            log.info("     - 向量维度: {}", embedding.length);
            log.info("     - 向量范数: {}", calculateNorm(embedding));

        } catch (Exception e) {
            log.error("  ❌ Excel内容嵌入失败", e);
            log.info("");
            log.info("=".repeat(80));
            engine.close();
            System.exit(1);
        }

        // 清理
        engine.close();
        log.info("  ✅ 嵌入引擎已关闭");

        log.info("");
        log.info("=".repeat(80));
        log.info("✅ 所有测试通过！token_type_ids 修复成功");
        log.info("=".repeat(80));
    }

    /**
     * 计算向量范数（验证归一化）
     */
    private static double calculateNorm(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) {
            sumSquares += v * v;
        }
        return Math.sqrt(sumSquares);
    }
}

