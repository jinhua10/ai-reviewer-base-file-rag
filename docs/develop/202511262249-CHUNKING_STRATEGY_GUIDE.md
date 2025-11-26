# 📚 文档切分配置指南

## 🎯 概述

本系统支持**4种文档切分策略**，让您可以根据预算、性能和质量需求灵活选择：

| 策略 | 性能 | 成本 | 效果 | 适用场景 |
|------|------|------|------|----------|
| **NONE** | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ | 小文档，不需要切分 |
| **SIMPLE** | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | 性能优先，对质量要求不高 |
| **SMART_KEYWORD** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 平衡方案（推荐） |
| **AI_SEMANTIC** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 质量优先，预算充足 |

---

## 📖 配置说明

### 基础配置

在 `application.yml` 中配置：

```yaml
knowledge:
  qa:
    llm:
      # 总上下文限制
      max-context-length: 32000
      
      # 单文档限制
      max-doc-length: 10000
      
      # 切分策略 (NONE/SIMPLE/SMART_KEYWORD/AI_SEMANTIC)
      chunking-strategy: SMART_KEYWORD
      
      # 切分配置
      chunking:
        # 块大小（建议为 max-doc-length 的 80%）
        chunk-size: 8000
        
        # 重叠大小（建议为 chunk-size 的 10-20%）
        chunk-overlap: 800
        
        # 是否在句子边界切分（强烈推荐）
        split-on-sentence: true
```

---

## 🎨 策略详解

### 1. NONE - 不切分

**配置**:
```yaml
chunking-strategy: NONE
```

**特点**:
- ✅ 性能最好，无额外开销
- ✅ 保留完整文档结构
- ❌ 超长文档会被直接截断
- ❌ 可能丢失大量内容

**适用场景**:
- 所有文档都很小（< 5000 字符）
- 不在意内容丢失
- 追求极致性能

**示例效果**:
```
文档: 15000 字符
结果: 直接截断到 5000 字符
丢失: 10000 字符 (66.7%)
```

---

### 2. SIMPLE - 简单切分

**配置**:
```yaml
chunking-strategy: SIMPLE
chunking:
  chunk-size: 4000
  chunk-overlap: 400
  split-on-sentence: true
```

**特点**:
- ✅ 性能好，处理速度快
- ✅ 在句子边界切分，保持基本语义
- ✅ 有重叠，保证上下文连贯
- ❌ 不考虑内容相关性

**适用场景**:
- 文档结构简单
- 性能要求高
- 成本敏感

**示例效果**:
```
文档: 15000 字符
配置: chunk-size=4000, overlap=400
结果: 5个块，每块 4000 字符（有重叠）
```

```
块1: [0-4000]    "第一部分内容..."
块2: [3600-7600] "...第二部分内容..."  ← 与块1重叠400字符
块3: [7200-11200] "...第三部分内容..."
块4: [10800-14800] "...第四部分内容..."
块5: [14400-15000] "...最后部分内容"
```

---

### 3. SMART_KEYWORD - 智能关键词切分（推荐）

**配置**:
```yaml
chunking-strategy: SMART_KEYWORD
chunking:
  chunk-size: 4000
  chunk-overlap: 400
  split-on-sentence: true
```

**特点**:
- ✅ 优先提取包含查询关键词的部分
- ✅ 保留最相关的内容
- ✅ 在句子边界切分
- ✅ 平衡性能和效果

**适用场景**:
- **日常使用（推荐）**
- 需要平衡成本和效果
- 有明确的查询词

**工作原理**:
1. 从用户问题中提取关键词（去除停用词）
2. 在文档中查找所有关键词位置
3. 以关键词为中心提取文档块
4. 优先返回包含关键词最多的块

**示例效果**:
```
用户问题: "如何配置向量检索模型？"
关键词: ["配置", "向量", "检索", "模型"]

文档: 15000 字符
结果: 找到3个包含关键词的区域

块1: [2000-6000]   包含 "配置" 和 "模型" ← 高相关性
块2: [8000-12000]  包含 "向量" 和 "检索" ← 高相关性
块3: [0-4000]      包含 "配置" ← 中等相关性

总传递: 12000 字符 (80%)
丢失: 3000 字符 (20%)，且是最不相关的部分
```

