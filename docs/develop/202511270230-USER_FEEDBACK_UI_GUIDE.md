# 📝 用户反馈系统 UI 使用指南

## 🎯 功能概述

本指南说明如何使用新增的**用户反馈UI功能**，该功能已完全集成到 `index.html` 中，支持中英文切换。

---

## ✨ 新增功能

### 1. **整体评分反馈** ⭐

用户可以对AI的回答进行5星评分：

- ⭐⭐⭐⭐⭐ 非常好 (Excellent)
- ⭐⭐⭐⭐ 很好 (Very Good)  
- ⭐⭐⭐ 还行 (Good)
- ⭐⭐ 一般 (Fair)
- ⭐ 不好 (Poor)

### 2. **评论输入框** 💬

用户可以输入详细的反馈意见，帮助系统改进。

### 3. **单文档反馈** 📚

对每个参考文档，用户可以选择：

- 👍 **有帮助** - 文档对回答问题有帮助
- 👎 **无关** - 文档内容不相关

当用户点击"无关"时，会弹出对话框让用户输入原因。

### 4. **反馈状态显示** ✅

- 提交后显示感谢信息
- 已反馈的文档显示"已记录"状态
- 防止重复提交

---

## 🖥️ UI 界面说明

### 整体评分区域

```
┌─────────────────────────────────────────────┐
│ 💬 这个回答对您有帮助吗？                     │
├─────────────────────────────────────────────┤
│ [⭐⭐⭐⭐⭐ 非常好]  [⭐⭐⭐⭐ 很好]          │
│ [⭐⭐⭐ 还行]  [⭐⭐ 一般]  [⭐ 不好]        │
├─────────────────────────────────────────────┤
│ [文本框: 告诉我们更多您的想法...]            │
├─────────────────────────────────────────────┤
│           [提交反馈]                         │
└─────────────────────────────────────────────┘
```

### 文档反馈区域

```
┌─────────────────────────────────────────────┐
│ 📚 这些文档对回答问题有帮助吗？              │
├─────────────────────────────────────────────┤
│ 1. document1.pdf     [👍 有帮助] [👎 无关]  │
│ 2. document2.docx    [👍 有帮助] [👎 无关]  │
│ 3. document3.xlsx    [👍 有帮助] [👎 无关]  │
└─────────────────────────────────────────────┘
```

### 反馈原因对话框

当点击"👎 无关"时：

```
┌─────────────────────────────────────────────┐
│ 为什么这个文档没有帮助？                     │
├─────────────────────────────────────────────┤
│ [多行文本框: 请输入原因...]                  │
├─────────────────────────────────────────────┤
│              [关闭]  [提交反馈]              │
└─────────────────────────────────────────────┘
```

---

## 🌐 中英文切换

### 切换方法

点击右上角的语言切换按钮：
- 🌐 English (切换到英文)
- 🌐 中文 (切换到中文)

### 所有反馈文本均已翻译

| 中文 | 英文 |
|------|------|
| 这个回答对您有帮助吗？ | Was this answer helpful? |
| 非常好 | Excellent |
| 很好 | Very Good |
| 还行 | Good |
| 一般 | Fair |
| 不好 | Poor |
| 提交反馈 | Submit Feedback |
| 感谢您的反馈！ | Thank you for your feedback! |
| 有帮助 | Helpful |
| 无关 | Not Relevant |
| 已记录 | Recorded |
| 请先评分 | Please rate first |

---

## 🔧 技术实现

### React 状态管理

```javascript
// 反馈相关状态
const [feedbackRating, setFeedbackRating] = useState(0);
const [feedbackComment, setFeedbackComment] = useState('');
const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
const [documentFeedbacks, setDocumentFeedbacks] = useState({});
const [showReasonModal, setShowReasonModal] = useState(false);
const [currentFeedbackDoc, setCurrentFeedbackDoc] = useState(null);
```

### API 调用

#### 提交整体反馈
```javascript
const handleSubmitFeedback = async () => {
    const result = await api.submitOverallFeedback(
        answer.recordId,
        feedbackRating,
        feedbackComment
    );
    
    if (result.success) {
        setFeedbackSubmitted(true);
    }
};
```

#### 提交文档反馈
```javascript
// 有帮助
const handleDocumentHelpful = async (docName) => {
    const result = await api.submitDocumentFeedback(
        answer.recordId,
        docName,
        'HELPFUL',
        null
    );
};

// 无关 + 原因
const handleDocumentNotHelpful = (docName) => {
    setCurrentFeedbackDoc(docName);
    setShowReasonModal(true);
};

const submitDocumentNotHelpfulReason = async (reason) => {
    const result = await api.submitDocumentFeedback(
        answer.recordId,
        currentFeedbackDoc,
        'NOT_HELPFUL',
        reason
    );
};
```

---

## 🎨 样式定制

### CSS 类名

| 类名 | 用途 |
|------|------|
| `.feedback-section` | 整体反馈容器 |
| `.feedback-rating-buttons` | 评分按钮网格 |
| `.feedback-rating-button` | 单个评分按钮 |
| `.feedback-rating-button.selected` | 已选中的评分 |
| `.feedback-comment` | 评论输入框 |
| `.feedback-submit-btn` | 提交按钮 |
| `.feedback-success` | 成功提示 |
| `.document-feedback-section` | 文档反馈容器 |
| `.document-feedback-item` | 单个文档项 |
| `.document-feedback-btn` | 文档反馈按钮 |
| `.document-feedback-btn.liked` | 已点赞状态 |
| `.document-feedback-btn.disliked` | 已踩状态 |
| `.feedback-reason-modal` | 原因输入模态框 |

