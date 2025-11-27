/**
 * 语言上下文管理 / Language Context Management
 * 负责多语言切换和翻译功能
 */

const LanguageContext = React.createContext();

/**
 * 语言提供者组件
 */
class LanguageProvider extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            language: localStorage.getItem('language') || 'zh'
        };
    }

    toggleLanguage = () => {
        const newLang = this.state.language === 'zh' ? 'en' : 'zh';
        this.setState({ language: newLang });
        localStorage.setItem('language', newLang);
        document.getElementById('html-root')?.setAttribute('lang', newLang === 'zh' ? 'zh-CN' : 'en');
    };

    t = (key, params) => {
        let text = window.translations?.[this.state.language]?.[key] || key;

        // 如果提供了参数，进行替换
        if (params && typeof params === 'object') {
            Object.keys(params).forEach(paramKey => {
                const regex = new RegExp(`\\{${paramKey}\\}`, 'g');
                text = text.replace(regex, params[paramKey]);
            });
        }

        return text;
    };

    render() {
        return React.createElement(
            LanguageContext.Provider,
            {
                value: {
                    language: this.state.language,
                    toggleLanguage: this.toggleLanguage,
                    t: this.t
                }
            },
            this.props.children
        );
    }
}

/**
 * 自定义 Hook 使用语言
 */
function useTranslation() {
    return React.useContext(LanguageContext);
}

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { LanguageContext, LanguageProvider, useTranslation };
} else {
    window.LanguageModule = { LanguageContext, LanguageProvider, useTranslation };
}

