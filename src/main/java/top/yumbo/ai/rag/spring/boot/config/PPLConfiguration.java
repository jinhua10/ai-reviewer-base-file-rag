package top.yumbo.ai.rag.spring.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.ppl.PPLServiceFacade;
import top.yumbo.ai.rag.ppl.config.PPLConfig;
import top.yumbo.ai.rag.ppl.onnx.PPLOnnxService;
import java.util.ArrayList;
import java.util.List;

/**
 * PPL 服务配置类
 * 配置 PPL 服务和相关依赖
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
     * ONNX PPL 服务 Bean
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge.qa.ppl.onnx", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PPLOnnxService pplOnnxService(PPLConfig config) {
        log.info("🚀 初始化 ONNX PPL 服务...");
        PPLOnnxService service = new PPLOnnxService(config);

        // 初始化服务
        try {
            service.init();
            log.info("✅ ONNX PPL 服务初始化成功");
        } catch (Exception e) {
            log.error("❌ ONNX PPL 服务初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize ONNX PPL service", e);
        }

        return service;
    }

    /**
     * PPL 服务门面 Bean
     */
    @Bean
    public PPLServiceFacade pplServiceFacade(PPLConfig config, PPLOnnxService onnxService) {
        log.info("🚀 初始化 PPL 服务门面...");
        
        // 将可用的 PPL 服务放入列表
        List<top.yumbo.ai.rag.ppl.PPLService> availableServices = new ArrayList<>();
        availableServices.add(onnxService);
        
        // 使用正确的构造函数：PPLServiceFacade(PPLConfig, List<PPLService>)
        PPLServiceFacade facade = new PPLServiceFacade(config, availableServices);
        
        log.info("✅ PPL 服务门面初始化成功，默认提供商: {}", config.getDefaultProvider());
        
        return facade;
    }
}