### 颜色方案

- **整体反馈区域**: 绿色系 `#e8f5e9` → `#c8e6c9`
- **文档反馈区域**: 蓝色系 `#e3f2fd` → `#bbdefb`
- **点赞按钮**: 绿色 `#4caf50`
- **踩按钮**: 红色 `#f44336`

---

## 📱 响应式设计

### 移动端适配

```css
@media (max-width: 768px) {
    .feedback-rating-buttons {
        grid-template-columns: 1fr; /* 单列显示 */
    }
    
    .document-feedback-item {
        flex-direction: column; /* 垂直排列 */
    }
    
    .document-feedback-buttons {
        width: 100%;
        justify-content: flex-start;
    }
}
```

---

## 🔄 使用流程

### 完整使用流程

```
1. 用户提问
   ↓
2. AI 生成回答
   ↓
3. 显示回答 + 参考文档
   ↓
4. 用户评分（1-5星）
   ↓
5. (可选) 用户输入评论
   ↓
6. 点击"提交反馈"
   ↓
7. 显示感谢信息
   ↓
8. 用户对每个文档评价
   - 点击"👍 有帮助"
   - 或点击"👎 无关" → 输入原因 → 提交
   ↓
9. 按钮显示"✅ 已记录"
   ↓
10. 反馈数据保存到后端
```

---

## 🧪 测试步骤

### 1. 启动应用

```bash
cd d:\Jetbrains\hackathon\ai-reviewer-base-file-rag
mvn spring-boot:run
```

### 2. 打开浏览器

访问: `http://localhost:8080`

### 3. 测试中文界面

1. 在"💬 智能问答"标签页提问
2. 等待AI回答
3. 滚动到页面底部
4. 看到反馈区域（绿色背景）
5. 点击评分按钮（1-5星）
6. (可选) 输入评论
7. 点击"提交反馈"
8. 验证显示"✅ 感谢您的反馈！"

### 4. 测试英文界面

1. 点击右上角"🌐 English"
2. 提问（英文或中文）
3. 等待回答
4. 验证反馈区域文字变为英文
   - "Was this answer helpful?"
   - "⭐⭐⭐⭐⭐ Excellent"
   - "Submit Feedback"
5. 完成反馈提交

### 5. 测试文档反馈

1. 查看"📚 这些文档对回答问题有帮助吗？"区域
2. 点击某个文档的"👍 有帮助"
3. 验证按钮变为"✅ 已记录"且变绿色
4. 点击另一个文档的"👎 无关"
5. 弹出对话框，输入原因
6. 点击"提交反馈"
7. 验证按钮变为"✅ 已记录"且变红色

### 6. 测试防重复提交

1. 尝试再次点击已反馈的文档按钮
2. 验证按钮被禁用（disabled）
3. 无法重复提交

---

## 📊 数据流向

```
前端 UI
  ↓ (用户点击提交)
React 状态更新
  ↓ (调用 API)
Axios HTTP 请求
  ↓
Spring Boot Controller
  ↓
QARecordService
  ↓
保存到 JSON 文件
  ↓
data/qa-records/{date}/{time}_{id}.json
```

---

## 🔍 调试技巧

### 1. 查看网络请求

打开浏览器开发者工具 (F12)
- Network 标签
- 筛选 `/api/feedback`
- 查看请求体和响应

### 2. 查看保存的反馈数据

```bash
# 查看最新的问答记录
cd d:\Jetbrains\hackathon\ai-reviewer-base-file-rag\data\qa-records
dir /s /b *.json | sort

# 查看最后一个文件内容
type [最新文件路径]
```

### 3. 控制台日志

打开浏览器控制台 (F12)
- Console 标签
- 查看提交反馈的日志
- 错误信息会显示在这里

---

## ⚠️ 注意事项

### 1. 评分必填

必须选择评分（1-5星）才能提交整体反馈，否则按钮禁用。

### 2. 评论可选

评论文本框是可选的，可以留空。

### 3. 文档反馈独立

每个文档的反馈独立提交，不需要等整体反馈完成。

### 4. recordId

确保后端返回的 `AIAnswer` 对象包含 `recordId` 字段，否则反馈功能无法正常工作。

### 5. API 端点

确保以下 API 端点已实现：
- `POST /api/feedback/overall`
- `POST /api/feedback/document`

---

## 🎯 后续优化建议

### 1. 加载动画

在提交反馈时显示加载动画。

### 2. 错误提示

网络错误时显示友好的错误提示。

### 3. 本地存储

使用 localStorage 缓存未提交的反馈，防止页面刷新丢失。

### 4. 统计展示

在"统计信息"页面显示反馈数据统计：
- 平均评分
- 反馈数量
- 最受好评的文档

### 5. 快捷键

支持键盘快捷键：
- `1-5` 数字键快速评分
- `Enter` 提交反馈
- `Esc` 关闭模态框

---

## 📚 相关文档

- [用户反馈系统完成报告](./202511270200-USER_FEEDBACK_SYSTEM.md)
- [语言切换指南](../../LANGUAGE_SWITCHING_GUIDE.md)
- [前端开发指南](../../README.md#前端开发)

---

## 🤝 贡献

如有问题或建议，请提交 Issue 或 Pull Request。

---

**最后更新**: 2025-11-27  
**作者**: AI Reviewer Team  
**状态**: ✅ 已完成并测试

🎉 **用户反馈UI已完全实现，支持中英文切换！**

