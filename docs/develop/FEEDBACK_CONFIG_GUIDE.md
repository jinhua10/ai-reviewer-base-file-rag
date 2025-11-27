# 📊 反馈系统配置和动态权重管理指南

## 🎯 功能概述

实现了基于用户反馈的文档权重动态调整系统，支持：
1. **YAML配置** - 通过配置文件控制反馈生效方式
2. **直接生效模式** - 用户反馈直接影响文档权重（默认）
3. **审核模式** - 反馈需要管理员审核后才生效
4. **动态切换** - 可通过API动态修改配置

---

## 📋 配置说明

### application.yml 配置

```yaml
# 反馈系统配置
feedback:
  # 是否需要审核才能生效（默认 false - 直接生效）
  require-approval: false
  
  # 是否自动应用反馈到检索优化（默认 true）
  auto-apply: true
  
  # 点赞权重增量（默认 0.1）
  like-weight-increment: 0.1
  
  # 踩的权重减量（默认 -0.15）
  dislike-weight-decrement: -0.15
  
  # 最小权重限制（默认 0.1）
  min-weight: 0.1
  
  # 最大权重限制（默认 2.0）
  max-weight: 2.0
  
  # 是否启用动态权重调整（默认 true）
  enable-dynamic-weighting: true
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `require-approval` | boolean | `false` | 是否需要审核。`false`=直接生效，`true`=需要审核 |
| `auto-apply` | boolean | `true` | 是否自动应用反馈到权重 |
| `like-weight-increment` | double | `0.1` | 每次点赞增加的权重值 |
| `dislike-weight-decrement` | double | `-0.15` | 每次踩减少的权重值 |
| `min-weight` | double | `0.1` | 文档权重下限 |
| `max-weight` | double | `2.0` | 文档权重上限 |
| `enable-dynamic-weighting` | boolean | `true` | 是否启用动态权重 |

---

## 🔧 两种工作模式

### 模式1：直接生效（默认，推荐）

**配置**:
```yaml
feedback:
  require-approval: false
  auto-apply: true
```

**流程**:
```
用户提交反馈（👍/👎）
    ↓
立即应用到文档权重
    ↓
下次检索时生效
```

**优点**:
- ✅ 实时响应用户反馈
- ✅ 系统持续自动优化
- ✅ 无需人工干预
- ✅ 适合大多数场景

### 模式2：审核后生效

**配置**:
```yaml
feedback:
  require-approval: true
  auto-apply: true
```

**流程**:
```
用户提交反馈（👍/👎）
    ↓
记录为 PENDING 状态
    ↓
管理员审核
    ↓
批准后应用到权重
```

**优点**:
- ✅ 人工把关，避免误导
- ✅ 适合对质量要求极高的场景
- ✅ 可以过滤恶意反馈

---

## 🌐 REST API 接口

### 1. 获取当前配置

```http
GET /api/feedback/config
```

**响应**:
```json
{
  "requireApproval": false,
  "autoApply": true,
  "likeWeightIncrement": 0.1,
  "dislikeWeightDecrement": -0.15,
  "minWeight": 0.1,
  "maxWeight": 2.0,
  "enableDynamicWeighting": true
}
```

### 2. 更新配置

```http
POST /api/feedback/config
Content-Type: application/json

{
  "requireApproval": true,
  "autoApply": true,
  "likeWeightIncrement": 0.15,
  "dislikeWeightDecrement": -0.2
}
```

**响应**:
```json
{
  "success": true,
  "message": "配置更新成功",
  "config": {
    "requireApproval": true,
    "autoApply": true,
    ...
  }
}
```

### 3. 切换审核模式（快捷接口）

```http
POST /api/feedback/config/toggle-approval
Content-Type: application/json

{
  "requireApproval": true
}
```

**响应**:
```json
{
  "success": true,
  "message": "审核模式已切换为: 需要审核",
  "requireApproval": true
}
```

### 4. 获取文档权重统计

```http
GET /api/feedback/config/weights/statistics
```

**响应**:
```json
{
  "totalDocuments": 150,
  "highWeightDocuments": 25,
  "lowWeightDocuments": 18,
  "averageWeight": "1.05"
}
```

### 5. 获取所有文档权重

```http
GET /api/feedback/config/weights
```

**响应**:
```json
{
  "倡导节约用水PPT作品下载——.pptx": {
    "documentName": "倡导节约用水PPT作品下载——.pptx",
    "weight": 1.3,
    "likeCount": 5,
    "dislikeCount": 1,
    "originalWeight": 1.0,
    "lastUpdated": 1701234567890
  },
  "l0803.xls": {
    "documentName": "l0803.xls",
    "weight": 0.7,
    "likeCount": 0,
    "dislikeCount": 2,
    "originalWeight": 1.0,
    "lastUpdated": 1701234567890
  }
}
```

### 6. 重置文档权重

```http
POST /api/feedback/config/weights/reset
Content-Type: application/json

