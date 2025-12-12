# 📚 索引性能优化和诊断指南
# Index Performance Optimization and Diagnosis Guide

> **创建时间**: 2025-12-13  
> **问题**: 每次重启索引都很慢  
> **状态**: ✅ 诊断和优化方案

---

## 🔍 当前配置检查

### 1. 增量索引已启用 ✅

**配置文件**: `application.yml:95`
```yaml
knowledge:
  qa:
    knowledge-base:
      # 启动时索引模式
      rebuild-on-startup: false  # ✅ 已设置为增量模式
```

**作用**:
- `false`: 增量索引（只索引新增和修改的文件）✅
- `true`: 完全重建（删除所有索引，重新索引） ❌

### 2. 向量索引持久化已配置 ✅

**配置文件**: `application.yml:606`
```yaml
vector-search:
  # 向量索引存储路径
  index-path: ./data/vector-index  # ✅ 向量会持久化
```

**存储内容**:
```
data/vector-index/
  ├── vectors.bin      # 向量数据
  └── metadata.json    # 元数据
```

---

## 🐛 可能的问题原因

### 问题 1: 文件追踪数据丢失

**现象**: 
- 每次重启都重新索引所有文档
- 即使文档没有变化

**原因**:
```
data/knowledge-base/
  └── file-tracking.json  ❌ 可能被删除或损坏
```

**解决方案**:
检查 `file-tracking.json` 是否存在：

```bash
# 检查文件是否存在
ls data/knowledge-base/file-tracking.json

# 查看内容
cat data/knowledge-base/file-tracking.json
```

**正常的内容**:
```json
{
  "files": {
    "文档1.pdf": {
      "lastModified": 1702456789000,
      "size": 102400,
      "hash": "abc123..."
    },
    "文档2.docx": {
      "lastModified": 1702456790000,
      "size": 204800,
      "hash": "def456..."
    }
  },
  "lastUpdate": 1702456791000
}
```

---

### 问题 2: 删除了 data/documents

**你的情况** ✅ 符合！

```
你说: "我手动删除了 data/documents 以为所有数据"
```

**影响**:
1. **文档文件被删除** → 源文件丢失
2. **但索引数据还在** → `data/knowledge-base/` 和 `data/vector-index/` 没删
3. **文件追踪记录还在** → 系统认为文档还存在
4. **重启后系统发现不一致** → 需要重新扫描和索引

**解决方案**:

```bash
# 方案 1: 清理所有索引数据（推荐）
rm -rf data/knowledge-base/
rm -rf data/vector-index/

# 方案 2: 只清理文件追踪
rm -f data/knowledge-base/file-tracking.json

# 然后重启应用，会重新建立索引（只需一次）
```

---

### 问题 3: 向量模型文件大导致启动慢

**现象**:
- 启动时加载向量模型需要时间
- 第一次向量化文档很慢

**检查模型大小**:
```bash
du -sh models/bge-base-zh/
# 预期: 400-500MB
```

**优化方案**:
1. 使用更小的模型（如 `bge-small-zh`）
2. 或者禁用向量检索（仅用关键词）

```yaml
vector-search:
  enabled: false  # 禁用向量检索，速度更快
```

---

## ⚡ 性能优化建议

### 1. 启用批处理（已默认启用）✅

**配置**: `application.yml`
```yaml
document:
  # 批处理大小（文档数）
  batch-size: 10  # ✅ 已配置
  
  # 是否启用并行处理
  parallel-processing: true  # ✅ 已启用
  
  # 并行线程数（0=自动）
  parallel-threads: 0  # ✅ 自动使用 CPU 核心数
```

### 2. 减少文档内容长度

**配置**: `application.yml`
```yaml
document:
  # 索引时单个文档最大内容长度（字符数）
  max-index-content-length: 50000  # 可以降低到 30000
  
  # 问答时文档切分最大内容长度
  max-chunk-content-length: 100000  # 可以降低到 50000
```

**效果**:
- 减少向量计算时间
- 减少内存占用
- 加快索引速度

### 3. 调整检索数量（只影响查询，不影响索引）

**配置**: `application.yml`
```yaml
vector-search:
  # 减少检索数量可以加快查询速度（不影响索引）
  lucene-top-k: 50      # 从 100 降到 50
  vector-top-k: 30      # 从 50 降到 30
  hybrid-top-k: 15      # 从 30 降到 15
```

---

## 🔧 诊断步骤

### 步骤 1: 检查索引数据

```bash
# 1. 检查文件追踪
ls -lh data/knowledge-base/file-tracking.json

# 2. 检查向量索引
ls -lh data/vector-index/

# 3. 检查文档元数据
ls -lh data/knowledge-base/*.db
```

### 步骤 2: 查看启动日志

**关键日志**:

