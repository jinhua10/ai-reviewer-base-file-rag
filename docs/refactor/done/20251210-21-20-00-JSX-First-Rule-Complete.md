# ✅ 前端代码规范更新完成 - JSX 优先规则
# Frontend Code Standards Update Complete - JSX First Rule

> **更新日期**: 2025-12-10 21:20:00  
> **规范版本**: v1.3  
> **状态**: ✅ 已生效

---

## 🎯 更新内容

### 核心变更：新增规则 6 - JSX 优先规范

**⭐ 核心原则 ⭐**: **所有新的前端代码必须使用 JSX 格式实现**

---

## 📋 新增规则总览

| 规则编号 | 规则名称 | 核心要求 |
|---------|---------|---------|
| **6.1** | 必须使用 JSX 实现新组件 | 禁止用纯 JS 创建 DOM，必须用 JSX |
| **6.2** | JSX 文件命名和组织 | `.jsx` 扩展名，PascalCase 命名 |
| **6.3** | JSX 组件结构规范 | 标准结构：imports → state → effects → handlers → render |
| **6.4** | JSX 代码风格 | 清晰缩进、条件渲染、列表渲染规范 |
| **6.5** | Props 和 State 管理 | Props 参数解构，State 清晰命名 |
| **6.6** | 事件处理 | `handle` 前缀，避免内联函数 |
| **6.7** | 注释和文档 | 组件文档注释，中英文代码注释 |
| **6.8** | 兼容性考虑 | React.createElement 后备方案 |

---

## 🚫 禁止的做法

### ❌ 错误：使用纯 JavaScript 创建 DOM

```javascript
// ❌ 不允许：使用 document.createElement
function createButton() {
    const button = document.createElement('button');
    button.className = 'btn btn-primary';
    button.textContent = 'Click Me';
    button.onclick = handleClick;
    return button;
}

// ❌ 不允许：使用字符串拼接 HTML
function createCard() {
    return `
        <div class="card">
            <h3>${title}</h3>
            <p>${content}</p>
        </div>
    `;
}

// ❌ 不允许：使用 innerHTML
element.innerHTML = '<button>Click Me</button>';
```

---

## ✅ 推荐的做法

### ✅ 正确：使用 JSX 实现组件

```jsx
/**
 * 按钮组件 (Button Component)
 * 
 * @param {Function} onClick - 点击事件处理 (Click handler)
 * @param {ReactNode} children - 按钮内容 (Button content)
 * @author AI Reviewer Team
 * @since 2025-12-10
 */
function Button({ onClick, children }) {
    return (
        <button className="btn btn-primary" onClick={onClick}>
            {children}
        </button>
    );
}

/**
 * 卡片组件 (Card Component)
 * 
 * @param {string} title - 卡片标题 (Card title)
 * @param {string} content - 卡片内容 (Card content)
 */
function Card({ title, content }) {
    return (
        <div className="card">
            <h3>{title}</h3>
            <p>{content}</p>
        </div>
    );
}

// 导出到全局 (Export to global)
window.Button = Button;
window.Card = Card;
```

---

## 📁 文件命名规范

### 组件文件命名

```yaml
规则:
  - 扩展名必须是 .jsx（不是 .js）
  - 使用 PascalCase 命名（每个单词首字母大写）
  - 文件名与组件名一致

✅ 正确示例:
  - WelcomeGuide.jsx          # 引导页面组件
  - DualTrackAnswer.jsx       # 双轨答案组件
  - HOPEDashboardPanel.jsx    # HOPE 仪表盘组件
  - DocumentUpload.jsx        # 文档上传组件
  - SearchFilter.jsx          # 搜索过滤组件

❌ 错误示例:
  - welcome-guide.js          # 错误：不是 JSX，命名不规范
  - welcomeGuide.jsx          # 错误：应使用 PascalCase
  - WelcomeGuide.js           # 错误：应该是 .jsx 扩展名
  - welcome_guide.jsx         # 错误：不应使用下划线
```

### 目录结构

