# Release 发布包说明文档

> **版本**: v2.0.0  
> **更新日期**: 2025-12-07  
> **适用场景**: 生产环境部署

**语言**: [English](README_EN.md) | [中文](README.md)

---

## 📦 目录结构

```
release/
├── ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar  # 主程序
├── config/                                                   # 配置目录
│   └── application.yml                                      # 主配置文件
├── data/                                                    # 数据目录
│   ├── documents/                                          # 文档存储（用户上传）
│   ├── knowledge-base/                                     # 知识库数据
│   │   ├── cache/                                         # 缓存目录
│   │   ├── documents/                                     # 文档存储（按日期分类）
│   │   ├── index/                                         # Lucene 索引
│   │   │   └── lucene-index/                             # 索引文件
│   │   └── metadata/                                      # 元数据
│   │       └── metadata.db                               # 元数据数据库
│   ├── rag/                                               # RAG 检索数据
│   │   ├── cache/                                        # RAG 缓存
│   │   ├── documents/                                    # RAG 文档
│   │   ├── index/                                        # RAG 索引
│   │   │   └── lucene-index/                            # Lucene 索引
│   │   └── metadata/                                     # RAG 元数据
│   │       └── metadata.db                              # 元数据数据库
│   ├── vector-index/                                      # 向量索引
│   │   └── vector-index/                                 # 向量数据
│   │       └── vectors.dat                              # 向量文件
│   ├── feedback/                                          # 用户反馈（运行时生成）
│   ├── llm-results/                                       # LLM 分析结果（运行时生成）
│   ├── qa-records/                                        # 问答记录（运行时生成）
│   └── hope/                                              # HOPE 三层记忆（运行时生成）
│       ├── permanent/                                     # 低频层 - 永久知识
│       ├── ordinary/                                      # 中频层 - 近期知识
│       └── high-frequency/                                # 高频层 - 实时上下文（内存）
├── models/                                                  # AI 模型文件
│   ├── bge-base-zh/                                        # 向量模型（已内置）
│   └── qwen2.5-0.5b-instruct/                             # PPL 模型（可选）
├── logs/                                                    # 日志目录
├── temp/                                                    # 临时文件
├── scripts/                                                 # 工具脚本
│   ├── download_embedding_model.py                         # 下载向量模型
│   ├── download_qwen_onnx.py                              # 下载 PPL 模型
│   └── convert_bge_to_onnx.py                             # BGE 转 ONNX
├── start.bat                                                # Windows 启动脚本
├── stop.bat                                                 # Windows 停止脚本
├── fix-lock.bat                                            # 修复端口占用
├── PROMPT_QUICK_START.md                                   # 提示词配置指南
├── PROMPT_TEMPLATE_CONFIG_EN.md                            # 提示词配置（英文）
└── jdk download.txt                                        # JDK 下载链接
```

---

## 📄 文件说明

### 1. 核心程序文件

#### `ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar`
**主程序 JAR 包**

- **类型**: 可执行 JAR（包含所有依赖）
- **启动方式**: 
  ```bash
  # Windows
  start.bat
  
  # Linux/Mac
  java -jar ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar
  ```
- **说明**: 包含完整的 Spring Boot 应用和所有依赖库
- **大小**: 约 150MB

---

### 2. 配置目录 (`config/`)

#### `application.yml`
**主配置文件**

配置项分类：

| 配置节 | 说明 | 关键配置 |
|--------|------|---------|
| `server` | 服务器配置 | 端口（8080）、Tomcat 线程池 |
| `spring` | Spring 框架配置 | 国际化、文件上传（最大 100MB） |
| `knowledge.qa.knowledge-base` | 知识库配置 | 文档路径、索引模式 |
| `knowledge.qa.vector-search` | 向量检索配置 | 是否启用、模型路径、权重 |
| `knowledge.qa.llm` | LLM 配置 | API Key、模型名称、Prompt 模板 |
| `knowledge.qa.ppl` | PPL 服务配置 | 启用状态、提供商（ONNX/Ollama/OpenAI） |
| `knowledge.qa.image-processing` | 图像理解配置 | Vision LLM 策略、模型选择 |
| `knowledge.qa.hope` | **HOPE 三层记忆** | 启用状态、三层配置 |
| `knowledge.qa.feedback` | 反馈系统配置 | 权重调整、自动应用 |

