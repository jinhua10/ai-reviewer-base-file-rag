package top.yumbo.ai.rag.role.detector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;
import top.yumbo.ai.rag.role.Role;

import java.util.*;

/**
 * AI 角色分析器 (AI Role Analyzer)
 *
 * 使用 LLM 进行智能角色检测
 * (Uses LLM for intelligent role detection)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class AIRoleAnalyzer {

    @Autowired(required = false)
    private LLMClient llmClient;

    @Value("${rag.role.ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * 分析角色 (Analyze role)
     *
     * @param question 用户问题 (User question)
     * @param roles 候选角色列表 (Candidate roles)
     * @return 角色匹配结果列表 (Role match results)
     */
    public List<RoleMatchResult> analyze(String question, List<Role> roles) {
        if (!aiEnabled) {
            log.debug(I18N.get("detector.ai.disabled"));
            return Collections.emptyList();
        }

        if (llmClient == null) {
            log.warn(I18N.get("detector.ai.client.unavailable"));
            return Collections.emptyList();
        }

        if (question == null || question.trim().isEmpty()) {
            log.warn(I18N.get("detector.ai.question.empty"));
            return Collections.emptyList();
        }

        if (roles == null || roles.isEmpty()) {
            log.warn(I18N.get("detector.ai.roles.empty"));
            return Collections.emptyList();
        }

        try {
            log.info(I18N.get("detector.ai.analyzing", question));

            // 构建提示词 (Build prompt)
            String prompt = buildPrompt(question, roles);

            // 调用 LLM (Call LLM)
            String response = llmClient.generate(prompt);

            // 解析响应 (Parse response)
            List<RoleMatchResult> results = parseResponse(response, roles);

            log.info(I18N.get("detector.ai.analyzed", results.size()));
            return results;

        } catch (Exception e) {
            log.error(I18N.get("detector.ai.error", e.getMessage()), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建提示词 (Build prompt)
     *
     * @param question 用户问题 (User question)
     * @param roles 候选角色列表 (Candidate roles)
     * @return 提示词 (Prompt)
     */
    private String buildPrompt(String question, List<Role> roles) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个角色识别专家。请分析以下问题，判断它最适合哪个角色来回答。\n\n");
        prompt.append("You are a role identification expert. Please analyze the following question and determine which role is most suitable to answer it.\n\n");

        prompt.append("问题 (Question): ").append(question).append("\n\n");

        prompt.append("可选角色 (Available Roles):\n");
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            prompt.append(i + 1).append(". ").append(role.getName()).append("\n");
            prompt.append("   描述 (Description): ").append(role.getDescription()).append("\n");
            prompt.append("   关键词 (Keywords): ").append(String.join(", ", role.getKeywords())).append("\n\n");
        }

        prompt.append("请按以下格式返回结果 (Please return results in the following format):\n");
        prompt.append("角色ID|置信度|原因\n");
        prompt.append("RoleID|Confidence|Reason\n\n");
        prompt.append("示例 (Example): developer|0.95|问题涉及代码和编程\n");
        prompt.append("可以返回多个角色，按置信度降序排列。置信度范围 0-1。\n");
        prompt.append("You can return multiple roles, sorted by confidence descending. Confidence range 0-1.\n");

        return prompt.toString();
    }

    /**
     * 解析 LLM 响应 (Parse LLM response)
     *
     * @param response LLM 响应 (LLM response)
     * @param roles 角色列表 (Role list)
     * @return 角色匹配结果列表 (Role match results)
     */
    private List<RoleMatchResult> parseResponse(String response, List<Role> roles) {
        List<RoleMatchResult> results = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            return results;
        }

        // 创建角色映射 (Create role map)
        Map<String, Role> roleMap = new HashMap<>();
        for (Role role : roles) {
            roleMap.put(role.getId(), role);
        }

        // 按行解析 (Parse by lines)
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            // 查找包含 | 的行 (Find lines with |)
            if (!line.contains("|")) {
                continue;
            }

            try {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String roleId = parts[0].trim();
                    double confidence = Double.parseDouble(parts[1].trim());
                    String reason = parts.length >= 3 ? parts[2].trim() : "";

                    Role role = roleMap.get(roleId);
                    if (role != null) {
                        RoleMatchResult result = RoleMatchResult.builder()
                                .roleId(roleId)
                                .roleName(role.getName())
                                .score(confidence * 10)  // 转换为分数 (Convert to score)
                                .confidence(confidence)
                                .method("ai")
                                .reason(reason)
                                .build();

                        results.add(result);
                    }
                }
            } catch (Exception e) {
                log.debug(I18N.get("detector.ai.parse.line.failed", line, e.getMessage()));
            }
        }

        // 按置信度降序排序 (Sort by confidence descending)
        results.sort(Comparator.comparingDouble(RoleMatchResult::getConfidence).reversed());

        return results;
    }

    /**
     * 获取最佳匹配角色 (Get best matched role)
     *
     * @param question 用户问题 (User question)
     * @param roles 候选角色列表 (Candidate roles)
     * @return 最佳匹配结果 (Best match result)
     */
    public Optional<RoleMatchResult> getBestMatch(String question, List<Role> roles) {
        List<RoleMatchResult> results = analyze(question, roles);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    /**
     * 检查 AI 是否可用 (Check if AI is available)
     *
     * @return 是否可用 (Whether available)
     */
    public boolean isAvailable() {
        return aiEnabled && llmClient != null;
    }
}

