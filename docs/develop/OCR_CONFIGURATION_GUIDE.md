# OCR 图片提取配置指南

## 🎯 问题描述

日志显示：`[图片1: slide1_image1.png - 未提取内容]`

这表明系统正在使用 **PlaceholderImageStrategy**（占位符策略），而不是 **TesseractOCRStrategy**（OCR识别策略）。

## 🔍 根本原因

`SmartImageExtractor` 默认只使用占位符策略。要启用OCR，需要设置环境变量。

## ✅ 解决方案

### 方案1：设置环境变量（推荐）

#### Windows (PowerShell)
```powershell
# 设置临时环境变量（当前会话有效）
$env:ENABLE_OCR="true"
$env:TESSDATA_PREFIX="D:\path\to\tessdata"
$env:OCR_LANGUAGE="chi_sim+eng"

# 然后启动应用
java -jar your-app.jar
```

#### Windows (cmd)
```cmd
set ENABLE_OCR=true
set TESSDATA_PREFIX=D:\path\to\tessdata
set OCR_LANGUAGE=chi_sim+eng
java -jar your-app.jar
```

#### Linux/Mac
```bash
export ENABLE_OCR=true
export TESSDATA_PREFIX=/usr/share/tessdata
export OCR_LANGUAGE=chi_sim+eng
java -jar your-app.jar
```

### 方案2：在 application.properties 中配置

将环境变量添加到配置文件中：

```properties
# 启用OCR
ocr.enabled=true

# Tesseract数据路径
ocr.tessdata.path=D:/tessdata

# OCR识别语言（chi_sim=简体中文, eng=英文）
ocr.language=chi_sim+eng
```

然后在代码中读取这些配置。

### 方案3：在启动脚本中设置

修改 `start.bat`：

```batch
@echo off
echo 启动 AI Reviewer 应用...

REM 设置OCR环境变量
set ENABLE_OCR=true
set TESSDATA_PREFIX=%~dp0tessdata
set OCR_LANGUAGE=chi_sim+eng

REM 启动应用
java -jar ai-reviewer.jar

pause
```

## 📥 Tesseract OCR 安装

### Windows

1. **下载 Tesseract**
   - 官方下载: https://github.com/UB-Mannheim/tesseract/wiki
   - 选择 `tesseract-ocr-w64-setup-vX.X.X.exe`

2. **安装 Tesseract**
   - 运行安装程序
   - 安装到 `C:\Program Files\Tesseract-OCR`
   - **重要**：安装时勾选"Additional Language Data"，选择中文语言包

3. **设置环境变量**
   ```
   TESSDATA_PREFIX=C:\Program Files\Tesseract-OCR\tessdata
   ```

4. **验证安装**
   ```cmd
   tesseract --version
   tesseract --list-langs
   ```

### Linux (Ubuntu/Debian)

```bash
# 安装 Tesseract
sudo apt-get update
sudo apt-get install tesseract-ocr

# 安装中文语言包
sudo apt-get install tesseract-ocr-chi-sim tesseract-ocr-chi-tra

# 验证安装
tesseract --version
tesseract --list-langs
```

### macOS

```bash
# 使用 Homebrew 安装
brew install tesseract

# 安装中文语言包
brew install tesseract-lang

# 验证安装
tesseract --version
tesseract --list-langs
```

## 🗂️ Tessdata 文件结构

```
tessdata/
├── chi_sim.traineddata    # 简体中文
├── chi_tra.traineddata    # 繁体中文
├── eng.traineddata        # 英文
├── jpn.traineddata        # 日文
└── ...其他语言
```

## 🔧 常见语言代码

| 语言 | 代码 | 说明 |
|------|------|------|
| 简体中文 | `chi_sim` | Simplified Chinese |
| 繁体中文 | `chi_tra` | Traditional Chinese |
| 英文 | `eng` | English |
| 日文 | `jpn` | Japanese |
| 韩文 | `kor` | Korean |
| 中英混合 | `chi_sim+eng` | 同时识别中英文 |

## 📊 完整配置示例

### start.bat（Windows启动脚本）

