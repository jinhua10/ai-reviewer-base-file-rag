package top.yumbo.ai.rag.evolution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 概念更新器 (Concept Updater)
 *
 * 管理概念的版本和更新
 * (Manages concept versions and updates)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class ConceptUpdater {

    /**
     * 版本存储 (Version storage)
     * Key: conceptId, Value: List of versions
     */
    private final Map<String, List<ConceptVersion>> versionStorage = new ConcurrentHashMap<>();

    /**
     * 创建新版本 (Create new version)
     *
     * @param conceptId 概念ID (Concept ID)
     * @param content 内容 (Content)
     * @param changes 变更说明 (Changes)
     * @param author 作者 (Author)
     * @return 新版本 (New version)
     */
    public ConceptVersion createVersion(String conceptId, String content,
                                       String changes, String author) {
        log.info(I18N.get("evolution.version.creating", conceptId));

        // 获取当前版本列表
        List<ConceptVersion> versions = versionStorage.computeIfAbsent(
                conceptId, k -> new ArrayList<>());

        // 生成版本号
        String versionNumber = "v" + (versions.size() + 1);

        // 获取父版本
        String parentVersion = null;
        if (!versions.isEmpty()) {
            ConceptVersion latest = versions.get(versions.size() - 1);
            parentVersion = latest.getVersionId();
        }

        // 创建新版本
        ConceptVersion version = ConceptVersion.builder()
                .versionId(UUID.randomUUID().toString())
                .conceptId(conceptId)
                .version(versionNumber)
                .content(content)
                .changes(changes)
                .author(author)
                .parentVersion(parentVersion)
                .status(ConceptVersion.VersionStatus.DRAFT)
                .build();

        versions.add(version);

        log.info(I18N.get("evolution.version.created", version.getVersionId(), versionNumber));
        return version;
    }

    /**
     * 发布版本 (Publish version)
     *
     * @param versionId 版本ID (Version ID)
     * @param approver 审批者 (Approver)
     */
    public void publishVersion(String versionId, String approver) {
        ConceptVersion version = findVersion(versionId);
        if (version != null) {
            version.setStatus(ConceptVersion.VersionStatus.PUBLISHED);
            version.setApprover(approver);
            log.info(I18N.get("evolution.version.published", versionId, approver));
        }
    }

    /**
     * 废弃版本 (Deprecate version)
     *
     * @param versionId 版本ID (Version ID)
     */
    public void deprecateVersion(String versionId) {
        ConceptVersion version = findVersion(versionId);
        if (version != null) {
            version.setStatus(ConceptVersion.VersionStatus.DEPRECATED);
            log.info(I18N.get("evolution.version.deprecated", versionId));
        }
    }

    /**
     * 获取版本历史 (Get version history)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 版本列表 (Version list)
     */
    public List<ConceptVersion> getVersionHistory(String conceptId) {
        return new ArrayList<>(versionStorage.getOrDefault(conceptId, Collections.emptyList()));
    }

    /**
     * 获取最新版本 (Get latest version)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 最新版本 (Latest version)
     */
    public ConceptVersion getLatestVersion(String conceptId) {
        List<ConceptVersion> versions = versionStorage.get(conceptId);
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.get(versions.size() - 1);
    }

    /**
     * 获取已发布版本 (Get published version)
     *
     * @param conceptId 概念ID (Concept ID)
     * @return 已发布版本 (Published version)
     */
    public ConceptVersion getPublishedVersion(String conceptId) {
        List<ConceptVersion> versions = versionStorage.get(conceptId);
        if (versions == null) {
            return null;
        }

        // 从后往前找最新的已发布版本
        for (int i = versions.size() - 1; i >= 0; i--) {
            ConceptVersion version = versions.get(i);
            if (version.isPublished()) {
                return version;
            }
        }
        return null;
    }

    /**
     * 比较版本 (Compare versions)
     *
     * @param versionId1 版本1 ID (Version 1 ID)
     * @param versionId2 版本2 ID (Version 2 ID)
     * @return 比较结果 (Comparison result)
     */
    public String compareVersions(String versionId1, String versionId2) {
        ConceptVersion v1 = findVersion(versionId1);
        ConceptVersion v2 = findVersion(versionId2);

        if (v1 == null || v2 == null) {
            return "版本未找到";
        }

        StringBuilder diff = new StringBuilder();
        diff.append("版本对比:\n");
        diff.append("版本1: ").append(v1.getVersion()).append("\n");
        diff.append("版本2: ").append(v2.getVersion()).append("\n");
        diff.append("内容差异: ");

        if (v1.getContent().equals(v2.getContent())) {
            diff.append("无差异");
        } else {
            diff.append("有差异");
        }

        return diff.toString();
    }

    /**
     * 回滚到指定版本 (Rollback to version)
     *
     * @param conceptId 概念ID (Concept ID)
     * @param versionId 目标版本ID (Target version ID)
     * @return 是否成功 (Whether successful)
     */
    public boolean rollbackToVersion(String conceptId, String versionId) {
        ConceptVersion targetVersion = findVersion(versionId);
        if (targetVersion == null || !targetVersion.getConceptId().equals(conceptId)) {
            log.error(I18N.get("evolution.rollback.version_not_found", versionId));
            return false;
        }

        log.info(I18N.get("evolution.rollback.start", conceptId, versionId));

        // 创建回滚版本（基于目标版本的内容）
        createVersion(conceptId, targetVersion.getContent(),
                "回滚到版本 " + targetVersion.getVersion(), "system");

        log.info(I18N.get("evolution.rollback.success", conceptId, versionId));
        return true;
    }

    /**
     * 查找版本 (Find version)
     *
     * @param versionId 版本ID (Version ID)
     * @return 版本对象 (Version object)
     */
    private ConceptVersion findVersion(String versionId) {
        for (List<ConceptVersion> versions : versionStorage.values()) {
            for (ConceptVersion version : versions) {
                if (version.getVersionId().equals(versionId)) {
                    return version;
                }
            }
        }
        return null;
    }

    /**
     * 获取统计信息 (Get statistics)
     *
     * @return 统计信息 (Statistics)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalConcepts = versionStorage.size();
        int totalVersions = versionStorage.values().stream()
                .mapToInt(List::size)
                .sum();

        stats.put("totalConcepts", totalConcepts);
        stats.put("totalVersions", totalVersions);
        stats.put("averageVersions", totalConcepts > 0 ? totalVersions / (double) totalConcepts : 0);

        return stats;
    }

    /**
     * 清空所有版本 (Clear all versions)
     * 仅用于测试 (For testing only)
     */
    public void clearAll() {
        versionStorage.clear();
        log.info(I18N.get("evolution.cleared"));
    }
}

