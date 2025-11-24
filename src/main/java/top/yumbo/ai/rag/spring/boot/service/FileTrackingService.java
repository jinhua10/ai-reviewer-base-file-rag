package top.yumbo.ai.rag.spring.boot.service;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件追踪服务
 * 用于记录已索引的文件及其最后修改时间，支持增量索引
 *
 * @author AI Reviewer Team
 * @since 2025-11-23
 */
@Slf4j
@Service
public class FileTrackingService {

    private static final String TRACKING_FILE = ".file_tracking.json";
    private final Map<String, FileInfo> fileTracking = new HashMap<>();
    private String trackingFilePath;

    /**
     * 初始化文件追踪
     *
     * @param storagePath 知识库存储路径
     */
    public void initialize(String storagePath) {
        this.trackingFilePath = storagePath + File.separator + TRACKING_FILE;
        loadTracking();
    }

    /**
     * 加载追踪信息
     */
    private void loadTracking() {
        try {
            Path path = Paths.get(trackingFilePath);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                Map<String, FileInfo> loaded = JSON.parseObject(content,
                    new com.alibaba.fastjson2.TypeReference<Map<String, FileInfo>>() {});
                if (loaded != null) {
                    fileTracking.putAll(loaded);
                    log.info("✅ 加载文件追踪信息: {} 个文件", fileTracking.size());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️  加载文件追踪信息失败: {}", e.getMessage());
        }
    }

    /**
     * 保存追踪信息
     */
    public void saveTracking() {
        try {
            Path path = Paths.get(trackingFilePath);
            Files.createDirectories(path.getParent());
            String content = JSON.toJSONString(fileTracking);
            Files.writeString(path, content);
            log.debug("✅ 保存文件追踪信息: {} 个文件", fileTracking.size());
        } catch (Exception e) {
            log.error("❌ 保存文件追踪信息失败", e);
        }
    }

    /**
     * 检查文件是否需要更新
     *
     * @param file 文件对象
     * @return true 如果文件是新的或已修改
     */
    public boolean needsUpdate(File file) {
        try {
            String absolutePath = file.getAbsolutePath();
            long lastModified = file.lastModified();
            long fileSize = file.length();

            FileInfo info = fileTracking.get(absolutePath);

            // 新文件
            if (info == null) {
                return true;
            }

            // 检查修改时间和文件大小
            return info.lastModified != lastModified || info.fileSize != fileSize;

        } catch (Exception e) {
            log.warn("⚠️  检查文件状态失败: {}", file.getName(), e);
            return true; // 出错时默认更新
        }
    }

    /**
     * 标记文件已索引
     *
     * @param file 文件对象
     */
    public void markAsIndexed(File file) {
        try {
            String absolutePath = file.getAbsolutePath();
            FileInfo info = new FileInfo();
            info.fileName = file.getName();
            info.filePath = absolutePath;
            info.lastModified = file.lastModified();
            info.fileSize = file.length();
            info.indexedAt = System.currentTimeMillis();

            fileTracking.put(absolutePath, info);
        } catch (Exception e) {
            log.warn("⚠️  标记文件失败: {}", file.getName(), e);
        }
    }

    /**
     * 移除文件追踪
     *
     * @param file 文件对象
     */
    public void removeTracking(File file) {
        fileTracking.remove(file.getAbsolutePath());
    }

    /**
     * 清空所有追踪信息
     */
    public void clearAll() {
        fileTracking.clear();
        try {
            Path path = Paths.get(trackingFilePath);
            Files.deleteIfExists(path);
            log.info("✅ 已清空文件追踪信息");
        } catch (Exception e) {
            log.warn("⚠️  清空文件追踪信息失败", e);
        }
    }

    /**
     * 获取追踪信息统计
     */
    public TrackingStats getStats() {
        TrackingStats stats = new TrackingStats();
        stats.totalTracked = fileTracking.size();
        stats.trackingFilePath = trackingFilePath;
        return stats;
    }

    /**
     * 文件信息
     */
    @Data
    public static class FileInfo {
        private String fileName;
        private String filePath;
        private long lastModified;
        private long fileSize;
        private long indexedAt;
    }

    /**
     * 追踪统计
     */
    @Data
    public static class TrackingStats {
        private int totalTracked;
        private String trackingFilePath;
    }
}

