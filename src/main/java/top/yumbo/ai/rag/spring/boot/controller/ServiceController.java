package top.yumbo.ai.rag.spring.boot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.service.dto.*;
import top.yumbo.ai.rag.service.ai.AIServiceManager;
import top.yumbo.ai.rag.service.ai.ModelSwitchService;
import top.yumbo.ai.rag.service.ai.PPTGeneratorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务控制器 (AI Service Controller)
 *
 * 提供 AI 服务相关的 API 接口
 * (Provides AI service related API endpoints)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceController {

    private final AIServiceManager serviceManager;
    private final PPTGeneratorService pptGeneratorService;
    private final ModelSwitchService modelSwitchService;

    public ServiceController(AIServiceManager serviceManager,
                           PPTGeneratorService pptGeneratorService,
                           ModelSwitchService modelSwitchService) {
        this.serviceManager = serviceManager;
        this.pptGeneratorService = pptGeneratorService;
        this.modelSwitchService = modelSwitchService;
    }

    /**
     * 获取服务列表 (Get service list)
     *
     * GET /api/services?category=generation&installed=true
     *
     * @param category 分类筛选 (Category filter)
     * @param installed 安装状态筛选 (Installed filter)
     * @return 服务列表 (Service list)
     */
    @GetMapping
    public ResponseEntity<?> getServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean installed) {

        log.info(I18N.get("service.api.list_request"), category, installed);

        try {
            List<ServiceDTO> services = serviceManager.getServices(category, installed);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            log.error(I18N.get("service.api.list_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取服务详情 (Get service detail)
     *
     * GET /api/services/{id}
     *
     * @param id 服务 ID (Service ID)
     * @return 服务详情 (Service detail)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceDetail(@PathVariable String id) {

        log.info(I18N.get("service.api.detail_request"), id);

        try {
            ServiceDTO service = serviceManager.getServiceDetail(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            log.error(I18N.get("service.api.detail_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 安装服务 (Install service)
     *
     * POST /api/services/{id}/install
     *
     * @param id 服务 ID (Service ID)
     * @return 安装结果 (Install result)
     */
    @PostMapping("/{id}/install")
    public ResponseEntity<?> installService(@PathVariable String id) {

        log.info(I18N.get("service.api.install_request"), id);

        try {
            Map<String, Object> result = serviceManager.installService(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("service.api.install_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 卸载服务 (Uninstall service)
     *
     * POST /api/services/{id}/uninstall
     *
     * @param id 服务 ID (Service ID)
     * @return 卸载结果 (Uninstall result)
     */
    @PostMapping("/{id}/uninstall")
    public ResponseEntity<?> uninstallService(@PathVariable String id) {

        log.info(I18N.get("service.api.uninstall_request"), id);

        try {
            Map<String, Object> result = serviceManager.uninstallService(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("service.api.uninstall_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 更新服务配置 (Update service configuration)
     *
     * PUT /api/services/{id}/config
     * Body: { "key": "value" }
     *
     * @param id 服务 ID (Service ID)
     * @param config 配置 (Configuration)
     * @return 更新结果 (Update result)
     */
    @PutMapping("/{id}/config")
    public ResponseEntity<?> updateServiceConfig(
            @PathVariable String id,
            @RequestBody Map<String, Object> config) {

        log.info(I18N.get("service.api.config_request"), id);

        try {
            Map<String, Object> result = serviceManager.updateServiceConfig(id, config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("service.api.config_error", id), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 生成 PPT (Generate PPT)
     *
     * POST /api/services/ppt/generate
     * Body: {
     *   "topic": "主题",
     *   "content": "内容",
     *   "slides": 5,
     *   "template": "default",
     *   "style": "modern"
     * }
     *
     * @param request PPT 生成请求 (PPT generation request)
     * @return 生成结果 (Generation result)
     */
    @PostMapping("/ppt/generate")
    public ResponseEntity<?> generatePPT(@RequestBody PPTGenerateRequest request) {

        log.info(I18N.get("service.api.ppt_request"), request.getTopic());

        try {
            PPTGenerateResult result = pptGeneratorService.generatePPT(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("service.api.ppt_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 切换模型 (Switch model)
     *
     * POST /api/services/model/switch
     * Body: {
     *   "modelType": "local",  // or "online-openai", "online-deepseek", "custom"
     *   "customEndpoint": "...",  // 可选，自定义模型时需要
     *   "customModel": "..."      // 可选，自定义模型时需要
     * }
     *
     * @param request 模型切换请求 (Model switch request)
     * @return 切换结果 (Switch result)
     */
    @PostMapping("/model/switch")
    public ResponseEntity<?> switchModel(@RequestBody Map<String, String> request) {

        String modelType = request.get("modelType");
        log.info(I18N.get("service.api.model_request"), modelType);

        try {
            String customEndpoint = request.get("customEndpoint");
            String customModel = request.get("customModel");

            ModelSwitchService.SwitchResult result = modelSwitchService.switchModel(
                modelType, customEndpoint, customModel);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(I18N.get("service.api.model_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取当前模型配置 (Get current model configuration)
     *
     * GET /api/services/model/current
     *
     * @return 当前模型配置 (Current model configuration)
     */
    @GetMapping("/model/current")
    public ResponseEntity<?> getCurrentModel() {
        log.info(I18N.get("service.api.model_current_request"));

        try {
            ModelSwitchService.ModelConfig config = modelSwitchService.getCurrentConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error(I18N.get("service.api.model_current_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取可用模型列表 (Get available models)
     *
     * GET /api/services/model/available
     *
     * @return 可用模型列表 (Available models list)
     */
    @GetMapping("/model/available")
    public ResponseEntity<?> getAvailableModels() {
        log.info(I18N.get("service.api.model_available_request"));

        try {
            List<ModelSwitchService.ModelConfig> models = modelSwitchService.getAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error(I18N.get("service.api.model_available_error"), e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 创建错误响应 (Create error response)
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}

