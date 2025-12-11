package top.yumbo.ai.rag.ai.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 服务注册中心 (AI Service Registry)
 *
 * 功能 (Features):
 * 1. 插件化服务注册 (Plugin-based service registration)
 * 2. 服务发现 (Service discovery)
 * 3. 服务调用 (Service invocation)
 * 4. 服务统计 (Service statistics)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class AIServiceRegistry {

    /**
     * 已注册的服务 (Registered services)
     */
    private final Map<String, AIService> services = new ConcurrentHashMap<>();

    /**
     * 服务调用统计 (Service invocation statistics)
     */
    private final Map<String, ServiceStats> stats = new ConcurrentHashMap<>();

    // ========== 初始化 (Initialization) ==========

    public AIServiceRegistry() {
        registerBuiltInServices();
        log.info(I18N.get("ai.service.registry.initialized"));
    }

    /**
     * 注册内置服务 (Register built-in services)
     */
    private void registerBuiltInServices() {
        // 示例：PPT生成服务 (Example: PPT generation service)
        registerService(new PPTGenerationService());

        // 可以添加更多内置服务 (Can add more built-in services)
        log.debug(I18N.get("ai.service.builtin.registered"), services.size());
    }

    // ========== 服务注册 (Service Registration) ==========

    /**
     * 注册服务 (Register service)
     *
     * @param service AI服务 (AI service)
     */
    public void registerService(AIService service) {
        services.put(service.getServiceId(), service);
        stats.put(service.getServiceId(), new ServiceStats(service.getServiceId()));

        log.info(I18N.get("ai.service.registered"),
            service.getServiceName(), service.getServiceId());
    }

    /**
     * 注销服务 (Unregister service)
     *
     * @param serviceId 服务ID (Service ID)
     */
    public void unregisterService(String serviceId) {
        AIService service = services.remove(serviceId);
        if (service != null) {
            log.info(I18N.get("ai.service.unregistered"), serviceId);
        }
    }

    // ========== 服务调用 (Service Invocation) ==========

    /**
     * 调用服务 (Invoke service)
     *
     * @param serviceId 服务ID (Service ID)
     * @param request 请求 (Request)
     * @return 响应 (Response)
     */
    public ServiceResponse invoke(String serviceId, ServiceRequest request) {
        try {
            // 1. 查找服务 (Find service)
            AIService service = services.get(serviceId);
            if (service == null) {
                log.warn(I18N.get("ai.service.not_found"), serviceId);
                return ServiceResponse.error("Service not found: " + serviceId);
            }

            // 2. 检查服务状态 (Check service status)
            if (!service.isEnabled()) {
                log.warn(I18N.get("ai.service.disabled"), serviceId);
                return ServiceResponse.error("Service disabled: " + serviceId);
            }

            // 3. 调用服务 (Invoke service)
            log.info(I18N.get("ai.service.invoking"), serviceId);
            long startTime = System.currentTimeMillis();

            ServiceResponse response = service.execute(request);

            long duration = System.currentTimeMillis() - startTime;

            // 4. 更新统计 (Update statistics)
            updateStats(serviceId, duration, response.isSuccess());

            log.info(I18N.get("ai.service.invoked"), serviceId, duration);
            return response;

        } catch (Exception e) {
            log.error(I18N.get("ai.service.invoke_failed"), serviceId, e.getMessage(), e);
            updateStats(serviceId, 0, false);
            return ServiceResponse.error(e.getMessage());
        }
    }

    /**
     * 更新统计信息 (Update statistics)
     */
    private void updateStats(String serviceId, long duration, boolean success) {
        ServiceStats stat = stats.get(serviceId);
        if (stat != null) {
            stat.incrementInvocationCount();
            if (success) {
                stat.incrementSuccessCount();
            } else {
                stat.incrementFailureCount();
            }
            stat.addDuration(duration);
        }
    }

    // ========== 服务发现 (Service Discovery) ==========

    /**
     * 获取所有服务 (Get all services)
     */
    public List<AIService> getAllServices() {
        return new ArrayList<>(services.values());
    }

    /**
     * 按类别获取服务 (Get services by category)
     */
    public List<AIService> getServicesByCategory(String category) {
        return services.values().stream()
            .filter(s -> category.equals(s.getCategory()))
            .toList();
    }

    /**
     * 获取服务统计 (Get service statistics)
     */
    public ServiceStats getServiceStats(String serviceId) {
        return stats.get(serviceId);
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * AI 服务接口 (AI Service Interface)
     */
    public interface AIService {
        String getServiceId();
        String getServiceName();
        String getDescription();
        String getCategory();
        boolean isEnabled();
        ServiceResponse execute(ServiceRequest request);
    }

    /**
     * 服务请求 (Service Request)
     */
    @Data
    public static class ServiceRequest {
        private String userId;
        private Map<String, Object> parameters;
        private LocalDateTime requestTime;
    }

    /**
     * 服务响应 (Service Response)
     */
    @Data
    public static class ServiceResponse {
        private boolean success;
        private Object result;
        private String errorMessage;
        private LocalDateTime responseTime;

        public static ServiceResponse success(Object result) {
            ServiceResponse response = new ServiceResponse();
            response.success = true;
            response.result = result;
            response.responseTime = LocalDateTime.now();
            return response;
        }

        public static ServiceResponse error(String message) {
            ServiceResponse response = new ServiceResponse();
            response.success = false;
            response.errorMessage = message;
            response.responseTime = LocalDateTime.now();
            return response;
        }
    }

    /**
     * 服务统计 (Service Statistics)
     */
    @Data
    public static class ServiceStats {
        private final String serviceId;
        private long invocationCount = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private long totalDuration = 0;

        public void incrementInvocationCount() { invocationCount++; }
        public void incrementSuccessCount() { successCount++; }
        public void incrementFailureCount() { failureCount++; }
        public void addDuration(long duration) { totalDuration += duration; }

        public double getSuccessRate() {
            return invocationCount == 0 ? 0 : (double) successCount / invocationCount;
        }

        public double getAvgDuration() {
            return invocationCount == 0 ? 0 : (double) totalDuration / invocationCount;
        }
    }

    /**
     * PPT 生成服务示例 (PPT Generation Service Example)
     */
    public static class PPTGenerationService implements AIService {

        @Override
        public String getServiceId() {
            return "ppt-generation";
        }

        @Override
        public String getServiceName() {
            return "PPT生成服务";
        }

        @Override
        public String getDescription() {
            return "根据文本内容自动生成PPT";
        }

        @Override
        public String getCategory() {
            return "document";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public ServiceResponse execute(ServiceRequest request) {
            try {
                // TODO: 实现实际的PPT生成逻辑
                String content = (String) request.getParameters().get("content");

                // 模拟生成结果 (Simulate generation)
                Map<String, Object> result = new HashMap<>();
                result.put("pptPath", "/generated/ppt-" + System.currentTimeMillis() + ".pptx");
                result.put("slideCount", 10);

                return ServiceResponse.success(result);

            } catch (Exception e) {
                return ServiceResponse.error(e.getMessage());
            }
        }
    }
}

