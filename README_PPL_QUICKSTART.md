# 🚀 PPL 服务快速入门（5 分钟）

## ✅ 当前配置

你的项目已经配置好使用 **Ollama + Qwen2.5**，只需 3 步即可开始使用！

---

## 📋 快速开始

### 第 1 步：安装 Ollama（2 分钟）

```powershell
# 1. 访问 Ollama 官网下载安装程序
# https://ollama.com/download/windows

# 2. 双击 OllamaSetup.exe 安装
# 约 500MB，安装后自动启动服务

# 3. 验证安装
curl http://localhost:11434
# 应该返回：Ollama is running
```

### 第 2 步：下载 Qwen 模型（3 分钟）

```powershell
# 下载 Qwen2.5-0.5B（推荐，约 400MB）
ollama pull qwen2.5:0.5b

# 等待下载完成后，验证
ollama list
# 应该看到：qwen2.5:0.5b
```

### 第 3 步：启动应用（1 分钟）

```powershell
# 返回项目目录
cd D:\Jetbrains\hackathon\ai-reviewer-base-file-rag

# 启动应用
./mvnw spring-boot:run

# 或使用 IDE 直接运行
```

---

## ✅ 验证功能

### 1. 检查健康状态

```powershell
# 访问健康检查接口
curl http://localhost:8080/api/ppl/health
```

**预期响应**：
```json
{
  "status": "UP",
  "providers": {
    "ollama": {
      "healthy": true,
      "latency": 150
    }
  },
  "currentProvider": "ollama"
}
```

### 2. 测试 PPL 计算

```powershell
# 计算文本困惑度
curl -X POST http://localhost:8080/api/ppl/calculate `
  -H "Content-Type: application/json" `
  -d '{\"text\":\"今天天气很好，适合出去散步。\",\"provider\":\"ollama\"}'
```

### 3. 测试文档切分

```powershell
# PPL Chunking
curl -X POST http://localhost:8080/api/ppl/chunk `
  -H "Content-Type: application/json" `
  -d '{\"content\":\"这是一段很长的文本...\",\"provider\":\"ollama\"}'
```

---

## 📊 当前配置详情

### application.yml

```yaml
knowledge:
  qa:
    ppl:
      # 使用 Ollama（默认）
      default-provider: ollama
      
      ollama:
        enabled: true
        base-url: http://localhost:11434
        model: qwen2.5:0.5b
        timeout: 30000
      
      chunking:
        ppl-threshold: 20.0
        max-chunk-size: 2000
      
      reranking:
        enabled: false  # 可选启用
```

---

## 🎯 使用场景

### 1. PPL Chunking（智能文档切分）

```java
// 在你的代码中使用
@Autowired
private PPLServiceFacade pplService;

public void processDocument(String content) {
    // PPL 智能切分
    List<DocumentChunk> chunks = pplService.chunk(content, null);
    
    // 切分后的块会在主题转换处自动分界
    chunks.forEach(chunk -> {
        System.out.println("Chunk " + chunk.getIndex() + ": " + chunk.getContent());
    });
}
```

### 2. PPL Rerank（检索结果重排序）

```yaml
# 启用 Rerank
ppl:
  reranking:
    enabled: true
    weight: 0.15
    top-k: 5
```

```java
// 自动应用于混合检索
List<Document> results = hybridSearchService.search(question);
// 结果已经过 PPL Rerank 优化
```

---

## 💡 高级配置

### 切换到更好的模型

```powershell
# 下载 1.5B 模型（更好的质量）
ollama pull qwen2.5:1.5b
```

```yaml
# 更新配置
ollama:
  model: qwen2.5:1.5b
```

### 调整 Chunking 参数

```yaml
chunking:
  # 更细粒度的切分
  ppl-threshold: 15.0
  max-chunk-size: 1500
  
  # 或更粗粒度
  ppl-threshold: 25.0
  max-chunk-size: 3000
```

### 启用 Rerank

```yaml
reranking:
  enabled: true
  weight: 0.20    # PPL 权重（0.1-0.3）
  top-k: 10       # 重排序前 10 个
  async: true     # 异步处理
```

---

## 🔍 故障排查

### 问题：Ollama 服务未启动

```powershell
# 检查服务
curl http://localhost:11434

# 如果失败，手动启动
ollama serve
```

### 问题：模型未下载

```powershell
# 检查已安装的模型
ollama list

# 重新下载
ollama pull qwen2.5:0.5b
```

### 问题：应用无法连接 Ollama

```yaml
# 检查配置
ollama:
  enabled: true
  base-url: http://localhost:11434  # 确保端口正确
```

---

## 📚 更多资源

### 文档
- `20251204200000-PPL统一接口架构实施计划.md` - 完整架构
- `20251204213000-最终方案-使用Ollama.md` - 详细说明
- `20251204204500-PPL国产模型配置指南.md` - 配置指南

### Ollama
- 官网：https://ollama.com/
- 模型库：https://ollama.com/library
- GitHub：https://github.com/ollama/ollama

### Qwen
- GitHub：https://github.com/QwenLM/Qwen
- 模型：https://huggingface.co/Qwen

---

## ✅ 检查清单

完成以下步骤即可开始使用：

- [ ] 安装 Ollama
- [ ] 下载 Qwen2.5-0.5B 模型
- [ ] 验证 Ollama 服务运行
- [ ] 启动 Spring Boot 应用
- [ ] 测试健康检查接口
- [ ] 测试 PPL 计算功能

---

## 🎉 开始使用！

现在你可以：

1. **智能文档切分** - 基于语义自动识别主题边界
2. **检索结果优化** - PPL Rerank 提升准确率
3. **完全免费** - 本地运行，零成本
4. **国产支持** - 使用阿里 Qwen 模型

**预计总时间：5-10 分钟** ⏱️

---

**版本**：v1.0  
**更新时间**：2025-12-04 21:35:00  
**状态**：✅ 开箱即用