**重点配置**：
```yaml
knowledge:
  qa:
    llm:
      api-key: ${AI_API_KEY}      # 从环境变量读取
      model: deepseek-chat         # 推荐：性价比高
    
    hope:
      enabled: true                # HOPE 三层记忆（v2.0 新增）
      
    vector-search:
      enabled: true                # 向量检索
      lucene-weight: 0.3          # Lucene 权重
      vector-weight: 0.7          # 向量权重
```

---

### 3. 数据目录 (`data/`)

#### `documents/`
**用户上传的文档存储目录**

- 支持格式：PDF, DOCX, XLSX, PPTX, TXT, MD 等 35+ 格式
- 自动检测文件变更并建议增量索引
- 路径可在 `application.yml` 中配置

#### `knowledge-base/`
**知识库索引数据**

```
knowledge-base/
├── cache/                    # 查询缓存
├── documents/                # 文档存储（按日期分类 YYYY/MM/DD）
│   └── 2025/11/23/          # 示例：2025年11月23日的文档
│       └── *.txt.gz         # 压缩的文档内容
├── index/                   # 索引目录
│   └── lucene-index/        # Lucene 全文索引
│       ├── segments_*       # 索引段文件
│       ├── *.cfe/.cfs       # 索引数据文件
│       └── write.lock       # 写锁文件
└── metadata/                # 元数据
    └── metadata.db          # 文档元数据数据库
```

**说明**：
- 文档按日期归档，便于管理
- Lucene 索引支持全文检索
- 元数据包含文件信息、索引状态等

#### `rag/`
**RAG 检索专用数据**

```
rag/
├── cache/                   # RAG 查询缓存
├── documents/               # RAG 处理后的文档
├── index/                   # RAG 索引
│   └── lucene-index/        # Lucene 索引
└── metadata/                # RAG 元数据
    └── metadata.db          # 元数据数据库
```

**说明**：
- 独立的 RAG 数据存储
- 与主知识库分离，便于管理

#### `vector-index/`
**向量索引存储**

```
vector-index/
└── vector-index/
    └── vectors.dat          # 向量数据文件
```

**说明**：
- BGE 模型生成的文档向量
- 向量维度：768（BGE-Base-ZH）
- 支持语义相似度搜索
- 二进制格式存储，高效检索

#### `feedback/`
**用户反馈数据**（运行时生成）

- QA 记录（问题、答案、来源）
- 用户评分（1-5 星）
- 文档权重调整记录
- 高分问答归档

#### `llm-results/`
**LLM 分析结果**（运行时生成）

- AI 分析历史记录
- Markdown 格式分析报告
- PDF 导出结果
- 支持多文档联合分析结果

#### `qa-records/`
**问答记录存档**（运行时生成）

- 所有问答会话记录
- 用户反馈记录
- 用于相似问题推荐
- 用于 HOPE 学习

#### `hope/`
**HOPE 三层记忆架构存储**（v2.0 新增，运行时生成）

| 子目录 | 说明 | 数据类型 | 持久化 |
|--------|------|---------|--------|
| `permanent/` | 低频层 - 永久知识 | 技能模板、确定性知识 | ✅ JSON |
| `ordinary/` | 中频层 - 近期知识 | 30 天内高分问答 | ✅ JSON |
| `high-frequency/` | 高频层 - 实时上下文 | 会话记忆 | ❌ 内存 |

**说明**：
- 低频层和中频层会持久化到磁盘
- 高频层仅保存在内存中，重启后清空
- 自动学习和晋升机制

---

### 4. 模型目录 (`models/`)

#### `bge-base-zh/`
**BGE 向量模型（已内置）**

- **用途**: 文档向量化、语义检索
- **提供商**: 智源研究院
- **维度**: 768
- **文件**: 
  - `model.onnx` (约 400MB)
  - `tokenizer.json`
  - `config.json`
- **状态**: ✅ 开箱即用

#### `qwen2.5-0.5b-instruct/`
**Qwen PPL 模型（可选）**

- **用途**: PPL 智能分块、文档重排序
- **提供商**: 阿里通义千问
- **文件**: 
  - `model.onnx` (约 500MB)
  - `tokenizer.json`
- **下载**: 运行 `scripts/download_qwen_onnx.py`
- **替代方案**: 使用 Ollama 更简单

