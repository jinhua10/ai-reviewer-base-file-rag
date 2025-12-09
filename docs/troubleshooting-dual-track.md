# HOPE 流式双轨 - 故障排查指南
# HOPE Streaming Dual-Track - Troubleshooting Guide

> **文档**: troubleshooting-dual-track.md  
> **用途**: 常见问题和解决方案

---

## 📖 目录

1. [编译和启动问题](#编译和启动问题)
2. [API 接口问题](#api-接口问题)
3. [前端组件问题](#前端组件问题)
4. [SSE 连接问题](#sse-连接问题)
5. [HOPE 相关问题](#hope-相关问题)
6. [性能问题](#性能问题)
7. [国际化问题](#国际化问题)

---

## 🔧 编译和启动问题

### 问题 1.1: 编译失败 - 找不到 StreamMessage 类

**错误信息**:
```
Cannot resolve symbol 'StreamMessage'
```

**原因**: 新增的类未被正确识别

**解决方案**:
```bash
# 1. 清理并重新编译
mvn clean compile

# 2. 如果使用 IDE，刷新 Maven 项目
# IntelliJ IDEA: 右键项目 -> Maven -> Reload Project
```

---

### 问题 1.2: 启动失败 - 端口被占用

**错误信息**:
```
Port 8080 is already in use
```

**解决方案**:

**Windows**:
```powershell
# 查找占用端口的进程
netstat -ano | findstr :8080

# 杀死进程（替换 PID）
taskkill /PID <PID> /F

# 或者修改 application.yml 中的端口
server:
  port: 8081
```

**Linux/Mac**:
```bash
# 查找并杀死进程
lsof -ti:8080 | xargs kill -9
```

---

### 问题 1.3: 启动失败 - 国际化文件加载错误

**错误信息**:
```
Failed to load i18n files
```

**解决方案**:
1. 检查文件路径：`src/main/resources/i18n/zh/` 和 `i18n/en/`
2. 确认文件命名正确：`zh-*.yml` 和 `en-*.yml`
3. 检查 YAML 语法是否正确

---

## 🌐 API 接口问题

### 问题 2.1: 双轨 API 返回 404

**错误信息**:
```
GET /api/qa/stream/dual-track - 404 Not Found
```

**原因**: 
- Controller 未被扫描
- URL 路径错误

**解决方案**:
```bash
# 1. 检查 Controller 注解
@RestController
@RequestMapping("/api/qa/stream")

# 2. 确认应用已启动
curl http://localhost:8080/actuator/health

# 3. 检查日志中的映射信息
grep "Mapped" logs/app-*.log
```

---

### 问题 2.2: API 返回 500 - NullPointerException

**错误信息**:
```
java.lang.NullPointerException at StreamingQAController.dualTrackStreaming
```

**原因**: 
- HybridStreamingService 未注入
- HOPE 服务未初始化

**解决方案**:
1. 检查 Spring 依赖注入是否正确
2. 确认 HOPE 配置已启用
3. 查看详细堆栈信息定位问题

---

### 问题 2.3: 反馈 API 失败

**错误信息**:
```
POST /api/feedback/dual-track - 500 Internal Server Error
```

**原因**: 反馈接口后端未实现

**解决方案**:
这是预期行为。反馈 API 在前端已做容错处理：
```javascript
// api.js 中
catch (error) {
    console.error('提交双轨反馈失败:', error);
    // 返回成功避免影响用户体验
    return { success: true, message: 'Feedback received (client-side)' };
}
```

**如需实现后端**，创建以下 Controller：
```java
@PostMapping("/feedback/dual-track")
public ResponseEntity<Map<String, Object>> submitDualTrackFeedback(
    @RequestBody DualTrackFeedbackRequest request) {
    
    // 处理反馈逻辑
    // ...
    
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Feedback received"
    ));
}
```

---

## 💻 前端组件问题

### 问题 3.1: DualTrackAnswer 组件未定义

**错误信息**:
```javascript
Uncaught ReferenceError: DualTrackAnswer is not defined
```

**原因**: JSX 文件未加载或编译失败

**解决方案**:
1. 检查 `index.html` 中的引入顺序：
```html
<!-- Babel 必须在 JSX 之前加载 -->
<script src="js/lib/babel.min.js"></script>

<!-- JSX 组件 -->
<script type="text/babel" src="js/components/DualTrackAnswer.jsx"></script>
```

2. 清除浏览器缓存（Ctrl + Shift + Delete）

3. 检查浏览器控制台是否有 Babel 编译错误

---

### 问题 3.2: 样式不显示 - 无渐变背景

**症状**: 组件显示但样式错误，无紫色/粉色渐变

**原因**: CSS 文件未加载

**解决方案**:
1. 检查 `index.html` 中是否引入了 CSS：
```html
<link rel="stylesheet" href="assets/css/dual-track-answer.css">
```

2. 检查文件路径是否正确：
```
src/main/resources/static/assets/css/dual-track-answer.css
```

3. 清除浏览器缓存并强制刷新（Ctrl + F5）

---

### 问题 3.3: 国际化文本不显示

**症状**: 界面显示 `undefined` 或 key 值

**原因**: 翻译未加载或 key 不存在

**解决方案**:
1. 检查 `lang.js` 中是否有对应的 key
2. 确认语言切换正常：
```javascript
console.log(window.LanguageModule.getCurrentLanguage());
```

3. 使用默认值：
```jsx
{t('chooseAnswer') || '请选择您更满意的答案：'}
```

---

## 🔌 SSE 连接问题

### 问题 4.1: EventSource 连接立即关闭

**症状**: `onerror` 立即触发，无数据接收

**原因**:
- 后端未启动
- URL 错误
- CORS 问题

**解决方案**:
```javascript
// 1. 检查 URL
const url = `/api/qa/stream/dual-track?question=${encodeURIComponent(question)}`;
console.log('SSE URL:', url);

// 2. 测试 URL 是否可访问
fetch(url, { method: 'GET' })
    .then(r => console.log('Status:', r.status))
    .catch(e => console.error('Error:', e));

// 3. 检查 CORS 设置
// Controller 应有 @CrossOrigin 注解
```

---

### 问题 4.2: 只收到 HOPE 答案，无 LLM 流

**症状**: `event: hope` 收到，但没有 `event: llm`

**原因**:
- StreamingSession 未正确创建
- LLM 流式生成失败

**排查步骤**:
1. 查看后端日志：
```bash
grep "Streaming" logs/app-*.log
```

2. 检查 HybridStreamingService 是否正常工作

3. 确认 LLM 配置正确

---

### 问题 4.3: SSE 连接频繁断开

**症状**: 连接几秒后自动断开

**原因**:
- 超时设置过短
- 网络不稳定
- 代理/负载均衡器限制

**解决方案**:
```java
// 后端：增加超时时间
SseEmitter emitter = new SseEmitter(60000L); // 60 秒

// 前端：添加重连逻辑
const eventSource = new EventSource(url);
eventSource.onerror = () => {
    setTimeout(() => {
        // 重新连接
        connectSSE();
    }, 3000);
};
```

---

## 💡 HOPE 相关问题

### 问题 5.1: HOPE 始终返回 "暂无答案"

**症状**: `hopeAnswer` 为空或无内容

**原因**:
- HOPE 系统未启用
- 问题在 HOPE 中无匹配
- 置信度过低

**排查步骤**:
1. 检查 HOPE 配置：
```yaml
knowledge:
  qa:
    hope:
      enabled: true
```

2. 查看 HOPE 仪表盘：
```
http://localhost:8080/#hope
```

3. 检查 HOPE 日志：
```bash
grep "HOPE" logs/app-*.log
```

4. 尝试训练问题：
```
什么是 RAG？
什么是向量数据库？
```

---

### 问题 5.2: HOPE 响应时间 > 300ms

**症状**: HOPE 触发超时警告

**原因**:
- 查询逻辑复杂
- 数据量大
- 服务器性能不足

**优化方案**:
1. 添加缓存
2. 优化查询算法
3. 增加服务器资源

---

### 问题 5.3: HOPE 置信度异常低

**症状**: `confidence < 0.5`，但答案正确

**原因**: 评分机制需要调整

**解决方案**:
调整 HOPE 配置中的阈值：
```yaml
knowledge:
  qa:
    hope:
      confidence-threshold: 0.7  # 降低阈值
```

---

## ⚡ 性能问题

### 问题 6.1: 页面加载慢

**症状**: 首次加载需要 > 5 秒

**排查**:
1. 检查网络请求（F12 -> Network）
2. 查看是否有大文件阻塞
3. 检查资源是否被缓存

**优化方案**:
```html
<!-- 启用浏览器缓存 -->
<meta http-equiv="Cache-Control" content="max-age=31536000">

<!-- 压缩 JS/CSS -->
<script src="js/lib/react.production.min.js"></script>
```

---

### 问题 6.2: 流式文本渲染卡顿

**症状**: LLM 文本追加时界面卡顿

**原因**: 频繁 DOM 更新

**优化方案**:
```javascript
// 使用 React.memo 优化
const DualTrackAnswer = React.memo(({ question, ... }) => {
    // ...
});

// 减少不必要的 re-render
const [llmAnswer, setLlmAnswer] = useState('');
// 使用函数式更新
setLlmAnswer(prev => prev + chunk);
```

---

### 问题 6.3: 内存占用过高

**症状**: 长时间使用后浏览器内存 > 500MB

**原因**: EventSource 未正确关闭

**解决方案**:
```javascript
// 确保组件卸载时清理
useEffect(() => {
    const eventSource = new EventSource(url);
    
    return () => {
        eventSource.close(); // 关键：清理连接
    };
}, [question]);
```

---

## 🌍 国际化问题

### 问题 7.1: 切换语言后部分文本未更新

**症状**: 部分界面仍显示旧语言

**原因**: 组件未重新渲染

**解决方案**:
```javascript
// 使用 useTranslation hook
const { t } = window.LanguageModule.useTranslation();

// 确保组件监听语言变化
useEffect(() => {
    // 强制刷新
    forceUpdate();
}, [window.LanguageModule.getCurrentLanguage()]);
```

---

### 问题 7.2: 新增翻译不生效

**症状**: 新添加的 key 显示为 key 值

**原因**: 翻译未加载或 key 错误

**解决方案**:
1. 检查 `lang.js` 中是否正确添加
2. 清除缓存并刷新
3. 检查 key 拼写是否正确

---

## 🆘 紧急故障处理

### 系统完全无法启动

**应急步骤**:
1. 回退到上一个可用版本：
```bash
git log --oneline
git checkout <commit-hash>
```

2. 检查日志文件：
```bash
tail -f logs/app-*.log
```

3. 联系技术支持

---

### 数据丢失或损坏

**应急步骤**:
1. 立即停止服务
2. 备份当前数据
3. 从备份恢复
4. 检查数据一致性

---

## 📞 获取帮助

### 查看日志
```bash
# 查看最新日志
tail -f logs/app-info.log

# 搜索错误
grep "ERROR" logs/app-*.log

# 搜索 HOPE 相关
grep "HOPE" logs/app-*.log
```

### 启用调试模式
```yaml
# application.yml
logging:
  level:
    top.yumbo.ai.rag: DEBUG
```

### 提交 Issue
如果问题无法解决，提交 Issue 时请包含：
1. 错误信息和堆栈
2. 日志文件相关片段
3. 重现步骤
4. 环境信息（OS、JDK、浏览器）

---

**最后更新**: 2025-12-10  
**文档版本**: v1.0

