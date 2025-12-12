package top.yumbo.ai.rag.model.wish.dto;

import lombok.Data;

/**
 * 投票结果 DTO (Vote Result DTO)
 *
 * 投票操作的返回结果
 * (Result of vote operation)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class VoteResultDTO {

    /**
     * 是否成功 (Success)
     */
    private Boolean success;

    /**
     * 消息 (Message)
     */
    private String message;

    /**
     * 新的投票数 (New vote count)
     */
    private Integer voteCount;

    /**
     * 新的赞成票数 (New up votes)
     */
    private Integer upVotes;

    /**
     * 新的反对票数 (New down votes)
     */
    private Integer downVotes;

    /**
     * 用户当前的投票类型 (User's current vote type)
     */
    private String currentVoteType;
}

