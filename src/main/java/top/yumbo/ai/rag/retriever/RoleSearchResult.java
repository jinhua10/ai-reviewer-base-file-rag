package top.yumbo.ai.rag.retriever;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yumbo.ai.rag.model.ScoredDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色搜索结果 (Role Search Result)
 *
 * 封装单个角色的搜索结果及其权重
 * (Encapsulates search result from a single role with its weight)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSearchResult {

    /**
     * 角色ID (Role ID)
     */
    private String roleId;

    /**
     * 角色名称 (Role name)
     */
    private String roleName;

    /**
     * 角色权重（检测置信度） (Role weight - detection confidence)
     */
    private double roleWeight;

    /**
     * 搜索到的文档列表 (List of found documents)
     */
    private List<ScoredDocument> documents;

    /**
     * 搜索耗时（毫秒） (Search time in milliseconds)
     */
    private long searchTimeMs;

    /**
     * 构造函数（基础版本） (Constructor - basic version)
     *
     * @param roleId 角色ID (Role ID)
     * @param roleWeight 角色权重 (Role weight)
     * @param documents 文档列表 (Document list)
     */
    public RoleSearchResult(String roleId, double roleWeight, List<ScoredDocument> documents) {
        this.roleId = roleId;
        this.roleWeight = roleWeight;
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    /**
     * 获取文档数量 (Get document count)
     *
     * @return 文档数量 (Document count)
     */
    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }

    /**
     * 是否有结果 (Has results)
     *
     * @return 是否有文档 (Whether has documents)
     */
    public boolean hasResults() {
        return documents != null && !documents.isEmpty();
    }

    /**
     * 获取平均分数 (Get average score)
     *
     * @return 平均分数 (Average score)
     */
    public double getAverageScore() {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }
        return documents.stream()
                .mapToDouble(ScoredDocument::getScore)
                .average()
                .orElse(0.0);
    }
}