```
src/main/resources/static/js/components/
├── common/                    # 通用组件 (Common components)
│   ├── Button.jsx
│   ├── Modal.jsx
│   ├── LoadingSpinner.jsx
│   └── ErrorMessage.jsx
├── tabs/                      # Tab 组件 (Tab components)
│   ├── QATab.jsx
│   ├── DocumentsTab.jsx
│   └── StatisticsTab.jsx
├── WelcomeGuide.jsx           # 顶层组件 (Top-level components)
├── DualTrackAnswer.jsx
├── HOPEDashboardPanel.jsx
└── App.jsx                    # 主应用 (Main app)
```

---

## 🏗️ 标准组件结构

### 完整的 JSX 组件模板

```jsx
/**
 * 组件名称 (Component Name)
 * 详细功能描述 (Detailed function description)
 * 
 * @param {Type} propName - 参数说明 (Parameter description)
 * @author AI Reviewer Team
 * @since 2025-12-10
 */

// 1️⃣ 导入 React 和 Hooks (Import React and Hooks)
const { useState, useEffect, useCallback, useMemo } = React;

// 2️⃣ 定义组件 (Define component)
function MyComponent({ 
    prop1, 
    prop2, 
    onAction,
    initialValue = 'default' 
}) {
    // 3️⃣ 国际化 (Internationalization)
    const { t } = window.LanguageModule.useTranslation();
    
    // 4️⃣ 状态定义 (State definition)
    const [data, setData] = useState(initialValue);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    
    // 5️⃣ 副作用 (Side effects)
    useEffect(() => {
        // 组件挂载时执行 (Execute on component mount)
        loadData();
        
        // 清理函数 (Cleanup function)
        return () => {
            // 清理逻辑 (Cleanup logic)
        };
    }, []);
    
    // 6️⃣ 事件处理函数 (Event handlers)
    const handleClick = useCallback(() => {
        // 处理点击事件 (Handle click event)
        setData(newValue);
        onAction?.();
    }, [onAction]);
    
    const handleSubmit = useCallback(async (e) => {
        e.preventDefault();
        setIsLoading(true);
        
        try {
            // 提交数据 (Submit data)
            await submitData(data);
            onAction?.('success');
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    }, [data, onAction]);
    
    // 7️⃣ 计算属性 (Computed values)
    const formattedData = useMemo(() => {
        // 格式化数据 (Format data)
        return data?.toString() || '';
    }, [data]);
    
    // 8️⃣ 辅助渲染函数 (Helper render functions)
    const renderContent = () => {
        if (isLoading) return <LoadingSpinner />;
        if (error) return <ErrorMessage message={error} />;
        if (!data) return <EmptyState />;
        return <DataView data={formattedData} />;
    };
    
    // 9️⃣ 主渲染 (Main render)
    return (
        <div className="my-component">
            {/* 头部 (Header) */}
            <header className="my-component__header">
                <h2>{t('myTitle')}</h2>
            </header>
            
            {/* 内容 (Content) */}
            <main className="my-component__content">
                {renderContent()}
            </main>
            
            {/* 底部 (Footer) */}
            <footer className="my-component__footer">
                <button onClick={handleClick}>
                    {t('myButton')}
                </button>
            </footer>
        </div>
    );
}

// 🔟 导出到全局 (Export to global)
window.MyComponent = MyComponent;
```

---

## 🎨 代码风格规范

### 缩进和格式

```jsx
// ✅ 正确：清晰的层级结构
function MyComponent() {
    return (
        <div className="container">
            <header>
                <h1>Title</h1>
                <nav>
                    <a href="#home">Home</a>
                    <a href="#about">About</a>
                </nav>
            </header>
            <main>
                <section>
                    <p>Content</p>
                </section>
            </main>
        </div>
    );
}

// ❌ 错误：��有代码挤在一起
function MyComponent() {
    return <div className="container"><header><h1>Title</h1><nav><a href="#home">Home</a><a href="#about">About</a></nav></header><main><section><p>Content</p></section></main></div>;
}
```

### 条件渲染

