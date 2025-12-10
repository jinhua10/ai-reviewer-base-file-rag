package top.yumbo.ai.rag.hope;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 问题分类器 - 决定使用哪一层知识回答
 * (Question Classifier - Decides which layer to use for answering)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Component
public class QuestionClassifier {

    /**
     * 问题类型枚举
     * (Question Type Enum)
     */
    public enum QuestionType {
        /**
         * 事实型：有确定答案，如"项目使用什么框架？"
         * (Factual: Has definite answer, e.g., "What framework does the project use?")
         */
        FACTUAL,

        /**
         * 过程型：怎么做，如"如何配置数据库？"
         * (Procedural: How-to, e.g., "How to configure database?")
         */
        PROCEDURAL,

        /**
         * 概念型：是什么，如"什么是 RAG？"
         * (Conceptual: What is, e.g., "What is RAG?")
         */
        CONCEPTUAL,

        /**
         * 分析型：为什么，如"为什么要使用混合检索？"
         * (Analytical: Why, e.g., "Why use hybrid search?")
         */
        ANALYTICAL,

        /**
         * 创作型：需要生成新内容
         * (Creative: Requires generating new content)
         */
        CREATIVE,

        /**
         * 未知类型
         * (Unknown type)
         */
        UNKNOWN
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

        return builder.build();
    }

    /**
     * 检测问题类型
     * (Detect question type)
     * 
     * @param question 问题文本 (Question text)
     * @return 问题类型 (Question type)
     */
    private QuestionType detectQuestionType(String question) {
        // 优先检查创作型（通常需要完整 RAG）
        if (containsAny(question, CREATIVE_KEYWORDS)) {
            return QuestionType.CREATIVE;
        }

        // 检查事实型（可能可以直接回答）
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
     * 评估问题复杂度
     * (Assess question complexity)
     * 
     * @param question 问题文本 (Question text)
     * @param type 问题类型 (Question type)
     * @return 复杂度等级 (Complexity level)
     */
    private ComplexityLevel assessComplexity(String question, QuestionType type) {
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
}

