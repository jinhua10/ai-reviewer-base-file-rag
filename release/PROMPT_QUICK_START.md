# Prompt 提示词配置功能 - 快速使用指南

## 🎯 功能说明

通过配置文件自定义 AI 回答的提示词模板，无需修改代码即可调整回答风格。

## 📝 配置位置

**文件**: `release/config/application.yml`

**路径**: `knowledge.qa.llm.prompt-template`

## ⚙️ 配置示例

```yaml
knowledge:
  qa:
    llm:
      prompt-template: |
        你是一个专业的知识助手。请基于文档内容回答用户问题。
        
        # 回答要求
        1. 必须基于文档内容回答，不要编造信息
        2. 如果文档中没有相关信息，明确告知用户
        3. 回答要清晰、准确、有条理
        4. 可以引用文档名称作为信息来源
        5. 保持专业友好的语气
        
        # 用户问题
        {question}
        
        # 相关文档
        {context}
        
        # 请提供你的回答：
```

## 🔧 占位符说明

- `{question}` - 自动替换为用户提出的问题
- `{context}` - 自动替换为检索到的相关文档内容

⚠️ **注意**: 这两个占位符必须包含在模板中！

## 🚀 使用步骤

### 1️⃣ 修改配置

编辑 `release/config/application.yml`，找到 `prompt-template` 配置项，修改为你想要的提示词。

### 2️⃣ 重启应用

```bash
# Windows
stop.bat
start.bat
```

### 3️⃣ 测试效果

访问 http://localhost:8080，提问并观察 AI 回答风格。

## 📚 常用模板示例

### 客服助手风格

```yaml
prompt-template: |
  你是一位热情友好的客服助手。请根据产品文档回答客户问题。
  
  客户问题：{question}
  相关文档：{context}
  
  请提供专业且友好的回答：
```

### 技术支持风格

```yaml
prompt-template: |
  你是专业的技术支持助手。基于技术文档回答问题。
  
  问题：{question}
  技术文档：{context}
  
  技术回答：
```

### 简洁风格

```yaml
prompt-template: |
  基于以下文档简洁回答问题。
  
  问题：{question}
  文档：{context}
  
  回答：
```

## ❓ 常见问题

**Q: 修改后多久生效？**  
A: 需要重启应用才能生效。

**Q: 能删除占位符吗？**  
A: 不能。`{question}` 和 `{context}` 必须存在。

**Q: 提示词能写多长？**  
A: 建议控制在 500 字符以内。

**Q: 如何恢复默认？**  
A: 使用上面"配置示例"中的默认模板。

## 📖 详细文档

更多信息请查看：
- 中文文档: `docs/PROMPT_TEMPLATE_CONFIG.md`
- English Doc: `release/PROMPT_TEMPLATE_CONFIG_EN.md`
- 实现报告: `md/Prompt配置功能实现报告.md`

## 💡 提示

- 提示词越具体，AI 回答质量越好
- 使用分点列举让 AI 更容易理解要求
- 可以通过提示词控制回答的语气和风格
- 建议保存多个版本的提示词模板以便对比测试

---

**版本**: v1.0  
**日期**: 2025-11-25

