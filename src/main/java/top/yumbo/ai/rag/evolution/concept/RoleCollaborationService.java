package top.yumbo.ai.rag.evolution.concept;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色协作服务 (Role Collaboration Service)
 * <p>
 * 实现核心功能：
 * 1. 举手抢答机制：多个角色对同一问题竞争响应
 * 2. 通用角色转发：General 角色识别并转发给专业角色
 * 3. 术业有专攻：只分配必须的概念给专业角色
 * 4. 分布式协作：为未来分布式节点做准备
 * <p>
 * (Implements core features:
 * 1. Raise hand mechanism: Multiple roles compete to respond to the same question
 * 2. General role forwarding: General role identifies and forwards to expert roles
 * 3. Specialization: Only assign essential concepts to expert roles
 * 4. Distributed collaboration: Prepare for future distributed nodes)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Slf4j
@Service
public class RoleCollaborationService {

    private final RoleKnowledgeService roleKnowledgeService;

    // 通用角色的概念索引：conceptId -> 专业角色列表
    // (General role's concept index: conceptId -> list of expert roles)
    private final Map<String, List<String>> conceptToExpertRolesIndex = new HashMap<>();

    @Autowired
    public RoleCollaborationService(RoleKnowledgeService roleKnowledgeService) {
        this.roleKnowledgeService = roleKnowledgeService;
        initializeGeneralRoleIndex();
    }

    /**
     * 初始化通用角色索引 (Initialize general role index)
     * <p>
     * 通用角色需要知道所有概念，但不深入理解，主要用于快速定位专业角色
     * (General role needs to know all concepts but not deeply, mainly for quick expert role location)
     */
    private void initializeGeneralRoleIndex() {
        try {
            // 遍历所有专业角色的概念，建立索引
            // (Iterate through all expert roles' concepts to build index)
            for (RoleKnowledgeBase.RoleType roleType : RoleKnowledgeBase.RoleType.values()) {
                if (roleType.isGeneralRole()) {
                    continue; // 跳过通用角色自己
                }

                List<MinimalConcept> concepts =
                        roleKnowledgeService.getConceptsForRole(roleType.getCode());

                for (MinimalConcept concept : concepts) {
                    conceptToExpertRolesIndex
                            .computeIfAbsent(concept.getId(), k -> new ArrayList<>())
                            .add(roleType.getCode());
                }
            }

            log.info(I18N.get("log.evolution.general_index_built"),
                    conceptToExpertRolesIndex.size());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.general_index_failed"), e);
        }
    }

    /**
     * 举手抢答：让所有角色竞争响应同一个问题 (Raise hand to answer: All roles compete for the same question)
     *
     * @param question 用户问题 (User question)
     * @return 所有角色的响应竞标列表，按综合得分排序 (List of all role bids, sorted by overall score)
     */
    public List<RoleResponseBid> collectRoleBids(String question) {
        List<RoleResponseBid> bids = new ArrayList<>();

        try {
            log.info(I18N.get("log.evolution.collecting_bids"), question);

            // 1. 让所有角色（包括通用角色）举手
            // (Let all roles including general role raise hands)
            for (RoleKnowledgeBase.RoleType roleType : RoleKnowledgeBase.RoleType.values()) {
                RoleResponseBid bid = createRoleBid(roleType, question);
                if (bid != null) {
                    bids.add(bid);
                }
            }

            // 2. 计算每个角色的综合得分
            // (Calculate overall score for each role)
            bids.forEach(RoleResponseBid::calculateOverallScore);

            // 3. 按得分排序（专业角色优先）
            // (Sort by score, expert roles first)
            bids.sort((a, b) -> {
                // 专业角色优先于通用角色
                if (a.isGeneralRole() != b.isGeneralRole()) {
                    return a.isGeneralRole() ? 1 : -1;
                }
                // 同类角色按得分排序
                return Double.compare(b.getOverallScore(), a.getOverallScore());
            });

            log.info(I18N.get("log.evolution.bids_collected"), bids.size());

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.bids_collect_failed"), e);
        }

        return bids;
    }

