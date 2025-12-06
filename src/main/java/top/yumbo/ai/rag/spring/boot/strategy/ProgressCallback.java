package top.yumbo.ai.rag.spring.boot.strategy;

/**
 * 进度回调接口（Progress Callback Interface）
 *
 * <p>用于报告分析任务的执行进度</p>
 * <p>Used to report analysis task execution progress</p>
 *
 * @author AI Reviewer Team
 * @since 2025-12-06
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * 报告进度（Report progress）
     *
     * @param progress 进度百分比 0-100（Progress percentage 0-100）
     * @param message 进度消息（Progress message）
     */
    void onProgress(int progress, String message);

    /**
     * 空实现（Empty implementation）
     *
     * <p>用于不需要进度回调的场景</p>
     * <p>Used for scenarios that don't need progress callback</p>
     *
     * @return 空的进度回调（Empty progress callback）
     */
    static ProgressCallback empty() {
        return (progress, message) -> {};
    }
}

