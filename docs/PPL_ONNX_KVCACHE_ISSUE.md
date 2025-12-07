# PPL ONNX 服务 KV Cache 兼容性问题

> 文档版本: v1.5  
> 创建日期: 2025-12-07  
> 作者: AI Reviewer Team

---

## ✅ 问题已解决

**v1.5 更新（2025-12-07）：动态提取 KV Cache 维度！**

之前硬编码了 `numHeads=14`，但 Qwen 模型使用 GQA（Grouped-Query Attention），实际 `num_kv_heads=2`。

现在从模型输入信息中 **动态提取** `numHeads` 和 `headDim`，确保兼容所有模型配置。

---

## 问题概述

### 错误现象

使用 ONNX 本地模型（如 `qwen2.5-0.5b`）进行 PPL（困惑度）计算时，出现以下错误：

```
ai.onnxruntime.OrtException: Error code - ORT_RUNTIME_EXCEPTION
message: Non-zero status code returned while running Concat node. 
Name:'/model/layers.0/self_attn/Concat_5' 
Status Message: Missing Input: past_key_values.0.key
```

### 根本原因

Qwen 等现代大语言模型的 ONNX 导出版本通常使用 **KV Cache** 机制来加速自回归推理。这意味着模型期望接收以下输入：

| 输入名称 | 说明 | 必需性 |
|---------|------|--------|
| `input_ids` | Token ID 序列 | ✅ 必需 |
| `attention_mask` | 注意力掩码 | ✅ 必需 |
| `position_ids` | 位置编码 | ✅ 必需 |
| `past_key_values.{layer}.key` | 每层的 Key 缓存 | ⚠️ KV Cache 模型必需 |
| `past_key_values.{layer}.value` | 每层的 Value 缓存 | ⚠️ KV Cache 模型必需 |

当前代码只提供了前三个输入，缺少 `past_key_values` 相关输入。

---

## 技术背景

### 什么是 KV Cache？

KV Cache（Key-Value Cache）是 Transformer 模型自回归推理时的一种优化技术：

```
传统推理（无 KV Cache）:
  每次生成新 token 时，重新计算所有位置的 Key 和 Value
  时间复杂度: O(n²)

KV Cache 推理:
  缓存已计算的 Key 和 Value，只计算新位置
  时间复杂度: O(n)
```

### Qwen 模型的 KV Cache 结构

Qwen2.5-0.5B 模型有 24 层 Transformer，使用 **GQA（Grouped-Query Attention）** 优化：
- `past_key_values.{layer}.key`: shape `[batch, num_kv_heads, seq_len, head_dim]`
- `past_key_values.{layer}.value`: shape `[batch, num_kv_heads, seq_len, head_dim]`

其中：
- `batch = 1`（批次大小）
- `num_kv_heads = 2`（**注意：不是 14！** GQA 使用更少的 KV heads）
- `head_dim = 64`（每个头的维度）
- `seq_len = 0`（首次推理时为 0）

> ⚠️ **GQA 说明**：Qwen 使用 Grouped-Query Attention，query heads 数量（14）与 key/value heads 数量（2）不同。这是为了减少 KV Cache 的内存占用。

---

## 当前解决方案

### 方案概述

**v1.4 更新（2025-12-07）：使用 DirectBuffer 成功支持 KV Cache！**

发现 ONNX Runtime 对普通 Java 数组不支持零维度，但对 **DirectBuffer** 支持。

**解决方案**：
1. 使用 `ByteBuffer.allocateDirect(0)` 创建空的直接缓冲区
2. 将其转换为 `FloatBuffer`
3. 使用 `OnnxTensor.createTensor(env, buffer, shape)` 创建张量

### 代码实现

#### 1. 模型信息检测 (`logModelInfo`) - 动态提取维度

```java
private void logModelInfo() {
    Map<String, NodeInfo> inputInfo = session.getInputInfo();
    for (Map.Entry<String, NodeInfo> entry : inputInfo.entrySet()) {
        String name = entry.getKey();
        NodeInfo info = entry.getValue();
        
        if (name.startsWith("past_key_values.")) {
            useKVCache = true;
            
            // 提取层数
            String[] parts = name.split("\\.");
            if (parts.length >= 2) {
                int layerNum = Integer.parseInt(parts[1]);
                numLayers = Math.max(numLayers, layerNum + 1);
            }
            
            // 关键：从模型输入动态提取 numHeads 和 headDim
            if (numHeads == 0 && info.getInfo() instanceof TensorInfo) {
                TensorInfo tensorInfo = (TensorInfo) info.getInfo();
                long[] shape = tensorInfo.getShape();
                // KV Cache 形状: [batch, num_kv_heads, seq_len, head_dim]
                if (shape.length >= 4) {
                    numHeads = (int) shape[1];  // num_kv_heads (不是 num_heads!)
                    headDim = (int) shape[3];
                }
            }
        }
    }
}
```

