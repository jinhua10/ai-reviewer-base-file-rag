package top.yumbo.ai.rag.i18n;

import java.util.Locale;

/**
 * API响应消息提供者 / API Response Message Provider
 * 为前端API返回提供国际化消息支持 / Provides i18n message support for frontend API responses
 *
 * 使用 LogMessageProvider 的静态方法从 YAML 文件读取国际化消息
 * Uses LogMessageProvider's static methods to read i18n messages from YAML files
 *
 * @author AI Reviewer Team
 * @since 2025-11-30
 */
public class ApiMessageProvider {

    /**
     * 根据语言参数获取国际化消息 / Get internationalized message based on language parameter
     *
     * @param key 消息键 / Message key
     * @param lang 语言参数 (zh/en/null) / Language parameter (zh/en/null)
     * @param args 格式化参数 / Format arguments
     * @return 国际化消息 / Internationalized message
     */
    public static String getMessage(String key, String lang, Object... args) {
        // 临时设置语言环境 / Temporarily set locale
        String originalLocale = System.getProperty("log.locale");

        try {
            // 根据lang参数设置语言 / Set language based on lang parameter
            if ("en".equalsIgnoreCase(lang)) {
                System.setProperty("log.locale", "en");
            } else if ("zh".equalsIgnoreCase(lang)) {
                System.setProperty("log.locale", "zh");
            }
            // 如果lang为null或其他值，使用默认行为（自动检测）
            // If lang is null or other value, use default behavior (auto-detect)

            // 调用 LogMessageProvider 获取消息 / Call LogMessageProvider to get message
            return LogMessageProvider.getMessage(key, args);

        } finally {
            // 恢复原来的语言设置 / Restore original locale setting
            if (originalLocale != null) {
                System.setProperty("log.locale", originalLocale);
            } else {
                System.clearProperty("log.locale");
            }
        }
    }

    /**
     * 获取成功消息：感谢评价 / Get success message: thank you for rating
     */
    public static String getThankYou(String lang) {
        return getMessage("feedback.api.success.thank_you", lang);
    }

    /**
     * 获取成功消息：感谢反馈 / Get success message: thank you for feedback
     */
    public static String getFeedbackReceived(String lang) {
        return getMessage("feedback.api.success.feedback_received", lang);
    }

    /**
     * 获取错误消息：记录不存在 / Get error message: record not found
     */
    public static String getRecordNotFound(String lang) {
        return getMessage("feedback.api.error.record_not_found", lang);
    }

    /**
     * 获取错误消息：处理失败 / Get error message: processing failed
     */
    public static String getProcessingFailed(String lang, String error) {
        return getMessage("feedback.api.error.processing_failed", lang, error);
    }

    /**
     * 获取错误消息：评分无效 / Get error message: invalid rating
     */
    public static String getInvalidRating(String lang) {
        return getMessage("feedback.api.error.invalid_rating", lang);
    }

    /**
     * 获取错误消息：参数缺失 / Get error message: missing parameters
     */
    public static String getMissingParams(String lang, String params) {
        return getMessage("feedback.api.error.missing_params", lang, params);
    }

    /**
     * 获取错误消息：反馈类型无效 / Get error message: invalid feedback type
     */
    public static String getInvalidFeedbackType(String lang) {
        return getMessage("feedback.api.error.invalid_feedback_type", lang);
    }

    /**
     * 获取文档评价影响描述 / Get document rating impact description
     */
    public static String getDocumentImpact(String lang, int rating) {
        return getMessage("feedback.api.impact.document." + rating, lang);
    }

    /**
     * 获取整体评价影响描述 / Get overall rating impact description
     */
    public static String getOverallImpact(String lang, int rating) {
        return getMessage("feedback.api.impact.overall." + rating, lang);
    }

    /**
     * 获取表情描述 / Get emoji description
     */
    public static String getEmojiDescription(String lang, int rating) {
        return getMessage("feedback.api.emoji." + rating, lang);
    }
}

