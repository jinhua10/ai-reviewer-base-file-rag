package top.yumbo.ai.rag.ai.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务数据收集器 (Business Data Collector)
 *
 * 功能 (Features):
 * 1. 用户授权的数据收集 (User-authorized data collection)
 * 2. 隐私保护 (Privacy protection)
 * 3. 自动去敏 (Auto desensitization)
 * 4. 业务场景识别 (Business scenario identification)
 *
 * 核心价值 (Core Value):
 * - PPT 生成 → 提取业务术语和概念
 * - 报告生成 → 识别业务逻辑和流程
 * - 用户修改 → 黄金标注数据
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class BusinessDataCollector {

    /**
     * 收集的数据 (Collected data)
     */
    private final Map<String, CollectedData> dataStore = new ConcurrentHashMap<>();

    /**
     * 用户授权记录 (User consent records)
     */
    private final Map<String, UserConsent> consentRecords = new ConcurrentHashMap<>();

    /**
     * 敏感词列表 (Sensitive words list)
     */
    private final Set<String> sensitiveWords = new HashSet<>(Arrays.asList(
        "密码", "password", "secret", "token", "key",
        "身份证", "手机号", "银行卡", "邮箱", "email"
    ));

    // ========== 初始化 (Initialization) ==========

    public BusinessDataCollector() {
        log.info(I18N.get("data.collector.initialized"));
    }

    // ========== 用户授权 (User Consent) ==========

    /**
     * 请求用户授权 (Request user consent)
     *
     * @param userId 用户ID (User ID)
     * @param dataType 数据类型 (Data type)
     * @return 是否授权 (Consent granted)
     */
    public boolean requestConsent(String userId, DataType dataType) {
        try {
            // 检查是否已授权 (Check if already consented)
            UserConsent consent = consentRecords.get(userId);
            if (consent != null && consent.getConsentedTypes().contains(dataType)) {
                return true;
            }

            // TODO: 实际应显示授权对话框
            // 这里简化实现，自动授权
            if (consent == null) {
                consent = new UserConsent();
                consent.setUserId(userId);
                consent.setConsentTime(LocalDateTime.now());
                consent.setConsentedTypes(new HashSet<>());
                consentRecords.put(userId, consent);
            }

            consent.getConsentedTypes().add(dataType);
            log.info(I18N.get("data.collector.consent_granted"), userId, dataType);

            return true;

        } catch (Exception e) {
            log.error(I18N.get("data.collector.consent_failed"), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 撤销授权 (Revoke consent)
     */
    public void revokeConsent(String userId, DataType dataType) {
        UserConsent consent = consentRecords.get(userId);
        if (consent != null) {
            consent.getConsentedTypes().remove(dataType);
            log.info(I18N.get("data.collector.consent_revoked"), userId, dataType);
        }
    }

    // ========== 数据收集 (Data Collection) ==========

    /**
     * 收集数据 (Collect data)
     *
     * @param userId 用户ID (User ID)
     * @param dataType 数据类型 (Data type)
     * @param content 内容 (Content)
     * @param context 上下文 (Context)
     * @return 数据ID (Data ID)
     */
    public String collectData(String userId, DataType dataType, String content, Map<String, Object> context) {
        try {
            // 1. 检查授权 (Check consent)
            if (!hasConsent(userId, dataType)) {
                log.warn(I18N.get("data.collector.no_consent"), userId, dataType);
                return null;
            }

            // 2. 数据去敏 (Desensitize data)
            String desensitized = desensitizeContent(content);

            // 3. 创建数据记录 (Create data record)
            String dataId = UUID.randomUUID().toString();
            CollectedData data = new CollectedData();
            data.setDataId(dataId);
            data.setUserId(userId);
            data.setDataType(dataType);
            data.setOriginalContent(content);
            data.setDesensitizedContent(desensitized);
            data.setContext(context);
            data.setCollectTime(LocalDateTime.now());
            data.setScenario(detectScenario(content, context));

            // 4. 存储 (Store)
            dataStore.put(dataId, data);

            log.info(I18N.get("data.collector.collected"), dataType, dataId);
            return dataId;

        } catch (Exception e) {
            log.error(I18N.get("data.collector.collect_failed"), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查是否有授权 (Check if has consent)
     */
    private boolean hasConsent(String userId, DataType dataType) {
        UserConsent consent = consentRecords.get(userId);
        return consent != null && consent.getConsentedTypes().contains(dataType);
    }

    /**
     * 数据去敏 (Desensitize content)
     */
    private String desensitizeContent(String content) {
        String result = content;

        // 去除敏感词 (Remove sensitive words)
        for (String word : sensitiveWords) {
            result = result.replaceAll(word, "***");
        }

        // 去除邮箱 (Remove emails)
        result = result.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***@***.com");

        // 去除手机号 (Remove phone numbers)
        result = result.replaceAll("1[3-9]\\d{9}", "***********");

        // 去除身份证号 (Remove ID numbers)
        result = result.replaceAll("\\d{17}[0-9Xx]", "******************");

        return result;
    }

    /**
     * 检测业务场景 (Detect business scenario)
     */
    private BusinessScenario detectScenario(String content, Map<String, Object> context) {
        String lowerContent = content.toLowerCase();

        // PPT 生成场景 (PPT generation scenario)
        if (lowerContent.contains("ppt") || lowerContent.contains("presentation")
            || lowerContent.contains("幻灯片")) {
            return BusinessScenario.PPT_GENERATION;
        }

        // 报告生成场景 (Report generation scenario)
        if (lowerContent.contains("报告") || lowerContent.contains("report")
            || lowerContent.contains("总结")) {
            return BusinessScenario.REPORT_GENERATION;
        }

        // 代码生成场景 (Code generation scenario)
        if (lowerContent.contains("代码") || lowerContent.contains("code")
            || lowerContent.contains("function")) {
            return BusinessScenario.CODE_GENERATION;
        }

        // 文档处理场景 (Document processing scenario)
        if (lowerContent.contains("文档") || lowerContent.contains("document")) {
            return BusinessScenario.DOCUMENT_PROCESSING;
        }

        return BusinessScenario.GENERAL;
    }

    // ========== 数据查询 (Data Query) ==========

    /**
     * 按场景获取数据 (Get data by scenario)
     */
    public List<CollectedData> getDataByScenario(BusinessScenario scenario) {
        return dataStore.values().stream()
            .filter(d -> d.getScenario() == scenario)
            .toList();
    }

    /**
     * 按数据类型获取数据 (Get data by type)
     */
    public List<CollectedData> getDataByType(DataType dataType) {
        return dataStore.values().stream()
            .filter(d -> d.getDataType() == dataType)
            .toList();
    }

    // ========== 统计信息 (Statistics) ==========

    /**
     * 获取收集统计 (Get collection statistics)
     */
    public CollectionStats getStats() {
        CollectionStats stats = new CollectionStats();
        stats.setTotalCollected(dataStore.size());
        stats.setTotalUsers(consentRecords.size());

        // 按场景统计 (Count by scenario)
        Map<BusinessScenario, Long> scenarioCounts = new HashMap<>();
        for (CollectedData data : dataStore.values()) {
            scenarioCounts.merge(data.getScenario(), 1L, Long::sum);
        }
        stats.setScenarioCounts(scenarioCounts);

        return stats;
    }

    // ========== 内部类 (Inner Classes) ==========

    /**
     * 用户授权 (User Consent)
     */
    @Data
    public static class UserConsent {
        private String userId;
        private Set<DataType> consentedTypes;
        private LocalDateTime consentTime;
    }

    /**
     * 收集的数据 (Collected Data)
     */
    @Data
    public static class CollectedData {
        private String dataId;
        private String userId;
        private DataType dataType;
        private String originalContent;
        private String desensitizedContent;
        private Map<String, Object> context;
        private BusinessScenario scenario;
        private LocalDateTime collectTime;
    }

    /**
     * 数据类型 (Data Type)
     */
    public enum DataType {
        USER_INPUT("user_input", "用户输入", "User Input"),
        AI_OUTPUT("ai_output", "AI输出", "AI Output"),
        USER_EDIT("user_edit", "用户修改", "User Edit"),
        INTERACTION("interaction", "交互数据", "Interaction");

        private final String code;
        private final String nameCn;
        private final String nameEn;

        DataType(String code, String nameCn, String nameEn) {
            this.code = code;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

        public String getCode() { return code; }
        public String getNameCn() { return nameCn; }
        public String getNameEn() { return nameEn; }
    }

    /**
     * 业务场景 (Business Scenario)
     */
    public enum BusinessScenario {
        PPT_GENERATION,      // PPT生成
        REPORT_GENERATION,   // 报告生成
        CODE_GENERATION,     // 代码生成
        DOCUMENT_PROCESSING, // 文档处理
        GENERAL              // 通用
    }

    /**
     * 收集统计 (Collection Statistics)
     */
    @Data
    public static class CollectionStats {
        private int totalCollected;
        private int totalUsers;
        private Map<BusinessScenario, Long> scenarioCounts;
    }
}

