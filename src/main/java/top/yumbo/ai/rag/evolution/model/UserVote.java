package top.yumbo.ai.rag.evolution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户投票记录 (User Vote Record)
 *
 * 记录用户对概念冲突的投票
 * (Records user votes on concept conflicts)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVote {

    /**
     * 投票ID (Vote ID)
     */
    private String id;

    /**
     * 冲突ID (Conflict ID)
     */
    private String conflictId;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 选择 (Choice: A or B)
     */
    private String choice;

    /**
     * 投票原因/备注 (Reason/notes)
     */
    private String reason;

    /**
     * 投票时间 (Vote time)
     */
    private LocalDateTime votedAt;

    /**
     * 用户角色/权重 (User role/weight)
     */
    private UserRole role;

    /**
     * IP地址 (IP address)
     */
    private String ipAddress;

    /**
     * 用户角色枚举 (User role enum)
     */
    public enum UserRole {
        ANONYMOUS(1.0, "匿名用户", "Anonymous"),
        REGISTERED(1.5, "注册用户", "Registered"),
        EXPERT(3.0, "专家", "Expert"),
        ADMIN(5.0, "管理员", "Admin");

        private final double weight;
        private final String zhName;
        private final String enName;

        UserRole(double weight, String zhName, String enName) {
            this.weight = weight;
            this.zhName = zhName;
            this.enName = enName;
        }

        public double getWeight() {
            return weight;
        }

        public String getZhName() {
            return zhName;
        }

        public String getEnName() {
            return enName;
        }
    }

    /**
     * 获取投票权重 (Get vote weight)
     */
    public double getWeight() {
        return role != null ? role.getWeight() : 1.0;
    }
}

