package top.yumbo.ai.rag.spring.boot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询扩展服务
 *
 * 通过同义词扩展和 LLM 改写提升检索召回率
 *
 * 📈 优化说明（2025-12-05）：
 * 根据 RAG 收敛性分析，查询扩展可减少 2-3 次反馈交互
 * 详见: md/20251205140000-RAG系统收敛性分析.md
 *
 * 📈 优化说明（2025-12-07）：
 * 支持从配置文件和外部文件加载同义词和停用词
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Service
public class QueryExpansionService {

    private final LLMClient llmClient;
    private final KnowledgeQAProperties properties;

    /** 同义词词典（支持从配置加载）(Synonym dictionary) */
    private final Map<String, List<String>> synonymDict = new HashMap<>();

    /** 同义词反向索引，优化查找效率 (Reverse index for efficient lookup) */
    private final Map<String, String> synonymReverseIndex = new HashMap<>();

    /** 停用词（从配置加载）(Stopwords from config) */
    private final Set<String> stopWords = new HashSet<>();

    /** 分词正则 (Token pattern) */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\s,.;:?!]+");

    /** 短语匹配正则 (Phrase pattern) */
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");

    @Autowired
    public QueryExpansionService(@Autowired(required = false) LLMClient llmClient,
                                  KnowledgeQAProperties properties) {
        this.llmClient = llmClient;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // 1. 加载内置同义词 (Load builtin synonyms)
        initBuiltinSynonyms();

        // 2. 从配置加载停用词 (Load stopwords from config)
        initStopWordsFromConfig();

        // 3. 加载外部同义词文件（如果配置了）(Load external synonym file if configured)
        loadSynonymsFromFile();

        // 4. 构建反向索引 (Build reverse index)
        buildReverseIndex();

        log.info(I18N.get("log.query_expansion.init", synonymDict.size(), stopWords.size()));
    }

    /**
     * 构建同义词反向索引
     * (Build synonym reverse index for O(1) lookup)
     */
    private void buildReverseIndex() {
        synonymReverseIndex.clear();
        for (Map.Entry<String, List<String>> entry : synonymDict.entrySet()) {
            String mainWord = entry.getKey();
            for (String synonym : entry.getValue()) {
                synonymReverseIndex.put(synonym.toLowerCase(), mainWord);
            }
        }
        log.debug(I18N.get("log.query_expansion.reverse_index", synonymReverseIndex.size()));
    }

    /**
     * 扩展查询（完整版）
     * (Expand query - full version)
     *
     * @param originalQuery 原始查询 (Original query)
     * @param useLLM 是否使用 LLM 改写 (Whether to use LLM rewrite)
     * @return 扩展后的查询 (Expanded query)
     */
    public ExpandedQuery expandQuery(String originalQuery, boolean useLLM) {
        log.debug(I18N.get("log.query_expansion.start", originalQuery));

        ExpandedQuery result = new ExpandedQuery();
        result.setOriginalQuery(originalQuery);

        // 1. 同义词扩展
        Set<String> expandedTerms = synonymExpand(originalQuery);
        result.setExpandedTerms(new ArrayList<>(expandedTerms));

        // 2. 关键词提取
        List<String> keywords = extractKeywords(originalQuery);
        result.setKeywords(keywords);

        // 3. LLM 改写（可选）
        if (useLLM && llmClient != null) {
            try {
                String rewrittenQuery = llmRewriteQuery(originalQuery);
                result.setRewrittenQuery(rewrittenQuery);
            } catch (Exception e) {
                log.warn("⚠️ LLM 查询改写失败: {}", e.getMessage());
                result.setRewrittenQuery(originalQuery);
            }
        } else {
            result.setRewrittenQuery(originalQuery);
        }

        // 4. 生成最终扩展查询
        String finalQuery = buildExpandedQueryString(result);
        result.setFinalQuery(finalQuery);

        log.debug("✅ 查询扩展完成: {} -> {}", originalQuery, finalQuery);
        return result;
    }

    /**
     * 简单同义词扩展（快速版）
     */
    public String simpleExpand(String query) {
        Set<String> expanded = synonymExpand(query);
        if (expanded.isEmpty()) {
            return query;
        }
        return query + " " + String.join(" ", expanded);
    }

    /**
     * 同义词扩展
     * (Synonym expansion with O(1) reverse lookup)
     */
    private Set<String> synonymExpand(String query) {
        Set<String> expandedTerms = new LinkedHashSet<>();

        // 分词 (Tokenize)
        String[] tokens = TOKEN_PATTERN.split(query);

        for (String token : tokens) {
            if (token.length() < 2 || stopWords.contains(token.toLowerCase())) {
                continue;
            }

            String lowerToken = token.toLowerCase();

            // 正向查找：当前词是主词 (Forward lookup: current word is main word)
            List<String> synonyms = synonymDict.get(lowerToken);
            if (synonyms != null) {
                expandedTerms.addAll(synonyms);
            }

            // 反向查找：使用反向索引 O(1) (Reverse lookup: use reverse index O(1))
            String mainWord = synonymReverseIndex.get(lowerToken);
            if (mainWord != null) {
                expandedTerms.add(mainWord);
                List<String> relatedSynonyms = synonymDict.get(mainWord);
                if (relatedSynonyms != null) {
                    expandedTerms.addAll(relatedSynonyms);
                }
            }
        }

        // 移除原始查询中已有的词 (Remove words already in original query)
        for (String token : tokens) {
            expandedTerms.remove(token.toLowerCase());
        }

        return expandedTerms;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();

        // 分词
        String[] tokens = TOKEN_PATTERN.split(query);

        for (String token : tokens) {
            // 过滤停用词和短词（使用配置的停用词）
            if (token.length() >= 2 && !stopWords.contains(token.toLowerCase())) {
                keywords.add(token);
            }
        }

        // 提取引号内的短语
        Matcher matcher = PHRASE_PATTERN.matcher(query);
        while (matcher.find()) {
            keywords.add(matcher.group(1));
        }

        return keywords;
    }

    /**
     * 使用 LLM 改写查询（使用配置的 Prompt 模板）
     */
    private String llmRewriteQuery(String originalQuery) {
        // 使用配置的 Prompt 模板
        String promptTemplate = properties.getQueryExpansion().getLlmRewritePrompt();
        String prompt = promptTemplate.replace("{query}", originalQuery);

        String response = llmClient.generate(prompt);

        // 清理响应（移除可能的前缀）
        if (response != null) {
            response = response.trim();
            // 移除可能的"改写后的查询："前缀
            if (response.startsWith("改写后的查询：")) {
                response = response.substring("改写后的查询：".length()).trim();
            }
        }

        return response != null ? response : originalQuery;
    }

    /**
     * 构建最终扩展查询字符串
     */
    private String buildExpandedQueryString(ExpandedQuery result) {
        StringBuilder sb = new StringBuilder();

        // 优先使用 LLM 改写的查询
        if (result.getRewrittenQuery() != null &&
            !result.getRewrittenQuery().equals(result.getOriginalQuery())) {
            sb.append(result.getRewrittenQuery());
        } else {
            sb.append(result.getOriginalQuery());
        }

        // 添加同义词扩展
        if (!result.getExpandedTerms().isEmpty()) {
            sb.append(" ");
            sb.append(String.join(" ", result.getExpandedTerms()));
        }

        return sb.toString();
    }

    /**
     * 初始化内置同义词词典
     */
    private void initBuiltinSynonyms() {
        // 技术领域同义词
        synonymDict.put("数据库", Arrays.asList("DB", "database", "存储", "数据存储"));
        synonymDict.put("接口", Arrays.asList("API", "interface", "端点", "endpoint"));
        synonymDict.put("服务器", Arrays.asList("server", "服务端", "后端", "backend"));
        synonymDict.put("客户端", Arrays.asList("client", "前端", "frontend", "用户端"));
        synonymDict.put("配置", Arrays.asList("config", "configuration", "设置", "参数"));
        synonymDict.put("文档", Arrays.asList("document", "doc", "文件", "资料"));
        synonymDict.put("错误", Arrays.asList("error", "异常", "exception", "bug", "问题"));
        synonymDict.put("性能", Arrays.asList("performance", "效率", "速度", "优化"));
        synonymDict.put("安全", Arrays.asList("security", "安全性", "加密", "权限"));
        synonymDict.put("部署", Arrays.asList("deploy", "deployment", "发布", "上线"));

        // 业务领域同义词
        synonymDict.put("用户", Arrays.asList("user", "客户", "会员", "账户"));
        synonymDict.put("订单", Arrays.asList("order", "交易", "购买记录"));
        synonymDict.put("支付", Arrays.asList("pay", "payment", "付款", "结算"));
        synonymDict.put("报表", Arrays.asList("report", "统计", "分析", "报告"));

        // 通用同义词
        synonymDict.put("如何", Arrays.asList("怎么", "怎样", "方法", "步骤", "how"));
        synonymDict.put("什么", Arrays.asList("哪些", "which", "what"));
        synonymDict.put("为什么", Arrays.asList("原因", "why", "理由"));
        synonymDict.put("创建", Arrays.asList("新建", "添加", "create", "add"));
        synonymDict.put("删除", Arrays.asList("移除", "清除", "delete", "remove"));
        synonymDict.put("修改", Arrays.asList("更新", "编辑", "update", "edit", "change"));
        synonymDict.put("查询", Arrays.asList("搜索", "检索", "查找", "search", "query", "find"));
    }

    /**
     * 从配置加载停用词
     */
    private void initStopWordsFromConfig() {
        KnowledgeQAProperties.SearchConfig searchConfig = properties.getSearch();
        if (searchConfig.getChineseStopWords() != null) {
            stopWords.addAll(searchConfig.getChineseStopWords());
        }
        if (searchConfig.getEnglishStopWords() != null) {
            searchConfig.getEnglishStopWords().forEach(w -> stopWords.add(w.toLowerCase()));
        }
    }

    /**
     * 从外部文件加载同义词
     * 格式: 每行一组同义词，用逗号分隔
     * 例如: 数据库,DB,database,存储
     */
    private void loadSynonymsFromFile() {
        String synonymFile = properties.getQueryExpansion().getSynonymFile();
        if (synonymFile == null || synonymFile.isBlank()) {
            return;
        }

        Path filePath = Path.of(synonymFile);
        if (!Files.exists(filePath)) {
            log.warn("Synonym file not found: {}", synonymFile);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // 跳过空行和注释
                }
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String key = parts[0].trim().toLowerCase();
                    List<String> synonyms = Arrays.stream(parts)
                        .skip(1)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                    synonymDict.put(key, new ArrayList<>(synonyms));
                    count++;
                }
            }
            log.info("Loaded {} synonyms from file: {}", count, synonymFile);
        } catch (IOException e) {
            log.error("Failed to load synonyms from file: {}", synonymFile, e);
        }
    }

    /**
     * 扩展查询结果
     */
    @lombok.Data
    public static class ExpandedQuery {
        /** 原始查询 */
        private String originalQuery;
        /** 同义词扩展词 */
        private List<String> expandedTerms;
        /** 提取的关键词 */
        private List<String> keywords;
        /** LLM 改写后的查询 */
        private String rewrittenQuery;
        /** 最终扩展查询 */
        private String finalQuery;
    }
}

