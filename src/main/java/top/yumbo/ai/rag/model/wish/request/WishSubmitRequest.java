package top.yumbo.ai.rag.model.wish.request;

import lombok.Data;

/**
 * 提交愿望请求 (Submit Wish Request)
 *
 * 用户提交新愿望的请求对象
 * (Request object for submitting new wish)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class WishSubmitRequest {

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
     */
    private String category;

    /**
     * 提交用户 ID (Submit user ID)
     * 临时使用，后续可以从 Session/Token 获取
     * (Temporary, can be obtained from Session/Token later)
     */
    private Long submitUserId;

    /**
     * 提交用户名 (Submit username)
     */
    private String submitUsername;
}

