package top.yumbo.ai.rag.hope;

import lombok.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import top.yumbo.ai.rag.hope.learning.QuestionClassifierLearningService;
import top.yumbo.ai.rag.i18n.I18N;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 问题分类器 - 决定使用哪一层知识回答
 * (Question Classifier - Decides which layer to use for answering)
 *
 * <p>
 * 设计特点 (Design Features):
 * <ul>
 *   <li>✅ 动态配置加载 - 支持从 YAML 文件加载分类规则</li>
 *   <li>✅ 完整国际化 - 所有文本支持中英文</li>
 *   <li>✅ 可扩展类型 - 支持动态添加问题类型</li>
 *   <li>✅ 角色知识库适配 - 不同角色可以有不同的分类策略</li>
 *   <li>✅ 热重载 - 可以在运行时重新加载配置</li>
 *   <li>✅ 高性能 - 使用缓存和优化的匹配算法</li>
 * </ul>
 * </p>
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 * @version 2.1.0 - 重构支持动态配置和国际化
 */
@Slf4j
@Component
public class QuestionClassifier {

    /**
     * 配置文件路径 (Configuration file path)
     */
    private static final String CONFIG_FILE = "question-classifier-config.yml";

    /**
     * 持久化管理器 (Persistence manager) - 支持可插拔策略
     */
    @Autowired(required = false)
    private top.yumbo.ai.rag.hope.persistence.PersistenceManager persistenceManager;

    /**
     * 学习服务 (Learning service)
     * 使用 @Lazy 避免循环依赖 (Use @Lazy to avoid circular dependency)
     */
    @Autowired(required = false)
    @Lazy
    private QuestionClassifierLearningService learningService;

    /**
     * 分类配置缓存 (Classification configuration cache)
     */
    private Map<String, Object> configCache = new ConcurrentHashMap<>();

    /**
     * 问题类型定义缓存 (Question type definition cache)
     */
    private List<QuestionTypeConfig> questionTypeConfigs = new ArrayList<>();

    /**
     * 关键词库缓存 (Keyword library cache)
     */
    private Map<String, List<String>> keywordCache = new ConcurrentHashMap<>();

    /**
     * 模式库缓存 (Pattern library cache)
     */
    private Map<String, List<String>> patternCache = new ConcurrentHashMap<>();

    /**
     * 配置版本号 (Configuration version)
     */
    private String configVersion = "unknown";

    /**
     * 是否启用 (Whether enabled)
     */
    private boolean enabled = true;

    /**
     * 问题类型配置 (Question Type Configuration)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionTypeConfig {
        private String id;
        private String name;
        private String nameEn;
        private int priority;
        private String complexity;
        private String suggestedLayer;
        private boolean enabled;
    }

    /**
     * 初始化配置 (Initialize configuration)
     */
    @PostConstruct
    public void init() {
        try {
            loadConfiguration();
            log.info(I18N.get("question.classifier.log.config_loaded") + " (version: {})", configVersion);
        } catch (Exception e) {
            log.error("Failed to load question classifier configuration", e);
            // 使用默认配置 (Use default configuration)
            initDefaultConfiguration();
        }
    }

    /**
     * 加载配置 (Load configuration)
     *
     * 优先级 (Priority):
     * 1. 持久化存储 (Persistence storage) - 最高优先级
     * 2. YAML 配置文件 (YAML config file)
     * 3. 默认配置 (Default configuration)
     */
    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        // 1. 尝试从持久化加载 (Try to load from persistence)
        if (persistenceManager != null && loadFromPersistence()) {
            log.info("✅ Loaded configuration from persistence (strategy: {})",
                    persistenceManager.getCurrentStrategy().getDescription());
            return;
        }

        // 2. 从 YAML 文件加载 (Load from YAML file)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                log.warn("Configuration file not found: {}, using default configuration", CONFIG_FILE);
                initDefaultConfiguration();

