# Release Package Documentation

> **Version**: v2.0.0  
> **Updated**: 2025-12-07  
> **Scenario**: Production Deployment

**Language**: [English](README_EN.md) | [中文](README.md)

---

## 📦 Directory Structure

```
release/
├── ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar  # Main Application
├── config/                                                   # Configuration
│   └── application.yml                                      # Main Config File
├── data/                                                    # Data Directory
│   ├── documents/                                          # Document Storage (User Uploads)
│   ├── knowledge-base/                                     # Knowledge Base Data
│   │   ├── cache/                                         # Cache Directory
│   │   ├── documents/                                     # Documents (Date-based)
│   │   ├── index/                                         # Lucene Index
│   │   │   └── lucene-index/                             # Index Files
│   │   └── metadata/                                      # Metadata
│   │       └── metadata.db                               # Metadata Database
│   ├── rag/                                               # RAG Retrieval Data
│   │   ├── cache/                                        # RAG Cache
│   │   ├── documents/                                    # RAG Documents
│   │   ├── index/                                        # RAG Index
│   │   │   └── lucene-index/                            # Lucene Index
│   │   └── metadata/                                     # RAG Metadata
│   │       └── metadata.db                              # Metadata DB
│   ├── vector-index/                                      # Vector Index
│   │   └── vector-index/                                 # Vector Data
│   │       └── vectors.dat                              # Vector File
│   ├── feedback/                                          # User Feedback (Runtime)
│   ├── llm-results/                                       # LLM Analysis Results (Runtime)
│   ├── qa-records/                                        # QA Records (Runtime)
│   └── hope/                                              # HOPE 3-Layer Memory (Runtime)
│       ├── permanent/                                     # Low-freq Layer - Permanent
│       ├── ordinary/                                      # Mid-freq Layer - Recent
│       └── high-frequency/                                # High-freq Layer - Real-time (Memory)
├── models/                                                  # AI Model Files
│   ├── bge-base-zh/                                        # Vector Model (Built-in)
│   └── qwen2.5-0.5b-instruct/                             # PPL Model (Optional)
├── logs/                                                    # Log Directory
├── temp/                                                    # Temporary Files
├── scripts/                                                 # Utility Scripts
│   ├── download_embedding_model.py                         # Download Vector Model
│   ├── download_qwen_onnx.py                              # Download PPL Model
│   └── convert_bge_to_onnx.py                             # Convert BGE to ONNX
├── start.bat                                                # Windows Startup Script
├── stop.bat                                                 # Windows Stop Script
├── fix-lock.bat                                            # Fix Port Occupation
├── PROMPT_QUICK_START.md                                   # Prompt Config Guide (CN)
├── PROMPT_TEMPLATE_CONFIG_EN.md                            # Prompt Config Guide (EN)
└── jdk download.txt                                        # JDK Download Links
```

---

## 📄 File Descriptions

### 1. Core Application

#### `ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar`
**Main Application JAR Package**

- **Type**: Executable JAR (includes all dependencies)
- **Startup**: 
  ```bash
  # Windows
  start.bat
  
  # Linux/Mac
  java -jar ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar
  ```
- **Description**: Complete Spring Boot application with all dependency libraries
- **Size**: ~150MB

---

### 2. Configuration Directory (`config/`)

#### `application.yml`
**Main Configuration File**

Configuration Categories:

| Section | Description | Key Settings |
|---------|-------------|--------------|
| `server` | Server Config | Port (8080), Tomcat Thread Pool |
| `spring` | Spring Framework | I18n, File Upload (Max 100MB) |
| `knowledge.qa.knowledge-base` | Knowledge Base | Document Path, Index Mode |
| `knowledge.qa.vector-search` | Vector Search | Enable, Model Path, Weight |
| `knowledge.qa.llm` | LLM Config | API Key, Model Name, Prompt Template |
| `knowledge.qa.ppl` | PPL Service | Enable, Provider (ONNX/Ollama/OpenAI) |
| `knowledge.qa.image-processing` | Image Understanding | Vision LLM Strategy, Model |
| `knowledge.qa.hope` | **HOPE 3-Layer Memory** | Enable, Layer Config |
| `knowledge.qa.feedback` | Feedback System | Weight Adjustment, Auto Apply |

