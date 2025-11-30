package top.yumbo.ai.rag.i18n;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 application.yml 中的 log.locale 配置
 */
@ConfigurationProperties(prefix = "log")
public class LogLocaleProperties {
    /**
     * 可选值: auto | zh | en
     */
    private String locale = "auto";

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}

