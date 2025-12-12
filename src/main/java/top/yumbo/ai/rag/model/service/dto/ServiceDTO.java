package top.yumbo.ai.rag.model.service.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 服务 DTO (Service DTO)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class ServiceDTO {

    private String id;
    private String name;
    private String description;
    private String category;
    private String version;
    private boolean installed;
    private String icon;
    private List<String> features;
    private Map<String, Object> config;
}

