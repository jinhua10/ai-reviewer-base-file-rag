package top.yumbo.ai.rag.spring.boot.service.document;

import org.springframework.stereotype.Component;

/**
 * Token 估算器
 *
 * 用于估算文本的 Token 数量，支持中英文混合内容。
 *
 * 估算规则：
 * - 中文：1 字符 ≈ 0.6 token
 * - 英文：1 单词 ≈ 1.3 token
 * - 代码：1 字符 ≈ 0.4 token
 * - 混合：字符数 / 2（保守估算）
 */
@Component
public class TokenEstimator {

    // 默认估算因子
    private static final double CHINESE_CHAR_FACTOR = 0.6;
    private static final double ENGLISH_WORD_FACTOR = 1.3;
    private static final double CODE_CHAR_FACTOR = 0.4;
    private static final double DEFAULT_FACTOR = 0.5;

    /**
     * 估算文本的 Token 数量（自动检测内容类型）
     *
     * @param text 文本内容
     * @return 估算的 Token 数
     */
    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 统计字符类型
        int chineseCount = 0;
        int englishCount = 0;
        int digitCount = 0;
        int symbolCount = 0;
        int spaceCount = 0;

        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                chineseCount++;
            } else if (Character.isLetter(c)) {
                englishCount++;
            } else if (Character.isDigit(c)) {
                digitCount++;
            } else if (Character.isWhitespace(c)) {
                spaceCount++;
            } else {
                symbolCount++;
            }
        }

        int totalChars = text.length();

        // 判断内容类型并估算
        double chineseRatio = (double) chineseCount / totalChars;
        double codeIndicator = (double) symbolCount / totalChars;

        if (chineseRatio > 0.3) {
            // 中文为主
            return estimateChinese(text, chineseCount, englishCount);
        } else if (codeIndicator > 0.2) {
            // 代码内容
            return estimateCode(text);
        } else {
            // 英文或混合内容
            return estimateEnglish(text);
        }
    }

    /**
     * 估算中文文本的 Token 数
     */
    public int estimateChinese(String text, int chineseCount, int englishCount) {
        // 中文字符
        double chineseTokens = chineseCount * CHINESE_CHAR_FACTOR;

        // 英文单词（估算：英文字符数 / 5 = 大约单词数）
        double englishWords = englishCount / 5.0;
        double englishTokens = englishWords * ENGLISH_WORD_FACTOR;

        return (int) Math.ceil(chineseTokens + englishTokens);
    }

    /**
     * 估算英文文本的 Token 数
     */
    public int estimateEnglish(String text) {
        // 按空格分词
        String[] words = text.split("\\s+");
        double tokens = words.length * ENGLISH_WORD_FACTOR;
        return (int) Math.ceil(tokens);
    }

    /**
     * 估算代码文本的 Token 数
     */
    public int estimateCode(String text) {
        return (int) Math.ceil(text.length() * CODE_CHAR_FACTOR);
    }

    /**
     * 简单估算（字符数 / 2）
     */
    public int estimateSimple(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() * DEFAULT_FACTOR);
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /**
     * 计算压缩到目标 Token 数需要的目标字符数
     *
     * @param currentTokens 当前 Token 数
     * @param targetTokens 目标 Token 数
     * @param currentLength 当前字符数
     * @return 目标字符数
     */
    public int calculateTargetLength(int currentTokens, int targetTokens, int currentLength) {
        if (currentTokens <= 0 || currentLength <= 0) {
            return targetTokens * 2; // 默认估算
        }

        double ratio = (double) targetTokens / currentTokens;
        return (int) Math.ceil(currentLength * ratio);
    }

    /**
     * 检查文本是否在 Token 预算内
     *
     * @param text 文本
     * @param budget Token 预算
     * @return 是否在预算内
     */
    public boolean isWithinBudget(String text, int budget) {
        return estimate(text) <= budget;
    }

    /**
     * 截断文本到目标 Token 数
     *
     * @param text 文本
     * @param targetTokens 目标 Token 数
     * @return 截断后的文本
     */
    public String truncateToTokens(String text, int targetTokens) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int currentTokens = estimate(text);
        if (currentTokens <= targetTokens) {
            return text;
        }

        // 估算目标字符数
        double ratio = (double) targetTokens / currentTokens;
        int targetLength = (int) (text.length() * ratio * 0.95); // 留 5% 余量

        if (targetLength >= text.length()) {
            return text;
        }

        // 截断并添加省略号
        String truncated = text.substring(0, targetLength);

        // 尝试在句子边界截断
        int lastPeriod = Math.max(
            truncated.lastIndexOf('。'),
            Math.max(truncated.lastIndexOf('.'), truncated.lastIndexOf('\n'))
        );

        if (lastPeriod > targetLength * 0.7) {
            truncated = truncated.substring(0, lastPeriod + 1);
        }

        return truncated + "...";
    }
}