---

### 4. AI_SEMANTIC - AI 语义切分

**配置**:
```yaml
chunking-strategy: AI_SEMANTIC
chunking:
  chunk-size: 4000
  chunk-overlap: 400
  split-on-sentence: true
  
  # AI 切分配置
  ai-chunking:
    enabled: true
    model: deepseek-chat  # 建议用便宜的模型
    prompt: |
      请将以下文档智能切分成多个语义完整的段落。
      
      要求：
      1. 每个段落应该是一个完整的主题或概念
      2. 保持段落之间的逻辑连贯性
      3. 每个段落大小在 {chunk_size} 字符左右
      4. 返回 JSON 格式：[{"content": "段落1内容", "title": "段落1标题"}, ...]
      
      文档内容：
      {content}
```

**特点**:
- ✅ 按语义切分，逻辑最连贯
- ✅ 每个块都是完整的主题
- ✅ 自动生成块标题
- ❌ 需要额外的 LLM 调用（成本高）
- ❌ 处理速度较慢

**适用场景**:
- 重要文档，对质量要求极高
- 预算充足
- 文档结构复杂，主题多样

**工作原理**:
1. 将文档发送给 LLM
2. LLM 分析文档结构和语义
3. 按照完整主题切分
4. 返回 JSON 格式的切分结果

**示例效果**:
```
文档: 技术文档，包含多个章节

AI 切分结果:
[
  {
    "title": "项目介绍",
    "content": "本项目是一个基于 RAG 的知识库系统..."
  },
  {
    "title": "安装配置",
    "content": "首先需要安装 Java 17 和 Maven..."
  },
  {
    "title": "向量检索配置",
    "content": "向量检索支持多种嵌入模型..."
  },
  {
    "title": "LLM 配置",
    "content": "系统支持 OpenAI、DeepSeek 等多种 LLM..."
  }
]

每个块都是完整的主题，标题清晰
```

**成本估算**:
- 每个文档需要 1 次额外的 LLM 调用
- DeepSeek: ~¥0.001/文档
- GPT-4o-mini: ~¥0.003/文档
- 10000 文档 ≈ ¥10-30

---

## 🎮 使用建议

### 预算分级推荐

#### 💰 低预算（<¥100/月）
```yaml
chunking-strategy: SIMPLE
max-context-length: 20000
max-doc-length: 5000
chunking:
  chunk-size: 4000
  chunk-overlap: 400
```

#### 💰💰 中等预算（¥100-500/月）
```yaml
chunking-strategy: SMART_KEYWORD  # 推荐
max-context-length: 32000
max-doc-length: 10000
chunking:
  chunk-size: 8000
  chunk-overlap: 800
```

#### 💰💰💰 高预算（>¥500/月）
```yaml
chunking-strategy: AI_SEMANTIC
max-context-length: 100000  # 使用 GPT-4o
max-doc-length: 30000
chunking:
  chunk-size: 25000
  ai-chunking:
    enabled: true
    model: gpt-4o-mini  # 或 deepseek-chat
```

---

### 文档类型推荐

#### 技术文档（API 文档、用户手册）
```yaml
chunking-strategy: SMART_KEYWORD
chunk-size: 6000
chunk-overlap: 600
split-on-sentence: true
```

**原因**: 用户查询通常包含明确的技术术语，关键词匹配效果好

#### 长篇文章（报告、论文）
```yaml
chunking-strategy: AI_SEMANTIC
chunk-size: 8000
ai-chunking:
  enabled: true
```

**原因**: 需要保持完整的论述逻辑

#### 表格数据（Excel）
```yaml
chunking-strategy: SIMPLE
chunk-size: 10000
split-on-sentence: false
```

**原因**: 表格数据没有自然的句子边界