---

### 5. 启动脚本

#### `start.bat`
**Windows 启动脚本**

**功能**：
- 设置 Java 环境变量（UTF-8 编码）
- 配置 JVM 内存（-Xms512m -Xmx2g）
- 读取环境变量 `AI_API_KEY`
- 启动 Spring Boot 应用

**使用方法**：
```bash
# 1. 设置环境变量（可选，也可在 application.yml 中配置）
set AI_API_KEY=your-deepseek-api-key

# 2. 双击或命令行运行
start.bat

# 3. 访问
http://localhost:8080
```

**自定义内存**：
修改脚本中的 `-Xms512m -Xmx2g`（最小 512MB，最大 2GB）

#### `stop.bat`
**Windows 停止脚本**

- 查找占用 8080 端口的进程
- 终止 Java 进程
- 清理临时文件

#### `fix-lock.bat`
**修复端口占用脚本**

- **场景**: 程序异常退出后 8080 端口被占用
- **功能**: 强制释放端口
- **使用**: 启动失败时先运行此脚本

---

### 6. 工具脚本 (`scripts/`)

#### `download_embedding_model.py`
**下载向量模型脚本**

```bash
# 下载 BGE-Base-ZH 模型
python scripts/download_embedding_model.py --model bge-base-zh

# 下载 BGE-Large-ZH 模型（更高精度）
python scripts/download_embedding_model.py --model bge-large-zh
```

**支持模型**：
- `bge-base-zh` (推荐，400MB)
- `bge-large-zh` (高精度，1.5GB)
- `bge-m3` (多语言，2GB)

#### `download_qwen_onnx.py`
**下载 Qwen PPL 模型脚本**

```bash
# 下载 Qwen2.5-0.5B（推荐，轻量）
python scripts/download_qwen_onnx.py --model qwen2.5-0.5b-instruct

# 下载 Qwen2.5-1.5B（平衡）
python scripts/download_qwen_onnx.py --model qwen2.5-1.5b-instruct
```

**注意**: 也可以使用 Ollama 替代，更简单：
```bash
ollama pull qwen2.5:0.5b
```

#### `convert_bge_to_onnx.py`
**BGE 模型转 ONNX 格式**

- **用途**: 将 HuggingFace 模型转为 ONNX（仅开发使用）
- **说明**: 发布包中已包含 ONNX 模型，一般用户无需运行

---

### 7. 文档

#### `PROMPT_QUICK_START.md`
**Prompt 提示词配置快速指南（中文）**

- 如何自定义 AI 回答风格
- Prompt 模板配置示例
- 占位符说明（`{question}`, `{context}`）

#### `PROMPT_TEMPLATE_CONFIG_EN.md`
**Prompt Template Configuration Guide (English)**

- English version of prompt customization guide
- Template examples for different scenarios

#### `jdk download.txt`
**JDK 下载链接**

- Oracle JDK 11/17 下载地址
- OpenJDK 下载地址
- 安装说明

---

## 🚀 快速启动指南

### 最小化配置（3 步启动）

#### 1️⃣ 配置 LLM API Key

**方式 1**：环境变量（推荐）
```bash
# Windows CMD
set AI_API_KEY=your-deepseek-api-key

# Windows PowerShell
$env:AI_API_KEY="your-deepseek-api-key"

# Linux/Mac
export AI_API_KEY="your-deepseek-api-key"
```

**方式 2**：修改 `config/application.yml`
```yaml
knowledge:
  qa:
    llm:
      api-key: "sk-xxxxxxxxxxxx"  # 直接填写（不推荐，安全性低）
```

#### 2️⃣ 启动应用

```bash
# Windows
start.bat

# Linux/Mac
java -jar ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar
```

#### 3️⃣ 访问系统

打开浏览器访问：**http://localhost:8080**

---

## 🔧 配置建议

### 生产环境推荐配置

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| **LLM 模型** | `deepseek-chat` | 性价比高，中文优秀 |
| **向量检索** | `enabled: true` | 提升语义理解能力 |
| **HOPE 架构** | `enabled: true` | 减少 LLM 调用，越用越快 |
| **PPL 提供商** | `ollama` | 本地化，无需下载大模型 |
| **Vision LLM** | `qwen-vl-plus` | 处理图片和 PPT |
| **JVM 内存** | `-Xmx2g` | 2GB 足够 10K 文档 |

