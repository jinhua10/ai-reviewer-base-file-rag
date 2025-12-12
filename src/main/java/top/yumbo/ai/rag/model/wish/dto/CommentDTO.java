package top.yumbo.ai.rag.model.wish.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论 DTO (Comment DTO)
 *
 * 用于评论展示的数据传输对象
 * (Data transfer object for comment display)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class CommentDTO {

    private Long id;

    /**
     * 愿望 ID (Wish ID)
     */
    private Long wishId;

    /**
     * 用户 ID (User ID)
     */
    private Long userId;

    /**
     * 用户名 (Username)
     */
    private String username;

    /**
     * 父评论 ID (Parent comment ID)
     */
    private Long parentId;

    /**
     * 评论内容 (Comment content)
     */
    private String content;

    /**
     * 点赞数 (Like count)
     */
    private Integer likeCount;

    /**
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt;

    /**
     * 回复列表 (Replies)
     * 嵌套的子评论
     * (Nested child comments)
     */
    private List<CommentDTO> replies;
}

