<div align="center">
  <img src="docs/images/logo-banner.svg" alt="AI Reviewer Base File RAG" width="400"/>
</div>
<div align="center">

**🚀 可插拔 AI 引擎架构的智能 RAG 系统 | 企业级文档检索与分析框架**

[![Version](https://img.shields.io/badge/version-2.0-blue.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag/actions)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE.txt)
[![Lucene](https://img.shields.io/badge/Lucene-9.9.1-red.svg)](https://lucene.apache.org/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag/pulls)

[English](README_EN.md) | 简体中文

[快速开始](#-快速开始) • [特性](#-核心特性) • [架构](#-系统架构) • [文档](#-详细文档) • [FAQ](#-常见问题)

</div>

---

## 📖 项目简介

**AI Reviewer Base File RAG** 是一个采用**可插拔 AI 引擎架构**的智能 RAG（Retrieval-Augmented Generation）系统，支持在文档分块、向量嵌入、检索策略、问答生成、图片理解等关键环节灵活切换不同的 AI 能力提供者。

> 💡 **项目类型**: 企业级 RAG 框架 / Spring Boot Starter  
> 🎯 **v2.0 核心升级**: 可插拔 AI 引擎 + 智能策略调度 + 多文档联合分析

### 💡 核心价值

| 价值 | 说明 |
|------|------|
| 🔌 **可插拔架构** | 所有 AI 组件可自由切换，适配不同场景需求 |
| 💰 **成本可控** | 支持本地模型，零 API 费用部署方案 |
| 🔒 **隐私保护** | 数据完全本地化，支持内网离线部署 |
| 🧠 **越用越智能** | 反馈闭环 + 知识积累，精度持续提升 |

---

## ✨ 核心特性

### 🔌 可插拔 AI 引擎架构（v2.0）

系统在多个关键环节支持灵活切换不同的 AI 能力提供者：

| 环节 | 可选引擎 | 特点 |
|------|---------|------|
| 🧩 **文档分块** | ONNX 本地 / Ollama / 在线 LLM | 基于 PPL 的智能语义边界检测 |
| 📊 **向量嵌入** | BGE-Base-ZH / BGE-M3 / 其他 ONNX | 国产模型优先，本地推理 |
| 🎯 **文档重排** | PPL Rerank (ONNX/Ollama/OpenAI) | 困惑度二次排序，提升精度 |
| 🤖 **问答生成** | DeepSeek / OpenAI / Qwen / Ollama | 成本与质量自由平衡 |
| 🖼️ **图片理解** | Qwen-VL / GPT-4o / Ollama Vision | 多模态文档支持 |
| 🔍 **检索策略** | Hybrid / Keyword / Vector | 智能策略调度器自动选择 |
| 📑 **多文档分析** | 并行摘要 / 结构对比 / 问题导向 / 实体关联 | 意图感知 + 策略组合 |

### 🎯 智能检索策略框架

```
┌─────────────────────────────────────────────────────────────────┐
│                SearchStrategyDispatcher 调度器                   │
├─────────────────────────────────────────────────────────────────┤
│  策略评估 → 自动选择最佳策略 → 执行检索 → 失败自动降级          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   hybrid    │  │   keyword   │  │   vector    │             │
│  │  混合检索   │  │ 关键词检索  │  │  向量检索   │             │
│  │ (默认,高优) │  │ (精确匹配)  │  │ (语义相似)  │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

- **3 种检索策略**：hybrid（混合）、keyword（关键词）、vector（向量）
- **智能调度**：根据查询特征自动评估策略适用性（0-100分）
- **自动降级**：策略失败时自动切换到默认策略
- **可扩展**：支持运行时动态注册新策略

### 🧠 多文档智能分析框架

```
┌─────────────────────────────────────────────────────────────────┐
│                  StrategyDispatcher 智能调度器                   │
├─────────────────────────────────────────────────────────────────┤
│  意图分析 → 策略评估 → 选择/组合 → 执行分析 → 合并结果          │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │ parallel-      │  │ structured-    │  │ question-      │    │
│  │ summary        │  │ compare        │  │ driven         │    │
│  │ 并行摘要       │  │ 结构化对比     │  │ 问题导向       │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│  ┌────────────────┐                                             │
│  │ entity-        │  支持策略组合执行，结果自动合并              │
│  │ relation       │                                             │
│  │ 实体关联       │                                             │
│  └────────────────┘                                             │
└─────────────────────────────────────────────────────────────────┘
```

**4 种分析策略**（已实现）：
| 策略 | 适用场景 | Token 成本 |
|------|---------|-----------|
| `parallel-summary` | 快速总结、综合概览 | 中 |
| `structured-compare` | 方案对比、优缺点分析 | 中 |
| `question-driven` | 精确查询、找答案 | 低 |
| `entity-relation` | 关联分析、因果追溯 | 中 |

### 🔀 混合检索与评分融合

```
┌─────────────────────────────────────────────────────────────────┐
│                     ScoreFusionService 评分融合                  │
├─────────────────────────────────────────────────────────────────┤
│  finalScore = Σ(contributor.weight × contributor.score)         │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Lucene       │  │ Vector       │  │ Feedback     │          │
│  │ Contributor  │  │ Contributor  │  │ Contributor  │          │
│  │ 权重: 0.3    │  │ 权重: 0.7    │  │ 权重: 0.2    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

- **Lucene 关键词检索**：BM25 算法，快速粗筛
- **向量语义检索**：BGE 模型嵌入，语义精排
- **反馈权重调整**：用户评分影响文档排名
- **PPL Rerank**：困惑度二次排序（可选）

### 📊 反馈优化闭环

```
用户提问 → RAG检索 → LLM生成 → 用户反馈
                                    ↓
              ┌─────────────────────┴─────────────────────┐
              ↓                     ↓                     ↓
        动态权重调整          高分问答归档          相似问题索引
              ↓                     ↓                     ↓
        好文档自动置顶      自动加入知识库        复用历史高分答案
```

**效果**：减少约 50% 的交互次数达到满意答案

### ⚡ PPL 智能分块

基于困惑度（Perplexity）的语义边界检测：

```
输入文档 → 分句 → 计算每句 PPL → 检测突变点 → 语义分块
                        ↓
          PPL: [12.5, 15.2, 45.8, 18.3, ...]
                            ↑
                    突变！在此切分
```

| 引擎 | 速度 | 精度 | 成本 |
|------|------|------|------|
| ONNX (qwen2.5-0.5b) | ⚡快 | 中 | 免费 |
| Ollama (qwen2.5:0.5b) | ⚡快 | 中 | 免费 |
| OpenAI API | 慢 | 高 | 收费 |
| LLM 直接切分 | 慢 | 最高 | 收费 |

### 🖼️ 多模态图片理解

| 模型 | 精度 | 成本 | 适用场景 |
|------|------|------|----------|
| **Qwen-VL-Plus** | 高 | 低 | 中文优化 ✅推荐 |
| **GPT-4o Vision** | 极高 | 高 | 复杂图表 |
| **Ollama llava** | 中 | 免费 | 离线部署 |

三种处理策略：`placeholder` / `vision-llm` / `llm-client`

### 📄 多模态文档支持

- 📄 **文本**：TXT, MD, CSV, JSON, XML, HTML
- 📊 **办公**：PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX
- 🖼️ **图片**：PNG, JPG, JPEG, GIF, BMP（Vision LLM）
- 🔤 **代码**：Java, Python, JavaScript, Go, C++
- 📦 **35+ 格式**：自动识别，智能解析

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     用户界面 (React)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ 智能问答 │ │ 文档管理 │ │ AI分析   │ │ 统计反馈 │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot 后端服务                           │
├─────────────────────────────────────────────────────────────────┤
│  KnowledgeQAController  │  DocumentController  │  FeedbackCtrl  │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    策略调度层 (可插拔)                           │
├─────────────────────────────────────────────────────────────────┤
│  SearchStrategyDispatcher     │     StrategyDispatcher          │
│  (检索策略调度)               │     (多文档分析策略调度)         │
│  • HybridSearchStrategy       │     • ParallelSummaryStrategy   │
│  • KeywordSearchStrategy      │     • StructuredCompareStrategy │
│  • VectorSearchStrategy       │     • QuestionDrivenStrategy    │
│                               │     • EntityRelationStrategy    │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      核心引擎层                                  │
├─────────────────────────────────────────────────────────────────┤
│  LocalFileRAG    │  PPLServiceFacade  │  LLMClient              │
│  (Lucene+向量)   │  (ONNX/Ollama/API) │  (DeepSeek/OpenAI/...)  │
├─────────────────────────────────────────────────────────────────┤
│  EmbeddingEngine │  VisionLLMStrategy │  DocumentParser         │
│  (BGE-Base-ZH)   │  (Qwen-VL/GPT-4o)  │  (Tika/POI)             │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                       存储层                                     │
│  ./data/documents  │  ./data/vector-index  │  ./data/feedback   │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⚡ 性能指标

| 指标 | 性能表现 | 说明 |
|------|----------|------|
| **索引速度** | 1000+ 文档/分钟 | 依赖文档大小 |
| **检索响应** | < 100ms | P95，1万文档规模 |
| **内存占用** | 256MB - 2GB | 随索引规模线性增长 |
| **并发处理** | 200+ QPS | 单实例，4核8G配置 |

---

## 📦 前置依赖

### 必需
- **Java 11+**（推荐 Java 17）
- **Maven 3.6+**

### 可选
- **Ollama**（本地 PPL 分块 / Vision LLM）
  ```bash
  ollama pull qwen2.5:0.5b   # PPL 分块
  ollama pull llava:7b       # 图片理解
  ```

- **向量模型**（已内置 BGE-Base-ZH，开箱即用）

---

## 🚀 快速开始

### 1️⃣ 添加依赖

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>2.0</version>
</dependency>
```

### 2️⃣ 配置

```yaml
knowledge:
  qa:
    llm:
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
    
    ppl:
      default-provider: onnx  # 或 ollama / openai
      
    vector-search:
      enabled: true
      lucene-weight: 0.3
      vector-weight: 0.7
      
    image-processing:
      strategy: vision-llm
      vision-llm:
        model: qwen-vl-plus
```

### 3️⃣ 启动

```bash
export AI_API_KEY="your-api-key"
mvn spring-boot:run
```

---

## 📚 详细文档

| 文档 | 说明 |
|------|------|
| [RAG系统架构与工作流程](md/20251207-RAG系统整体架构与工作流程.md) | 完整的系统设计文档 |
| [多文档联合分析策略](md/20251206-多文档联合分析策略方案.md) | 策略框架详解 |
| [PPL技术分析](md/20251204-PPL技术分析与对比.md) | PPL 分块原理 |
| [Vision LLM配置](docs/VISION_LLM_UNIVERSAL_CONFIG.md) | 图片理解配置 |
| [LightRAG 技术分析](docs/LIGHTRAG_ANALYSIS.md) | 图增强 RAG 集成方案 |

---

## 🗺️ 发展路线图

### ✅ v1.0 - 2024 Q4
- ✅ Lucene 核心检索引擎
- ✅ 35+ 文档格式支持
- ✅ 多 LLM 集成

### ✅ v2.0 (当前) - 2025 Q1
- ✅ **可插拔 AI 引擎架构**
- ✅ **检索策略框架** (3种策略)
- ✅ **多文档分析框架** (4种策略)
- ✅ **评分融合服务** (3种贡献者)
- ✅ PPL 智能分块 (ONNX/Ollama/OpenAI)
- ✅ Vision LLM 图片理解
- ✅ 反馈闭环优化
- ✅ 检索缓存

### 🚀 v3.0 (规划) - 2025 Q2
- 📋 **LightRAG 集成** - 轻量级图增强 RAG
- 📋 分布式索引
- 📋 多租户架构
- 📋 Docker 镜像
- 📋 WebSocket 实时推送

---

## 📝 更新日志

### v2.0.0 (2025-12-07) - 当前版本

#### 🚀 重大升级
- **可插拔 AI 引擎架构**：所有关键环节支持引擎切换
- **检索策略框架**：SearchStrategyDispatcher + 3种策略
- **多文档分析框架**：StrategyDispatcher + 4种分析策略
- **评分融合服务**：ScoreFusionService + 3种贡献者

#### 🔧 功能完善
- 混合检索权重可配置
- PPL Rerank 引擎可切换
- Vision LLM 多模型支持
- 反馈权重应用到检索
- 检索结果缓存

#### 🗑️ 移除
- 移除 Tesseract OCR（替换为 Vision LLM）
- 移除 Netty HTTP 服务器代码

### v1.0.0 (2025-11-22)
- 🎉 首次发布

---

## 📄 许可证

**Apache License 2.0** - 可安全用于商业用途

---

## 🌟 致谢

- [Apache Lucene](https://lucene.apache.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [ONNX Runtime](https://onnxruntime.ai/)
- [Ollama](https://ollama.com/)

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐ Star！**

Made with ❤️ by [AI Reviewer Team](https://github.com/jinhua10)

</div>
