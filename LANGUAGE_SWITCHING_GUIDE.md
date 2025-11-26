# 🌐 中英文切换功能指南 / Language Switching Feature Guide

## 📋 功能概述 / Feature Overview

### 中文
已为知识库问答系统添加完整的中英文切换功能。用户可以通过页面右上角的语言切换按钮，在中文和英文界面之间无缝切换。语言选择会自动保存到浏览器本地存储，下次访问时会记住用户的选择。

### English
A complete Chinese/English language switching feature has been added to the Knowledge Base QA System. Users can seamlessly switch between Chinese and English interfaces using the language toggle button in the top right corner. Language preferences are automatically saved to browser local storage and remembered on subsequent visits.

---

## ✨ 实现的功能 / Implemented Features

### 1. 语言切换按钮 / Language Toggle Button
- **位置 / Position**: 页面右上角固定位置 / Fixed position in top right corner
- **样式 / Style**: 悬浮按钮，带有平滑过渡动画 / Floating button with smooth transition animation
- **交互 / Interaction**: 点击切换语言，显示目标语言名称 / Click to toggle, shows target language name

### 2. 全面的翻译覆盖 / Comprehensive Translation Coverage

#### 页面标题和导航 / Page Title and Navigation
- 应用标题和副标题 / App title and subtitle
- 状态显示（运行中/离线）/ Status display (Online/Offline)
- 四个主要标签页 / Four main tabs:
  - 💬 智能问答 / Q&A
  - 🔍 文档搜索 / Search
  - 📁 文档管理 / Documents
  - 📊 统计信息 / Statistics

#### 智能问答页面 / Q&A Tab
- 输入框提示文字 / Input placeholder
- 按钮文本（提问、思考中）/ Button text (Ask, Thinking)
- 加载状态提示 / Loading state messages
- 回答标题和参考来源 / Answer title and references
- 下载按钮文本 / Download button text
- 响应时间显示 / Response time display
- 空状态提示 / Empty state messages

#### 文档搜索页面 / Search Tab
- 搜索框提示文字 / Search placeholder
- 搜索按钮和状态 / Search button and states
- 结果计数显示 / Results count display
- 空状态提示 / Empty state messages

#### 文档管理页面 / Documents Tab
- 上传区域文本 / Upload area text
- 文档列表标题 / Document list title
- 操作按钮（刷新、删除、下载）/ Action buttons (Refresh, Delete, Download)
- 确认对话框 / Confirmation dialogs
- 成功/失败提示 / Success/error messages

#### 统计信息页面 / Statistics Tab
- 统计卡片标签 / Statistics card labels
- 索引操作按钮 / Index operation buttons
- 索引说明文本 / Index guide text
- 操作结果提示 / Operation result messages

### 3. 语言持久化 / Language Persistence
- 使用 `localStorage` 保存用户选择 / Uses localStorage to save user choice
- 页面刷新后保持选择 / Maintains choice after page refresh
- 更新 HTML lang 属性以支持辅助功能 / Updates HTML lang attribute for accessibility

---

## 🎨 技术实现 / Technical Implementation

### 架构设计 / Architecture Design

```javascript
// 1. 翻译字典 / Translation Dictionary
const translations = {
    zh: { /* 中文翻译 */ },
    en: { /* English translations */ }
};

// 2. 语言上下文 / Language Context
const LanguageContext = createContext();

// 3. 语言提供者 / Language Provider
function LanguageProvider({ children }) {
    const [language, setLanguage] = useState(() => {
        return localStorage.getItem('language') || 'zh';
    });
    
    const toggleLanguage = () => {
        const newLang = language === 'zh' ? 'en' : 'zh';
        setLanguage(newLang);
        localStorage.setItem('language', newLang);
        document.getElementById('html-root').setAttribute('lang', 
            newLang === 'zh' ? 'zh-CN' : 'en');
    };
    
    const t = (key) => translations[language][key] || key;
    
    return (
        <LanguageContext.Provider value={{ language, toggleLanguage, t }}>
            {children}
        </LanguageContext.Provider>
    );
}

// 4. 自定义 Hook / Custom Hook
function useTranslation() {
    return useContext(LanguageContext);
}
```

### 组件使用示例 / Component Usage Example

```javascript
function QATab() {
    const { t } = useTranslation();
    
    return (
        <div>
            <input placeholder={t('qaPlaceholder')} />
            <button>{t('qaButton')}</button>
        </div>
    );
}
```

---

## 🚀 使用方法 / How to Use

### 开发环境 / Development Environment

1. **启动应用 / Start Application**
   ```bash
   mvn spring-boot:run
   ```

2. **访问页面 / Access Page**
   ```
   http://localhost:8080
   ```

3. **切换语言 / Switch Language**
   - 点击右上角的语言切换按钮 / Click the language toggle button in top right
   - 默认为中文，点击切换到英文 / Default is Chinese, click to switch to English
   - 再次点击返回中文 / Click again to return to Chinese

### 生产环境 / Production Environment

1. **打包应用 / Build Application**
   ```bash
   mvn clean package -DskipTests
   ```

2. **运行 JAR / Run JAR**
   ```bash
   cd release
   start.bat
   ```

