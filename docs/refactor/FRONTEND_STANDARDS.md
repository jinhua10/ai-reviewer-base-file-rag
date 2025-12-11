# RAG 2.0 前端技术栈与规范
# RAG 2.0 Frontend Tech Stack and Standards

> **文档编号**: 20251212-FRONTEND-STANDARDS  
> **创建日期**: 2025-12-12  
> **状态**: ✅ 生效中  
> **优先级**: 🔥 必须遵守

---

## ⚠️ 重要提醒

**前端使用 React + JSX，不是 Vue！**

所有前端代码必须严格遵守 `20251209-23-00-00-CODE_STANDARDS.md` 中的规则 6-7。

---

## 🎯 技术栈

### 核心框架
```yaml
框架: React 18
语法: JSX（不是 Vue Template！）
状态管理: React Context API + Hooks
UI 库: Ant Design 或 Material-UI
HTTP: Axios
```

### 构建工具
```yaml
开发环境: Babel Standalone (支持 JSX 即时编译)
生产环境: Webpack + Babel
代码检查: ESLint (React 规则)
```

### 样式方案
```yaml
方案: 独立 CSS 文件
命名: BEM 命名法 (.block__element--modifier)
变量: CSS Variables (:root)
禁止: 内联样式（除动态值外）
```

---

## 📝 核心规范（必须遵守）

### 规则 1: JSX 优先 ⭐

**所有新组件必须使用 JSX 格式实现**

#### ❌ 错误做法 - 使用纯 JavaScript
```javascript
// 禁止：使用 document.createElement
function createButton() {
    const button = document.createElement('button');
    button.className = 'btn btn-primary';
    button.textContent = 'Click Me';
    return button;
}

// 禁止：使用字符串拼接 HTML
function createCard() {
    return `<div class="card"><h3>${title}</h3></div>`;
}
```

#### ✅ 正确做法 - 使用 JSX
```jsx
// 推荐：使用 JSX 创建组件
function Button({ onClick, children }) {
    return (
        <button className="btn btn-primary" onClick={onClick}>
            {children}
        </button>
    );
}

function Card({ title, content }) {
    return (
        <div className="card">
            <h3>{title}</h3>
            <p>{content}</p>
        </div>
    );
}
```

---

### 规则 2: 样式分离 ⭐

**所有样式必须提取到独立 CSS 文件，禁止内联样式**

#### ❌ 错误做法 - 内联样式
```jsx
// 禁止：内联样式难以维护
<div style={{
    padding: '10px 15px',
    margin: '10px 0',
    background: 'linear-gradient(135deg, #667eea15 0%, #764ba215 100%)',
    border: '1px solid #667eea40',
    borderRadius: '8px'
}}>
    提示信息
</div>
```

#### ✅ 正确做法 - CSS 类
```jsx
// 推荐：使用 CSS 类
<div className="filter-hint">
    提示信息
</div>
```

**对应 CSS 文件** (`filter-hint.css`):
```css
.filter-hint {
    padding: 10px 15px;
    margin: 10px 0;
    background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
    border: 1px solid #667eea40;
    border-radius: 8px;
}
```

#### ⚠️ 允许内联样式的特殊情况

仅在以下情况允许：

1. **动态计算的值**
   ```jsx
   <div style={{ width: `${progress}%` }}>进度条</div>
   <div style={{ left: `${position}px` }}>拖拽元素</div>
   ```

2. **第三方库要求**
   ```jsx
   <ThirdPartyComponent style={requiredStyles} />
   ```

---

### 规则 3: 文件命名与组织

#### 文件命名
```yaml
组件文件: PascalCase.jsx
  ✅ MyComponent.jsx
  ✅ DocumentUpload.jsx
  ✅ UserProfile.jsx
  ❌ myComponent.jsx
  ❌ my-component.jsx
  ❌ MyComponent.js (应该用 .jsx)

CSS 文件: kebab-case.css
  ✅ my-component.css
  ✅ document-upload.css
  ✅ user-profile.css
  ❌ MyComponent.css
  ❌ my_component.css
```

