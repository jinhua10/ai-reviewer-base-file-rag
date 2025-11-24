package top.yumbo.ai.rag.security;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String username;
    private String passwordHash;
    private Set<String> roles;
    private boolean enabled;
    private long createdAt;
    private long lastLoginAt;
}
