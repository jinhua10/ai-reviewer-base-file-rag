# 🎨 Vision LLM 通用配置指南

## 📖 概述

`VisionLLMStrategy` 是一个**通用的多模态视觉语言模型接口**，支持任何兼容的 Vision API，包括：
- ✅ OpenAI GPT-4o / GPT-4 Vision（在线）
- ✅ DeepSeek VL（在线）
- ✅ Ollama LLaVA / MiniCPM-V / Qwen-VL（离线）
- ✅ 任何其他兼容的 Vision API 服务

**核心特性**：
- 🔄 **自动格式检测**：根据 endpoint 自动识别 API 格式
- 🎯 **统一接口**：无需修改代码即可切换不同服务
- 🔌 **即插即用**：只需配置 endpoint、model、api-key

## 🚀 快速开始

### 方式1：Ollama 离线部署（推荐）

#### 步骤1：安装 Ollama

```bash
# Windows
# 下载：https://ollama.ai/download

# Linux/Mac
curl -fsSL https://ollama.ai/install.sh | sh
```

#### 步骤2：下载模型

```bash
# 选择一个模型下载
ollama pull llava:7b        # 推荐，7B 参数，平衡
ollama pull llava:13b       # 13B 参数，高质量
ollama pull minicpm-v       # 2.4B 参数，中文优秀，资源占用小
ollama pull qwen-vl         # 阿里通义千问，中文最强
```

#### 步骤3：启动服务

```bash
ollama serve
```

#### 步骤4：配置 application.yml

```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      
      vision-llm:
        enabled: true
        # Ollama 本地服务
        endpoint: http://localhost:11434/api/generate
        model: llava:7b
        api-key: ""  # Ollama 不需要 API Key，留空即可
```

### 方式2：OpenAI 在线服务

```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      
      vision-llm:
        enabled: true
        endpoint: https://api.openai.com/v1/chat/completions
        model: gpt-4o
        api-key: sk-your-openai-api-key
```

### 方式3：DeepSeek VL（国产，兼容 OpenAI 格式）

```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      
      vision-llm:
        enabled: true
        endpoint: https://api.deepseek.com/v1/chat/completions
        model: deepseek-vl
        api-key: sk-your-deepseek-api-key
```

### 方式4：自定义服务

```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      
      vision-llm:
        enabled: true
        endpoint: http://your-server:8080/v1/chat/completions
        model: your-model-name
        api-key: your-api-key
```

## 🔧 API 格式自动检测

系统会根据 `endpoint` 自动检测 API 格式：

| Endpoint 特征 | 检测为 | 说明 |
|--------------|--------|------|
| 包含 `/api/generate` | Ollama | Ollama 标准格式 |
| 包含 `:11434` | Ollama | Ollama 默认端口 |
| 包含 `/chat/completions` | OpenAI Chat | OpenAI 标准格式 |
| 包含 `/v1/` | OpenAI Chat | OpenAI API v1 |
| 其他 | OpenAI Chat | 默认使用最通用格式 |

**示例**：

```yaml
# 自动检测为 Ollama 格式
endpoint: http://localhost:11434/api/generate

# 自动检测为 OpenAI Chat 格式
endpoint: https://api.openai.com/v1/chat/completions
endpoint: https://api.deepseek.com/v1/chat/completions
endpoint: http://custom-server/v1/chat/completions
```

## 📊 请求格式对比

### OpenAI Chat Completions 格式

```json
{
  "model": "gpt-4o",
  "max_tokens": 1000,
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "请识别并提取这张图片中的所有文字内容..."
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
            "detail": "high"
          }
        }
      ]
    }
  ]
}
```

**响应格式**：
```json
{
  "choices": [
    {
      "message": {
        "content": "识别结果..."
      }
    }
  ],
  "usage": {
    "prompt_tokens": 100,
    "completion_tokens": 50,
    "total_tokens": 150
  }
}
```

### Ollama 格式

```json
{
  "model": "llava:7b",
  "prompt": "请识别并提取这张图片中的所有文字内容...",
  "images": [
    "/9j/4AAQSkZJRg..."  // base64 图片数据
  ],
  "stream": false
}
```

**响应格式**：
```json
{
  "response": "识别结果...",
  "done": true
}
```

## 💡 使用技巧

### 1. 多服务混合部署

```yaml
# 开发环境：使用 Ollama 本地服务（免费）
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      vision-llm:
        enabled: true
        endpoint: http://localhost:11434/api/generate
        model: llava:7b
        api-key: ""
```

```yaml
# 生产环境：使用 OpenAI（效果最佳）
knowledge:
  qa:
    image-processing:
      strategy: vision-llm
      vision-llm:
        enabled: true
        endpoint: https://api.openai.com/v1/chat/completions
        model: gpt-4o
        api-key: ${OPENAI_API_KEY}
```

