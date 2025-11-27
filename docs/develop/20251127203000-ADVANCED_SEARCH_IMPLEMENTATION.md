# 文档管理高级搜索功能实现报告

## 📋 需求概述

实现一个功能强大的高级搜索系统，支持：

1. **多条件组合搜索**
   - 文件名（支持包含/精确/正则三种模式）
   - 文件类型（多选）
   - 文件大小范围
   - 上传日期范围
   - 索引状态

2. **高性能**
   - 后端处理过滤逻辑
   - 支持大量文档（10000+）

3. **良好的用户体验**
   - 简单搜索/高级搜索切换
   - 可视化的筛选条件显示
   - 一键重置

## 🎯 已完成：后端API

### API参数

已扩展 `/api/documents/list` 接口，新增以下参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| search | String | "" | 搜索关键词 |
| searchMode | String | "contains" | 搜索模式：contains/exact/regex |
| fileTypes | String | "" | 文件类型，逗号分隔，如"pdf,docx" |
| minSize | long | 0 | 最小文件大小（字节） |
| maxSize | long | Long.MAX | 最大文件大小（字节） |
| indexed | String | "all" | 索引状态：true/false/all |
| startDate | String | "" | 开始日期 yyyy-MM-dd |
| endDate | String | "" | 结束日期 yyyy-MM-dd |

### API 请求示例

```http
GET /api/documents/list?
    page=1&
    pageSize=20&
    sortBy=date&
    sortOrder=desc&
    search=报告&
    searchMode=contains&
    fileTypes=pdf,docx&
    minSize=1048576&
    maxSize=10485760&
    indexed=true&
    startDate=2025-01-01&
    endDate=2025-12-31
```

### 后端过滤逻辑

实现了 `advancedFilter()` 方法，使用Java Stream API进行高效过滤：

```java
private List<DocumentInfo> advancedFilter(
    List<DocumentInfo> documents,
    String search, String searchMode, String fileTypes,
    long minSize, long maxSize, String indexed,
    String startDate, String endDate) {
    
    return documents.stream().filter(doc -> {
        // 1. 文件名搜索（支持包含/精确/正则）
        // 2. 文件类型过滤（多选）
        // 3. 文件大小范围
        // 4. 索引状态
        // 5. 日期范围
        return true; // 满足所有条件
    }).collect(Collectors.toList());
}
```

## 🎨 需要实现：前端UI

### 1. API方法更新

**位置**: `index.html` 第1208行

**当前**:
```javascript
listDocuments: async (page = 1, pageSize = 20, sortBy = 'date', sortOrder = 'desc', search = '') => {
    const response = await axios.get(`${API_DOCS_URL}/list`, {
        params: { page, pageSize, sortBy, sortOrder, search }
    });
    return response.data;
},
```

**需要修改为**:
```javascript
listDocuments: async (page, pageSize, sortBy, sortOrder, filters = {}) => {
    const params = {
        page: page || 1,
        pageSize: pageSize || 20,
        sortBy: sortBy || 'date',
        sortOrder: sortOrder || 'desc',
        search: filters.search || '',
        searchMode: filters.searchMode || 'contains',
        fileTypes: filters.fileTypes || '',
        minSize: filters.minSize || 0,
        maxSize: filters.maxSize || 9223372036854775807,
        indexed: filters.indexed || 'all',
        startDate: filters.startDate || '',
        endDate: filters.endDate || ''
    };
    
    const response = await axios.get(`${API_DOCS_URL}/list`, { params });
    return response.data;
},
```

### 2. DocumentsTab组件状态

**需要添加的状态**:

```javascript
// 高级搜索状态
const [showAdvancedSearch, setShowAdvancedSearch] = useState(false);
const [advancedFilters, setAdvancedFilters] = useState({
    search: '',
    searchMode: 'contains',
    fileTypes: [],  // 选中的文件类型数组
    minSize: '',
    maxSize: '',
    indexed: 'all',
    startDate: '',
    endDate: ''
});

// 常用文件类型列表
const FILE_TYPES = ['pdf', 'docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'txt', 'md', 'html', 'xml'];
```

### 3. loadDocuments方法更新

**当前调用**:
```javascript
const result = await api.listDocuments(currentPage, pageSize, sortBy, sortOrder, filterText);
```

