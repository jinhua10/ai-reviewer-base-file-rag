package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.spring.boot.strategy.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 智能多文档分析服务（Smart Multi-Document Analysis Service）
 *
 * <p>提供智能的多文档联合分析功能</p>
 * <p>Provides smart multi-document joint analysis capabilities</p>
 *
 * <p>优先使用索引阶段已经生成的文本化内容（chunks），避免重复解析原始文件</p>
 * <p>Prefer using text content generated during indexing (chunks), avoid re-parsing original files</p>
 */
@Service
@Slf4j
public class SmartAnalysisService {

    @Autowired
    private StrategyDispatcher strategyDispatcher;

    @Autowired(required = false)
    private DocumentParserService documentParserService;

    @Autowired(required = false)
    private ChunkStorageService chunkStorageService;

    @Autowired(required = false)
    private SlideContentCacheService slideContentCacheService;

    @Value("${file.upload.path:./data/documents}")
    private String documentBasePath;

    @Value("${knowledge.qa.documents-path:./data/knowledge-base}")
    private String knowledgeBasePath;

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
     * 加载文档内容（Load document contents）
     *
     * <p>优先从已索引的 chunks 中加载内容（已经过 Vision LLM 文本化处理）</p>
     * <p>对于 PPT 文件，优先从 SlideContentCache 加载（包含 Vision LLM 处理结果）</p>
     * <p>Prefer loading from indexed chunks (already processed by Vision LLM for text conversion)</p>
     * <p>For PPT files, prefer loading from SlideContentCache (containing Vision LLM results)</p>
     */
    private List<AnalysisContext.DocumentContent> loadDocumentContents(List<String> documentPaths) {
        List<AnalysisContext.DocumentContent> contents = new ArrayList<>();

        for (String docPath : documentPaths) {
            try {
                Path fullPath = resolveDocumentPath(docPath);
                String fileName = fullPath.getFileName().toString();

                if (!Files.exists(fullPath)) {
                    log.warn("Document not found: {}", fullPath);
                    continue;
                }

                String content = null;
                boolean fromCache = false;

                // 1. 对于 PPT 文件，优先从 SlideContentCache 加载（包含 Vision LLM 处理结果）
                //    For PPT files, first try loading from SlideContentCache (containing Vision LLM results)
                String fileNameLower = fileName.toLowerCase();
                if (fileNameLower.endsWith(".pptx") || fileNameLower.endsWith(".ppt")) {
                    content = loadFromSlideCache(fullPath.toAbsolutePath().toString());
                    if (content != null && !content.trim().isEmpty()) {
                        fromCache = true;
                        log.info(top.yumbo.ai.rag.i18n.LogMessageProvider.getMessage(
                                "smart_analysis.log.loaded_from_slide_cache", fileName, content.length()));
                    }
                }

                // 2. 如果没有从缓存加载到，尝试从 chunks 加载已文本化的内容
                if (content == null || content.trim().isEmpty()) {
                    content = loadFromChunks(fileName);
                }

                // 3. 如果没有 chunks，尝试使用文档解析服务
                if (content == null || content.trim().isEmpty()) {
                    log.debug("No chunks found for {}, trying document parser", fileName);
                    content = parseDocument(fullPath);
                }

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
                        .metadata(Map.of(
                                "originalPath", docPath,
                                "fromChunks", content != null && content.contains("文档块"),
                                "fromSlideCache", fromCache))
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
     * 从 SlideContentCache 加载 PPT 内容（Load PPT content from SlideContentCache）
     *
     * <p>SlideContentCache 存储了 Vision LLM 对幻灯片图片的处理结果</p>
     * <p>SlideContentCache stores Vision LLM processing results for slide images</p>
     *
     * @param pptPath PPT 文件的绝对路径（Absolute path of PPT file）
     * @return 组合后的 PPT 内容，包含文字和图片 Vision LLM 分析结果（Combined PPT content with text and Vision LLM image analysis）
     */
    private String loadFromSlideCache(String pptPath) {
        if (slideContentCacheService == null) {
            return null;
        }

        try {
            SlideContentCacheService.PPTCache pptCache = slideContentCacheService.getPPTCache(pptPath);
            if (pptCache == null || pptCache.getSlides() == null || pptCache.getSlides().isEmpty()) {
                log.debug(top.yumbo.ai.rag.i18n.LogMessageProvider.getMessage(
                        "smart_analysis.log.no_slide_cache", pptPath));
                return null;
            }

            StringBuilder content = new StringBuilder();
            Map<Integer, SlideContentCacheService.SlideCache> slides = pptCache.getSlides();

            // 按幻灯片序号排序
            List<Integer> slideNumbers = new ArrayList<>(slides.keySet());
            Collections.sort(slideNumbers);

            for (Integer slideNum : slideNumbers) {
                SlideContentCacheService.SlideCache slideCache = slides.get(slideNum);
                if (slideCache == null) continue;

                // 添加幻灯片标题
                content.append("\n\n========== 幻灯片 ").append(slideNum).append(" ==========\n");

                // 添加幻灯片文字内容
                if (slideCache.getSlideText() != null && !slideCache.getSlideText().trim().isEmpty()) {
                    content.append("【文字内容】\n").append(slideCache.getSlideText()).append("\n");
                }

                // 添加 Vision LLM 分析结果（图片文本化内容）
                if (slideCache.getVisionLLMResult() != null && !slideCache.getVisionLLMResult().trim().isEmpty()) {
                    content.append("\n【图片分析】\n").append(slideCache.getVisionLLMResult()).append("\n");
                }
            }

            return content.toString();

        } catch (Exception e) {
            log.warn(top.yumbo.ai.rag.i18n.LogMessageProvider.getMessage(
                    "smart_analysis.log.load_slide_cache_failed", pptPath), e);
            return null;
        }
    }

    /**
     * 从已索引的 chunks 加载文档内容（Load document content from indexed chunks）
     *
     * <p>chunks 已经过 Vision LLM 处理，包含图片的文本化内容</p>
     * <p>Chunks have been processed by Vision LLM, containing text conversion of images</p>
     *
     * @param fileName 文件名（File name）
     * @return 合并后的 chunks 内容，如果没有 chunks 则返回 null
     */
    private String loadFromChunks(String fileName) {
        try {
            // 构建 chunks 目录路径
            Path chunksDir = Paths.get(knowledgeBasePath, "chunks", fileName);

            if (!Files.exists(chunksDir) || !Files.isDirectory(chunksDir)) {
                log.debug("Chunks directory not found: {}", chunksDir);
                return null;
            }

            // 读取所有 .md 文件并按文件名排序
            List<Path> chunkFiles = Files.list(chunksDir)
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();

            if (chunkFiles.isEmpty()) {
                log.debug("No chunk files found in: {}", chunksDir);
                return null;
            }

            // 合并所有 chunks 内容
            StringBuilder content = new StringBuilder();
            for (Path chunkFile : chunkFiles) {
                String chunkContent = Files.readString(chunkFile, StandardCharsets.UTF_8);
                if (content.length() > 0) {
                    content.append("\n\n---\n\n");
                }
                content.append(chunkContent);
            }

            log.info("📦 Loaded {} chunks for document: {} ({} chars)",
                    chunkFiles.size(), fileName, content.length());

            return content.toString();

        } catch (Exception e) {
            log.warn("Failed to load chunks for {}: {}", fileName, e.getMessage());
            return null;
        }
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

