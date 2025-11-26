# 📝 文档切分配置示例

## 场景 1: 经济型配置（低预算）

**适用**: 个人项目、学习用途、预算 < ¥100/月

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      # 基础限制
      max-context-length: 20000
      max-doc-length: 5000
      
      # 简单切分策略
      chunking-strategy: SIMPLE
      
      chunking:
        chunk-size: 4000
        chunk-overlap: 400
        split-on-sentence: true
        
        ai-chunking:
          enabled: false
```

**预期效果**:
- ✅ 成本最低
- ✅ 性能最好
- ⚠️ 内容丢失率 30-40%
- ⚠️ 回答质量一般

---

## 场景 2: 标准配置（推荐）

**适用**: 中小企业、商业项目、预算 ¥100-500/月

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      # 标准限制
      max-context-length: 32000
      max-doc-length: 10000
      
      # 智能关键词切分（推荐）
      chunking-strategy: SMART_KEYWORD
      
      chunking:
        chunk-size: 8000
        chunk-overlap: 800
        split-on-sentence: true
        
        ai-chunking:
          enabled: false
```

**预期效果**:
- ✅ 成本适中
- ✅ 效果良好
- ✅ 内容丢失率 15-25%
- ✅ 回答质量高

---

## 场景 3: 高质量配置（使用 GPT-4o）

**适用**: 大型企业、高质量需求、预算 > ¥500/月

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${OPENAI_API_KEY}
      api-url: https://api.openai.com/v1/chat/completions
      model: gpt-4o
      
      # 大上下文限制
      max-context-length: 100000  # GPT-4o 支持 128K
      max-doc-length: 30000
      
      # 智能关键词切分（即使用 GPT-4o 也推荐这个）
      chunking-strategy: SMART_KEYWORD
      
      chunking:
        chunk-size: 25000
        chunk-overlap: 2500
        split-on-sentence: true
        
        ai-chunking:
          enabled: false
```

**预期效果**:
- ⚠️ 成本较高
- ✅ 效果极佳
- ✅ 内容丢失率 < 10%
- ✅ 回答质量最高

---

## 场景 4: AI 语义切分配置（质量优先）

**适用**: 重要文档处理、对质量要求极高

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      # 中等限制
      max-context-length: 32000
      max-doc-length: 10000
      
      # AI 语义切分
      chunking-strategy: AI_SEMANTIC
      
      chunking:
        chunk-size: 8000
        chunk-overlap: 800
        split-on-sentence: true
        
        ai-chunking:
          enabled: true
          model: deepseek-chat  # 用便宜的模型切分
          prompt: |
            请将以下文档智能切分成多个语义完整的段落。
            
            要求：
            1. 每个段落应该是一个完整的主题或概念
            2. 保持段落之间的逻辑连贯性
            3. 每个段落大小在 {chunk_size} 字符左右
            4. 如果内容包含标题、章节，优先按章节切分
            5. 返回 JSON 格式：[{"content": "段落1内容", "title": "段落1标题"}, ...]
            
            文档内容：
            {content}
```

**预期效果**:
- ⚠️ 成本高（每文档额外 1 次 API 调用）
- ⚠️ 速度较慢（索引时间增加 2-3 倍）
- ✅ 效果最佳
- ✅ 语义完整，逻辑连贯
- ✅ 内容丢失率 < 10%

---

## 场景 5: 混合策略（推荐给高级用户）

**思路**: 根据文档类型使用不同策略

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      max-context-length: 32000
      max-doc-length: 10000
      
      # 默认策略
      chunking-strategy: SMART_KEYWORD
      
      chunking:
        chunk-size: 8000
        chunk-overlap: 800
        split-on-sentence: true
        
        ai-chunking:
          enabled: true  # 启用但不默认使用
          model: deepseek-chat
