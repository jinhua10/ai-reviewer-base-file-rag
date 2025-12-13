# 📐 QA Controller 架构重构方案

> **文档编号**: 20251213-QA-Controller-Refactor  
> **创建日期**: 2025-12-13  
> **类型**: 架构重构方案  
> **状态**: 🔄 进行中

---

## 🎯 重构目标

整合 `KnowledgeQAController` 和 `StreamingQAController`，实现统一、清晰的 API 架构，支持：
1. **单轨流式**：简单的流式输出（当前实现）
2. **双轨流式**：HOPE 快速答案 + LLM 详细答案（计划实现）

---

## 📊 当前架构分析

### KnowledgeQAController (`/api/qa`)

#### 问答接口
- `POST /ask` - 非流式问答 ✅
- `POST /ask-stream` - **单轨流式**问答 ⚠️
- `POST /ask-with-session` - 会话文档问答 ✅

#### 管理接口
- `GET /search` - 文档搜索 ✅
- `GET /statistics` - 统计信息 ✅
- `GET /health` - 健康检查 ✅
- `POST /rebuild` - 重建索引 ✅
- `POST /incremental-index` - 增量索引 ✅
- `GET /indexing-status` - 索引状态 ✅

#### 辅助接口
- `GET /similar` - 相似问题 ✅
- `GET /archive/statistics` - 归档统计 ✅
- `GET /role/leaderboard` - 角色排行榜 ✅
- `GET /bounty/active` - 活跃悬赏 ✅
- `POST /bounty/{bountyId}/submit` - 提交悬赏答案 ✅

**特点**:
- ✅ 接口丰富，功能完整
- ✅ 支持三种知识库模式（none/rag/role）
- ⚠️ 流式接口只支持单轨输出

---

### StreamingQAController (`/api/qa/stream`)

#### 双轨流式接口
- `POST /` - 创建会话，返回 HOPE 快速答案 ✅
- `GET /{sessionId}` - SSE 订阅 LLM 流式输出 ✅
- `GET /{sessionId}/status` - 查询会话状态 ✅
- `GET /dual-track` - 双轨流式（SSE）✅

**特点**:
- ✅ 真正的双轨架构
- ✅ HOPE 快速答案（<300ms）
- ✅ LLM 详细答案（流式）
- ✅ 会话管理完善
- ⚠️ 前端未使用此接口

---

## 🔍 架构问题

### 问题 1：接口重复

| 功能 | KnowledgeQAController | StreamingQAController |
|------|----------------------|----------------------|
| 流式问答 | `/ask-stream` (单轨) | `/` + `/{sessionId}` (双轨) |
| 模式支持 | none/rag/role | 仅 rag |

**影响**:
- 前端调用 `/ask-stream`，但**只能获得单轨输出**
- 双轨架构的 `/stream` 接口未被使用
- 架构不统一，维护复杂

### 问题 2：前端调用不匹配

**前端期望**:
```javascript
// 期望：双轨流式（HOPE + LLM）
POST /api/qa/ask-stream
```

**实际返回**:
```javascript
// 实际：单轨流式（仅 LLM）
Flux<String> // 只有 LLM 流式输出
```

**缺失**:
- ❌ 没有 HOPE 快速答案
- ❌ 没有会话管理
- ❌ 无法区分 HOPE 和 LLM 输出

---

## 💡 重构方案

### 方案 A：升级 `/ask-stream` 为双轨架构（推荐）

**目标**: 让前端当前使用的 `/ask-stream` 支持双轨输出

#### 架构设计

```
POST /api/qa/ask-stream
    ↓
返回 SSE 流：
    1. event: hope     - HOPE 快速答案
       data: {...}
    2. event: llm      - LLM 流式块
       data: "chunk1"
    3. event: llm
       data: "chunk2"
    ...
    4. event: complete - 完成标记
       data: {...}
```

#### 优点
- ✅ 前端无需修改接口调用
- ✅ 统一的接口路径
- ✅ 支持双轨输出
- ✅ 向后兼容

