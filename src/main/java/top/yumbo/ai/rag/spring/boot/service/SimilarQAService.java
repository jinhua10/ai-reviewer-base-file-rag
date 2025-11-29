package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.impl.embedding.LocalEmbeddingEngine;
import top.yumbo.ai.rag.impl.index.SimpleVectorIndexEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 相似问题检测服务
 * 检索历史问答，避免重复调用AI
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
@Slf4j
@Service
public class SimilarQAService {

    private LocalEmbeddingEngine embeddingEngine;
    private SimpleVectorIndexEngine vectorIndexEngine;
    private final ObjectMapper objectMapper;
    private final Path archivePath;

    public SimilarQAService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.archivePath = Paths.get("./data/rag");

        // 延迟初始化，避免启动时就加载模型
        try {
            this.embeddingEngine = new LocalEmbeddingEngine();
            this.vectorIndexEngine = new SimpleVectorIndexEngine(
                    "./data/vector-index",
                    embeddingEngine.getEmbeddingDim()
            );
            log.info("✅ 相似问题检测服务初始化完成");
        } catch (Exception e) {
            log.warn("⚠️ 向量检索引擎初始化失败，相似问题检测将不可用", e);
        }
    }

    /**
     * 搜索相似的历史问答
     *
     * @param question  用户问题
     * @param threshold 相似度阈值 (0.0-1.0)
     * @param limit     返回数量限制
     * @return 相似问答列表
     */
    public List<SimilarQA> findSimilar(String question, float threshold, int limit) {
        if (embeddingEngine == null || vectorIndexEngine == null) {
            log.warn("⚠️ 向量检索引擎未初始化，无法搜索相似问题");
            return List.of();
        }

        try {
            log.info("🔍 搜索相似问题: question='{}', threshold={}, limit={}", question, threshold, limit);

            // 1. 生成问题向量
            float[] queryVector = embeddingEngine.embed(question);

            // 2. 向量检索
            List<SimpleVectorIndexEngine.VectorSearchResult> results = vectorIndexEngine.search(queryVector, limit * 2, threshold);

            // 3. 过滤和转换为 SimilarQA 对象
            List<SimilarQA> similarQAs = new ArrayList<>();
            for (VectorSearchResult result : results) {
                try {
                    SimilarQA qa = loadSimilarQA(result);
                    if (qa != null && qa.getRating() >= 4) {  // 只返回高质量问答
                        similarQAs.add(qa);
                        if (similarQAs.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("⚠️ 加载相似问答失败: {}", result.getDocumentId(), e);
                }
            }

            log.info("✅ 找到 {} 个相似问答", similarQAs.size());
            return similarQAs;

        } catch (Exception e) {
            log.error("❌ 搜索相似问题失败", e);
            return List.of();
        }
    }

    /**
     * 从向量检索结果加载完整的问答信息
     */
    private SimilarQA loadSimilarQA(SimpleVectorIndexEngine.VectorSearchResult result) {
        try {
            // 从文档ID推断文件路径
            String documentId = result.getDocumentId();
            Path qaFilePath = findQAFile(documentId);

            if (qaFilePath == null || !Files.exists(qaFilePath)) {
                log.debug("问答文件不存在: {}", documentId);
                return null;
            }

            // 读取文件内容
            String content = Files.readString(qaFilePath);

            // 解析 YAML Front Matter 和 Markdown 内容
            return parseQADocument(content, result.getSimilarity());

        } catch (Exception e) {
            log.error("加载问答文件失败: {}", result.getDocumentId(), e);
            return null;
        }
    }

    /**
     * 查找问答文件
     */
    private Path findQAFile(String documentId) {
        try {
            // 在 approved 和 temp 目录中搜索
            List<Path> searchPaths = List.of(
                    archivePath.resolve("approved"),
                    archivePath.resolve("temp")
            );

            for (Path searchPath : searchPaths) {
                if (!Files.exists(searchPath)) {
                    continue;
                }

                List<Path> found = Files.walk(searchPath)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .filter(p -> p.getFileName().toString().contains(documentId.substring(0, Math.min(8, documentId.length()))))
                        .collect(Collectors.toList());

                if (!found.isEmpty()) {
                    return found.get(0);
                }
            }

            return null;
        } catch (IOException e) {
            log.error("搜索问答文件失败", e);
            return null;
        }
    }

    /**
     * 解析问答文档
     */
    private SimilarQA parseQADocument(String content, float similarity) {
        SimilarQA qa = new SimilarQA();
        qa.setSimilarity(similarity);

        // 解析 YAML Front Matter
        Pattern yamlPattern = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
        Matcher yamlMatcher = yamlPattern.matcher(content);

        if (yamlMatcher.find()) {
            String yaml = yamlMatcher.group(1);

            // 提取字段
            qa.setId(extractYamlField(yaml, "id"));
            qa.setQuestion(extractYamlField(yaml, "question"));
            qa.setTimestamp(extractYamlField(yaml, "timestamp"));
            qa.setRating(extractYamlInt(yaml, "rating", 0));
            qa.setCategory(extractYamlField(yaml, "category"));
            qa.setSourceDocuments(extractYamlList(yaml, "sourceDocuments"));
            qa.setUsageCount(extractYamlInt(yaml, "usageCount", 0));

            // 提取回答内容
            String remainingContent = content.substring(yamlMatcher.end());
            String answer = extractAnswer(remainingContent);
            qa.setAnswer(answer);
        }

        return qa;
    }

    /**
     * 提取YAML字段
     */
    private String extractYamlField(String yaml, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(yaml);
        return matcher.find() ? matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"") : null;
    }

    /**
     * 提取YAML整数字段
     */
    private int extractYamlInt(String yaml, String fieldName, int defaultValue) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(\\d+)");
        Matcher matcher = pattern.matcher(yaml);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 提取YAML列表字段
     */
    private List<String> extractYamlList(String yaml, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(yaml);
        if (matcher.find()) {
            String listContent = matcher.group(1);
            return java.util.Arrays.stream(listContent.split(","))
                    .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 提取回答内容
     */
    private String extractAnswer(String content) {
        // 提取 "## 回答" 部分
        Pattern answerPattern = Pattern.compile("##\\s*回答\\s*\\n\\n(.*?)(?=\\n##|$)", Pattern.DOTALL);
        Matcher matcher = answerPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果没有找到，返回整个内容（去除标题）
        String[] lines = content.split("\n");
        StringBuilder answer = new StringBuilder();
        boolean startCollecting = false;
        for (String line : lines) {
            if (line.startsWith("#")) {
                startCollecting = true;
                continue;
            }
            if (startCollecting && !line.trim().isEmpty()) {
                answer.append(line).append("\n");
            }
        }
        return answer.toString().trim();
    }

    /**
     * 相似问答数据结构
     */
    @Data
    public static class SimilarQA {
        private String id;
        private String question;
        private String answer;
        private int rating;
        private float similarity;
        private String timestamp;
        private String category;
        private List<String> sourceDocuments = new ArrayList<>();
        private int usageCount;

        /**
         * 获取类别显示名称
         */
        public String getCategoryDisplay() {
            switch (category != null ? category : "") {
                case "concept":
                    return "概念解释";
                case "howto":
                    return "操作指南";
                case "troubleshooting":
                    return "问题排查";
                default:
                    return "其他";
            }
        }

        /**
         * 获取格式化的时间
         */
        public String getFormattedTime() {
            if (timestamp == null) {
                return "";
            }
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp);
                return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception e) {
                return timestamp;
            }
        }
    }
}