```batch
@echo off
title AI Reviewer - 启动中...

echo ========================================
echo   AI Reviewer 启动脚本
echo ========================================
echo.

REM ===== OCR 配置 =====
echo [1/3] 配置 OCR 环境...
set ENABLE_OCR=true
set TESSDATA_PREFIX=%~dp0tessdata
set OCR_LANGUAGE=chi_sim+eng
echo   ✓ OCR 已启用
echo   ✓ Tessdata: %TESSDATA_PREFIX%
echo   ✓ 语言: %OCR_LANGUAGE%
echo.

REM ===== JVM 配置 =====
echo [2/3] 配置 JVM 参数...
set JAVA_OPTS=-Xmx2g -Xms512m
echo   ✓ 最大内存: 2GB
echo   ✓ 初始内存: 512MB
echo.

REM ===== 启动应用 =====
echo [3/3] 启动应用...
java %JAVA_OPTS% -jar ai-reviewer.jar
echo.

if %ERRORLEVEL% NEQ 0 (
    echo ❌ 应用启动失败！错误代码: %ERRORLEVEL%
) else (
    echo ✓ 应用已正常关闭
)

echo.
pause
```

### application.yml（Spring Boot配置）

```yaml
# OCR配置
ocr:
  enabled: ${ENABLE_OCR:false}
  tessdata-path: ${TESSDATA_PREFIX:}
  language: ${OCR_LANGUAGE:chi_sim+eng}
  
# 日志配置
logging:
  level:
    top.yumbo.ai.rag.impl.parser.image: INFO
```

## 🧪 测试OCR是否生效

### 1. 查看启动日志

正确配置后，启动日志应显示：

```
✅ 选择图片处理策略: Tesseract OCR
🔍 OCR配置:
  ├─ ENABLE_OCR: true
  ├─ TESSDATA_PREFIX: D:\tessdata
  └─ OCR_LANGUAGE: chi_sim+eng
```

### 2. 上传测试文档

上传一个包含图片的PPTX文件，查看日志：

```
📷 提取图片: slide1_image1.png (125KB)
✅ OCR提取文字 [slide1_image1.png]: 245 字符
✅ 图片内容提取成功: slide1_image1.png -> 245 字符
```

### 3. 预期输出格式

**之前（占位符）**：
```
【图片内容】
[图片1: slide1_image1.png - 未提取内容]
```

**现在（OCR识别）**：
```
【图片内容】
=== 图片: slide1_image1.png ===
节约用水从我做起
保护水资源人人有责
...（识别的文字内容）
=== /图片 ===
```

## 🐛 故障排除

### 问题1：提示"OCR不可用"

**可能原因**：
- 环境变量未设置
- Tesseract 未安装
- tessdata 路径错误

**解决方法**：
```powershell
# 检查环境变量
echo $env:ENABLE_OCR
echo $env:TESSDATA_PREFIX

# 检查 Tesseract 安装
tesseract --version

# 检查 tessdata 文件
ls "$env:TESSDATA_PREFIX"
```

### 问题2：识别率低

**可能原因**：
- 图片质量差
- 语言包不匹配
- 图片包含复杂背景

**解决方法**：
1. 使用高清图片
2. 安装正确的语言包
3. 使用多语言组合：`chi_sim+eng`

### 问题3：识别速度慢

**可能原因**：
- 图片过大
- 使用多个语言包

**优化方法**：
1. 限制图片尺寸
2. 只使用必要的语言包
3. 考虑使用异步处理

## 🎨 进阶配置

### 多语言识别

```properties
# 同时识别中文、英文、日文
OCR_LANGUAGE=chi_sim+eng+jpn
```

### 自定义 Tesseract 参数

修改 `TesseractOCRStrategy.java`：

```java
tesseract.setLanguage(language);
tesseract.setPageSegMode(1); // 自动页面分割
tesseract.setOcrEngineMode(TessOcrEngineMode.LSTM_ONLY); // 使用LSTM引擎
```

### 性能优化

```java
// 预处理图片（提高识别率）
BufferedImage preprocessed = preprocessImage(image);

// 并行处理多张图片
ExecutorService executor = Executors.newFixedThreadPool(4);
```

## 📚 相关文件

- `TesseractOCRStrategy.java` - OCR策略实现
- `SmartImageExtractor.java` - 智能图片提取器
- `PlaceholderImageStrategy.java` - 占位符策略（默认）
- `OfficeImageExtractor.java` - Office文档图片提取
- `TikaDocumentParser.java` - 文档解析器

## 🔗 有用链接

- [Tesseract 官方文档](https://github.com/tesseract-ocr/tesseract)
- [Tesseract Windows安装](https://github.com/UB-Mannheim/tesseract/wiki)
- [Tess4J (Java Wrapper)](http://tess4j.sourceforge.net/)
- [语言包下载](https://github.com/tesseract-ocr/tessdata)

---

**更新日期**: 2025-11-28  
**版本**: 1.0.0  
**作者**: AI Reviewer Team

