# 📦 文档切分持久化 + 图片支持功能实现指南

## 🎯 功能概述

### 需求 1: 文档切分持久化
- ✅ 切分后的文档块保存到文件系统
- ✅ 每个块带语义标题或编号
- ✅ 支持单独下载每个块
- ✅ Markdown 格式存储

### 需求 2: 图片支持
- ✅ 提取文档中的图片
- ✅ 图片独立存储
- ✅ Markdown 中自动替换图片引用
- ✅ 页面直接展示图片

---

## 📦 已实现的组件

### 1. 核心服务类

#### ✅ ChunkStorageService (文档块存储服务)
**位置**: `src/main/java/top/yumbo/ai/rag/chunking/storage/ChunkStorageService.java`

**核心功能**：
```java
public class ChunkStorageService {
    // 保存文档块
    List<ChunkStorageInfo> saveChunks(String documentId, List<DocumentChunk> chunks);
    
    // 读取文档块
    String readChunkContent(String chunkId, String documentId);
    
    // 列出所有块
    List<ChunkStorageInfo> listChunks(String documentId);
    
    // 删除块
    void deleteChunks(String documentId);
}
```

**存储结构**：
```
data/
  └── chunks/
      └── {documentId}/
          ├── {documentId}_chunk_001_标题.md          # 内容文件（Markdown）
          ├── {documentId}_chunk_001_标题.meta.json   # 元数据文件
          ├── {documentId}_chunk_002_标题.md
          └── {documentId}_chunk_002_标题.meta.json
```

**内容格式示例**：
```markdown
# 项目介绍

> **块信息**: 第 1/5 块 | 标签: keyword:项目

---

本项目是一个基于 RAG 的知识库系统...
```

#### ✅ ImageStorageService (图片存储服务)
**位置**: `src/main/java/top/yumbo/ai/rag/image/ImageStorageService.java`

**核心功能**：
```java
public class ImageStorageService {
    // 保存图片
    ImageInfo saveImage(String documentId, byte[] imageData, String originalFilename);
    
    // 读取图片
    byte[] readImage(String documentId, String filename);
    
    // 列出图片
    List<ImageInfo> listImages(String documentId);
    
    // 生成图片 URL
    String generateImageUrl(String documentId, String filename);
    
    // 替换图片引用
    String replaceImageReferences(String content, String documentId, List<ImageInfo> images);
}
```

**存储结构**：
```
data/
  └── images/
      └── {documentId}/
          ├── {documentId}_{uuid}.jpg
          ├── {documentId}_{uuid}.png
          └── ...
```

### 2. 数据模型

#### ✅ ChunkStorageInfo
```java
@Data
public class ChunkStorageInfo {
    private String chunkId;           // 唯一ID
    private String documentId;        // 文档ID
    private int chunkIndex;           // 块序号
    private String title;             // 块标题
    private String contentPath;       // 内容文件路径
    private String metadataPath;      // 元数据文件路径
    private int contentLength;        // 内容长度
}
```

#### ✅ ImageInfo
```java
@Data
public class ImageInfo {
    private String imageId;           // 图片ID
    private String documentId;        // 文档ID
    private String filename;          // 文件名
    private String originalFilename;  // 原始文件名
    private String filePath;          // 文件路径
    private long fileSize;            // 文件大小
    private String format;            // 图片格式
    
    // 获取 Markdown 引用
    String getMarkdownReference();
}
```

### 3. REST API 控制器

#### ✅ ChunkDownloadController
**位置**: `src/main/java/top/yumbo/ai/rag/spring/boot/controller/ChunkDownloadController.java`

**API 接口**：
```
GET  /api/chunks/list/{documentId}           # 列出所有块
GET  /api/chunks/download/{documentId}/{chunkId}  # 下载块
GET  /api/chunks/content/{documentId}/{chunkId}   # 获取块内容
```

#### ✅ ImageController
**位置**: `src/main/java/top/yumbo/ai/rag/spring/boot/controller/ImageController.java`

**API 接口**：
```
GET  /api/images/{documentId}/{filename}     # 获取图片
GET  /api/images/list/{documentId}           # 列出所有图片
```