    /**
     * 创建角色竞标 (Create role bid)
     */
    private RoleResponseBid createRoleBid(RoleKnowledgeBase.RoleType roleType, String question) {
        try {
            String roleName = roleType.getCode();

            // 1. 搜索角色知识库中相关的概念
            // (Search for related concepts in role's knowledge base)
            List<MinimalConcept> relatedConcepts =
                    roleKnowledgeService.searchConceptsForRole(roleName, extractKeywords(question));

            if (relatedConcepts.isEmpty() && !roleType.isGeneralRole()) {
                // 专业角色没有相关概念，不参与竞标
                // (Expert role has no related concepts, don't bid)
                return null;
            }

            // 2. 计算置信度
            // (Calculate confidence)
            double confidence = calculateRoleConfidence(roleType, relatedConcepts, question);

            // 3. 构建竞标
            // (Build bid)
            RoleResponseBid bid = RoleResponseBid.builder()
                    .id(UUID.randomUUID().toString())
                    .question(question)
                    .roleName(roleName)
                    .isGeneralRole(roleType.isGeneralRole())
                    .confidenceScore(confidence)
                    .expertiseScore(roleType.getExpertiseLevel())
                    .relatedConceptCount(relatedConcepts.size())
                    .responseTime(LocalDateTime.now())
                    .build();

            // 4. 如果是通用角色，推荐专业角色
            // (If general role, recommend expert roles)
            if (roleType.isGeneralRole()) {
                List<String> expertRoles = findExpertRolesForQuestion(question, relatedConcepts);
                bid.setRecommendedExpertRoles(expertRoles);
                bid.setResponseType(RoleResponseBid.ResponseType.FORWARD);
                bid.setReason("通用角色：识别到相关概念，推荐专业角色处理");
            } else {
                bid.setResponseType(RoleResponseBid.ResponseType.DIRECT_ANSWER);
                bid.setReason(String.format("专业角色：拥有 %d 个相关概念，置信度 %.2f",
                        relatedConcepts.size(), confidence));
            }

            return bid;

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.bid_create_failed", roleType.getCode()), e);
            return null;
        }
    }

    /**
     * 选择最佳响应角色 (Select best responding role)
     * <p>
     * 策略：
     * 1. 如果有专业角色响应且置信度高（>0.7），选择得分最高的专业角色
     * 2. 如果专业角色置信度低，但通用角色推荐了专业角色，转发给推荐的角色
     * 3. 如果都不行，返回通用角色作为兜底
     * <p>
     * (Strategy:
     * 1. If expert role responds with high confidence (>0.7), select highest scoring expert
     * 2. If expert confidence low but general recommends experts, forward to recommended
     * 3. If neither works, return general role as fallback)
     */
    public RoleResponseBid selectBestRole(List<RoleResponseBid> bids) {
        if (bids == null || bids.isEmpty()) {
            return null;
        }

        // 1. 查找高置信度的专业角色
        // (Find high-confidence expert roles)
        Optional<RoleResponseBid> highConfidenceExpert = bids.stream()
                .filter(bid -> !bid.isGeneralRole())
                .filter(bid -> bid.getConfidenceScore() >= 0.7)
                .findFirst(); // 已按得分排序

        if (highConfidenceExpert.isPresent()) {
            log.info(I18N.get("log.evolution.expert_selected"),
                    highConfidenceExpert.get().getRoleName(),
                    highConfidenceExpert.get().getConfidenceScore());
            return highConfidenceExpert.get();
        }

        // 2. 查找通用角色的推荐
        // (Find general role's recommendation)
        Optional<RoleResponseBid> generalBid = bids.stream()
                .filter(RoleResponseBid::isGeneralRole)
                .findFirst();

        if (generalBid.isPresent() &&
                generalBid.get().getRecommendedExpertRoles() != null &&
                !generalBid.get().getRecommendedExpertRoles().isEmpty()) {

            String recommendedRole = generalBid.get().getRecommendedExpertRoles().getFirst();
            log.info(I18N.get("log.evolution.general_forward"), recommendedRole);

            // 创建转发的虚拟竞标
            // (Create forwarded virtual bid)

            return RoleResponseBid.builder()
                    .id(UUID.randomUUID().toString())
                    .question(generalBid.get().getQuestion())
                    .roleName(recommendedRole)
                    .isGeneralRole(false)
                    .confidenceScore(0.6) // 转发的置信度中等
                    .responseType(RoleResponseBid.ResponseType.FORWARD)
                    .reason("由通用角色转发")
                    .responseTime(LocalDateTime.now())
                    .build();
        }

        // 3. 兜底：返回得分最高的（可能是通用角色）
        // (Fallback: Return highest scoring role, might be general)
        log.info(I18N.get("log.evolution.fallback_selected"), bids.getFirst().getRoleName());
        return bids.getFirst();
    }

