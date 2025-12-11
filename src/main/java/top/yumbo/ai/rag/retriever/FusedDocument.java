package top.yumbo.ai.rag.retriever;

import lombok.Data;
import top.yumbo.ai.rag.model.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 融合文档 (Fused Document)
 *
 * 记录文档在多角色检索中的综合信息
 * (Records comprehensive information of a document across multiple role retrievals)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
public class FusedDocument {

    /**
     * 原始文档 (Original document)
     */
    private Document document;

    /**
     * 综合得分 (Total score)
     */
    private double totalScore;

    /**
     * 来源角色列表 (Source roles)
     */
    private List<String> sourceRoles;

    /**
     * 角色分数详情 (Role score details)
     */
    private List<RoleScoreDetail> roleScores;

    /**
     * 构造函数 (Constructor)
     *
     * @param document 文档 (Document)
     * @param score 初始分数 (Initial score)
     * @param roleId 角色ID (Role ID)
     */
    public FusedDocument(Document document, double score, String roleId) {
        this.document = document;
        this.totalScore = score;
        this.sourceRoles = new ArrayList<>();
        this.sourceRoles.add(roleId);
        this.roleScores = new ArrayList<>();
        this.roleScores.add(new RoleScoreDetail(roleId, score));
    }

    /**
     * 添加角色分数 (Add role score)
     *
     * @param score 分数 (Score)
     * @param roleId 角色ID (Role ID)
     */
    public void addRoleScore(double score, String roleId) {
        this.totalScore += score;
        if (!this.sourceRoles.contains(roleId)) {
            this.sourceRoles.add(roleId);
        }
        this.roleScores.add(new RoleScoreDetail(roleId, score));
    }

    /**
     * 获取来源角色数量 (Get source role count)
     *
     * @return 角色数量 (Role count)
     */
    public int getSourceRoleCount() {
        return sourceRoles.size();
    }

    /**
     * 是否来自多个角色 (Is from multiple roles)
     *
     * @return 是否多角色 (Whether from multiple roles)
     */
    public boolean isFromMultipleRoles() {
        return sourceRoles.size() > 1;
    }

    /**
     * 角色分数详情 (Role Score Detail)
     */
    @Data
    public static class RoleScoreDetail {
        /**
         * 角色ID (Role ID)
         */
        private String roleId;

        /**
         * 分数 (Score)
         */
        private double score;

        public RoleScoreDetail(String roleId, double score) {
            this.roleId = roleId;
            this.score = score;
        }
    }
}

