package top.yumbo.ai.rag.spring.boot.streaming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.hope.HOPEKnowledgeManager;
import top.yumbo.ai.rag.hope.layer.OrdinaryLayerService;
import top.yumbo.ai.rag.hope.layer.PermanentLayerService;
import top.yumbo.ai.rag.hope.model.FactualKnowledge;
import top.yumbo.ai.rag.hope.model.RecentQA;

/**
 * P0 任务验证器 - 启动时自动验证 HOPE 依赖方法
 * (P0 Task Verifier - Automatically verify HOPE dependent methods on startup)
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
 * 使用方式：启动应用时会自动运行验证
 * (Usage: Automatically runs on application startup)
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */
@Slf4j
@Component
@Profile("!test") // 仅在非测试环境运行
public class P0TaskVerifier implements CommandLineRunner {

    private final HOPEKnowledgeManager hopeManager;

    @Autowired
    public P0TaskVerifier(@Autowired(required = false) HOPEKnowledgeManager hopeManager) {
        this.hopeManager = hopeManager;
    }

    @Override
    public void run(String... args) {
        log.info("\n" +
            "========================================\n" +
            "   P0 任务验证开始\n" +
            "   (P0 Task Verification Started)\n" +
            "========================================");

        if (hopeManager == null) {
            log.warn("⚠️ HOPE 管理器未启用，跳过 P0 验证 (HOPE manager not enabled, skip P0 verification)");
            return;
        }

        boolean allPassed = true;

        // 测试 1: HOPEKnowledgeManager.getPermanentLayer()
        allPassed &= testGetPermanentLayer();

        // 测试 2: HOPEKnowledgeManager.getOrdinaryLayer()
        allPassed &= testGetOrdinaryLayer();

        // 测试 3: PermanentLayerService.findDirectAnswer()
        allPassed &= testFindDirectAnswer();

        // 测试 4: OrdinaryLayerService.findSimilarQA()
        allPassed &= testFindSimilarQA();

        // 测试 5: OrdinaryLayerService.save()
        allPassed &= testSaveRecentQA();

        // 测试 6 & 7: RecentQA 字段
        allPassed &= testRecentQAFields();

        // 综合测试
        allPassed &= testIntegratedFlow();

        log.info("\n" +
            "========================================\n" +
            "   P0 任务验证结果: " + (allPassed ? "✅ 全部通过" : "❌ 部分失败") + "\n" +
            "   (P0 Task Verification Result: " + (allPassed ? "All Passed" : "Some Failed") + ")\n" +
            "========================================\n");
    }

