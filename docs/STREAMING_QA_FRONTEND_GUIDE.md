# 流式问答前端组件使用指南
# Streaming QA Frontend Components Usage Guide

> 创建日期: 2025-12-09  
> 版本: 1.0  
> 状态: ✅ 已完成

---

## 📦 组件清单 (Component List)

### JSX 组件 (JSX Components)

1. **StreamingQA.jsx** - 主组件 (Main Component)
   - 路径: `src/main/resources/static/js/components/streaming/StreamingQA.jsx`
   - 功能: 问题输入、会话管理、双轨响应协调

2. **HOPEAnswerCard.jsx** - HOPE 答案卡片 (HOPE Answer Card)
   - 路径: `src/main/resources/static/js/components/streaming/HOPEAnswerCard.jsx`
   - 功能: 显示 HOPE 快速答案、置信度、来源层、响应时间

3. **LLMStreamingAnswer.jsx** - LLM 流式答案 (LLM Streaming Answer)
   - 路径: `src/main/resources/static/js/components/streaming/LLMStreamingAnswer.jsx`
   - 功能: 实时显示 LLM 流式生成、Markdown 渲染、代码高亮

4. **ComparisonFeedback.jsx** - 对比反馈组件 (Comparison Feedback)
   - 路径: `src/main/resources/static/js/components/streaming/ComparisonFeedback.jsx`
   - 功能: 答案对比、用户反馈收集、差异分析

### CSS 样式文件 (CSS Style Files)

1. **streaming-qa.css** - 主容器样式
   - 路径: `src/main/resources/static/assets/css/streaming-qa.css`

2. **hope-answer-card.css** - HOPE 卡片样式
   - 路径: `src/main/resources/static/assets/css/hope-answer-card.css`

3. **llm-streaming-answer.css** - LLM 答案样式
   - 路径: `src/main/resources/static/assets/css/llm-streaming-answer.css`

4. **comparison-feedback.css** - 对比反馈样式
   - 路径: `src/main/resources/static/assets/css/comparison-feedback.css`

---

## 🌐 国际化支持 (Internationalization)

所有文本已添加到 `lang.js` 字典中：

### 中文翻译 (Chinese Translations)

```javascript
// 流式问答 - 双轨响应
streamingTitle: '智能流式问答',
streamingHopeAnswer: 'HOPE 快速答案',
streamingLlmAnswer: 'LLM 详细回答',
streamingHopeLoading: '正在从知识库查询...',
// ... 更多翻译
```

### 英文翻译 (English Translations)

```javascript
// Streaming QA - Dual-Track Response
streamingTitle: 'Intelligent Streaming Q&A',
streamingHopeAnswer: 'HOPE Quick Answer',
streamingLlmAnswer: 'LLM Detailed Response',
streamingHopeLoading: 'Querying knowledge base...',
// ... more translations
```

---

## 🚀 集成步骤 (Integration Steps)

### 1. 在 HTML 中引入资源

在 `index.html` 或主 HTML 文件中添加：

```html
<!-- CSS 样式文件 -->
<link rel="stylesheet" href="/assets/css/streaming-qa.css">
<link rel="stylesheet" href="/assets/css/hope-answer-card.css">
<link rel="stylesheet" href="/assets/css/llm-streaming-answer.css">
<link rel="stylesheet" href="/assets/css/comparison-feedback.css">

<!-- JSX 组件（在 Babel 转译后） -->
<script src="/js/components/streaming/HOPEAnswerCard.jsx" type="text/babel"></script>
<script src="/js/components/streaming/LLMStreamingAnswer.jsx" type="text/babel"></script>
<script src="/js/components/streaming/ComparisonFeedback.jsx" type="text/babel"></script>
<script src="/js/components/streaming/StreamingQA.jsx" type="text/babel"></script>
```

### 2. 在 React 应用中使用

```javascript
// 在 App.jsx 或其他父组件中
function App() {
    const [activeTab, setActiveTab] = useState('streaming-qa');

    return React.createElement('div', { className: 'app-container' },
        // 标签页切换
        React.createElement('div', { className: 'tabs' },
            React.createElement('button', {
                onClick: () => setActiveTab('streaming-qa')
            }, '流式问答')
        ),

        // 渲染组件
        activeTab === 'streaming-qa' && React.createElement(StreamingQA, null)
    );
}
```