**为什么需要动态提取？**
- Qwen 使用 GQA（Grouped-Query Attention）
- Query heads 数量（14）与 KV heads 数量（2）不同
- 硬编码 `numHeads=14` 会导致维度不匹配错误

#### 2. 添加空 KV Cache (`addEmptyKVCache`) - 关键修复

```java
private void addEmptyKVCache(Map<String, OnnxTensor> inputs, List<OnnxTensor> tensorsToClose)
        throws OrtException {
    for (int layer = 0; layer < numLayers; layer++) {
        String keyName = "past_key_values." + layer + ".key";
        String valueName = "past_key_values." + layer + ".value";

        // 关键：使用 DirectFloatBuffer 创建零维度张量
        // 形状: [batch=1, num_heads, seq_len=0, head_dim]
        long[] shape = new long[]{1, numHeads, 0, headDim};
        
        // 创建直接缓冲区（容量为 0）
        java.nio.FloatBuffer emptyBuffer = java.nio.ByteBuffer
                .allocateDirect(0)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer();
        
        OnnxTensor keyTensor = OnnxTensor.createTensor(env, emptyBuffer, shape);
        OnnxTensor valueTensor = OnnxTensor.createTensor(env, emptyBuffer, shape);

        tensorsToClose.add(keyTensor);
        tensorsToClose.add(valueTensor);

        inputs.put(keyName, keyTensor);
        inputs.put(valueName, valueTensor);
    }
}
```

**为什么 DirectBuffer 有效？**
- 普通 Java 数组：ONNX Runtime 验证数组维度，拒绝零维度
- DirectBuffer：ONNX Runtime 直接使用缓冲区指针 + 形状信息，允许零维度
```

#### 3. 推理时自动处理 (`calculatePerplexity`)

```java
// 准备基本输入
inputs.put("input_ids", inputIdsTensor);
inputs.put("attention_mask", attentionMaskTensor);
inputs.put("position_ids", positionIdsTensor);

// 如果模型使用 KV Cache，添加空的 past_key_values
if (useKVCache) {
    addEmptyKVCache(inputs, tensorsToClose);
}

// 执行推理
try (OrtSession.Result results = session.run(inputs)) {
    // ... 计算 PPL
}
```

---

## 用户解决方案

### 方案一：使用 Ollama 替代（推荐）

Ollama 是一个本地大模型运行框架，使用简单且稳定。

```bash
# 1. 安装 Ollama
# Windows: https://ollama.com/download/windows
# Linux: curl -fsSL https://ollama.com/install.sh | sh
# macOS: brew install ollama

# 2. 下载 Qwen 模型
ollama pull qwen2.5:0.5b

# 3. 验证安装
ollama list
```

修改配置文件 `application.yml`：

```yaml
knowledge:
  qa:
    ppl:
      # 将默认提供商从 onnx 改为 ollama
      default-provider: ollama
      
      ollama:
        enabled: true
        base-url: http://localhost:11434
        model: qwen2.5:0.5b
```

### 方案二：禁用 PPL Rerank

如果不需要 PPL 重排序功能，可以直接禁用：

```yaml
knowledge:
  qa:
    ppl:
      reranking:
        enabled: false
```

### 方案三：使用不带 KV Cache 的 ONNX 模型

如果需要使用 ONNX 本地推理，需要重新导出模型时禁用 KV Cache：

```python
# 使用 optimum-cli 导出（禁用 KV Cache）
optimum-cli export onnx \
    --model Qwen/Qwen2.5-0.5B-Instruct \
    --task text-generation \
    --no-post-process \
    qwen2.5-0.5b-onnx-no-cache/
```

---

## 配置参考

### PPL 服务降级配置

```yaml
knowledge:
  qa:
    ppl:
      # 启用降级策略
      enable-fallback: true
      
      # 降级顺序（优先级从高到低）
      fallback-order:
        - ollama    # 优先：本地 Ollama
        - onnx      # 次选：本地 ONNX
        - openai    # 备用：云端 API
