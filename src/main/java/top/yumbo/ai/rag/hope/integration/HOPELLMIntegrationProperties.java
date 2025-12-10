package top.yumbo.ai.rag.hope.integration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HOPE LLM 集成配置属性类 (HOPE LLM Integration Configuration Properties)
 * 从 application.yml 读取配置
 * (Read configuration from application.yml)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge.qa.hope.llm-integration")
public class HOPELLMIntegrationProperties {

    /**
     * 是否在 LLM 调用前查询 HOPE
     * (Whether to query HOPE before LLM call)
     */
    private boolean queryBeforeLlm = true;

    /**
     * 是否启用自动学习（每次 LLM 调用后自动学习）
     * (Whether to enable auto learning after each LLM call)
     */
    private boolean autoLearnEnabled = true;

    /**
     * 自动学习的默认评分（1-5）
     * (Default rating for auto learning, 1-5)
     * 3 分表示一般质量，会进入高频层但不会进入中频层
     * (Rating 3 means average quality, enters high-frequency layer but not ordinary layer)
     */
    private int autoLearnRating = 3;

    /**
     * 是否启用参考增强（将相似问答作为上下文）
     * (Whether to enable reference enhancement - use similar QA as context)
     */
    private boolean referenceEnhanceEnabled = true;

    /**
     * 手动反馈学习的最小评分（只有 ≥ 此评分才会学习）
     * (Minimum rating for manual feedback learning - only ratings >= this value will be learned)
     */
    private int minRatingForLearning = 4;
}