**Key Configuration**:
```yaml
knowledge:
  qa:
    llm:
      api-key: ${AI_API_KEY}      # Read from environment variable
      model: deepseek-chat         # Recommended: Cost-effective
    
    hope:
      enabled: true                # HOPE 3-Layer Memory (v2.0 New)
      
    vector-search:
      enabled: true                # Vector Search
      lucene-weight: 0.3          # Lucene Weight
      vector-weight: 0.7          # Vector Weight
```

---

### 3. Data Directory (`data/`)

#### `documents/`
**User Upload Document Storage**

- Supported formats: PDF, DOCX, XLSX, PPTX, TXT, MD, etc. (35+ formats)
- Auto-detect file changes and suggest incremental indexing
- Path configurable in `application.yml`

#### `knowledge-base/`
**Knowledge Base Index Data**

```
knowledge-base/
├── cache/                    # Query Cache
├── documents/                # Document Storage (Date-based YYYY/MM/DD)
│   └── 2025/11/23/          # Example: Documents from Nov 23, 2025
│       └── *.txt.gz         # Compressed Document Content
├── index/                   # Index Directory
│   └── lucene-index/        # Lucene Full-text Index
│       ├── segments_*       # Index Segment Files
│       ├── *.cfe/.cfs       # Index Data Files
│       └── write.lock       # Write Lock File
└── metadata/                # Metadata
    └── metadata.db          # Document Metadata Database
```

**Description**:
- Documents archived by date for easy management
- Lucene index supports full-text search
- Metadata includes file info, index status, etc.

#### `rag/`
**RAG Retrieval Specific Data**

```
rag/
├── cache/                   # RAG Query Cache
├── documents/               # RAG Processed Documents
├── index/                   # RAG Index
│   └── lucene-index/        # Lucene Index
└── metadata/                # RAG Metadata
    └── metadata.db          # Metadata Database
```

**Description**:
- Independent RAG data storage
- Separated from main knowledge base for better management

#### `vector-index/`
**Vector Index Storage**

```
vector-index/
└── vector-index/
    └── vectors.dat          # Vector Data File
```

**Description**:
- Document vectors generated by BGE model
- Vector dimension: 768 (BGE-Base-ZH)
- Supports semantic similarity search
- Binary format storage, efficient retrieval

#### `feedback/`
**User Feedback Data** (Runtime Generated)

- QA records (questions, answers, sources)
- User ratings (1-5 stars)
- Document weight adjustment records
- High-rated QA archives

#### `llm-results/`
**LLM Analysis Results** (Runtime Generated)

- AI analysis history
- Markdown format analysis reports
- PDF export results
- Multi-document joint analysis results

#### `qa-records/`
**QA Record Archive** (Runtime Generated)

- All QA session records
- User feedback records
- For similar question recommendations
- For HOPE learning

#### `hope/`
**HOPE 3-Layer Memory Storage** (v2.0 New, Runtime Generated)

| Subdirectory | Description | Data Type | Persistent |
|--------------|-------------|-----------|------------|
| `permanent/` | Low-freq Layer - Permanent Knowledge | Skill Templates, Factual Knowledge | ✅ JSON |
| `ordinary/` | Mid-freq Layer - Recent Knowledge | High-rated QA within 30 days | ✅ JSON |
| `high-frequency/` | High-freq Layer - Real-time Context | Session Memory | ❌ Memory |

**Description**:
- Low-freq and Mid-freq layers are persisted to disk
- High-freq layer stored in memory only, cleared on restart
- Automatic learning and promotion mechanism

---

### 4. Model Directory (`models/`)

#### `bge-base-zh/`
**BGE Vector Model (Built-in)**

- **Usage**: Document vectorization, semantic search
- **Provider**: Beijing Academy of Artificial Intelligence
- **Dimension**: 768
- **Files**: 
  - `model.onnx` (~400MB)
  - `tokenizer.json`
  - `config.json`
- **Status**: ✅ Ready to use out-of-the-box

#### `qwen2.5-0.5b-instruct/`
**Qwen PPL Model (Optional)**

- **Usage**: PPL intelligent chunking, document reranking
- **Provider**: Alibaba Tongyi Qianwen
- **Files**: 
  - `model.onnx` (~500MB)
  - `tokenizer.json`
- **Download**: Run `scripts/download_qwen_onnx.py`
- **Alternative**: Use Ollama (simpler)

---

### 5. Startup Scripts

