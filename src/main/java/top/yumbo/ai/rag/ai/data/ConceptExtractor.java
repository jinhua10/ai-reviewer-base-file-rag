package top.yumbo.ai.rag.ai.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 概念提取器 (Concept Extractor)
 *
 * 功能 (Features):
 * 1. 业务术语提取 (Business term extraction)
 * 2. 关键概念识别 (Key concept identification)
 * 3. 概念关系分析 (Concept relationship analysis)
 * 4. 频率统计 (Frequency statistics)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class ConceptExtractor {

    /**
     * 停用词列表 (Stop words list)
     */
    private final Set<String> stopWords = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
        "都", "一", "个", "上", "也", "为", "这", "来", "要", "说",
        "the", "is", "at", "which", "on", "a", "an", "and", "or", "but"
    ));

    /**
     * 概念模式 (Concept patterns)
     */
    private final List<Pattern> conceptPatterns = Arrays.asList(
        Pattern.compile("[A-Z][a-z]+(?:[A-Z][a-z]+)+"),  // 驼峰命名
        Pattern.compile("\\b[A-Z]{2,}\\b"),               // 缩写
        Pattern.compile("\\b\\w+(?:系统|模块|服务|接口)\\b") // 技术术语
    );

    // ========== 初始化 (Initialization) ==========

    public ConceptExtractor() {
        log.info(I18N.get("data.extractor.initialized"));
    }

    // ========== 概念提取 (Concept Extraction) ==========

    /**
     * 提取概念 (Extract concepts)
     *
     * @param text 文本 (Text)
     * @return 提取的概念列表 (Extracted concepts list)
     */
    public List<ExtractedConcept> extract(String text) {
        try {
            log.debug(I18N.get("data.extractor.extracting"));

            Map<String, ExtractedConcept> conceptMap = new HashMap<>();

            // 1. 分词 (Tokenize)
            List<String> words = tokenize(text);

            // 2. 提取候选概念 (Extract candidate concepts)
            for (String word : words) {
                if (isValidConcept(word)) {
                    ExtractedConcept concept = conceptMap.computeIfAbsent(
                        word,
                        k -> {
                            ExtractedConcept c = new ExtractedConcept();
                            c.setTerm(k);
                            c.setFrequency(0);
                            c.setType(detectConceptType(k));
                            return c;
                        }
                    );
                    concept.setFrequency(concept.getFrequency() + 1);
                }
            }

            // 3. 使用模式提取 (Extract using patterns)
            for (Pattern pattern : conceptPatterns) {
                var matcher = pattern.matcher(text);
                while (matcher.find()) {
                    String term = matcher.group();
                    if (!stopWords.contains(term.toLowerCase())) {
                        ExtractedConcept concept = conceptMap.computeIfAbsent(
                            term,
                            k -> {
                                ExtractedConcept c = new ExtractedConcept();
                                c.setTerm(k);
                                c.setFrequency(0);
                                c.setType(ConceptType.TECHNICAL_TERM);
                                return c;
                            }
                        );
                        concept.setFrequency(concept.getFrequency() + 1);
                    }
                }
            }

            // 4. 按频率排序 (Sort by frequency)
            List<ExtractedConcept> concepts = new ArrayList<>(conceptMap.values());
            concepts.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));

            log.info(I18N.get("data.extractor.extracted"), concepts.size());
            return concepts;

        } catch (Exception e) {
            log.error(I18N.get("data.extractor.extract_failed"), e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量提取概念 (Batch extract concepts)
     */
    public Map<String, List<ExtractedConcept>> batchExtract(List<String> texts) {
        Map<String, List<ExtractedConcept>> results = new HashMap<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            List<ExtractedConcept> concepts = extract(text);
            results.put("text-" + i, concepts);
        }

        log.info(I18N.get("data.extractor.batch_extracted"), texts.size());
        return results;
    }

    // ========== 辅助方法 (Helper Methods) ==========

    /**
     * 分词 (Tokenize)
     */
    private List<String> tokenize(String text) {
        // 简单实现：按空格和标点分割
        return Arrays.stream(text.split("[\\s\\p{Punct}]+"))
            .filter(w -> !w.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * 判断是否是有效概念 (Check if valid concept)
     */
    private boolean isValidConcept(String word) {
        // 长度检查 (Length check)
        if (word.length() < 2) {
            return false;
        }

        // 停用词检查 (Stop words check)
        if (stopWords.contains(word.toLowerCase())) {
            return false;
        }

        // 纯数字检查 (Pure number check)
        if (word.matches("\\d+")) {
            return false;
        }

        return true;
    }

    /**
     * 检测概念类型 (Detect concept type)
     */
    private ConceptType detectConceptType(String term) {
        // 驼峰命名 -> 技术术语 (CamelCase -> Technical term)
        if (term.matches("[A-Z][a-z]+(?:[A-Z][a-z]+)+")) {
            return ConceptType.TECHNICAL_TERM;
        }

        // 全大写缩写 (All caps -> Abbreviation)
        if (term.matches("[A-Z]{2,}")) {
            return ConceptType.ABBREVIATION;
        }

        // 包含系统/模块等 -> 系统组件 (Contains system/module -> Component)
        if (term.matches(".*(系统|模块|服务|接口).*")) {
            return ConceptType.SYSTEM_COMPONENT;
        }

        // 中文 -> 业务术语 (Chinese -> Business term)
        if (term.matches(".*[\\u4e00-\\u9fa5].*")) {
            return ConceptType.BUSINESS_TERM;
        }

        return ConceptType.GENERAL;
    }

    // ========== 概念关系分析 (Concept Relationship Analysis) ==========

    /**
     * 分析概念关系 (Analyze concept relationships)
     */
    public List<ConceptRelation> analyzeRelationships(List<ExtractedConcept> concepts, String text) {
        List<ConceptRelation> relations = new ArrayList<>();

        // 简单实现：如果两个概念在同一句话中出现，认为有关系
        for (int i = 0; i < concepts.size(); i++) {
            for (int j = i + 1; j < concepts.size(); j++) {
                ExtractedConcept c1 = concepts.get(i);
                ExtractedConcept c2 = concepts.get(j);

                if (areRelated(c1.getTerm(), c2.getTerm(), text)) {
                    ConceptRelation relation = new ConceptRelation();
                    relation.setSource(c1.getTerm());
                    relation.setTarget(c2.getTerm());
                    relation.setRelationType(RelationType.CO_OCCURRENCE);
                    relation.setStrength(calculateRelationStrength(c1, c2, text));

                    relations.add(relation);
                }
            }
        }

        return relations;
    }

    /**
     * 判断是否相关 (Check if related)
     */
    private boolean areRelated(String term1, String term2, String text) {
        // 在同一句话中出现 (Appear in same sentence)
        String[] sentences = text.split("[。！？.!?]");
        for (String sentence : sentences) {
            if (sentence.contains(term1) && sentence.contains(term2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算关系强度 (Calculate relation strength)
     */
    private double calculateRelationStrength(ExtractedConcept c1, ExtractedConcept c2, String text) {
        // 简单实现：基于共现次数
        int coOccurrence = 0;
        String[] sentences = text.split("[。！？.!?]");
        for (String sentence : sentences) {
            if (sentence.contains(c1.getTerm()) && sentence.contains(c2.getTerm())) {
                coOccurrence++;
            }
        }

        return Math.min(1.0, coOccurrence / 10.0);
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 提取的概念 (Extracted Concept)
     */
    @Data
    public static class ExtractedConcept {
        private String term;            // 术语
        private ConceptType type;       // 类型
        private int frequency;          // 频率
        private double importance;      // 重要性
    }

    /**
     * 概念类型 (Concept Type)
     */
    public enum ConceptType {
        TECHNICAL_TERM,      // 技术术语
        BUSINESS_TERM,       // 业务术语
        SYSTEM_COMPONENT,    // 系统组件
        ABBREVIATION,        // 缩写
        GENERAL              // 通用
    }

    /**
     * 概念关系 (Concept Relation)
     */
    @Data
    public static class ConceptRelation {
        private String source;          // 源概念
        private String target;          // 目标概念
        private RelationType relationType; // 关系类型
        private double strength;        // 关系强度
    }

    /**
     * 关系类型 (Relation Type)
     */
    public enum RelationType {
        CO_OCCURRENCE,  // 共现
        HIERARCHY,      // 层级关系
        DEPENDENCY,     // 依赖关系
        ASSOCIATION     // 关联关系
    }
}

