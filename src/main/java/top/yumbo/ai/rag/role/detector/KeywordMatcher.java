package top.yumbo.ai.rag.role.detector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.role.Role;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词匹配器 (Keyword Matcher)
 *
 * 通过关键词匹配检测问题所属角色
 * (Detects role by keyword matching)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class KeywordMatcher {

    /**
     * 匹配角色 (Match roles)
     *
     * @param question 用户问题 (User question)
     * @param roles 候选角色列表 (Candidate roles)
     * @return 角色匹配结果列表 (Role match results)
     */
    public List<RoleMatchResult> match(String question, List<Role> roles) {
        if (question == null || question.trim().isEmpty()) {
            log.warn(I18N.get("detector.keyword.question.empty"));
            return Collections.emptyList();
        }

        if (roles == null || roles.isEmpty()) {
            log.warn(I18N.get("detector.keyword.roles.empty"));
            return Collections.emptyList();
        }

        log.debug(I18N.get("detector.keyword.matching", question, roles.size()));

        // 预处理问题 (Preprocess question)
        String processedQuestion = preprocessQuestion(question);
        Set<String> questionTokens = tokenize(processedQuestion);

        // 对每个角色进行匹配 (Match each role)
        List<RoleMatchResult> results = new ArrayList<>();

        for (Role role : roles) {
            if (!role.isEnabled()) {
                continue;
            }

            RoleMatchResult result = matchRole(role, questionTokens);
            if (result.getScore() > 0) {
                results.add(result);
            }
        }

        // 按分数降序排序 (Sort by score descending)
        results.sort(Comparator.comparingDouble(RoleMatchResult::getScore).reversed());

        log.info(I18N.get("detector.keyword.matched", results.size()));
        return results;
    }

    /**
     * 匹配单个角色 (Match single role)
     *
     * @param role 角色 (Role)
     * @param questionTokens 问题词组 (Question tokens)
     * @return 匹配结果 (Match result)
     */
    private RoleMatchResult matchRole(Role role, Set<String> questionTokens) {
        Set<String> roleKeywords = role.getKeywords();
        if (roleKeywords == null || roleKeywords.isEmpty()) {
            return RoleMatchResult.builder()
                    .roleId(role.getId())
                    .roleName(role.getName())
                    .score(0.0)
                    .matchedKeywords(new ArrayList<>())
                    .method("keyword")
                    .build();
        }

        // 计算匹配的关键词 (Calculate matched keywords)
        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : roleKeywords) {
            String processedKeyword = keyword.toLowerCase().trim();
            if (questionTokens.contains(processedKeyword)) {
                matchedKeywords.add(keyword);
            }
        }

        // 计算匹配分数 (Calculate match score)
        double score = calculateScore(matchedKeywords.size(), roleKeywords.size(),
                                     questionTokens.size(), role.getWeight());

        return RoleMatchResult.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .score(score)
                .matchedKeywords(matchedKeywords)
                .method("keyword")
                .confidence(calculateConfidence(score))
                .build();
    }

    /**
     * 计算匹配分数 (Calculate match score)
     *
     * 评分公式 (Scoring formula):
     * score = (matchedCount / totalKeywords) * roleWeight * (1 + log(questionLength))
     *
     * @param matchedCount 匹配的关键词数量 (Matched keyword count)
     * @param totalKeywords 总关键词数量 (Total keyword count)
     * @param questionLength 问题长度 (Question length)
     * @param roleWeight 角色权重 (Role weight)
     * @return 匹配分数 (Match score)
     */
    private double calculateScore(int matchedCount, int totalKeywords,
                                  int questionLength, double roleWeight) {
        if (matchedCount == 0) {
            return 0.0;
        }

        // 基础分数：匹配率 (Base score: match rate)
        double baseScore = (double) matchedCount / totalKeywords;

        // 调整因子：问题长度 (Adjustment factor: question length)
        double lengthFactor = 1.0 + Math.log10(Math.max(questionLength, 1));

        // 最终分数 (Final score)
        return baseScore * roleWeight * lengthFactor;
    }

    /**
     * 计算置信度 (Calculate confidence)
     *
     * @param score 匹配分数 (Match score)
     * @return 置信度 (Confidence, 0-1)
     */
    private double calculateConfidence(double score) {
        // 使用 sigmoid 函数将分数映射到 0-1 (Use sigmoid to map score to 0-1)
        return 1.0 / (1.0 + Math.exp(-score));
    }

    /**
     * 预处理问题 (Preprocess question)
     *
     * @param question 原始问题 (Raw question)
     * @return 处理后的问题 (Processed question)
     */
    private String preprocessQuestion(String question) {
        // 转小写 (To lowercase)
        String processed = question.toLowerCase();

        // 移除标点符号 (Remove punctuation)
        processed = processed.replaceAll("[\\p{Punct}]+", " ");

        // 移除多余空格 (Remove extra spaces)
        processed = processed.replaceAll("\\s+", " ").trim();

        return processed;
    }

    /**
     * 分词 (Tokenize)
     *
     * @param text 文本 (Text)
     * @return 词组集合 (Token set)
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptySet();
        }

        // 简单的空格分词 (Simple space-based tokenization)
        String[] tokens = text.split("\\s+");

        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 获取最佳匹配角色 (Get best matched role)
     *
     * @param question 用户问题 (User question)
     * @param roles 候选角色列表 (Candidate roles)
     * @return 最佳匹配结果 (Best match result)
     */
    public Optional<RoleMatchResult> getBestMatch(String question, List<Role> roles) {
        List<RoleMatchResult> results = match(question, roles);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }
}

