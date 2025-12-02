# OCR 图片识别快速启动指南

## 📖 概述

本系统支持通过 OCR（光学字符识别）技术从图片中提取文字内容，无需设置复杂的环境变量，所有配置都可以在 `application.yml` 文件中完成。

## 🚀 快速开始

### 1. 安装 Tesseract OCR

根据你的操作系统选择安装方式：

#### Windows
1. 下载 Tesseract 安装包：https://github.com/UB-Mannheim/tesseract/wiki
2. 运行安装程序（推荐安装到：`C:\Program Files\Tesseract-OCR`）
3. 记下 `tessdata` 文件夹的路径（通常是：`C:\Program Files\Tesseract-OCR\tessdata`）

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim tesseract-ocr-eng
```

#### macOS
```bash
brew install tesseract tesseract-lang
```

### 2. 配置 application.yml

打开 `src/main/resources/application.yml` 或 `release/config/application.yml`，找到 `image-processing` 部分：

```yaml
knowledge:
  qa:
    image-processing:
      # 选择 OCR 策略
      strategy: ocr
      
      # 启用 OCR
      enable-ocr: true
      
      # OCR 配置
      ocr:
        # 方式1：直接指定 tessdata 路径（推荐）
        tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
        
        # 方式2：使用环境变量（如果你已经设置了 TESSDATA_PREFIX）
        # tessdata-path: ${TESSDATA_PREFIX:./tessdata}
        
        # 识别语言
        language: chi_sim+eng
```

### 3. 重启应用

保存配置文件后，重启应用即可。

## 🔧 配置说明

### 图片处理策略

系统支持4种图片处理策略：

| 策略 | 说明 | 依赖 |
|------|------|------|
| `placeholder` | 占位符模式，显示 `[图片: xxx.png]` | 无 |
| `ocr` | OCR 文字识别，提取图片中的文字 | Tesseract OCR |
| `vision-llm` | 使用 AI 模型理解图片内容 | API Key |
| `hybrid` | 混合模式（OCR + Vision LLM） | Tesseract + API Key |

### tessdata-path 配置方式

#### 方式1：直接指定路径（推荐）

```yaml
tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
```

**优点**：
- ✅ 配置清晰明确
- ✅ 不需要设置环境变量
- ✅ 便于团队协作和部署

#### 方式2：使用环境变量

```yaml
tessdata-path: ${TESSDATA_PREFIX:./tessdata}
```

设置环境变量：
```bash
# Windows
set TESSDATA_PREFIX=C:\Program Files\Tesseract-OCR\tessdata

# Linux/Mac
export TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata
```

**优点**：
- ✅ 兼容旧版本配置
- ✅ 适合多环境部署

#### 方式3：使用项目内置路径

```yaml
tessdata-path: ./tessdata
```

将 tessdata 文件夹复制到项目根目录。

**优点**：
- ✅ 便于打包分发
- ✅ 无需安装 Tesseract

### 语言配置

| 配置值 | 说明 |
|--------|------|
| `chi_sim` | 简体中文 |
| `chi_tra` | 繁体中文 |
| `eng` | 英文 |
| `chi_sim+eng` | 中英文混合（推荐） |
| `jpn` | 日文 |
| `kor` | 韩文 |

## ✅ 验证配置

启动应用后，查看日志输出：

```
✅ 简化版向量索引引擎已初始化
   - 索引路径: ./data/vector-index
   - 向量维度: 384
   - 当前向量数: 210
   - 检索方式: 线性扫描（适合<10万条）

🎨 图片处理配置已初始化
   - 策略: ocr
   添加 OCR 策略：
   - Tesseract 数据路径：C:/Program Files/Tesseract-OCR/tessdata
   - 识别语言：chi_sim+eng
   ✅ OCR 策略可用
   🎯 激活策略：Tesseract OCR (优先级: 2)
```

如果看到 `✅ OCR 策略可用`，说明配置成功！

## 🐛 常见问题

### Q1: 显示 "OCR 策略不可用"

**可能原因**：
1. Tesseract OCR 未安装
2. tessdata 路径配置错误
3. 缺少语言数据文件

**解决方法**：
1. 检查 Tesseract 是否已正确安装
2. 验证 tessdata 路径是否存在
3. 确认语言数据文件（如 `chi_sim.traineddata`）存在于 tessdata 目录

### Q2: 图片中的中文识别不准确

**解决方法**：
1. 确保已安装中文语言包
2. 使用混合语言配置：`language: chi_sim+eng`
3. 考虑升级到混合模式（hybrid），结合 Vision LLM 提高准确率

### Q3: 不想设置环境变量怎么办？

**解决方法**：
直接在配置文件中指定绝对路径：

```yaml
ocr:
  tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
```

这是最简单直接的方式，无需设置任何环境变量！

## 📚 进阶配置

### 混合模式（推荐）

结合 OCR 和 Vision LLM，获得最佳效果：

```yaml
image-processing:
  strategy: hybrid
  enable-ocr: true
  
  ocr:
    tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
    language: chi_sim+eng
  
  vision-llm:
    enabled: true
    api-key: sk-your-api-key
    model: gpt-4o
```

### Vision LLM 独立模式

如果不想安装 Tesseract，可以只使用 Vision LLM：

```yaml
image-processing:
  strategy: vision-llm
  enable-ocr: false
  
  vision-llm:
    enabled: true
    api-key: sk-your-api-key
    model: gpt-4o
```

## 💡 最佳实践

1. **开发环境**：使用直接路径配置，方便快速调试
   ```yaml
   tessdata-path: C:/Program Files/Tesseract-OCR/tessdata
   ```

2. **生产环境**：使用环境变量，便于不同服务器部署
   ```yaml
   tessdata-path: ${TESSDATA_PREFIX:/opt/tesseract/tessdata}
   ```

3. **混合语言文档**：使用多语言配置
   ```yaml
   language: chi_sim+eng+jpn
   ```

4. **高准确率要求**：启用混合模式
   ```yaml
   strategy: hybrid
   ```

## 📞 获取帮助

如有问题，请：
1. 查看应用日志：`logs/app-info.log`
2. 查看详细文档：`docs/develop/OCR_CONFIGURATION_GUIDE.md`
3. 提交 Issue 到项目仓库

