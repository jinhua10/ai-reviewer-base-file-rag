package top.yumbo.ai.rag.audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String eventId;
    private String eventType;
    private String userId;
    private String username;
    private String action;
    private String resource;
    private String details;
    private boolean success;
    private long timestamp;
    private String ipAddress;
}