---

## 🔧 集成步骤

### 步骤 1: 添加 Bean 配置

在 `KnowledgeQAConfiguration.java` 或新建配置类：

```java
package top.yumbo.ai.rag.spring.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.yumbo.ai.rag.chunking.storage.ChunkStorageService;
import top.yumbo.ai.rag.image.ImageStorageService;

@Configuration
public class StorageConfiguration {
    
    @Bean
    public ChunkStorageService chunkStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        return new ChunkStorageService(storagePath);
    }
    
    @Bean
    public ImageStorageService imageStorageService(KnowledgeQAProperties properties) {
        String storagePath = properties.getKnowledgeBase().getStoragePath();
        return new ImageStorageService(storagePath);
    }
}
```

### 步骤 2: 在 SmartContextBuilder 中集成

更新 `extractWithChunker` 方法，保存切分块：

```java
private String extractWithChunker(String query, String content, int maxLength) {
    try {
        // 使用切分器切分文档
        List<DocumentChunk> chunks = chunker.chunk(content, query);
        
        // 保存切分块到文件系统（新增）
        if (chunkStorageService != null && currentDocumentId != null) {
            chunkStorageService.saveChunks(currentDocumentId, chunks);
            log.info("Saved {} chunks for document: {}", chunks.size(), currentDocumentId);
        }
        
        // ...existing code...
    } catch (Exception e) {
        // ...
    }
}
```

### 步骤 3: 在 KnowledgeQAService 中使用

```java
@Service
public class KnowledgeQAService {
    
    @Autowired
    private ChunkStorageService chunkStorageService;
    
    @Autowired
    private ImageStorageService imageStorageService;
    
    public AIAnswer ask(String question) {
        // ...existing code...
        
        // 获取答案后，处理图片引用
        if (imageStorageService != null) {
            List<ImageInfo> images = imageStorageService.listImages(currentDocumentId);
            answer = imageStorageService.replaceImageReferences(answer, currentDocumentId, images);
        }
        
        // 添加切分块信息到答案中
        if (chunkStorageService != null) {
            List<ChunkStorageInfo> chunks = chunkStorageService.listChunks(currentDocumentId);
            // 将 chunks 信息添加到 AIAnswer 对象
        }
        
        return aiAnswer;
    }
}
```

### 步骤 4: 更新前端页面

#### 4.1 添加块下载功能

在 `index.html` 的 QA 答案区域添加：

```javascript
// 显示可下载的块列表
function renderChunksList(chunks, documentId) {
    if (!chunks || chunks.length === 0) return '';
    
    return `
        <div style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;">
            <h4>📦 文档切分块（可下载）</h4>
            <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px;">
                ${chunks.map((chunk, index) => `
                    <button 
                        onclick="downloadChunk('${documentId}', '${chunk.chunkId}')"
                        style="
                            padding: 8px 16px;
                            background: #667eea;
                            color: white;
                            border: none;
                            border-radius: 6px;
                            cursor: pointer;
                            font-size: 14px;
                        ">
                        📄 ${chunk.title || `块 ${index + 1}`}
                        <span style="opacity: 0.8; font-size: 12px;">
                            (${(chunk.contentLength / 1024).toFixed(1)}KB)
                        </span>
                    </button>
                `).join('')}
            </div>
        </div>
    `;
}

// 下载块
async function downloadChunk(documentId, chunkId) {
    try {
        const response = await fetch(`/api/chunks/download/${documentId}/${chunkId}`);
        const blob = await response.blob();
        
        // 创建下载链接
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${chunkId}.md`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        console.log('✅ 下载成功:', chunkId);
    } catch (err) {
        console.error('❌ 下载失败:', err);
        alert('下载失败: ' + err.message);
    }
}
```

#### 4.2 更新答案显示支持图片

```javascript
function renderAnswer(answer, sources, chunks) {
    return `
        <div class="qa-item">
            <div class="qa-question">
                <div class="qa-icon">💬</div>
                <div class="qa-content">
                    ${marked.parse(answer)}  <!-- Markdown 会自动渲染图片 -->
                </div>
            </div>
            
            <!-- 参考来源 -->
            ${renderSources(sources)}
            
            <!-- 文档切分块下载 -->
            ${renderChunksList(chunks, currentDocumentId)}
        </div>
    `;
}
```

#### 4.3 图片样式优化

添加 CSS 样式：

```css
/* 图片样式 */
.qa-content img {
    max-width: 100%;
    height: auto;
    margin: 15px 0;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    cursor: pointer;
    transition: transform 0.2s;
}

