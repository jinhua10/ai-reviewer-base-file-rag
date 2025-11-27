# 文档管理 API 文档

## 📚 API 概览

本文档描述文档管理系统的 RESTful API 接口。

**基础路径**: `/api/documents`

**内容类型**: `application/json`

## 📋 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/list` | 获取文档列表（支持分页、排序、搜索） |
| POST | `/upload` | 上传单个文档 |
| POST | `/upload-batch` | 批量上传文档 |
| DELETE | `/{fileName}` | 删除文档 |
| DELETE | `/batch` | 批量删除文档 |
| GET | `/download/{fileName}` | 下载文档 |
| POST | `/download-batch` | 批量下载文档 |

---

## 1. 获取文档列表

获取文档列表，支持分页、排序和搜索功能。

### 请求

**方法**: `GET`

**路径**: `/api/documents/list`

**参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码，从1开始 |
| pageSize | int | 否 | 20 | 每页数量，-1表示全部 |
| sortBy | string | 否 | date | 排序字段：name, size, date, type |
| sortOrder | string | 否 | desc | 排序方向：asc, desc |
| search | string | 否 | "" | 搜索关键词 |

### 请求示例

```http
GET /api/documents/list?page=2&pageSize=20&sortBy=size&sortOrder=desc&search=report HTTP/1.1
Host: localhost:8080
```

```bash
# cURL 示例
curl "http://localhost:8080/api/documents/list?page=2&pageSize=20&sortBy=size&sortOrder=desc&search=report"
```

```javascript
// JavaScript 示例
const response = await fetch('/api/documents/list?' + new URLSearchParams({
    page: 2,
    pageSize: 20,
    sortBy: 'size',
    sortOrder: 'desc',
    search: 'report'
}));
const data = await response.json();
```

### 响应

**成功响应** (200 OK):

```json
{
    "success": true,
    "message": null,
    "total": 100,
    "documents": [
        {
            "fileName": "annual_report_2024.pdf",
            "fileSize": 2097152,
            "fileType": "pdf",
            "uploadTime": "2025-11-27 10:30:00",
            "indexed": true
        },
        {
            "fileName": "monthly_report_nov.docx",
            "fileSize": 1048576,
            "fileType": "docx",
            "uploadTime": "2025-11-27 09:15:00",
            "indexed": true
        }
    ],
    "page": 2,
    "pageSize": 20,
    "totalPages": 5
}
```

**失败响应** (200 OK):

```json
{
    "success": false,
    "message": "获取列表失败: 详细错误信息",
    "total": 0,
    "documents": null,
    "page": 0,
    "pageSize": 0,
    "totalPages": 0
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 操作是否成功 |
| message | string | 错误消息（成功时为null） |
| total | int | 总文档数（过滤后） |
| documents | array | 文档列表 |
| ├─ fileName | string | 文件名 |
| ├─ fileSize | long | 文件大小（字节） |
| ├─ fileType | string | 文件类型 |
| ├─ uploadTime | string | 上传时间 |
| └─ indexed | boolean | 是否已索引 |
| page | int | 当前页码 |
| pageSize | int | 每页数量 |
| totalPages | int | 总页数 |

### 使用示例

**示例 1: 获取第一页**
```
GET /api/documents/list
```
返回前 20 个文档，按上传时间降序。

**示例 2: 查找所有 PDF**
```
GET /api/documents/list?search=.pdf
```
搜索所有 PDF 文件。

**示例 3: 查看最大的文件**
```
GET /api/documents/list?sortBy=size&sortOrder=desc&pageSize=10
```
按大小降序，返回前 10 个最大的文件。

**示例 4: 显示所有文档**
```
GET /api/documents/list?pageSize=-1
```
返回所有文档，不分页。

---

## 2. 上传文档

上传单个文档文件。

### 请求

**方法**: `POST`

**路径**: `/api/documents/upload`

**Content-Type**: `multipart/form-data`

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 要上传的文件 |

### 请求示例

```http
POST /api/documents/upload HTTP/1.1
Host: localhost:8080
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="document.pdf"
Content-Type: application/pdf

[文件内容]
------WebKitFormBoundary--
```

```bash
# cURL 示例
curl -X POST \
  -F "file=@/path/to/document.pdf" \
  http://localhost:8080/api/documents/upload
```

```javascript
// JavaScript 示例
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('/api/documents/upload', {
    method: 'POST',
    body: formData
});
const data = await response.json();
```

### 响应

**成功响应** (200 OK):

```json
{
    "success": true,
    "message": "文档上传成功",
    "fileName": "document.pdf",
    "fileSize": 1048576,
    "documentId": "doc_123456"
}
```

**失败响应** (200 OK):

```json
{
    "success": false,
    "message": "上传失败: 文件为空",
    "fileName": null,
    "fileSize": 0,
    "documentId": null
}
```

---

## 3. 批量上传文档

一次上传多个文档文件。

### 请求

**方法**: `POST`

**路径**: `/api/documents/upload-batch`

**Content-Type**: `multipart/form-data`

**参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | file[] | 是 | 要上传的文件数组 |

### 请求示例

```bash
# cURL 示例
curl -X POST \
  -F "files=@/path/to/doc1.pdf" \
  -F "files=@/path/to/doc2.docx" \
  -F "files=@/path/to/doc3.xlsx" \
  http://localhost:8080/api/documents/upload-batch
```

```javascript
// JavaScript 示例
const formData = new FormData();
for (let file of fileInput.files) {
    formData.append('files', file);
}