#### 实现步骤
1. 修改 `KnowledgeQAController.askStream()` 返回类型为 `SseEmitter`
2. 集成 `HybridStreamingService` 实现双轨
3. 返回结构化 SSE 事件（hope/llm/complete）

---

### 方案 B：合并两个 Controller

**目标**: 将 `StreamingQAController` 的功能合并到 `KnowledgeQAController`

#### 架构设计

```
KnowledgeQAController (/api/qa)
├─ 问答接口
│  ├─ POST /ask                    - 非流式
│  ├─ POST /ask-stream            - 双轨流式（SSE）
│  └─ POST /ask-with-session      - 会话问答
│
├─ 流式管理（从 StreamingQAController 移入）
│  ├─ GET /stream/{sessionId}     - 订阅流式输出
│  └─ GET /stream/{sessionId}/status - 会话状态
│
└─ 其他接口
   ├─ GET /search
   ├─ GET /statistics
   └─ ...
```

#### 优点
- ✅ 统一的 Controller
- ✅ 更清晰的接口组织
- ✅ 减少代码重复
- ✅ 易于维护

#### 缺点
- ⚠️ 需要删除 `StreamingQAController`
- ⚠️ 可能影响已有的其他调用

---

### 方案 C：保持现状，明确职责分工

**目标**: 两个 Controller 各司其职

#### 职责划分

**KnowledgeQAController** (`/api/qa`):
- 所有问答接口（非流式 + 单轨流式）
- 索引管理
- 统计查询
- 辅助功能

**StreamingQAController** (`/api/qa/stream`):
- **仅**双轨流式架构
- 会话管理
- SSE 流管理

#### 优点
- ✅ 职责清晰
- ✅ 不影响现有代码
- ✅ 双轨架构独立维护

#### 缺点
- ⚠️ 前端需要**明确选择**使用哪个接口
- ⚠️ 接口路径不统一（/ask-stream vs /stream）

---

## 🎯 推荐方案：方案 A

### 理由

1. **前端兼容性最好**
   - 前端已经调用 `/ask-stream`
   - 只需调整返回格式处理
   - 无需修改接口路径

2. **架构最清晰**
   - 统一入口：`/api/qa/ask-stream`
   - 双轨输出：HOPE + LLM
   - 向后兼容：支持所有模式（none/rag/role）

3. **实现成本最低**
   - 复用现有 `HybridStreamingService`
   - 只需修改一个方法
   - 不影响其他接口

---

## 📋 实施计划

### Phase 1: 升级 `/ask-stream` 为双轨架构

#### 1.1 修改返回类型

**修改前**:
```java
@PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> askStream(@RequestBody QuestionRequest request) {
    // 返回单轨流式
    return qaService.askStream(question, sessionId);
}
```

**修改后**:
```java
@PostMapping("/ask-stream")
public ResponseEntity<Map<String, Object>> askStreamDualTrack(
        @RequestBody QuestionRequest request) {
    // 1. 创建会话
    // 2. 返回 sessionId + HOPE 快速答案 + SSE URL
    return ResponseEntity.ok(Map.of(
        "sessionId", sessionId,
        "hopeAnswer", hopeAnswer,
        "sseUrl", "/api/qa/stream/" + sessionId
    ));
}
```

#### 1.2 添加 SSE 订阅接口

```java
@GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribeLLMStream(@PathVariable String sessionId) {
    // 返回 LLM 流式输出
    return hybridStreamingService.createSSEStream(sessionId);
}
```

#### 1.3 注入 HybridStreamingService

```java
private final HybridStreamingService hybridStreamingService;

@Autowired
public KnowledgeQAController(
        KnowledgeQAService qaService,
        SimilarQAService similarQAService,
        QAArchiveService qaArchiveService,
        RoleKnowledgeQAService roleKnowledgeQAService,
        HybridStreamingService hybridStreamingService) { // 新增
    // ...
    this.hybridStreamingService = hybridStreamingService;
}
```

