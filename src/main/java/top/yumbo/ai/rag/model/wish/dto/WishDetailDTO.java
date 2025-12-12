package top.yumbo.ai.rag.model.wish.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 愿望详情 DTO (Wish Detail DTO)
 *
 * 用于详情页面的愿望数据传输对象
 * (Data transfer object for wish detail page)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class WishDetailDTO {

    private Long id;

    /**
     * 标题 (Title)
     */
    private String title;

    /**
     * 完整描述 (Full description)
     */
    private String description;

    /**
     * 分类 (Category)
     */
    private String category;

    /**
     * 状态 (Status)
     */
    private String status;

    /**
     * 提交用户 ID (Submit user ID)
     */
    private Long submitUserId;

    /**
     * 提交用户名 (Submit username)
     */
    private String submitUsername;

    /**
     * 投票数 (Vote count)
     */
    private Integer voteCount;

    /**
     * 赞成票数 (Up votes)
     */
    private Integer upVotes;

    /**
     * 反对票数 (Down votes)
     */
    private Integer downVotes;

    /**
     * 评论数 (Comment count)
     */
    private Integer commentCount;

    /**
     * 评论列表 (Comments)
     */
    private List<CommentDTO> comments;

    /**
     * 当前用户的投票类型 (Current user's vote type)
     * null: 未投票, "up": 赞成, "down": 反对
     */
    private String currentUserVote;

    /**
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (Updated at)
     */
    private LocalDateTime updatedAt;
}