{
  "documentName": "l0803.xls"
}
```

### 7. 清除所有权重

```http
POST /api/feedback/config/weights/clear
```

---

## 💻 使用示例

### JavaScript 前端调用

```javascript
// 获取当前配置
async function getConfig() {
    const response = await fetch('/api/feedback/config');
    const config = await response.json();
    console.log('当前配置:', config);
    return config;
}

// 切换审核模式
async function toggleApproval(requireApproval) {
    const response = await fetch('/api/feedback/config/toggle-approval', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requireApproval })
    });
    const result = await response.json();
    console.log(result.message);
}

// 获取权重统计
async function getWeightStats() {
    const response = await fetch('/api/feedback/config/weights/statistics');
    const stats = await response.json();
    console.log('权重统计:', stats);
    return stats;
}

// 使用示例
getConfig();
toggleApproval(false);  // 切换到直接生效模式
getWeightStats();
```

### Python 调用

```python
import requests

BASE_URL = 'http://localhost:8080'

# 获取配置
def get_config():
    response = requests.get(f'{BASE_URL}/api/feedback/config')
    return response.json()

# 切换审核模式
def toggle_approval(require_approval):
    response = requests.post(
        f'{BASE_URL}/api/feedback/config/toggle-approval',
        json={'requireApproval': require_approval}
    )
    return response.json()

# 使用
config = get_config()
print('当前配置:', config)

result = toggle_approval(False)
print('切换结果:', result['message'])
```

---

## 📊 权重计算逻辑

### 权重调整公式

```
新权重 = 当前权重 + 调整值
最终权重 = max(min_weight, min(max_weight, 新权重))
```

### 示例计算

**初始状态**:
- 文档: `节约用水.pptx`
- 权重: `1.0`（默认）

**场景1: 连续3次点赞**
```
第1次: 1.0 + 0.1 = 1.1
第2次: 1.1 + 0.1 = 1.2
第3次: 1.2 + 0.1 = 1.3
```

**场景2: 连续2次踩**
```
第1次: 1.0 + (-0.15) = 0.85
第2次: 0.85 + (-0.15) = 0.70
```

**场景3: 混合反馈**
```
初始: 1.0
点赞: 1.0 + 0.1 = 1.1
点赞: 1.1 + 0.1 = 1.2
踩:   1.2 + (-0.15) = 1.05
点赞: 1.05 + 0.1 = 1.15
```

### 边界保护

```yaml
min-weight: 0.1
max-weight: 2.0
```

**极端情况**:
- 被踩100次: `1.0 + (-0.15 * 100) = -14.0` → 保护为 `0.1`
- 被赞100次: `1.0 + (0.1 * 100) = 11.0` → 保护为 `2.0`

---

## 🎯 权重对检索的影响

### 检索评分公式

```
最终评分 = 相似度分数 × 文档权重
```

### 实际案例

**查询**: "为什么要节约用水"

| 文档 | 相似度 | 权重 | 最终评分 | 排序 |
|------|--------|------|----------|------|
| 节约用水PPT | 0.85 | 1.3 | 1.105 | 第1 ⬆️ |
| 海洋环保PPT | 0.80 | 1.0 | 0.800 | 第2 |
| l0803.xls | 0.78 | 0.7 | 0.546 | 第3 ⬇️ |

**说明**:
- ✅ `节约用水PPT` 被点赞多次，权重1.3，排名提升
- ⚠️ `l0803.xls` 被踩多次，权重0.7，排名下降
- ➡️ `海洋环保PPT` 无反馈，权重1.0，保持原位

---

## 🔄 工作流程

### 完整流程图

```
                    【用户提问】
                        ↓
                【系统检索文档】
                        ↓
            【应用文档权重计算最终评分】
                        ↓
                【返回排序后的结果】
                        ↓
                【用户查看答案】
                        ↓
            【用户提交反馈（👍/👎）】
                        ↓
        ┌───────────【检查配置】────────────┐
        ↓                                    ↓
【requireApproval=false】        【requireApproval=true】
        ↓                                    ↓
【立即应用到权重】                    【设置为PENDING】
        ↓                                    ↓