```

**代码层面实现**:
```java
// 在索引时根据文档特征选择策略
public void indexDocument(Document doc) {
    ChunkingStrategy strategy;
    
    if (doc.getLength() < 5000) {
        strategy = ChunkingStrategy.NONE;  // 小文档不切分
    } else if (doc.isImportant()) {
        strategy = ChunkingStrategy.AI_SEMANTIC;  // 重要文档用 AI
    } else if (doc.hasKeywords()) {
        strategy = ChunkingStrategy.SMART_KEYWORD;  // 有关键词用智能
    } else {
        strategy = ChunkingStrategy.SIMPLE;  // 其他用简单
    }
    
    DocumentChunker chunker = DocumentChunkerFactory.createChunker(
        strategy, config, llmClient
    );
    
    List<DocumentChunk> chunks = chunker.chunk(doc.getContent(), null);
    // ... 索引处理
}
```

---

## 场景 6: 超长文档处理

**适用**: 处理技术手册、完整书籍等超长文档

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${OPENAI_API_KEY}
      api-url: https://api.openai.com/v1/chat/completions
      model: gpt-4o  # 必须用支持大上下文的模型
      
      # 超大上下文
      max-context-length: 100000
      max-doc-length: 30000
      
      # AI 语义切分
      chunking-strategy: AI_SEMANTIC
      
      chunking:
        chunk-size: 25000  # 大块切分
        chunk-overlap: 2500
        split-on-sentence: true
        
        ai-chunking:
          enabled: true
          model: gpt-4o-mini  # 用 mini 版本切分更经济
          prompt: |
            请将以下长文档按照章节和主题智能切分。
            
            要求：
            1. 识别文档的章节结构（标题、子标题）
            2. 每个切分块是一个完整的章节或主题
            3. 保留章节标题和层级关系
            4. 每个块大小控制在 {chunk_size} 字符左右
            5. 返回 JSON 格式：
               [{
                 "title": "第1章：项目介绍",
                 "level": 1,
                 "content": "本章介绍..."
               }, ...]
            
            文档内容：
            {content}
```

**额外建议**:
- 预处理文档，提取章节结构
- 考虑分层索引（章、节、段）
- 使用摘要 + 详情的两级检索

---

## 场景 7: 代码文件处理

**适用**: 索引代码库、API 文档

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      max-context-length: 32000
      max-doc-length: 10000
      
      # 简单切分（代码不适合语义切分）
      chunking-strategy: SIMPLE
      
      chunking:
        chunk-size: 8000
        chunk-overlap: 500  # 代码重叠可以小一些
        split-on-sentence: false  # 不在"句子"边界切分
        
        ai-chunking:
          enabled: false
```

**额外配置**:
```yaml
document:
  # 代码文件特殊处理
  code-files:
    enabled: true
    # 按函数/类切分而不是按字符数
    split-by-syntax: true
    # 保留完整的函数和类
    preserve-structure: true
```

---

## 场景 8: 表格数据处理

**适用**: Excel、CSV 等结构化数据

```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
      
      max-context-length: 32000
      max-doc-length: 15000  # 表格可以设大一些
      
      # 简单切分
      chunking-strategy: SIMPLE
      
      chunking:
        chunk-size: 12000
        chunk-overlap: 0  # 表格数据不需要重叠
        split-on-sentence: false
        
        ai-chunking:
          enabled: false
```

**建议**:
- 按行数而不是字符数切分
- 保留表头在每个块中
- 考虑转换为文本描述后再索引

---

## 🎯 快速选择指南

### 我该用哪个配置？

```
开始
  │
  ├─ 预算 < ¥100/月？
  │  └─ 是 → 场景1（经济型）
  │
  ├─ 文档很长（>50000字符）？
  │  └─ 是 → 场景6（超长文档）
  │
  ├─ 主要是代码文件？
  │  └─ 是 → 场景7（代码文件）
  │
  ├─ 对质量要求极高？
  │  ├─ 预算充足 → 场景3（GPT-4o）
  │  └─ 预算一般 → 场景4（AI语义）
  │
  └─ 正常使用 → 场景2（标准配置，推荐）
```

---

## 📊 成本对比

假设: 10000 个文档，平均每个 15000 字符

| 配置 | 索引成本 | 每次问答成本 | 总成本/月* |
|------|---------|------------|-----------|
| 场景1 | ¥5 | ¥0.002 | ¥25 |
| 场景2 | ¥10 | ¥0.003 | ¥110 |
| 场景3 | ¥50 | ¥0.015 | ¥650 |
| 场景4 | ¥20 | ¥0.003 | ¥140 |

*假设每月 10000 次问答

---

## 🚀 迁移指南

### 从 SIMPLE 升级到 SMART_KEYWORD

```yaml
# 修改前
chunking-strategy: SIMPLE

# 修改后
chunking-strategy: SMART_KEYWORD
```

**影响**:
- ✅ 质量提升 20-30%
- ⚠️ 成本增加约 10%
- ⚠️ 速度降低约 15%

### 从 SMART_KEYWORD 升级到 AI_SEMANTIC

```yaml
# 修改前
chunking-strategy: SMART_KEYWORD

# 修改后
chunking-strategy: AI_SEMANTIC
chunking:
  ai-chunking:
    enabled: true
    model: deepseek-chat
```

**影响**:
- ✅ 质量提升 30-40%
- ⚠️ 成本增加 100-200%
- ⚠️ 索引时间增加 2-3 倍

---

**提示**: 可以先在小规模数据上测试不同配置，找到最适合你的方案！

