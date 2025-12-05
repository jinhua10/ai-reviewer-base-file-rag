package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

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
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Service
public class QueryExpansionService {

    private final LLMClient llmClient;

    /** 同义词词典 */
    private static final Map<String, List<String>> SYNONYM_DICT = new HashMap<>();

    /** 停用词 */
    private static final Set<String> STOP_WORDS = new HashSet<>();

    /** 分词正则 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\s,.;:?!]+");

    /** 短语匹配正则 */
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");

    static {
        // 初始化同义词词典（可从外部配置加载）
        initSynonyms();
        initStopWords();
    }

    @Autowired
    public QueryExpansionService(@Autowired(required = false) LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 扩展查询（完整版）
     *
     * @param originalQuery 原始查询
     * @param useLLM 是否使用 LLM 改写
     * @return 扩展后的查询
     */
    public ExpandedQuery expandQuery(String originalQuery, boolean useLLM) {
        log.debug("🔍 开始扩展查询: {}", originalQuery);

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
     */
    private Set<String> synonymExpand(String query) {
        Set<String> expandedTerms = new LinkedHashSet<>();

        // 分词
        String[] tokens = TOKEN_PATTERN.split(query);

        for (String token : tokens) {
            if (token.length() < 2 || STOP_WORDS.contains(token.toLowerCase())) {
                continue;
            }

            // 查找同义词
            List<String> synonyms = SYNONYM_DICT.get(token.toLowerCase());
            if (synonyms != null) {
                expandedTerms.addAll(synonyms);
            }

            // 反向查找（如果当前词是某个词的同义词）
            for (Map.Entry<String, List<String>> entry : SYNONYM_DICT.entrySet()) {
                if (entry.getValue().contains(token.toLowerCase())) {
                    expandedTerms.add(entry.getKey());
                    expandedTerms.addAll(entry.getValue());
                }
            }
        }

        // 移除原始查询中已有的词
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
            // 过滤停用词和短词
            if (token.length() >= 2 && !STOP_WORDS.contains(token.toLowerCase())) {
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
     * 使用 LLM 改写查询
     */
    private String llmRewriteQuery(String originalQuery) {
        String prompt = "请帮我改写以下搜索查询，使其更适合在知识库中检索相关文档。\n\n" +
            "要求：\n" +
            "1. 保持原意，但使用更通用、更专业的表述\n" +
            "2. 添加可能的同义词或相关概念\n" +
            "3. 如果查询太模糊，尝试明确化\n" +
            "4. 只返回改写后的查询，不要解释\n\n" +
            "原始查询：" + originalQuery + "\n\n" +
            "改写后的查询：";

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
     * 初始化同义词词典
     */
    private static void initSynonyms() {
        // 技术领域同义词
        SYNONYM_DICT.put("数据库", Arrays.asList("DB", "database", "存储", "数据存储"));
        SYNONYM_DICT.put("接口", Arrays.asList("API", "interface", "端点", "endpoint"));
        SYNONYM_DICT.put("服务器", Arrays.asList("server", "服务端", "后端", "backend"));
        SYNONYM_DICT.put("客户端", Arrays.asList("client", "前端", "frontend", "用户端"));
        SYNONYM_DICT.put("配置", Arrays.asList("config", "configuration", "设置", "参数"));
        SYNONYM_DICT.put("文档", Arrays.asList("document", "doc", "文件", "资料"));
        SYNONYM_DICT.put("错误", Arrays.asList("error", "异常", "exception", "bug", "问题"));
        SYNONYM_DICT.put("性能", Arrays.asList("performance", "效率", "速度", "优化"));
        SYNONYM_DICT.put("安全", Arrays.asList("security", "安全性", "加密", "权限"));
        SYNONYM_DICT.put("部署", Arrays.asList("deploy", "deployment", "发布", "上线"));

        // 业务领域同义词
        SYNONYM_DICT.put("用户", Arrays.asList("user", "客户", "会员", "账户"));
        SYNONYM_DICT.put("订单", Arrays.asList("order", "交易", "购买记录"));
        SYNONYM_DICT.put("支付", Arrays.asList("pay", "payment", "付款", "结算"));
        SYNONYM_DICT.put("报表", Arrays.asList("report", "统计", "分析", "报告"));

        // 通用同义词
        SYNONYM_DICT.put("如何", Arrays.asList("怎么", "怎样", "方法", "步骤", "how"));
        SYNONYM_DICT.put("什么", Arrays.asList("哪些", "which", "what"));
        SYNONYM_DICT.put("为什么", Arrays.asList("原因", "why", "理由"));
        SYNONYM_DICT.put("创建", Arrays.asList("新建", "添加", "create", "add"));
        SYNONYM_DICT.put("删除", Arrays.asList("移除", "清除", "delete", "remove"));
        SYNONYM_DICT.put("修改", Arrays.asList("更新", "编辑", "update", "edit", "change"));
        SYNONYM_DICT.put("查询", Arrays.asList("搜索", "检索", "查找", "search", "query", "find"));
    }

    /**
     * 初始化停用词
     */
    private static void initStopWords() {
        STOP_WORDS.addAll(Arrays.asList(
            // 中文停用词
            "的", "了", "和", "与", "或", "是", "在", "有", "这", "那",
            "吗", "呢", "啊", "吧", "呀", "哦", "哈", "嗯", "呵",
            "我", "你", "他", "她", "它", "我们", "你们", "他们",
            "一个", "一些", "这个", "那个", "这些", "那些",
            "请", "请问", "想", "要", "能", "可以", "应该",
            // 英文停用词
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "to", "of", "in", "for", "on", "with", "at", "by", "from",
            "i", "you", "he", "she", "it", "we", "they",
            "this", "that", "these", "those",
            "and", "or", "but", "if", "then", "else"
        ));
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