**需要修改为**:
```javascript
// 构建过滤参数
const filters = showAdvancedSearch ? {
    search: advancedFilters.search,
    searchMode: advancedFilters.searchMode,
    fileTypes: advancedFilters.fileTypes.join(','),
    minSize: advancedFilters.minSize ? parseInt(advancedFilters.minSize) * 1024 * 1024 : 0,
    maxSize: advancedFilters.maxSize ? parseInt(advancedFilters.maxSize) * 1024 * 1024 : 9223372036854775807,
    indexed: advancedFilters.indexed,
    startDate: advancedFilters.startDate,
    endDate: advancedFilters.endDate
} : {
    search: filterText,
    searchMode: 'contains'
};

const result = await api.listDocuments(currentPage, pageSize, sortBy, sortOrder, filters);
```

### 4. 高级搜索UI布局

```jsx
{/* 搜索模式切换 */}
<div style={{marginBottom: '10px'}}>
    <button 
        onClick={() => setShowAdvancedSearch(!showAdvancedSearch)}
        className="btn btn-secondary"
    >
        {showAdvancedSearch ? t('docsSimpleSearch') : t('docsAdvancedSearch')}
    </button>
</div>

{/* 简单搜索 */}
{!showAdvancedSearch && (
    <input
        type="text"
        className="input-field"
        placeholder={t('docsFilterPlaceholder')}
        value={filterText}
        onChange={(e) => handleSearchChange(e.target.value)}
    />
)}

{/* 高级搜索面板 */}
{showAdvancedSearch && (
    <div className="advanced-search-panel">
        {/* 文件名搜索 */}
        <div className="filter-row">
            <label>{t('docsFilterPlaceholder')}</label>
            <input
                type="text"
                value={advancedFilters.search}
                onChange={(e) => updateFilter('search', e.target.value)}
            />
            <select
                value={advancedFilters.searchMode}
                onChange={(e) => updateFilter('searchMode', e.target.value)}
            >
                <option value="contains">{t('docsSearchModeContains')}</option>
                <option value="exact">{t('docsSearchModeExact')}</option>
                <option value="regex">{t('docsSearchModeRegex')}</option>
            </select>
        </div>

        {/* 文件类型多选 */}
        <div className="filter-row">
            <label>{t('docsFileTypeFilter')}</label>
            <div className="file-type-checkboxes">
                {FILE_TYPES.map(type => (
                    <label key={type} className="checkbox-label">
                        <input
                            type="checkbox"
                            checked={advancedFilters.fileTypes.includes(type)}
                            onChange={(e) => toggleFileType(type, e.target.checked)}
                        />
                        {type.toUpperCase()}
                    </label>
                ))}
            </div>
        </div>

        {/* 文件大小 */}
        <div className="filter-row">
            <label>{t('docsFileSizeFilter')}</label>
            <input
                type="number"
                placeholder={t('docsFileSizeMin')}
                value={advancedFilters.minSize}
                onChange={(e) => updateFilter('minSize', e.target.value)}
            />
            <span> - </span>
            <input
                type="number"
                placeholder={t('docsFileSizeMax')}
                value={advancedFilters.maxSize}
                onChange={(e) => updateFilter('maxSize', e.target.value)}
            />
            <span>{t('docsFileSizeUnit')}</span>
        </div>

        {/* 索引状态 */}
        <div className="filter-row">
            <label>{t('docsIndexedFilter')}</label>
            <select
                value={advancedFilters.indexed}
                onChange={(e) => updateFilter('indexed', e.target.value)}
            >
                <option value="all">{t('docsIndexedAll')}</option>
                <option value="true">{t('docsIndexedYes')}</option>
                <option value="false">{t('docsIndexedNo')}</option>
            </select>
        </div>

        {/* 日期范围 */}
        <div className="filter-row">
            <label>{t('docsDateFilter')}</label>
            <input
                type="date"
                value={advancedFilters.startDate}
                onChange={(e) => updateFilter('startDate', e.target.value)}
            />
            <span> - </span>
            <input
                type="date"
                value={advancedFilters.endDate}
                onChange={(e) => updateFilter('endDate', e.target.value)}
            />
        </div>

        {/* 操作按钮 */}
        <div className="filter-actions">
            <button onClick={applyFilters} className="btn btn-primary">
                {t('docsApplyFilter')}
            </button>
            <button onClick={resetFilters} className="btn btn-secondary">
                {t('docsResetFilter')}
            </button>
        </div>
    </div>
)}

{/* 当前激活的筛选条件显示 */}
{showAdvancedSearch && hasActiveFilters() && (
    <div className="active-filters">
        <span>{t('docsActiveFilters')}: {getActiveFilterCount()} {t('docsFilterCount')}</span>
        {/* 显示各个激活的筛选条件标签 */}
    </div>
)}
```