---

### Phase 2: 废弃 StreamingQAController

#### 2.1 标记为 @Deprecated

```java
@Deprecated(since = "2025-12-13", forRemoval = true)
@RestController
@RequestMapping("/api/qa/stream")
public class StreamingQAController {
    // 保留接口但标记废弃
}
```

#### 2.2 添加迁移说明

```java
/**
 * @deprecated 请使用 KnowledgeQAController.askStreamDualTrack()
 * 
 * 迁移路径:
 * - POST /api/qa/stream → POST /api/qa/ask-stream
 * - GET /api/qa/stream/{sessionId} → GET /api/qa/stream/{sessionId}
 */
```

---

### Phase 3: 前端适配

#### 3.1 前端调用流程

```javascript
// 1. 发起流式问答
const response = await fetch('/api/qa/ask-stream', {
  method: 'POST',
  body: JSON.stringify({ question, knowledgeMode, roleName })
})

const { sessionId, hopeAnswer, sseUrl } = await response.json()

// 2. 显示 HOPE 快速答案
if (hopeAnswer) {
  displayHopeAnswer(hopeAnswer)
}

// 3. 订阅 LLM 流式输出
const eventSource = new EventSource(sseUrl)

eventSource.addEventListener('llm', (event) => {
  appendLLMChunk(event.data)
})

eventSource.addEventListener('complete', () => {
  eventSource.close()
})
```

---

## 📊 对比总结

### 方案对比

| 维度 | 方案 A（推荐） | 方案 B | 方案 C |
|------|---------------|--------|--------|
| 前端兼容性 | ✅ 最好 | ⚠️ 需调整 | ⚠️ 需选择 |
| 架构清晰度 | ✅ 清晰 | ✅ 最清晰 | ⚠️ 分散 |
| 实现成本 | ✅ 最低 | ⚠️ 中等 | ✅ 最低 |
| 维护成本 | ✅ 低 | ✅ 最低 | ⚠️ 中等 |
| 向后兼容 | ✅ 完全 | ❌ 不兼容 | ✅ 完全 |

---

## 🚀 下一步行动

### 立即执行（方案 A）

1. ✅ **修改 KnowledgeQAController**
   - 改造 `askStream()` 方法
   - 添加 SSE 订阅接口
   - 注入 `HybridStreamingService`

2. ✅ **标记 StreamingQAController 为废弃**
   - 添加 `@Deprecated` 注解
   - 添加迁移说明文档

3. ✅ **测试验证**
   - 测试双轨流式输出
   - 验证三种模式（none/rag/role）
   - 检查 HOPE + LLM 输出

4. ✅ **前端适配**
   - 调整响应处理逻辑
   - 支持双轨显示
   - 测试用户体验

---

## 📝 API 设计文档

### 统一的流式问答接口

#### POST /api/qa/ask-stream

**请求**:
```json
{
  "question": "如何优化数据库？",
  "knowledgeMode": "role",
  "roleName": "developer",
  "useKnowledgeBase": true
}
```

**响应**:
```json
{
  "sessionId": "abc123",
  "question": "如何优化数据库？",
  "hopeAnswer": {
    "answer": "HOPE 快速答案...",
    "source": "CONCEPT_LAYER",
    "confidence": 0.85,
    "canDirectAnswer": false
  },
  "sseUrl": "/api/qa/stream/abc123",
  "knowledgeMode": "role",
  "roleName": "developer"
}
```

#### GET /api/qa/stream/{sessionId}

**响应**（SSE 流）:
```
event: llm
data: 作为开发者，

event: llm
data: 我可以从以下几个方面

event: llm
data: 帮你优化数据库...

event: complete
data: {"totalChunks": 50, "totalTime": 3000}
```

---

**架构师**: AI Assistant  
**方案日期**: 2025-12-13  
**推荐方案**: 方案 A - 升级 `/ask-stream` 为双轨架构  
**实施优先级**: 🔥 高

🎯 **下一步**: 立即开始实施方案 A！