#### 目录结构
```
src/main/resources/static/
├── js/
│   ├── components/              (React JSX 组件)
│   │   ├── common/              (通用组件)
│   │   │   ├── Button.jsx
│   │   │   ├── Modal.jsx
│   │   │   └── Loading.jsx
│   │   ├── document/            (文档管理)
│   │   │   ├── DocumentList.jsx
│   │   │   └── DocumentUpload.jsx
│   │   ├── qa/                  (问答)
│   │   │   ├── QAPanel.jsx
│   │   │   └── ChatBox.jsx
│   │   └── App.jsx              (主应用)
│   │
│   ├── contexts/                (React Context)
│   │   ├── UserContext.js
│   │   ├── LanguageContext.js
│   │   └── AppContext.js
│   │
│   ├── hooks/                   (自定义 Hooks)
│   │   ├── useApi.js
│   │   ├── useFetch.js
│   │   └── useAuth.js
│   │
│   └── api/                     (API 封装)
│       ├── index.js
│       └── modules/
│           ├── document.js
│           └── qa.js
│
└── assets/
    └── css/                     (CSS 样式文件)
        ├── reset.css            (重置样式)
        ├── main.css             (全局样式 + CSS 变量)
        ├── common/              (通用组件样式)
        │   ├── button.css
        │   └── modal.css
        ├── document/            (文档管理样式)
        │   ├── document-list.css
        │   └── document-upload.css
        └── qa/                  (问答样式)
            ├── qa-panel.css
            └── chat-box.css
```

---

### 规则 4: JSX 组件结构

**标准 JSX 组件结构**:

```jsx
/**
 * 组件名称 (Component Name)
 * 功能描述 (Function description)
 * 
 * @param {Object} props - 组件属性
 * @param {string} props.title - 标题
 * @param {Function} props.onSave - 保存回调
 * 
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

// 1. 导入 React 和 Hooks (Import React and Hooks)
const { useState, useEffect, useCallback } = React;

// 2. 定义组件 (Define component)
function MyComponent({ title, content, onSave }) {
    // 3. 状态定义 (State definition)
    const [isEditing, setIsEditing] = useState(false);
    const [data, setData] = useState(null);
    
    // 4. 国际化 (Internationalization)
    const { t } = window.LanguageModule.useTranslation();
    
    // 5. 副作用 (Side effects)
    useEffect(() => {
        // 初始化逻辑 (Initialization logic)
        loadData();
    }, []);
    
    // 6. 事件处理函数 (Event handlers)
    const handleSave = useCallback(() => {
        // 处理保存 (Handle save)
        onSave?.(data);
    }, [data, onSave]);
    
    const handleEdit = useCallback(() => {
        setIsEditing(true);
    }, []);
    
    // 7. 辅助函数 (Helper functions)
    const loadData = async () => {
        // 加载数据 (Load data)
        const result = await fetchData();
        setData(result);
    };
    
    // 8. 条件渲染辅助函数 (Conditional render helper)
    const renderContent = () => {
        if (!data) return <Loading />;
        if (isEditing) return <Editor data={data} />;
        return <Display data={data} />;
    };
    
    // 9. 渲染 (Render)
    return (
        <div className="my-component">
            <header className="my-component__header">
                <h2 className="my-component__title">{title}</h2>
                <button 
                    className="my-component__edit-btn"
                    onClick={handleEdit}
                >
                    {t('edit')}
                </button>
            </header>
            
            <main className="my-component__content">
                {renderContent()}
            </main>
            
            <footer className="my-component__footer">
                <button 
                    className="my-component__save-btn"
                    onClick={handleSave}
                >
                    {t('save')}
                </button>
            </footer>
        </div>
    );
}

// 10. 默认 Props (Default props)
MyComponent.defaultProps = {
    title: 'Default Title',
    content: '',
};

// 11. 导出到全局 (Export to global)
window.MyComponent = MyComponent;
```

---

### 规则 5: CSS 类命名（BEM）

**使用 BEM 命名法**:

```css
/* Block (块) */
.my-component { }

/* Element (元素) */
.my-component__header { }
.my-component__title { }
.my-component__content { }
.my-component__footer { }
.my-component__save-btn { }

/* Modifier (修饰符) */
.my-component--large { }
.my-component--disabled { }
.my-component__save-btn--primary { }
.my-component__save-btn--loading { }
```

**命名规则**:
- 使用小写字母和连字符（kebab-case）
- 类名应具有语义化，描述用途而非样式
- 避免使用缩写（除非是通用缩写如 btn、nav）
- 模块前缀 + 功能描述

**示例**:
```css
/* ✅ 好的命名 */
.document-upload-area { }
.search-filter-container { }
.ai-analysis-panel { }
.qa-chat-box__message--sent { }

/* ❌ 不好的命名 */
.blue-box { }          /* 描述样式而非用途 */
.div1 { }              /* 无意义 */
.temp { }              /* 过于笼统 */
```

---

### 规则 6: CSS 变量管理

**在 main.css 中定义全局 CSS 变量**:

