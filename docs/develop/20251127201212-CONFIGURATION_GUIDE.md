# 文档管理分页功能 - 配置说明

## 📋 配置概览

文档管理分页功能支持灵活配置，以适应不同的使用场景和性能需求。

## ⚙️ 默认配置

### JavaScript 配置（在 index.html 中）

```javascript
// 分页默认配置
const [currentPage, setCurrentPage] = useState(1);        // 默认第一页
const [pageSize, setPageSize] = useState(20);            // 默认每页 20 条

// 排序默认配置
const [sortBy, setSortBy] = useState('date');            // 默认按日期排序
const [sortOrder, setSortOrder] = useState('desc');       // 默认降序
```

### 配置参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `currentPage` | number | 1 | 当前页码 |
| `pageSize` | number | 20 | 每页显示数量 |
| `sortBy` | string | 'date' | 排序字段 |
| `sortOrder` | string | 'desc' | 排序方向 |

## 🎛️ 可配置项详解

### 1. 每页显示数量 (pageSize)

**可选值**:
```javascript
10   // 适合移动端或快速浏览
20   // 推荐默认值，平衡性能和体验
50   // 适合查看更多内容
100  // 适合高级用户或批量操作
-1   // 显示全部（不推荐超过1000个文档时使用）
```

**修改位置**:
```javascript
// 在 DocumentsTab 组件中
const [pageSize, setPageSize] = useState(20);  // 修改这里的数字
```

**推荐配置**:
| 文档数量 | 推荐值 | 理由 |
|---------|--------|------|
| < 50 | -1 | 全部显示，无需分页 |
| 50-500 | 20 | 最佳用户体验 |
| 500-2000 | 50 | 减少翻页次数 |
| > 2000 | 100 | 高效批量操作 |

### 2. 默认排序方式 (sortBy)

**可选值**:
```javascript
'name'  // 按文件名排序
'size'  // 按文件大小排序
'date'  // 按上传时间排序（推荐）
'type'  // 按文件类型排序
```

**修改位置**:
```javascript
const [sortBy, setSortBy] = useState('date');  // 修改为其他值
```

**使用场景**:
- **'date'**: 最常用，显示最新文档
- **'name'**: 按字母顺序查找
- **'size'**: 空间管理，查找大文件
- **'type'**: 文件分类管理

### 3. 默认排序方向 (sortOrder)

**可选值**:
```javascript
'asc'   // 升序：从小到大、旧到新、A-Z
'desc'  // 降序：从大到小、新到旧、Z-A（推荐）
```

**修改位置**:
```javascript
const [sortOrder, setSortOrder] = useState('desc');  // 修改为 'asc'
```

**推荐组合**:
| 排序字段 | 推荐方向 | 说明 |
|---------|---------|------|
| date | desc | 最新的在前 |
| size | desc | 大文件在前 |
| name | asc | A-Z 顺序 |
| type | asc | 按类型分组 |

## 🎨 UI 配置

### 1. 分页按钮样式

**位置**: `<style>` 标签中的 `.pagination-btn`

**可配置属性**:
```css
.pagination-btn {
    padding: 8px 16px;              /* 按钮内边距 */
    border: 2px solid #667eea;       /* 边框颜色和宽度 */
    border-radius: 6px;              /* 圆角大小 */
    background: white;               /* 背景色 */
    color: #667eea;                  /* 文字颜色 */
    font-size: 14px;                 /* 字体大小 */
}
```

### 2. 控制栏背景色

**位置**: 控制栏的 `style` 属性

**修改示例**:
```javascript
style={{
    background: '#f8f9fa',  // 修改为其他颜色
    padding: '10px',
    borderRadius: '6px'
}}
```

**推荐色彩**:
- 浅灰: `#f8f9fa` (默认)
- 浅蓝: `#e3f2fd`
- 浅绿: `#f1f8f4`
- 浅黄: `#fffbf0`

