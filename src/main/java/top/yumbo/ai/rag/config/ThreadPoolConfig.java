package top.yumbo.ai.rag.config;
import java.util.concurrent.*;

/**
 * 线程池配置类 (Thread pool configuration class)
 * 提供各种线程池的创建方法 (Provides methods to create various thread pools)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
public class ThreadPoolConfig {
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 1000;

    /**
     * 创建索引线程池 (Create index thread pool)
     *
     * @return 执行器服务 (executor service)
     */
    public static ExecutorService createIndexThreadPool() {
        return new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "IndexThread-" + counter++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建搜索线程池 (Create search thread pool)
     *
     * @return 执行器服务 (executor service)
     */
    public static ExecutorService createSearchThreadPool() {
        return new ThreadPoolExecutor(
            CORE_POOL_SIZE * 2,
            MAX_POOL_SIZE * 2,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY * 2),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "SearchThread-" + counter++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建工作线程池 (Create worker thread pool)
     *
     * @param namePrefix 线程名前缀 (thread name prefix)
     * @return 执行器服务 (executor service)
     */
    public static ExecutorService createWorkerThreadPool(String namePrefix) {
        return new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, namePrefix + "-" + counter++);
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