.qa-content img:hover {
    transform: scale(1.02);
}

/* 图片点击放大 */
.image-modal {
    display: none;
    position: fixed;
    z-index: 1000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0,0,0,0.9);
    justify-content: center;
    align-items: center;
}

.image-modal img {
    max-width: 90%;
    max-height: 90%;
    box-shadow: 0 4px 20px rgba(255,255,255,0.3);
}

.image-modal.active {
    display: flex;
}
```

#### 4.4 添加图片点击放大功能

```javascript
// 图片点击放大
document.addEventListener('click', function(e) {
    if (e.target.tagName === 'IMG' && e.target.closest('.qa-content')) {
        // 创建模态框
        const modal = document.createElement('div');
        modal.className = 'image-modal active';
        modal.innerHTML = `<img src="${e.target.src}" alt="${e.target.alt}">`;
        
        // 点击关闭
        modal.onclick = function() {
            modal.remove();
        };
        
        document.body.appendChild(modal);
    }
});
```

---

## 📊 完整工作流程

### 文档索引时

```
1. 上传文档
   ↓
2. 提取文档内容
   ↓
3. 提取文档中的图片
   ↓
4. ImageStorageService.saveImage()  ← 保存图片
   ↓
5. 切分文档（使用配置的切分器）
   ↓
6. ChunkStorageService.saveChunks()  ← 保存切分块
   ↓
7. 创建 Lucene 索引
```

### 用户问答时

```
1. 用户提问
   ↓
2. 检索相关文档
   ↓
3. 构建上下文（从切分块中选择）
   ↓
4. 调用 LLM 生成答案
   ↓
5. ImageStorageService.replaceImageReferences()  ← 替换图片引用
   ↓
6. 返回答案 + 切分块列表
   ↓
7. 前端渲染：
   - Markdown 渲染（自动显示图片）
   - 显示可下载的切分块列表
```

### 下载切分块时

```
用户点击下载按钮
   ↓
GET /api/chunks/download/{documentId}/{chunkId}
   ↓
ChunkStorageService.readChunkContent()
   ↓
返回 Markdown 文件
   ↓
浏览器下载文件
```

### 获取图片时

```
Markdown 中的图片引用：![alt](/api/images/doc1/image.jpg)
   ↓
GET /api/images/doc1/image.jpg
   ↓
ImageStorageService.readImage()
   ↓
返回图片数据
   ↓
浏览器渲染图片
```

---

## 🎨 UI 效果预览

### 问答页面（带图片和下载块）

```
┌─────────────────────────────────────────┐
│ 💬 问题：这个项目的架构是什么样的？      │
├─────────────────────────────────────────┤
│ 🤖 答案：                               │
│                                         │
│ 根据文档，本项目采用以下架构：          │
│                                         │
│ [架构图显示]                            │
│ ┌─────────────────────┐                │
│ │   架构图.png        │                │
│ │   (实际图片渲染)     │                │
│ └─────────────────────┘                │
│                                         │
│ 主要包括以下几层：                      │
│ 1. 接入层...                           │
│ 2. 服务层...                           │
│                                         │
├─────────────────────────────────────────┤
│ 📚 参考来源：                           │
│ • 项目文档.pdf (95% 相关)               │
│ • 架构设计.docx (88% 相关)              │
├─────────────────────────────────────────┤
│ 📦 文档切分块（可下载）                 │
│                                         │
│ [📄 项目介绍 (4.2KB)]                   │
│ [📄 架构设计 (6.8KB)]                   │
│ [📄 技术选型 (3.5KB)]                   │
└─────────────────────────────────────────┘
```

---

## 🎯 使用示例

### 示例 1: 查看文档包含的所有图片

```bash
curl http://localhost:8080/api/images/list/项目文档.pdf
```

响应：
```json
[
  {
    "imageId": "uuid-1",
    "documentId": "项目文档.pdf",
    "filename": "项目文档_uuid-1.png",
    "originalFilename": "架构图.png",
    "fileSize": 245678,
    "format": "png",
    "url": "/api/images/项目文档/项目文档_uuid-1.png"
  },
  ...
]
```

### 示例 2: 下载文档的某个切分块

```bash
curl -O http://localhost:8080/api/chunks/download/项目文档/项目文档_chunk_001_项目介绍
```

下载的文件内容：
```markdown
# 项目介绍