#### 代码文件
```yaml
chunking-strategy: SIMPLE
chunk-size: 5000
split-on-sentence: false
```

**原因**: 代码需要保持完整的函数或类

---

## 🧪 测试和调优

### 测试步骤

1. **准备测试文档**
   - 选择代表性的文档（小、中、大）
   - 准备常见的用户问题

2. **测试不同策略**
   ```bash
   # 修改配置
   vim src/main/resources/application.yml
   
   # 重启应用
   mvn spring-boot:run
   
   # 测试问答
   curl -X POST http://localhost:8080/api/qa/ask \
     -H "Content-Type: application/json" \
     -d '{"question":"测试问题"}'
   ```

3. **检查日志**
   ```
   # 查看内容统计
   grep "Context Stats" logs/app-info.log
   
   # 查看切分信息
   grep "chunking" logs/app-info.log
   ```

4. **评估指标**
   - 内容保留率（>70% 为佳）
   - 回答准确性
   - 响应时间（<3秒为佳）
   - API 成本

### 调优建议

#### 如果内容丢失严重（>50%）
→ 增大 `max-context-length` 和 `chunk-size`

#### 如果响应太慢（>5秒）
→ 减小 `max-context-length` 或使用 SIMPLE 策略

#### 如果回答不准确
→ 使用 SMART_KEYWORD 或 AI_SEMANTIC

#### 如果成本太高
→ 降低 `max-context-length`，使用 SIMPLE 策略

---

## 📊 策略对比实测

### 测试条件
- 文档: 20000 字符的技术文档
- 问题: "如何配置向量检索？"
- 模型: DeepSeek-chat

### 结果对比

| 策略 | 传递字符数 | 丢失率 | 响应时间 | 成本 | 回答质量 |
|------|-----------|--------|---------|------|---------|
| NONE | 5000 | 75% | 1.2s | ¥0.001 | ⭐⭐ |
| SIMPLE | 12000 | 40% | 1.5s | ¥0.002 | ⭐⭐⭐ |
| SMART_KEYWORD | 8000 | 60%* | 1.8s | ¥0.002 | ⭐⭐⭐⭐ |
| AI_SEMANTIC | 16000 | 20% | 3.5s | ¥0.005 | ⭐⭐⭐⭐⭐ |

*注: SMART_KEYWORD 虽然传递字符少，但都是相关内容，所以质量高

---

## 🎯 快速开始

### 1. 默认配置（零配置）
系统默认使用 `SMART_KEYWORD` 策略，开箱即用。

### 2. 快速切换策略
只需修改一个配置项：
```yaml
chunking-strategy: AI_SEMANTIC  # 改这一行就够了
```

### 3. 高级定制
完整配置示例：
```yaml
knowledge:
  qa:
    llm:
      max-context-length: 32000
      max-doc-length: 10000
      chunking-strategy: SMART_KEYWORD
      
      chunking:
        chunk-size: 8000
        chunk-overlap: 800
        split-on-sentence: true
        
        ai-chunking:
          enabled: false  # 仅 AI_SEMANTIC 需要
          model: deepseek-chat
```

---

## ❓ 常见问题

### Q: 应该选择哪个策略？
**A**: 
- 95% 的场景：`SMART_KEYWORD`（推荐）
- 预算紧张：`SIMPLE`
- 质量优先：`AI_SEMANTIC`

### Q: chunk-size 应该设置多大？
**A**: 建议设置为 `max-doc-length` 的 80%，留出重叠空间。

### Q: AI_SEMANTIC 会很慢吗？
**A**: 会增加 1-2 秒，但只在索引时执行一次，问答时不影响。

### Q: 可以动态切换策略吗？
**A**: 可以！修改配置后重启即生效。未来版本会支持 API 动态切换。

### Q: 切分会破坏代码吗？
**A**: 对于代码文件，建议使用 `SIMPLE` 策略并关闭 `split-on-sentence`。

---

**版本**: 1.0  
**最后更新**: 2025-11-26  
**作者**: AI Reviewer Team

