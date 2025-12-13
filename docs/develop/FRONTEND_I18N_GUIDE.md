# 前端国际化使用指南

> **文档编号**: FRONTEND_I18N_GUIDE  
> **创建时间**: 2025-12-13  
> **最后更新**: 2025-12-13

## 📋 概述

本文档说明前端如何正确调用后端API以支持中英文切换，确保用户切换语言时消息能够实时响应。

---

## 🎯 核心原则

1. **所有API请求必须携带 `Accept-Language` Header**
2. **后端使用 `I18N.getLang()` 返回国际化消息**
3. **前端切换语言后需要刷新数据**

---

## 📡 前端调用示例

### 1. Axios 配置（推荐）

```javascript
// src/utils/request.js
import axios from 'axios';

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
});

// 请求拦截器：自动添加语言 Header
request.interceptors.request.use(config => {
  // 从 localStorage 获取当前语言
  const lang = localStorage.getItem('language') || 'zh';
  config.headers['Accept-Language'] = lang;
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器：处理错误消息
request.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.error || 'Request failed';
    console.error(message);
    return Promise.reject(error);
  }
);

export default request;
```

### 2. API 调用示例

```javascript
// src/api/role.js
import request from '@/utils/request';

// 获取角色列表（自动携带语言 Header）
export function getRoleList(params) {
  return request.get('/roles', { params });
}

// 创建角色
export function createRole(data) {
  return request.post('/roles', data);
}

// 更新角色
export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data);
}

// 删除角色
export function deleteRole(id) {
  return request.delete(`/roles/${id}`);
}

// 检测角色
export function detectRole(question) {
  return request.post('/roles/detect', { question });
}
```

### 3. 组件中使用

```javascript
// src/components/RoleList.jsx
import React, { useState, useEffect } from 'react';
import { getRoleList, createRole } from '@/api/role';
import { message } from 'antd';

export default function RoleList() {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);

  // 加载角色列表
  const loadRoles = async () => {
    setLoading(true);
    try {
      const response = await getRoleList({
        page: 1,
        pageSize: 10
      });
      setRoles(response.list);
      // response.message 会根据 Accept-Language 自动显示中英文
    } catch (error) {
      message.error(error.response?.data?.error || 'Failed to load roles');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRoles();
  }, []);

  // 创建角色
  const handleCreate = async (roleData) => {
    try {
      const response = await createRole(roleData);
      // response.message 是国际化的消息
      message.success(response.message);
      loadRoles(); // 刷新列表
    } catch (error) {
      message.error(error.response?.data?.error || 'Failed to create role');
    }
  };

  return (
    <div>
      {/* 角色列表 UI */}
    </div>
  );
}
```

### 4. 语言切换功能

```javascript
// src/components/LanguageSwitch.jsx
import React from 'react';
import { Select } from 'antd';

export default function LanguageSwitch({ onLanguageChange }) {
  const currentLang = localStorage.getItem('language') || 'zh';

  const handleChange = (lang) => {
    // 保存到 localStorage
    localStorage.setItem('language', lang);
    
    // 通知父组件刷新数据
    if (onLanguageChange) {
      onLanguageChange(lang);
    }
    
    // 刷新页面（简单方式）
    // window.location.reload();
  };

  return (
    <Select 
      value={currentLang} 
      onChange={handleChange}
      style={{ width: 120 }}
    >
      <Select.Option value="zh">中文</Select.Option>
      <Select.Option value="en">English</Select.Option>
    </Select>
  );
}
```

### 5. App 组件集成语言切换

```javascript
// src/App.jsx
import React, { useState, useEffect } from 'react';
import LanguageSwitch from './components/LanguageSwitch';
import RoleList from './components/RoleList';

export default function App() {
  const [language, setLanguage] = useState(
    localStorage.getItem('language') || 'zh'
  );

  // 语言切换处理
  const handleLanguageChange = (newLang) => {
    setLanguage(newLang);
    // 刷新所有组件数据
    window.location.reload();
  };

  return (
    <div className="app">
      <header>
        <LanguageSwitch onLanguageChange={handleLanguageChange} />
      </header>
      <main>
        <RoleList key={language} /> {/* 使用 key 强制重新渲染 */}
      </main>
    </div>
  );
}
```

