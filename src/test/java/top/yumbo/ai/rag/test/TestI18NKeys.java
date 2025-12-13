package top.yumbo.ai.rag.test;

import top.yumbo.ai.rag.i18n.I18N;

/**
 * 测试国际化键是否正确加载
 */
public class TestI18NKeys {
    public static void main(String[] args) {
        System.out.println("Testing I18N keys from FeedbackController...\n");

        String[] keys = {
            "feedback.conflicts.query.start",
            "feedback.conflicts.query.success",
            "feedback.conflicts.query.failed",
            "feedback.vote.submitted",
            "feedback.vote.success",
            "feedback.vote.failed",
            "feedback.vote.impact",
            "feedback.vote.error.invalid_choice",
            "feedback.evolution.query.start",
            "feedback.evolution.query.success",
            "feedback.evolution.query.failed",
            "feedback.quality.query.start",
            "feedback.quality.query.success",
            "feedback.quality.query.failed",
            "feedback.prompts.query.start",
            "feedback.prompts.query.success",
            "feedback.prompts.query.failed",
            "feedback.submit.received",
            "feedback.submit.failed",
            "feedback.list.query.start",
            "feedback.list.query.success",
            "feedback.list.query.failed"
        };

        System.out.println("=== 中文测试 (Chinese) ===");
        for (String key : keys) {
            String message = I18N.getLang(key, "zh");
            boolean missing = message.startsWith("[") && message.endsWith("]");
            System.out.printf("%s %s: %s\n",
                missing ? "❌" : "✅",
                key,
                message);
        }

        System.out.println("\n=== 英文测试 (English) ===");
        for (String key : keys) {
            String message = I18N.getLang(key, "en");
            boolean missing = message.startsWith("[") && message.endsWith("]");
            System.out.printf("%s %s: %s\n",
                missing ? "❌" : "✅",
                key,
                message);
        }
    }
}

