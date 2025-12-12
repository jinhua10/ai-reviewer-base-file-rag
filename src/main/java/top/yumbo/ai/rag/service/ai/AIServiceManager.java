package top.yumbo.ai.rag.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.model.service.AIService;
import top.yumbo.ai.rag.model.service.dto.ServiceDTO;
import top.yumbo.ai.rag.spring.boot.autoconfigure.SimpleRAGService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 服务管理器 (AI Service Manager)
 *
 * 管理可安装的 AI 服务
 * (Manages installable AI services)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class AIServiceManager {

    private final SimpleRAGService ragService;
    private final ObjectMapper objectMapper;

    private static final String DOC_TYPE_SERVICE = "ai-service";

    // 预定义的服务列表 (Predefined services)
    private static final List<AIService> AVAILABLE_SERVICES = new ArrayList<>();

    static {
        // PPT 生成器 (PPT Generator)
        AIService pptService = new AIService();
        pptService.setId("ppt-generator");
        pptService.setName("PPT 生成器");
        pptService.setDescription("基于主题和内容自动生成 PPT 演示文稿");
        pptService.setCategory("generation");
        pptService.setVersion("1.0.0");
        pptService.setIcon("📊");
        pptService.setFeatures(Arrays.asList("自动生成", "多种模板", "智能排版"));
        AVAILABLE_SERVICES.add(pptService);

        // 文档摘要 (Document Summary)
        AIService summaryService = new AIService();
        summaryService.setId("doc-summarizer");
        summaryService.setName("文档摘要");
        summaryService.setDescription("自动提取文档关键信息，生成摘要");
        summaryService.setCategory("analysis");
        summaryService.setVersion("1.0.0");
        summaryService.setIcon("📝");
        summaryService.setFeatures(Arrays.asList("智能提取", "多语言支持", "关键词标注"));
        AVAILABLE_SERVICES.add(summaryService);

        // 代码生成器 (Code Generator)
        AIService codeService = new AIService();
        codeService.setId("code-generator");
        codeService.setName("代码生成器");
        codeService.setDescription("根据需求描述自动生成代码");
        codeService.setCategory("generation");
        codeService.setVersion("1.0.0");
        codeService.setIcon("💻");
        codeService.setFeatures(Arrays.asList("多语言支持", "单元测试生成", "代码优化建议"));
        AVAILABLE_SERVICES.add(codeService);
    }

    public AIServiceManager(SimpleRAGService ragService) {
        this.ragService = ragService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取所有可用服务 (Get all available services)
     *
     * @param category 分类筛选 (Category filter)
     * @param installed 安装状态筛选 (Installed filter)
     * @return 服务列表 (Service list)
     */
    public List<ServiceDTO> getServices(String category, Boolean installed) {
        log.info(I18N.get("service.list.loading"), category, installed);

        try {
            // 获取已安装的服务 (Get installed services)
            List<Document> installedDocs = ragService.search("type:" + DOC_TYPE_SERVICE, 100);
            Set<String> installedIds = installedDocs.stream()
                .map(doc -> (String) doc.getMetadata().get("serviceId"))
                .collect(Collectors.toSet());

            // 合并预定义服务和已安装状态 (Merge predefined services with installed status)
            List<ServiceDTO> services = AVAILABLE_SERVICES.stream()
                .map(service -> {
                    ServiceDTO dto = toDTO(service);
                    dto.setInstalled(installedIds.contains(service.getId()));
                    return dto;
                })
                .filter(dto -> category == null || category.equals(dto.getCategory()))
                .filter(dto -> installed == null || installed.equals(dto.isInstalled()))
                .collect(Collectors.toList());

            log.info(I18N.get("service.list.success"), services.size());
            return services;

        } catch (Exception e) {
            log.error(I18N.get("service.list.failed", e.getMessage()), e);
            throw new RuntimeException(I18N.get("service.list.failed", e.getMessage()), e);
        }
    }

    /**
     * 获取服务详情 (Get service detail)
     *
     * @param serviceId 服务 ID (Service ID)
     * @return 服务 DTO (Service DTO)
     */
    public ServiceDTO getServiceDetail(String serviceId) {
        log.info(I18N.get("service.detail.loading"), serviceId);

        try {
            AIService service = AVAILABLE_SERVICES.stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(I18N.get("service.not_found", serviceId)));

            ServiceDTO dto = toDTO(service);

            // 检查是否已安装 (Check if installed)
            Document doc = ragService.getDocument(serviceId);
            dto.setInstalled(doc != null);

            log.info(I18N.get("service.detail.success"), serviceId);
            return dto;

        } catch (Exception e) {
            log.error(I18N.get("service.detail.failed", serviceId, e.getMessage()), e);
            throw new RuntimeException(I18N.get("service.detail.failed", serviceId, e.getMessage()), e);
        }
    }

    /**
     * 安装服务 (Install service)
     *
     * @param serviceId 服务 ID (Service ID)
     * @return 安装结果 (Install result)
     */
    public Map<String, Object> installService(String serviceId) {
        log.info(I18N.get("service.install.start"), serviceId);

        try {
            // 检查服务是否存在 (Check if service exists)
            AIService service = AVAILABLE_SERVICES.stream()
                .filter(s -> s.getId().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(I18N.get("service.not_found", serviceId)));

            // 检查是否已安装 (Check if already installed)
            Document existingDoc = ragService.getDocument(serviceId);
            if (existingDoc != null) {
                throw new RuntimeException(I18N.get("service.already_installed", serviceId));
            }

            // 创建服务文档 (Create service document)
            service.setInstalled(true);
            service.setInstalledAt(LocalDateTime.now());

            String serviceJson = objectMapper.writeValueAsString(service);

            Document doc = new Document();
            doc.setId(service.getId());
            doc.setTitle(service.getName());
            doc.setContent(serviceJson);
            doc.setMetadata(service.toMetadata());

            // 索引到文档系统 (Index to document system)
            ragService.getRag().index(doc);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", I18N.get("service.install.success", service.getName()));
            result.put("serviceId", serviceId);

            log.info(I18N.get("service.install.success"), serviceId);
            return result;

        } catch (Exception e) {
            log.error(I18N.get("service.install.failed", serviceId, e.getMessage()), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", I18N.get("service.install.failed", serviceId, e.getMessage()));
            return result;
        }
    }

    /**
     * 卸载服务 (Uninstall service)
     *
     * @param serviceId 服务 ID (Service ID)
     * @return 卸载结果 (Uninstall result)
     */
    public Map<String, Object> uninstallService(String serviceId) {
        log.info(I18N.get("service.uninstall.start"), serviceId);

        try {
            // 删除服务文档 (Delete service document)
            boolean deleted = ragService.deleteDocument(serviceId);

            Map<String, Object> result = new HashMap<>();
            if (deleted) {
                result.put("success", true);
                result.put("message", I18N.get("service.uninstall.success", serviceId));
                log.info(I18N.get("service.uninstall.success"), serviceId);
            } else {
                result.put("success", false);
                result.put("message", I18N.get("service.not_installed", serviceId));
            }

            return result;

        } catch (Exception e) {
            log.error(I18N.get("service.uninstall.failed", serviceId, e.getMessage()), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", I18N.get("service.uninstall.failed", serviceId, e.getMessage()));
            return result;
        }
    }

    /**
     * 更新服务配置 (Update service configuration)
     *
     * @param serviceId 服务 ID (Service ID)
     * @param config 配置 (Configuration)
     * @return 更新结果 (Update result)
     */
    public Map<String, Object> updateServiceConfig(String serviceId, Map<String, Object> config) {
        log.info(I18N.get("service.config.update_start"), serviceId);

        try {
            // 获取服务文档 (Get service document)
            Document doc = ragService.getDocument(serviceId);
            if (doc == null) {
                throw new RuntimeException(I18N.get("service.not_installed", serviceId));
            }

            // 更新配置 (Update configuration)
            AIService service = objectMapper.readValue(doc.getContent(), AIService.class);
            service.setConfig(config);
            service.setUpdatedAt(LocalDateTime.now());

            // 重新索引 (Re-index)
            String serviceJson = objectMapper.writeValueAsString(service);
            doc.setContent(serviceJson);
            doc.setMetadata(service.toMetadata());
            ragService.getRag().index(doc);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", I18N.get("service.config.update_success", serviceId));

            log.info(I18N.get("service.config.update_success"), serviceId);
            return result;

        } catch (Exception e) {
            log.error(I18N.get("service.config.update_failed", serviceId, e.getMessage()), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", I18N.get("service.config.update_failed", serviceId, e.getMessage()));
            return result;
        }
    }

    /**
     * 转换为 DTO (Convert to DTO)
     */
    private ServiceDTO toDTO(AIService service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setCategory(service.getCategory());
        dto.setVersion(service.getVersion());
        dto.setInstalled(service.isInstalled());
        dto.setIcon(service.getIcon());
        dto.setFeatures(service.getFeatures());
        dto.setConfig(service.getConfig());
        return dto;
    }
}

