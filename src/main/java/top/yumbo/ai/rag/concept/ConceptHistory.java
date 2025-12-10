package top.yumbo.ai.rag.concept;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 概念历史 (Concept History)
 *
 * 管理概念的完整版本历史记录
 * (Manages complete version history of a concept)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptHistory {

    /**
     * 历史记录ID (History ID)
     */
    private String historyId;

    /**
     * 概念ID (Concept ID)
     */
    private String conceptId;

    /**
     * 版本列表 (Version list)
     * 按时间顺序排列，最新的在最后 (Sorted by time, newest at the end)
     */
    @Builder.Default
    private List<ConceptVersion> versions = new ArrayList<>();

    /**
     * 当前版本号 (Current version number)
     */
    @Builder.Default
    private int currentVersion = 1;

    /**
     * 创建时间 (Creation time)
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 最后更新时间 (Last update time)
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 总变更次数 (Total change count)
     */
    @Builder.Default
    private int totalChanges = 0;

    // ==================== 业务方法 (Business Methods) ====================

    /**
     * 添加新版本 (Add new version)
     *
     * @param version 新版本 (New version)
     */
    public void addVersion(ConceptVersion version) {
        this.versions.add(version);
        this.currentVersion = version.getVersionNumber();
        this.totalChanges++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取最新版本 (Get latest version)
     *
     * @return 最新版本 (Latest version)
     */
    public ConceptVersion getLatestVersion() {
        if (versions.isEmpty()) {
            return null;
        }
        return versions.get(versions.size() - 1);
    }

    /**
     * 获取指定版本 (Get specific version)
     *
     * @param versionNumber 版本号 (Version number)
     * @return 指定版本，如果不存在返回 null (Specific version, null if not exists)
     */
    public ConceptVersion getVersion(int versionNumber) {
        return versions.stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取版本数量 (Get version count)
     *
     * @return 版本数量 (Version count)
     */
    public int getVersionCount() {
        return versions.size();
    }

    /**
     * 获取版本范围 (Get version range)
     *
     * @param fromVersion 起始版本号（包含） (Start version, inclusive)
     * @param toVersion 结束版本号（包含） (End version, inclusive)
     * @return 版本列表 (Version list)
     */
    public List<ConceptVersion> getVersionRange(int fromVersion, int toVersion) {
        return versions.stream()
                .filter(v -> v.getVersionNumber() >= fromVersion && v.getVersionNumber() <= toVersion)
                .toList();
    }

    /**
     * 获取最近的 N 个版本 (Get recent N versions)
     *
     * @param n 版本数量 (Number of versions)
     * @return 版本列表 (Version list)
     */
    public List<ConceptVersion> getRecentVersions(int n) {
        int size = versions.size();
        if (n >= size) {
            return new ArrayList<>(versions);
        }
        return new ArrayList<>(versions.subList(size - n, size));
    }

    /**
     * 按变更类型过滤版本 (Filter versions by change type)
     *
     * @param changeType 变更类型 (Change type)
     * @return 版本列表 (Version list)
     */
    public List<ConceptVersion> getVersionsByChangeType(ConceptVersion.ChangeType changeType) {
        return versions.stream()
                .filter(v -> v.getChangeType() == changeType)
                .toList();
    }

    /**
     * 检查是否有版本 (Check if has versions)
     *
     * @return 是否有版本 (Whether has versions)
     */
    public boolean hasVersions() {
        return !versions.isEmpty();
    }

    /**
     * 获取演化次数 (Get evolution count)
     *
     * @return 演化次数 (Evolution count)
     */
    public int getEvolutionCount() {
        return (int) versions.stream()
                .filter(v -> v.getChangeType() == ConceptVersion.ChangeType.EVOLUTION)
                .count();
    }

    /**
     * 获取修正次数 (Get fix count)
     *
     * @return 修正次数 (Fix count)
     */
    public int getFixCount() {
        return (int) versions.stream()
                .filter(v -> v.getChangeType() == ConceptVersion.ChangeType.FIX)
                .count();
    }

    /**
     * 比较两个版本的差异 (Compare differences between two versions)
     *
     * @param version1 版本1 (Version 1)
     * @param version2 版本2 (Version 2)
     * @return 差异描述 (Difference description)
     */
    public String compareVersions(int version1, int version2) {
        ConceptVersion v1 = getVersion(version1);
        ConceptVersion v2 = getVersion(version2);

        if (v1 == null || v2 == null) {
            return "版本不存在 (Version not found)";
        }

        StringBuilder diff = new StringBuilder();
        diff.append("版本对比 (Version comparison): v").append(version1)
            .append(" -> v").append(version2).append("\n");

        ConceptUnit s1 = v1.getSnapshot();
        ConceptUnit s2 = v2.getSnapshot();

        if (!s1.getDefinition().equals(s2.getDefinition())) {
            diff.append("- 定义变更 (Definition changed)\n");
        }

        if (!s1.getDescription().equals(s2.getDescription())) {
            diff.append("- 描述变更 (Description changed)\n");
        }

        if (s1.getVersion() != s2.getVersion()) {
            diff.append("- 版本号: ").append(s1.getVersion())
                .append(" -> ").append(s2.getVersion()).append("\n");
        }

        return diff.toString();
    }

    /**
     * 获取创建版本 (Get creation version)
     *
     * @return 创建版本 (Creation version)
     */
    public ConceptVersion getCreationVersion() {
        return versions.stream()
                .filter(v -> v.getChangeType() == ConceptVersion.ChangeType.CREATE)
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查是否需要清理旧版本 (Check if needs to clean old versions)
     * 当版本数量超过阈值时返回 true (Return true when version count exceeds threshold)
     *
     * @param threshold 阈值 (Threshold)
     * @return 是否需要清理 (Whether needs cleanup)
     */
    public boolean needsCleanup(int threshold) {
        return versions.size() > threshold;
    }
}

