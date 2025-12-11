package top.yumbo.ai.rag.index;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.Document;
import top.yumbo.ai.rag.role.Role;
import top.yumbo.ai.rag.role.RoleManager;
import top.yumbo.ai.rag.role.detector.RoleDetector;
import top.yumbo.ai.rag.role.detector.RoleDetectionResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档分类器 (Document Classifier)
 *
 * 将文档分类到对应的角色知识库
 * (Classifies documents to corresponding role knowledge bases)
 *
 * 分类策略 (Classification Strategy):
 * 1. 基于内容的角色识别 (Content-based role identification)
 * 2. 基于标签/元数据 (Tag/metadata-based)
 * 3. 基于文档类型 (Document type-based)
 * 4. 多角色分配 (Multi-role assignment)
 *
 * @author AI Reviewer Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class DocumentClassifier {

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private RoleDetector roleDetector;

    /**
     * 分类单个文档 (Classify single document)
     *
     * @param document 文档 (Document)
     * @return 分类结果 (Classification result)
     */
    public ClassificationResult classify(Document document) {
        if (document == null) {
            log.warn(I18N.get("classifier.document.null"));
            return ClassificationResult.empty();
        }

        log.debug(I18N.get("classifier.classifying", document.getId()));

        try {
            // 1. 准备分类内容 (Prepare classification content)
            String content = prepareContent(document);

            // 2. 使用角色检测器识别角色 (Use role detector to identify roles)
            RoleDetectionResult detection = roleDetector.detect(content, null);

            // 3. 构建分类结果 (Build classification result)
            List<Role> primaryRoles = new ArrayList<>();
            List<Role> secondaryRoles = new ArrayList<>();

            if (detection.getSelectedRole() != null) {
                primaryRoles.add(detection.getSelectedRole());

                // 添加其他高置信度角色作为次要角色 (Add other high-confidence roles as secondary)
                if (detection.getAllCandidates() != null) {
                    detection.getAllCandidates().stream()
                            .filter(r -> !r.getRoleId().equals(detection.getSelectedRole().getId()))
                            .filter(r -> r.getConfidence() > 0.5)
                            .limit(2)
                            .forEach(r -> {
                                Role role = roleManager.getRole(r.getRoleId());
                                if (role != null) {
                                    secondaryRoles.add(role);
                                }
                            });
                }
            }

            // 4. 如果没有识别到角色，使用默认角色 (Use default role if none identified)
            if (primaryRoles.isEmpty()) {
                Role defaultRole = roleManager.getDefaultRole();
                if (defaultRole != null) {
                    primaryRoles.add(defaultRole);
                }
            }

            ClassificationResult result = ClassificationResult.builder()
                    .documentId(document.getId())
                    .primaryRoles(primaryRoles)
                    .secondaryRoles(secondaryRoles)
                    .confidence(detection.getConfidence())
                    .reason(buildReason(detection))
                    .build();

            log.info(I18N.get("classifier.classified", document.getId(),
                    primaryRoles.stream().map(Role::getName).collect(Collectors.joining(", ")),
                    detection.getConfidence()));

            return result;

        } catch (Exception e) {
            log.error(I18N.get("classifier.error", document.getId(), e.getMessage()), e);
            return ClassificationResult.error(document.getId(), e.getMessage());
        }
    }

    /**
     * 批量分类文档 (Batch classify documents)
     *
     * @param documents 文档列表 (Document list)
     * @return 分类结果列表 (Classification result list)
     */
    public List<ClassificationResult> classifyBatch(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn(I18N.get("classifier.documents.empty"));
            return Collections.emptyList();
        }

        log.info(I18N.get("classifier.batch.start", documents.size()));
        long startTime = System.currentTimeMillis();

        List<ClassificationResult> results = documents.stream()
                .map(this::classify)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info(I18N.get("classifier.batch.complete", documents.size(), duration));

        return results;
    }

    /**
     * 按角色分组文档 (Group documents by role)
     *
     * @param documents 文档列表 (Document list)
     * @return 按角色分组的文档 (Documents grouped by role)
     */
    public Map<Role, List<Document>> groupByRole(List<Document> documents) {
        List<ClassificationResult> results = classifyBatch(documents);

        Map<Role, List<Document>> grouped = new HashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            ClassificationResult result = results.get(i);

            // 添加到主要角色 (Add to primary roles)
            for (Role role : result.getPrimaryRoles()) {
                grouped.computeIfAbsent(role, k -> new ArrayList<>()).add(doc);
            }

            // 添加到次要角色 (Add to secondary roles)
            for (Role role : result.getSecondaryRoles()) {
                grouped.computeIfAbsent(role, k -> new ArrayList<>()).add(doc);
            }
        }

        log.info(I18N.get("classifier.grouped", documents.size(), grouped.size()));
        return grouped;
    }

    /**
     * 准备分类内容 (Prepare content for classification)
     *
     * @param document 文档 (Document)
     * @return 分类内容 (Classification content)
     */
    private String prepareContent(Document document) {
        StringBuilder content = new StringBuilder();

        // 标题 (Title)
        if (document.getTitle() != null && !document.getTitle().isEmpty()) {
            content.append(document.getTitle()).append(" ");
        }

        // 内容摘要（前1000字符） (Content excerpt - first 1000 characters)
        if (document.getContent() != null && !document.getContent().isEmpty()) {
            String excerpt = document.getContent().length() > 1000
                    ? document.getContent().substring(0, 1000)
                    : document.getContent();
            content.append(excerpt);
        }

        // 标签 (Tags)
        if (document.getTags() != null && document.getTags().length > 0) {
            content.append(" [标签: ").append(String.join(", ", document.getTags())).append("]");
        }

        return content.toString();
    }

    /**
     * 构建分类原因说明 (Build classification reason)
     *
     * @param detection 检测结果 (Detection result)
     * @return 原因说明 (Reason description)
     */
    private String buildReason(RoleDetectionResult detection) {
        if (detection.getDetectionMethods() != null && !detection.getDetectionMethods().isEmpty()) {
            return String.format("使用方法: %s, 置信度: %.2f",
                    String.join(", ", detection.getDetectionMethods()),
                    detection.getConfidence());
        }
        return "默认分类";
    }

    /**
     * 分类结果 (Classification Result)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClassificationResult {
        /**
         * 文档ID (Document ID)
         */
        private String documentId;

        /**
         * 主要角色列表 (Primary role list)
         */
        private List<Role> primaryRoles;

        /**
         * 次要角色列表 (Secondary role list)
         */
        private List<Role> secondaryRoles;

        /**
         * 置信度 (Confidence)
         */
        private double confidence;

        /**
         * 分类原因 (Classification reason)
         */
        private String reason;

        /**
         * 是否有错误 (Whether has error)
         */
        private boolean hasError;

        /**
         * 错误消息 (Error message)
         */
        private String errorMessage;

        /**
         * 创建空结果 (Create empty result)
         */
        public static ClassificationResult empty() {
            return ClassificationResult.builder()
                    .primaryRoles(Collections.emptyList())
                    .secondaryRoles(Collections.emptyList())
                    .confidence(0.0)
                    .build();
        }

        /**
         * 创建错误结果 (Create error result)
         */
        public static ClassificationResult error(String documentId, String errorMessage) {
            return ClassificationResult.builder()
                    .documentId(documentId)
                    .primaryRoles(Collections.emptyList())
                    .secondaryRoles(Collections.emptyList())
                    .hasError(true)
                    .errorMessage(errorMessage)
                    .build();
        }

        /**
         * 获取所有角色 (Get all roles)
         */
        public List<Role> getAllRoles() {
            List<Role> allRoles = new ArrayList<>();
            if (primaryRoles != null) {
                allRoles.addAll(primaryRoles);
            }
            if (secondaryRoles != null) {
                allRoles.addAll(secondaryRoles);
            }
            return allRoles;
        }
    }
}