    /**
     * 测试 1: HOPEKnowledgeManager.getPermanentLayer()
     */
    private boolean testGetPermanentLayer() {
        try {
            PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
            if (permanentLayer != null) {
                log.info("✅ 测试 1 通过: HOPEKnowledgeManager.getPermanentLayer()");
                return true;
            } else {
                log.error("❌ 测试 1 失败: getPermanentLayer() 返回 null");
                return false;
            }
        } catch (Exception e) {
            log.error("❌ 测试 1 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试 2: HOPEKnowledgeManager.getOrdinaryLayer()
     */
    private boolean testGetOrdinaryLayer() {
        try {
            OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
            if (ordinaryLayer != null) {
                log.info("✅ 测试 2 通过: HOPEKnowledgeManager.getOrdinaryLayer()");
                return true;
            } else {
                log.error("❌ 测试 2 失败: getOrdinaryLayer() 返回 null");
                return false;
            }
        } catch (Exception e) {
            log.error("❌ 测试 2 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试 3: PermanentLayerService.findDirectAnswer()
     */
    private boolean testFindDirectAnswer() {
        try {
            PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
            if (permanentLayer == null) {
                log.error("❌ 测试 3 失败: 低频层服务为 null");
                return false;
            }

            // 测试方法调用（结果可能为 null，这是正常的）
            FactualKnowledge result = permanentLayer.findDirectAnswer("测试问题");
            log.info("✅ 测试 3 通过: PermanentLayerService.findDirectAnswer() - 方法可调用，结果: " +
                (result != null ? "找到答案" : "未找到答案（正常）"));
            return true;
        } catch (Exception e) {
            log.error("❌ 测试 3 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试 4: OrdinaryLayerService.findSimilarQA()
     */
    private boolean testFindSimilarQA() {
        try {
            OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
            if (ordinaryLayer == null) {
                log.error("❌ 测试 4 失败: 中频层服务为 null");
                return false;
            }

            // 测试方法调用（结果可能为 null，这是正常的）
            RecentQA result = ordinaryLayer.findSimilarQA("什么是Docker？", 0.8);
            log.info("✅ 测试 4 通过: OrdinaryLayerService.findSimilarQA() - 方法可调用，结果: " +
                (result != null ? "找到相似问答" : "未找到相似问答（正常）"));
            return true;
        } catch (Exception e) {
            log.error("❌ 测试 4 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试 5: OrdinaryLayerService.save()
     */
    private boolean testSaveRecentQA() {
        try {
            OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();
            if (ordinaryLayer == null) {
                log.error("❌ 测试 5 失败: 中频层服务为 null");
                return false;
            }

            // 创建测试问答
            RecentQA testQA = RecentQA.builder()
                .id("p0-test-" + System.currentTimeMillis())
                .question("P0验证测试问题")
                .answer("这是 P0 验证测试的答案")
                .rating(5)
                .sessionId("p0-test-session")
                .similarityScore(0.95)
                .build();

            // 测试保存
            ordinaryLayer.save(testQA);
            log.info("✅ 测试 5 通过: OrdinaryLayerService.save() - 成功保存问答");
            return true;
        } catch (Exception e) {
            log.error("❌ 测试 5 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试 6 & 7: RecentQA 字段
     */
    private boolean testRecentQAFields() {
        try {
            // 创建 RecentQA 对象
            RecentQA qa = RecentQA.builder()
                .id("field-test-123")
                .question("字段测试问题")
                .answer("字段测试答案")
                .sessionId("field-test-session")
                .similarityScore(0.92)
                .build();

            // 测试 sessionId 字段
            if (qa.getSessionId() == null || !qa.getSessionId().equals("field-test-session")) {
                log.error("❌ 测试 6 失败: sessionId 字段不正确");
                return false;
            }

            // 测试 similarityScore 字段
            if (qa.getSimilarityScore() == null || Math.abs(qa.getSimilarityScore() - 0.92) > 0.001) {
                log.error("❌ 测试 7 失败: similarityScore 字段不正确");
                return false;
            }

            log.info("✅ 测试 6 & 7 通过: RecentQA.sessionId 和 RecentQA.similarityScore 字段");
            return true;
        } catch (Exception e) {
            log.error("❌ 测试 6 & 7 失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 综合集成测试
     */
    private boolean testIntegratedFlow() {
        try {
            log.info("开始综合集成测试...");

            // 步骤 1: 获取服务
            PermanentLayerService permanentLayer = hopeManager.getPermanentLayer();
            OrdinaryLayerService ordinaryLayer = hopeManager.getOrdinaryLayer();

            if (permanentLayer == null || ordinaryLayer == null) {
                log.error("❌ 综合测试失败: 服务未初始化");
                return false;
            }

            // 步骤 2: 查询低频层
            String testQuestion = "什么是Docker？";
            FactualKnowledge fact = permanentLayer.findDirectAnswer(testQuestion);
            log.debug("查询低频层: " + (fact != null ? "找到" : "未找到"));

            // 步骤 3: 查询中频层
            RecentQA similarQA = ordinaryLayer.findSimilarQA(testQuestion, 0.7);
            log.debug("查询中频层: " + (similarQA != null ? "找到" : "未找到"));

            // 步骤 4: 保存新问答
            RecentQA newQA = RecentQA.builder()
                .id("integrated-test-" + System.currentTimeMillis())
                .question(testQuestion)
                .answer("Docker 是一个开源的容器化平台，用于开发、交付和运行应用程序...")
                .rating(5)
                .sessionId("integrated-test-session")
                .similarityScore(0.95)
                .build();

            ordinaryLayer.save(newQA);

            log.info("✅ 综合集成测试通过: 所有流程正常运行");
            return true;
        } catch (Exception e) {
            log.error("❌ 综合集成测试失败: " + e.getMessage(), e);
            return false;
        }
    }
}