### 5. CSS样式

```css
/* 高级搜索面板 */
.advanced-search-panel {
    background: #f8f9fa;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 15px;
    border: 2px solid #667eea;
}

.filter-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 15px;
    flex-wrap: wrap;
}

.filter-row label {
    min-width: 100px;
    font-weight: 600;
    color: #333;
}

.filter-row input[type="text"],
.filter-row input[type="number"],
.filter-row input[type="date"],
.filter-row select {
    padding: 8px 12px;
    border: 2px solid #e0e0e0;
    border-radius: 4px;
    font-size: 14px;
}

.filter-row input[type="text"]:focus,
.filter-row input[type="number"]:focus,
.filter-row input[type="date"]:focus,
.filter-row select:focus {
    border-color: #667eea;
    outline: none;
}

/* 文件类型多选 */
.file-type-checkboxes {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
}

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 5px;
    cursor: pointer;
    font-size: 14px;
}

.checkbox-label input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
}

/* 筛选操作按钮 */
.filter-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    margin-top: 20px;
}

/* 激活的筛选条件显示 */
.active-filters {
    background: #e3f2fd;
    padding: 10px 15px;
    border-radius: 6px;
    margin-bottom: 15px;
    border-left: 4px solid #2196f3;
}

.filter-tag {
    display: inline-block;
    background: #2196f3;
    color: white;
    padding: 4px 8px;
    border-radius: 4px;
    margin: 0 5px;
    font-size: 12px;
}

.filter-tag-remove {
    margin-left: 5px;
    cursor: pointer;
    font-weight: bold;
}
```

### 6. 辅助函数

```javascript
// 更新筛选条件
const updateFilter = (key, value) => {
    setAdvancedFilters(prev => ({
        ...prev,
        [key]: value
    }));
};

// 切换文件类型选择
const toggleFileType = (type, checked) => {
    setAdvancedFilters(prev => ({
        ...prev,
        fileTypes: checked 
            ? [...prev.fileTypes, type]
            : prev.fileTypes.filter(t => t !== type)
    }));
};

// 应用筛选
const applyFilters = () => {
    setCurrentPage(1);
    loadDocuments();
};

// 重置筛选
const resetFilters = () => {
    setAdvancedFilters({
        search: '',
        searchMode: 'contains',
        fileTypes: [],
        minSize: '',
        maxSize: '',
        indexed: 'all',
        startDate: '',
        endDate: ''
    });
    setCurrentPage(1);
    loadDocuments();
};

// 检查是否有激活的筛选条件
const hasActiveFilters = () => {
    return advancedFilters.search !== '' ||
           advancedFilters.fileTypes.length > 0 ||
           advancedFilters.minSize !== '' ||
           advancedFilters.maxSize !== '' ||
           advancedFilters.indexed !== 'all' ||
           advancedFilters.startDate !== '' ||
           advancedFilters.endDate !== '';
};

// 获取激活的筛选条件数量
const getActiveFilterCount = () => {
    let count = 0;
    if (advancedFilters.search) count++;
    if (advancedFilters.fileTypes.length > 0) count++;
    if (advancedFilters.minSize || advancedFilters.maxSize) count++;
    if (advancedFilters.indexed !== 'all') count++;
    if (advancedFilters.startDate || advancedFilters.endDate) count++;
    return count;
};
```

## 🔄 数据流

```
用户输入筛选条件
    ↓
点击"应用筛选"
    ↓
applyFilters()
    ↓
构建filters对象
    ↓
api.listDocuments(page, size, sort, order, filters)
    ↓
HTTP请求（带所有筛选参数）
    ↓
后端advancedFilter()处理
    ↓
返回过滤后的结果
    ↓
前端展示
```

## 📊 使用示例

### 示例1: 查找所有PDF和Word文档

1. 点击"高级搜索"
2. 文件类型勾选：PDF, DOCX, DOC
3. 点击"应用筛选"

**后端请求**:
```
GET /api/documents/list?fileTypes=pdf,docx,doc&...
```

### 示例2: 查找大于10MB且已索引的文档

1. 点击"高级搜索"
2. 最小大小：10 MB
3. 索引状态：已索引
4. 点击"应用筛选"

**后端请求**:
```
GET /api/documents/list?minSize=10485760&indexed=true&...
```

### 示例3: 使用正则表达式查找

1. 点击"高级搜索"
2. 文件名：`report_\d{4}`
3. 搜索模式：正则表达式
4. 点击"应用筛选"

