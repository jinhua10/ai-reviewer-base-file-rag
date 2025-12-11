package top.yumbo.ai.rag.ai.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.ai.engine.impl.LocalOllamaEngine;
import top.yumbo.ai.rag.ai.engine.impl.OnlineAPIEngine;
import top.yumbo.ai.rag.ai.engine.impl.RemoteOllamaEngine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 引擎管理器测试 (AI Engine Manager Tests)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@DisplayName("AI 引擎管理器测试")
class AIEngineManagerTest {

    private AIEngineManager manager;

    @BeforeEach
    void setUp() {
        manager = new AIEngineManager();

        // 注册测试引擎 (Register test engines)
        manager.registerEngine(
            AIEngineManager.EngineType.LOCAL_OLLAMA,
            new LocalOllamaEngine()
        );

        manager.registerEngine(
            AIEngineManager.EngineType.REMOTE_OLLAMA,
            new RemoteOllamaEngine()
        );

        manager.registerEngine(
            AIEngineManager.EngineType.ONLINE_API,
            new OnlineAPIEngine()
        );
    }

    @Test
    @DisplayName("测试引擎注册")
    void testRegisterEngine() {
        // Then
        assertEquals(3, manager.getEngines().size());
        assertTrue(manager.getEngines().containsKey(AIEngineManager.EngineType.LOCAL_OLLAMA));
        assertTrue(manager.getEngines().containsKey(AIEngineManager.EngineType.REMOTE_OLLAMA));
        assertTrue(manager.getEngines().containsKey(AIEngineManager.EngineType.ONLINE_API));
    }

    @Test
    @DisplayName("测试获取引擎")
    void testGetEngine() {
        // When
        AIEngine engine = manager.getEngine();

        // Then
        assertNotNull(engine);
    }

    @Test
    @DisplayName("测试手动切换引擎")
    void testSwitchEngine() {
        // When
        manager.switchEngine(AIEngineManager.EngineType.LOCAL_OLLAMA);

        // Then
        assertEquals(AIEngineManager.EngineType.LOCAL_OLLAMA, manager.getActiveEngineType());
    }

    @Test
    @DisplayName("测试健康检查")
    void testHealthCheck() {
        // When & Then
        // 注意：实际测试需要真实的 Ollama 服务
        // 这里只测试方法不抛异常
        assertDoesNotThrow(() -> {
            manager.isHealthy(AIEngineManager.EngineType.LOCAL_OLLAMA);
        });
    }

    @Test
    @DisplayName("测试获取所有引擎状态")
    void testGetAllEnginesStatus() {
        // When
        var statusMap = manager.getAllEnginesStatus();

        // Then
        assertNotNull(statusMap);
        assertEquals(3, statusMap.size());
    }

    @Test
    @DisplayName("测试引擎类型枚举")
    void testEngineType() {
        // Then
        assertEquals("auto", AIEngineManager.EngineType.AUTO.getCode());
        assertEquals("自动选择", AIEngineManager.EngineType.AUTO.getNameCn());
        assertEquals("Auto Select", AIEngineManager.EngineType.AUTO.getNameEn());

        assertEquals("local_ollama", AIEngineManager.EngineType.LOCAL_OLLAMA.getCode());
        assertEquals("remote_ollama", AIEngineManager.EngineType.REMOTE_OLLAMA.getCode());
        assertEquals("online_api", AIEngineManager.EngineType.ONLINE_API.getCode());
    }
}

