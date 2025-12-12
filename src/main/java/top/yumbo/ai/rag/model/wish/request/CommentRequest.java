package top.yumbo.ai.rag.model.wish.request;

import lombok.Data;

/**
 * 评论请求 (Comment Request)
 *
 * 添加评论的请求对象
 * (Request object for adding comment)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class CommentRequest {

    /**
     * 评论内容 (Comment content)
     */
    private String content;

    /**
     * 父评论 ID (Parent comment ID)
     * null 表示顶级评论
     * (null means top-level comment)
     */
    private Long parentId;

    /**
     * 用户 ID (User ID)
     * 临时使用，后续可以从 Session/Token 获取
     * (Temporary, can be obtained from Session/Token later)
     */
    private Long userId;

    /**
     * 用户名 (Username)
     */
    private String username;
}

