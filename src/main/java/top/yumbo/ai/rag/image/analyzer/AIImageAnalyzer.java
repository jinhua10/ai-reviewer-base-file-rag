package top.yumbo.ai.rag.image.analyzer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.List;

/**
 * AI 图片分析服务（AI image analysis service）
 * 使用 LLM 对图片进行语义分析（Use LLM for semantic analysis of images）
 *
 * 核心功能：（Core features:）
 * 1. 识别图片类型（架构图、流程图、数据图、截图等）（Identify image types (architecture diagrams, flowcharts, data charts, screenshots, etc.)）
 * 2. 生成图片的文本描述（Generate text descriptions of images）
 * 3. 提取图片中的关键信息（Extract key information from images）
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Slf4j
public class AIImageAnalyzer {

    private final LLMClient llmClient;
    private final boolean enabled;
    private final String model;

    public AIImageAnalyzer(LLMClient llmClient, boolean enabled, String model) {
        this.llmClient = llmClient;
        this.enabled = enabled;
        this.model = model;
    }

    /**
     * 分析图片并生成描述（Analyze image and generate description）
     *
     * @param image 提取的图片（Extracted image）
     * @return 更新后的图片（包含 AI 分析结果）（Updated image (containing AI analysis results)）
     */
    public ExtractedImage analyzeImage(ExtractedImage image) {
        if (!enabled || llmClient == null) {
            log.debug(I18N.get("log.image.ai.disabled"));
            return image;
        }

        try {
            log.info(I18N.get("log.image.ai.analyzing", image.getOriginalName()));

            // 构建分析 Prompt（Build analysis prompt）
            String prompt = buildAnalysisPrompt(image);

            // 调用 LLM（注意：这里需要支持图片输入的 LLM，如 GPT-4 Vision）（Call LLM (Note: LLM that supports image input is required, such as GPT-4 Vision)）
            // 如果 LLM 不支持图片，则使用上下文文本进行分析（If LLM does not support images, use context text for analysis）
            String analysis = analyzeWithLLM(prompt, image);

            // 解析分析结果（Parse analysis results）
            parseAnalysisResult(image, analysis);

            log.info(I18N.get("log.image.ai.completed", image.getImageType(),
                    image.getAiDescription() != null ? image.getAiDescription().substring(0, Math.min(50, image.getAiDescription().length())) : "N/A"));

            return image;

        } catch (Exception e) {
            log.error(I18N.get("log.image.ai.failed", image.getOriginalName()), e);
            // 返回原图片，不中断流程（Return original image, do not interrupt process）
            return image;
        }
    }

    /**
     * 批量分析图片（Batch analyze images）
     */
    public List<ExtractedImage> analyzeImages(List<ExtractedImage> images) {
        if (!enabled) {
            return images;
        }

        log.info(I18N.get("log.image.ai.batch_start", images.size()));

        for (ExtractedImage image : images) {
            analyzeImage(image);
        }

        return images;
    }

    /**
     * 构建分析 Prompt（Build analysis prompt）
     */
    private String buildAnalysisPrompt(ExtractedImage image) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请分析这张图片，并提供以下信息：\n\n");
        prompt.append("1. **图片类型**：识别图片属于哪种类型（选择一个）\n");
        prompt.append("   - 架构图（Architecture Diagram）\n");
        prompt.append("   - 流程图（Flowchart）\n");
        prompt.append("   - 数据图表（Data Chart/Graph）\n");
        prompt.append("   - 截图（Screenshot）\n");
        prompt.append("   - UML 图（UML Diagram）\n");
        prompt.append("   - 网络拓扑图（Network Topology）\n");
        prompt.append("   - 界面原型（UI Mockup）\n");
        prompt.append("   - 照片（Photo）\n");
        prompt.append("   - 其他（Other）\n\n");

        prompt.append("2. **图片描述**：用 1-2 句话描述图片的主要内容\n\n");

        prompt.append("3. **关键信息**：提取图片中的关键文字、数据或概念\n\n");

        // 添加上下文信息（Add context information）
        if (image.getContextText() != null && !image.getContextText().isEmpty()) {
            prompt.append("**文档上下文**：\n");
            prompt.append(image.getContextText()).append("\n\n");
        }

        prompt.append("请以以下 JSON 格式返回结果：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"type\": \"图片类型\",\n");
        prompt.append("  \"description\": \"图片描述\",\n");
        prompt.append("  \"keywords\": [\"关键词1\", \"关键词2\"]\n");
        prompt.append("}\n");
        prompt.append("```");

        return prompt.toString();
    }

    /**
     * 使用 LLM 分析（Use LLM for analysis）
     * 注意：如果 LLM 支持 Vision API，应该传递图片数据（Note: If LLM supports Vision API, image data should be passed）
     * 目前实现：仅使用上下文文本进行分析（Current implementation: Only use context text for analysis）
     */
    private String analyzeWithLLM(String prompt, ExtractedImage image) {
        try {
            // TODO: 如果 LLM 支持 Vision API（如 GPT-4 Vision），可以传递图片（TODO: If LLM supports Vision API (such as GPT-4 Vision), image can be passed）
            // 当前实现：仅使用文本分析（Current implementation: Only use text analysis）

            String response = llmClient.generate(prompt);
            return response;

        } catch (Exception e) {
            log.error(I18N.get("log.image.ai.llm_failed"), e);
            throw e;
        }
    }

    /**
     * 解析分析结果（Parse analysis results）
     */
    private void parseAnalysisResult(ExtractedImage image, String analysis) {
        try {
            // 简单的 JSON 解析（实际应使用 Jackson）（Simple JSON parsing (should use Jackson in practice)）

            // 提取 type（Extract type）
            int typeStart = analysis.indexOf("\"type\"");
            if (typeStart > 0) {
                int typeValueStart = analysis.indexOf(":", typeStart) + 1;
                int typeValueEnd = analysis.indexOf(",", typeValueStart);
                if (typeValueEnd < 0) typeValueEnd = analysis.indexOf("}", typeValueStart);

                String type = analysis.substring(typeValueStart, typeValueEnd)
                        .replace("\"", "")
                        .trim();
                image.setImageType(type);
            }

            // 提取 description（Extract description）
            int descStart = analysis.indexOf("\"description\"");
            if (descStart > 0) {
                int descValueStart = analysis.indexOf(":", descStart) + 1;
                int descValueEnd = analysis.indexOf(",", descValueStart);
                if (descValueEnd < 0) descValueEnd = analysis.indexOf("}", descValueStart);

                String description = analysis.substring(descValueStart, descValueEnd)
                        .replace("\"", "")
                        .trim();
                image.setAiDescription(description);
            }

            // 如果解析失败，使用整个响应作为描述（If parsing fails, use the entire response as description）
            if (image.getAiDescription() == null || image.getAiDescription().isEmpty()) {
                // 去除 JSON 标记（Remove JSON markers）
                String cleanText = analysis
                        .replace("```json", "")
                        .replace("```", "")
                        .trim();

                if (cleanText.length() > 200) {
                    cleanText = cleanText.substring(0, 200);
                }

                image.setAiDescription(cleanText);
            }

        } catch (Exception e) {
            log.warn(I18N.get("log.image.ai.parse_failed"), e);

            // 使用原始响应（截断）（Use raw response (truncated)）
            String desc = analysis.length() > 200 ? analysis.substring(0, 200) : analysis;
            image.setAiDescription(desc);
        }
    }

    /**
     * 简化版分析（仅基于上下文文本，不调用 LLM）（Simplified analysis (based only on context text, does not call LLM)）
     * 用于降级场景（Used for fallback scenarios）
     */
    public ExtractedImage simpleAnalyze(ExtractedImage image) {
        try {
            // 基于上下文文本进行简单的关键词匹配（Perform simple keyword matching based on context text）
            String context = image.getContextText();
            if (context == null) context = "";

            String contextLower = context.toLowerCase();

            // 简单的类型判断（Simple type judgment）
            if (contextLower.contains("架构") || contextLower.contains("architecture")) {
                image.setImageType("架构图");
            } else if (contextLower.contains("流程") || contextLower.contains("flow")) {
                image.setImageType("流程图");
            } else if (contextLower.contains("图表") || contextLower.contains("chart") || contextLower.contains("数据")) {
                image.setImageType("数据图表");
            } else if (contextLower.contains("界面") || contextLower.contains("ui") || contextLower.contains("页面")) {
                image.setImageType("界面原型");
            } else {
                image.setImageType("图片");
            }

            // 简单的描述生成（Simple description generation）
            String desc = "位于文档第 " + image.getPosition() + " 页/幻灯片";
            if (context.length() > 0) {
                desc += "，相关内容：" + context.substring(0, Math.min(100, context.length()));
            }
            image.setAiDescription(desc);

        } catch (Exception e) {
            log.warn(I18N.get("log.image.ai.simple_failed"), e);
        }

        return image;
    }
}
