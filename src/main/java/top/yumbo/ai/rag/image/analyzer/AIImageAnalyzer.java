package top.yumbo.ai.rag.image.analyzer;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.image.extractor.ExtractedImage;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.util.Base64;
import java.util.List;

/**
 * AI 图片分析服务
 * 使用 LLM 对图片进行语义分析
 *
 * 核心功能：
 * 1. 识别图片类型（架构图、流程图、数据图、截图等）
 * 2. 生成图片的文本描述
 * 3. 提取图片中的关键信息
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
     * 分析图片并生成描述
     *
     * @param image 提取的图片
     * @return 更新后的图片（包含 AI 分析结果）
     */
    public ExtractedImage analyzeImage(ExtractedImage image) {
        if (!enabled || llmClient == null) {
            log.debug("AI image analysis is disabled");
            return image;
        }

        try {
            log.info("🤖 Analyzing image with AI: {}", image.getOriginalName());

            // 构建分析 Prompt
            String prompt = buildAnalysisPrompt(image);

            // 调用 LLM（注意：这里需要支持图片输入的 LLM，如 GPT-4 Vision）
            // 如果 LLM 不支持图片，则使用上下文文本进行分析
            String analysis = analyzeWithLLM(prompt, image);

            // 解析分析结果
            parseAnalysisResult(image, analysis);

            log.info("✅ Image analysis completed: type={}, description={}",
                    image.getImageType(),
                    image.getAiDescription() != null ? image.getAiDescription().substring(0, Math.min(50, image.getAiDescription().length())) : "N/A");

            return image;

        } catch (Exception e) {
            log.error("Failed to analyze image: {}", image.getOriginalName(), e);
            // 返回原图片，不中断流程
            return image;
        }
    }

    /**
     * 批量分析图片
     */
    public List<ExtractedImage> analyzeImages(List<ExtractedImage> images) {
        if (!enabled) {
            return images;
        }

        log.info("🤖 Starting AI analysis for {} images", images.size());

        for (ExtractedImage image : images) {
            analyzeImage(image);
        }

        return images;
    }

    /**
     * 构建分析 Prompt
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

        // 添加上下文信息
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
     * 使用 LLM 分析
     * 注意：如果 LLM 支持 Vision API，应该传递图片数据
     * 目前实现：仅使用上下文文本进行分析
     */
    private String analyzeWithLLM(String prompt, ExtractedImage image) {
        try {
            // TODO: 如果 LLM 支持 Vision API（如 GPT-4 Vision），可以传递图片
            // 当前实现：仅使用文本分析

            String response = llmClient.generate(prompt);
            return response;

        } catch (Exception e) {
            log.error("LLM analysis failed", e);
            throw e;
        }
    }

    /**
     * 解析分析结果
     */
    private void parseAnalysisResult(ExtractedImage image, String analysis) {
        try {
            // 简单的 JSON 解析（实际应使用 Jackson）

            // 提取 type
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

            // 提取 description
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

            // 如果解析失败，使用整个响应作为描述
            if (image.getAiDescription() == null || image.getAiDescription().isEmpty()) {
                // 去除 JSON 标记
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
            log.warn("Failed to parse analysis result, using raw response", e);

            // 使用原始响应（截断）
            String desc = analysis.length() > 200 ? analysis.substring(0, 200) : analysis;
            image.setAiDescription(desc);
        }
    }

    /**
     * 简化版分析（仅基于上下文文本，不调用 LLM）
     * 用于降级场景
     */
    public ExtractedImage simpleAnalyze(ExtractedImage image) {
        try {
            // 基于上下文文本进行简单的关键词匹配
            String context = image.getContextText();
            if (context == null) context = "";

            String contextLower = context.toLowerCase();

            // 简单的类型判断
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

            // 简单的描述生成
            String desc = "位于文档第 " + image.getPosition() + " 页/幻灯片";
            if (context.length() > 0) {
                desc += "，相关内容：" + context.substring(0, Math.min(100, context.length()));
            }
            image.setAiDescription(desc);

        } catch (Exception e) {
            log.warn("Simple analysis failed", e);
        }

        return image;
    }
}

