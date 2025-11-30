package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.spring.boot.service.SearchConfigService;

/**
 * 搜索配置管理控制器 / Search Configuration Management Controller
 *
 * @author AI Reviewer Team
 * @since 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/api/search/config")
@CrossOrigin(origins = "*")
public class SearchConfigController {

    private final SearchConfigService configService;

    public SearchConfigController(SearchConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取当前配置 / Get current configuration
     */
    @GetMapping
    public SearchConfigService.SearchConfigInfo getConfig() {
        return configService.getCurrentConfig();
    }

    /**
     * 更新配置 / Update configuration
     */
    @PutMapping
    public SearchConfigService.SearchConfigInfo updateConfig(
            @RequestBody SearchConfigService.SearchConfigUpdate update) {
        configService.updateConfig(update);
        return configService.getCurrentConfig();
    }

    /**
     * 更新单个配置项 / Update individual configuration item
     */
    @PutMapping("/lucene-top-k")
    public SearchConfigService.SearchConfigInfo updateLuceneTopK(@RequestParam int value) {
        configService.setLuceneTopK(value);
        return configService.getCurrentConfig();
    }

    @PutMapping("/vector-top-k")
    public SearchConfigService.SearchConfigInfo updateVectorTopK(@RequestParam int value) {
        configService.setVectorTopK(value);
        return configService.getCurrentConfig();
    }

    @PutMapping("/hybrid-top-k")
    public SearchConfigService.SearchConfigInfo updateHybridTopK(@RequestParam int value) {
        configService.setHybridTopK(value);
        return configService.getCurrentConfig();
    }

    @PutMapping("/documents-per-query")
    public SearchConfigService.SearchConfigInfo updateDocumentsPerQuery(@RequestParam int value) {
        configService.setDocumentsPerQuery(value);
        return configService.getCurrentConfig();
    }

    @PutMapping("/min-score-threshold")
    public SearchConfigService.SearchConfigInfo updateMinScoreThreshold(@RequestParam float value) {
        configService.setMinScoreThreshold(value);
        return configService.getCurrentConfig();
    }

    /**
     * 重置为默认配置 / Reset to default configuration
     */
    @PostMapping("/reset")
    public SearchConfigService.SearchConfigInfo resetConfig() {
        configService.resetToDefault();
        return configService.getCurrentConfig();
    }
}

