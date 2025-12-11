package top.yumbo.ai.rag.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.yumbo.ai.rag.company.CompanyKBClient.Knowledge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公司服务器集成测试
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@DisplayName("Phase 4.5.3 - 公司服务器集成测试")
class CompanyModuleTest {

    private CompanyKBClient client;
    private ContributionWorkflow workflow;

    private static final String TEST_USER_ID = "user-001";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_ROLE_ID = "developer";

    @BeforeEach
    void setUp() {
        client = new CompanyKBClient(TEST_USER_ID, TEST_API_KEY);
        workflow = new ContributionWorkflow(client, TEST_USER_ID);
    }

    // ========== CompanyKBClient 测试 ==========

    @Test
    @DisplayName("测试客户端初始化")
    void testClientInit() {
        // Then
        assertNotNull(client);
        assertEquals(TEST_USER_ID, client.getUserId());
        assertEquals(TEST_API_KEY, client.getApiKey());
    }

    @Test
    @DisplayName("测试知识贡献")
    void testContribute() {
        // Given
        List<Knowledge> knowledgeList = createTestKnowledge(5);

        // When
        var result = client.contribute(knowledgeList);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalCount());
        assertNotNull(result.getSubmitTime());
    }

    @Test
    @DisplayName("测试增量下载")
    void testIncrementalDownload() {
        // Given
        LocalDateTime lastSyncTime = LocalDateTime.now().minusDays(1);

        // When
        List<Knowledge> downloaded = client.incrementalDownload(TEST_ROLE_ID, lastSyncTime);

        // Then
        assertNotNull(downloaded);
    }

    @Test
    @DisplayName("测试搜索公司知识库")
    void testSearch() {
        // When
        List<Knowledge> results = client.search("Java", TEST_ROLE_ID);

        // Then
        assertNotNull(results);
    }

    @Test
    @DisplayName("测试同步知识")
    void testSync() {
        // When
        var result = client.sync(TEST_ROLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ROLE_ID, result.getRoleId());
        assertNotNull(result.getSyncTime());
    }

    @Test
    @DisplayName("测试健康检查")
    void testHealthCheck() {
        // When
        boolean connected = client.checkConnection();

        // Then - 在没有真实服务器的情况下，默认返回 true
        assertTrue(connected);
    }

    // ========== ContributionWorkflow 测试 ==========

    @Test
    @DisplayName("测试工作流初始化")
    void testWorkflowInit() {
        // Then
        assertNotNull(workflow);
        assertEquals(TEST_USER_ID, workflow.getUserId());
    }

    @Test
    @DisplayName("测试添加到待贡献队列")
    void testAddToPendingQueue() {
        // Given
        Knowledge knowledge = createQualifiedKnowledge();

        // When
        workflow.addToPendingQueue(knowledge);

        // Then
        assertEquals(1, workflow.getPendingQueue().size());
    }

    @Test
    @DisplayName("测试批量添加到队列")
    void testBatchAddToPendingQueue() {
        // Given
        List<Knowledge> knowledgeList = createTestKnowledge(10);

        // When
        workflow.batchAddToPendingQueue(knowledgeList);

        // Then
        assertTrue(workflow.getPendingQueue().size() > 0);
    }

    @Test
    @DisplayName("测试获取贡献统计")
    void testGetStats() {
        // When
        var stats = workflow.getStats();

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.getPendingCount());
        assertEquals(0, stats.getTotalContributions());
    }

    @Test
    @DisplayName("测试获取贡献历史")
    void testGetContributionHistory() {
        // When
        var history = workflow.getContributionHistory();

        // Then
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试知识列表
     */
    private List<Knowledge> createTestKnowledge(int count) {
        List<Knowledge> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Knowledge k = new Knowledge();
            k.setId("knowledge-" + i);
            k.setUserId(TEST_USER_ID);
            k.setRoleId(TEST_ROLE_ID);
            k.setQuestion("Question " + i);
            k.setAnswer("Answer " + i);
            k.setQualityScore(0.9);
            k.setVerificationCount(3);
            k.setCreateTime(LocalDateTime.now());

            list.add(k);
        }

        return list;
    }

    /**
     * 创建合格的测试知识
     */
    private Knowledge createQualifiedKnowledge() {
        Knowledge k = new Knowledge();
        k.setId("qualified-knowledge");
        k.setUserId(TEST_USER_ID);
        k.setRoleId(TEST_ROLE_ID);
        k.setQuestion("Test Question");
        k.setAnswer("Test Answer");
        k.setQualityScore(0.9);      // > 0.8
        k.setVerificationCount(3);    // >= 3
        k.setCreateTime(LocalDateTime.now());

        return k;
    }
}

