package top.yumbo.ai.rag.i18n;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 提供后端日志国际化支持（静态工具类）
 * (Provides backend log internationalization support - static utility class)
 *
 * 使用静态方法 `getMessage(key, args...)` 在任何场景下获取日志模板（支持非 Spring 启动）。
 * (Use static method `getMessage(key, args...)` to get log templates in any scenario)
 *
 * <p>编码说明 (Encoding Notes):
 * <ul>
 *   <li>所有 messages*.yml 文件统一使用 UTF-8 编码保存 (All files saved in UTF-8)</li>
 *   <li>使用 SnakeYAML 库加载 YAML 格式的国际化文件 (Use SnakeYAML to load YAML i18n files)</li>
 *   <li>支持嵌套的 YAML 结构，自动展平为点号分隔的 key (Support nested YAML structure)</li>
 * </ul>
 * </p>
 */
@Slf4j
public final class I18N {

    // 静态加载的消息映射表 (Statically loaded message maps)
    private static final Map<String, String> messagesZh = new HashMap<>();
    private static final Map<String, String> messagesEn = new HashMap<>();

    static {
        // 加载中文消息 (Load Chinese messages)
        try (InputStream is = I18N.class.getClassLoader()
                .getResourceAsStream("messages_zh.yml")) {
            if (is != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(is);
                flattenYaml("", data, messagesZh);
                log.debug("Loaded {} Chinese message keys from messages_zh.yml", messagesZh.size());
            } else {
                log.warn("messages_zh.yml not found in classpath");
            }
        } catch (Exception e) {
            log.error("Failed to load messages_zh.yml", e);
        }

        // 加载英文消息 (Load English messages)
        try (InputStream is = I18N.class.getClassLoader()
                .getResourceAsStream("messages_en.yml")) {
            if (is != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(is);
                flattenYaml("", data, messagesEn);
                log.debug("Loaded {} English message keys from messages_en.yml", messagesEn.size());
            } else {
                log.warn("messages_en.yml not found in classpath");
            }
        } catch (Exception e) {
            log.error("Failed to load messages_en.yml", e);
        }
    }

    /**
     * 将嵌套的 YAML 结构展平为点号分隔的 key
     * (Flatten nested YAML structure to dot-separated keys)
     */
    @SuppressWarnings("unchecked")
    private static void flattenYaml(String prefix, Map<String, Object> map, Map<String, String> result) {
        // 保护性校验：空 map 直接返回
        if (map == null || map.isEmpty()) {
            return;
        }

        // 规范化 prefix，避免后续调用 NPE
        String safePrefix = prefix == null ? "" : prefix;

        // 如果顶层是单个 'lang' 节点（来自非标准挂载），解包它以去掉 'lang.' 前缀
        // If the top-level map contains only a single 'lang' node, unwrap it so keys are not prefixed with 'lang.'
        if (safePrefix.isEmpty() && map.size() == 1 && map.containsKey("lang") && map.get("lang") instanceof Map) {
            flattenYaml("", (Map<String, Object>) map.get("lang"), result);
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = safePrefix.isEmpty() ? entry.getKey() : safePrefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // 安全处理嵌套的 Map，可能包含非字符串键 (Safely handle nested maps that may contain non-string keys)
                flattenYamlSafe(key, (Map<?, ?>) value, result);
            } else if (value != null) {
                // 叶子节点，存储值 (Leaf node, store value)
                result.put(key, value.toString());
            }
        }
    }

    /**
     * 安全地处理嵌套的 YAML Map，支持非字符串键
     * (Safely handle nested YAML maps with non-string keys)
     */
    @SuppressWarnings("unchecked")
    private static void flattenYamlSafe(String prefix, Map<?, ?> map, Map<String, String> result) {
        if (map == null || map.isEmpty()) {
            return;
        }
        String safePrefix = prefix == null ? "" : prefix;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = safePrefix.isEmpty() ? entry.getKey().toString() : safePrefix + "." + entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // 递归处理嵌套的 Map (Recursively process nested maps)
                flattenYamlSafe(key, (Map<?, ?>) value, result);
            } else if (value != null) {
                // 叶子节点，存储值 (Leaf node, store value)
                result.put(key, value.toString());
            }
        }
    }

    private I18N() {
        // utility class
    }

    /**
     * 确定当前语言环境 (Determine current locale)
     */
    private static Locale determineStaticLocale() {
        // 首先检查系统属性 (Check system property first)
        String cfg = System.getProperty("log.locale");
        if (cfg == null || cfg.isEmpty()) {
            cfg = System.getenv("LOG_LOCALE");
        }
        if (cfg != null) {
            if ("zh".equalsIgnoreCase(cfg) || "zh-CN".equalsIgnoreCase(cfg)) {
                return Locale.SIMPLIFIED_CHINESE;
            } else if ("en".equalsIgnoreCase(cfg) || "en-US".equalsIgnoreCase(cfg)) {
                return Locale.ENGLISH;
            }
        }

        // 使用系统默认语言 (Use system default locale)
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null && "zh".equalsIgnoreCase(defaultLocale.getLanguage())) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }

    /**
     * 静态方法：在任何场景下直接调用以获取日志模板
     * (Static method: get log message template in any scenario)
     */
    public static String get(String key, Object... args) {
        Locale locale = determineStaticLocale();
        return getMessageWithLocale(key, locale, args);
    }

    /**
     * 根据指定语言获取消息（用于 API 响应）
     * (Get message with specified language for API responses)
     *
     * @param key 消息键 / Message key
     * @param lang 语言参数 (zh/en/null) / Language parameter (zh/en/null)
     * @param args 格式化参数 / Format arguments
     * @return 国际化消息 / Internationalized message
     */
    public static String getLang(String key, String lang, Object... args) {
        Locale locale;
        if ("en".equalsIgnoreCase(lang)) {
            locale = Locale.ENGLISH;
        } else if ("zh".equalsIgnoreCase(lang)) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            // 如果lang为null或其他值，使用默认行为（自动检测）
            locale = determineStaticLocale();
        }
        return getMessageWithLocale(key, locale, args);
    }

    /**
     * 内部方法：根据 Locale 获取消息
     * (Internal method: get message with Locale)
     */
    private static String getMessageWithLocale(String key, Locale locale, Object... args) {
        String pattern = null;

        // 根据语言环境选择消息源 (Select message source based on locale)
        if (Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            pattern = messagesZh.get(key);
        }

        // 如果中文未找到，尝试英文 (Fallback to English if Chinese not found)
        if (pattern == null) {
            pattern = messagesEn.get(key);
        }

        // 如果都未找到，返回 key 本身 (If not found, return key itself)
        if (pattern == null) {
            log.debug("Missing static log key {} in resources", key);
            pattern = "[" + key + "]";
        }

        // 格式化消息 (Format message)
        try {
            return MessageFormat.format(pattern, args == null ? new Object[0] : args);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to format message for key: {} with pattern: {}", key, pattern, e);
            return pattern;
        }
    }
}
