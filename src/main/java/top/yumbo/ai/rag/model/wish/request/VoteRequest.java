package top.yumbo.ai.rag.model.wish.request;

import lombok.Data;

/**
 * 投票请求 (Vote Request)
 *
 * 用户投票的请求对象
 * (Request object for user voting)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class VoteRequest {

    /**
     * 投票类型 (Vote type)
     * up: 赞成, down: 反对
     */
    private String voteType;

    /**
     * 用户 ID (User ID)
     * 临时使用，后续可以从 Session/Token 获取
     * (Temporary, can be obtained from Session/Token later)
     */
    private Long userId;
}