    /**
     * 分配概念到专业角色（术业有专攻）(Assign concepts to expert roles - specialization)
     * <p>
     * 策略：
     * 1. 专业角色：只分配必须的概念（高相关性 + 高置信度）
     * 2. 通用角色：接收所有概念的浅层索引（只记录概念ID和所属专业角色）
     * <p>
     * (Strategy:
     * 1. Expert roles: Only assign essential concepts (high relevance + high confidence)
     * 2. General role: Receive shallow index of all concepts (only concept ID and expert roles))
     */
    public void assignConceptsWithSpecialization(MinimalConcept concept) {
        try {
            List<String> conceptRoles = concept.getRoles();
            if (conceptRoles == null || conceptRoles.isEmpty()) {
                conceptRoles = List.of("developer"); // 默认
            }

            // 1. 只分配给相关的专业角色（深度理解）
            // (Only assign to relevant expert roles for deep understanding)
            for (String roleName : conceptRoles) {
                if ("general".equals(roleName)) {
                    continue; // 跳过通用角色
                }

                // 计算相关性权重
                // (Calculate relevance weight)
                double weight = calculateConceptRelevanceForRole(concept, roleName);

                // 只有高相关性的概念才分配给专业角色
                // (Only assign high-relevance concepts to expert roles)
                if (weight >= 0.6) {
                    roleKnowledgeService.assignConceptToRole(concept.getId(), roleName, weight);
                    log.debug(I18N.get("log.evolution.concept_assigned_expert"),
                            concept.getId(), roleName, weight);
                }
            }

            // 2. 添加到通用角色的索引（浅层理解）
            // (Add to general role's index for shallow understanding)
            conceptToExpertRolesIndex.put(concept.getId(), new ArrayList<>(conceptRoles));

            // 通用角色只存储索引，不深入理解
            // (General role only stores index, no deep understanding)
            roleKnowledgeService.assignConceptToRole(concept.getId(), "general", 0.3);

            log.debug(I18N.get("log.evolution.concept_indexed_general"),
                    concept.getId(), conceptRoles);

        } catch (Exception e) {
            log.error(I18N.get("log.evolution.concept_assign_failed"), e);
        }
    }

    /**
     * 为问题查找专业角色 (Find expert roles for question)
     */
    private List<String> findExpertRolesForQuestion(String question, List<MinimalConcept> relatedConcepts) {
        Map<String, Integer> roleScores = new HashMap<>();

        // 基于相关概念统计哪些专业角色最相关
        // (Based on related concepts, count which expert roles are most relevant)
        for (MinimalConcept concept : relatedConcepts) {
            List<String> expertRoles = conceptToExpertRolesIndex.get(concept.getId());
            if (expertRoles != null) {
                for (String role : expertRoles) {
                    if (!"general".equals(role)) {
                        roleScores.put(role, roleScores.getOrDefault(role, 0) + 1);
                    }
                }
            }
        }

        // 按相关概念数量排序
        // (Sort by number of related concepts)
        return roleScores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(3) // 最多推荐3个专业角色
                .collect(Collectors.toList());
    }

