package top.yumbo.ai.rag.spring.boot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略配置服务
 * (Strategy Configuration Service)
 *
 * 管理分析策略的配置，支持：
 * - 从 YAML 文件动态加载策略配置
 * - 热更新策略
 * - 策略市场管理
 *
 * (Manages analysis strategy configurations, supports:)
 * - Dynamic loading from YAML files
 * - Hot update strategies
 * - Strategy marketplace management
 */
@Service
@Slf4j
public class StrategyConfigService {

    @Value("${strategy.config.path:classpath:strategies/}")
    private String strategyConfigPath;

    @Value("${strategy.marketplace.enabled:true}")
    private boolean marketplaceEnabled;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    // 已加载的策略配置 (Loaded strategy configurations)
    private final Map<String, Map<String, Object>> loadedStrategies = new ConcurrentHashMap<>();

    // 用户分析目标配置 (User analysis goals configuration)
    private final Map<String, Map<String, Object>> analysisGoals = new ConcurrentHashMap<>();

    // 意图检测规则 (Intent detection rules)
    private final List<Map<String, Object>> intentRules = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void init() {
        log.info("📦 Initializing Strategy Configuration Service...");
        loadDefaultGoals();
        loadDefaultIntentRules();
        loadStrategiesFromFiles();
        log.info("✅ Strategy Configuration Service initialized. {} goals, {} strategies loaded",
                analysisGoals.size(), loadedStrategies.size());
    }

    /**
     * 加载默认的分析目标
     * (Load default analysis goals)
     */
    private void loadDefaultGoals() {
        // 快速了解 (Quick Overview)
        analysisGoals.put("quick", createGoal(
                "quick", "🚀", 1,
                Map.of("zh", "快速了解大意", "en", "Quick Overview"),
                Map.of("zh", "几分钟内了解主要内容", "en", "Understand main content in minutes"),
                List.of("parallel-summary", "compress"),
                Map.of("zh", "1-2分钟", "en", "1-2 min"),
                "low", 1, null
        ));

        // 精确查找 (Precise Search)
        analysisGoals.put("precise", createGoal(
                "precise", "🔍", 2,
                Map.of("zh", "精确查找答案", "en", "Find Precise Answers"),
                Map.of("zh", "针对问题找出准确答案", "en", "Find accurate answers to questions"),
                List.of("question-driven", "hyde"),
                Map.of("zh", "30秒", "en", "30 sec"),
                "lowest", 1, null
        ));

        // 对比分析 (Comparison)
        analysisGoals.put("compare", createGoal(
                "compare", "⚖️", 3,
                Map.of("zh", "对比优劣", "en", "Compare Pros & Cons"),
                Map.of("zh", "对比文档的优缺点和差异", "en", "Compare advantages and differences"),
                List.of("parallel-summary", "structured-compare"),
                Map.of("zh", "2-3分钟", "en", "2-3 min"),
                "medium", 2, null
        ));

        // 关联分析 (Relation Analysis)
        analysisGoals.put("relation", createGoal(
                "relation", "🔗", 4,
                Map.of("zh", "分析关联关系", "en", "Analyze Relationships"),
                Map.of("zh", "找出文档间的联系和异同点", "en", "Find connections and differences"),
                List.of("entity-relation", "mind-map"),
                Map.of("zh", "3-5分钟", "en", "3-5 min"),
                "medium", 2, null
        ));

        // 因果分析 (Causal Analysis)
        analysisGoals.put("causal", createGoal(
                "causal", "⛓️", 5,
                Map.of("zh", "追溯因果脉络", "en", "Trace Cause & Effect"),
                Map.of("zh", "分析前因后果和逻辑链条", "en", "Analyze causes and consequences"),
                List.of("sequential-summary", "entity-relation"),
                Map.of("zh", "3-5分钟", "en", "3-5 min"),
                "medium", 2, null
        ));

        // 深度分析 (Comprehensive Analysis)
        analysisGoals.put("comprehensive", createGoal(
                "comprehensive", "📊", 6,
                Map.of("zh", "全面深度分析", "en", "Comprehensive Analysis"),
                Map.of("zh", "最详细的分析报告", "en", "Most detailed analysis report"),
                List.of("hierarchical", "iterative-refine", "self-consistency"),
                Map.of("zh", "10-15分钟", "en", "10-15 min"),
                "high", 1, "comprehensive"
        ));

        log.info("📋 Loaded {} default analysis goals", analysisGoals.size());
    }

    /**
     * 创建分析目标配置
     */
    private Map<String, Object> createGoal(
            String id, String icon, int order,
            Map<String, String> label, Map<String, String> description,
            List<String> strategies, Map<String, String> estimatedTime,
            String tokenCost, int minDocs, String buttonClass
    ) {
        Map<String, Object> goal = new LinkedHashMap<>();
        goal.put("id", id);
        goal.put("icon", icon);
        goal.put("order", order);
        goal.put("label", label);
        goal.put("description", description);
        goal.put("strategies", strategies);
        goal.put("estimatedTime", estimatedTime);
        goal.put("tokenCost", tokenCost);
        goal.put("minDocs", minDocs);
        if (buttonClass != null) {
            goal.put("buttonClass", buttonClass);
        }
        return goal;
    }

