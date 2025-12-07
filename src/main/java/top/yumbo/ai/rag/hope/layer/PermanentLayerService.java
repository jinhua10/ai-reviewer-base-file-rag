package top.yumbo.ai.rag.hope.layer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.hope.HOPEConfig;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.SkillTemplate;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 低频层服务 - 管理技能知识和确定性知识
 * (Permanent Layer Service - Manages skill templates and factual knowledge)
 *
 * 特点：
 * - 存储稳定的、经过验证的知识
 * - 可直接回答，无需 LLM
 * - 极少更新，高置信度
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */
@Slf4j
@Service
public class PermanentLayerService {

    private final HOPEConfig config;

    // 技能模板存储
    private final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();

    // 确定性知识存储
    private final Map<String, FactualKnowledge> factualKnowledge = new ConcurrentHashMap<>();

    // 关键词到知识ID的索引
    private final Map<String, Set<String>> keywordIndex = new ConcurrentHashMap<>();

    @Autowired
    public PermanentLayerService(HOPEConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        if (!config.isEnabled()) {
            log.info(I18N.get("hope.permanent.disabled"));
            return;
        }

        try {
            // 确保存储目录存在
            Path storagePath = Paths.get(config.getPermanent().getStoragePath());
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // 加载已保存的数据
            loadData();

            // 初始化内置技能模板
            initBuiltinSkillTemplates();

            // 初始化内置确定性知识
            initBuiltinFactualKnowledge();

            log.info(I18N.get("hope.permanent.init_success",
                skillTemplates.size(), factualKnowledge.size()));

        } catch (IOException e) {
            log.error(I18N.get("hope.permanent.init_failed"), e);
        }
    }