### 成本优化配置

**方案 1**：纯本地化（零 API 费用）
```yaml
knowledge:
  qa:
    llm:
      provider: ollama
      model: qwen2.5:7b
    ppl:
      default-provider: ollama
    vector-search:
      enabled: true
```

**方案 2**：混合模式（低成本）
```yaml
knowledge:
  qa:
    llm:
      provider: deepseek  # 在线 API（便宜）
    ppl:
      default-provider: ollama  # 本地
    vector-search:
      enabled: true  # 本地
```

---

## 📊 HOPE 三层记忆架构说明

### 什么是 HOPE？

HOPE（Hierarchical Optimized Persistent Engine）是 v2.0 的核心升级，参考 Google HOPE 论文设计的三层记忆架构：

```
┌──────────────────────────────────────────────────┐
│  高频层 (High-frequency)  - 实时会话上下文        │
│  • 多轮对话记忆                                   │
│  • 临时定义和更正                                 │
│  • 会话超时：30 分钟                              │
└──────────────────────────────────────────────────┘
                    ↓ 高分晋升
┌──────────────────────────────────────────────────┐
│  中频层 (Ordinary)  - 近期高分问答               │
│  • 30 天内高评分问答                             │
│  • 相似度 ≥ 0.95 直接使用                        │
│  • 相似度 ≥ 0.7 作为参考                         │
└──────────────────────────────────────────────────┘
                    ↓ 持续验证晋升
┌──────────────────────────────────────────────────┐
│  低频层 (Permanent)  - 永久技能知识              │
│  • 技能模板（如"如何配置"）                       │
│  • 确定性知识（如"版本号"）                       │
│  • 置信度 ≥ 0.9 可直接回答                       │
└──────────────────────────────────────────────────┘
```

### HOPE 效果

- ⚡ **响应提速**: 简单问题 < 100ms（无需 LLM）
- 💰 **成本降低**: LLM 节省率 20-30%
- 🎯 **精度提升**: 自动学习用户偏好
- 🧠 **越用越智能**: 知识自动积累晋升

### 监控 HOPE 状态

访问监控端点：
- **状态**: http://localhost:8080/api/hope/status
- **仪表盘**: http://localhost:8080/api/hope/dashboard
- **指标**: http://localhost:8080/api/hope/metrics

---

## 🐛 常见问题

### 1. 启动失败：端口 8080 被占用

**解决方案**：
```bash
# 运行修复脚本
fix-lock.bat

# 或手动修改端口（config/application.yml）
server:
  port: 8081
```

### 2. 向量检索不可用

**原因**: 模型文件缺失或损坏

**解决方案**：
```bash
# 重新下载模型
python scripts/download_embedding_model.py --model bge-base-zh

# 或禁用向量检索
knowledge:
  qa:
    vector-search:
      enabled: false
```

### 3. PPL 功能不可用

**解决方案 1**：使用 Ollama（推荐）
```bash
ollama pull qwen2.5:0.5b
```

**解决方案 2**：下载 ONNX 模型
```bash
python scripts/download_qwen_onnx.py --model qwen2.5-0.5b-instruct
```

**解决方案 3**：禁用 PPL
```yaml
knowledge:
  qa:
    ppl:
      enabled: false
```

### 4. 内存溢出（OOM）

**原因**: JVM 内存不足

**解决方案**：
编辑 `start.bat`，增加内存：
```bat
set JAVA_OPTS=%JAVA_OPTS% -Xms1g -Xmx4g
```

---

## 📞 技术支持

- **GitHub Issues**: [提交问题](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)
- **文档**: [完整文档](../README.md)
- **更新日志**: [CHANGELOG](../README.md#-changelog)

---

## 📝 更新日志

### v2.0.0 (2025-12-07)
- ✨ 新增 HOPE 三层记忆架构
- ✨ 新增多文档联合分析框架
- ✨ 新增智能搜索策略调度器
- 🔧 配置文件优化，支持更多自定义

### v1.0.0 (2025-11-22)
- 🎉 初始发布版本

---

<div align="center">

**AI Reviewer Base File RAG v2.0**

Made with ❤️ by [AI Reviewer Team](https://github.com/jinhua10)

</div>

