# Vision LLM 配置验证与测试指南

## ✅ 配置已完成

您的系统已成功配置为使用 **Ollama qwen3-vl:8b** 进行图片 OCR 解析。

### 已完成的配置

1. ✅ **更新 VisionLLMStrategy.java**
   - 支持 Ollama `/api/chat` 端点
   - 支持 `/api/generate` 端点（兼容）
   - 自动检测 API 格式
   - 支持 base64 图片传输

2. ✅ **更新配置文件**
   - `application.yml`: 启用 Vision LLM
   - 模型: `qwen3-vl:8b`
   - 端点: `http://localhost:11434/api/chat`

3. ✅ **验证环境**
   - Ollama 服务运行: ✅
   - qwen3-vl:8b 已安装: ✅
   - 项目编译成功: ✅
   - 应用已启动: ✅

## 🧪 测试 Vision LLM 功能

### 方法 1: 通过 Web UI 测试

1. **打开浏览器访问**: http://localhost:8080

2. **上传测试文档**
   - 上传一个包含图片的 PPT、Word 或 PDF 文档
   - 建议使用 `data/documents` 目录下的 PPT 文档

3. **触发索引**
   - 点击"重建索引"按钮
   - 或使用"增量索引"（如果是新文档）

4. **观察日志**
   - 查看控制台或日志文件
   - 应该看到类似以下的日志：
     ```
     ✅ Vision LLM 可用（Vision LLM available）
        - API 格式（API Format）: OLLAMA
        - 模型（Model）: qwen3-vl:8b
        - 端点（Endpoint）: http://localhost:11434/api/chat
     
     使用 Vision LLM 处理图片: slide1_image1.png
     Vision LLM 提取内容 [slide1_image1.png]: 234 字符
     ```

5. **测试问答**
   - 提问关于图片内容的问题
   - 例如: "图片中显示了什么内容?"
   - 系统应该能回答基于 Vision LLM 提取的内容

### 方法 2: 手动测试 Ollama API

```bash
# 测试 Ollama qwen3-vl:8b 是否工作
curl -X POST http://localhost:11434/api/chat -d '{
  "model": "qwen3-vl:8b",
  "messages": [
    {
      "role": "user",
      "content": "请描述这张图片",
      "images": ["<base64_image_data>"]
    }
  ],
  "stream": false
}'
```

### 方法 3: 查看日志文件

```bash
# 查看应用日志
tail -f logs/app-info.log

# 或在 Windows PowerShell
Get-Content logs\app-info.log -Tail 50 -Wait
```

查找以下关键日志：
- `Vision LLM 可用`
- `使用 Vision LLM 处理图片`
- `Vision LLM 提取内容`

## 🔍 故障排查

### 问题 1: Vision LLM 显示"不可用"

**原因**: Ollama 服务未运行或模型未加载

**解决方案**:
```bash
# 检查 Ollama 服务
curl http://localhost:11434/api/tags

# 如果失败，启动 Ollama
ollama serve

# 确认模型已安装
ollama list
ollama pull qwen3-vl:8b  # 如果未安装
```

### 问题 2: 图片未被处理

**原因**: 图片处理策略未正确配置

**解决方案**:
检查 `application.yml`:
```yaml
knowledge:
  qa:
    image-processing:
      strategy: vision-llm  # 或 hybrid
      vision-llm:
        enabled: true
```

### 问题 3: 处理速度慢

**原因**: Vision LLM 处理比 OCR 慢，这是正常的

**解决方案**:
1. 使用 GPU 加速（Ollama 会自动使用）
2. 考虑使用更小的模型（如 `llava:7b`）
3. 或使用 hybrid 模式（Vision LLM + OCR 备用）

### 问题 4: 内存不足

**原因**: qwen3-vl:8b 需要约 6GB 内存

**解决方案**:
1. 关闭其他应用释放内存
2. 使用更小的模型: `llava:7b` (约 4.7GB)
3. 增加系统交换空间（不推荐，会很慢）

## 📋 配置选项

### 切换图片处理策略

编辑 `application.yml`:

```yaml
image-processing:
  strategy: vision-llm  # 可选: placeholder, ocr, vision-llm, hybrid
```

- **placeholder**: 占位符，快速但无图片内容
- **ocr**: Tesseract OCR，只提取文字
- **vision-llm**: Vision LLM，语义理解（当前配置）
- **hybrid**: Vision LLM + OCR 备用（推荐生产环境）

### 调整并行处理

```yaml
document:
  parallel-processing: true
  parallel-threads: 4  # 根据 CPU 核心数调整
  batch-size: 10
```

## 🎯 预期效果

使用 Vision LLM 后，系统能够:

1. ✅ **提取图片中的文字**
   - 比纯 OCR 更准确
   - 支持多语言（中英文等）

2. ✅ **理解图表和表格**
   - 识别图表类型（柱状图、饼图等）
   - 提取表格结构和数据

3. ✅ **描述图片内容**
   - 识别图片中的对象
   - 理解图片主题和语义

4. ✅ **结构化信息提取**
   - 从复杂图片中提取关键信息
   - 保持信息的上下文关系

## 📞 需要帮助?

如遇到问题，请:

1. 查看日志文件: `logs/app-info.log`
2. 检查配置: `application.yml`
3. 验证 Ollama: `curl http://localhost:11434/api/tags`
4. 查看详细报告: `VISION_LLM_CONFIG_REPORT.md`

---

**状态**: ✅ 配置完成并验证
**时间**: 2025-12-03
**模型**: Ollama qwen3-vl:8b