### 3. 独立使用（不依赖 App）

```html
<!-- 在任意 HTML 页面中 -->
<div id="streaming-qa-root"></div>

<script type="text/babel">
    const root = ReactDOM.createRoot(document.getElementById('streaming-qa-root'));
    root.render(React.createElement(StreamingQA));
</script>
```

---

## 🎨 样式特性 (Style Features)

### 1. 深色模式支持 (Dark Mode Support)

所有组件自动适配系统深色模式：

```css
@media (prefers-color-scheme: dark) {
    .hope-answer-card {
        background: linear-gradient(135deg, #2a2412 0%, #1e1e1e 100%);
        border-color: #b8860b;
    }
}
```

### 2. 响应式设计 (Responsive Design)

移动端自动切换为单列布局：

```css
@media (max-width: 1024px) {
    .streaming-response-container {
        grid-template-columns: 1fr;
    }
}
```

### 3. 动画效果 (Animations)

- ✅ 加载动画 (Loading spinner)
- ✅ 流式输入动画 (Typing dots)
- ✅ 进度条动画 (Progress bar)
- ✅ 淡入效果 (Fade in)

---

## 📡 API 交互 (API Interaction)

### 1. 发起流式请求

```javascript
POST /api/qa/stream
Content-Type: application/json
Accept-Language: zh-CN 或 en-US

Body:
{
    "question": "什么是Docker？",
    "userId": "web-user-123456"
}

Response:
{
    "sessionId": "uuid-xxx",
    "question": "什么是Docker？",
    "hopeAnswer": {
        "answer": "Docker 是一个容器化平台...",
        "confidence": 0.95,
        "source": "HOPE_PERMANENT",
        "canDirectAnswer": true,
        "responseTime": 150
    },
    "sseUrl": "/api/qa/stream/uuid-xxx"
}
```

### 2. 连接 SSE 流式输出

```javascript
const eventSource = new EventSource('/api/qa/stream/' + sessionId);

// 接收文本块
eventSource.addEventListener('chunk', (event) => {
    const chunk = event.data;
    setLlmAnswer(prev => prev + chunk);
});

// 会话完成
eventSource.addEventListener('complete', (event) => {
    setSessionStatus('completed');
    eventSource.close();
});

// 错误处理
eventSource.addEventListener('error', (event) => {
    setError('连接错误');
    eventSource.close();
});
```

### 3. 提交对比反馈

```javascript
POST /api/qa/stream/feedback
Content-Type: application/json
Accept-Language: zh-CN 或 en-US

Body:
{
    "sessionId": "uuid-xxx",
    "hopeAnswerId": "hope-answer-id",
    "question": "什么是Docker？",
    "choice": "hope" | "llm" | "both" | "neither",
    "comment": "HOPE 答案更准确",
    "timestamp": "2025-12-09T10:30:00Z"
}
```

---

## 🎯 使用示例 (Usage Examples)

### 示例 1: 基础流式问答

```javascript
// 用户输入问题
setQuestion('什么是 Docker？');

// 点击提问按钮
handleAsk();

// 1. HOPE 快速响应（<300ms）
// 显示：置信度 95%、来源层、响应时间

// 2. LLM 流式生成（实时）
// 实时显示：生成文本、进度、耗时
```

### 示例 2: 答案对比

```javascript
// 流式生成完成后
// 1. 显示对比按钮
setShowComparison(true);

// 2. 切换视图模式
setViewMode('both'); // 并排显示 HOPE 和 LLM

// 3. 用户选择更好的答案
setSelectedChoice('hope'); // HOPE 答案更准确

// 4. 提交反馈
handleSubmit();
```

### 示例 3: 错误重试

```javascript
// LLM 生成失败
setError('连接超时');

// 显示重试按钮
<button onClick={handleRetry}>重试</button>

// 重新连接 SSE
connectSSE(`/api/qa/stream/${sessionId}`);
```

