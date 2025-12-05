package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.i18n.LogMessageProvider;
import top.yumbo.ai.rag.ppl.PPLServiceFacade;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.ppl.onnx.PPLOnnxService;
import java.util.ArrayList;
import java.util.List;

/**
 * PPL 服务配置类（PPL Service Configuration）
 * 配置 PPL 服务和相关依赖（Configure PPL services and dependencies）
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PPLConfig.class)
@ConditionalOnProperty(prefix = "knowledge.qa.ppl", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PPLConfiguration {


    /**
     * ONNX PPL 服务 Bean（ONNX PPL Service Bean）
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge.qa.ppl.onnx", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PPLOnnxService pplOnnxService(PPLConfig config) {
        log.info(LogMessageProvider.getMessage("ppl.log.onnx_init_start"));
        PPLOnnxService service = new PPLOnnxService(config);

        // 初始化服务（Initialize service）
        try {
            service.init();
            log.info(LogMessageProvider.getMessage("ppl.log.onnx_init_success"));
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("ppl.log.onnx_init_failed", e.getMessage()), e);
            throw new RuntimeException("Failed to initialize ONNX PPL service", e);
        }

        return service;
    }

    /**
     * PPL 服务门面 Bean（PPL Service Facade Bean）
     */
    @Bean
    public PPLServiceFacade pplServiceFacade(PPLConfig config, PPLOnnxService onnxService) {
        log.info(LogMessageProvider.getMessage("ppl.log.facade_init_start"));

        // 将可用的 PPL 服务放入列表（Put available PPL services into list）
        List<top.yumbo.ai.rag.ppl.PPLService> availableServices = new ArrayList<>();
        availableServices.add(onnxService);
        
        // 使用正确的构造函数（Use correct constructor）
        PPLServiceFacade facade = new PPLServiceFacade(config, availableServices);
        
        log.info(LogMessageProvider.getMessage("ppl.log.facade_init_success", config.getDefaultProvider()));

        return facade;
    }
}