【下次检索生效】                      【等待管理员审核】
                                             ↓
                                        【审核通过】
                                             ↓
                                      【应用到权重】
                                             ↓
                                      【下次检索生效】
```

---

## 📁 数据存储

### 权重数据文件

**位置**: `data/document-weights.json`

**格式**:
```json
{
  "倡导节约用水PPT作品下载——.pptx": {
    "documentName": "倡导节约用水PPT作品下载——.pptx",
    "weight": 1.3,
    "likeCount": 5,
    "dislikeCount": 1,
    "originalWeight": 1.0,
    "lastUpdated": 1701234567890
  },
  "l0803.xls": {
    "documentName": "l0803.xls",
    "weight": 0.7,
    "likeCount": 0,
    "dislikeCount": 2,
    "originalWeight": 1.0,
    "lastUpdated": 1701234567890
  }
}
```

### 反馈记录文件

**位置**: `data/qa-records/YYYYMMDD/HHmmss_recordId.json`

**字段变化**:
```json
{
  "id": "a1b2c3d4",
  "appliedToOptimization": true,  // 新增：是否已应用
  "reviewStatus": "APPROVED",      // PENDING/APPROVED/REJECTED
  ...
}
```

---

## 🎛️ 管理员操作

### 1. 查看待审核反馈

```bash
curl http://localhost:8080/api/feedback/pending
```

### 2. 批准反馈（手动应用权重）

```python
# 获取待审核记录
pending = get_pending_records()

for record in pending:
    # 审核通过
    if should_approve(record):
        # 应用文档反馈
        for feedback in record['documentFeedbacks']:
            apply_weight(
                feedback['documentName'],
                feedback['feedbackType']
            )
        
        # 更新记录状态
        update_record_status(record['id'], 'APPROVED')
```

### 3. 监控权重变化

```bash
# 查看权重统计
curl http://localhost:8080/api/feedback/config/weights/statistics

# 查看所有权重
curl http://localhost:8080/api/feedback/config/weights
```

---

## 🧪 测试场景

### 测试1: 直接生效模式

```yaml
feedback:
  require-approval: false
  auto-apply: true
```

**步骤**:
1. 提问: "为什么要节约用水"
2. 点赞文档: "节约用水PPT"
3. 立即查看权重: 应该从 1.0 → 1.1
4. 再次提问: "节约用水PPT" 排名应该上升

### 测试2: 审核模式

```yaml
feedback:
  require-approval: true
  auto-apply: true
```

**步骤**:
1. 提问: "为什么要节约用水"
2. 点赞文档: "节约用水PPT"
3. 立即查看权重: 应该仍然是 1.0
4. 查看待审核记录: 应该能看到这条反馈
5. 批准反馈
6. 再次查看权重: 应该变为 1.1

### 测试3: 动态切换模式

```bash
# 初始为直接生效
curl -X POST http://localhost:8080/api/feedback/config/toggle-approval \
  -H "Content-Type: application/json" \
  -d '{"requireApproval": false}'

# 切换到审核模式
curl -X POST http://localhost:8080/api/feedback/config/toggle-approval \
  -H "Content-Type: application/json" \
  -d '{"requireApproval": true}'
```

---

## 📈 预期效果

### 1周后
- 收集 100+ 条反馈
- 识别 10+ 个高质量文档（权重 > 1.2）
- 识别 5+ 个低相关文档（权重 < 0.8）

### 1个月后
- 收集 1000+ 条反馈
- 检索准确率提升 15-20%
- 用户满意度提升 25-30%
- 误召回率降低 30-40%

---

## ✅ 总结

### 核心特性

1. **灵活配置** - YML配置 + API动态修改
2. **两种模式** - 直接生效（默认）/ 审核后生效
3. **自动优化** - 用户反馈直接改善检索质量
4. **权重保护** - 最小/最大限制防止极端情况
5. **实时生效** - 反馈立即影响下次检索

### 推荐配置

**生产环境**（推荐）:
```yaml
feedback:
  require-approval: false      # 直接生效，快速优化
  auto-apply: true
  like-weight-increment: 0.1
  dislike-weight-decrement: -0.15
  min-weight: 0.1
  max-weight: 2.0
```

**严格质量控制**:
```yaml
feedback:
  require-approval: true       # 需要审核
  auto-apply: true
  like-weight-increment: 0.15  # 更大的调整幅度
  dislike-weight-decrement: -0.2
```

---

**创建日期**: 2025-11-28  
**版本**: 1.0.0  
**作者**: AI Reviewer Team  
**状态**: ✅ 已完成并测试

