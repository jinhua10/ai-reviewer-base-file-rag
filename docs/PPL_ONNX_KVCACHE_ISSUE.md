# PPL ONNX 服务 KV Cache 兼容性问题

> 文档版本: v1.0  
> 创建日期: 2025-12-07  
> 作者: AI Reviewer Team

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

Qwen2.5-0.5B 模型有 24 层 Transformer，每层需要：
- `past_key_values.{layer}.key`: shape `[batch, num_heads, seq_len, head_dim]`
- `past_key_values.{layer}.value`: shape `[batch, num_heads, seq_len, head_dim]`

其中：
- `batch = 1`（批次大小）
- `num_heads = 14`（注意力头数）
- `head_dim = 64`（每个头的维度）
- `seq_len = 0`（首次推理时为 0）

---

## 当前解决方案

### 方案概述

由于正确处理 KV Cache 输入非常复杂，当前采用 **检测并降级** 策略：

1. **模型加载时**：自动检测模型是否使用 KV Cache
2. **健康检查时**：如果使用 KV Cache，返回 `false`
3. **推理时**：如果使用 KV Cache，抛出明确的异常消息
4. **服务降级**：`PPLServiceFacade` 自动切换到 Ollama 等替代服务

### 代码实现

#### 1. 模型信息检测 (`logModelInfo`)

```java
private void logModelInfo() {
    Map<String, NodeInfo> inputInfo = session.getInputInfo();
    for (Map.Entry<String, NodeInfo> entry : inputInfo.entrySet()) {
        String name = entry.getKey();
        
        // 检测是否使用 KV Cache
        if (name.startsWith("past_key_values.")) {
            useKVCache = true;
            // 提取层数
            String[] parts = name.split("\\.");
            if (parts.length >= 2) {
                int layerNum = Integer.parseInt(parts[1]);
                numLayers = Math.max(numLayers, layerNum + 1);
            }
        }
    }
    
    if (useKVCache) {
        log.info("⚠️ 模型使用 KV Cache，共 {} 层", numLayers);
    }
}
```

#### 2. 健康检查 (`isHealthy`)

```java
@Override
public boolean isHealthy() {
    // 如果模型使用 KV Cache，返回 false 触发降级
    if (useKVCache) {
        log.warn("⚠️ ONNX 模型使用 KV Cache，当前不支持，请使用 Ollama 替代");
        return false;
    }
    // ... 其他检查
}
```

#### 3. 推理时检查 (`calculatePerplexity`)

```java
@Override
public double calculatePerplexity(String text) throws PPLException {
    if (useKVCache) {
        throw new PPLException(PPLProviderType.ONNX,
            "当前 ONNX 模型使用 KV Cache，暂不支持。请使用 Ollama 作为替代...");
    }
    // ... 正常推理逻辑
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
| ONNX | ⚡快 | 中 | 免费 | ❌ 需要无 KV Cache 模型 | 有合适模型时使用 |
| Ollama | ⚡快 | 中 | 免费 | ✅ 自动处理 | **推荐日常使用** |
| OpenAI | 慢 | 高 | 收费 | ✅ 不涉及 | 高精度需求 |

---

## 未来优化方向

### 短期（v2.1）

- [ ] 添加空 KV Cache 张量支持（首次推理时传入全零张量）
- [ ] 测试并验证空 KV Cache 方案的正确性
- [ ] 提供 KV Cache 模型和无 KV Cache 模型的自动检测和适配

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
⚠️ ONNX 模型使用 KV Cache，当前不支持，请使用 Ollama 替代
```

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
| v1.0 | 2025-12-07 | 初始版本，记录 KV Cache 兼容性问题及解决方案 |

