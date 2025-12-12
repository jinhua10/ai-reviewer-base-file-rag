package top.yumbo.ai.rag.model.service.dto;

import lombok.Data;

/**
 * PPT 生成请求 (PPT Generation Request)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Data
public class PPTGenerateRequest {

    /**
     * 主题 (Topic)
     */
    private String topic;

    /**
     * 内容 (Content)
     */
    private String content;

    /**
     * 页数 (Slides count)
     */
    private int slides = 5;

    /**
     * 模板 (Template)
     */
    private String template = "default";

    /**
     * 风格 (Style)
     */
    private String style = "modern";
}

