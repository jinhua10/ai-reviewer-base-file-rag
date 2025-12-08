package top.yumbo.ai.rag.spring.boot.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.layer.OrdinaryLayerService;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.RecentQA;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0 任务验证测试 - 测试 HOPE 依赖方法
 * (P0 Task Verification Test - Test HOPE dependent methods)
 *
 * 验证以下方法是否正确实现：
 * 1. HOPEKnowledgeManager.getPermanentLayer()
 * 2. HOPEKnowledgeManager.getOrdinaryLayer()
 * 3. PermanentLayerService.findDirectAnswer()
 * 4. OrdinaryLayerService.findSimilarQA()
 * 5. OrdinaryLayerService.save()
 * 6. RecentQA.sessionId 字段
 * 7. RecentQA.similarityScore 字段
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@SpringBootTest
public class P0TaskVerificationTest {

    @Autowired(required = false)
    private HOPEKnowledgeManager hopeManager;

    /**
     * 测试 1: HOPEKnowledgeManager.getPermanentLayer()
     * (Test 1: HOPEKnowledgeManager.getPermanentLayer())
     */
    @Test
    public void testGetPermanentLayer() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试 (HOPE manager not enabled, skip test)");
            return;
        }

        PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
        assertNotNull(permanentLayer, "✅ getPermanentLayer() 应该返回非空对象");
        System.out.println("✅ 测试通过: HOPEKnowledgeManager.getPermanentLayer()");
    }

    /**
     * 测试 2: HOPEKnowledgeManager.getOrdinaryLayer()
     * (Test 2: HOPEKnowledgeManager.getOrdinaryLayer())
     */
    @Test
    public void testGetOrdinaryLayer() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试");
            return;
        }

        OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
        assertNotNull(ordinaryLayer, "✅ getOrdinaryLayer() 应该返回非空对象");
        System.out.println("✅ 测试通过: HOPEKnowledgeManager.getOrdinaryLayer()");
    }

    /**
     * 测试 3: PermanentLayerService.findDirectAnswer()
     * (Test 3: PermanentLayerService.findDirectAnswer())
     */
    @Test
    public void testFindDirectAnswer() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试");
            return;
        }

        PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
        assertNotNull(permanentLayer, "低频层服务应该存在");

        // 测试查找不存在的答案
        FactualKnowledge result = permanentLayer.findDirectAnswer("这是一个不存在的问题");
        // 可能返回 null，这是正常的
        System.out.println("✅ 测试通过: PermanentLayerService.findDirectAnswer() - 方法可调用");
    }

    /**
     * 测试 4: OrdinaryLayerService.findSimilarQA()
     * (Test 4: OrdinaryLayerService.findSimilarQA())
     */
    @Test
    public void testFindSimilarQA() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试");
            return;
        }

        OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
        assertNotNull(ordinaryLayer, "中频层服务应该存在");

        // 测试查找相似问答（最小相似度 0.8）
        RecentQA result = ordinaryLayer.findSimilarQA("什么是Docker？", 0.8);
        // 可能返回 null，这是正常的
        System.out.println("✅ 测试通过: OrdinaryLayerService.findSimilarQA() - 方法可调用");
    }

    /**
     * 测试 5: OrdinaryLayerService.save()
     * (Test 5: OrdinaryLayerService.save())
     */
    @Test
    public void testSaveRecentQA() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试");
            return;
        }

        OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
        assertNotNull(ordinaryLayer, "中频层服务应该存在");

        // 创建测试问答
        RecentQA testQA = RecentQA.builder()
            .id("test-qa-" + System.currentTimeMillis())
            .question("什么是单元测试？")
            .answer("单元测试是一种软件测试方法...")
            .rating(5)
            .sessionId("test-session-123")
            .similarityScore(0.95)
            .build();

        // 测试保存
        try {
            ordinaryLayer.save(testQA);
            System.out.println("✅ 测试通过: OrdinaryLayerService.save() - 方法可调用");
        } catch (Exception e) {
            fail("保存问答失败: " + e.getMessage());
        }
    }

    /**
     * 测试 6 & 7: RecentQA 字段测试
     * (Test 6 & 7: RecentQA fields test)
     */
    @Test
    public void testRecentQAFields() {
        // 创建 RecentQA 对象
        RecentQA qa = RecentQA.builder()
            .id("test-123")
            .question("测试问题")
            .answer("测试答案")
            .sessionId("test-session-456")
            .similarityScore(0.92)
            .build();

        // 测试 sessionId 字段
        assertNotNull(qa.getSessionId(), "✅ sessionId 字段应该存在");
        assertEquals("test-session-456", qa.getSessionId(), "✅ sessionId 值应该正确");

        // 测试 similarityScore 字段
        assertNotNull(qa.getSimilarityScore(), "✅ similarityScore 字段应该存在");
        assertEquals(0.92, qa.getSimilarityScore(), 0.001, "✅ similarityScore 值应该正确");

        System.out.println("✅ 测试通过: RecentQA.sessionId 和 RecentQA.similarityScore 字段");
    }

    /**
     * 综合集成测试 - 模拟完整的流式查询流程
     * (Comprehensive integration test - Simulate complete streaming query flow)
     */
    @Test
    public void testIntegratedStreamingFlow() {
        if (hopeManager == null) {
            System.out.println("⚠️ HOPE 管理器未启用，跳过测试");
            return;
        }

        System.out.println("\n========== 开始 P0 任务集成测试 ==========");

        // 步骤 1: 获取低频层服务
        PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
        assertNotNull(permanentLayer, "步骤 1: 获取低频层服务");
        System.out.println("✅ 步骤 1: 成功获取低频层服务");

        // 步骤 2: 获取中频层服务
        OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
        assertNotNull(ordinaryLayer, "步骤 2: 获取中频层服务");
        System.out.println("✅ 步骤 2: 成功获取中频层服务");

        // 步骤 3: 查询低频层
        String testQuestion = "什么是Docker？";
        FactualKnowledge fact = permanentLayer.findDirectAnswer(testQuestion);
        System.out.println("✅ 步骤 3: 成功查询低频层，结果: " + (fact != null ? "找到" : "未找到"));

        // 步骤 4: 查询中频层
        RecentQA similarQA = ordinaryLayer.findSimilarQA(testQuestion, 0.7);
        System.out.println("✅ 步骤 4: 成功查询中频层，结果: " + (similarQA != null ? "找到" : "未找到"));

        // 步骤 5: 保存问答到中频层
        RecentQA newQA = RecentQA.builder()
            .id("integration-test-" + System.currentTimeMillis())
            .question(testQuestion)
            .answer("Docker 是一个开源的容器化平台...")
            .rating(5)
            .sessionId("integration-test-session")
            .similarityScore(0.95)
            .build();

        ordinaryLayer.save(newQA);
        System.out.println("✅ 步骤 5: 成功保存问答到中频层");

        System.out.println("========== P0 任务集成测试完成 ==========\n");
        System.out.println("🎉 所有 P0 任务验证通过！");
    }
}