models download go page for details:
[download model](https://github.com/jinhua10/models/tree/1.0)

vector embedding models:
https://github.com/jinhua10/models/releases/download/1.0/bge-base-zh.zip
ppl chunk models:
https://github.com/jinhua10/models/releases/download/1.0/qwen2.5-0.5b-instruct.zip

#### `start.bat`
**Windows Startup Script**

**Features**:
- Set Java environment variables (UTF-8 encoding)
- Configure JVM memory (-Xms512m -Xmx2g)
- Read environment variable `AI_API_KEY`
- Start Spring Boot application

**Usage**:
```bash
# 1. Set environment variable (optional, can also configure in application.yml)
set AI_API_KEY=your-deepseek-api-key

# 2. Double-click or run from command line
start.bat

# 3. Access
http://localhost:8080
```

**Customize Memory**:
Modify `-Xms512m -Xmx2g` in the script (min 512MB, max 2GB)

#### `stop.bat`
**Windows Stop Script**

- Find process occupying port 8080
- Terminate Java process
- Clean temporary files

#### `fix-lock.bat`
**Fix Port Occupation Script**

- **Scenario**: Port 8080 occupied after abnormal exit
- **Function**: Force release port
- **Usage**: Run this script first if startup fails

---

### 6. Utility Scripts (`scripts/`)

#### `download_embedding_model.py`
**Download Vector Model Script**

```bash
# Download BGE-Base-ZH model
python scripts/download_embedding_model.py --model bge-base-zh

# Download BGE-Large-ZH model (higher accuracy)
python scripts/download_embedding_model.py --model bge-large-zh
```

**Supported Models**:
- `bge-base-zh` (Recommended, 400MB)
- `bge-large-zh` (High accuracy, 1.5GB)
- `bge-m3` (Multilingual, 2GB)

#### `download_qwen_onnx.py`
**Download Qwen PPL Model Script**

```bash
# Download Qwen2.5-0.5B (Recommended, lightweight)
python scripts/download_qwen_onnx.py --model qwen2.5-0.5b-instruct

# Download Qwen2.5-1.5B (Balanced)
python scripts/download_qwen_onnx.py --model qwen2.5-1.5b-instruct
```

**Note**: Can also use Ollama as alternative (simpler):
```bash
ollama pull qwen2.5:0.5b
```

#### `convert_bge_to_onnx.py`
**Convert BGE Model to ONNX Format**

- **Usage**: Convert HuggingFace model to ONNX (development only)
- **Description**: Release package already includes ONNX models, general users don't need to run

---

### 7. Documentation

#### `PROMPT_QUICK_START.md`
**Prompt Configuration Quick Start Guide (Chinese)**

- How to customize AI response style
- Prompt template configuration examples
- Placeholder descriptions (`{question}`, `{context}`)

#### `PROMPT_TEMPLATE_CONFIG_EN.md`
**Prompt Template Configuration Guide (English)**

- English version of prompt customization guide
- Template examples for different scenarios

#### `jdk download.txt`
**JDK Download Links**

- Oracle JDK 11/17 download addresses
- OpenJDK download addresses
- Installation instructions

---

## 🚀 Quick Start Guide

### Minimal Configuration (3 Steps to Start)

#### 1️⃣ Configure LLM API Key

**Method 1**: Environment Variable (Recommended)
```bash
# Windows CMD
set AI_API_KEY=your-deepseek-api-key

# Windows PowerShell
$env:AI_API_KEY="your-deepseek-api-key"

# Linux/Mac
export AI_API_KEY="your-deepseek-api-key"
```

**Method 2**: Modify `config/application.yml`
```yaml
knowledge:
  qa:
    llm:
      api-key: "sk-xxxxxxxxxxxx"  # Direct input (not recommended, low security)
```

#### 2️⃣ Start Application

```bash
# Windows
start.bat

# Linux/Mac
java -jar ai-reviewer-base-file-rag-2.0-jar-with-dependencies.jar
```

#### 3️⃣ Access System

Open browser and visit: **http://localhost:8080**

---

## 🔧 Configuration Recommendations

### Production Environment Recommended Configuration

| Config Item | Recommended Value | Description |
|-------------|------------------|-------------|
| **LLM Model** | `deepseek-chat` | Cost-effective, excellent Chinese |
| **Vector Search** | `enabled: true` | Enhance semantic understanding |
| **HOPE Architecture** | `enabled: true` | Reduce LLM calls, faster over time |
| **PPL Provider** | `ollama` | Local, no need to download large models |
| **Vision LLM** | `qwen-vl-plus` | Handle images and PPT |
| **JVM Memory** | `-Xmx2g` | 2GB sufficient for 10K documents |

### Cost Optimization Configuration

**Option 1**: Pure Local (Zero API Cost)
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

**Option 2**: Hybrid Mode (Low Cost)
```yaml
knowledge:
  qa:
    llm:
      provider: deepseek  # Online API (cheap)
    ppl:
      default-provider: ollama  # Local
    vector-search:
      enabled: true  # Local
```

---

## 📊 HOPE 3-Layer Memory Architecture

### What is HOPE?

HOPE (Hierarchical Optimized Persistent Engine) is the core upgrade of v2.0, designed based on Google's HOPE paper with a three-layer memory architecture:

```
┌──────────────────────────────────────────────────┐
│  High-frequency Layer - Real-time Session Context│
│  • Multi-turn conversation memory                 │
│  • Temporary definitions and corrections          │
│  • Session timeout: 30 minutes                    │
└──────────────────────────────────────────────────┘
                    ↓ High-score Promotion
┌──────────────────────────────────────────────────┐
│  Ordinary Layer - Recent High-rated QA           │
│  • High-rated QA within 30 days                  │
│  • Similarity ≥ 0.95 use directly                │
│  • Similarity ≥ 0.7 as reference                 │
└──────────────────────────────────────────────────┘
                    ↓ Continuous Validation & Promotion
┌──────────────────────────────────────────────────┐
│  Permanent Layer - Permanent Skill Knowledge     │
│  • Skill templates (e.g., "how to configure")    │
│  • Factual knowledge (e.g., "version number")    │
│  • Confidence ≥ 0.9 for direct answers          │
└──────────────────────────────────────────────────┘
```

### HOPE Benefits

- ⚡ **Faster Response**: Simple questions < 100ms (no LLM needed)
- 💰 **Cost Reduction**: 20-30% LLM savings rate
- 🎯 **Better Accuracy**: Auto-learn user preferences
- 🧠 **Smarter Over Time**: Knowledge automatically accumulates and promotes

### Monitor HOPE Status

Access monitoring endpoints:
- **Status**: http://localhost:8080/api/hope/status
- **Dashboard**: http://localhost:8080/api/hope/dashboard
- **Metrics**: http://localhost:8080/api/hope/metrics

---

## 🐛 Common Issues

### 1. Startup Failure: Port 8080 Occupied

**Solution**:
```bash
# Run fix script
fix-lock.bat

# Or manually change port (config/application.yml)
server:
  port: 8081
```

### 2. Vector Search Unavailable

**Cause**: Model files missing or corrupted

**Solution**:
```bash
# Re-download model
python scripts/download_embedding_model.py --model bge-base-zh

# Or disable vector search
knowledge:
  qa:
    vector-search:
      enabled: false
```

### 3. PPL Feature Unavailable

**Solution 1**: Use Ollama (Recommended)
```bash
ollama pull qwen2.5:0.5b
```

**Solution 2**: Download ONNX Model
```bash
python scripts/download_qwen_onnx.py --model qwen2.5-0.5b-instruct
```

**Solution 3**: Disable PPL
```yaml
knowledge:
  qa:
    ppl:
      enabled: false
```

### 4. Out of Memory (OOM)

**Cause**: Insufficient JVM memory

**Solution**:
Edit `start.bat`, increase memory:
```bat
set JAVA_OPTS=%JAVA_OPTS% -Xms1g -Xmx4g
```

---

## 📞 Technical Support

- **GitHub Issues**: [Submit Issue](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)
- **Documentation**: [Full Documentation](../README.md)
- **Changelog**: [CHANGELOG](../README.md#-changelog)

---

## 📝 Changelog

### v2.0.0 (2025-12-07)
- ✨ Added HOPE 3-layer memory architecture
- ✨ Added multi-document joint analysis framework
- ✨ Added intelligent search strategy dispatcher
- 🔧 Optimized configuration file, support more customization

### v1.0.0 (2025-11-22)
- 🎉 Initial release

---

<div align="center">

**AI Reviewer Base File RAG v2.0**

Made with ❤️ by [AI Reviewer Team](https://github.com/jinhua10)

</div>