### 3. 分页栏位置

**默认**: 底部中心对齐

**修改为左对齐**:
```css
.pagination-container {
    justify-content: flex-start;  /* 改为 flex-start */
}
```

**修改为右对齐**:
```css
.pagination-container {
    justify-content: flex-end;    /* 改为 flex-end */
}
```

## 🌐 国际化配置

### 添加新语言

**步骤**:
1. 在 `lang.js` 中添加语言代码
2. 复制现有翻译结构
3. 翻译所有文本

**示例**（添加日语）:
```javascript
const translations = {
    zh: { /* 中文 */ },
    en: { /* 英文 */ },
    ja: {  // 新增日语
        docsPageSize: 'ページあたりのアイテム数',
        docsPaginationPrev: '前へ',
        docsPaginationNext: '次へ',
        // ... 其他翻译
    }
};
```

### 修改默认语言

**位置**: `LanguageProvider` 组件中

```javascript
const [language, setLanguage] = useState('zh');  // 修改为 'en' 或其他
```

## 📱 响应式配置

### 移动端断点

**默认配置**:
```css
@media (max-width: 768px) {
    /* 移动端样式 */
}
```

**修改断点**:
```css
@media (max-width: 600px) {  /* 修改为 600px */
    /* 更小的屏幕 */
}
```

### 移动端每页显示

**建议在移动端自动调整**:
```javascript
useEffect(() => {
    // 检测是否为移动端
    if (window.innerWidth < 768) {
        setPageSize(10);  // 移动端显示 10 条
    }
}, []);
```

## 🔧 高级配置

### 1. 自定义排序规则

**位置**: `sortDocuments` 函数

**添加自定义排序**:
```javascript
case 'custom':
    // 自定义排序逻辑
    compareResult = /* 你的逻辑 */;
    break;
```

### 2. 搜索字段扩展

**当前**: 只搜索文件名

**扩展为多字段搜索**:
```javascript
const filteredDocuments = documents.filter(doc =>
    doc.fileName.toLowerCase().includes(filterText.toLowerCase()) ||
    doc.fileType.toLowerCase().includes(filterText.toLowerCase()) ||
    doc.uploadTime.includes(filterText)
);
```

### 3. 分页缓存

**添加分页状态缓存**:
```javascript
// 保存到 localStorage
localStorage.setItem('pageSize', pageSize);
localStorage.setItem('sortBy', sortBy);

// 初始化时读取
const [pageSize, setPageSize] = useState(
    Number(localStorage.getItem('pageSize')) || 20
);
```

## ⚡ 性能优化配置

### 1. 虚拟滚动阈值

**适用场景**: 超过 10000 个文档

**配置示例**:
```javascript
const VIRTUAL_SCROLL_THRESHOLD = 10000;

if (documents.length > VIRTUAL_SCROLL_THRESHOLD) {
    // 启用虚拟滚动
    useVirtualScrolling();
}
```

### 2. 搜索防抖

**添加搜索防抖**:
```javascript
const [debouncedFilter, setDebouncedFilter] = useState('');

useEffect(() => {
    const timer = setTimeout(() => {
        setDebouncedFilter(filterText);
    }, 300);  // 300ms 延迟
    
    return () => clearTimeout(timer);
}, [filterText]);
```

### 3. 懒加载

**延迟加载非首屏文档**:
```javascript
const [loadedPages, setLoadedPages] = useState(new Set([1]));

useEffect(() => {
    if (!loadedPages.has(currentPage)) {
        // 加载当前页数据
        loadPage(currentPage);
    }
}, [currentPage]);
```

## 📊 数据格式配置

### 文档数据结构

**标准格式**:
```javascript
{
    fileName: string,      // 文件名
    fileSize: number,      // 文件大小（字节）
    fileType: string,      // 文件类型
    uploadTime: string,    // 上传时间
    indexed: boolean       // 是否已索引
}
```

