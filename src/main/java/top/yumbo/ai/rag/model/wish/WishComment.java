package top.yumbo.ai.rag.model.wish;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 愿望评论 (Wish Comment)
 *
 * 用户对愿望的评论，支持嵌套回复
 * (User comments on wishes, supports nested replies)
 *
 * 存储方式：作为 Wish 文档的一部分
 * (Storage: Stored as part of Wish document)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class WishComment {

    /**
     * 评论 ID (Comment ID)
     */
    private String id;

    /**
     * 用户 ID (User ID)
     */
    private String userId;

    /**
     * 用户名 (Username)
     */
    private String username;

    /**
     * 父评论 ID (Parent comment ID)
     * null 表示顶级评论，非 null 表示回复
     * (null means top-level comment, non-null means reply)
     */
    private String parentId;

    /**
     * 评论内容 (Comment content)
     */
    private String content;

    /**
     * 点赞数 (Like count)
     */
    private Integer likeCount = 0;

    /**
     * 点赞用户列表 (List of users who liked)
     */
    private Set<String> likedBy = new HashSet<>();

    /**
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 回复列表 (Replies)
     * 用于构建嵌套结构
     * (Used for building nested structure)
     */
    private List<WishComment> replies = new ArrayList<>();
}