    /**
     * 加载默认的意图检测规则
     * (Load default intent detection rules)
     */
    private void loadDefaultIntentRules() {
        intentRules.add(createIntentRule(
                "什么|哪个|多少|是否|有没有|who|what|which|how many",
                "precise",
                Map.of("zh", "检测到精确查询类问题", "en", "Detected precise query")
        ));

        intentRules.add(createIntentRule(
                "总结|概括|简述|概述|summarize|summary|overview",
                "quick",
                Map.of("zh", "检测到总结类需求", "en", "Detected summary request")
        ));

        intentRules.add(createIntentRule(
                "对比|比较|区别|相同|不同|差异|compare|difference|versus|vs",
                "compare",
                Map.of("zh", "检测到对比分析需求", "en", "Detected comparison request")
        ));

        intentRules.add(createIntentRule(
                "为什么|原因|导致|因为|结果|影响|why|cause|effect|because|result",
                "causal",
                Map.of("zh", "检测到因果分析需求", "en", "Detected causal analysis")
        ));

        intentRules.add(createIntentRule(
                "关系|关联|联系|相关|how.*relate|relationship|connection|link",
                "relation",
                Map.of("zh", "检测到关联分析需求", "en", "Detected relationship analysis")
        ));

        intentRules.add(createIntentRule(
                "全面|详细|深入|完整|comprehensive|detailed|thorough|complete",
                "comprehensive",
                Map.of("zh", "检测到深度分析需求", "en", "Detected deep analysis")
        ));

        log.info("📝 Loaded {} intent detection rules", intentRules.size());
    }