```jsx
// ✅ 推荐：使用 && 操作符（简单条件）
{isLoading && <LoadingSpinner />}
{error && <ErrorMessage message={error} />}
{showModal && <Modal onClose={handleClose} />}

// ✅ 推荐：使用三元运算符（二选一）
{isLoggedIn ? <Dashboard /> : <LoginForm />}
{hasData ? <DataTable data={data} /> : <EmptyState />}

// ✅ 推荐：复杂条件提取为函数
const renderContent = () => {
    if (isLoading) return <LoadingSpinner />;
    if (error) return <ErrorMessage message={error} />;
    if (!data || data.length === 0) return <EmptyState />;
    return <DataTable data={data} />;
};

return (
    <div className="container">
        {renderContent()}
    </div>
);

// ❌ 不推荐：复杂的内联条件
{isLoading ? <LoadingSpinner /> : error ? <ErrorMessage /> : !data ? <EmptyState /> : <DataTable />}
```

### 列表渲染

```jsx
// ✅ 正确：使用 map 和唯一的 key
<ul>
    {items.map(item => (
        <li key={item.id}>
            <span>{item.name}</span>
        </li>
    ))}
</ul>

// ✅ 正确：复杂列表项提取为组件
<ul>
    {items.map(item => (
        <ListItem key={item.id} item={item} />
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

## 🎯 Props 和 State 管理

### Props 解构

```jsx
// ✅ 推荐：在参数中解构 props，提供默认值
function MyComponent({ 
    title, 
    content, 
    onSave, 
    isEditing = false,
    maxLength = 1000 
}) {
    return (
        <div>
            <h2>{title}</h2>
            <p>{content.substring(0, maxLength)}</p>
            {isEditing && (
                <button onClick={onSave}>
                    Save
                </button>
            )}
        </div>
    );
}

// ❌ 不推荐：在组件内部解构
function MyComponent(props) {
    const { title, content, onSave, isEditing } = props;
    const maxLength = props.maxLength || 1000;
    // ...
}
```

### State 命名规范

```jsx
// ✅ 推荐：清晰的布尔值命名
const [isLoading, setIsLoading] = useState(false);
const [isVisible, setIsVisible] = useState(true);
const [hasError, setHasError] = useState(false);
const [canSubmit, setCanSubmit] = useState(false);

// ✅ 推荐：清晰的对象/数组命名
const [documents, setDocuments] = useState([]);
const [selectedDocument, setSelectedDocument] = useState(null);
const [userProfile, setUserProfile] = useState({});
const [formData, setFormData] = useState({ name: '', email: '' });

// ❌ 不推荐：模糊的命名
const [flag, setFlag] = useState(false);
const [data, setData] = useState([]);
const [item, setItem] = useState(null);
const [value, setValue] = useState('');
```

---

## 🎪 事件处理规范

### 事件处理函数命名

```jsx
// ✅ 推荐：事件处理函数使用 handle 前缀
const handleClick = () => { /* ... */ };
const handleSubmit = (e) => { e.preventDefault(); /* ... */ };
const handleChange = (e) => { setValue(e.target.value); };
const handleDelete = (id) => { deleteItem(id); };
const handleFileSelect = (files) => { uploadFiles(files); };

// ✅ 推荐：传递给子组件的 props 使用 on 前缀
<ChildComponent
    onClick={handleClick}
    onSubmit={handleSubmit}
    onChange={handleChange}
    onDelete={handleDelete}
    onFileSelect={handleFileSelect}
/>
```

### 避免内联函数

```jsx
// ❌ 不推荐：内联函数（每次渲染都创建新函数）
<button onClick={() => handleDelete(item.id)}>Delete</button>
<input onChange={(e) => setValue(e.target.value)} />

// ✅ 推荐：使用 useCallback
const handleDeleteClick = useCallback(() => {
    handleDelete(item.id);
}, [item.id]);

const handleInputChange = useCallback((e) => {
    setValue(e.target.value);
}, []);

<button onClick={handleDeleteClick}>Delete</button>
<input onChange={handleInputChange} />