const response = await fetch('/api/documents/upload-batch', {
    method: 'POST',
    body: formData
});
const data = await response.json();
```

### 响应

**成功响应** (200 OK):

```json
{
    "total": 3,
    "successCount": 2,
    "failureCount": 1,
    "message": "成功: 2, 失败: 1",
    "successFiles": [
        "doc1.pdf",
        "doc2.docx"
    ],
    "failedFiles": [
        "doc3.xlsx"
    ]
}
```

---

## 4. 删除文档

删除指定的文档。

### 请求

**方法**: `DELETE`

**路径**: `/api/documents/{fileName}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | string | 是 | 要删除的文件名 |

### 请求示例

```http
DELETE /api/documents/document.pdf HTTP/1.1
Host: localhost:8080
```

```bash
# cURL 示例
curl -X DELETE http://localhost:8080/api/documents/document.pdf
```

```javascript
// JavaScript 示例
const response = await fetch('/api/documents/document.pdf', {
    method: 'DELETE'
});
const data = await response.json();
```

### 响应

**成功响应** (200 OK):

```json
{
    "success": true,
    "message": "文档删除成功",
    "fileName": "document.pdf"
}
```

**失败响应** (200 OK):

```json
{
    "success": false,
    "message": "文档不存在",
    "fileName": null
}
```

---

## 5. 批量删除文档

一次删除多个文档。

### 请求

**方法**: `DELETE`

**路径**: `/api/documents/batch`

**Content-Type**: `application/json`

**请求体**:

```json
[
    "document1.pdf",
    "document2.docx",
    "document3.xlsx"
]
```

### 请求示例

```bash
# cURL 示例
curl -X DELETE \
  -H "Content-Type: application/json" \
  -d '["document1.pdf", "document2.docx", "document3.xlsx"]' \
  http://localhost:8080/api/documents/batch
```

```javascript
// JavaScript 示例
const response = await fetch('/api/documents/batch', {
    method: 'DELETE',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify([
        'document1.pdf',
        'document2.docx',
        'document3.xlsx'
    ])
});
const data = await response.json();
```

### 响应

**成功响应** (200 OK):

```json
{
    "total": 3,
    "successCount": 2,
    "failureCount": 1,
    "message": "成功: 2, 失败: 1",
    "successFiles": [
        "document1.pdf",
        "document2.docx"
    ],
    "failedFiles": [
        "document3.xlsx"
    ]
}
```

---

## 6. 下载文档

下载指定的文档文件。

### 请求

**方法**: `GET`

**路径**: `/api/documents/download/{fileName}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | string | 是 | 要下载的文件名 |

### 请求示例

```http
GET /api/documents/download/document.pdf HTTP/1.1
Host: localhost:8080
```

```bash
# cURL 示例
curl -O http://localhost:8080/api/documents/download/document.pdf
```

```javascript
// JavaScript 示例
window.location.href = '/api/documents/download/document.pdf';

// 或使用 fetch
const response = await fetch('/api/documents/download/document.pdf');
const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const link = document.createElement('a');
link.href = url;
link.download = 'document.pdf';
link.click();
```

### 响应

**成功响应** (200 OK):

```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="document.pdf"

[文件内容]
```

**失败响应** (404 Not Found):

```
文件不存在
```

---

## 7. 批量下载文档

下载多个文档，打包为 ZIP 文件。

### 请求

**方法**: `POST`

**路径**: `/api/documents/download-batch`

**Content-Type**: `application/json`

**请求体**:

```json
[
    "document1.pdf",
    "document2.docx",
    "document3.xlsx"
]
```

### 请求示例

```bash
# cURL 示例
curl -X POST \
  -H "Content-Type: application/json" \
  -d '["document1.pdf", "document2.docx", "document3.xlsx"]' \
  -o documents.zip \
  http://localhost:8080/api/documents/download-batch
```

```javascript
// JavaScript 示例
const response = await fetch('/api/documents/download-batch', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify([
        'document1.pdf',
        'document2.docx',
        'document3.xlsx'
    ])
});
const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
const link = document.createElement('a');
link.href = url;
link.download = `documents_${Date.now()}.zip`;
link.click();
```

### 响应

**成功响应** (200 OK):

```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="documents_1732766400000.zip"

[ZIP文件内容]
```

---

## 📊 错误码

| HTTP状态码 | 说明 |
|-----------|------|
| 200 | 成功（包括业务失败，通过 success 字段判断） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 🔐 认证

当前版本暂无认证机制。未来版本可能添加：

- JWT Token 认证
- API Key 认证
- OAuth 2.0

## ⚡ 限流

建议实现以下限流策略：

| 接口 | 限制 |
|------|------|
| `/upload` | 每分钟 60 次 |
| `/upload-batch` | 每分钟 10 次 |
| `/list` | 每秒 100 次 |
| `/delete` | 每分钟 60 次 |
| `/download` | 每分钟 120 次 |

## 📝 注意事项

1. **文件大小限制**: 默认 100MB，可在配置文件中修改
2. **支持的文件类型**: PDF, Word, Excel, PowerPoint, TXT, MD, HTML, XML
3. **文件名编码**: 使用 UTF-8 编码
4. **并发上传**: 支持，但建议不超过 10 个文件
5. **临时文件**: 批量下载的 ZIP 文件会在下载完成后自动删除

## 🔄 版本历史

### v1.0 (2025-11-27)

- ✅ 基础文档管理功能
- ✅ 分页、排序、搜索支持
- ✅ 批量操作支持
- ✅ 文件上传下载

---

**文档版本**: v1.0  
**最后更新**: 2025-11-27  
**维护团队**: AI Reviewer Team

