package top.yumbo.ai.rag.ai.scheduler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * 智能资源调度器 (Intelligent Resource Scheduler)
 *
 * 功能 (Features):
 * 1. 工作时间低资源占用 (Low resource usage during work hours)
 * 2. 下班时间批处理任务 (Batch tasks after work)
 * 3. 周末模型训练 (Model training on weekends)
 * 4. CPU/内存监控 (CPU/Memory monitoring)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class ResourceScheduler {

    /**
     * 调度执行器 (Scheduler executor)
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 任务队列 (Task queue)
     */
    private final Queue<ScheduledTask> taskQueue = new ConcurrentLinkedQueue<>();

    /**
     * 工作时间配置 (Work hours configuration)
     */
    private LocalTime workStartTime = LocalTime.of(9, 0);   // 9:00
    private LocalTime workEndTime = LocalTime.of(18, 0);     // 18:00

    /**
     * 资源限制配置 (Resource limit configuration)
     */
    private double maxCpuUsageWorkHours = 0.2;     // 工作时间最大20% CPU
    private double maxCpuUsageOffHours = 0.8;       // 下班时间最大80% CPU
    private double maxCpuUsageWeekend = 1.0;        // 周末可用100% CPU

    /**
     * 当前模式 (Current mode)
     */
    private ResourceMode currentMode = ResourceMode.WORK_HOURS;

    // ========== 初始化 (Initialization) ==========

    public ResourceScheduler() {
        startScheduler();
        log.info(I18N.get("scheduler.initialized"));
    }

    /**
     * 启动调度器 (Start scheduler)
     */
    private void startScheduler() {
        // 每分钟检查一次模式 (Check mode every minute)
        scheduler.scheduleAtFixedRate(
            this::updateMode,
            0,
            1,
            TimeUnit.MINUTES
        );

        // 每10秒处理一次任务队列 (Process task queue every 10 seconds)
        scheduler.scheduleAtFixedRate(
            this::processTasks,
            0,
            10,
            TimeUnit.SECONDS
        );

        log.info(I18N.get("scheduler.started"));
    }

    // ========== 模式管理 (Mode Management) ==========

    /**
     * 更新资源模式 (Update resource mode)
     */
    private void updateMode() {
        LocalDateTime now = LocalDateTime.now();
        ResourceMode newMode = determineMode(now);

        if (newMode != currentMode) {
            ResourceMode oldMode = currentMode;
            currentMode = newMode;
            log.info(I18N.get("scheduler.mode_changed"), oldMode, newMode);
        }
    }

    /**
     * 判断当前模式 (Determine current mode)
     */
    private ResourceMode determineMode(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();

        // 周末模式 (Weekend mode)
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return ResourceMode.WEEKEND;
        }

        // 工作时间模式 (Work hours mode)
        if (time.isAfter(workStartTime) && time.isBefore(workEndTime)) {
            return ResourceMode.WORK_HOURS;
        }

        // 下班时间模式 (Off hours mode)
        return ResourceMode.OFF_HOURS;
    }

    /**
     * 获取当前最大CPU使用率 (Get current max CPU usage)
     */
    public double getCurrentMaxCpuUsage() {
        return switch (currentMode) {
            case WORK_HOURS -> maxCpuUsageWorkHours;
            case OFF_HOURS -> maxCpuUsageOffHours;
            case WEEKEND -> maxCpuUsageWeekend;
        };
    }

    // ========== 任务调度 (Task Scheduling) ==========

    /**
     * 提交任务 (Submit task)
     *
     * @param task 任务 (Task)
     */
    public void submitTask(ScheduledTask task) {
        taskQueue.offer(task);
        log.info(I18N.get("scheduler.task_submitted"),
            task.getName(), task.getPriority());
    }

    /**
     * 处理任务队列 (Process task queue)
     */
    private void processTasks() {
        // 获取系统资源状态 (Get system resource status)
        ResourceStatus status = getResourceStatus();

        // 检查是否可以执行任务 (Check if can execute task)
        if (!canExecuteTask(status)) {
            return;
        }

        // 从队列取出任务 (Poll task from queue)
        ScheduledTask task = taskQueue.poll();
        if (task == null) {
            return;
        }

        // 检查任务是否适合当前模式 (Check if task suits current mode)
        if (!task.getSuitableModes().contains(currentMode)) {
            // 重新放回队列 (Put back to queue)
            taskQueue.offer(task);
            return;
        }

        // 执行任务 (Execute task)
        executeTask(task);
    }

    /**
     * 检查是否可以执行任务 (Check if can execute task)
     */
    private boolean canExecuteTask(ResourceStatus status) {
        double maxCpu = getCurrentMaxCpuUsage();

        if (status.getCpuUsage() > maxCpu) {
            log.debug(I18N.get("scheduler.cpu_limit_reached"), status.getCpuUsage(), maxCpu);
            return false;
        }

        return true;
    }

    /**
     * 执行任务 (Execute task)
     */
    private void executeTask(ScheduledTask task) {
        log.info(I18N.get("scheduler.task_executing"), task.getName());

        CompletableFuture.runAsync(() -> {
            try {
                task.getRunnable().run();
                log.info(I18N.get("scheduler.task_completed"), task.getName());
            } catch (Exception e) {
                log.error(I18N.get("scheduler.task_failed"), task.getName(), e.getMessage(), e);
            }
        });
    }

    // ========== 资源监控 (Resource Monitoring) ==========

    /**
     * 获取资源状态 (Get resource status)
     */
    private ResourceStatus getResourceStatus() {
        ResourceStatus status = new ResourceStatus();

        // 获取CPU使用率 (Get CPU usage)
        status.setCpuUsage(getCpuUsage());

        // 获取内存使用 (Get memory usage)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        status.setMemoryUsage((double) usedMemory / totalMemory);

        return status;
    }

    /**
     * 获取CPU使用率 (Get CPU usage)
     * 简化实现，实际应使用 OperatingSystemMXBean
     */
    private double getCpuUsage() {
        // TODO: 实现真实的CPU监控
        // 可以使用 com.sun.management.OperatingSystemMXBean
        return 0.1; // 临时返回10%
    }

    // ========== 关闭 (Shutdown) ==========

    /**
     * 关闭调度器 (Shutdown scheduler)
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info(I18N.get("scheduler.stopped"));
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 资源模式 (Resource Mode)
     */
    public enum ResourceMode {
        WORK_HOURS("work_hours", "工作时间", "Work Hours"),      // CPU < 20%
        OFF_HOURS("off_hours", "下班时间", "Off Hours"),         // CPU < 80%
        WEEKEND("weekend", "周末", "Weekend");                   // CPU < 100%

        private final String code;
        private final String nameCn;
        private final String nameEn;

        ResourceMode(String code, String nameCn, String nameEn) {
            this.code = code;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

        public String getCode() { return code; }
        public String getNameCn() { return nameCn; }
        public String getNameEn() { return nameEn; }
    }

    /**
     * 调度任务 (Scheduled Task)
     */
    @Data
    public static class ScheduledTask {
        private String name;                        // 任务名称
        private Runnable runnable;                  // 任务执行体
        private TaskPriority priority;              // 优先级
        private Set<ResourceMode> suitableModes;    // 适合的模式

        public ScheduledTask(String name, Runnable runnable, TaskPriority priority) {
            this.name = name;
            this.runnable = runnable;
            this.priority = priority;
            this.suitableModes = EnumSet.allOf(ResourceMode.class);
        }
    }

    /**
     * 任务优先级 (Task Priority)
     */
    public enum TaskPriority {
        LOW(1),
        MEDIUM(2),
        HIGH(3);

        private final int level;

        TaskPriority(int level) {
            this.level = level;
        }

        public int getLevel() { return level; }
    }

    /**
     * 资源状态 (Resource Status)
     */
    @Data
    public static class ResourceStatus {
        private double cpuUsage;        // CPU使用率
        private double memoryUsage;     // 内存使用率
    }
}