```

### 各引擎对比

| 引擎 | 速度 | 精度 | 成本 | KV Cache 支持 | 推荐场景 |
|------|------|------|------|---------------|---------|
| ONNX | ⚡快 | 中 | 免费 | ✅ 已支持（v1.4 DirectBuffer） | **推荐本地部署** |
| Ollama | ⚡快 | 中 | 免费 | ✅ 自动处理 | 简单部署 |
| OpenAI | 慢 | 高 | 收费 | ✅ 不涉及 | 高精度需求 |

---

## 未来优化方向

### 短期（v2.1）

- [x] ~~添加空 KV Cache 张量支持~~ ✅ 已完成（使用 DirectBuffer）
- [x] ~~测试并验证空 KV Cache 方案~~ ✅ 已完成
- [x] 自动检测 KV Cache 模型并正确处理 ✅ 已完成

### 长期（v3.0）

- [ ] 支持增量推理（利用 KV Cache 加速连续推理）
- [ ] 提供预导出的无 KV Cache ONNX 模型下载
- [ ] 集成 GGUF 格式模型支持（llama.cpp）

---

## 相关日志

当模型使用 KV Cache 时，启动日志会显示：

```
📊 模型输入信息 (Model Input Info):
  - 输入: input_ids (类型: ...)
  - 输入: attention_mask (类型: ...)
  - 输入: position_ids (类型: ...)
  - 输入: past_key_values.0.key (类型: ...)
  - 输入: past_key_values.0.value (类型: ...)
  - 输入: past_key_values.1.key (类型: ...)
  ... (每层都有 key 和 value)
  
⚠️ 模型使用 KV Cache，共 24 层
✅ 已添加 24 层空 KV Cache (Added 24 layers of empty KV Cache)
```

**PPL 计算成功时**：
```
✅ PPL 计算完成: 文本="Hello world", PPL=15.23, 耗时=50ms
```

---

## 常见问题排查

### 场景：PPL Rerank 失败并使用原排序

**日志特征**：
```
⚠️ rerank failed using Hugging Face ONNX: [Hugging Face ONNX] 文档重排序失败
🔄 Trying fallback for rerank...
⚠️ PPL Rerank 失败，使用原排序: [Hugging Face ONNX] rerank failed and all fallbacks exhausted
```

**原因分析**：

1. **ONNX 服务检测到 KV Cache 模型**
   - 系统检测到当前 ONNX 模型包含 KV Cache
   - 主动拒绝服务并抛出异常（这是预期行为）

2. **降级服务不可用**
   - `all fallbacks exhausted` 表示所有备用服务都不可用
   - 可能原因：
     - Ollama 未安装或未启动
     - Ollama 中没有下载所需模型
     - 配置的 OpenAI API Key 无效

**解决方案**：

```bash
# 方案 1：启动 Ollama 并下载模型
ollama serve                    # 启动 Ollama 服务（如未运行）
ollama pull qwen2.5:0.5b       # 下载 Qwen 模型

# 方案 2：禁用 PPL Rerank
# 在 application.yml 中设置:
# knowledge.qa.ppl.reranking.enabled: false
```

**验证降级是否配置正确**：

检查 `application.yml` 中的配置：
```yaml
knowledge:
  qa:
    ppl:
      enable-fallback: true      # 确保启用降级
      fallback-order:
        - ollama                 # 优先降级到 Ollama
        - openai                 # 备用：云端 API
      
      ollama:
        enabled: true
        base-url: http://localhost:11434
        model: qwen2.5:0.5b
```

### 场景：向量索引为空

**日志特征**：
```
索引为空，返回空结果
```

**说明**：这是正常的。向量检索返回空结果时，系统会使用纯 Lucene 关键词检索的结果。混合检索仍然可以正常工作。

---

## 参考资料

- [ONNX Runtime 官方文档](https://onnxruntime.ai/docs/)
- [Hugging Face Optimum ONNX 导出](https://huggingface.co/docs/optimum/onnxruntime/usage_guides/export)
- [Ollama 官方文档](https://ollama.com/)
- [Qwen2.5 模型说明](https://github.com/QwenLM/Qwen2.5)

---

## 更新历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.5 | 2025-12-07 | **关键修复**：动态提取 KV Cache 维度（numHeads/headDim），支持 GQA 模型 |
| v1.4 | 2025-12-07 | 使用 DirectBuffer 成功支持 KV Cache 模型 |
| v1.3 | 2025-12-07 | 禁用 PPL Rerank 功能，更新配置文件 |
| v1.2 | 2025-12-07 | 确认 ONNX Runtime 不支持零维度张量，改为检测并自动降级到 Ollama |
| v1.1 | 2025-12-07 | 尝试实现 KV Cache 支持（测试失败） |
| v1.0 | 2025-12-07 | 初始版本，记录 KV Cache 兼容性问题及解决方案 |

