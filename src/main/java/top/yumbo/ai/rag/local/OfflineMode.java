package top.yumbo.ai.rag.local;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 离线模式管理器 (Offline Mode Manager)
 *
 * 功能 (Features):
 * 1. 自动检测网络状态 (Auto detect network status)
 * 2. 自动切换在线/离线模式 (Auto switch online/offline mode)
 * 3. 离线模式下的降级策略 (Degradation strategy in offline mode)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class OfflineMode {

    /**
     * 是否离线 (Is offline)
     */
    private volatile boolean offline = false;

    /**
     * 上次在线时间 (Last online time)
     */
    private LocalDateTime lastOnlineTime;

    /**
     * 网络检测间隔（秒） (Network check interval in seconds)
     */
    private int checkInterval = 30;

    /**
     * 定时任务执行器 (Scheduled executor)
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 网络检测目标 (Network check target)
     */
    private String checkTarget = "www.baidu.com";

    // ========== 初始化 (Initialization) ==========

    public OfflineMode() {
        startNetworkMonitor();
    }

    /**
     * 启动网络监控 (Start network monitor)
     */
    private void startNetworkMonitor() {
        scheduler.scheduleAtFixedRate(
            this::checkNetworkStatus,
            0,  // 初始延迟 (Initial delay)
            checkInterval,  // 间隔 (Interval)
            TimeUnit.SECONDS
        );

        log.info(I18N.get("offline.monitor_started"), checkInterval);
    }

    // ========== 网络检测 (Network Detection) ==========

    /**
     * 检查网络状态 (Check network status)
     */
    public void checkNetworkStatus() {
        boolean currentlyOnline = isNetworkAvailable();

        if (currentlyOnline && offline) {
            // 从离线恢复到在线 (Recovered from offline to online)
            offline = false;
            lastOnlineTime = LocalDateTime.now();
            log.info(I18N.get("offline.network_restored"));

        } else if (!currentlyOnline && !offline) {
            // 从在线变为离线 (Changed from online to offline)
            offline = true;
            log.warn(I18N.get("offline.network_lost"));
        }
    }

    /**
     * 检测网络是否可用 (Check if network is available)
     *
     * @return 是否在线 (Is online)
     */
    private boolean isNetworkAvailable() {
        try {
            // 尝试连接检测目标 (Try to connect to check target)
            InetAddress address = InetAddress.getByName(checkTarget);
            boolean reachable = address.isReachable(5000);  // 5秒超时 (5s timeout)

            log.debug(I18N.get("offline.network_check"), checkTarget, reachable ? "online" : "offline");
            return reachable;

        } catch (Exception e) {
            log.debug(I18N.get("offline.network_check_failed"), e.getMessage());
            return false;
        }
    }

    // ========== 模式控制 (Mode Control) ==========

    /**
     * 手动进入离线模式 (Manually enter offline mode)
     */
    public void enterOfflineMode() {
        offline = true;
        log.info(I18N.get("offline.mode_enabled"));
    }

    /**
     * 手动退出离线模式 (Manually exit offline mode)
     */
    public void exitOfflineMode() {
        offline = false;
        lastOnlineTime = LocalDateTime.now();
        log.info(I18N.get("offline.mode_disabled"));
    }

    /**
     * 是否在线 (Is online)
     */
    public boolean isOnline() {
        return !offline;
    }

    // ========== 降级策略 (Degradation Strategy) ==========

    /**
     * 获取降级后的功能列表 (Get degraded features)
     */
    public DegradationPolicy getDegradationPolicy() {
        DegradationPolicy policy = new DegradationPolicy();

        if (offline) {
            // 离线模式的降级策略 (Degradation policy in offline mode)
            policy.setLocalSearchEnabled(true);      // 本地搜索可用
            policy.setOnlineSearchEnabled(false);    // 在线搜索不可用
            policy.setP2pSyncEnabled(false);         // P2P 同步不可用
            policy.setCompanySyncEnabled(false);     // 公司同步不可用
            policy.setOnlineAPIEnabled(false);       // 在线 API 不可用

            log.debug(I18N.get("offline.degradation_applied"));
        } else {
            // 在线模式全功能 (Full features in online mode)
            policy.setLocalSearchEnabled(true);
            policy.setOnlineSearchEnabled(true);
            policy.setP2pSyncEnabled(true);
            policy.setCompanySyncEnabled(true);
            policy.setOnlineAPIEnabled(true);
        }

        return policy;
    }

    /**
     * 检查功能是否可用 (Check if feature is available)
     */
    public boolean isFeatureAvailable(String featureName) {
        if (offline) {
            // 离线模式下只有本地功能可用 (Only local features available in offline mode)
            return "local_search".equals(featureName) ||
                   "local_storage".equals(featureName);
        }
        return true;
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取离线统计 (Get offline statistics)
     */
    public OfflineStats getOfflineStats() {
        OfflineStats stats = new OfflineStats();
        stats.setCurrentlyOffline(offline);
        stats.setLastOnlineTime(lastOnlineTime);

        if (offline && lastOnlineTime != null) {
            // 计算离线时长 (Calculate offline duration)
            var duration = java.time.Duration.between(lastOnlineTime, LocalDateTime.now());
            stats.setOfflineDuration(duration.toMinutes());
        }

        return stats;
    }

    // ========== 关闭 (Shutdown) ==========

    /**
     * 关闭网络监控 (Shutdown network monitor)
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            log.info(I18N.get("offline.monitor_stopped"));
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 降级策略 (Degradation Policy)
     */
    @Data
    public static class DegradationPolicy {
        private boolean localSearchEnabled;      // 本地搜索
        private boolean onlineSearchEnabled;     // 在线搜索
        private boolean p2pSyncEnabled;          // P2P 同步
        private boolean companySyncEnabled;      // 公司同步
        private boolean onlineAPIEnabled;        // 在线 API
    }

    /**
     * 离线统计 (Offline Statistics)
     */
    @Data
    public static class OfflineStats {
        private boolean currentlyOffline;        // 当前是否离线
        private LocalDateTime lastOnlineTime;    // 上次在线时间
        private long offlineDuration;            // 离线时长（分钟）
    }
}