    /**
     * 计算角色对问题的置信度 (Calculate role's confidence for question)
     */
    private double calculateRoleConfidence(RoleKnowledgeBase.RoleType roleType,
                                           List<MinimalConcept> relatedConcepts,
                                           String question) {
        if (relatedConcepts.isEmpty()) {
            return 0.0;
        }

        // 基于相关概念的数量和平均置信度计算
        // (Calculate based on number of related concepts and average confidence)
        double avgConceptConfidence = relatedConcepts.stream()
                .mapToDouble(MinimalConcept::getConfidence)
                .average()
                .orElse(0.0);

        double countBonus = Math.min(relatedConcepts.size() / 10.0, 0.2);

        return Math.min((avgConceptConfidence * 0.8) + countBonus, 1.0);
    }

    /**
     * 计算概念与角色的相关性 (Calculate concept relevance for role)
     */
    private double calculateConceptRelevanceForRole(MinimalConcept concept, String roleName) {
        double relevance = 0.5; // 基础相关性

        // 基于概念类型调整
        // (Adjust based on concept type)
        RoleKnowledgeBase.RoleType roleType = RoleKnowledgeBase.RoleType.fromCode(roleName);
        if (roleType != null) {
            List<MinimalConcept.ConceptType> focusedTypes = getFocusedTypesForRole(roleType);
            if (focusedTypes.contains(concept.getType())) {
                relevance += 0.3;
            }
        }

        // 基于概念置信度调整
        // (Adjust based on concept confidence)
        relevance += concept.getConfidence() * 0.2;

        return Math.min(relevance, 1.0);
    }

    /**
     * 获取角色关注的概念类型 (Get focused concept types for role)
     */
    private List<MinimalConcept.ConceptType> getFocusedTypesForRole(RoleKnowledgeBase.RoleType roleType) {
        return switch (roleType) {
            case DEVELOPER -> List.of(MinimalConcept.ConceptType.SKILL,
                    MinimalConcept.ConceptType.PROCESS,
                    MinimalConcept.ConceptType.FACT);
            case DEVOPS -> List.of(MinimalConcept.ConceptType.PROCESS,
                    MinimalConcept.ConceptType.SKILL,
                    MinimalConcept.ConceptType.RULE);
            case ARCHITECT -> List.of(MinimalConcept.ConceptType.DEFINITION,
                    MinimalConcept.ConceptType.RELATIONSHIP,
                    MinimalConcept.ConceptType.RULE);
            case RESEARCHER -> List.of(MinimalConcept.ConceptType.DEFINITION,
                    MinimalConcept.ConceptType.RELATIONSHIP,
                    MinimalConcept.ConceptType.FACT);
            case GENERAL ->
                // 通用角色对所有类型都有浅层理解
                    Arrays.asList(MinimalConcept.ConceptType.values());
            default -> Arrays.asList(MinimalConcept.ConceptType.values());
        };
    }

    /**
     * 从问题提取关键词 (Extract keywords from question)
     */
    private String extractKeywords(String question) {
        // 简化版：去除标点符号
        // (Simplified: Remove punctuation)
        return question.replaceAll("[?？！!。，,、；;：:\"'（）()]", " ").trim();
    }

    /**
     * 获取通用角色的概念索引统计 (Get general role's concept index statistics)
     */
    public Map<String, Object> getGeneralRoleIndexStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConceptsIndexed", conceptToExpertRolesIndex.size());

        // 统计每个专业角色的概念数
        // (Count concepts for each expert role)
        Map<String, Integer> roleConceptCounts = new HashMap<>();
        for (List<String> roles : conceptToExpertRolesIndex.values()) {
            for (String role : roles) {
                roleConceptCounts.put(role, roleConceptCounts.getOrDefault(role, 0) + 1);
            }
        }
        stats.put("conceptsByExpertRole", roleConceptCounts);

        return stats;
    }
}

