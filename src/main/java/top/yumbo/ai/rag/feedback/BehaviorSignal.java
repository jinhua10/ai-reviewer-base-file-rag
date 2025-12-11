package top.yumbo.ai.rag.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 行为信号 (Behavior Signal)
 *
 * 记录用户的行为数据，用于推断隐式反馈
 * (Records user behavior data for inferring implicit feedback)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorSignal {

    /**
     * 会话ID (Session ID)
     */
    private String sessionId;

    /**
     * 用户ID (User ID)
     */
    private String userId;

    /**
     * 信号类型 (Signal type)
     */
    private SignalType type;

    /**
     * 信号值 (Signal value)
     */
    private double value;

    /**
     * 信号权重 (Signal weight)
     * 不同信号的重要程度不同
     */
    private double weight;

    /**
     * 记录时间 (Record time)
     */
    @Builder.Default
    private Date timestamp = new Date();

    /**
     * 信号类型 (Signal Type)
     */
    public enum SignalType {
        /**
         * 阅读时间 (Reading time)
         * 阅读答案的时长
         */
        READING_TIME(0.8),

        /**
         * 复制内容 (Copy content)
         * 用户复制了答案
         */
        COPY_CONTENT(0.9),

        /**
         * 点击链接 (Click link)
         * 点击答案中的参考链接
         */
        CLICK_LINK(0.7),

        /**
         * 重新提问 (Rephrase question)
         * 用类似问题重新提问
         */
        REPHRASE_QUESTION(-0.5),

        /**
         * 快速关闭 (Quick close)
         * 快速关闭答案页面
         */
        QUICK_CLOSE(-0.8),

        /**
         * 滚动深度 (Scroll depth)
         * 答案滚动深度
         */
        SCROLL_DEPTH(0.6),

        /**
         * 停留时间 (Dwell time)
         * 页面停留时间
         */
        DWELL_TIME(0.7),

        /**
         * 跳出率 (Bounce rate)
         * 是否立即跳出
         */
        BOUNCE_RATE(-0.7),

        /**
         * 反馈点击 (Feedback click)
         * 点击反馈按钮（未提交）
         */
        FEEDBACK_CLICK(0.5),

        /**
         * 分享内容 (Share content)
         * 分享答案
         */
        SHARE_CONTENT(0.95),

        /**
         * 收藏答案 (Bookmark answer)
         * 收藏此答案
         */
        BOOKMARK(0.9),

        /**
         * 打印页面 (Print page)
         * 打印答案页面
         */
        PRINT_PAGE(0.85),

        /**
         * 高亮文本 (Highlight text)
         * 高亮答案中的文本
         */
        HIGHLIGHT_TEXT(0.8);

        /**
         * 默认权重 (Default weight)
         */
        private final double defaultWeight;

        SignalType(double defaultWeight) {
            this.defaultWeight = defaultWeight;
        }

        public double getDefaultWeight() {
            return defaultWeight;
        }
    }

    /**
     * 创建阅读时间信号 (Create reading time signal)
     *
     * @param sessionId 会话ID (Session ID)
     * @param readingTimeMs 阅读时长（毫秒） (Reading time in ms)
     * @return 行为信号 (Behavior signal)
     */
    public static BehaviorSignal createReadingTime(String sessionId, long readingTimeMs) {
        // 阅读时间越长，信号越强（但有上限）
        // 假设3分钟为满分
        double value = Math.min(1.0, readingTimeMs / (3.0 * 60 * 1000));

        return BehaviorSignal.builder()
                .sessionId(sessionId)
                .type(SignalType.READING_TIME)
                .value(value)
                .weight(SignalType.READING_TIME.getDefaultWeight())
                .build();
    }

    /**
     * 创建复制内容信号 (Create copy content signal)
     *
     * @param sessionId 会话ID (Session ID)
     * @param copiedLength 复制长度 (Copied length)
     * @return 行为信号 (Behavior signal)
     */
    public static BehaviorSignal createCopyContent(String sessionId, int copiedLength) {
        // 复制长度越长，信号越强
        double value = Math.min(1.0, copiedLength / 500.0);

        return BehaviorSignal.builder()
                .sessionId(sessionId)
                .type(SignalType.COPY_CONTENT)
                .value(value)
                .weight(SignalType.COPY_CONTENT.getDefaultWeight())
                .build();
    }

    /**
     * 创建快速关闭信号 (Create quick close signal)
     *
     * @param sessionId 会话ID (Session ID)
     * @param dwellTimeMs 停留时长（毫秒） (Dwell time in ms)
     * @return 行为信号 (Behavior signal)
     */
    public static BehaviorSignal createQuickClose(String sessionId, long dwellTimeMs) {
        // 停留时间越短，负面信号越强
        // 假设5秒内关闭为快速关闭
        double value = 1.0 - Math.min(1.0, dwellTimeMs / 5000.0);

        return BehaviorSignal.builder()
                .sessionId(sessionId)
                .type(SignalType.QUICK_CLOSE)
                .value(value)
                .weight(SignalType.QUICK_CLOSE.getDefaultWeight())
                .build();
    }

    /**
     * 创建滚动深度信号 (Create scroll depth signal)
     *
     * @param sessionId 会话ID (Session ID)
     * @param scrollDepth 滚动深度（0-1） (Scroll depth 0-1)
     * @return 行为信号 (Behavior signal)
     */
    public static BehaviorSignal createScrollDepth(String sessionId, double scrollDepth) {
        return BehaviorSignal.builder()
                .sessionId(sessionId)
                .type(SignalType.SCROLL_DEPTH)
                .value(Math.min(1.0, Math.max(0.0, scrollDepth)))
                .weight(SignalType.SCROLL_DEPTH.getDefaultWeight())
                .build();
    }

    /**
     * 创建简单信号 (Create simple signal)
     *
     * @param sessionId 会话ID (Session ID)
     * @param type 信号类型 (Signal type)
     * @return 行为信号 (Behavior signal)
     */
    public static BehaviorSignal createSimpleSignal(String sessionId, SignalType type) {
        return BehaviorSignal.builder()
                .sessionId(sessionId)
                .type(type)
                .value(1.0)
                .weight(type.getDefaultWeight())
                .build();
    }
}