---

## 🔧 自定义配置 (Customization)

### 1. 修改颜色主题

在 CSS 文件中修改变量：

```css
/* hope-answer-card.css */
.hope-answer-card {
    background: linear-gradient(135deg, #fff9e6 0%, #fff 100%);
    border: 2px solid #ffd700; /* 修改边框颜色 */
}
```

### 2. 调整响应速度阈值

在 `application.yml` 中配置：

```yaml
knowledge:
  qa:
    streaming:
      hope-query-timeout: 300  # HOPE 查询超时（毫秒）
      llm-streaming-timeout: 300000  # LLM 流式超时（毫秒）
```

### 3. 自定义翻译文本

在 `lang.js` 中修改：

```javascript
const translations = {
    zh: {
        streamingTitle: '智能流式问答', // 修改标题
        // ...
    },
    en: {
        streamingTitle: 'AI Streaming Q&A', // 修改标题
        // ...
    }
};
```

---

## 📊 性能指标 (Performance Metrics)

| 指标 | 目标 | 说明 |
|------|------|------|
| HOPE 响应时间 | <300ms | 从发起请求到显示 HOPE 答案 |
| LLM TTFB | <1s | 从发起请求到收到第一个文本块 |
| SSE 连接延迟 | <100ms | 每个文本块的传输延迟 |
| 页面渲染 | <50ms | React 组件重新渲染耗时 |

---

## ⚠️ 注意事项 (Important Notes)

### 1. 依赖项

确保已引入以下库：

```html
<!-- React & ReactDOM -->
<script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
<script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>

<!-- Babel (for JSX) -->
<script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>

<!-- Marked (for Markdown) -->
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>

<!-- Highlight.js (for code highlighting) -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js"></script>
```

### 2. SSE 连接管理

```javascript
// ❌ 不要忘记关闭连接
useEffect(() => {
    return () => {
        if (eventSourceRef.current) {
            eventSourceRef.current.close(); // 组件卸载时关闭
        }
    };
}, []);
```

### 3. 语言切换

组件会自动读取 `window.LanguageModule.getCurrentLanguage()`：

```javascript
headers: {
    'Accept-Language': window.LanguageModule.getCurrentLanguage()
}
```

---

## 🐛 故障排查 (Troubleshooting)

### 问题 1: HOPE 答案不显示

**可能原因:**
- HOPE 服务未启动
- 知识库为空

**解决方法:**
```bash
# 检查 HOPE 配置
curl http://localhost:8080/api/qa/stream/health

# 查看日志
tail -f logs/app-info.log | grep HOPE
```

### 问题 2: LLM 流式不工作

**可能原因:**
- SSE 连接失败
- LLM 服务未响应

**解决方法:**
```javascript
// 检查浏览器控制台
console.log('SSE URL:', sseUrl);

// 测试 SSE 连接
curl -N http://localhost:8080/api/qa/stream/{sessionId}
```

### 问题 3: 样式混乱

**可能原因:**
- CSS 文件未加载
- 样式冲突

**解决方法:**
```html
<!-- 检查 CSS 加载顺序 -->
<link rel="stylesheet" href="/assets/css/streaming-qa.css">
<!-- 确保在其他样式之后 -->
```

---

## 📚 相关文档 (Related Documents)

- [Phase -1 完成报告](PHASE_MINUS_1_FINAL_REPORT.md) - 完整实施报告
- [层次化语义 RAG 设计](HIERARCHICAL_SEMANTIC_RAG.md) - 系统架构设计
- [后端 API 文档](../README.md#api-endpoints) - REST API 接口说明

---

## 🎉 完成状态 (Completion Status)

- ✅ 4 个 JSX 组件已创建
- ✅ 4 个 CSS 样式文件已创建
- ✅ 完整的国际化支持（中英文）
- ✅ 深色模式支持
- ✅ 响应式布局
- ✅ 动画效果
- ✅ 错误处理
- ✅ SSE 连接管理

**总完成度**: **100%** ✅

---

**创建者**: GitHub Copilot  
**创建日期**: 2025-12-09  
**更新日期**: 2025-12-09

