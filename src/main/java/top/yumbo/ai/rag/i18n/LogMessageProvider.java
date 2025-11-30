package top.yumbo.ai.rag.i18n;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 提供后端日志国际化支持（静态工具类）
 *
 * 使用静态方法 `getMessage(key, args...)` 在任何场景下获取日志模板（支持非 Spring 启动）。
 */
@Slf4j
public final class LogMessageProvider {

    // 静态加载，便于非 Spring 场景下也能使用
    private static final ResourceBundle staticBundleZh;
    private static final ResourceBundle staticBundleEn;

    static {
        ResourceBundle bz = null;
        ResourceBundle be = null;
        try {
            bz = ResourceBundle.getBundle("messages", Locale.SIMPLIFIED_CHINESE);
        } catch (MissingResourceException e) {
            log.debug("static messages_zh.properties not found");
        }
        try {
            be = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        } catch (MissingResourceException e) {
            log.debug("static messages_en.properties not found");
        }
        staticBundleZh = bz;
        staticBundleEn = be;
    }

    private LogMessageProvider() {
        // utility
    }

    private static Locale determineStaticLocale() {
        // check system property first
        String cfg = System.getProperty("log.locale");
        if (cfg == null || cfg.isEmpty()) {
            cfg = System.getenv("LOG_LOCALE");
        }
        if (cfg != null) {
            if ("zh".equalsIgnoreCase(cfg)) {
                return Locale.SIMPLIFIED_CHINESE;
            } else if ("en".equalsIgnoreCase(cfg)) {
                return Locale.ENGLISH;
            }
        }
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null && "zh".equalsIgnoreCase(defaultLocale.getLanguage())) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }

    /**
     * 静态方法：在任何场景下直接调用以获取日志模板
     */
    public static String getMessage(String key, Object... args) {
        Locale locale = determineStaticLocale();
        String pattern = null;
        if (Locale.SIMPLIFIED_CHINESE.equals(locale) && staticBundleZh != null) {
            try {
                pattern = staticBundleZh.getString(key);
            } catch (MissingResourceException ignored) {
            }
        }
        if (pattern == null && staticBundleEn != null) {
            try {
                pattern = staticBundleEn.getString(key);
            } catch (MissingResourceException ignored) {
            }
        }
        if (pattern == null) {
            log.debug("Missing static log key {} in resources", key);
            pattern = "[" + key + "]";
        }
        return MessageFormat.format(pattern, args == null ? new Object[0] : args);
    }
}