```css
:root {
    /* 主题色 (Theme Colors) */
    --primary-color: #667eea;
    --secondary-color: #764ba2;
    --primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    
    /* 文字颜色 (Text Colors) */
    --text-primary: #333;
    --text-secondary: #666;
    --text-disabled: #999;
    
    /* 背景色 (Background Colors) */
    --bg-primary: #ffffff;
    --bg-secondary: #f5f5f5;
    --bg-hover: #f0f0f0;
    
    /* 间距 (Spacing) */
    --spacing-xs: 5px;
    --spacing-sm: 10px;
    --spacing-md: 15px;
    --spacing-lg: 20px;
    --spacing-xl: 30px;
    
    /* 圆角 (Border Radius) */
    --radius-sm: 4px;
    --radius-md: 8px;
    --radius-lg: 12px;
    
    /* 阴影 (Shadow) */
    --shadow-sm: 0 2px 4px rgba(0,0,0,0.1);
    --shadow-md: 0 4px 8px rgba(0,0,0,0.15);
    --shadow-lg: 0 8px 16px rgba(0,0,0,0.2);
}

/* 暗色模式 (Dark Mode) */
[data-theme="dark"] {
    --text-primary: #f0f0f0;
    --text-secondary: #ccc;
    --bg-primary: #1a1a1a;
    --bg-secondary: #2a2a2a;
}
```

**在组件样式中使用变量**:

```css
.my-component {
    padding: var(--spacing-md);
    margin: var(--spacing-sm) 0;
    background: var(--primary-gradient);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-md);
    color: var(--text-primary);
}
```

---

### 规则 7: Props 和事件处理

#### Props 解构
```jsx
// ✅ 推荐：在参数中解构 props
function MyComponent({ title, content, onSave, isEditing = false }) {
    return (
        <div>
            <h2>{title}</h2>
            <p>{content}</p>
            {isEditing && <button onClick={onSave}>Save</button>}
        </div>
    );
}

// ❌ 不推荐：在组件内部解构
function MyComponent(props) {
    const { title, content, onSave, isEditing } = props;
    // ...
}
```

#### 事件处理命名
```jsx
// ✅ 推荐：使用 handle 前缀
const handleClick = () => { /* ... */ };
const handleSubmit = () => { /* ... */ };
const handleChange = (e) => { /* ... */ };
const handleDelete = (id) => { /* ... */ };

// ✅ 推荐：传递给子组件用 on 前缀
<ChildComponent
    onClick={handleClick}
    onSubmit={handleSubmit}
    onDelete={handleDelete}
/>
```

#### 避免内联函数
```jsx
// ❌ 不推荐：内联函数（每次渲染都创建新函数）
<button onClick={() => handleDelete(item.id)}>Delete</button>

// ✅ 推荐：使用 useCallback
const handleDeleteClick = useCallback(() => {
    handleDelete(item.id);
}, [item.id]);

<button onClick={handleDeleteClick}>Delete</button>

// ✅ 简单场景可以接受（如切换布尔值）
<button onClick={() => setShowModal(true)}>Open</button>
```

---

### 规则 8: 条件渲染

```jsx
// ✅ 推荐：使用 && 操作符
{isLoading && <LoadingSpinner />}
{error && <ErrorMessage message={error} />}

// ✅ 推荐：使用三元运算符
{isLoggedIn ? <Dashboard /> : <LoginForm />}

// ✅ 推荐：复杂条件提取为函数
const renderContent = () => {
    if (isLoading) return <LoadingSpinner />;
    if (error) return <ErrorMessage message={error} />;
    if (data) return <DataView data={data} />;
    return <EmptyState />;
};

return (
    <div className="container">
        {renderContent()}
    </div>
);
```

---

### 规则 9: 列表渲染

```jsx
// ✅ 正确：使用 map 和 key
<ul>
    {items.map(item => (
        <li key={item.id}>
            {item.name}
        </li>
    ))}
</ul>

// ❌ 错误：没有 key 属性
<ul>
    {items.map(item => (
        <li>{item.name}</li>
    ))}
</ul>

// ❌ 错误：使用 index 作为 key（如果列表会变化）
<ul>
    {items.map((item, index) => (
        <li key={index}>{item.name}</li>
    ))}
</ul>
```

---

### 规则 10: 动画定义

**动画必须在 CSS 中定义，使用 @keyframes**:

```css
/* ✅ 正确：在 CSS 中定义动画 */
.fade-in {
    animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.slide-left {
    animation: slideLeft 0.5s ease-out;
}

@keyframes slideLeft {
    from {
        transform: translateX(100%);
    }
    to {
        transform: translateX(0);
    }
}
```

