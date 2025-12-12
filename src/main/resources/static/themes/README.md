# 主题管理服务说明 / Theme Management Service Documentation

## 📋 概述 / Overview

本服务提供了完整的主题管理功能，包括主题的上传、查询、删除和同步。
This service provides complete theme management functionality, including upload, query, delete, and sync.

---

## 🚀 API接口 / API Endpoints

### 1. 上传主题 / Upload Theme

**请求 / Request:**
```http
POST /api/themes/upload
Content-Type: multipart/form-data

参数 / Parameters:
- themeConfig: String (JSON格式的主题配置)
- files: MultipartFile[] (可选，主题相关文件)
```

**响应 / Response:**
```json
{
  "success": true,
  "themeId": "custom-xxx",
  "path": "/static/themes/custom-xxx",
  "message": "主题上传成功 / Theme uploaded successfully"
}
```

**示例 / Example:**
```bash
curl -X POST http://localhost:8080/api/themes/upload \
  -F "themeConfig={\"id\":\"my-theme\",\"name\":{\"zh\":\"我的主题\"}}" \
  -F "files=@theme.css" \
  -F "files=@preview.png"
```

---

### 2. 获取主题列表 / Get Theme List

**请求 / Request:**
```http
GET /api/themes/list
```

**响应 / Response:**
```json
[
  {
    "id": "custom-xxx",
    "name": {"zh": "我的主题", "en": "My Theme"},
    "description": {"zh": "描述", "en": "Description"},
    "type": "custom",
    "source": "server",
    "version": "1.0.0",
    "author": "Author Name",
    "uploadDate": "2025-12-12T18:00:00"
  }
]
```

**示例 / Example:**
```bash
curl http://localhost:8080/api/themes/list
```

---

### 3. 获取主题详情 / Get Theme Details

**请求 / Request:**
```http
GET /api/themes/{themeId}
```

**响应 / Response:**
```json
{
  "id": "custom-xxx",
  "name": {"zh": "我的主题"},
  "config": {
    "layout": "modern",
    "animation": "smooth"
  }
}
```

**示例 / Example:**
```bash
curl http://localhost:8080/api/themes/my-theme
```

---

### 4. 删除主题 / Delete Theme

**请求 / Request:**
```http
DELETE /api/themes/{themeId}
```

**响应 / Response:**
```json
{
  "success": true,
  "message": "主题删除成功 / Theme deleted successfully"
}
```

**示例 / Example:**
```bash
curl -X DELETE http://localhost:8080/api/themes/my-theme
```

---

### 5. 同步主题 / Sync Theme

**请求 / Request:**
```http
PUT /api/themes/sync
Content-Type: application/json

Body: {主题配置对象}
```

**响应 / Response:**
```json
{
  "success": true,
  "message": "主题同步成功 / Theme synced successfully"
}
```

**示例 / Example:**
```bash
curl -X PUT http://localhost:8080/api/themes/sync \
  -H "Content-Type: application/json" \
  -d '{"id":"my-theme","name":{"zh":"更新的主题"}}'
```

---

### 6. 健康检查 / Health Check

**请求 / Request:**
```http
GET /api/themes/health
```

**响应 / Response:**
```json
{
  "status": "healthy",
  "service": "Theme Management Service",
  "timestamp": 1702368000000
}
```

---

## 📁 文件结构 / File Structure

```
src/main/resources/static/themes/
└── {themeId}/
    ├── theme.json          # 主题配置文件
    ├── layout.jsx          # 布局组件（可选）
    ├── styles.css          # 样式文件（可选）
    └── assets/             # 资源文件夹（可选）
        ├── preview.png     # 预览图
        └── decorations/    # 装饰元素
```

---

## ⚙️ 配置说明 / Configuration

在 `application.yml` 中配置：

```yaml
theme:
  # 主题上传路径
  upload-path: src/main/resources/static/themes/
  # 最大文件大小（10MB）
  max-file-size: 10485760
  # 允许的文件类型
  allowed-types:
    - application/json
    - text/css
    - image/png
```

---

## 🔒 安全配置 / Security Configuration

### 文件类型限制 / File Type Restrictions
- 只允许配置中指定的文件类型
- Only allowed file types specified in configuration

### 文件大小限制 / File Size Limit
- 默认最大10MB
- Default maximum 10MB

### 路径安全 / Path Security
- 自动验证路径，防止目录遍历攻击
- Automatic path validation to prevent directory traversal

---

## 📊 日志说明 / Logging

服务使用SLF4J记录详细日志：

```
✅ 成功操作 / Success operations
❌ 失败操作 / Failed operations
⚠️ 警告信息 / Warning messages
📦 数据操作 / Data operations
```

---

## 🧪 测试 / Testing

### 使用Postman测试

1. **上传主题**
   - Method: POST
   - URL: http://localhost:8080/api/themes/upload
   - Body: form-data
     - themeConfig: {"id":"test-theme","name":{"zh":"测试主题"}}
     - files: 选择文件

2. **获取列表**
   - Method: GET
   - URL: http://localhost:8080/api/themes/list

3. **查看详情**
   - Method: GET
   - URL: http://localhost:8080/api/themes/test-theme

4. **删除主题**
   - Method: DELETE
   - URL: http://localhost:8080/api/themes/test-theme

---

## 🐛 故障排除 / Troubleshooting

### 问题1: 上传失败
**原因**: 文件大小超过限制
**解决**: 检查 `application.yml` 中的 `max-file-size` 配置

### 问题2: 找不到主题
**原因**: 主题目录不存在
**解决**: 检查 `upload-path` 配置是否正确

### 问题3: 文件类型不允许
**原因**: 文件类型不在允许列表中
**解决**: 更新 `allowed-types` 配置

---

## 📝 开发说明 / Development Notes

### 添加新的文件类型支持

在 `application.yml` 中添加：
```yaml
theme:
  allowed-types:
    - your/mime-type
```

### 修改存储路径

```yaml
theme:
  upload-path: /custom/path/themes/
```

注意：确保应用有该路径的读写权限。
Note: Ensure the application has read/write permissions for the path.

---

## 🔄 版本历史 / Version History

- **v1.0.0** (2025-12-12)
  - ✅ 初始版本发布
  - ✅ 实现所有核心功能
  - ✅ 支持多文件上传
  - ✅ 完整的错误处理

---

## 👥 维护者 / Maintainers

AI Reviewer Team

---

## 📄 许可证 / License

本项目使用 Apache License 2.0
This project is licensed under Apache License 2.0

