<div align="center">
  <img src="docs/images/logo-banner.svg" alt="AI Reviewer Base File RAG" width="400"/>
</div>
<div align="center">

**🚀 Pluggable AI Engine Architecture RAG System | Enterprise Document Retrieval & Analysis Framework**

[![Version](https://img.shields.io/badge/version-2.0-blue.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag/actions)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE.txt)
[![Lucene](https://img.shields.io/badge/Lucene-9.9.1-red.svg)](https://lucene.apache.org/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag/pulls)

English | [简体中文](README.md)

[Quick Start](#-quick-start) • [Features](#-key-features) • [Architecture](#-system-architecture) • [Documentation](#-documentation) • [FAQ](#-faq)

</div>

---

## 📖 Introduction

**AI Reviewer Base File RAG** is an intelligent RAG (Retrieval-Augmented Generation) system with **pluggable AI engine architecture**, supporting flexible switching of different AI capability providers across document chunking, vector embedding, retrieval strategies, Q&A generation, and image understanding.

> 💡 **Project Type**: Enterprise RAG Framework / Spring Boot Starter  
> 🎯 **v2.0 Core Upgrade**: Pluggable AI Engines + Intelligent Strategy Dispatch + Multi-Document Joint Analysis

### 💡 Core Values

| Value | Description |
|-------|-------------|
| 🔌 **Pluggable Architecture** | All AI components can be freely switched to adapt to different scenarios |
| 💰 **Cost Controllable** | Support local models, zero API fee deployment |
| 🔒 **Privacy Protection** | Fully localized data, support offline intranet deployment |
| 🧠 **Smarter with Use** | Feedback loop + knowledge accumulation, continuous precision improvement |

---

## ✨ Key Features

### 🔌 Pluggable AI Engine Architecture (v2.0)

System supports flexible switching of AI capability providers at multiple key stages:

| Stage | Available Engines | Features |
|-------|-------------------|----------|
| 🧩 **Doc Chunking** | ONNX Local / Ollama / Online LLM | PPL-based intelligent semantic boundary detection |
| 📊 **Vector Embedding** | BGE-Base-ZH / BGE-M3 / Other ONNX | Chinese models first, local inference |
| 🎯 **Doc Reranking** | PPL Rerank (ONNX/Ollama/OpenAI) | Perplexity-based secondary ranking |
| 🤖 **Q&A Generation** | DeepSeek / OpenAI / Qwen / Ollama | Balance cost and quality freely |
| 🖼️ **Image Understanding** | Qwen-VL / GPT-4o / Ollama Vision | Multimodal document support |
| 🔍 **Search Strategy** | Hybrid / Keyword / Vector | Intelligent strategy dispatcher auto-selection |
| 📑 **Multi-Doc Analysis** | Parallel Summary / Structured Compare / Question-Driven / Entity-Relation | Intent-aware + strategy combination |

### 🎯 Intelligent Search Strategy Framework

```
┌─────────────────────────────────────────────────────────────────┐
│                SearchStrategyDispatcher                          │
├─────────────────────────────────────────────────────────────────┤
│  Evaluate → Select Best Strategy → Execute → Auto Fallback      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   hybrid    │  │   keyword   │  │   vector    │             │
│  │  (default)  │  │ (exact)     │  │ (semantic)  │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

- **3 Search Strategies**: hybrid, keyword, vector
- **Smart Dispatch**: Auto-evaluate strategy suitability (0-100 score)
- **Auto Fallback**: Switch to default strategy on failure
- **Extensible**: Support runtime dynamic strategy registration

### 🧠 Multi-Document Analysis Framework

```
┌─────────────────────────────────────────────────────────────────┐
│                  StrategyDispatcher                              │
├─────────────────────────────────────────────────────────────────┤
│  Intent Analysis → Evaluate → Select/Combine → Execute → Merge  │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │ parallel-      │  │ structured-    │  │ question-      │    │
│  │ summary        │  │ compare        │  │ driven         │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│  ┌────────────────┐                                             │
│  │ entity-        │  Support strategy combination execution     │
│  │ relation       │                                             │
│  └────────────────┘                                             │
└─────────────────────────────────────────────────────────────────┘
```

**4 Analysis Strategies** (Implemented):
| Strategy | Use Case | Token Cost |
|----------|----------|------------|
| `parallel-summary` | Quick summary, overview | Medium |
| `structured-compare` | Comparison, pros/cons analysis | Medium |
| `question-driven` | Precise query, find answers | Low |
| `entity-relation` | Association analysis, causal tracing | Medium |

### 🔀 Hybrid Search & Score Fusion

```
┌─────────────────────────────────────────────────────────────────┐
│                     ScoreFusionService                           │
├─────────────────────────────────────────────────────────────────┤
│  finalScore = Σ(contributor.weight × contributor.score)         │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Lucene       │  │ Vector       │  │ Feedback     │          │
│  │ Contributor  │  │ Contributor  │  │ Contributor  │          │
│  │ weight: 0.3  │  │ weight: 0.7  │  │ weight: 0.2  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

- **Lucene Keyword Search**: BM25 algorithm, fast rough filtering
- **Vector Semantic Search**: BGE model embedding, semantic refinement
- **Feedback Weight Adjustment**: User ratings affect document ranking
- **PPL Rerank**: Perplexity-based secondary sorting (optional)

### 📊 Feedback Optimization Loop

```
User Question → RAG Search → LLM Generate → User Feedback
                                    ↓
              ┌─────────────────────┴─────────────────────┐
              ↓                     ↓                     ↓
        Dynamic Weight         High-Score QA          Similar Question
        Adjustment             Archiving              Indexing
              ↓                     ↓                     ↓
        Good docs auto-        Auto add to            Reuse historical
        promoted               knowledge base         high-score answers
```

**Effect**: Reduce ~50% interactions to reach satisfactory answer

### ⚡ PPL Intelligent Chunking

Perplexity-based semantic boundary detection:

```
Input Doc → Sentence Split → Calculate PPL → Detect Spikes → Semantic Chunks
                        ↓
          PPL: [12.5, 15.2, 45.8, 18.3, ...]
                            ↑
                    Spike! Cut here
```

| Engine | Speed | Precision | Cost |
|--------|-------|-----------|------|
| ONNX (qwen2.5-0.5b) | ⚡Fast | Medium | Free |
| Ollama (qwen2.5:0.5b) | ⚡Fast | Medium | Free |
| OpenAI API | Slow | High | Paid |
| LLM Direct Split | Slow | Highest | Paid |

### 🖼️ Multimodal Image Understanding

| Model | Precision | Cost | Use Case |
|-------|-----------|------|----------|
| **Qwen-VL-Plus** | High | Low | Chinese optimized ✅Recommended |
| **GPT-4o Vision** | Very High | High | Complex charts |
| **Ollama llava** | Medium | Free | Offline deployment |

Three strategies: `placeholder` / `vision-llm` / `llm-client`

### 📄 Multimodal Document Support

- 📄 **Text**: TXT, MD, CSV, JSON, XML, HTML
- 📊 **Office**: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX
- 🖼️ **Images**: PNG, JPG, JPEG, GIF, BMP (Vision LLM)
- 🔤 **Code**: Java, Python, JavaScript, Go, C++
- 📦 **35+ Formats**: Auto-detection, smart parsing

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     User Interface (React)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Smart QA │ │ Doc Mgmt │ │ AI Anal. │ │ Feedback │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend Service                    │
├─────────────────────────────────────────────────────────────────┤
│  KnowledgeQAController  │  DocumentController  │  FeedbackCtrl  │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                    Strategy Dispatch Layer (Pluggable)           │
├─────────────────────────────────────────────────────────────────┤
│  SearchStrategyDispatcher     │     StrategyDispatcher          │
│  (Search Strategy)            │     (Multi-Doc Analysis)        │
│  • HybridSearchStrategy       │     • ParallelSummaryStrategy   │
│  • KeywordSearchStrategy      │     • StructuredCompareStrategy │
│  • VectorSearchStrategy       │     • QuestionDrivenStrategy    │
│                               │     • EntityRelationStrategy    │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      Core Engine Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  LocalFileRAG    │  PPLServiceFacade  │  LLMClient              │
│  (Lucene+Vector) │  (ONNX/Ollama/API) │  (DeepSeek/OpenAI/...)  │
├─────────────────────────────────────────────────────────────────┤
│  EmbeddingEngine │  VisionLLMStrategy │  DocumentParser         │
│  (BGE-Base-ZH)   │  (Qwen-VL/GPT-4o)  │  (Tika/POI)             │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                       Storage Layer                              │
│  ./data/documents  │  ./data/vector-index  │  ./data/feedback   │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Performance Benchmarks

| Metric | Performance | Notes |
|--------|-------------|-------|
| **Indexing Speed** | 1000+ docs/min | Depends on doc size |
| **Search Latency** | < 100ms | P95, 10K docs |
| **Memory Usage** | 256MB - 2GB | Scales linearly with index |
| **Concurrent QPS** | 200+ | Single instance, 4C8G |

---

## 📦 Prerequisites

### Required
- **Java 11+** (Java 17 recommended)
- **Maven 3.6+**

### Optional
- **Ollama** (Local PPL chunking / Vision LLM)
  ```bash
  ollama pull qwen2.5:0.5b   # PPL chunking
  ollama pull llava:7b       # Image understanding
  ```

- **Vector Model** (BGE-Base-ZH built-in, ready to use)

---

## 🚀 Quick Start

### 1️⃣ Add Dependency

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>2.0</version>
</dependency>
```

### 2️⃣ Configuration

```yaml
knowledge:
  qa:
    llm:
      api-key: ${AI_API_KEY}
      api-url: https://api.deepseek.com/v1/chat/completions
      model: deepseek-chat
    
    ppl:
      default-provider: onnx  # or ollama / openai
      
    vector-search:
      enabled: true
      lucene-weight: 0.3
      vector-weight: 0.7
      
    image-processing:
      strategy: vision-llm
      vision-llm:
        model: qwen-vl-plus
```

### 3️⃣ Start

```bash
export AI_API_KEY="your-api-key"
mvn spring-boot:run
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [RAG System Architecture](md/20251207-RAG系统整体架构与工作流程.md) | Complete system design doc |
| [Multi-Doc Analysis Strategy](md/20251206-多文档联合分析策略方案.md) | Strategy framework details |
| [PPL Technical Analysis](md/20251204-PPL技术分析与对比.md) | PPL chunking principles |
| [Vision LLM Config](docs/VISION_LLM_UNIVERSAL_CONFIG.md) | Image understanding config |

---

## 🗺️ Roadmap

### ✅ v1.0 - 2024 Q4
- ✅ Lucene core search engine
- ✅ 35+ document format support
- ✅ Multi-LLM integration

### ✅ v2.0 (Current) - 2025 Q1
- ✅ **Pluggable AI Engine Architecture**
- ✅ **Search Strategy Framework** (3 strategies)
- ✅ **Multi-Doc Analysis Framework** (4 strategies)
- ✅ **Score Fusion Service** (3 contributors)
- ✅ PPL Smart Chunking (ONNX/Ollama/OpenAI)
- ✅ Vision LLM Image Understanding
- ✅ Feedback Loop Optimization
- ✅ Search Cache

### 🚀 v3.0 (Planned) - 2025 Q2
- 📋 **LightRAG Integration** - Lightweight graph-enhanced RAG
- 📋 Distributed indexing
- 📋 Multi-tenancy architecture
- 📋 Docker image
- 📋 WebSocket real-time push

---

## 📝 Changelog

### v2.0.0 (2025-12-07) - Current Version

#### 🚀 Major Upgrades
- **Pluggable AI Engine Architecture**: All key stages support engine switching
- **Search Strategy Framework**: SearchStrategyDispatcher + 3 strategies
- **Multi-Doc Analysis Framework**: StrategyDispatcher + 4 analysis strategies
- **Score Fusion Service**: ScoreFusionService + 3 contributors

#### 🔧 Improvements
- Configurable hybrid search weights
- Switchable PPL Rerank engines
- Multi-model Vision LLM support
- Feedback weight applied to search
- Search result caching

#### 🗑️ Removed
- Removed Tesseract OCR (replaced by Vision LLM)
- Removed Netty HTTP server code

### v1.0.0 (2025-11-22)
- 🎉 Initial release

---

## 📄 License

**Apache License 2.0** - Safe for commercial use

---

## 🌟 Acknowledgments

- [Apache Lucene](https://lucene.apache.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [ONNX Runtime](https://onnxruntime.ai/)
- [Ollama](https://ollama.com/)

---

<div align="center">

**If this project helps you, please give us a ⭐ Star!**

Made with ❤️ by [AI Reviewer Team](https://github.com/jinhua10)

</div>