    private Map<String, Object> createIntentRule(String pattern, String goal, Map<String, String> reason) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("pattern", pattern);
        rule.put("goal", goal);
        rule.put("reason", reason);
        return rule;
    }

    /**
     * 从文件加载策略配置
     * (Load strategies from files)
     */
    private void loadStrategiesFromFiles() {
        try {
            // 尝试从 classpath 加载 JSON 格式的策略
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:strategies/*.json");

            for (Resource resource : resources) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = jsonMapper.readValue(resource.getInputStream(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> strategyMeta = (Map<String, Object>) config.get("strategy");
                    if (strategyMeta != null) {
                        String id = (String) strategyMeta.get("id");
                        loadedStrategies.put(id, config);
                        log.info("📦 Loaded strategy from classpath: {}", id);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Failed to load strategy: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.debug("No strategies found in classpath");
        }

        // 尝试从外部目录加载
        Path externalPath = Paths.get("strategies");
        if (Files.exists(externalPath) && Files.isDirectory(externalPath)) {
            try (var stream = Files.list(externalPath)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                      .forEach(this::loadStrategyFromFile);
            } catch (IOException e) {
                log.warn("⚠️ Failed to scan external strategies directory", e);
            }
        }
    }

    private void loadStrategyFromFile(Path path) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = jsonMapper.readValue(path.toFile(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> strategyMeta = (Map<String, Object>) config.get("strategy");
            if (strategyMeta != null) {
                String id = (String) strategyMeta.get("id");
                loadedStrategies.put(id, config);
                log.info("📦 Loaded strategy from file: {}", id);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to load strategy from {}", path, e);
        }
    }

    /**
     * 获取完整配置（供前端使用）
     * (Get full configuration for frontend)
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("goals", analysisGoals);
        config.put("intentRules", intentRules);
        config.put("strategies", loadedStrategies);
        config.put("defaultGoal", "quick");
        config.put("version", "1.0.0");
        config.put("lastUpdated", System.currentTimeMillis());
        return config;
    }

    /**
     * 获取策略市场列表
     * (Get strategy marketplace list)
     */
    public Map<String, Object> getMarketplace() {
        List<Map<String, Object>> strategies = new ArrayList<>();

        // 添加已安装的策略
        for (Map.Entry<String, Map<String, Object>> entry : loadedStrategies.entrySet()) {
            Map<String, Object> strategyConfig = entry.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> strategyMeta = (Map<String, Object>) strategyConfig.get("strategy");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", entry.getKey());
            item.put("name", strategyMeta.get("name"));
            item.put("version", strategyMeta.get("version"));
            item.put("description", strategyMeta.get("description"));
            item.put("author", strategyMeta.get("author"));
            item.put("icon", strategyMeta.getOrDefault("icon", "📦"));
            item.put("status", "installed");
            item.put("tags", strategyMeta.getOrDefault("tags", List.of()));

            strategies.add(item);
        }

        // 添加模拟的可安装策略（实际应从远程仓库获取）
        strategies.addAll(getAvailableStrategiesFromMarket());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategies", strategies);
        result.put("total", strategies.size());
        return result;
    }

    /**
     * 获取远程可用策略（模拟）
     */
    private List<Map<String, Object>> getAvailableStrategiesFromMarket() {
        List<Map<String, Object>> available = new ArrayList<>();

        // GraphRAG
        if (!loadedStrategies.containsKey("graph-rag")) {
            Map<String, Object> graphRag = new LinkedHashMap<>();
            graphRag.put("id", "graph-rag");
            graphRag.put("name", Map.of("zh", "GraphRAG", "en", "GraphRAG"));
            graphRag.put("version", "2.1.0");
            graphRag.put("description", Map.of(
                    "zh", "基于知识图谱的多文档分析，来自Microsoft Research",
                    "en", "Knowledge graph based multi-document analysis from Microsoft Research"
            ));
            graphRag.put("author", "Microsoft Research");
            graphRag.put("icon", "📐");
            graphRag.put("status", "available");
            graphRag.put("tags", List.of("knowledge-graph", "multi-doc", "relation"));
            graphRag.put("rating", 4.8);
            graphRag.put("quality", Map.of("zh", "最佳", "en", "Best"));
            graphRag.put("speed", Map.of("zh", "中等", "en", "Medium"));
            graphRag.put("cost", Map.of("zh", "中等", "en", "Medium"));
            available.add(graphRag);
        }

        // LLMLingua
        if (!loadedStrategies.containsKey("llm-lingua")) {
            Map<String, Object> llmLingua = new LinkedHashMap<>();
            llmLingua.put("id", "llm-lingua");
            llmLingua.put("name", Map.of("zh", "LLMLingua-2", "en", "LLMLingua-2"));
            llmLingua.put("version", "2.0.0");
            llmLingua.put("description", Map.of(
                    "zh", "新一代提示压缩，支持90%压缩率",
                    "en", "Next-gen prompt compression, up to 90% compression rate"
            ));
            llmLingua.put("author", "Microsoft");
            llmLingua.put("icon", "🔮");
            llmLingua.put("status", "available");
            llmLingua.put("tags", List.of("compression", "token-saving", "efficiency"));
            llmLingua.put("rating", 4.9);
            llmLingua.put("quality", Map.of("zh", "良好", "en", "Good"));
            llmLingua.put("speed", Map.of("zh", "最快", "en", "Fastest"));
            llmLingua.put("cost", Map.of("zh", "最低", "en", "Lowest"));
            available.add(llmLingua);
        }

        // Multi-Agent
        if (!loadedStrategies.containsKey("multi-agent")) {
            Map<String, Object> multiAgent = new LinkedHashMap<>();
            multiAgent.put("id", "multi-agent");
            multiAgent.put("name", Map.of("zh", "多Agent协作", "en", "Multi-Agent Collaboration"));
            multiAgent.put("version", "1.0.0");
            multiAgent.put("description", Map.of(
                    "zh", "多个AI角色协作分析，模拟专家团队讨论",
                    "en", "Multiple AI agents collaborate, simulating expert team discussion"
            ));
            multiAgent.put("author", "Community");
            multiAgent.put("icon", "🎭");
            multiAgent.put("status", "available");
            multiAgent.put("tags", List.of("multi-agent", "collaboration", "deep-analysis"));
            multiAgent.put("rating", 4.5);
            multiAgent.put("quality", Map.of("zh", "优秀", "en", "Excellent"));
            multiAgent.put("speed", Map.of("zh", "较慢", "en", "Slow"));
            multiAgent.put("cost", Map.of("zh", "较高", "en", "High"));
            available.add(multiAgent);
        }

        return available;
    }

    /**
     * 安装策略
     */
    public Map<String, Object> installStrategy(String strategyId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 模拟安装过程
        log.info("📥 Installing strategy: {}", strategyId);

        // 实际实现中应该从远程下载并加载
        // 这里简单模拟
        result.put("success", true);
        result.put("message", "Strategy installed successfully");
        result.put("strategyId", strategyId);

        return result;
    }

    /**
     * 卸载策略
     */
    public Map<String, Object> uninstallStrategy(String strategyId) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (loadedStrategies.containsKey(strategyId)) {
            loadedStrategies.remove(strategyId);
            result.put("success", true);
            result.put("message", "Strategy uninstalled successfully");
        } else {
            result.put("success", false);
            result.put("message", "Strategy not found");
        }

        return result;
    }

    /**
     * 获取策略详情
     */
    public Map<String, Object> getStrategyDetails(String strategyId) {
        if (loadedStrategies.containsKey(strategyId)) {
            return loadedStrategies.get(strategyId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", "Strategy not found");
        return result;
    }

    /**
     * 动态添加分析目标
     * (Dynamically add analysis goal)
     */
    public void addGoal(String id, Map<String, Object> goalConfig) {
        analysisGoals.put(id, goalConfig);
        log.info("➕ Added new analysis goal: {}", id);
    }

    /**
     * 动态添加意图规则
     * (Dynamically add intent rule)
     */
    public void addIntentRule(Map<String, Object> rule) {
        intentRules.add(rule);
        log.info("➕ Added new intent rule for goal: {}", rule.get("goal"));
    }

    /**
     * 热重载配置
     * (Hot reload configuration)
     */
    public void reloadConfiguration() {
        log.info("🔄 Reloading strategy configuration...");
        loadedStrategies.clear();
        loadStrategiesFromFiles();
        log.info("✅ Configuration reloaded. {} strategies loaded", loadedStrategies.size());
    }
}

