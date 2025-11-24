package top.yumbo.ai.rag.security;
import org.junit.jupiter.api.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class UserTest {
    @Test
    void testUserBuilder() {
        User user = User.builder()
            .userId("user_001")
            .username("testuser")
            .passwordHash("hash123")
            .roles(Set.of("USER", "ADMIN"))
            .enabled(true)
            .createdAt(System.currentTimeMillis())
            .build();
        assertNotNull(user);
        assertEquals("user_001", user.getUserId());
        assertEquals("testuser", user.getUsername());
        assertTrue(user.isEnabled());
        assertTrue(user.getRoles().contains("USER"));
        assertTrue(user.getRoles().contains("ADMIN"));
    }
    @Test
    void testUserSettersAndGetters() {
        User user = new User();
        user.setUserId("id123");
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setEnabled(false);
        assertEquals("id123", user.getUserId());
        assertEquals("alice", user.getUsername());
        assertFalse(user.isEnabled());
    }
}
