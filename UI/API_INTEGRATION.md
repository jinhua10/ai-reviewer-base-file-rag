# 智能问答前后端联调说明

## 已完成的工作

### 1. 后端API接口（Spring Boot）

位置：`src/main/java/top/yumbo/ai/rag/spring/boot/controller/KnowledgeQAController.java`

#### 已对接的接口：

- ✅ `GET /api/qa/statistics` - 获取知识库统计信息
  - 返回：文档总数、已索引数、未索引数、索引进度等
  
- ✅ `GET /api/qa/health` - 健康检查
  - 返回：系统运行状态

#### 可用但未集成到Shell的接口：

- `POST /api/qa/ask` - 提问接口
- `GET /api/qa/search` - 搜索文档
- `POST /api/qa/rebuild` - 重建知识库
- `POST /api/qa/incremental-index` - 增量索引

### 2. 前端数据适配器

位置：`UI/src/adapters/PageDataAdapter.jsx`

#### 已实现的功能：

```javascript
// 获取QA页面数据（已对接后端）
const { stats, systemStatus, loading, error } = useQAPageData();

// 可用的API函数
await askQuestion(question, hopeSessionId);
await searchDocuments(query, limit);
await rebuildKnowledgeBase();
await incrementalIndex();
```

### 3. QA Shell组件

位置：`UI/src/components/theme/shells/bubble/QAShell.jsx`

#### 显示的数据：

- 📚 知识库文档总数
- ✅ 已索引文档数
- 📊 索引进度百分比
- 🎯 系统运行状态

## 测试步骤

### 1. 启动后端服务

```bash
# 确保后端Spring Boot应用运行在 http://localhost:8080
cd d:\Jetbrains\hackathon\ai-reviewer-base-file-rag
mvn spring-boot:run
```

### 2. 启动前端开发服务器

```bash
cd UI
npm run dev
```

### 3. 访问QA页面

1. 打开浏览器访问 `http://localhost:3000`
2. 切换到"智能问答"页面
3. 查看是否显示真实的统计数据

### 4. 验证数据

打开浏览器控制台（F12），应该看到：

```
API Call: GET http://localhost:8080/api/qa/statistics
API Response: {documentCount: xxx, indexedDocumentCount: xxx, ...}
API Call: GET http://localhost:8080/api/qa/health  
API Response: {status: "系统正常运行", ...}
```

## API数据格式

### 统计信息响应

```json
{
  "documentCount": 856,
  "indexedDocumentCount": 789,
  "unindexedCount": 67,
  "indexProgress": 92,
  "needsIndexing": true,
  "message": "有67个文档尚未索引，建议执行增量索引"
}
```

### 健康检查响应

```json
{
  "status": "系统正常运行",
  "message": "AI审查系统运行正常"
}
```

## 故障排查

### 问题1：无法连接后端

**现象：** 控制台显示 `Failed to fetch` 或 `Network Error`

**解决：**
1. 确认后端服务运行在 `http://localhost:8080`
2. 检查CORS配置是否允许前端域名
3. 查看后端控制台是否有错误日志

### 问题2：显示加载失败

**现象：** QA页面显示 "加载失败"

**解决：**
1. 打开浏览器控制台查看错误详情
2. 检查API返回的数据格式是否正确
3. 确认 `PageDataAdapter.jsx` 中的字段映射

### 问题3：数据不更新

**现象：** 后端数据变化，前端不更新

**解决：**
1. 清除浏览器缓存
2. 刷新页面（Ctrl+F5）
3. 检查React组件是否正确触发重新渲染

## 配置选项

### 修改API基础URL

编辑 `UI/.env` 文件：

```bash
# 开发环境
REACT_APP_API_BASE_URL=http://localhost:8080/api

# 生产环境
REACT_APP_API_BASE_URL=https://api.yourapp.com/api
```

## 下一步扩展

### 1. 添加问答功能

在Shell组件中添加问答输入框：

```jsx
import { askQuestion } from '../../../../adapters/PageDataAdapter';

function QAShell() {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState(null);
  
  const handleAsk = async () => {
    const result = await askQuestion(question);
    setAnswer(result);
  };
  
  // 渲染问答界面
}
```

### 2. 添加文档搜索

```jsx
import { searchDocuments } from '../../../../adapters/PageDataAdapter';

function DocumentsShell() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  
  const handleSearch = async () => {
    const docs = await searchDocuments(query);
    setResults(docs.documents);
  };
  
  // 渲染搜索结果
}
```

### 3. 添加知识库管理

```jsx
import { rebuildKnowledgeBase, incrementalIndex } from '../../../../adapters/PageDataAdapter';

function SettingsShell() {
  const handleRebuild = async () => {
    await rebuildKnowledgeBase();
    // 显示成功提示
  };
  
  const handleIncremental = async () => {
    await incrementalIndex();
    // 显示成功提示
  };
  
  // 渲染管理界面
}
```

## 重要提示

✅ **单点修改** - 所有API调用都在 `PageDataAdapter.jsx` 中，后续修改只需改这一个文件

✅ **自动更新** - 修改Adapter后，所有使用该数据的Shell组件自动更新

✅ **主题独立** - 无论是气泡主题还是其他主题，都使用相同的数据源

✅ **错误处理** - 所有API调用都有统一的错误处理和日志输出

## 联系人

如有问题，请查看：
- 后端API文档：Controller类中的注释
- 前端适配器：`UI/src/adapters/README.md`
- Shell组件：各主题的Shell组件实现