**匹配**: `report_2024.pdf`, `report_2023.docx`

### 示例4: 查找特定日期范围的文档

1. 点击"高级搜索"
2. 开始日期：2025-01-01
3. 结束日期：2025-03-31
4. 点击"应用筛选"

**后端请求**:
```
GET /api/documents/list?startDate=2025-01-01&endDate=2025-03-31&...
```

## ⚡ 性能优化

### 1. 防抖处理

```javascript
const [searchDebounce, setSearchDebounce] = useState(null);

const handleSearchInput = (value) => {
    updateFilter('search', value);
    
    if (searchDebounce) {
        clearTimeout(searchDebounce);
    }
    
    const timer = setTimeout(() => {
        applyFilters();
    }, 500);
    
    setSearchDebounce(timer);
};
```

### 2. 缓存策略

```javascript
const filterCache = new Map();

const getCacheKey = (filters) => {
    return JSON.stringify(filters);
};

const loadDocumentsWithCache = async () => {
    const cacheKey = getCacheKey({ currentPage, pageSize, sortBy, sortOrder, advancedFilters });
    
    if (filterCache.has(cacheKey)) {
        setDocuments(filterCache.get(cacheKey));
        return;
    }
    
    const result = await loadDocuments();
    filterCache.set(cacheKey, result);
};
```

### 3. 文件类型预加载

从后端获取实际存在的文件类型列表：

```javascript
const [availableFileTypes, setAvailableFileTypes] = useState([]);

useEffect(() => {
    // 从文档列表中提取所有唯一的文件类型
    const types = [...new Set(documents.map(doc => doc.fileType))];
    setAvailableFileTypes(types.sort());
}, [documents]);
```

## 🎯 用户体验优化

### 1. 筛选条件可视化

显示当前激活的筛选标签：

```jsx
{advancedFilters.search && (
    <span className="filter-tag">
        文件名: {advancedFilters.search}
        <span className="filter-tag-remove" onClick={() => updateFilter('search', '')}>
            ×
        </span>
    </span>
)}

{advancedFilters.fileTypes.length > 0 && (
    <span className="filter-tag">
        类型: {advancedFilters.fileTypes.join(', ')}
        <span className="filter-tag-remove" onClick={() => updateFilter('fileTypes', [])}>
            ×
        </span>
    </span>
)}
```

### 2. 快捷筛选

添加常用筛选快捷按钮：

```jsx
<div className="quick-filters">
    <button onClick={() => quickFilter('pdf')}>仅PDF</button>
    <button onClick={() => quickFilter('word')}>仅Word</button>
    <button onClick={() => quickFilter('indexed')}>已索引</button>
    <button onClick={() => quickFilter('large')}>大文件(>10MB)</button>
</div>
```

### 3. 保存筛选方案

```javascript
const [savedFilters, setSavedFilters] = useState([]);

const saveCurrentFilter = (name) => {
    const newFilter = {
        name,
        filters: { ...advancedFilters }
    };
    setSavedFilters([...savedFilters, newFilter]);
    localStorage.setItem('savedFilters', JSON.stringify([...savedFilters, newFilter]));
};

const loadSavedFilter = (filter) => {
    setAdvancedFilters(filter.filters);
    applyFilters();
};
```

## ✅ 实现检查清单

### 后端
- [x] 扩展API参数
- [x] 实现advancedFilter方法
- [x] 支持正则表达式搜索
- [x] 支持文件类型多选
- [x] 支持文件大小范围
- [x] 支持日期范围
- [x] 支持索引状态过滤

### 前端
- [ ] 更新API调用方法
- [ ] 添加高级搜索状态
- [ ] 实现高级搜索UI
- [ ] 添加筛选条件可视化
- [ ] 实现快捷筛选
- [ ] 添加CSS样式
- [ ] 测试所有功能

## 📚 总结

高级搜索功能将大大提升文档管理的灵活性和效率：

**核心优势**:
- ✨ 支持复杂的多条件组合搜索
- ✨ 正则表达式支持高级用户
- ✨ 文件类型多选更便捷
- ✨ 后端处理保证高性能
- ✨ 良好的用户体验

**使用场景**:
- 🎯 查找特定类型的文档
- 🎯 按大小管理存储空间
- 🎯 查找特定时间段的文档
- 🎯 使用正则批量查找

---

**创建时间**: 2025-11-27 20:30
**状态**: 后端已完成，前端待实现
**优先级**: ⭐⭐⭐⭐⭐