**在 JSX 中应用动画**:

```jsx
// ✅ 正确：使用 CSS 类切换动画
function MyComponent({ isVisible }) {
    return (
        <div className={`my-component ${isVisible ? 'fade-in' : ''}`}>
            Content
        </div>
    );
}
```

**避免在 JS 中操作样式**:
```javascript
// ❌ 错误：在 JS 中直接操作样式
element.style.opacity = '0';
element.style.transform = 'translateY(-5px)';

// ✅ 正确：使用 CSS 类切换
element.classList.add('fade-in');
```

---

## 📋 代码质量检查清单

### 前端代码提交前必须检查

- [ ] **⭐ JSX 格式**: 所有新组件使用 JSX 格式实现
- [ ] **⭐ 样式分离**: 所有样式提取到 CSS 文件，无内联样式
- [ ] **文件命名**: 组件文件 PascalCase.jsx，CSS 文件 kebab-case.css
- [ ] **组件结构**: 遵循标准结构（imports → state → effects → handlers → render）
- [ ] **Props 解构**: 在函数参数中解构
- [ ] **事件命名**: handle 前缀（内部）/ on 前缀（传递给子组件）
- [ ] **条件渲染**: 使用 &&、三元运算符或函数
- [ ] **列表 key**: 使用唯一 ID，不用 index
- [ ] **CSS 类名**: 使用 BEM 命名法
- [ ] **CSS 变量**: 使用 CSS 变量管理主题色
- [ ] **动画定义**: 在 CSS 中使用 @keyframes
- [ ] **注释完整**: 文件头注释 + 关键逻辑注释
- [ ] **国际化**: 使用 t() 函数，不硬编码文本

---

## 🚀 快速开始

### 1. 创建新组件

```bash
# 1. 创建 JSX 文件
touch src/main/resources/static/js/components/MyComponent.jsx

# 2. 创建 CSS 文件
touch src/main/resources/static/assets/css/my-component.css
```

### 2. 组件模板

**MyComponent.jsx**:
```jsx
/**
 * My Component (我的组件)
 */
const { useState } = React;

function MyComponent({ title }) {
    const [count, setCount] = useState(0);
    
    const handleClick = () => {
        setCount(count + 1);
    };
    
    return (
        <div className="my-component">
            <h2 className="my-component__title">{title}</h2>
            <button 
                className="my-component__button" 
                onClick={handleClick}
            >
                Count: {count}
            </button>
        </div>
    );
}

window.MyComponent = MyComponent;
```

**my-component.css**:
```css
.my-component {
    padding: var(--spacing-md);
    background: var(--bg-secondary);
    border-radius: var(--radius-md);
}

.my-component__title {
    color: var(--text-primary);
    margin-bottom: var(--spacing-sm);
}

.my-component__button {
    padding: var(--spacing-sm) var(--spacing-md);
    background: var(--primary-gradient);
    color: white;
    border: none;
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.my-component__button:hover {
    opacity: 0.9;
}
```

### 3. 在 HTML 中引入

```html
<!-- index.html -->
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="assets/css/reset.css">
    <link rel="stylesheet" href="assets/css/main.css">
    <link rel="stylesheet" href="assets/css/my-component.css">
</head>
<body>
    <div id="root"></div>
    
    <!-- React -->
    <script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
    <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
    
    <!-- Babel Standalone (支持 JSX) -->
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
    
    <!-- 组件 -->
    <script type="text/babel" src="js/components/MyComponent.jsx"></script>
    
    <!-- 启动应用 -->
    <script type="text/babel">
        ReactDOM.render(
            <MyComponent title="Hello React!" />,
            document.getElementById('root')
        );
    </script>
</body>
</html>
```

---

## 📚 相关文档

- **完整代码规范**: `20251209-23-00-00-CODE_STANDARDS.md` 规则 6-7
- **实施计划**: `20251212-POLISH_AND_FRONTEND_PLAN.md`
- **快速查看**: `QUICK_VIEW.md`

---

## ✅ 总结

### 必须记住的核心要点

1. **JSX 优先** - 所有新组件用 JSX
2. **样式分离** - 禁止内联样式
3. **BEM 命名** - CSS 类名语义化
4. **文件命名** - 组件 PascalCase.jsx，CSS kebab-case.css
5. **动画 CSS** - 使用 @keyframes

**遵守这些规范，代码质量有保障！** 🎯

---

**文档版本**: v1.0  
**创建日期**: 2025-12-12  
**维护者**: AI Reviewer Team

