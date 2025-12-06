package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 智能多文档分析服务
 * (Smart Multi-Document Analysis Service)
 *
 * 提供智能的多文档联合分析功能
 */
@Service
@Slf4j
public class SmartAnalysisService {

    @Autowired
    private StrategyDispatcher strategyDispatcher;

    @Autowired
    private DocumentParserService documentParserService;

    @Value("${file.upload.path:./data/documents}")
    private String documentBasePath;

    @Value("${analysis.max.content.length:100000}")
    private int maxContentLength;

    /**
     * 执行智能分析
     * (Execute smart analysis)
     *
     * @param request 分析请求
     * @return 分析结果
     */
    public AnalysisResult analyzeSmartly(SmartAnalysisRequest request) {
        log.info("📊 Starting smart analysis for {} documents, goal: {}",
                request.getDocumentPaths().size(), request.getGoalId());

        try {
            // 1. 加载文档内容
            List<AnalysisContext.DocumentContent> documentContents =
                    loadDocumentContents(request.getDocumentPaths());

            if (documentContents.isEmpty()) {
                return AnalysisResult.failure("无法加载任何文档内容");
            }

            // 2. 构建分析上下文
            AnalysisContext context = AnalysisContext.builder()
                    .documentPaths(request.getDocumentPaths())
                    .documentContents(documentContents)
                    .question(request.getQuestion())
                    .goalId(request.getGoalId())
                    .strategies(request.getStrategies())
                    .advancedParams(request.getAdvancedParams())
                    .language(request.getLanguage())
                    .maxTokens(request.getMaxTokens() > 0 ? request.getMaxTokens() : 4000)
                    .useKnowledgeBase(request.isUseKnowledgeBase())
                    .build();

            // 3. 执行策略调度分析
            ProgressCallback callback = (progress, message) -> {
                log.debug("Progress: {}% - {}", progress, message);
            };

            AnalysisResult result;
            if (request.getStrategies() != null && !request.getStrategies().isEmpty()) {
                // 使用指定的策略
                result = strategyDispatcher.analyzeWithStrategies(
                        context, request.getStrategies(), callback);
            } else {
                // 智能选择策略
                result = strategyDispatcher.analyze(context, callback);
            }

            log.info("✅ Smart analysis completed. Success: {}, Time: {}ms",
                    result.isSuccess(), result.getExecutionTimeMs());

            return result;

        } catch (Exception e) {
            log.error("❌ Smart analysis failed", e);
            return AnalysisResult.failure("分析失败: " + e.getMessage());
        }
    }

    /**
     * 加载文档内容
     * (Load document contents)
     */
    private List<AnalysisContext.DocumentContent> loadDocumentContents(List<String> documentPaths) {
        List<AnalysisContext.DocumentContent> contents = new ArrayList<>();

        for (String docPath : documentPaths) {
            try {
                Path fullPath = resolveDocumentPath(docPath);

                if (!Files.exists(fullPath)) {
                    log.warn("Document not found: {}", fullPath);
                    continue;
                }

                String content = parseDocument(fullPath);
                String fileName = fullPath.getFileName().toString();
                String fileType = getFileType(fileName);

                // 限制内容长度
                if (content != null && content.length() > maxContentLength) {
                    log.warn("Document {} content truncated from {} to {} chars",
                            fileName, content.length(), maxContentLength);
                    content = content.substring(0, maxContentLength) + "\n...(内容已截断)";
                }

                contents.add(AnalysisContext.DocumentContent.builder()
                        .path(fullPath.toString())
                        .name(fileName)
                        .content(content)
                        .type(fileType)
                        .size(Files.size(fullPath))
                        .metadata(Map.of("originalPath", docPath))
                        .build());

                log.debug("Loaded document: {} ({} chars)", fileName,
                        content != null ? content.length() : 0);

            } catch (Exception e) {
                log.error("Failed to load document: {}", docPath, e);
            }
        }

        return contents;
    }

    /**
     * 解析文档路径
     */
    private Path resolveDocumentPath(String docPath) {
        Path path = Paths.get(docPath);

        // 如果是绝对路径且存在，直接返回
        if (path.isAbsolute() && Files.exists(path)) {
            return path;
        }

        // 否则相对于文档基础路径
        return Paths.get(documentBasePath, docPath);
    }

    /**
     * 解析文档内容
     */
    private String parseDocument(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        // 使用文档解析服务
        if (documentParserService != null) {
            try {
                return documentParserService.parseDocument(path.toString());
            } catch (Exception e) {
                log.warn("Document parser failed for {}, falling back to text read", fileName);
            }
        }

        // 尝试作为文本读取
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "无法解析文档内容: " + e.getMessage();
        }
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * 获取可用策略列表
     */
    public List<Map<String, Object>> getAvailableStrategies() {
        return strategyDispatcher.getAvailableStrategies();
    }

    /**
     * 获取策略统计
     */
    public Map<String, StrategyDispatcher.StrategyStats> getStrategyStats() {
        return strategyDispatcher.getStrategyStats();
    }

    /**
     * 智能分析请求
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SmartAnalysisRequest {
        private List<String> documentPaths;
        private String question;
        private String goalId;
        private List<String> strategies;
        private Map<String, Object> advancedParams;
        private String language;
        private int maxTokens;
        private boolean useKnowledgeBase;
    }
}