---

## 🔍 API 响应格式

### 成功响应示例

```json
{
  "data": {
    "id": "role-123",
    "name": "Java开发工程师",
    "enabled": true
  },
  "message": "✅ 创建角色成功: Java开发工程师 (role-123)"
}
```

切换为英文后（`Accept-Language: en`）：

```json
{
  "data": {
    "id": "role-123",
    "name": "Java开发工程师",
    "enabled": true
  },
  "message": "✅ Role created successfully: Java开发工程师 (role-123)"
}
```

### 错误响应示例

```json
{
  "error": "⚠️ 角色不存在: role-999"
}
```

切换为英文后：

```json
{
  "error": "⚠️ Role not found: role-999"
}
```

---

## ✅ 检查清单

### 后端开发者

- [ ] Controller 所有方法添加 `@RequestHeader(value = "Accept-Language", defaultValue = "zh") String lang` 参数
- [ ] 所有返回前端的消息使用 `I18N.getLang(key, lang, args)`
- [ ] Service 层日志使用 `I18N.get(key, args)`（不需要 lang）
- [ ] 异常处理器支持 lang 参数
- [ ] 所有国际化键值在 `zh` 和 `en` 目录都有对应的 YAML 文件

### 前端开发者

- [ ] Axios 拦截器自动添加 `Accept-Language` Header
- [ ] 语言切换后刷新数据或重新加载页面
- [ ] 错误处理显示后端返回的国际化消息
- [ ] 提供语言切换组件（中文/English）
- [ ] localStorage 保存用户语言偏好

---

## 🚨 常见错误

### ❌ 错误 1: 前端未发送语言 Header

```javascript
// 错误：直接使用 fetch，未添加 Header
fetch('/api/roles')
  .then(res => res.json())
  .then(data => console.log(data.message)); // 消息语言不会切换
```

**解决方案**：使用配置好的 axios 实例或手动添加 Header。

---

### ❌ 错误 2: 后端使用 I18N.get() 而非 getLang()

```java
// 错误：Controller 使用 get()
@GetMapping("/api/roles")
public ResponseEntity<?> getRoles() {
    return ResponseEntity.ok(Map.of(
        "message", I18N.get("role.list.loaded", count) // 语言固定，不会切换
    ));
}
```

**解决方案**：使用 `I18N.getLang(key, lang, args)`。

---

### ❌ 错误 3: 前端切换语言后未刷新数据

```javascript
// 错误：只改变语言状态，数据不刷新
const switchLanguage = (lang) => {
  localStorage.setItem('language', lang);
  setLanguage(lang); // 数据还是旧语言的
};
```

**解决方案**：切换语言后重新调用 API 或刷新页面。

---

## 📚 相关文档

- [代码规范 - 国际化部分](../refactor/20251209-23-00-00-CODE_STANDARDS.md#规则-52-前端消息国际化强制要求)
- [I18N 工具类文档](../../src/main/java/top/yumbo/ai/rag/i18n/I18N.java)
- [角色管理 API 文档](../../src/main/java/top/yumbo/ai/rag/spring/boot/controller/RoleController.java)

---

## 🎓 最佳实践

1. **统一封装 Axios**：创建统一的请求工具，自动处理语言 Header
2. **全局拦截器**：统一处理错误消息和国际化响应
3. **语言持久化**：使用 localStorage 保存用户语言偏好
4. **响应式刷新**：切换语言时自动刷新所有国际化内容
5. **降级处理**：后端未返回 message 时，前端提供默认消息

---

## 🔧 调试技巧

### 查看请求 Header

```javascript
// Chrome DevTools -> Network -> 选择请求 -> Headers
Accept-Language: zh
```

### 测试不同语言

```bash
# 使用 curl 测试中文
curl -H "Accept-Language: zh" http://localhost:8080/api/roles

# 使用 curl 测试英文
curl -H "Accept-Language: en" http://localhost:8080/api/roles
```

### 验证国际化文件

```bash
# 检查中文文件
cat src/main/resources/i18n/zh/zh-role.yml

# 检查英文文件
cat src/main/resources/i18n/en/en-role.yml
```

---

**祝开发顺利！如有问题，请参考代码规范或联系团队成员。** 🚀
