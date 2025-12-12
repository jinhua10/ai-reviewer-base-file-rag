package top.yumbo.ai.rag.model.wish;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 愿望实体 (Wish Entity)
 *
 * 基于文档存储的愿望数据模型
 * (Document-based wish data model)
 *
 * 存储方式：作为 JSON 文档存储在文档管理系统中
 * (Storage: Stored as JSON document in document management system)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class Wish {

    /**
     * 愿望 ID (Wish ID)
     * 使用文档 ID
     * (Uses document ID)
     */
    private String id;

    /**
     * 标题 (Title)
     */
    private String title;

    /**
     * 描述 (Description)
     */
    private String description;

    /**
     * 分类 (Category)
     * 例如: feature, bug, improvement, documentation
     * (e.g.: feature, bug, improvement, documentation)
     */
    private String category;

    /**
     * 状态 (Status)
     * pending: 待处理
     * accepted: 已接受
     * rejected: 已拒绝
     * completed: 已完成
     */
    private String status = "pending";

    /**
     * 提交用户 ID (Submit user ID)
     */
    private String submitUserId;

    /**
     * 提交用户名 (Submit username)
     */
    private String submitUsername;

    /**
     * 投票数 (Vote count)
     * 赞成票 - 反对票
     */
    private Integer voteCount = 0;

    /**
     * 赞成票数 (Up votes)
     */
    private Integer upVotes = 0;

    /**
     * 反对票数 (Down votes)
     */
    private Integer downVotes = 0;

    /**
     * 投票记录 (Vote records)
     * Key: userId, Value: "up" or "down"
     */
    private Map<String, String> votes = new HashMap<>();

    /**
     * 评论数 (Comment count)
     */
    private Integer commentCount = 0;

    /**
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间 (Updated at)
     */
    private LocalDateTime updatedAt;

    /**
     * 转换为文档元数据 (Convert to document metadata)
     *
     * 用于存储到文档管理系统
     * (Used for storing in document management system)
     */
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "wish");
        metadata.put("wishId", id);
        metadata.put("title", title);
        metadata.put("category", category);
        metadata.put("status", status);
        metadata.put("submitUserId", submitUserId);
        metadata.put("submitUsername", submitUsername);
        metadata.put("voteCount", voteCount);
        metadata.put("upVotes", upVotes);
        metadata.put("downVotes", downVotes);
        metadata.put("commentCount", commentCount);
        metadata.put("createdAt", createdAt != null ? createdAt.toString() : null);
        metadata.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return metadata;
    }
}