3. **访问并使用 / Access and Use**
   - 打开浏览器访问 `http://localhost:8080`
   - 使用语言切换功能

---

## 📝 翻译键值对照表 / Translation Key Reference

### 常用键 / Common Keys

| Key | 中文 (zh) | English (en) |
|-----|----------|--------------|
| `title` | 🤖 知识库问答系统 | 🤖 Knowledge Base QA System |
| `subtitle` | 基于 LocalFileRAG 的智能问答平台 | Intelligent Q&A Platform Based on LocalFileRAG |
| `statusOnline` | ✅ 运行中 | ✅ Online |
| `statusOffline` | ❌ 离线 | ❌ Offline |
| `qaButton` | 🤔 提问 | 🤔 Ask |
| `qaThinking` | 思考中... | Thinking... |
| `qaAnswer` | 💡 回答 | 💡 Answer |
| `searchButton` | 🔍 搜索 | 🔍 Search |
| `docsUploadButton` | 选择文件 | Select File |
| `statsRefresh` | 🔄 刷新统计 | 🔄 Refresh |

*完整列表请参考源代码中的 `translations` 对象*

---

## 🎯 扩展指南 / Extension Guide

### 添加新的翻译 / Adding New Translations

1. **在翻译字典中添加键值对 / Add key-value pairs to translation dictionary**
   ```javascript
   const translations = {
       zh: {
           newKey: '新的中文文本',
           // ...
       },
       en: {
           newKey: 'New English Text',
           // ...
       }
   };
   ```

2. **在组件中使用 / Use in components**
   ```javascript
   const { t } = useTranslation();
   return <div>{t('newKey')}</div>;
   ```

### 添加新语言 / Adding New Languages

1. **扩展翻译字典 / Extend translation dictionary**
   ```javascript
   const translations = {
       zh: { /* ... */ },
       en: { /* ... */ },
       ja: { /* 日本語 */ },  // 新增日语 / Add Japanese
       // ...
   };
   ```

2. **更新语言切换逻辑 / Update toggle logic**
   ```javascript
   const toggleLanguage = () => {
       const langs = ['zh', 'en', 'ja'];
       const currentIndex = langs.indexOf(language);
       const nextIndex = (currentIndex + 1) % langs.length;
       const newLang = langs[nextIndex];
       // ...
   };
   ```

---

## 🔧 故障排除 / Troubleshooting

### 语言不切换 / Language Not Switching

**问题 / Problem**: 点击按钮后界面没有变化
**解决方案 / Solution**:
1. 检查浏览器控制台是否有错误 / Check browser console for errors
2. 清除浏览器缓存和 localStorage / Clear browser cache and localStorage
3. 确保 React 正确加载 / Ensure React is properly loaded

### 部分文本未翻译 / Partial Text Not Translated

**问题 / Problem**: 某些文本仍然显示为中文/英文
**解决方案 / Solution**:
1. 检查该文本是否在翻译字典中 / Check if the text is in translation dictionary
2. 确认组件正确使用了 `t()` 函数 / Verify component correctly uses `t()` function
3. 检查键名是否正确 / Check if key name is correct

### localStorage 不工作 / localStorage Not Working

**问题 / Problem**: 刷新页面后语言选择丢失
**解决方案 / Solution**:
1. 检查浏览器是否启用了 localStorage / Check if localStorage is enabled
2. 确认不是在隐私/无痕模式下浏览 / Confirm not browsing in private/incognito mode
3. 检查浏览器安全设置 / Check browser security settings

---

## 📊 测试清单 / Testing Checklist

- [x] 语言切换按钮显示正常 / Language toggle button displays correctly
- [x] 点击按钮可以切换语言 / Clicking button switches language
- [x] 所有页面标签都已翻译 / All page tabs are translated
- [x] 输入框和按钮文本已翻译 / Input fields and buttons are translated
- [x] 加载和错误提示已翻译 / Loading and error messages are translated
- [x] 空状态提示已翻译 / Empty state messages are translated
- [x] 语言选择持久化保存 / Language choice persists
- [x] 刷新页面后保持选择 / Choice maintained after page refresh
- [x] 不同浏览器表现一致 / Consistent behavior across browsers

---

## 🎉 完成状态 / Completion Status

✅ **已完成 / Completed**:
- 完整的中英文翻译字典 / Complete Chinese/English translation dictionary
- 语言切换功能 / Language switching functionality
- 语言持久化 / Language persistence
- 所有主要组件的翻译 / Translation of all major components
- 响应式语言切换按钮 / Responsive language toggle button

📋 **可选增强 / Optional Enhancements**:
- 添加更多语言支持（日语、韩语等）/ Add more language support (Japanese, Korean, etc.)
- 根据浏览器语言自动选择 / Auto-detect browser language
- 语言切换动画效果 / Animation effects for language switching
- 右键菜单语言选择 / Context menu for language selection

---

## 📞 支持 / Support

如有问题或建议，请提交 Issue 或 Pull Request。
For issues or suggestions, please submit an Issue or Pull Request.

---

**版本 / Version**: 1.0  
**更新日期 / Last Updated**: 2025-11-26  
**作者 / Author**: AI Reviewer Team

