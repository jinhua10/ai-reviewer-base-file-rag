package top.yumbo.ai.rag.spring.boot.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.service.document.LLMResultDocumentService.LLMAnalysisResult;
import top.yumbo.ai.rag.spring.boot.service.document.LLMResultDocumentService.LLMResultDocument;
import top.yumbo.ai.rag.spring.boot.service.document.LLMResultDocumentService.ImageInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 结果收集器
 *
 * 自动收集 LLM 调用结果，支持：
 * - 单次问答结果收集
 * - 文档分析结果收集
 * - 图片分析结果收集
 * - 批量结果聚合
 */
@Slf4j
@Service
public class LLMResultCollector {

    private final LLMResultDocumentService documentService;

    @Value("${knowledge.qa.llm-result.auto-save:true}")
    private boolean autoSave;

    @Value("${knowledge.qa.llm-result.min-content-length:100}")
    private int minContentLength;

    /** 会话级别的结果缓存 */
    private final Map<String, List<CollectedResult>> sessionResults = new ConcurrentHashMap<>();

    /** KEY_POINTS 提取正则 */
    private static final Pattern KEY_POINTS_PATTERN = Pattern.compile(
            "(?:KEY_POINTS|核心要点|关键要点)[\\s\\S]*?(?:\\n[-*]\\s*([^\\n]+))+",
            Pattern.MULTILINE
    );

