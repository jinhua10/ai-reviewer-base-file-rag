# Refactor 目录说明
# Refactor Directory Documentation

> **目录用途**: 存放系统重构和新功能开发的所有重要文档  
> **文档规范**: 使用时间戳前缀，便于追溯和恢复

---

## 📂 目录结构

```
refactor/
├── README.md                                    (本文档)
├── SESSION_RECOVERY_GUIDE.md                    (会话恢复指南)
├── 20251209-22-29-00-IMPLEMENTATION_PLAN.md    (主实施计划)
│
├── phase-1/                                     (Phase 1 相关文档)
│   ├── 20251210-xx-xx-Phase1-Role-Model.md
│   └── 20251210-xx-xx-Phase1-Complete.md
│
├── phase-2/                                     (Phase 2 相关文档)
│   ├── 20251211-xx-xx-Phase2-Vector-Index.md
│   └── 20251211-xx-xx-Phase2-Complete.md
│
├── design/                                      (设计文档)
│   ├── HIERARCHICAL_SEMANTIC_RAG.md
│   └── ROLE_BASED_KNOWLEDGE_ARCHITECTURE.md
│
├── reports/                                     (进度报告)
│   ├── 20251209-Weekly-Report.md
│   └── 20251216-Weekly-Report.md
│
└── issues/                                      (问题追踪)
    ├── 20251210-Bug-Vector-Index.md
    └── 20251211-Performance-Issue.md
```

---

## 📝 文档命名规范

### 格式
```
yyyyMMdd-HH-mm-ss-{类型}-{描述}.md
```

### 示例
```
20251209-22-29-00-IMPLEMENTATION_PLAN.md
20251210-14-30-00-Phase1-Complete-Report.md
20251211-09-15-00-Bug-Fix-Vector-Index.md
20251212-16-45-00-Design-Role-Detector.md
```

### 类型分类
```yaml
IMPLEMENTATION: 实施计划
Phase{X}: Phase X 相关
Complete: 完成报告
Bug: Bug 修复
Design: 设计文档
Report: 进度报告
Issue: 问题追踪
Refactor: 重构文档
```

---

## 🔄 文档生命周期

### 1. 创建阶段
```
- 开始新任务时创建文档
- 使用时间戳前缀
- 标注"Status: Draft"
```

### 2. 进行中
```
- 持续更新进度
- 记录遇到的问题
- 更新状态和完成度
```

### 3. 完成阶段
```
- 标注"Status: Completed"
- 生成完成报告
- 归档到对应的子目录
```

### 4. 归档
```
- 过期文档移到 archive/ 目录
- 保留时间戳便于追溯
- 更新索引文档
```

---

## 🎯 核心文档

### 必读文档（新手入门）
1. **SESSION_RECOVERY_GUIDE.md** - 如何在新 Session 中恢复工作
2. **20251209-22-29-00-IMPLEMENTATION_PLAN.md** - 主实施计划
3. **HIERARCHICAL_SEMANTIC_RAG.md** - 系统设计理念

### 进行中的文档
- 查看实施计划中的"当前进度"
- 查看最新的 Phase 文档

### 历史文档
- phase-*/xx-Complete.md - 已完成的 Phase 报告
- reports/xx-Weekly-Report.md - 周报

---

## 🚀 快速开始

### 新 Session 如何开始？

**步骤 1**: 告诉 Copilot
```
"查看 refactor 目录中的实施计划，继续未完成的任务"
```

**步骤 2**: Copilot 会自动
- 读取最新的实施计划
- 查看当前进度
- 加载相关文档
- 开始执行下一个任务

**步骤 3**: 完成任务后
```
"更新实施计划的进度"
```

---

## 📊 进度追踪

### 主实施计划
- 文件: `20251209-22-29-00-IMPLEMENTATION_PLAN.md`
- 包含: 6 个 Phase 的详细任务清单
- 状态: 实时更新

### 查看进度
```
"查看当前开发进度"
"显示 Phase X 的完成情况"
"列出未完成的任务"
```

---

## 🔍 文档索引

### 按时间顺序
```
2025-12-09 22:29:00 - IMPLEMENTATION_PLAN (创建主实施计划)
2025-12-09 22:35:00 - SESSION_RECOVERY_GUIDE (创建恢复指南)
2025-12-09 22:40:00 - README (创建本文档)
...待补充...
```

### 按类型分类
```yaml
实施计划:
  - 20251209-22-29-00-IMPLEMENTATION_PLAN.md

指南:
  - SESSION_RECOVERY_GUIDE.md
  - README.md

设计文档:
  - (待迁移) HIERARCHICAL_SEMANTIC_RAG.md
  - (待迁移) ROLE_BASED_KNOWLEDGE_ARCHITECTURE.md

Phase 文档:
  - (待创建) Phase 1 系列
  - (待创建) Phase 2 系列
  ...
```

---

## 🛠️ 维护规范

### 每次完成任务后
1. ✅ 更新主实施计划的进度
2. ✅ 更新"进度更新日志"
3. ✅ 如果完成了 Phase，生成完成报告
4. ✅ 提交代码到 git

### 每周
1. ✅ 生成周报
2. ✅ 归档已完成的文档
3. ✅ 更新文档索引

### 每月
1. ✅ 整理和清理过期文档
2. ✅ 更新 README
3. ✅ 备份重要文档

---

## 📚 相关资源

### 设计文档
- `../HIERARCHICAL_SEMANTIC_RAG.md` - 知识演化系统
- `../ROLE_BASED_KNOWLEDGE_ARCHITECTURE.md` - 分角色知识库

### 历史报告
- `../PHASE_MINUS_1_FINAL_REPORT.md` - Phase -1 报告
- `../P2_TASKS_COMPLETION_REPORT.md` - P2 任务报告

### 代码仓库
- `src/main/java/top/yumbo/ai/rag/` - 核心代码

---

## 💡 最佳实践

### ✅ DO（推荐做法）
- ✅ 使用时间戳前缀
- ✅ 及时更新进度
- ✅ 详细记录问题
- ✅ 生成完成报告
- ✅ 定期归档文档

### ❌ DON'T（避免做法）
- ❌ 不要手动修改时间戳
- ❌ 不要删除历史文档
- ❌ 不要跳过进度更新
- ❌ 不要忘记提交代码
- ❌ 不要混乱的命名

---

## 🎯 目标

**让每一次会话恢复都像从未中断过一样流畅！**

通过良好的文档组织和命名规范：
- ✅ 任何人都能快速了解项目状态
- ✅ 任何时候都能从中断处继续
- ✅ 所有决策和变更都有记录
- ✅ 问题和解决方案都有追溯

---

**文档版本**: v1.0  
**创建日期**: 2025-12-09  
**维护者**: AI Reviewer Team  
**更新频率**: 随项目进展持续更新