```
✅ 正常增量索引:
[KnowledgeBaseService] INFO - 📁 扫描文档目录: ./data/documents
[KnowledgeBaseService] INFO - 📊 找到 50 个文件
[KnowledgeBaseService] INFO - 🔍 需要索引的文件: 0 个  ← 0 个表示都是最新的
[KnowledgeBaseService] INFO - ✅ 所有文件都是最新的，无需索引

❌ 异常重新索引:
[KnowledgeBaseService] INFO - 📁 扫描文档目录: ./data/documents
[KnowledgeBaseService] INFO - 📊 找到 50 个文件
[KnowledgeBaseService] INFO - 🔍 需要索引的文件: 50 个  ← 全部需要重新索引
[KnowledgeBaseService] INFO - ⏳ 开始处理文档...
```

### 步骤 3: 测试增量索引

1. **正常启动一次**（会建立索引）
2. **不修改任何文档**
3. **重启应用**
4. **查看日志** - 应该显示 "需要索引的文件: 0 个"

---

## 📊 性能对比

### 场景 1: 完全重建（rebuild-on-startup: true）

```
文档数量: 100 个
索引时间: 5-10 分钟
每次启动: 都要重新索引 ❌
```

### 场景 2: 增量索引（rebuild-on-startup: false）

```
文档数量: 100 个

第一次启动:
  - 索引时间: 5-10 分钟
  - 建立文件追踪

后续启动（文档无变化）:
  - 索引时间: 5-10 秒 ✅
  - 只加载已有索引

后续启动（新增 5 个文档）:
  - 索引时间: 30-60 秒 ✅
  - 只索引新文档
```

---

## 💡 推荐配置

### 开发环境（快速迭代）

```yaml
knowledge:
  qa:
    knowledge-base:
      rebuild-on-startup: false     # 增量索引
      enable-cache: true            # 启用缓存
    
    document:
      max-index-content-length: 30000   # 减小内容长度
      batch-size: 5                     # 小批次
      parallel-processing: true         # 并行处理
    
    vector-search:
      enabled: false                # 禁用向量（开发时）
      # 或者使用小模型
      # enabled: true
      # model.name: bge-small-zh
```

### 生产环境（性能优先）

```yaml
knowledge:
  qa:
    knowledge-base:
      rebuild-on-startup: false     # 增量索引
      enable-cache: true            # 启用缓存
    
    document:
      max-index-content-length: 50000   # 标准长度
      batch-size: 10                    # 标准批次
      parallel-processing: true          # 并行处理
      parallel-threads: 0                # 自动检测 CPU
    
    vector-search:
      enabled: true                 # 启用向量
      model.name: bge-base-zh       # 标准模型
```

---

## 🎯 快速解决方案

### 你的情况（删除了 data/documents）

```bash
# 1. 清理所有索引数据
rm -rf data/knowledge-base/
rm -rf data/vector-index/

# 2. 重新放入文档到 data/documents/
cp -r /path/to/your/documents/* data/documents/

# 3. 重启应用（第一次会建立索引，之后就快了）
mvn spring-boot:run

# 4. 之后每次重启（如果文档没变化）
# 启动时间: 10-30 秒（不会重新索引）✅
```

---

## 📈 索引时间估算

### 影响因素

1. **文档数量**: 100 个文档 ≈ 5-10 分钟
2. **文档大小**: 每个文档 1MB ≈ 0.5-1 秒
3. **是否有图片**: 有图片 ≈ 时间 +30%
4. **向量化**: 启用向量 ≈ 时间 +50%
5. **CPU 性能**: 影响并行处理效率

### 预期时间

```
10 个文档 (纯文本):
  - 完全索引: 30-60 秒
  - 增量索引: 5-10 秒

100 个文档 (含图片):
  - 完全索引: 5-10 分钟
  - 增量索引: 10-20 秒（无变化时）

1000 个文档:
  - 完全索引: 30-60 分钟
  - 增量索引: 15-30 秒（无变化时）
```

---

## ✅ 检查清单

### 优化前检查

- [ ] 确认 `rebuild-on-startup: false`
- [ ] 检查 `file-tracking.json` 是否存在
- [ ] 检查 `data/vector-index/` 是否有数据
- [ ] 查看启动日志中 "需要索引的文件" 数量

### 优化后验证

- [ ] 第一次启动完成索引
- [ ] 不修改文档
- [ ] 第二次启动应该很快（10-30秒）
- [ ] 日志显示 "需要索引的文件: 0 个"

---

## 🆘 仍然很慢？

### 可能的原因

1. **向量模型加载慢** → 使用更小的模型或禁用向量
2. **CPU 性能不足** → 减少 `parallel-threads`
3. **磁盘 I/O 慢** → 使用 SSD
4. **内存不足** → 减小 `max-index-content-length`
5. **文档格式复杂** → 简化文档内容

### 进一步优化

```yaml
# 极速模式（牺牲部分检索质量）
vector-search:
  enabled: false          # 禁用向量检索

document:
  max-index-content-length: 20000   # 减小到 20K
  parallel-threads: 2                # 限制线程数
```

---

**总结**: 
- ✅ 系统已支持增量索引
- ✅ 配置正确（`rebuild-on-startup: false`）
- ⚠️ 删除 `data/documents` 导致需要重新索引
- 💡 清理索引数据后重启一次即可恢复正常

**下次重启应该很快！** 🚀