**自定义字段**:
```javascript
{
    // ...标准字段
    tags: string[],        // 标签数组
    description: string,   // 描述
    category: string       // 分类
}
```

### 日期格式

**默认**: 服务器返回格式

**自定义格式化**:
```javascript
const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
};
```

## 🎯 使用场景配置

### 场景 1: 企业文档管理

**推荐配置**:
```javascript
pageSize: 50           // 较大的每页数量
sortBy: 'date'        // 按时间排序
sortOrder: 'desc'     // 最新的在前
```

### 场景 2: 个人文档整理

**推荐配置**:
```javascript
pageSize: 20          // 中等每页数量
sortBy: 'name'       // 按名称排序
sortOrder: 'asc'     // A-Z 排序
```

### 场景 3: 移动端访问

**推荐配置**:
```javascript
pageSize: 10          // 较小的每页数量
// 启用触摸优化
touchOptimized: true
```

### 场景 4: 归档查询

**推荐配置**:
```javascript
pageSize: 100         // 大每页数量
sortBy: 'date'       // 按时间排序
sortOrder: 'asc'     // 从旧到新
```

## 🔒 安全配置

### 1. 页码验证

**防止非法页码**:
```javascript
const goToPage = (page) => {
    // 验证页码
    if (page < 1) page = 1;
    if (page > totalPages) page = totalPages;
    if (isNaN(page)) return;
    
    setCurrentPage(page);
};
```

### 2. 输入过滤

**防止 XSS 攻击**:
```javascript
const sanitizeInput = (input) => {
    return input
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .trim();
};
```

## 📝 配置文件模板

**创建单独的配置文件**: `paginationConfig.js`

```javascript
export const PAGINATION_CONFIG = {
    // 默认设置
    defaults: {
        pageSize: 20,
        sortBy: 'date',
        sortOrder: 'desc',
        currentPage: 1
    },
    
    // 每页显示选项
    pageSizeOptions: [10, 20, 50, 100, -1],
    
    // 排序选项
    sortOptions: {
        fields: ['name', 'size', 'date', 'type'],
        orders: ['asc', 'desc']
    },
    
    // 性能设置
    performance: {
        virtualScrollThreshold: 10000,
        searchDebounceMs: 300,
        cacheEnabled: true
    },
    
    // UI 设置
    ui: {
        theme: 'light',
        primaryColor: '#667eea',
        borderRadius: '6px'
    }
};
```

## 🧪 测试配置

### 单元测试配置

**Jest 配置示例**:
```javascript
describe('Pagination', () => {
    test('should calculate pages correctly', () => {
        const totalItems = 100;
        const pageSize = 20;
        const expectedPages = 5;
        
        expect(Math.ceil(totalItems / pageSize)).toBe(expectedPages);
    });
});
```

## 📚 相关文档

- [实现报告](./202511270230-DOCUMENT_PAGINATION.md)
- [使用指南](./202511270230-PAGINATION_USER_GUIDE.md)
- [快速参考](./202511270230-QUICK_REFERENCE.md)

## 🤝 配置建议

### DO ✅

- ✅ 根据文档数量调整每页显示
- ✅ 使用合理的默认值
- ✅ 启用搜索防抖
- ✅ 缓存用户偏好设置
- ✅ 移动端使用较小的每页数量

### DON'T ❌

- ❌ 超过 1000 文档时使用"显示全部"
- ❌ 在移动端使用过大的每页数量
- ❌ 禁用排序功能
- ❌ 忽略性能优化
- ❌ 使用不合理的默认排序

## 🔄 配置更新流程

1. **修改配置** → 更新相关变量
2. **测试功能** → 验证配置效果
3. **性能检查** → 确保性能达标
4. **用户反馈** → 收集使用反馈
5. **持续优化** → 迭代改进

---

**版本**: v1.0  
**更新**: 2025-11-27  
**维护**: AI Reviewer Team

