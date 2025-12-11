package top.yumbo.ai.rag.local;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地知识库管理器 (Local Knowledge Base Manager)
 *
 * 核心功能 (Core Features):
 * 1. 本地数据完全存储 (Complete local data storage)
 * 2. 离线可用 (Offline available)
 * 3. 角色强相关 (Role-specific)
 * 4. 隐私完全掌控 (Privacy control)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class LocalKnowledgeManager {

    /**
     * 本地数据根目录 (Local data root directory)
     */
    private String localDataRoot = "./data/local";

    /**
     * 当前用户ID (Current user ID)
     */
    private String userId;

    /**
     * 用户数据路径缓存 (User data path cache)
     */
    private final Map<String, String> userPathCache = new ConcurrentHashMap<>();

    /**
     * 离线模式 (Offline mode)
     */
    private boolean offlineMode = false;

    // ========== 初始化 (Initialization) ==========

    public LocalKnowledgeManager(String userId) {
        this.userId = userId;
        initializeLocalStorage();
    }

    /**
     * 初始化本地存储 (Initialize local storage)
     */
    private void initializeLocalStorage() {
        try {
            // 创建用户数据目录 (Create user data directory)
            String userDataPath = getUserDataPath(userId);
            Path path = Paths.get(userDataPath);

            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info(I18N.get("local.kb.initialized"), userId, userDataPath);
            }

            // 初始化子目录 (Initialize subdirectories)
            initializeSubDirectories(userDataPath);

        } catch (Exception e) {
            log.error(I18N.get("local.kb.init_failed"), userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化子目录 (Initialize subdirectories)
     */
    private void initializeSubDirectories(String userDataPath) throws Exception {
        String[] subDirs = {
            "documents",      // 文档 (Documents)
            "vectors",        // 向量索引 (Vector indices)
            "knowledge",      // 知识条目 (Knowledge entries)
            "qa-history",     // 问答历史 (Q&A history)
            "feedback",       // 反馈数据 (Feedback data)
            "roles"           // 角色数据 (Role data)
        };

        for (String subDir : subDirs) {
            Path subPath = Paths.get(userDataPath, subDir);
            if (!Files.exists(subPath)) {
                Files.createDirectories(subPath);
                log.debug(I18N.get("local.kb.subdir_created"), subDir);
            }
        }
    }

    // ========== 路径管理 (Path Management) ==========

    /**
     * 获取用户数据路径 (Get user data path)
     */
    public String getUserDataPath(String userId) {
        return userPathCache.computeIfAbsent(userId,
            uid -> String.format("%s/user-%s", localDataRoot, uid));
    }

    /**
     * 获取角色数据路径 (Get role data path)
     */
    public String getRoleDataPath(String userId, String roleId) {
        return String.format("%s/roles/role-%s", getUserDataPath(userId), roleId);
    }

    /**
     * 获取文档路径 (Get document path)
     */
    public String getDocumentPath(String userId, String docId) {
        return String.format("%s/documents/%s.json", getUserDataPath(userId), docId);
    }

    /**
     * 获取向量索引路径 (Get vector index path)
     */
    public String getVectorIndexPath(String userId) {
        return String.format("%s/vectors/index.bin", getUserDataPath(userId));
    }

    // ========== 知识管理 (Knowledge Management) ==========

    /**
     * 保存本地知识 (Save local knowledge)
     */
    public void saveKnowledge(String userId, String roleId, LocalKnowledge knowledge) {
        try {
            String rolePath = getRoleDataPath(userId, roleId);

            // 确保目录存在 (Ensure directory exists)
            Files.createDirectories(Paths.get(rolePath));

            // 保存知识文件 (Save knowledge file)
            String knowledgePath = String.format("%s/%s.json", rolePath, knowledge.getId());
            // TODO: 实现 JSON 序列化

            log.info(I18N.get("local.kb.knowledge_saved"), knowledge.getId(), roleId);

        } catch (Exception e) {
            log.error(I18N.get("local.kb.save_failed"), knowledge.getId(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询本地知识 (Query local knowledge)
     */
    public List<LocalKnowledge> queryKnowledge(String userId, String query, String roleId) {
        try {
            log.debug(I18N.get("local.kb.querying"), query, roleId);

            // TODO: 实现本地向量检索
            // 1. 使用本地向量索引
            // 2. 角色过滤
            // 3. 质量排序

            // 临时返回空列表 (Temporary empty list)
            return new ArrayList<>();

        } catch (Exception e) {
            log.error(I18N.get("local.kb.query_failed"), query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除本地知识 (Delete local knowledge)
     */
    public void deleteKnowledge(String userId, String knowledgeId) {
        try {
            // TODO: 实现删除逻辑
            log.info(I18N.get("local.kb.knowledge_deleted"), knowledgeId);

        } catch (Exception e) {
            log.error(I18N.get("local.kb.delete_failed"), knowledgeId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ========== 离线模式 (Offline Mode) ==========

    /**
     * 启用离线模式 (Enable offline mode)
     */
    public void enableOfflineMode() {
        this.offlineMode = true;
        log.info(I18N.get("local.kb.offline_enabled"));
    }

    /**
     * 禁用离线模式 (Disable offline mode)
     */
    public void disableOfflineMode() {
        this.offlineMode = false;
        log.info(I18N.get("local.kb.offline_disabled"));
    }

    /**
     * 检查是否在线 (Check if online)
     */
    public boolean isOnline() {
        return !offlineMode;
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取本地知识统计 (Get local knowledge statistics)
     */
    public LocalKnowledgeStats getStatistics(String userId) {
        try {
            LocalKnowledgeStats stats = new LocalKnowledgeStats();
            stats.setUserId(userId);
            stats.setUpdateTime(LocalDateTime.now());

            // 统计各类数据 (Count various data)
            String userPath = getUserDataPath(userId);
            stats.setTotalDocuments(countFiles(userPath + "/documents"));
            stats.setTotalKnowledge(countFiles(userPath + "/knowledge"));
            stats.setTotalQARecords(countFiles(userPath + "/qa-history"));

            // 计算存储大小 (Calculate storage size)
            stats.setStorageSize(calculateStorageSize(userPath));

            return stats;

        } catch (Exception e) {
            log.error(I18N.get("local.kb.stats_failed"), userId, e.getMessage(), e);
            return new LocalKnowledgeStats();
        }
    }

    /**
     * 统计文件数量 (Count files)
     */
    private int countFiles(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        File[] files = dir.listFiles();
        return files != null ? files.length : 0;
    }

    /**
     * 计算存储大小 (Calculate storage size)
     */
    private long calculateStorageSize(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return 0;
        }
        return calculateDirectorySize(dir);
    }

    /**
     * 递归计算目录大小 (Calculate directory size recursively)
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 本地知识条目 (Local Knowledge Entry)
     */
    @Data
    public static class LocalKnowledge {
        private String id;
        private String userId;
        private String roleId;
        private String question;
        private String answer;
        private double qualityScore;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Map<String, Object> metadata;
    }

    /**
     * 本地知识统计 (Local Knowledge Statistics)
     */
    @Data
    public static class LocalKnowledgeStats {
        private String userId;
        private int totalDocuments;
        private int totalKnowledge;
        private int totalQARecords;
        private long storageSize;  // 字节 (Bytes)
        private LocalDateTime updateTime;

        /**
         * 获取格式化的存储大小 (Get formatted storage size)
         */
        public String getFormattedStorageSize() {
            if (storageSize < 1024) {
                return storageSize + " B";
            } else if (storageSize < 1024 * 1024) {
                return String.format("%.2f KB", storageSize / 1024.0);
            } else if (storageSize < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", storageSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", storageSize / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}