    /**
     * 查询低频层
     */
    public PermanentQueryResult query(String question) {
        long startTime = System.currentTimeMillis();
        PermanentQueryResult result = new PermanentQueryResult();

        String normalizedQuestion = question.toLowerCase().trim();

        // 1. 查找确定性知识（优先，因为可以直接回答）
        FactualKnowledge fact = findFactualKnowledge(normalizedQuestion);
        if (fact != null && fact.getConfidence() >= config.getPermanent().getDirectAnswerConfidence()) {
            result.setDirectAnswer(true);
            result.setAnswer(fact.getAnswer());
            result.setConfidence(fact.getConfidence());
            result.setSource(fact.getSource());
            result.setFactualKnowledge(fact);
            fact.recordAccess();
            log.debug(I18N.get("hope.permanent.factual_hit", fact.getId()));
        }

        // 2. 查找技能模板
        SkillTemplate template = findSkillTemplate(normalizedQuestion);
        if (template != null) {
            result.setSkillTemplate(template);
            template.setLastUsed(LocalDateTime.now());
            log.debug(I18N.get("hope.permanent.skill_hit", template.getId()));
        }

        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 查找匹配的确定性知识
     */
    private FactualKnowledge findFactualKnowledge(String question) {
        // 1. 通过关键词索引快速查找
        String[] words = question.split("\\s+");
        Set<String> candidateIds = new HashSet<>();

        for (String word : words) {
            Set<String> ids = keywordIndex.get(word.toLowerCase());
            if (ids != null) {
                if (candidateIds.isEmpty()) {
                    candidateIds.addAll(ids);
                } else {
                    candidateIds.retainAll(ids); // 交集
                }
            }
        }

        // 2. 对候选进行模式匹配
        FactualKnowledge bestMatch = null;
        double bestScore = 0;

        for (String id : candidateIds) {
            FactualKnowledge fact = factualKnowledge.get(id);
            if (fact == null || !fact.isEnabled()) {
                continue;
            }

            double score = calculateMatchScore(question, fact);
            if (score > bestScore && score >= 0.7) {
                bestScore = score;
                bestMatch = fact;
            }
        }

        // 3. 如果关键词索引没找到，遍历所有知识
        if (bestMatch == null) {
            for (FactualKnowledge fact : factualKnowledge.values()) {
                if (!fact.isEnabled()) {
                    continue;
                }
                double score = calculateMatchScore(question, fact);
                if (score > bestScore && score >= 0.7) {
                    bestScore = score;
                    bestMatch = fact;
                }
            }
        }

        return bestMatch;
    }

    /**
     * 查找匹配的技能模板
     */
    private SkillTemplate findSkillTemplate(String question) {
        for (SkillTemplate template : skillTemplates.values()) {
            if (!template.isEnabled()) {
                continue;
            }

            // 检查正则模式
            if (template.getPattern() != null) {
                try {
                    if (Pattern.matches(template.getPattern(), question)) {
                        return template;
                    }
                } catch (Exception e) {
                    // 正则错误，跳过
                }
            }

            // 检查关键词
            if (template.getKeywords() != null) {
                for (String keyword : template.getKeywords()) {
                    if (question.contains(keyword.toLowerCase())) {
                        return template;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 计算问题与知识的匹配分数
     */
    private double calculateMatchScore(String question, FactualKnowledge fact) {
        // 1. 正则匹配
        if (fact.getQuestionPattern() != null) {
            try {
                if (Pattern.matches(fact.getQuestionPattern().toLowerCase(), question)) {
                    return 1.0;
                }
            } catch (Exception e) {
                // 正则错误
            }
        }

        // 2. 关键词覆盖率
        if (fact.getKeywords() != null && fact.getKeywords().length > 0) {
            int matched = 0;
            for (String keyword : fact.getKeywords()) {
                if (question.contains(keyword.toLowerCase())) {
                    matched++;
                }
            }
            return (double) matched / fact.getKeywords().length;
        }

        return 0.0;
    }

    /**
     * 保存技能模板
     */
    public void saveSkillTemplate(SkillTemplate template) {
        if (template.getId() == null) {
            template.setId("skill_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(LocalDateTime.now());
        }
        template.setEnabled(true);

        skillTemplates.put(template.getId(), template);
        persistData();

        log.info(I18N.get("hope.permanent.skill_saved", template.getId(), template.getName()));
    }

    /**
     * 保存确定性知识
     */
    public void saveFactualKnowledge(FactualKnowledge knowledge) {
        if (knowledge.getId() == null) {
            knowledge.setId("fact_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (knowledge.getCreatedAt() == null) {
            knowledge.setCreatedAt(LocalDateTime.now());
        }
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledge.setEnabled(true);

        factualKnowledge.put(knowledge.getId(), knowledge);

        // 更新关键词索引
        if (knowledge.getKeywords() != null) {
            for (String keyword : knowledge.getKeywords()) {
                keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
                    .add(knowledge.getId());
            }
        }

        persistData();

        log.info(I18N.get("hope.permanent.factual_saved", knowledge.getId()));
    }

    /**
     * 记录技能模板使用反馈
     */
    public void recordSkillFeedback(String templateId, boolean success) {
        SkillTemplate template = skillTemplates.get(templateId);
        if (template != null) {
            template.recordUsage(success);
            persistData();
        }
    }

    /**
     * 记录确定性知识反馈
     */
    public void recordFactualFeedback(String knowledgeId, boolean positive) {
        FactualKnowledge knowledge = factualKnowledge.get(knowledgeId);
        if (knowledge != null) {
            knowledge.recordFeedback(positive);

            // 检查是否需要禁用
            if (knowledge.shouldDisable()) {
                knowledge.setEnabled(false);
                log.warn(I18N.get("hope.permanent.factual_disabled", knowledgeId));
            }

            persistData();
        }
    }

    /**
     * 初始化内置技能模板
     */
    private void initBuiltinSkillTemplates() {
        // 代码解释技能
        if (!skillTemplates.containsKey("skill_code_explain")) {
            saveSkillTemplate(SkillTemplate.builder()
                .id("skill_code_explain")
                .name("代码解释")
                .pattern(".*解释.*代码.*|.*代码.*什么意思.*|.*这段代码.*")
                .keywords(new String[]{"解释", "代码", "什么意思", "干什么"})
                .promptTemplate("""
                    请解释以下代码的功能：
                    
                    ```
                    {code}
                    ```
                    
                    要求：
                    1. 逐行或逐块注释关键逻辑
                    2. 总结整体功能
                    3. 指出潜在问题或优化点（如有）
                    """)
                .confidence(0.9)
                .build());
        }

        // 文档摘要技能
        if (!skillTemplates.containsKey("skill_doc_summary")) {
            saveSkillTemplate(SkillTemplate.builder()
                .id("skill_doc_summary")
                .name("文档摘要")
                .pattern(".*总结.*|.*摘要.*|.*概括.*|.*要点.*")
                .keywords(new String[]{"总结", "摘要", "概括", "要点", "归纳"})
                .promptTemplate("""
                    请对以下内容进行摘要：
                    
                    {content}
                    
                    要求：
                    1. 提取关键信息和核心观点
                    2. 保持逻辑清晰，条理分明
                    3. 摘要长度控制在原文的 20-30%
                    """)
                .confidence(0.85)
                .build());
        }

        // 对比分析技能
        if (!skillTemplates.containsKey("skill_compare")) {
            saveSkillTemplate(SkillTemplate.builder()
                .id("skill_compare")
                .name("对比分析")
                .pattern(".*比较.*|.*对比.*|.*区别.*|.*差异.*")
                .keywords(new String[]{"比较", "对比", "区别", "差异", "异同", "优缺点"})
                .promptTemplate("""
                    请对比分析以下内容：
                    
                    {content}
                    
                    要求：
                    1. 列出主要相同点
                    2. 列出主要差异点
                    3. 给出选择建议（如适用）
                    """)
                .confidence(0.85)
                .build());
        }
    }

    /**
     * 初始化内置确定性知识
     */
    private void initBuiltinFactualKnowledge() {
        // 项目框架信息
        if (!factualKnowledge.containsKey("fact_project_framework")) {
            saveFactualKnowledge(FactualKnowledge.builder()
                .id("fact_project_framework")
                .questionPattern(".*项目.*什么.*框架.*|.*使用.*什么.*技术.*")
                .keywords(new String[]{"项目", "框架", "技术", "技术栈"})
                .answer("本项目使用 Spring Boot 2.7.18 + Apache Lucene 9.9.1 构建，" +
                    "采用混合检索架构（BM25 + 向量检索），支持多种 LLM 后端。")
                .source("README.md")
                .confidence(1.0)
                .build());
        }

        // 支持格式信息
        if (!factualKnowledge.containsKey("fact_supported_formats")) {
            saveFactualKnowledge(FactualKnowledge.builder()
                .id("fact_supported_formats")
                .questionPattern(".*支持.*格式.*|.*哪些.*文档.*")
                .keywords(new String[]{"支持", "格式", "文档", "类型"})
                .answer("支持 35+ 种文档格式，包括：\n" +
                    "- 文本：TXT, MD, CSV, JSON, XML\n" +
                    "- 办公：PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX\n" +
                    "- 图片（OCR）：PNG, JPG, JPEG, GIF, BMP, TIFF\n" +
                    "- 代码：Java, Python, JS, Go, C++ 等")
                .source("README.md")
                .confidence(1.0)
                .build());
        }

        // 检索算法信息
        if (!factualKnowledge.containsKey("fact_search_algorithm")) {
            saveFactualKnowledge(FactualKnowledge.builder()
                .id("fact_search_algorithm")
                .questionPattern(".*检索.*算法.*|.*搜索.*原理.*|.*BM25.*")
                .keywords(new String[]{"检索", "算法", "搜索", "BM25", "向量"})
                .answer("系统采用混合检索架构：\n" +
                    "1. Lucene BM25 关键词检索（权重 0.3）\n" +
                    "2. BGE 向量语义检索（权重 0.7）\n" +
                    "3. 可选 PPL Rerank 二次排序\n" +
                    "综合评分后返回 Top-K 结果。")
                .source("HybridSearchService.java")
                .confidence(1.0)
                .build());
        }
    }

    /**
     * 持久化数据到文件
     */
    private void persistData() {
        try {
            Path storagePath = Paths.get(config.getPermanent().getStoragePath());

            // 保存技能模板
            Path skillPath = storagePath.resolve("skill_templates.json");
            Files.writeString(skillPath, JSON.toJSONString(skillTemplates.values(),
                JSONWriter.Feature.PrettyFormat));

            // 保存确定性知识
            Path factPath = storagePath.resolve("factual_knowledge.json");
            Files.writeString(factPath, JSON.toJSONString(factualKnowledge.values(),
                JSONWriter.Feature.PrettyFormat));

        } catch (IOException e) {
            log.error(I18N.get("hope.permanent.persist_failed"), e);
        }
    }

    /**
     * 从文件加载数据
     */
    private void loadData() {
        try {
            Path storagePath = Paths.get(config.getPermanent().getStoragePath());

            // 加载技能模板
            Path skillPath = storagePath.resolve("skill_templates.json");
            if (Files.exists(skillPath)) {
                String json = Files.readString(skillPath);
                List<SkillTemplate> templates = JSON.parseArray(json, SkillTemplate.class);
                for (SkillTemplate template : templates) {
                    skillTemplates.put(template.getId(), template);
                }
                log.info(I18N.get("hope.permanent.skills_loaded", templates.size()));
            }

            // 加载确定性知识
            Path factPath = storagePath.resolve("factual_knowledge.json");
            if (Files.exists(factPath)) {
                String json = Files.readString(factPath);
                List<FactualKnowledge> facts = JSON.parseArray(json, FactualKnowledge.class);
                for (FactualKnowledge fact : facts) {
                    factualKnowledge.put(fact.getId(), fact);
                    // 重建关键词索引
                    if (fact.getKeywords() != null) {
                        for (String keyword : fact.getKeywords()) {
                            keywordIndex.computeIfAbsent(keyword.toLowerCase(), k -> new HashSet<>())
                                .add(fact.getId());
                        }
                    }
                }
                log.info(I18N.get("hope.permanent.facts_loaded", facts.size()));
            }

        } catch (IOException e) {
            log.error(I18N.get("hope.permanent.load_failed"), e);
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("skillTemplateCount", skillTemplates.size());
        stats.put("factualKnowledgeCount", factualKnowledge.size());
        stats.put("enabledSkills", skillTemplates.values().stream().filter(SkillTemplate::isEnabled).count());
        stats.put("enabledFacts", factualKnowledge.values().stream().filter(FactualKnowledge::isEnabled).count());
        return stats;
    }

    /**
     * 低频层查询结果
     */
    @lombok.Data
    public static class PermanentQueryResult {
        private boolean directAnswer;
        private String answer;
        private double confidence;
        private String source;
        private SkillTemplate skillTemplate;
        private FactualKnowledge factualKnowledge;
        private long processingTimeMs;
    }
}

