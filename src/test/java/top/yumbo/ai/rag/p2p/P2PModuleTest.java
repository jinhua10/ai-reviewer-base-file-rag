package top.yumbo.ai.rag.p2p;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2P 协作网络测试
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@DisplayName("Phase 4.5.2 - P2P 协作网络测试")
class P2PModuleTest {

    private ConnectionCodeGenerator codeGenerator;
    private P2PEncryptionHandler encryptionHandler;
    private P2PCollaborationManager manager;

    private static final String TEST_USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        codeGenerator = new ConnectionCodeGenerator();
        encryptionHandler = new P2PEncryptionHandler();
        manager = new P2PCollaborationManager(TEST_USER_ID);
    }

    // ========== ConnectionCodeGenerator 测试 ==========

    @Test
    @DisplayName("测试连接码生成")
    void testGenerateCode() {
        // When
        String code = codeGenerator.generateCode(TEST_USER_ID, null);

        // Then
        assertNotNull(code);
        assertTrue(code.matches("[A-Z]{6}-[A-Z]{6}-\\d{3}"));
    }

    @Test
    @DisplayName("测试连接码验证")
    void testValidateCode() {
        // Given
        String code = codeGenerator.generateCode(TEST_USER_ID, null);

        // When
        boolean valid = codeGenerator.validateCode(code);

        // Then
        assertTrue(valid);
    }

    @Test
    @DisplayName("测试连接码使用")
    void testUseCode() {
        // Given
        String code = codeGenerator.generateCode(TEST_USER_ID, null);

        // When
        var info = codeGenerator.useCode(code);

        // Then
        assertNotNull(info);
        assertEquals(TEST_USER_ID, info.getUserId());
        assertTrue(info.isUsed());

        // 再次使用应该失败
        var info2 = codeGenerator.useCode(code);
        assertNull(info2);
    }

    @Test
    @DisplayName("测试连接码统计")
    void testCodeStats() {
        // Given
        codeGenerator.generateCode(TEST_USER_ID, null);
        codeGenerator.generateCode("user-002", null);

        // When
        var stats = codeGenerator.getStats();

        // Then
        assertNotNull(stats);
        assertEquals(2, stats.getTotalCodes());
    }

    // ========== P2PEncryptionHandler 测试 ==========

    @Test
    @DisplayName("测试加密和解密")
    void testEncryptDecrypt() {
        // Given
        String plaintext = "Hello, P2P!";

        // When
        String encrypted = encryptionHandler.encrypt(plaintext);
        String decrypted = encryptionHandler.decrypt(encrypted);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("测试获取公钥")
    void testGetPublicKey() {
        // When
        String publicKey = encryptionHandler.getPublicKey();

        // Then
        assertNotNull(publicKey);
        assertFalse(publicKey.isEmpty());
    }

    @Test
    @DisplayName("测试数字签名")
    void testSignAndVerify() {
        // Given
        String data = "Test data for signing";

        // When
        String signature = encryptionHandler.sign(data);
        boolean verified = encryptionHandler.verify(
            data,
            signature,
            encryptionHandler.getPublicKey()
        );

        // Then
        assertNotNull(signature);
        assertTrue(verified);

        // 修改数据后验证应该失败
        boolean verifiedWrong = encryptionHandler.verify(
            "Wrong data",
            signature,
            encryptionHandler.getPublicKey()
        );
        assertFalse(verifiedWrong);
    }

    // ========== P2PCollaborationManager 测试 ==========

    @Test
    @DisplayName("测试管理器初始化")
    void testManagerInit() {
        // Then
        assertNotNull(manager);
        assertEquals(TEST_USER_ID, manager.getCurrentUserId());
    }

    @Test
    @DisplayName("测试生成连接码")
    void testManagerGenerateCode() {
        // When
        String code = manager.generateConnectionCode();

        // Then
        assertNotNull(code);
        assertTrue(code.matches("[A-Z]{6}-[A-Z]{6}-\\d{3}"));
    }

    @Test
    @DisplayName("测试获取已连接同事")
    void testGetConnectedPeers() {
        // When
        var peers = manager.getConnectedPeers();

        // Then
        assertNotNull(peers);
        assertEquals(0, peers.size());
    }

    @Test
    @DisplayName("测试协作统计")
    void testCollaborationStats() {
        // When
        var stats = manager.getStats();

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.getTotalPeers());
        assertEquals(0, stats.getOnlinePeers());
    }
}