// ✅ 可接受：简单的状态切换可以使用内联
<button onClick={() => setShowModal(true)}>Open</button>
<button onClick={() => setIsVisible(!isVisible)}>Toggle</button>
```

---

## 📝 注释规范

### 组件文档注释

```jsx
/**
 * 文档上传组件 (Document Upload Component)
 * 
 * 功能说明：
 * - 支持拖拽上传文件
 * - 支持批量上传（最多 10 个文件）
 * - 支持文件类型限制
 * - 显示上传进度
 * 
 * Features:
 * - Drag & drop file upload
 * - Batch upload (max 10 files)
 * - File type restriction
 * - Upload progress display
 * 
 * @param {string} uploadUrl - 上传 API 地址 (Upload API URL)
 * @param {Function} onSuccess - 上传成功回调，参数：(fileData) (Success callback with fileData)
 * @param {Function} onError - 上传失败回调，参数：(error) (Error callback with error)
 * @param {Array<string>} acceptedTypes - 允许的文件类型，如 ['.pdf', '.docx'] (Accepted file types)
 * @param {number} maxSize - 最大文件大小（MB），默认 100 (Max file size in MB, default 100)
 * @param {boolean} multiple - 是否支持多文件上传，默认 true (Allow multiple files, default true)
 * 
 * @example
 * <DocumentUpload
 *     uploadUrl="/api/documents/upload"
 *     onSuccess={handleUploadSuccess}
 *     onError={handleUploadError}
 *     acceptedTypes={['.pdf', '.docx', '.xlsx']}
 *     maxSize={100}
 *     multiple={true}
 * />
 * 
 * @author AI Reviewer Team
 * @since 2025-12-10
 */
function DocumentUpload({ 
    uploadUrl, 
    onSuccess, 
    onError, 
    acceptedTypes = [], 
    maxSize = 100,
    multiple = true 
}) {
    // ...实现...
}
```

### 代码注释

```jsx
function MyComponent() {
    // 初始化状态 (Initialize state)
    const [data, setData] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    
    // 加载数据 (Load data)
    useEffect(() => {
        setIsLoading(true);
        
        // 发送请求到后端 API (Send request to backend API)
        fetch('/api/data')
            .then(response => response.json())
            .then(result => {
                // 过滤无效数据 (Filter invalid data)
                const validData = result.filter(item => item.isValid);
                setData(validData);
            })
            .catch(error => {
                // 记录错误日志 (Log error)
                console.error('Failed to load data:', error);
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);
    
    // ...existing code...
}
```

---

## 📊 更新统计

### 文档改动

| 项目 | 改动 |
|------|------|
| 新增章节 | 规则 6：JSX 优先规范 (8 个子规则) |
| 新增内容 | ~500 行规范文档 |
| 新增示例 | 30+ 个代码示例 |
| 更新版本 | v1.2 → v1.3 |

### 检查清单改动

**新增前端代码检查项**（7 项）:
- ⭐ JSX 优先
- 文件命名
- 组件结构
- Props 解构
- 事件处理
- 条件渲染
- 列表渲染

---

## ✅ 验收标准

### 规范完整性

- [x] ✅ 新增规则 6.1-6.8，共 8 个子规则
- [x] ✅ 提供 30+ 个代码示例
- [x] ✅ 包含正确和错误示例对比
- [x] ✅ 中英文注释完整
- [x] ✅ 更新版本历史
- [x] ✅ 更新检查清单

### 实用性

- [x] ✅ 提供标准组件模板
- [x] ✅ 提供完整的目录结构
- [x] ✅ 提供文件命名规范
- [x] ✅ 提供代码风格指南
- [x] ✅ 提供注释规范

---

## 🎉 总结

### 核心价值

1. **统一标准**: 明确 JSX 作为前端开发的唯一标准
2. **提高质量**: 标准化的组件结构和代码风格
3. **易于维护**: 清晰的命名和注释规范
4. **团队协作**: 统一的开发规范，降低沟通成本
5. **新人友好**: 详细的示例和模板，快速上手

### 立即生效

**从现在开始，所有新的前端代码必须**:
- ✅ 使用 `.jsx` 文件格式
- ✅ 使用 JSX 语法实现组件
- ✅ 遵循标准组件结构
- ✅ 使用 PascalCase 命名组件文件
- ✅ 添加完整的文档注释
- ✅ 遵循代码风格规范

---

**状态**: ✅ 完成  
**编译**: ✅ BUILD SUCCESS  
**规范版本**: v1.3  
**生效时间**: 立即生效  
**适用范围**: 所有新的前端代码开发