> **块信息**: 第 1/5 块 | 标签: keyword:项目

---

## 概述

本项目是一个基于 RAG (Retrieval-Augmented Generation) 的知识库问答系统...

[此处是完整的块内容]
```

### 示例 3: 在答案中引用图片

```markdown
系统架构如下图所示：

![系统架构图](/api/images/项目文档/项目文档_uuid-1.png)

主要分为三层...
```

前端渲染后，图片会自动显示。

---

## 📝 配置说明

### application.yml 配置

```yaml
knowledge:
  qa:
    storage:
      # 存储基础路径
      base-path: ./data
      
      # 切分块存储
      chunk:
        enabled: true           # 是否启用块存储
        auto-save: true         # 自动保存切分块
        
      # 图片存储
      image:
        enabled: true           # 是否启用图片存储
        auto-extract: true      # 自动提取图片
        max-size-mb: 10         # 单图片最大大小
```

---

## ✅ 功能检查清单

### 文档切分持久化
- [x] ChunkStorageService 实现
- [x] ChunkStorageInfo 数据模型
- [x] ChunkDownloadController API
- [x] 文件系统存储结构
- [x] Markdown 格式存储
- [x] 带语义标题/编号
- [x] 集成到 SmartContextBuilder ✅
- [x] Spring Bean 配置 ✅
- [x] AIAnswer 扩展支持 ✅
- [ ] 前端下载按钮（待完成）

### 图片支持
- [x] ImageStorageService 实现
- [x] ImageInfo 数据模型
- [x] ImageController API
- [x] 图片 URL 生成
- [x] Markdown 引用替换
- [x] 集成到 KnowledgeQAService ✅
- [ ] 文档图片提取（待集成到文档解析器）
- [x] 前端图片渲染（已支持，Markdown 自动渲染）
- [ ] 图片点击放大（待实现前端JS）

---

## ✅ 已完成的集成工作

### 1. Spring Bean 配置 ✅
- 创建 `StorageConfiguration` 类
- 注册 `ChunkStorageService` Bean
- 注册 `ImageStorageService` Bean

### 2. SmartContextBuilder 集成 ✅
- 添加 `ChunkStorageService` 字段
- 新增带存储服务的构造函数
- 添加 `setCurrentDocumentId()` 方法
- 在 `extractWithChunker()` 中自动保存切分块

### 3. KnowledgeQAService 集成 ✅
- 依赖注入 `ChunkStorageService` 和 `ImageStorageService`
- 在 `createQASystem()` 中传递存储服务到 `SmartContextBuilder`
- 在 `ask()` 方法中：
  - 设置当前文档ID
  - 处理图片引用替换
  - 获取切分块信息
  - 返回完整的 `AIAnswer`

### 4. AIAnswer 扩展 ✅
- 添加 `chunks` 字段
- 添加 `images` 字段
- 提供向后兼容的构造函数

### 5. 编译验证 ✅
- 所有代码编译通过
- 无错误和警告

---

## 🚀 剩余工作

### 前端 UI 更新（待完成）

1. **添加块下载功能** - 在答案区域显示可下载的切分块按钮
2. **图片点击放大** - 添加图片点击放大查看功能
3. **图片样式优化** - CSS 美化

### 文档图片提取（待集成）

需要在文档解析器中添加图片提取逻辑（Word/PDF/PPT等）

---

**实现时间**: 2025-11-26  
**版本**: v1.1  
**状态**: ✅ 后端集成完成，编译通过  
**下一步**: 前端 UI 更新