    @Autowired
    public LLMResultCollector(LLMResultDocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 收集单次问答结果
     *
     * @param question 用户问题
     * @param answer LLM 回答
     * @param context 上下文信息
     * @return 是否保存成功
     */
    public boolean collectQAResult(String question, String answer, Map<String, Object> context) {
        if (!shouldCollect(answer)) {
            return false;
        }

        try {
            LLMAnalysisResult result = LLMAnalysisResult.builder()
                    .title(generateTitle(question))
                    .question(question)
                    .analysisType("问答")
                    .content(answer)
                    .keyPoints(extractKeyPoints(answer))
                    .metadata(context)
                    .build();

            if (autoSave) {
                documentService.saveResult(result);
                log.debug("✅ 问答结果已自动保存");
            }

            return true;

        } catch (Exception e) {
            log.error("收集问答结果失败", e);
            return false;
        }
    }

    /**
     * 收集文档分析结果
     */
    public boolean collectDocumentAnalysisResult(String documentName, String question,
                                                  String analysis, List<String> keyPoints) {
        if (!shouldCollect(analysis)) {
            return false;
        }

        try {
            LLMAnalysisResult result = LLMAnalysisResult.builder()
                    .title("文档分析: " + documentName)
                    .sourceDocument(documentName)
                    .question(question)
                    .analysisType("文档分析")
                    .content(analysis)
                    .keyPoints(keyPoints != null ? keyPoints : extractKeyPoints(analysis))
                    .build();

            if (autoSave) {
                documentService.saveResult(result);
                log.debug("✅ 文档分析结果已自动保存");
            }

            return true;

        } catch (Exception e) {
            log.error("收集文档分析结果失败", e);
            return false;
        }
    }

    /**
     * 收集图片分析结果
     */
    public boolean collectImageAnalysisResult(String imageUrl, String description,
                                               String sourceDocument) {
        try {
            ImageInfo imageInfo = ImageInfo.builder()
                    .url(imageUrl)
                    .description(description)
                    .build();

            List<ImageInfo> images = new ArrayList<>();
            images.add(imageInfo);

            LLMAnalysisResult result = LLMAnalysisResult.builder()
                    .title("图片分析")
                    .sourceDocument(sourceDocument)
                    .analysisType("图片分析")
                    .content(description)
                    .images(images)
                    .build();

            if (autoSave) {
                documentService.saveResult(result);
                log.debug("✅ 图片分析结果已自动保存");
            }

            return true;

        } catch (Exception e) {
            log.error("收集图片分析结果失败", e);
            return false;
        }
    }

    /**
     * 开始会话收集（用于多轮对话或渐进式分析）
     */
    public String startSession(String sessionName) {
        String sessionId = "session-" + System.currentTimeMillis();
        sessionResults.put(sessionId, new ArrayList<>());
        log.debug("📝 开始会话收集: {} ({})", sessionName, sessionId);
        return sessionId;
    }

    /**
     * 添加结果到会话
     */
    public void addToSession(String sessionId, String segmentName, String content,
                             List<String> keyPoints) {
        List<CollectedResult> results = sessionResults.get(sessionId);
        if (results == null) {
            log.warn("会话不存在: {}", sessionId);
            return;
        }

        CollectedResult collected = new CollectedResult();
        collected.segmentName = segmentName;
        collected.content = content;
        collected.keyPoints = keyPoints;
        collected.timestamp = System.currentTimeMillis();

        results.add(collected);
        log.debug("➕ 添加到会话 {}: {}", sessionId, segmentName);
    }

    /**
     * 结束会话并保存所有结果
     */
    public LLMResultDocument endSessionAndSave(String sessionId, String title,
                                                String question, String finalSummary) {
        List<CollectedResult> results = sessionResults.remove(sessionId);
        if (results == null || results.isEmpty()) {
            log.warn("会话为空或不存在: {}", sessionId);
            return null;
        }

        try {
            // 合并所有结果
            StringBuilder combinedContent = new StringBuilder();
            List<String> allKeyPoints = new ArrayList<>();

            combinedContent.append("## 分析过程\n\n");

            for (int i = 0; i < results.size(); i++) {
                CollectedResult result = results.get(i);
                combinedContent.append("### ").append(i + 1).append(". ")
                               .append(result.segmentName).append("\n\n");
                combinedContent.append(result.content).append("\n\n");

                if (result.keyPoints != null) {
                    allKeyPoints.addAll(result.keyPoints);
                }
            }

            // 添加最终总结
            if (finalSummary != null && !finalSummary.isEmpty()) {
                combinedContent.append("---\n\n");
                combinedContent.append("## 综合总结\n\n");
                combinedContent.append(finalSummary).append("\n");
            }

            LLMAnalysisResult analysisResult = LLMAnalysisResult.builder()
                    .title(title)
                    .question(question)
                    .analysisType("渐进式分析")
                    .content(combinedContent.toString())
                    .keyPoints(allKeyPoints)
                    .build();

            LLMResultDocument document = documentService.saveResult(analysisResult);
            log.info("✅ 会话结果已保存: {} ({} 个片段)", sessionId, results.size());

            return document;

        } catch (Exception e) {
            log.error("保存会话结果失败: {}", sessionId, e);
            return null;
        }
    }

    /**
     * 取消会话
     */
    public void cancelSession(String sessionId) {
        sessionResults.remove(sessionId);
        log.debug("❌ 会话已取消: {}", sessionId);
    }

    /**
     * 获取会话进度
     */
    public int getSessionProgress(String sessionId) {
        List<CollectedResult> results = sessionResults.get(sessionId);
        return results != null ? results.size() : 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 判断是否应该收集该结果
     */
    private boolean shouldCollect(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // 内容太短不收集
        if (content.length() < minContentLength) {
            return false;
        }

        // 错误消息不收集
        if (content.startsWith("错误") || content.startsWith("Error") ||
            content.contains("处理问答时发生错误")) {
            return false;
        }

        return true;
    }

    /**
     * 生成标题
     */
    private String generateTitle(String question) {
        if (question == null || question.isEmpty()) {
            return "LLM 分析结果";
        }

        // 截取问题的前 50 个字符作为标题
        String title = question.length() > 50
                ? question.substring(0, 50) + "..."
                : question;

        return "问答: " + title;
    }

    /**
     * 从内容中提取关键点
     */
    private List<String> extractKeyPoints(String content) {
        List<String> keyPoints = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return keyPoints;
        }

        // 尝试匹配 KEY_POINTS 格式
        Matcher matcher = KEY_POINTS_PATTERN.matcher(content);
        if (matcher.find()) {
            String pointsSection = matcher.group();
            String[] lines = pointsSection.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("-") || line.startsWith("*")) {
                    String point = line.substring(1).trim();
                    if (!point.isEmpty()) {
                        keyPoints.add(point);
                    }
                }
            }
        }

        // 如果没有找到格式化的关键点，尝试提取第一层级的要点
        if (keyPoints.isEmpty()) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                // 匹配 "1. xxx" 或 "- xxx" 或 "* xxx" 格式
                if (line.matches("^\\d+\\.\\s+.+") ||
                    line.matches("^[-*]\\s+.+")) {
                    String point = line.replaceFirst("^(\\d+\\.\\s*|[-*]\\s*)", "").trim();
                    if (point.length() > 10 && point.length() < 200) {
                        keyPoints.add(point);
                    }
                }
            }
        }

        // 限制数量
        if (keyPoints.size() > 10) {
            keyPoints = keyPoints.subList(0, 10);
        }

        return keyPoints;
    }

    /**
     * 收集的结果
     */
    private static class CollectedResult {
        String segmentName;
        String content;
        List<String> keyPoints;
        long timestamp;
    }
}