                // 保存默认配置到持久化 (Save default config to persistence)
                if (persistenceManager != null) {
                    saveToPersonsistence();
                }
                return;
            }

            Yaml yaml = new Yaml();
            configCache = yaml.load(input);

            // 加载版本号 (Load version)
            configVersion = (String) configCache.getOrDefault("version", "unknown");
            enabled = (boolean) configCache.getOrDefault("enabled", true);

            // 加载问题类型 (Load question types)
            List<Map<String, Object>> types = (List<Map<String, Object>>) configCache.get("question_types");
            if (types != null) {
                questionTypeConfigs = types.stream()
                    .map(this::mapToQuestionTypeConfig)
                    .filter(config -> config != null && config.isEnabled())
                    .sorted(Comparator.comparingInt(QuestionTypeConfig::getPriority))
                    .collect(Collectors.toList());

                log.debug("Loaded {} question types", questionTypeConfigs.size());
            }

            // 加载关键词库 (Load keywords)
            Map<String, Object> keywords = (Map<String, Object>) configCache.get("keywords");
            if (keywords != null) {
                loadKeywords(keywords);
            }

            // 加载模式库 (Load patterns)
            Map<String, Object> patterns = (Map<String, Object>) configCache.get("patterns");
            if (patterns != null) {
                loadPatterns(patterns);
            }

            // 保存到持久化 (Save to persistence)
            if (persistenceManager != null) {
                saveToPersonsistence();
            }

        } catch (Exception e) {
            log.error("Error loading configuration", e);
            throw new RuntimeException("Failed to load question classifier configuration", e);
        }
    }

    /**
     * 从持久化加载 (Load from persistence)
     *
     * @return 是否成功
     */
    private boolean loadFromPersistence() {
        try {
            // 加载问题类型 (Load question types)
            List<QuestionTypeConfig> types = persistenceManager.getAllQuestionTypes();
            if (types.isEmpty()) {
                return false;
            }

            questionTypeConfigs = types.stream()
                .filter(QuestionTypeConfig::isEnabled)
                .sorted(Comparator.comparingInt(QuestionTypeConfig::getPriority))
                .collect(Collectors.toList());

            // 加载关键词 (Load keywords)
            Map<String, List<String>> keywords = persistenceManager.getAllKeywords();
            keywordCache.clear();
            keywordCache.putAll(keywords);

            // 加载模式 (Load patterns)
            Map<String, List<String>> patterns = persistenceManager.getAllPatterns();
            patternCache.clear();
            patternCache.putAll(patterns);

            // 加载版本 (Load version)
            configVersion = persistenceManager.getVersion();

            log.info("Loaded {} types, {} keyword groups, {} pattern groups from persistence",
                    questionTypeConfigs.size(), keywordCache.size(), patternCache.size());

            return true;
        } catch (Exception e) {
            log.error("Failed to load from persistence", e);
            return false;
        }
    }

    /**
     * 保存到持久化 (Save to persistence)
     */
    private void saveToPersonsistence() {
        try {
            // 保存问题类型 (Save question types)
            persistenceManager.saveQuestionTypes(questionTypeConfigs);

            // 保存关键词 (Save keywords)
            for (Map.Entry<String, List<String>> entry : keywordCache.entrySet()) {
                persistenceManager.saveKeywords(entry.getKey(), entry.getValue());
            }

            // 保存模式 (Save patterns)
            for (Map.Entry<String, List<String>> entry : patternCache.entrySet()) {
                persistenceManager.savePatterns(entry.getKey(), entry.getValue());
            }

            // 保存版本 (Save version)
            persistenceManager.saveVersion(configVersion);

            log.info("Saved configuration to persistence (strategy: {})",
                    persistenceManager.getCurrentStrategy().getDescription());
        } catch (Exception e) {
            log.error("Failed to save to persistence", e);
        }
    }

    /**
     * 映射配置到类型对象 (Map configuration to type object)
     */
    private QuestionTypeConfig mapToQuestionTypeConfig(Map<String, Object> map) {
        try {
            QuestionTypeConfig config = new QuestionTypeConfig();
            config.setId((String) map.get("id"));
            config.setName((String) map.get("name"));
            config.setNameEn((String) map.get("name_en"));
            config.setPriority(((Number) map.get("priority")).intValue());
            config.setComplexity((String) map.get("complexity"));
            config.setSuggestedLayer((String) map.get("suggested_layer"));
            config.setEnabled((boolean) map.getOrDefault("enabled", true));
            return config;
        } catch (Exception e) {
            log.error("Error mapping question type config", e);
            return null;
        }
    }

    /**
     * 加载关键词库 (Load keywords)
     */
    @SuppressWarnings("unchecked")
    private void loadKeywords(Map<String, Object> keywords) {
        for (Map.Entry<String, Object> entry : keywords.entrySet()) {
            String typeId = entry.getKey();
            Object value = entry.getValue();

            List<String> allKeywords = new ArrayList<>();

            if (value instanceof Map) {
                // 嵌套结构 (Nested structure)
                Map<String, List<String>> nested = (Map<String, List<String>>) value;
                nested.values().forEach(allKeywords::addAll);
            } else if (value instanceof List) {
                // 扁平列表 (Flat list)
                allKeywords.addAll((List<String>) value);
            }

            keywordCache.put(typeId, allKeywords);
            log.debug(I18N.get("question.classifier.log.keyword_loaded"), allKeywords.size());
        }
    }

    /**
     * 加载模式库 (Load patterns)
     */
    @SuppressWarnings("unchecked")
    private void loadPatterns(Map<String, Object> patterns) {
        for (Map.Entry<String, Object> entry : patterns.entrySet()) {
            String typeId = entry.getKey();
            List<String> patternList = (List<String>) entry.getValue();
            patternCache.put(typeId, patternList);
            log.debug(I18N.get("question.classifier.log.pattern_loaded"), patternList.size());
        }
    }

    /**
     * 初始化默认配置 (Initialize default configuration)
     */
    private void initDefaultConfiguration() {
        // 添加默认类型 (Add default types)
        questionTypeConfigs.clear();
        questionTypeConfigs.add(new QuestionTypeConfig("social", "社交型", "Social", 1, "simple", "direct_llm", true));
        questionTypeConfigs.add(new QuestionTypeConfig("factual", "事实型", "Factual", 2, "simple", "permanent", true));
        questionTypeConfigs.add(new QuestionTypeConfig("conceptual", "概念型", "Conceptual", 3, "simple", "ordinary", true));
        questionTypeConfigs.add(new QuestionTypeConfig("procedural", "过程型", "Procedural", 4, "moderate", "ordinary", true));
        questionTypeConfigs.add(new QuestionTypeConfig("analytical", "分析型", "Analytical", 5, "complex", "full_rag", true));
        questionTypeConfigs.add(new QuestionTypeConfig("creative", "创作型", "Creative", 6, "complex", "full_rag", true));
        questionTypeConfigs.add(new QuestionTypeConfig("unknown", "未知型", "Unknown", 999, "moderate", "full_rag", true));

        // 添加默认关键词 (Add default keywords)
        keywordCache.put("social", Arrays.asList("你好", "谢谢", "再见", "hello", "thanks", "bye"));
        keywordCache.put("conceptual", Arrays.asList("是什么", "什么是", "what is", "define"));
        keywordCache.put("procedural", Arrays.asList("如何", "怎么", "how to", "steps"));
        keywordCache.put("analytical", Arrays.asList("为什么", "原因", "why", "reason"));
        keywordCache.put("creative", Arrays.asList("写", "生成", "write", "generate"));

        configVersion = "default";
        enabled = true;
    }

    /**
     * 重新加载配置 (Reload configuration)
     *
     * @return 是否成功 (Whether successful)
     */
    public boolean reloadConfiguration() {
        try {
            log.info(I18N.get("question.classifier.log.config_reload"));
            loadConfiguration();
            return true;
        } catch (Exception e) {
            log.error("Failed to reload configuration", e);
            return false;
        }
    }

    /**
     * 问题类型枚举（扩展版）
     * (Question Type Enum - Extended Version)
     */
    @Getter
    public enum QuestionType {
        /**
         * 社交型：问候、感谢、闲聊，如"你好"、"谢谢"
         * (Social: Greetings, thanks, small talk)
         */
        SOCIAL("social"),

        /**
         * 事实型：有确定答案，如"项目使用什么框架？"
         * (Factual: Has definite answer)
         */
        FACTUAL("factual"),

        /**
         * 过程型：怎么做，如"如何配置数据库？"
         * (Procedural: How-to)
         */
        PROCEDURAL("procedural"),

        /**
         * 概念型：是什么，如"什么是 RAG？"
         * (Conceptual: What is)
         */
        CONCEPTUAL("conceptual"),

        /**
         * 分析型：为什么，如"为什么要使用混合检索？"
         * (Analytical: Why)
         */
        ANALYTICAL("analytical"),

        /**
         * 创作型：需要生成新内容
         * (Creative: Requires generating new content)
         */
        CREATIVE("creative"),

        /**
         * 比较型：对比、区别、优劣，如"A和B有什么区别？"
         * (Comparison: Compare, difference, pros/cons)
         */
        COMPARISON("comparison"),

        /**
         * 推荐型：建议、选择、推荐，如"应该用哪个？"
         * (Recommendation: Suggest, choose, recommend)
         */
        RECOMMENDATION("recommendation"),

        /**
         * 故障排查型：错误、异常、问题，如"为什么报错？"
         * (Troubleshooting: Errors, exceptions, problems)
         */
        TROUBLESHOOTING("troubleshooting"),

        /**
         * 配置型：配置、设置、参数，如"如何配置环境变量？"
         * (Configuration: Config, settings, parameters)
         */
        CONFIGURATION("configuration"),

        /**
         * 未知类型
         * (Unknown type)
         */
        UNKNOWN("unknown");

        private final String id;

        QuestionType(String id) {
            this.id = id;
        }

        /**
         * 从 ID 获取类型 (Get type from ID)
         */
        public static QuestionType fromId(String id) {
            for (QuestionType type : values()) {
                if (type.getId().equals(id)) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        /**
         * 获取国际化名称 (Get i18n name)
         */
        public String getI18nName() {
            return I18N.get("question.classifier.type." + id);
        }

        /**
         * 获取国际化描述 (Get i18n description)
         */
        public String getI18nDescription() {
            return I18N.get("question.classifier.description." + id);
        }
    }

    /**
     * 复杂度等级
     * (Complexity Level)
     */
    public enum ComplexityLevel {
        /**
         * 简单：可直接回答
         * (Simple: Can answer directly)
         */
        SIMPLE,

        /**
         * 中等：需要检索
         * (Moderate: Requires retrieval)
         */
        MODERATE,

        /**
         * 复杂：需要深度推理
         * (Complex: Requires deep reasoning)
         */
        COMPLEX
    }

    /**
     * 分类结果
     * (Classification Result)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Classification {
        /**
         * 问题类型
         * (Question type)
         */
        private QuestionType type;

        /**
         * 复杂度等级
         * (Complexity level)
         */
        private ComplexityLevel complexity;

        /**
         * 置信度 (0-1)
         * (Confidence score 0-1)
         */
        private double confidence;

        /**
         * 建议查询的层级: permanent / ordinary / full_rag
         * (Recommended layer: permanent / ordinary / full_rag)
         */
        private String suggestedLayer;

        /**
         * 提取的关键词
         * (Extracted keywords)
         */
        private String[] keywords;

        /**
         * 是否为简单问题（可能不需要 LLM）
         * (Is simple question - may not need LLM)
         */
        public boolean isSimple() {
            return complexity == ComplexityLevel.SIMPLE && confidence > 0.8;
        }
    }

    // 社交型问题关键词 (Social question keywords)
    private static final String[] SOCIAL_GREETINGS = {
        // 中文问候 (Chinese greetings)
        "你好", "您好", "嗨", "哈喽", "早上好", "下午好", "晚上好",
        "早安", "午安", "晚安", "你好啊", "您好啊", "你好呀", "您好呀",
        "在吗", "在不在", "有人吗",
        // 英文问候 (English greetings)
        "hello", "hi", "hey", "good morning", "good afternoon", "good evening"
    };

    private static final String[] SOCIAL_FAREWELLS = {
        // 中文告别 (Chinese farewells)
        "再见", "拜拜", "回见", "下次见", "886", "88", "走了", "拜",
        // 英文告别 (English farewells)
        "bye", "goodbye", "see you", "good night"
    };

    private static final String[] SOCIAL_THANKS = {
        // 中文感谢 (Chinese thanks)
        "谢谢", "感谢", "多谢", "谢了", "谢谢你", "非常感谢", "十分感谢",
        // 英文感谢 (English thanks)
        "thanks", "thank you", "thx", "3q"
    };

    private static final String[] SOCIAL_CONFIRMATIONS = {
        // 简单确认/否定 (Simple confirmations/negations)
        "好的", "好", "嗯", "嗯嗯", "是的", "对", "对的", "没错",
        "不", "不是", "不对", "否",
        // 英文 (English)
        "ok", "okay", "yes", "no", "nope"
    };

    // 事实型问题模式 (Factual question patterns)
    private static final String[] FACTUAL_PATTERNS = {
        ".*是什么.*框架.*",
        ".*用什么.*技术.*",
        ".*版本.*是.*",
        ".*作者.*是.*",
        ".*支持.*格式.*",
        ".*有.*功能.*",
        ".*包含.*",
        ".*由.*组成.*"
    };

    // 过程型问题关键词 (Procedural question keywords)
    private static final String[] PROCEDURAL_KEYWORDS = {
        "如何", "怎么", "怎样", "步骤", "方法", "流程",
        "how to", "how do", "steps", "guide"
    };

    // 概念型问题关键词 (Conceptual question keywords)
    private static final String[] CONCEPTUAL_KEYWORDS = {
        "是什么", "什么是", "定义", "概念", "含义", "意思",
        "what is", "define", "meaning"
    };

    // 分析型问题关键词 (Analytical question keywords)
    private static final String[] ANALYTICAL_KEYWORDS = {
        "为什么", "原因", "分析", "比较", "区别", "优缺点",
        "why", "reason", "analyze", "compare", "difference"
    };

    // 创作型问题关键词 (Creative question keywords)
    private static final String[] CREATIVE_KEYWORDS = {
        "写", "生成", "创建", "设计", "编写", "帮我",
        "write", "generate", "create", "design", "help me"
    };

    /**
     * 对问题进行分类
     * (Classify the question)
     *
     * @param question 用户问题 (User question)
     * @return 分类结果 (Classification result)
     */
    public Classification classify(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Classification.builder()
                .type(QuestionType.UNKNOWN)
                .complexity(ComplexityLevel.SIMPLE)
                .confidence(0.0)
                .suggestedLayer("full_rag")
                .build();
        }

        String normalizedQuestion = question.toLowerCase().trim();
        Classification.ClassificationBuilder builder = Classification.builder();

        // 1. 检测问题类型
        QuestionType type = detectQuestionType(normalizedQuestion);
        builder.type(type);

        // 2. 评估复杂度
        ComplexityLevel complexity = assessComplexity(normalizedQuestion, type);
        builder.complexity(complexity);

        // 3. 计算置信度
        double confidence = calculateConfidence(normalizedQuestion, type);
        builder.confidence(confidence);

        // 4. 建议查询层
        String suggestedLayer = suggestLayer(type, complexity, confidence);
        builder.suggestedLayer(suggestedLayer);

        // 5. 提取关键词
        builder.keywords(extractKeywords(normalizedQuestion));

        Classification result = builder.build();

        // 6. 记录分类结果到学习服务 (Record classification to learning service)
        if (learningService != null) {
            learningService.recordClassification(question, type, confidence);
        }

        return result;
    }

    /**
     * 检测问题类型（使用动态配置）
     * (Detect question type using dynamic configuration)
     *
     * @param question 问题文本 (Question text)
     * @return 问题类型 (Question type)
     */
    private QuestionType detectQuestionType(String question) {
        log.debug(I18N.get("question.classifier.log.classification_start") + ": {}", question);

        // 按优先级检查每种类型 (Check each type by priority)
        for (QuestionTypeConfig config : questionTypeConfigs) {
            if (!config.isEnabled()) {
                continue;
            }

            String typeId = config.getId();

            // 1. 检查正则模式 (Check regex patterns)
            List<String> patterns = patternCache.get(typeId);
            if (patterns != null) {
                for (String pattern : patterns) {
                    try {
                        if (question.matches(pattern)) {
                            log.debug(I18N.get("question.classifier.log.pattern_matched") + ": {} -> {}",
                                    pattern, typeId);
                            return QuestionType.fromId(typeId);
                        }
                    } catch (Exception e) {
                        log.warn("Invalid pattern: {}", pattern);
                    }
                }
            }

            // 2. 检查关键词匹配 (Check keyword matching)
            List<String> keywords = keywordCache.get(typeId);
            if (keywords != null && containsAny(question, keywords)) {
                log.debug(I18N.get("question.classifier.log.keyword_matched") + ": {}", typeId);
                return QuestionType.fromId(typeId);
            }
        }

        // 3. 兜底逻辑：使用遗留的硬编码检测（保证兼容性）
        // (Fallback: Use legacy hardcoded detection for compatibility)
        QuestionType fallbackType = detectQuestionTypeFallback(question);
        if (fallbackType != QuestionType.UNKNOWN) {
            log.debug("Using fallback detection: {}", fallbackType);
            return fallbackType;
        }

        log.debug(I18N.get("question.classifier.log.unknown_type"));
        return QuestionType.UNKNOWN;
    }

    /**
     * 兜底检测方法（使用硬编码规则）
     * (Fallback detection method using hardcoded rules)
     */
    private QuestionType detectQuestionTypeFallback(String question) {
        // 最优先检查社交型
        if (isSocialQuestion(question)) {
            return QuestionType.SOCIAL;
        }

        // 检查创作型
        if (containsAny(question, CREATIVE_KEYWORDS)) {
            return QuestionType.CREATIVE;
        }

        // 检查事实型
        for (String pattern : FACTUAL_PATTERNS) {
            if (question.matches(pattern)) {
                return QuestionType.FACTUAL;
            }
        }

        // 检查过程型
        if (containsAny(question, PROCEDURAL_KEYWORDS)) {
            return QuestionType.PROCEDURAL;
        }

        // 检查概念型
        if (containsAny(question, CONCEPTUAL_KEYWORDS)) {
            return QuestionType.CONCEPTUAL;
        }

        // 检查分析型
        if (containsAny(question, ANALYTICAL_KEYWORDS)) {
            return QuestionType.ANALYTICAL;
        }

        return QuestionType.UNKNOWN;
    }

    /**
     * 判断是否为社交性问题
     * (Check if it's a social question)
     *
     * @param question 问题文本 (Question text)
     * @return 是否为社交问题 (Whether it's a social question)
     */
    private boolean isSocialQuestion(String question) {
        String normalized = question.trim();

        // 完全匹配社交词汇 (Exact match social words)
        if (containsAny(normalized, SOCIAL_GREETINGS) ||
            containsAny(normalized, SOCIAL_FAREWELLS) ||
            containsAny(normalized, SOCIAL_THANKS) ||
            containsAny(normalized, SOCIAL_CONFIRMATIONS)) {
            return true;
        }

        // 短问题（长度 <= 5）且包含社交词汇
        // (Short questions containing social words)
        if (normalized.length() <= 5) {
            return containsAny(normalized, SOCIAL_GREETINGS) ||
                   containsAny(normalized, SOCIAL_FAREWELLS) ||
                   containsAny(normalized, SOCIAL_THANKS);
        }

        return false;
    }

    /**
     * 评估问题复杂度
     * (Assess question complexity)
     * 
     * @param question 问题文本 (Question text)
     * @param type 问题类型 (Question type)
     * @return 复杂度等级 (Complexity level)
     */
    private ComplexityLevel assessComplexity(String question, QuestionType type) {
        // 社交型问题最简单，直接回复即可
        if (type == QuestionType.SOCIAL) {
            return ComplexityLevel.SIMPLE;
        }

        // 事实型问题通常简单
        if (type == QuestionType.FACTUAL) {
            return ComplexityLevel.SIMPLE;
        }

        // 创作型和分析型通常复杂
        if (type == QuestionType.CREATIVE || type == QuestionType.ANALYTICAL) {
            return ComplexityLevel.COMPLEX;
        }

        // 根据问题长度评估
        int length = question.length();
        if (length < 20) {
            return ComplexityLevel.SIMPLE;
        } else if (length < 50) {
            return ComplexityLevel.MODERATE;
        } else {
            return ComplexityLevel.COMPLEX;
        }
    }

    /**
     * 计算置信度
     * (Calculate confidence score)
     * 
     * @param question 问题文本 (Question text)
     * @param type 问题类型 (Question type)
     * @return 置信度分数 (Confidence score)
     */
    private double calculateConfidence(String question, QuestionType type) {
        if (type == QuestionType.UNKNOWN) {
            return 0.5;
        }

        double baseConfidence = 0.7;

        // 明确的问题类型关键词增加置信度
        if (type == QuestionType.FACTUAL) {
            baseConfidence += 0.2;
        } else if (type == QuestionType.PROCEDURAL || type == QuestionType.CONCEPTUAL) {
            baseConfidence += 0.1;
        }

        // 问题长度适中增加置信度
        int length = question.length();
        if (length > 10 && length < 100) {
            baseConfidence += 0.1;
        }

        return Math.min(baseConfidence, 1.0);
    }

    /**
     * 建议查询层
     * (Suggest query layer)
     * 
     * @param type 问题类型 (Question type)
     * @param complexity 复杂度等级 (Complexity level)
     * @param confidence 置信度分数 (Confidence score)
     * @return 建议的查询层 (Suggested query layer)
     */
    private String suggestLayer(QuestionType type, ComplexityLevel complexity, double confidence) {
        // 社交型问题 -> 直接 LLM，不需要检索
        if (type == QuestionType.SOCIAL) {
            return "direct_llm";
        }

        // 简单的事实型问题 -> 优先查低频层
        if (type == QuestionType.FACTUAL && complexity == ComplexityLevel.SIMPLE) {
            return "permanent";
        }

        // 概念型和过程型 -> 查中频层
        if ((type == QuestionType.CONCEPTUAL || type == QuestionType.PROCEDURAL)
            && complexity != ComplexityLevel.COMPLEX) {
            return "ordinary";
        }

        // 其他情况 -> 完整 RAG
        return "full_rag";
    }

    /**
     * 提取关键词
     * (Extract keywords)
     * 
     * @param question 问题文本 (Question text)
     * @return 关键词数组 (Keyword array)
     */
    private String[] extractKeywords(String question) {
        // 简单的关键词提取：移除停用词，分词
        String[] stopWords = {"的", "是", "在", "了", "和", "有", "我", "你", "这", "那",
            "什么", "怎么", "如何", "为什么", "a", "an", "the", "is", "are", "what", "how", "why"};

        String cleaned = question;
        for (String stop : stopWords) {
            cleaned = cleaned.replace(stop, " ");
        }

        return cleaned.split("\\s+");
    }

    /**
     * 检查字符串是否包含数组中的任意关键词
     * (Check if string contains any keyword from array)
     * 
     * @param text 文本内容 (Text content)
     * @param keywords 关键词数组 (Keyword array)
     * @return 是否包含关键词 (Whether contains keywords)
     */
    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查字符串是否包含列表中的任意关键词
     * (Check if string contains any keyword from list)
     *
     * @param text 文本内容 (Text content)
     * @param keywords 关键词列表 (Keyword list)
     * @return 是否包含关键词 (Whether contains keywords)
     */
    private boolean containsAny(String text, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

