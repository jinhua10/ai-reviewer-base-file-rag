package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.StrategyConfigService;

import java.util.Map;

/**
 * 策略配置控制器
 * (Strategy Configuration Controller)
 *
 * 提供动态策略配置的 API，让前端可以自动适配后端的策略变化
 * (Provides dynamic strategy configuration API for frontend auto-adaptation)
 */
@RestController
@RequestMapping("/api/strategies")
@Slf4j
public class StrategyConfigController {

    @Autowired
    private StrategyConfigService strategyConfigService;

    /**
     * 获取策略配置
     * (Get strategy configuration)
     *
     * 前端启动时调用，获取所有可用的分析目标和策略配置
     * (Called when frontend starts to get all available analysis goals and strategy config)
     */
    @GetMapping("/config")
    public Map<String, Object> getConfiguration() {
        log.info("📋 Frontend requesting strategy configuration");
        return strategyConfigService.getConfiguration();
    }

    /**
     * 获取策略市场列表
     * (Get strategy marketplace list)
     */
    @GetMapping("/marketplace")
    public Map<String, Object> getMarketplace() {
        log.info("🏪 Frontend requesting strategy marketplace");
        return strategyConfigService.getMarketplace();
    }

    /**
     * 安装策略
     * (Install strategy)
     */
    @PostMapping("/{strategyId}/install")
    public Map<String, Object> installStrategy(@PathVariable String strategyId) {
        log.info("📥 Installing strategy: {}", strategyId);
        return strategyConfigService.installStrategy(strategyId);
    }

    /**
     * 卸载策略
     * (Uninstall strategy)
     */
    @DeleteMapping("/{strategyId}")
    public Map<String, Object> uninstallStrategy(@PathVariable String strategyId) {
        log.info("🗑️ Uninstalling strategy: {}", strategyId);
        return strategyConfigService.uninstallStrategy(strategyId);
    }

    /**
     * 获取单个策略详情
     * (Get single strategy details)
     */
    @GetMapping("/{strategyId}")
    public Map<String, Object> getStrategyDetails(@PathVariable String strategyId) {
        return strategyConfigService.getStrategyDetails(strategyId);
    }
}

