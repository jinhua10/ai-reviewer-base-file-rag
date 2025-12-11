package top.yumbo.ai.rag.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 索引统计信息 (Index Statistics)
 *
 * 记录索引的统计数据，用于监控和管理
 * (Records index statistical data for monitoring and management)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatistics {

    /**
     * 角色ID (Role ID)
     */
    private String roleId;

    /**
     * 角色名称 (Role name)
     */
    private String roleName;

    /**
     * 索引状态 (Index status)
     */
    private RoleVectorIndex.IndexStatus status;

    /**
     * 文档数量 (Document count)
     */
    private int documentCount;

    /**
     * 索引路径 (Index path)
     */
    private String indexPath;

    /**
     * 创建时间 (Creation time)
     */
    private Instant createdAt;

    /**
     * 最后加载时间 (Last load time)
     */
    private Instant lastLoadTime;

    /**
     * 最后访问时间 (Last access time)
     */
    private Instant lastAccessTime;

    /**
     * 索引大小（字节） (Index size in bytes)
     */
    private long indexSizeBytes;

    /**
     * 搜索次数 (Search count)
     */
    private long searchCount;

    /**
     * 平均搜索时间（毫秒） (Average search time in ms)
     */
    private double avgSearchTimeMs;
}

