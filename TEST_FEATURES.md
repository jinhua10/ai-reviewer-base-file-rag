# 新功能测试指南

## 1. Markdown 图片渲染支持 ✅

### 修复内容：
**问题**：重构前支持图片显示，但重构后图片被渲染为代码块而不是图像

**修复**：
1. ✅ `MarkdownRenderer.jsx` - 添加图片语法解析 `![alt](url)`
2. ✅ `markdown-renderer.css` - 添加图片样式（圆角、阴影、悬停效果）
3. ✅ `KnowledgeQAService.ask()` - 修复非流式图片格式（移除反引号）
4. ✅ `KnowledgeQAService.askWithSessionDocuments()` - 修复会话图片格式
5. ✅ `KnowledgeQAService.buildPromptWithContext()` - 流式图片支持

### 测试步骤：
1. 启动后端服务
2. 确保有包含图片的文档在知识库中
3. **测试非流式 + RAG**：关闭流式模式，提问
4. **测试流式 + RAG**：开启流式模式，提问
5. 查看回复中图片是否正确显示

### 验证点：
- ✅ 图片以图像形式显示（不是代码块）
- ✅ 图片有圆角和阴影效果
- ✅ 鼠标悬停时图片轻微放大
- ✅ 图片自适应宽度（max-width: 100%）
- ✅ 图片支持懒加载（loading="lazy"）
- ✅ 图片显示 alt 文本（无障碍）

### 代码位置：
- **后端图片格式**：
  - 旧格式：`` `![desc](url)` ``（代码块）❌
  - 新格式：`![desc](url)`（纯 Markdown）✅
  
- **前端解析**：
  - `MarkdownRenderer.jsx` - 正则：`/!\[([^\]]*)\]\(([^)]+)\)/g`
  - `markdown-renderer.css` - `.markdown-renderer__image`

---

## 2. 输入框历史记录导航（方向键）✅

### 测试步骤：
1. 提交几个不同的问题（至少 3 个）
2. 在输入框中按 **↑** 键
3. 应该显示最近的问题
4. 继续按 **↑** 翻看更早的历史
5. 按 **↓** 键向下翻
6. 按到底部应恢复为空或当前输入

### 验证点：
- ✅ 光标在第一行时，按 ↑ 显示历史记录
- ✅ 光标在最后一行时，按 ↓ 向下翻历史
- ✅ 多行输入时，方向键正常移动光标（不干扰历史导航）
- ✅ 历史记录保存到 localStorage（刷新页面后依然存在）
- ✅ 最多保存 50 条历史记录
- ✅ 提交问题后自动添加到历史顶部
- ✅ 重复问题不会重复添加

### 行为参照：
模仿 **VSCode Copilot 聊天输入框**：
- ↑ 向上翻历史（从新到旧）
- ↓ 向下翻历史（从旧到新）
- 回到底部恢复用户正在输入的内容

### 代码位置：
- `QuestionInput.jsx`：
  - `handleKeyDown()` - 方向键事件处理
  - `useEffect()` - 从 localStorage 加载历史
  - `handleSubmit()` - 保存历史到 localStorage

---

## 技术细节

### 历史记录存储
```javascript
// localStorage key
HISTORY_STORAGE_KEY = 'qa_question_history'

// 数据格式：
["最新问题", "次新问题", "更早问题", ...]

// 最大容量：
MAX_HISTORY_SIZE = 50
```

### 流式图片支持
```java
// KnowledgeQAService.buildPromptWithContext()
// 1. 遍历文档收集图片
for (Document doc : documents) {
    List<ImageInfo> docImages = imageStorageService.listImages(doc.getTitle());
    // 2. 生成 Markdown 格式
    imageContext.append(String.format("![%s](%s)\n", desc, url));
}
// 3. 传递给 buildEnhancedPrompt()
```

---

## 已知限制

1. **图片支持**：
   - 仅在开启 RAG 模式时有效
   - 图片数量受 `maxImagesPerDoc` 配置限制

2. **历史记录**：
   - 仅存储在浏览器本地（不同浏览器不共享）
   - 清除浏览器数据会丢失历史

---

## 快速验证命令

### 检查后端编译
```powershell
mvn compile -DskipTests
```

### 检查前端语法
```powershell
cd UI
npm run build
```

### 查看历史记录
```javascript
// 浏览器控制台
localStorage.getItem('qa_question_history')
```

### 清除历史记录
```javascript
// 浏览器控制台
localStorage.removeItem('qa_question_history')
```
