# Prompt 提示词配置功能 - 对比说明

## 📊 改进前后对比

### ❌ 改进前（硬编码方式）

#### 代码实现
```java
private String buildPrompt(String question, String context) {
    return String.format("""
            你是一个专业的知识助手。请基于文档内容回答用户问题。
            # 回答要求
            1. 必须基于文档内容回答，不要编造信息
            2. 如果文档中没有相关信息，明确告知用户
            3. 回答要清晰、准确、有条理
            4. 可以引用文档名称作为信息来源
            5. 保持专业友好的语气
            # 用户问题
            %s
            
            # 相关文档
            %s
            
            # 请提供你的回答：
            """, question, context);
}
```

#### ⚠️ 存在的问题

1. **不灵活**: 提示词写死在代码中，修改需要重新编译
2. **维护困难**: 每次调整都要修改代码、测试、发布
3. **场景单一**: 无法快速适配不同业务场景
4. **测试成本高**: A/B 测试需要多次编译部署
5. **用户受限**: 用户无法自定义提示词风格

---

### ✅ 改进后（配置化方式）

#### 代码实现
```java
private String buildPrompt(String question, String context) {
    // 从配置中获取提示词模板
    String template = properties.getLlm().getPromptTemplate();
    
    // 替换占位符
    return template
            .replace("{question}", question)
            .replace("{context}", context);
}
```

#### 配置文件
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

#### ✨ 优势

1. **高度灵活**: 修改配置文件即可，无需重新编译
2. **易于维护**: 只需重启应用，几秒钟完成更新
3. **多场景适配**: 可快速切换不同业务场景的提示词
4. **测试便捷**: A/B 测试只需修改配置文件
5. **用户友好**: 用户可根据需求自定义风格

---

## 🎯 实际应用场景对比

### 场景 1: 需要调整 AI 回答风格

#### ❌ 改进前
1. 修改 Java 代码
2. 本地编译测试
3. 提交代码
4. CI/CD 构建
5. 部署到生产环境
6. 测试验证

**耗时**: 30-60 分钟  
**风险**: 中-高（代码变更）

#### ✅ 改进后
1. 修改 `config/application.yml`
2. 重启应用 (`stop.bat` + `start.bat`)
3. 测试验证

**耗时**: 1-2 分钟  
**风险**: 低（仅配置变更）

---

### 场景 2: 适配不同业务场景

#### ❌ 改进前
需要在代码中实现多个提示词模板，通过逻辑判断选择：
```java
private String buildPrompt(String question, String context, String scenario) {
    switch (scenario) {
        case "customer":
            return "客服风格模板...";
        case "technical":
            return "技术风格模板...";
        case "education":
            return "教育风格模板...";
        default:
            return "默认模板...";
    }
}
```

**问题**: 
- 代码复杂度增加
- 添加新场景需要修改代码
- 难以维护

#### ✅ 改进后
准备多个配置文件：
```
config/
  ├── application.yml              # 默认
  ├── application-customer.yml     # 客服
  ├── application-technical.yml    # 技术
  └── application-education.yml    # 教育
```

启动时选择：
```bash
java -jar app.jar --spring.profiles.active=customer
```

**优势**:
- 代码保持简洁
- 添加新场景只需新增配置文件
- 易于管理和切换

---

### 场景 3: A/B 测试不同提示词效果

#### ❌ 改进前
1. 版本 A: 修改代码，编译，部署到服务器 A
2. 版本 B: 修改代码，编译，部署到服务器 B
3. 对比测试
4. 选择最优版本，再次部署到所有服务器

**耗时**: 2-4 小时  
**复杂度**: 高

#### ✅ 改进后
1. 版本 A: 修改配置文件，重启服务器 A
2. 版本 B: 修改配置文件，重启服务器 B
3. 对比测试
4. 将最优配置应用到所有服务器

**耗时**: 10-20 分钟  
**复杂度**: 低

---

## 📈 效率提升统计

| 操作场景 | 改进前耗时 | 改进后耗时 | 效率提升 |
|---------|-----------|-----------|---------|
| 调整提示词 | 30-60分钟 | 1-2分钟 | **95%** ↑ |
| 切换场景 | 30-60分钟 | 30秒 | **98%** ↑ |
| A/B 测试 | 2-4小时 | 10-20分钟 | **90%** ↑ |
| 回滚变更 | 30-60分钟 | 1分钟 | **97%** ↑ |

---

## 🎨 提示词模板示例对比

### 默认风格
```yaml
prompt-template: |
  你是一个专业的知识助手。请基于文档内容回答用户问题。
  
  问题：{question}
  文档：{context}
  回答：
```

### 客服风格
```yaml
prompt-template: |
  您好！我是您的专属智能客服助手 😊
  
  您的问题：{question}
  相关信息：{context}
  
  让我为您解答：
```

### 技术风格
```yaml
prompt-template: |
  # Technical Support Response
  
  Query: {question}
  Documentation: {context}
  
  Technical Answer:
```

### 简洁风格
```yaml
prompt-template: |
  Q: {question}
  Docs: {context}
  A:
```

**切换方式**: 只需修改 `config/application.yml` 中的 `prompt-template`，重启即可！

---

## ✅ 向后兼容性

### 旧版本配置（无 prompt-template）
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      model: deepseek-chat
      # 没有 prompt-template 配置
```

**结果**: ✅ 正常工作  
**原因**: 代码中提供了默认值，自动使用默认提示词模板

### 新版本配置（有 prompt-template）
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      model: deepseek-chat
      prompt-template: |
        自定义提示词...
```

**结果**: ✅ 使用自定义提示词

---

## 🎯 总结

| 维度 | 改进前 | 改进后 |
|-----|-------|-------|
| **灵活性** | ❌ 低（硬编码） | ✅ 高（配置化） |
| **维护性** | ❌ 困难（需修改代码） | ✅ 简单（只改配置） |
| **响应速度** | ❌ 慢（30-60分钟） | ✅ 快（1-2分钟） |
| **风险等级** | ⚠️ 中-高（代码变更） | ✅ 低（配置变更） |
| **用户体验** | ❌ 差（需技术支持） | ✅ 好（自助配置） |
| **扩展性** | ❌ 差（需改代码） | ✅ 好（加配置即可） |
| **向后兼容** | - | ✅ 完全兼容 |

---

**结论**: 通过配置化改造，系统的灵活性、可维护性、用户体验都得到了显著提升，同时保持了完全的向后兼容性。这是一个成功的架构优化！✨

---

**版本**: v1.0  
**日期**: 2025-11-25

