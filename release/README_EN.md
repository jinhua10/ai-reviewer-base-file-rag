# 🚀 AI Reviewer Base File RAG - Deployment Guide

> **Enterprise-Grade Local File RAG Retrieval System**  
> Zero External Dependencies | 77% Cost Savings | 100% Data Privacy

[![Version](https://img.shields.io/badge/version-1.0-blue.svg)](https://github.com/jinhua10/ai-reviewer-base-file-rag)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](../LICENSE.txt)

English | [简体中文](README.md)

---

## 📋 Table of Contents

- [Version Features](#-version-features)
- [Quick Start](#-quick-start-3-minute-deployment)
- [Directory Structure](#-directory-structure)
- [Configuration](#️-configuration)
- [API Usage](#-api-usage)
- [FAQ](#-faq)
- [Performance Tuning](#-performance-tuning)
- [Technical Support](#-technical-support)

---

## ✨ Version Features

### v1.0 Core Features

| Feature | Description | Status |
|---------|-------------|--------|
| **35+ Document Formats** | PDF, DOCX, XLSX, PPTX, TXT, MD, etc. | ✅ |
| **OCR Image Recognition** | Auto-extract text from images in documents | ✅ |
| **BM25 Keyword Search** | High-performance full-text search, no vector DB needed | ✅ |
| **Vector Semantic Search** | Optional ONNX local model for semantic search | ✅ |
| **Multi-LLM Support** | OpenAI, DeepSeek, Claude (optional) | ✅ |
| **REST API** | Standard HTTP interface, easy integration | ✅ |
| **Spring Boot** | Enterprise-grade framework, production-ready | ✅ |

### 🆕 Latest Updates

- ✅ **Enhanced OCR Recognition** - Support for Chinese-English mixed recognition
- ✅ **PDFBox Compatibility Fix** - Resolved PDF parsing issues
- ✅ **Enhanced Logging** - Clear processing status display
- ✅ **One-Click Diagnostic Tool** - `check-ocr.bat` for quick configuration check

---

## 🚀 Quick Start (3-Minute Deployment)

### Prerequisites

| Software | Version | Download |
|----------|---------|----------|
| **Java** | 17+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) |
| **Memory** | 2GB+ | - |
| **Disk** | 5GB+ | - |

### Step 1: Verify Java Environment

```bash
java -version
```

Expected output:
```
java version "17.0.x"
```

### Step 2: Prepare Documents

Place documents to be indexed in the `data/documents/` directory:

```
data/documents/
├── product-manual.pdf
├── technical-docs.docx
├── data-report.xlsx
├── presentation.pptx
└── README.md
```

**Supported Document Formats**:

| Category | Formats |
|----------|---------|
| **Office Documents** | `.pdf`, `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx` |
| **Text Files** | `.txt`, `.md`, `.csv`, `.json`, `.xml` |
| **Code Files** | `.java`, `.py`, `.js`, `.go`, `.cpp` |
| **Image Files** | `.png`, `.jpg`, `.jpeg` (requires OCR) |

### Step 3: (Optional) Enable OCR

If documents contain images or scanned content:

```bash
# Download OCR language packs (one-time, ~10MB)
download-tessdata.bat
```

> 💡 **Tip**: OCR is enabled by default after downloading language packs  
> 📖 Detailed guide: [快速启动-OCR.md](快速启动-OCR.md)

### Step 4: Start Application

**Windows**:
```bash
# Double-click to run
start.bat

# Or via command line
.\start.bat
```

**Linux/Mac**:
```bash
chmod +x start.sh
./start.sh
```

### Step 5: Verify Running

After successful startup (~10-30 seconds), access:

```bash
# Health check
curl http://localhost:8080/api/qa/health

# View statistics
curl http://localhost:8080/api/qa/statistics
```

**Expected output**:
```json
{
  "status": "UP",
  "message": "Knowledge QA System is running"
}
```

---

## 📦 Directory Structure

```
release/
├── 📄 ai-reviewer-base-file-rag-1.0.jar    # Application JAR
│
├── 🚀 Startup Scripts
│   ├── start.bat                            # Windows startup script
│   ├── stop.bat                             # Windows stop script
│   └── fix-lock.bat                         # Fix index lock script
│
├── 🔧 Utility Scripts
│   ├── download-tessdata.bat                # OCR language pack download
│   ├── check-ocr.bat                        # OCR configuration check
│   └── download model.bat                   # Vector model download (models/)
│
├── 📖 Documentation
│   ├── README.md                            # This file (deployment guide)
│   ├── 快速启动-OCR.md                      # OCR quick start guide ⭐
│   ├── 图片识别快速启用.md                  # OCR complete usage guide
│   ├── 模型下载说明.md                      # Vector model download guide
│   ├── OCR配置指南.md                       # OCR detailed configuration
│   ├── OCR诊断指南.md                       # OCR troubleshooting
│   └── 锁文件问题解决指南.md                # Index lock issue guide
│
├── ⚙️ config/
│   └── application.yml                      # External configuration file
│
├── 📦 models/                               # Vector model directory (optional)
│   ├── download model.bat                   # Model download script
│   └── paraphrase-multilingual/             # Model files
│       └── model.onnx                       # ONNX model (~420MB)
│
├── 🔤 tessdata/                             # OCR language pack directory
│   ├── chi_sim.traineddata                  # Simplified Chinese (~4MB)
│   └── eng.traineddata                      # English (~5MB)
│
├── 📁 data/                                 # Data directory
│   ├── documents/                           # 📄 Place documents to index here
│   ├── knowledge-base/                      # 🗄️ Knowledge base storage (auto-generated)
│   │   ├── documents/                       # Document copies
│   │   ├── index/                           # Lucene index
│   │   ├── metadata/                        # Metadata (SQLite)
│   │   └── cache/                           # Cache data
│   └── vector-index/                        # 🔢 Vector index (optional, auto-generated)
│
├── 📝 logs/                                 # Log directory (auto-generated)
│   └── ai-reviewer-rag.log                  # Application logs
│
└── 🔧 temp/                                 # Temp files directory (auto-generated)
    └── work/
```

### Directory Description

| Directory/File | Purpose | Required |
|----------------|---------|----------|
| **ai-reviewer-base-file-rag-1.0.jar** | Application main program | ✅ Required |
| **config/application.yml** | Configuration file | ✅ Required |
| **data/documents/** | Place documents to index | ✅ Required |
| **tessdata/** | OCR language packs | ⚠️ Required for OCR |
| **models/** | Vector search model | ⚠️ Required for vector search |
| **data/knowledge-base/** | Knowledge base index | 🔄 Auto-generated |
| **logs/** | Application logs | 🔄 Auto-generated |

---

## ⚙️ Configuration

### Default Configuration (Out-of-the-Box)

System uses the following default configuration, no modification needed:

```yaml
# Default port
server.port: 8080

# Keyword search mode (no model download needed)
knowledge.qa.vector-search.enabled: false

# Mock LLM (no API Key needed)
knowledge.qa.llm.provider: mock

# OCR enabled (requires language pack download)
# Run download-tessdata.bat
```

### Common Configuration Changes

Edit `config/application.yml` file:

#### 1. Change Service Port

```yaml
server:
  port: 9090  # Change to another port
```

#### 2. Configure Document Path

```yaml
knowledge:
  qa:
    knowledge-base:
      # Document source path (supports relative/absolute paths)
      source-path: ./data/documents
      
      # Rebuild index on startup
      rebuild-on-startup: false  # false=incremental, true=full rebuild
```

#### 3. Enable Vector Search (Semantic Search)

⚠️ **Prerequisite**: Download model first (~420MB)

```bash
# Download model (run once)
cd models
download model.bat
```

Then modify configuration:

```yaml
knowledge:
  qa:
    vector-search:
      enabled: true  # Enable vector search
      model:
        name: paraphrase-multilingual
        path: /models/paraphrase-multilingual/model.onnx
      top-k: 20                      # Retrieval count
      similarity-threshold: 0.4       # Similarity threshold
```

**Search Mode Comparison**:

| Feature | Keyword Search (BM25) | Vector Search (Semantic) |
|---------|----------------------|-------------------------|
| **Setup** | ✅ Zero config | ⚠️ Requires 420MB model |
| **Memory** | ✅ 256MB - 1GB | ⚠️ 1GB - 4GB |
| **Speed** | ✅ Very fast (<50ms) | ⚠️ Fast (<100ms) |
| **Exact Match** | ✅ Excellent | ⭐ Good |
| **Semantic Understanding** | ⭐ Basic | ✅ Excellent |
| **Use Case** | Exact keyword queries | Fuzzy semantic queries |

#### 4. Configure Real LLM (AI Q&A)

**OpenAI GPT-4**:
```yaml
knowledge:
  qa:
    llm:
      provider: openai
      api-key: sk-your-openai-key
      model: gpt-4o
      endpoint: https://api.openai.com/v1/chat/completions
      temperature: 0.7
```

**DeepSeek (Chinese)**:
```yaml
knowledge:
  qa:
    llm:
      provider: deepseek
      api-key: sk-your-deepseek-key
      model: deepseek-chat
      endpoint: https://api.deepseek.com/v1/chat/completions
```

**Use Environment Variables (Recommended)**:
```bash
# Windows
set AI_API_KEY=sk-your-api-key
start.bat

# Linux/Mac
export AI_API_KEY=sk-your-api-key
./start.sh
```

#### 5. OCR Configuration

Enabled by default, works after downloading language packs:

```yaml
# Built-in configuration, no modification needed
knowledge:
  qa:
    ocr:
      provider: tesseract  # Use Tesseract OCR
      tesseract:
        data-path: ./tessdata
        language: chi_sim+eng  # Chinese-English
```

#### 6. Memory and Performance Tuning

Edit `start.bat`:

```batch
# Development environment (2-4GB)
set JAVA_OPTS=-Xms512m -Xmx2g

# Production environment (4-8GB, recommended)
set JAVA_OPTS=-Xms1g -Xmx4g
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=200
```

---

## 🎬 First Startup Process

### 1. Start Application

```bash
# Windows
start.bat

# Linux/Mac
./start.sh
```

### 2. Observe Startup Logs

Expected output (~10-30 seconds):

```
================================================================================
📚 AI Reviewer Base File RAG - Knowledge QA System
================================================================================
Version: 1.0
Port: 8080
Document Path: ./data/documents
================================================================================

🔍 Scanning document directory...
   ✓ Found 150 document files

📝 Starting document indexing...
   [1/150] product-manual.pdf (2.5 MB)
      ✓ Extracted 15,234 characters
      ✓ OCR recognized 3 images
      ✓ Indexing complete

   [2/150] technical-docs.docx (1.2 MB)
      ✓ Extracted 8,567 characters
      ✓ Indexing complete

   ... (continuing)

================================================================================
✅ Knowledge Base Build Complete
================================================================================
   📊 Statistics:
      - Success: 148 files
      - Failed: 2 files
      - Total: 148 documents
      - Total chars: 1,234,567
      - Time: 32.5 seconds
================================================================================

🚀 Application Started Successfully!
   Access URL: http://localhost:8080
   API Docs: http://localhost:8080/swagger-ui.html (if enabled)
   Health Check: http://localhost:8080/api/qa/health
================================================================================
```

### 3. Verify Running Status

```bash
# Health check
curl http://localhost:8080/api/qa/health

# View statistics
curl http://localhost:8080/api/qa/statistics

# Simple search
curl "http://localhost:8080/api/qa/search?query=test&limit=3"
```

---

## 📡 API Usage Guide

### Base URL

```
http://localhost:8080
```

### 1. Health Check

```http
GET /api/qa/health
```

**Response Example**:
```json
{
  "status": "UP",
  "message": "Knowledge QA System is running",
  "timestamp": "2025-11-25T10:30:00"
}
```

### 2. Get Statistics

```http
GET /api/qa/statistics
```

**Response Example**:
```json
{
  "documentCount": 148,
  "indexedDocumentCount": 148,
  "totalSize": "125.5 MB",
  "indexPath": "./data/knowledge-base",
  "lastUpdated": "2025-11-25T10:25:00",
  "vectorSearchEnabled": false
}
```

### 3. Search Documents

```http
GET /api/qa/search?query={keyword}&limit={count}
```

**Parameters**:
| Parameter | Type | Required | Description | Default |
|-----------|------|----------|-------------|---------|
| `query` | String | ✅ | Search keyword | - |
| `limit` | Integer | ❌ | Return count | 10 |

**Request Example**:
```bash
curl "http://localhost:8080/api/qa/search?query=product+features&limit=5"
```

**Response Example**:
```json
{
  "query": "product features",
  "total": 15,
  "documents": [
    {
      "id": "doc-20251125-001",
      "title": "product-manual.pdf",
      "content": "Main product features include: high performance, ease of use, security...",
      "score": 0.92,
      "metadata": {
        "fileName": "product-manual.pdf",
        "fileSize": "2.5 MB",
        "indexedAt": "2025-11-25T10:25:00"
      }
    }
  ]
}
```

### 4. AI Q&A (Requires LLM Configuration)

```http
POST /api/qa/ask
Content-Type: application/json
```

**Request Body**:
```json
{
  "question": "What are the main product features?",
  "topK": 5  // Optional, retrieval document count
}
```

**Request Example**:
```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the main product features?"
  }'
```

**Response Example**:
```json
{
  "question": "What are the main product features?",
  "answer": "According to the product documentation, the main features include:\n1. High performance: Uses...\n2. Ease of use: Provides...\n3. Security: Supports...",
  "sources": [
    {
      "fileName": "product-manual.pdf",
      "content": "Main product features include...",
      "score": 0.92
    }
  ],
  "confidence": 0.89,
  "responseTimeMs": 1250
}
```

### 5. Rebuild Knowledge Base

```http
POST /api/qa/rebuild
```

**Description**: Delete existing index and re-index all documents

**Response Example**:
```json
{
  "status": "success",
  "message": "Knowledge base rebuild complete",
  "documentsProcessed": 148,
  "timeElapsedSeconds": 32.5
}
```

### API Error Handling

**Error Response Format**:
```json
{
  "error": "Bad Request",
  "message": "query parameter cannot be empty",
  "timestamp": "2025-11-25T10:30:00",
  "path": "/api/qa/search"
}
```

**Common Error Codes**:
| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 400 | Bad request parameters |
| 404 | Resource not found |
| 500 | Internal server error |
| 503 | Service unavailable (starting up) |

---

## ❓ FAQ

### Q1: Port 8080 Already in Use?

**Error Message**:
```
Port 8080 is already in use
```

**Solutions**:

**Method 1: Change Port** (Recommended)
```yaml
# Edit config/application.yml
server:
  port: 9090  # Change to another port
```

**Method 2: Stop Process Using Port**
```bash
# Find process using port
netstat -ano | findstr :8080

# Kill process
taskkill /PID <ProcessID> /F
```

---

### Q2: Documents Not Found or Not Indexed

**Possible Causes**:
1. ❌ Documents in wrong location
2. ❌ Unsupported file format
3. ❌ Incorrect configuration path

**Solutions**:

**Checklist**:
- [ ] Are documents in `data/documents/` directory?
- [ ] Is file format supported (PDF/DOCX/XLSX/TXT, etc.)?
- [ ] Check `source-path` in `config/application.yml`

**Verify Configuration**:
```yaml
knowledge:
  qa:
    knowledge-base:
      source-path: ./data/documents  # Confirm path is correct
```

---

### Q3: Out of Memory (OutOfMemoryError)

**Error Message**:
```
Java heap space
OutOfMemoryError: Java heap space
```

**Solution**:

**Increase JVM Memory**:

Edit `start.bat`:
```batch
# Increase max memory from 2g to 4g or higher
set JAVA_OPTS=-Xms1g -Xmx4g
```

**Memory Recommendations**:
| Document Count | Recommended Memory |
|----------------|-------------------|
| < 1,000 | 2GB |
| 1,000 - 10,000 | 4GB |
| > 10,000 | 8GB+ |

---

### Q4: Chinese Characters Garbled

**Symptoms**:
- Chinese characters display as garbled in logs
- Search results have incorrect Chinese

**Solutions**:

**1. Confirm Configuration File Encoding**:
- `config/application.yml` must be saved in **UTF-8 encoding**

**2. Set Console Encoding**:
```batch
# Already included in start.bat
chcp 65001
```

**3. Check Document Encoding**:
- Ensure documents use UTF-8 or system default encoding

---

### Q5: OCR Not Working or Images Not Recognized

**Symptoms**:
- Logs show "Tesseract unavailable"
- Text in images not extracted

**Solutions**:

**Quick Diagnosis**:
```bash
# Run diagnostic tool
check-ocr.bat
```

**Common Issues**:

1. **Language Packs Not Downloaded**:
   ```bash
   # Download language packs
   download-tessdata.bat
   ```

2. **Incorrect Language Pack Path**:
   ```yaml
   # Check config/application.yml
   knowledge:
     qa:
       ocr:
         tesseract:
           data-path: ./tessdata  # Confirm path is correct
   ```

3. **Detailed Troubleshooting**:
   - See [OCR诊断指南.md](OCR诊断指南.md)

---

### Q6: Vector Search Initialization Failed

**Error Message**:
```
Vector search engine initialization failed
Model file not found
```

**Solutions**:

**Method 1: Disable Vector Search** (Recommended if semantic search not needed)
```yaml
# config/application.yml
knowledge:
  qa:
    vector-search:
      enabled: false  # Use keyword search
```

**Method 2: Download Model** (If semantic search needed)
```bash
# Navigate to models directory
cd models

# Run download script
download model.bat

# Wait for download (~420MB)
```

Detailed guide: [模型下载说明.md](模型下载说明.md)

---

### Q7: Index Lock Error (LockObtainFailedException)

**Error Message**:
```
LockObtainFailedException: Lock held by this virtual machine
```

**Cause**: Previous application instance didn't shut down properly, leaving lock file

**Solutions**:

**Method 1: Use Fix Script** (Recommended)
```bash
# Run fix script
fix-lock.bat
```

**Method 2: Manually Delete Lock File**
```bash
# 1. Stop application
stop.bat

# 2. Delete lock file
del data\knowledge-base\index\lucene-index\write.lock

# 3. Restart
start.bat
```

**Method 3: Rebuild Index**
```bash
stop.bat
rmdir /s /q data\knowledge-base
start.bat
```

**Prevention**:
- ✅ Always use `stop.bat` to stop normally
- ❌ Don't force quit with Task Manager

Detailed guide: [锁文件问题解决指南.md](锁文件问题解决指南.md)

---

### Q8: LLM API Call Failed

**Error Message**:
```
Failed to call LLM API
Invalid API Key
```

**Solutions**:

**1. Check API Key**:
```yaml
# config/application.yml
knowledge:
  qa:
    llm:
      api-key: sk-your-api-key  # Confirm key is correct
```

**2. Use Environment Variable** (Recommended):
```bash
set AI_API_KEY=sk-your-api-key
start.bat
```

**3. Temporarily Use Mock Mode**:
```yaml
knowledge:
  qa:
    llm:
      provider: mock  # Don't call real LLM
```

---

### Q9: Many Documents, Slow Startup

**Issue**: First startup needs to index large amount of documents

**Optimization Solutions**:

**1. Enable Parallel Processing** (Recommended, 2-3x speed boost):
```yaml
knowledge:
  qa:
    document:
      # Enable parallel processing (enabled by default)
      parallel-processing: true
      
      # Parallel threads (0=auto, uses CPU cores)
      parallel-threads: 0
      
      # Batch size (recommended 10-20)
      batch-size: 10
```

**Performance Gain**:
- Small documents (< 1MB): **2-3x faster**
- Large documents (> 5MB): **1.5-2x faster**
- 1000 documents: From 10 minutes to **3-5 minutes**

**2. Incremental Indexing** (Recommended):
```yaml
knowledge:
  qa:
    knowledge-base:
      # Only index new and modified documents
      rebuild-on-startup: false
```

**3. Increase Memory**:
```batch
# Edit start.bat, more memory improves concurrent performance
set JAVA_OPTS=-Xms2g -Xmx8g
```

**4. Batch Processing**:
- Index important documents first (manuals, specs)
- Add other documents later
- Utilize incremental indexing feature

---

### Q10: Inaccurate Search Results

**Optimization Suggestions**:

**1. Adjust Retrieval Count**:
```yaml
knowledge:
  qa:
    vector-search:
      top-k: 20  # Increase candidate count
```

**2. Adjust Similarity Threshold**:
```yaml
knowledge:
  qa:
    vector-search:
      similarity-threshold: 0.3  # Lower threshold
```

**3. Enable Vector Search**:
- Vector search is more accurate for semantic understanding
- See Q6 to download model

**4. Optimize Document Quality**:
- Use clear document titles
- Avoid overly long documents (suggest segmentation)
- Regularly clean outdated documents

---

## 📁 Data Management

### Rebuild Knowledge Base

**Scenarios**: Documents updated, index corrupted, need full rebuild

**Method 1: Configure Rebuild**
```yaml
# config/application.yml
knowledge:
  qa:
    knowledge-base:
      rebuild-on-startup: true  # Rebuild on startup
```

Then restart application, change back to `false` after completion.

**Method 2: Delete Index and Rebuild**
```bash
stop.bat
rmdir /s /q data\knowledge-base
rmdir /s /q data\vector-index
start.bat
```

**Method 3: API Rebuild**
```bash
curl -X POST http://localhost:8080/api/qa/rebuild
```

### Data Backup

**Regularly backup the following**:

| Content | Path | Importance |
|---------|------|------------|
| Knowledge base index | `data/knowledge-base/` | ⭐⭐⭐ |
| Vector index | `data/vector-index/` | ⭐⭐ |
| Configuration | `config/application.yml` | ⭐⭐⭐ |
| Original documents | `data/documents/` | ⭐⭐⭐ |

**Backup Commands**:
```bash
# Create backup
tar -czf backup-$(date +%Y%m%d).tar.gz data/ config/

# Or use PowerShell
Compress-Archive -Path data,config -DestinationPath backup-$(Get-Date -Format yyyyMMdd).zip
```

### Log Management

**Log Location**: `logs/ai-reviewer-rag.log`

**Clean Old Logs**:
```bash
# Delete all logs
del logs\*.log

# Keep only last 7 days
forfiles /p logs /s /m *.log /d -7 /c "cmd /c del @path"
```

**Log Level Configuration**:
```yaml
# config/application.yml
logging:
  level:
    top.yumbo.ai.rag: INFO  # DEBUG/INFO/WARN/ERROR
```

---

## ⚡ Performance Tuning

### JVM Parameter Optimization

Edit `start.bat` according to environment:

```batch
:: Development environment (2-4GB)
set JAVA_OPTS=-Xms512m -Xmx2g

:: Production environment (4-8GB, recommended)
set JAVA_OPTS=-Xms1g -Xmx4g
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=200
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=./logs/heap-dump.hprof
```

### Index Performance Optimization

```yaml
knowledge:
  qa:
    knowledge-base:
      enable-cache: true  # Enable cache, improve query speed
      
    document:
      chunk-size: 2000     # Document chunk size
      chunk-overlap: 400   # Chunk overlap size
      parallel-processing: true  # Parallel processing
```

### Search Performance Optimization

```yaml
knowledge:
  qa:
    vector-search:
      top-k: 20                    # Increase retrieval count
      similarity-threshold: 0.4    # Adjust similarity threshold
      use-cache: true              # Enable search cache
```

---

## 📞 Technical Support

### Getting Help

1. **View Documentation**:
   - [快速启动-OCR.md](快速启动-OCR.md) - OCR quick guide
   - [模型下载说明.md](模型下载说明.md) - Vector model download
   - [锁文件问题解决指南.md](锁文件问题解决指南.md) - Index lock issues

2. **Run Diagnostic Tools**:
   ```bash
   check-ocr.bat  # OCR configuration check
   ```

3. **View Logs**:
   ```bash
   # View latest logs
   tail -f logs/ai-reviewer-rag.log
   
   # Windows
   Get-Content logs\ai-reviewer-rag.log -Tail 50
   ```

4. **Report Issues**:
   - GitHub Issues: [Submit Issue](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)
   - Email: 1015770492@qq.com

---

## 📝 Changelog

### v1.0.0 (2025-11-25)

#### 🆕 New Features
- ✅ Support for 35+ document formats (PDF, DOCX, XLSX, PPTX, TXT, MD, etc.)
- ✅ OCR text recognition (Tesseract)
- ✅ BM25 keyword search (zero configuration)
- ✅ Vector semantic search (optional)
- ✅ Multi-LLM support (OpenAI, DeepSeek, Claude)
- ✅ REST API interface
- ✅ External configuration support

#### 🔧 Fixes
- ✅ PDFBox compatibility issues
- ✅ Chinese encoding issues
- ✅ Index lock issues

#### 📖 Documentation
- ✅ Complete deployment guide
- ✅ OCR quick start guide
- ✅ Diagnostic tools

---

<div align="center">

## 🎉 AI Reviewer Base File RAG v1.0

**Enterprise-Grade Local File RAG Retrieval System**

Made with ❤️ by AI Reviewer Team

[GitHub](https://github.com/jinhua10/ai-reviewer-base-file-rag) | [Documentation](../README.md) | [Report Issues](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)

</div>

