package top.yumbo.ai.rag.company;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 贡献工作流 (Contribution Workflow)
 *
 * 功能 (Features):
 * 1. 管理知识贡献流程 (Manage knowledge contribution process)
 * 2. 自动筛选合格知识 (Auto filter qualified knowledge)
 * 3. 批量提交 (Batch submission)
 * 4. 追踪审核状态 (Track review status)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class ContributionWorkflow {

    /**
     * 公司知识库客户端 (Company KB client)
     */
    private final CompanyKBClient client;

    /**
     * 当前用户ID (Current user ID)
     */
    private String userId;

    /**
     * 待贡献知识队列 (Pending contribution queue)
     */
    private final List<CompanyKBClient.Knowledge> pendingQueue = new ArrayList<>();

    /**
     * 贡献历史 (Contribution history)
     */
    private final Map<String, ContributionRecord> contributionHistory = new ConcurrentHashMap<>();

    /**
     * 自动贡献阈值 (Auto contribution threshold)
     */
    private double autoContributeThreshold = 0.8;

    // ========== 初始化 (Initialization) ==========

    public ContributionWorkflow(CompanyKBClient client, String userId) {
        this.client = client;
        this.userId = userId;

        log.info(I18N.get("company.workflow.initialized"), userId);
    }

    // ========== 知识添加 (Knowledge Addition) ==========

    /**
     * 添加知识到待贡献队列 (Add knowledge to pending queue)
     *
     * @param knowledge 知识 (Knowledge)
     */
    public void addToPendingQueue(CompanyKBClient.Knowledge knowledge) {
        // 检查是否符合贡献标准 (Check if meets contribution criteria)
        if (isEligibleForContribution(knowledge)) {
            pendingQueue.add(knowledge);
            log.info(I18N.get("company.workflow.added_to_queue"),
                knowledge.getId(), pendingQueue.size());
        } else {
            log.debug(I18N.get("company.workflow.not_eligible"), knowledge.getId());
        }
    }

    /**
     * 批量添加知识 (Batch add knowledge)
     *
     * @param knowledgeList 知识列表 (Knowledge list)
     */
    public void batchAddToPendingQueue(List<CompanyKBClient.Knowledge> knowledgeList) {
        int addedCount = 0;

        for (CompanyKBClient.Knowledge knowledge : knowledgeList) {
            if (isEligibleForContribution(knowledge)) {
                pendingQueue.add(knowledge);
                addedCount++;
            }
        }

        log.info(I18N.get("company.workflow.batch_added"),
            addedCount, knowledgeList.size(), pendingQueue.size());
    }

    /**
     * 检查是否符合贡献标准 (Check if eligible for contribution)
     */
    private boolean isEligibleForContribution(CompanyKBClient.Knowledge knowledge) {
        // 1. 质量分数检查 (Quality score check)
        if (knowledge.getQualityScore() < autoContributeThreshold) {
            return false;
        }

        // 2. 验证数检查 (Verification count check)
        if (knowledge.getVerificationCount() < 3) {
            return false;
        }

        // 3. 角色标签检查 (Role tag check)
        if (knowledge.getRoleId() == null || knowledge.getRoleId().isEmpty()) {
            return false;
        }

        // 4. 内容完整性检查 (Content completeness check)
        if (knowledge.getQuestion() == null || knowledge.getAnswer() == null) {
            return false;
        }

        return true;
    }

    // ========== 提交贡献 (Submit Contribution) ==========

    /**
     * 提交待贡献知识 (Submit pending knowledge)
     *
     * @return 贡献结果 (Contribution result)
     */
    public CompanyKBClient.ContributionResult submitPendingQueue() {
        if (pendingQueue.isEmpty()) {
            log.warn(I18N.get("company.workflow.queue_empty"));
            return null;
        }

        try {
            log.info(I18N.get("company.workflow.submitting"), pendingQueue.size());

            // 1. 提交到公司服务器 (Submit to company server)
            CompanyKBClient.ContributionResult result = client.contribute(new ArrayList<>(pendingQueue));

            // 2. 记录贡献历史 (Record contribution history)
            recordContribution(result);

            // 3. 清空待贡献队列 (Clear pending queue)
            pendingQueue.clear();

            log.info(I18N.get("company.workflow.submitted"),
                result.getAcceptedCount(), result.getTotalCount());

            return result;

        } catch (Exception e) {
            log.error(I18N.get("company.workflow.submit_failed"), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 手动提交单个知识 (Manually submit single knowledge)
     *
     * @param knowledge 知识 (Knowledge)
     * @return 贡献结果 (Contribution result)
     */
    public CompanyKBClient.ContributionResult submitSingle(CompanyKBClient.Knowledge knowledge) {
        try {
            log.info(I18N.get("company.workflow.submitting_single"), knowledge.getId());

            List<CompanyKBClient.Knowledge> singleList = List.of(knowledge);
            CompanyKBClient.ContributionResult result = client.contribute(singleList);

            recordContribution(result);

            return result;

        } catch (Exception e) {
            log.error(I18N.get("company.workflow.submit_failed"), e.getMessage(), e);
            return null;
        }
    }

    // ========== 贡献记录 (Contribution Record) ==========

    /**
     * 记录贡献 (Record contribution)
     */
    private void recordContribution(CompanyKBClient.ContributionResult result) {
        ContributionRecord record = new ContributionRecord();
        record.setUserId(userId);
        record.setSubmitTime(result.getSubmitTime());
        record.setTotalCount(result.getTotalCount());
        record.setAcceptedCount(result.getAcceptedCount());
        record.setRejectedCount(result.getRejectedCount());
        record.setStatus(result.getStatus());

        String recordId = UUID.randomUUID().toString();
        contributionHistory.put(recordId, record);

        log.debug(I18N.get("company.workflow.recorded"), recordId);
    }

    /**
     * 获取贡献历史 (Get contribution history)
     *
     * @return 贡献记录列表 (Contribution record list)
     */
    public List<ContributionRecord> getContributionHistory() {
        return new ArrayList<>(contributionHistory.values());
    }

    /**
     * 获取最近的贡献记录 (Get recent contribution records)
     *
     * @param limit 限制数量 (Limit)
     * @return 贡献记录列表 (Contribution record list)
     */
    public List<ContributionRecord> getRecentContributions(int limit) {
        return contributionHistory.values().stream()
            .sorted((a, b) -> b.getSubmitTime().compareTo(a.getSubmitTime()))
            .limit(limit)
            .toList();
    }

    // ========== 自动贡献 (Auto Contribution) ==========

    /**
     * 启动自动贡献 (Start auto contribution)
     * 当队列达到一定数量时自动提交
     */
    public void startAutoContribution(int queueSizeThreshold) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟检查一次 (Check every minute)

                    if (pendingQueue.size() >= queueSizeThreshold) {
                        log.info(I18N.get("company.workflow.auto_contributing"),
                            pendingQueue.size());
                        submitPendingQueue();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error(I18N.get("company.workflow.auto_contribute_error"),
                        e.getMessage(), e);
                }
            }
        }).start();

        log.info(I18N.get("company.workflow.auto_contribution_started"), queueSizeThreshold);
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取贡献统计 (Get contribution statistics)
     */
    public ContributionStats getStats() {
        ContributionStats stats = new ContributionStats();
        stats.setPendingCount(pendingQueue.size());
        stats.setTotalContributions(contributionHistory.size());

        int totalAccepted = 0;
        int totalRejected = 0;

        for (ContributionRecord record : contributionHistory.values()) {
            totalAccepted += record.getAcceptedCount();
            totalRejected += record.getRejectedCount();
        }

        stats.setTotalAccepted(totalAccepted);
        stats.setTotalRejected(totalRejected);

        // 计算接受率 (Calculate acceptance rate)
        int total = totalAccepted + totalRejected;
        if (total > 0) {
            stats.setAcceptanceRate((double) totalAccepted / total);
        }

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 贡献记录 (Contribution Record)
     */
    @Data
    public static class ContributionRecord {
        private String userId;                      // 用户ID
        private LocalDateTime submitTime;           // 提交时间
        private int totalCount;                     // 总数
        private int acceptedCount;                  // 接受数
        private int rejectedCount;                  // 拒绝数
        private CompanyKBClient.ContributionStatus status; // 状态
    }

    /**
     * 贡献统计 (Contribution Statistics)
     */
    @Data
    public static class ContributionStats {
        private int pendingCount;        // 待贡献数
        private int totalContributions;  // 总贡献次数
        private int totalAccepted;       // 总接受数
        private int totalRejected;       // 总拒绝数
        private double acceptanceRate;   // 接受率
    }
}

