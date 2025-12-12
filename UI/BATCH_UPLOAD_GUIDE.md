# 批量上传功能使用指南

## 概述

前端已完全支持后端的批量上传接口 `/api/documents/upload-batch`，可以一次性上传多个文件。

## 实现内容

### 1. API 层 (`document.js`)

添加了 `batchUpload()` 方法：

```javascript
import api from '@api/modules';

// 批量上传文档
const result = await api.document.batchUpload(formData, onProgress);
```

**参数：**
- `formData`: FormData对象，包含多个文件（key为 `files`）
- `onProgress`: 上传进度回调函数 `(percent) => void`

**返回：**
```javascript
{
  total: 3,              // 总文件数
  successCount: 2,       // 成功数
  failureCount: 1,       // 失败数
  message: "...",        // 结果消息
  successFiles: [...],   // 成功文件列表
  failedFiles: [...]     // 失败文件列表
}
```

### 2. 上传组件 (`UploadDropZone.jsx`)

**修改：**
- ✅ `multiple={true}` - 支持多文件选择
- ✅ `name="files"` - 与后端参数名一致
- ✅ `beforeUpload` - 收集所有文件后统一上传

**使用方式：**

```jsx
import UploadDropZone from '@/components/document/UploadDropZone';

<UploadDropZone
  onUpload={handleBatchUpload}  // 接收文件数组
  uploading={uploading}
  progress={progress}
  multiple={true}               // 支持多选
/>
```

### 3. Context 层 (`KnowledgeContext.jsx`)

添加了 `batchUploadDocuments()` 方法：

```jsx
import { useKnowledge } from '@/contexts/KnowledgeContext';

const { batchUploadDocuments } = useKnowledge();

// 使用
const result = await batchUploadDocuments(formData, (percent) => {
  console.log(`上传进度: ${percent}%`);
});
```

### 4. 数据适配器 (`PageDataAdapter.jsx`)

添加了独立的批量上传函数：

```javascript
import { batchUploadDocuments } from '@/adapters/PageDataAdapter';

// 直接使用（不依赖Context）
const result = await batchUploadDocuments(fileArray, onProgress);
```

**特点：**
- ✅ 使用原生 XMLHttpRequest 实现
- ✅ 支持上传进度监听
- ✅ 自动添加语言参数
- ✅ 完整的错误处理

## 使用示例

### 完整示例：批量上传组件

```jsx
import React, { useState } from 'react';
import { message } from 'antd';
import { useLanguage } from '@/contexts/LanguageContext';
import { batchUploadDocuments } from '@/adapters/PageDataAdapter';
import UploadDropZone from '@/components/document/UploadDropZone';

function MyBatchUpload() {
  const { t } = useLanguage();
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleUpload = async (fileList) => {
    // 1. 验证文件
    const validFiles = fileList.filter(file => {
      return file.size / 1024 / 1024 < 100; // < 100MB
    });

    if (validFiles.length === 0) return;

    try {
      setUploading(true);
      
      // 2. 调用批量上传
      const result = await batchUploadDocuments(validFiles, (percent) => {
        setProgress(percent);
      });

      // 3. 处理结果
      if (result.failureCount === 0) {
        message.success(`成功上传 ${result.successCount} 个文件`);
      } else {
        message.warning(
          `上传完成：${result.successCount} 成功，${result.failureCount} 失败`
        );
      }
      
    } catch (error) {
      message.error('上传失败：' + error.message);
    } finally {
      setUploading(false);
      setProgress(0);
    }
  };

  return (
    <UploadDropZone
      onUpload={handleUpload}
      uploading={uploading}
      progress={progress}
      multiple={true}
    />
  );
}
```

### 简化示例：使用 Context

```jsx
import { useKnowledge } from '@/contexts/KnowledgeContext';

function MyComponent() {
  const { batchUploadDocuments, loading } = useKnowledge();

  const handleUpload = async (files) => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    
    await batchUploadDocuments(formData);
    // 上传后自动刷新文档列表
  };

  return (
    <UploadDropZone
      onUpload={handleUpload}
      uploading={loading}
      multiple={true}
    />
  );
}
```

## 后端 API 规范

**接口：** `POST /api/documents/upload-batch`

**请求参数：**
- `files`: MultipartFile[] - 文件数组
- `lang`: String - 语言参数（zh/en），默认 zh

**响应格式：**
```json
{
  "total": 3,
  "successCount": 2,
  "failureCount": 1,
  "message": "批量上传结果：2 成功，1 失败",
  "successFiles": ["file1.pdf", "file2.docx"],
  "failedFiles": ["file3.xlsx"]
}
```

## 代理配置

前端使用 **Vite 代理** 统一转发 API 请求：

**配置文件：** `vite.config.js`
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false
    }
  }
}
```

**工作原理：**
- 前端请求：`/api/documents/upload-batch`
- Vite 代理转发：`http://localhost:8080/api/documents/upload-batch`
- 避免 CORS 问题，统一管理后端地址

**环境变量：**
- 开发环境：使用相对路径 `/api`（由 Vite 代理）
- 生产环境：可配置 `VITE_API_BASE_URL` 环境变量
- 参考：`UI/.env.example`

## 国际化

已添加翻译键：

**中文 (zh.js):**
```javascript
document: {
  uploadHint: '支持 PDF、Word、Excel、PPT 等格式，单个文件不超过 100MB，支持多文件批量上传',
  batchUploadSuccess: '批量上传成功：{success}个成功，{failed}个失败',
  batchUploading: '正在上传 {count} 个文件...',
}
```

**英文 (en.js):**
```javascript
document: {
  uploadHint: 'Support PDF, Word, Excel, PPT formats, max 100MB per file, batch upload supported',
  batchUploadSuccess: 'Batch upload completed: {success} succeeded, {failed} failed',
  batchUploading: 'Uploading {count} files...',
}
```

## 文件位置

- 📄 `UI/src/api/modules/document.js` - API方法
- 📄 `UI/src/components/document/UploadDropZone.jsx` - 上传组件
- 📄 `UI/src/components/document/BatchUploadExample.jsx` - 完整示例
- 📄 `UI/src/contexts/KnowledgeContext.jsx` - Context方法
- 📄 `UI/src/adapters/PageDataAdapter.jsx` - 独立函数
- 📄 `UI/src/lang/zh.js` & `UI/src/lang/en.js` - 翻译文件

## 特性总结

✅ **多文件上传** - 一次选择多个文件  
✅ **进度监听** - 实时显示上传进度  
✅ **文件验证** - 自动检查文件大小  
✅ **批量处理** - 后端批量处理提高效率  
✅ **结果反馈** - 详细的成功/失败统计  
✅ **国际化** - 完整的中英文支持  
✅ **错误处理** - 完善的异常处理机制  

## 注意事项

1. **文件大小限制**：单个文件 < 100MB
2. **FormData 字段名**：必须使用 `files`（复数）
3. **语言参数**：自动从 localStorage 获取
4. **进度更新**：基于整体上传进度，非单文件进度
5. **自动刷新**：使用 Context 方法会自动刷新文档列表