### 2. 使用环境变量

```yaml
vision-llm:
  enabled: true
  endpoint: ${VISION_ENDPOINT:http://localhost:11434/api/generate}
  model: ${VISION_MODEL:llava:7b}
  api-key: ${VISION_API_KEY:}
```

**设置环境变量**：
```bash
# Ollama 本地
export VISION_ENDPOINT=http://localhost:11434/api/generate
export VISION_MODEL=llava:7b
export VISION_API_KEY=""

# OpenAI 在线
export VISION_ENDPOINT=https://api.openai.com/v1/chat/completions
export VISION_MODEL=gpt-4o
export VISION_API_KEY=sk-your-api-key
```

### 3. 降级策略

使用混合模式，自动降级：

```yaml
knowledge:
  qa:
    image-processing:
      # 混合模式：优先 Vision LLM，失败则降级到 OCR
      strategy: hybrid
      
      # Vision LLM 配置
      vision-llm:
        enabled: true
        endpoint: http://localhost:11434/api/generate
        model: llava:7b
        api-key: ""
      
      # OCR 降级配置
      enable-ocr: true
      ocr:
        tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
        language: chi_sim+eng
```

## 🐛 故障排查

### 问题1：Ollama 连接失败

**错误信息**：
```
⚠️  Vision LLM 服务不可用: Connection refused
```

**解决方法**：
1. 检查 Ollama 是否运行：
   ```bash
   ollama serve
   ```

2. 检查端口是否正确（默认 11434）：
   ```bash
   curl http://localhost:11434/api/tags
   ```

3. 检查防火墙设置

### 问题2：模型未下载

**错误信息**：
```
model 'llava:7b' not found
```

**解决方法**：
```bash
# 列出已下载的模型
ollama list

# 下载模型
ollama pull llava:7b
```

### 问题3：OpenAI API Key 无效

**错误信息**：
```
Vision API 错误: HTTP 401
```

**解决方法**：
1. 检查 API Key 是否正确
2. 检查 API Key 是否有 Vision 权限
3. 检查账户余额

### 问题4：响应解析失败

**错误信息**：
```
无法解析 OpenAI API 响应
```

**解决方法**：
1. 检查 endpoint 是否正确
2. 查看完整错误日志
3. 确认 API 格式是否匹配

## 📈 性能对比

| 服务 | 延迟 | 成本 | 准确率 | 推荐场景 |
|------|-----|------|--------|---------|
| **Ollama LLaVA-7B** | 500-2000ms | 免费 | ⭐⭐⭐⭐ | 本地开发，无网络 |
| **Ollama LLaVA-13B** | 1000-3000ms | 免费 | ⭐⭐⭐⭐⭐ | 高质量要求，本地 |
| **Ollama MiniCPM-V** | 300-1000ms | 免费 | ⭐⭐⭐⭐ | 低资源，中文优先 |
| **OpenAI GPT-4o** | 2000-4000ms | $0.01/图 | ⭐⭐⭐⭐⭐ | 生产环境，最佳效果 |
| **DeepSeek VL** | 2000-3000ms | $0.002/图 | ⭐⭐⭐⭐ | 国内，成本敏感 |

## 🎯 最佳实践

1. **开发阶段**：使用 Ollama 本地服务
   - 零成本
   - 快速迭代
   - 数据不出本地

2. **测试阶段**：使用混合模式
   - Vision LLM 主力
   - OCR 降级
   - 稳定性测试

3. **生产环境**：根据需求选择
   - 高质量：OpenAI GPT-4o
   - 成本敏感：DeepSeek VL
   - 数据安全：Ollama 自建

4. **监控告警**：
   - 记录响应时间
   - 监控失败率
   - 跟踪成本（在线服务）

## 🔗 相关资源

- **Ollama 官网**：https://ollama.ai/
- **LLaVA 模型**：https://ollama.ai/library/llava
- **MiniCPM-V 模型**：https://ollama.ai/library/minicpm-v
- **Qwen-VL 模型**：https://ollama.ai/library/qwen-vl
- **OpenAI Vision API**：https://platform.openai.com/docs/guides/vision
- **DeepSeek API**：https://platform.deepseek.com/

## ✅ 总结

现在您的系统支持：
- ✅ **通用 API 接口**：无需修改代码，只需配置即可切换服务
- ✅ **自动格式检测**：智能识别 OpenAI、Ollama 等不同格式
- ✅ **离线 + 在线**：灵活选择本地或云端服务
- ✅ **即插即用**：配置 3 行即可使用

只需在 `application.yml` 中配置 `endpoint`、`model`、`api-key` 三项，系统会自动处理其余所有细节！🎉

