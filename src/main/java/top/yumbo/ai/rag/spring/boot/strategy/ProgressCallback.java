package top.yumbo.ai.rag.spring.boot.strategy;

/**
 * 进度回调接口
 * (Progress Callback Interface)
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * 报告进度
     * (Report progress)
     *
     * @param progress 进度百分比 0-100
     * @param message 进度消息
     */
    void onProgress(int progress, String message);

    /**
     * 空实现
     * (Empty implementation)
     */
    static ProgressCallback empty() {
        return (progress, message) -> {};
    }
}

