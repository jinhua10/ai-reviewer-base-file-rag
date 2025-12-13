package top.yumbo.ai.rag.spring.boot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.evolution.concept.MinimalConcept;
import top.yumbo.ai.rag.evolution.concept.RoleCollaborationService;
import top.yumbo.ai.rag.evolution.concept.RoleKnowledgeService;
import top.yumbo.ai.rag.evolution.concept.RoleResponseBid;
import top.yumbo.ai.rag.spring.boot.model.AIAnswer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色知识库问答服务 (Role Knowledge Base Q&A Service)
 *
 * 实现基于"术业有专攻"理念的智能协作问答系统
 * (Implements intelligent collaborative Q&A system based on "specialization" principle)
 *
 * 核心功能 (Core Features):
 * 1. 本地角色知识库查询（优先） (Local role knowledge base query - priority)
 * 2. 举手抢答机制（本地无答案时） (Bidding mechanism when local answer unavailable)
 * 3. 悬赏机制（大家都不懂时） (Bounty system when no one knows)
 * 4. 积分系统和贡献排行榜 (Credit system and contribution leaderboard)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class RoleKnowledgeQAService {

    private final RoleKnowledgeService roleKnowledgeService;
    private final RoleCollaborationService collaborationService;
    private final KnowledgeQAService qaService;  // 传统 RAG 服务（作为兜底）

    // 悬赏系统 (Bounty System)
    private final Map<String, BountyRequest> activeBounties = new HashMap<>();

    // 积分系统 (Credit System)
    private final Map<String, RoleCredit> roleCredits = new HashMap<>();

    @Autowired
    public RoleKnowledgeQAService(
            RoleKnowledgeService roleKnowledgeService,
            RoleCollaborationService collaborationService,
            KnowledgeQAService qaService) {
        this.roleKnowledgeService = roleKnowledgeService;
        this.collaborationService = collaborationService;
        this.qaService = qaService;

        // 初始化所有角色的积分
        initializeRoleCredits();
    }

    /**
     * 使用角色知识库回答问题 (Answer question using role knowledge base)
     *
     * 策略 (Strategy):
     * 1. 如果指定角色，优先使用该角色的本地知识库
     * 2. 如果是通用角色或未指定，举手抢答
     * 3. 如果大家都不懂（置信度低），发起悬赏
     *
     * @param question 问题
     * @param roleName 角色名称（可选）
     * @return AIAnswer
     */
    public AIAnswer askWithRole(String question, String roleName) {
        log.info("🎭 角色知识库问答：问题=[{}], 角色=[{}]", question, roleName);

        AIAnswer answer;

        try {
            // 策略 1: 指定角色的本地知识库查询
            if (roleName != null && !roleName.isEmpty() && !"general".equals(roleName)) {
                log.info("📚 使用指定角色 [{}] 的本地知识库", roleName);
                answer = queryLocalRoleKnowledge(question, roleName);

                // 如果本地知识库能回答（置信度 >= 0.6），直接返回
                if (answer.getHopeConfidence() >= 0.6) {
                    log.info("✅ 角色 [{}] 本地知识库成功回答，置信度: {}",
                        roleName, answer.getHopeConfidence());
                    return answer;
                }

                log.info("⚠️ 角色 [{}] 本地知识库置信度不足: {}",
                    roleName, answer.getHopeConfidence());
            }

            // 策略 2: 通用角色或本地无答案 -> 举手抢答
            log.info("🙋 发起举手抢答机制");
            List<RoleResponseBid> bids = collaborationService.collectRoleBids(question);

            if (!bids.isEmpty()) {
                // 选择最佳角色
                RoleResponseBid bestBid = collaborationService.selectBestRole(bids);

                if (bestBid != null && bestBid.getConfidenceScore() >= 0.6) {
                    log.info("🏆 选中角色: {}, 置信度: {}",
                        bestBid.getRoleName(), bestBid.getConfidenceScore());

                    // 使用选中角色的知识库
                    answer = queryLocalRoleKnowledge(question, bestBid.getRoleName());

                    // 给予积分奖励
                    rewardRole(bestBid.getRoleName(), 10, "成功回答问题");

                    return answer;
                }
            }

            // 策略 3: 大家都不懂 -> 发起悬赏
            log.warn("❓ 所有角色都无法回答，发起悬赏机制");
            answer = createBountyRequest(question, roleName);

        } catch (Exception e) {
            log.error("❌ 角色知识库问答失败", e);
            answer = new AIAnswer(
                "抱歉，角色知识库查询失败：" + e.getMessage(),
                Collections.emptyList(),
                0
            );
            answer.setStrategyUsed("error");
        }

        return answer;
    }

    /**
     * 查询本地角色知识库 (Query local role knowledge base)
     *
     * @param question 问题
     * @param roleName 角色名称
     * @return AIAnswer
     */
    private AIAnswer queryLocalRoleKnowledge(String question, String roleName) {
        log.info("🔍 查询角色 [{}] 的本地知识库", roleName);

        long startTime = System.currentTimeMillis();

        // 1. 从角色知识库搜索相关概念
        List<MinimalConcept> concepts =
            roleKnowledgeService.searchConceptsForRole(roleName, extractKeywords(question));

        log.info("📦 找到 {} 个相关概念", concepts.size());

        if (concepts.isEmpty()) {
            // 没有相关概念，置信度为 0
            AIAnswer answer = new AIAnswer(
                "本地知识库暂无相关信息",
                Collections.emptyList(),
                System.currentTimeMillis() - startTime
            );
            answer.setHopeConfidence(0.0);
            answer.setStrategyUsed("role:" + roleName + ":no_concept");
            return answer;
        }

        // 2. 计算平均置信度
        double avgConfidence = concepts.stream()
            .mapToDouble(MinimalConcept::getConfidence)
            .average()
            .orElse(0.0);

        // 3. 构建上下文（从概念中提取知识）
        String context = buildContextFromConcepts(concepts, roleName);

        // 4. 调用 LLM 生成答案
        // TODO: 这里应该调用 LLM 服务，当前简化实现
        String answer = generateAnswerWithContext(question, context, roleName, concepts);

        // 5. 构建响应
        // 设置来源
        List<String> sources = concepts.stream()
            .map(c -> c.getId() + ":" + c.getName())
            .limit(5)
            .collect(Collectors.toList());

        AIAnswer aiAnswer = new AIAnswer(
            answer,
            sources,
            System.currentTimeMillis() - startTime
        );
        aiAnswer.setHopeConfidence(avgConfidence);
        aiAnswer.setStrategyUsed("role:" + roleName + ":local");

        return aiAnswer;
    }

    /**
     * 从概念构建上下文 (Build context from concepts)
     */
    private String buildContextFromConcepts(List<MinimalConcept> concepts, String roleName) {
        StringBuilder context = new StringBuilder();
        context.append("作为 ").append(getRoleDisplayName(roleName)).append("，我掌握以下知识：\n\n");

        for (MinimalConcept concept : concepts) {
            context.append("- ").append(concept.getName());
            if (concept.getDescription() != null && !concept.getDescription().isEmpty()) {
                context.append(": ").append(concept.getDescription());
            }
            context.append(" (置信度: ").append(String.format("%.2f", concept.getConfidence())).append(")\n");
        }

        return context.toString();
    }

    /**
     * 使用上下文生成答案 (Generate answer with context)
     *
     * 当前简化实现：基于概念拼接答案
     * TODO: 后续集成 LLM 服务进行智能生成
     */
    private String generateAnswerWithContext(String question, String context,
                                            String roleName, List<MinimalConcept> concepts) {
        StringBuilder answer = new StringBuilder();

        answer.append("【").append(getRoleDisplayName(roleName)).append("回答】\n\n");

        if (concepts.size() == 1) {
            MinimalConcept concept = concepts.get(0);
            answer.append("根据我的专业知识，").append(concept.getName());
            if (concept.getDescription() != null) {
                answer.append("：").append(concept.getDescription());
            }
        } else {
            answer.append("根据我的专业知识，这个问题涉及以下几个方面：\n\n");
            for (int i = 0; i < Math.min(concepts.size(), 3); i++) {
                MinimalConcept concept = concepts.get(i);
                answer.append((i + 1)).append(". ").append(concept.getName());
                if (concept.getDescription() != null) {
                    answer.append("：").append(concept.getDescription());
                }
                answer.append("\n");
            }
        }

        answer.append("\n💡 提示：这是基于角色本地知识库的回答");

        // TODO: 集成 LLM 后的实现
        // String llmAnswer = llmService.generateWithContext(question, context, roleName);
        // return llmAnswer;

        return answer.toString();
    }

    /**
     * 创建悬赏请求 (Create bounty request)
     *
     * 当所有角色都无法回答时，发起悬赏让子节点主动学习
     */
    private AIAnswer createBountyRequest(String question, String requestingRole) {
        String bountyId = UUID.randomUUID().toString();

        BountyRequest bounty = new BountyRequest();
        bounty.setId(bountyId);
        bounty.setQuestion(question);
        bounty.setRequestingRole(requestingRole);
        bounty.setReward(50);  // 悬赏 50 积分
        bounty.setStatus("active");
        bounty.setCreatedAt(System.currentTimeMillis());
        bounty.setDeadline(System.currentTimeMillis() + 24 * 60 * 60 * 1000);  // 24小时有效

        activeBounties.put(bountyId, bounty);

        log.info("🎯 创建悬赏: ID={}, 问题={}, 奖励={}积分", bountyId, question, bounty.getReward());

        // 构建响应
        String answerText = String.format(
            """
            【悬赏中】
            
            这个问题暂时没有角色能够回答。
            
            🎯 悬赏ID: %s
            💰 奖励: %d 积分
            ⏰ 截止时间: 24小时
            
            欢迎各角色节点主动学习相关知识后提交答案！
            提交答案后将获得积分，用于优先实现愿望单需求。
            """,
            bountyId, bounty.getReward()
        );

        AIAnswer answer = new AIAnswer(answerText, Collections.emptyList(), 100);
        answer.setStrategyUsed("bounty:" + bountyId);
        answer.setHopeConfidence(0.0);
        answer.setSessionId(bountyId);

        return answer;
    }

    /**
     * 提交悬赏答案 (Submit bounty answer)
     *
     * 角色节点学习后提交答案获取积分
     */
    public BountySubmission submitBountyAnswer(String bountyId, String roleName,
                                              String answer, List<String> sources) {
        BountyRequest bounty = activeBounties.get(bountyId);

        if (bounty == null) {
            throw new IllegalArgumentException("悬赏不存在: " + bountyId);
        }

        if (!"active".equals(bounty.getStatus())) {
            throw new IllegalStateException("悬赏已关闭");
        }

        // 创建提交记录
        BountySubmission submission = new BountySubmission();
        submission.setId(UUID.randomUUID().toString());
        submission.setBountyId(bountyId);
        submission.setRoleName(roleName);
        submission.setAnswer(answer);
        submission.setSources(sources);
        submission.setSubmittedAt(System.currentTimeMillis());
        submission.setStatus("pending");

        bounty.getSubmissions().add(submission);

        log.info("📝 角色 [{}] 提交悬赏答案: bountyId={}", roleName, bountyId);

        // TODO: 这里可以加入审核机制，当前简化为自动通过
        approveSubmission(bountyId, submission.getId());

        return submission;
    }

    /**
     * 批准悬赏提交 (Approve bounty submission)
     */
    private void approveSubmission(String bountyId, String submissionId) {
        BountyRequest bounty = activeBounties.get(bountyId);
        if (bounty == null) return;

        BountySubmission submission = bounty.getSubmissions().stream()
            .filter(s -> s.getId().equals(submissionId))
            .findFirst()
            .orElse(null);

        if (submission == null) return;

        // 批准提交
        submission.setStatus("approved");
        submission.setApprovedAt(System.currentTimeMillis());

        // 关闭悬赏
        bounty.setStatus("closed");
        bounty.setWinnerRole(submission.getRoleName());

        // 奖励积分
        rewardRole(submission.getRoleName(), bounty.getReward(),
            "完成悬赏：" + bounty.getQuestion());

        log.info("🎊 批准悬赏提交: bountyId={}, 获胜角色={}, 奖励={}积分",
            bountyId, submission.getRoleName(), bounty.getReward());
    }

    /**
     * 奖励角色积分 (Reward role credits)
     */
    private void rewardRole(String roleName, int credits, String reason) {
        RoleCredit roleCredit = roleCredits.computeIfAbsent(roleName, k -> {
            RoleCredit rc = new RoleCredit();
            rc.setRoleName(roleName);
            rc.setTotalCredits(0);
            rc.setAnswerCount(0);
            rc.setBountyWins(0);
            return rc;
        });

        roleCredit.setTotalCredits(roleCredit.getTotalCredits() + credits);
        roleCredit.setAnswerCount(roleCredit.getAnswerCount() + 1);

        if (reason.contains("悬赏")) {
            roleCredit.setBountyWins(roleCredit.getBountyWins() + 1);
        }

        roleCredit.setLastRewardTime(System.currentTimeMillis());
        roleCredit.setLastRewardReason(reason);

        log.info("🎁 奖励角色 [{}] {} 积分：{}", roleName, credits, reason);
    }

    /**
     * 获取角色贡献排行榜 (Get role contribution leaderboard)
     */
    public List<RoleCredit> getLeaderboard() {
        return roleCredits.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalCredits(), a.getTotalCredits()))
            .collect(Collectors.toList());
    }

    /**
     * 获取活跃悬赏列表 (Get active bounties)
     */
    public List<BountyRequest> getActiveBounties() {
        return activeBounties.values().stream()
            .filter(b -> "active".equals(b.getStatus()))
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .collect(Collectors.toList());
    }

    /**
     * 初始化角色积分 (Initialize role credits)
     */
    private void initializeRoleCredits() {
        String[] roles = {"general", "developer", "devops", "architect",
                         "researcher", "product_manager", "data_scientist",
                         "security_engineer", "tester"};

        for (String role : roles) {
            RoleCredit credit = new RoleCredit();
            credit.setRoleName(role);
            credit.setTotalCredits(0);
            credit.setAnswerCount(0);
            credit.setBountyWins(0);
            roleCredits.put(role, credit);
        }
    }

    /**
     * 提取关键词 (Extract keywords)
     */
    private String extractKeywords(String question) {
        // 简化版：去除标点符号
        return question.replaceAll("[?？！!。，,、；;：:\"\"''（）()]", " ").trim();
    }

    /**
     * 获取角色显示名称 (Get role display name)
     */
    private String getRoleDisplayName(String roleCode) {
        Map<String, String> names = Map.of(
            "general", "通用角色",
            "developer", "开发者",
            "devops", "运维工程师",
            "architect", "架构师",
            "researcher", "研究员",
            "product_manager", "产品经理",
            "data_scientist", "数据科学家",
            "security_engineer", "安全工程师",
            "tester", "测试工程师"
        );
        return names.getOrDefault(roleCode, roleCode);
    }

    // ========== 内部数据模型 (Internal Data Models) ==========

    /**
     * 悬赏请求 (Bounty Request)
     *
     * 当所有角色都无法回答问题时创建悬赏，激励子节点主动学习
     * (Created when no role can answer, incentivizing nodes to learn actively)
     */
    @Data
    public static class BountyRequest {
        private String id;                                      // 悬赏ID (Bounty ID)
        private String question;                                // 问题内容 (Question content)
        private String requestingRole;                          // 请求角色 (Requesting role)
        private int reward;                                     // 奖励积分 (Reward credits)
        private String status;                                  // 状态: active, closed, expired
        private long createdAt;                                 // 创建时间 (Creation time)
        private long deadline;                                  // 截止时间 (Deadline)
        private String winnerRole;                              // 获胜角色 (Winner role)
        private List<BountySubmission> submissions = new ArrayList<>();  // 提交列表 (Submissions)
    }

    /**
     * 悬赏提交 (Bounty Submission)
     *
     * 角色节点学习后提交的答案
     * (Answer submitted by role node after learning)
     */
    @Data
    public static class BountySubmission {
        private String id;                                      // 提交ID (Submission ID)
        private String bountyId;                                // 悬赏ID (Bounty ID)
        private String roleName;                                // 提交角色 (Submitting role)
        private String answer;                                  // 答案内容 (Answer content)
        private List<String> sources;                           // 资料来源 (Sources)
        private long submittedAt;                               // 提交时间 (Submission time)
        private String status;                                  // 状态: pending, approved, rejected
        private long approvedAt;                                // 批准时间 (Approval time)
    }

    /**
     * 角色积分 (Role Credit)
     *
     * 记录角色的贡献和积分信息
     * (Records role's contribution and credit information)
     */
    @Data
    public static class RoleCredit {
        private String roleName;                                // 角色名称 (Role name)
        private int totalCredits;                               // 总积分 (Total credits)
        private int answerCount;                                // 回答次数 (Answer count)
        private int bountyWins;                                 // 悬赏获胜次数 (Bounty wins)
        private long lastRewardTime;                            // 最后奖励时间 (Last reward time)
        private String lastRewardReason;                        // 最后奖励原因 (Last reward reason)
    }
}

