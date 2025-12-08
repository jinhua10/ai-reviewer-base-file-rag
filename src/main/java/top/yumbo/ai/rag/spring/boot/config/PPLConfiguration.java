package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.ppl.PPLServiceFacade;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.ppl.onnx.PPLOnnxService;

import java.util.ArrayList;
import java.util.List;

/**
 * PPL 服务配置类（PPL Service Configuration）
 * 配置 PPL 服务和相关依赖（Configure PPL services and dependencies）
 *
 * 注意：PPLConfig 始终可用，具体服务根据配置条件创建
 * (Note: PPLConfig is always available, specific services created based on config conditions)
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PPLConfig.class)
public class PPLConfiguration {


    /**
     * ONNX PPL 服务 Bean（ONNX PPL Service Bean）
     * 只在 PPL 启用且 ONNX 配置启用时创建
     * (Created only when PPL is enabled and ONNX config is enabled)
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge.qa.ppl.onnx", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PPLOnnxService pplOnnxService(PPLConfig config) {
        log.info(I18N.get("ppl.log.onnx_init_start"));
        PPLOnnxService service = new PPLOnnxService(config);

        // 初始化服务（Initialize service）
        try {
            service.init();
            log.info(I18N.get("ppl.log.onnx_init_success"));
        } catch (Exception e) {
            log.error(I18N.get("ppl.log.onnx_init_failed", e.getMessage()), e);
            throw new RuntimeException("Failed to initialize ONNX PPL service", e);
        }

        return service;
    }

    /**
     * PPL 服务门面 Bean（PPL Service Facade Bean）
     * 只在 PPL 启用时创建
     * (Created only when PPL is enabled)
     *
     * PPLOnnxService 是可选依赖，只有在配置启用时才会注入
     * (PPLOnnxService is optional, injected only when configured)
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge.qa.ppl", name = "enabled", havingValue = "true", matchIfMissing = false)
    public PPLServiceFacade pplServiceFacade(PPLConfig config,
                                             @Autowired(required = false) PPLOnnxService onnxService) {
        log.info(I18N.get("ppl.log.facade_init_start"));

        // 将可用的 PPL 服务放入列表（Put available PPL services into list）
        List<top.yumbo.ai.rag.ppl.PPLService> availableServices = new ArrayList<>();

        // 只有当 ONNX 服务存在时才添加（Add ONNX service only if available）
        if (onnxService != null) {
            availableServices.add(onnxService);
            log.info("✅ ONNX PPL service registered");
        } else {
            log.info("ℹ️ ONNX PPL service not available (using other providers)");
        }

        // 使用正确的构造函数（Use correct constructor）
        PPLServiceFacade facade = new PPLServiceFacade(config, availableServices);
        
        log.info(I18N.get("ppl.log.facade_init_success", config.getDefaultProvider()));

        return facade;
    }
}

