package top.yumbo.ai.rag.audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计事件 (Audit event)
 * 记录系统操作的审计信息 (Records audit information for system operations)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * 事件ID (event ID)
     */
    private String eventId;

    /**
     * 事件类型 (event type)
     */
    private String eventType;

    /**
     * 用户ID (user ID)
     */
    private String userId;

    /**
     * 用户名 (username)
     */
    private String username;

    /**
     * 操作 (action)
     */
    private String action;

    /**
     * 资源 (resource)
     */
    private String resource;

    /**
     * 详情 (details)
     */
    private String details;

    /**
     * 是否成功 (whether successful)
     */
    private boolean success;

    /**
     * 时间戳 (timestamp)
     */
    private long timestamp;

    /**
     * IP地址 (IP address)
     */
    private String ipAddress;
}
