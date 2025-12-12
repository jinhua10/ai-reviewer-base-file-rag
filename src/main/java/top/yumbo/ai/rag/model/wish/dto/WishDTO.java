package top.yumbo.ai.rag.model.wish.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 愿望 DTO (Wish DTO)
 *
 * 用于列表展示的愿望数据传输对象
 * (Data transfer object for wish list display)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class WishDTO {

    private Long id;

    /**
     * 标题 (Title)
     */
    private String title;

    /**
     * 描述（摘要） (Description summary)
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
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (Updated at)
     */
    private LocalDateTime updatedAt;
}

