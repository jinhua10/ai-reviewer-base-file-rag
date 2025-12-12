package top.yumbo.ai.rag.model.service;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务模型 (AI Service Model)
 *
 * 表示一个可安装的 AI 服务
 * (Represents an installable AI service)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class AIService {

    /**
     * 服务 ID (Service ID)
     */
    private String id;

    /**
     * 服务名称 (Service name)
     */
    private String name;

    /**
     * 描述 (Description)
     */
    private String description;

    /**
     * 分类 (Category)
     * 例如: generation, analysis, transformation
     */
    private String category;

    /**
     * 版本 (Version)
     */
    private String version;

    /**
     * 是否已安装 (Installed)
     */
    private boolean installed = false;

    /**
     * 图标 URL (Icon URL)
     */
    private String icon;

    /**
     * 功能列表 (Features)
     */
    private List<String> features = new ArrayList<>();

    /**
     * 配置 (Configuration)
     */
    private Map<String, Object> config = new HashMap<>();

    /**
     * 创建时间 (Created at)
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 更新时间 (Updated at)
     */
    private LocalDateTime updatedAt;

    /**
     * 安装时间 (Installed at)
     */
    private LocalDateTime installedAt;

    /**
     * 转换为文档元数据 (Convert to document metadata)
     */
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "ai-service");
        metadata.put("serviceId", id);
        metadata.put("name", name);
        metadata.put("category", category);
        metadata.put("version", version);
        metadata.put("installed", installed);
        metadata.put("createdAt", createdAt != null ? createdAt.toString() : null);
        metadata.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        metadata.put("installedAt", installedAt != null ? installedAt.toString() : null);
        return metadata;
    }
}

