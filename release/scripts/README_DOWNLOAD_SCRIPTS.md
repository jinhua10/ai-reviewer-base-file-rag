# 🚀 一键下载脚本使用指南

## 📅 最后更新
2025-12-05 01:30:00

## ✨ 特性

两个脚本都支持：
- ✅ **零依赖运行** - 全新 Python 环境也能用
- ✅ **自动安装依赖** - 缺失的包自动安装
- ✅ **自动 ONNX 转换** - 下载后立即可用
- ✅ **智能错误处理** - 清晰的提示和建议

---

## 📦 脚本 1: download_qwen_onnx.py

### 用途
下载 Qwen PPL 模型并转换为 ONNX 格式（用于 PPL 服务）

### 使用方法

```bash
# 下载 Qwen2.5-0.5B（轻量级，推荐）
python scripts/download_qwen_onnx.py --model 0.5b

# 下载 Qwen2.5-1.5B（平衡）
python scripts/download_qwen_onnx.py --model 1.5b

# 下载 Qwen2-7B（高质量）
python scripts/download_qwen_onnx.py --model 7b

# 使用国内镜像（更快）
python scripts/download_qwen_onnx.py --model 1.5b --mirror

# 自定义输出目录
python scripts/download_qwen_onnx.py --model 1.5b --output ./my_models
```

### 自动安装的依赖

- `transformers>=4.30.0`
- `optimum[onnxruntime]>=1.14.0`
- `onnxruntime>=1.15.0`
- `torch>=2.0.0`
- `onnxscript>=0.1.0`

### 输出

```
models/
└── qwen2.5-1.5b-instruct/
    ├── model.onnx          (1.3 MB)   ✅
    ├── model.onnx_data     (7.1 GB)   ✅
    ├── tokenizer.json
    └── config.json
```

---

## 📦 脚本 2: download_embedding_model.py

### 用途
下载国产向量嵌入模型并转换为 ONNX 格式（用于向量检索）

### 使用方法

```bash
# 下载 BGE-M3（最推荐，智源研究院）
python scripts/download_embedding_model.py --model bge-m3

# 下载 BGE-Base-ZH（轻量级）
python scripts/download_embedding_model.py --model bge-base-zh

# 下载 BGE-Large-ZH（高质量）
python scripts/download_embedding_model.py --model bge-large-zh

# 使用魔搭社区镜像（国内快）
python scripts/download_embedding_model.py --model bge-m3 --mirror

# 只下载不转换 ONNX
python scripts/download_embedding_model.py --model bge-m3 --no-convert-onnx

# 自定义输出目录
python scripts/download_embedding_model.py --model bge-m3 --output ./my_models
```

### 自动安装的依赖

**基础依赖**：
- `sentence-transformers>=2.0.0`
- `torch>=2.0.0`
- `transformers>=4.30.0`
- `optimum[onnxruntime]>=1.14.0`
- `onnxruntime>=1.15.0`
- `onnxscript>=0.1.0`

**使用 --mirror 时额外安装**：
- `modelscope>=1.0.0`

### 输出

```
models/
└── bge-m3/
    ├── model.onnx          (0.5 MB)   ✅
    ├── model.onnx_data     (2.16 GB)  ✅
    ├── tokenizer.json
    └── config.json
```

**注意**：
- ✅ 转换过程中会创建临时目录（如 `bge-m3-onnx`）
- ✅ 临时目录在转换完成后会**自动清理**，无需手动删除
- ✅ 最终只保留原始模型目录及其中的 ONNX 文件

---

## 🎯 完整使用流程

### 步骤 1：下载模型

```bash
# 1. 下载 PPL 模型
python scripts/download_qwen_onnx.py --model 1.5b --mirror

# 2. 下载向量检索模型
python scripts/download_embedding_model.py --model bge-m3 --mirror
```

### 步骤 2：配置应用

模型会自动保存到 `./models/` 目录，配置已经预设好：

```yaml
# application.yml（无需修改）
knowledge:
  qa:
    ppl:
      ollama:
        model: qwen2.5:1.5b
      onnx:
        model-path: ./models/qwen2.5-1.5b-instruct/model.onnx
    
    vector-search:
      model:
        name: bge-m3
        path: ./models/bge-m3/model.onnx
```

### 步骤 3：启动应用

```bash
./mvnw spring-boot:run
```

### 步骤 4：重建索引

```bash
# 访问 http://localhost:8080
# 点击 "文档管理" → "重建索引"
```

---

## 💡 常见场景

### 场景 1：首次安装

```bash
# 全新 Python 环境，什么都没装
python scripts/download_qwen_onnx.py --model 1.5b

# ✅ 脚本会自动安装所有依赖
# ✅ 下载并转换模型
# ✅ 验证模型可用
```

### 场景 2：网络慢

```bash
# 使用国内镜像加速
python scripts/download_qwen_onnx.py --model 1.5b --mirror
python scripts/download_embedding_model.py --model bge-m3 --mirror
```

### 场景 3：磁盘空间有限

```bash
# 使用小模型
python scripts/download_qwen_onnx.py --model 0.5b
python scripts/download_embedding_model.py --model bge-base-zh
```

### 场景 4：只需要 PyTorch 版本

```bash
# 不转换 ONNX
python scripts/download_embedding_model.py --model bge-m3 --no-convert-onnx
```

---

## 🔍 故障排查

### 问题 1：依赖安装失败

**现象**：
```
❌ 以下依赖安装失败: torch
```

**解决**：
```bash
# 手动安装失败的依赖
pip install torch>=2.0.0

# 然后重新运行脚本
python scripts/download_qwen_onnx.py --model 1.5b
```

### 问题 2：网络超时

**解决**：
```bash
# 1. 使用镜像
python scripts/download_qwen_onnx.py --model 1.5b --mirror

# 2. 或配置 pip 镜像
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### 问题 3：磁盘空间不足

**检查**：
```bash
# 查看可用空间
df -h  # Linux/Mac
wmic logicaldisk get size,freespace,caption  # Windows
```

**解决**：
- Qwen2.5-0.5B: 需要 ~1 GB
- Qwen2.5-1.5B: 需要 ~7 GB
- Qwen2-7B: 需要 ~15 GB
- BGE-M3: 需要 ~2.5 GB
- BGE-Base-ZH: 需要 ~500 MB

---

## ✅ 验证安装

### 验证脚本可用

```bash
# 查看帮助
python scripts/download_qwen_onnx.py --help
python scripts/download_embedding_model.py --help
```

### 验证模型已下载

```bash
# 检查 Qwen 模型
ls models/qwen2.5-1.5b-instruct/

# 检查 BGE 模型
ls models/bge-m3/

# 验证 ONNX 文件
ls models/*/model.onnx
ls models/*/model.onnx_data
```

---

## 📚 推荐组合

### 开发环境（轻量级）

```bash
python scripts/download_qwen_onnx.py --model 0.5b
python scripts/download_embedding_model.py --model bge-base-zh
# 总大小: ~1.5 GB
```

### 生产环境（推荐）

```bash
python scripts/download_qwen_onnx.py --model 1.5b --mirror
python scripts/download_embedding_model.py --model bge-m3 --mirror
# 总大小: ~9.5 GB
```

### 高性能环境

```bash
python scripts/download_qwen_onnx.py --model 7b
python scripts/download_embedding_model.py --model bge-large-zh
# 总大小: ~16 GB
```

---

## 🎉 总结

### 改进成果

1. ✅ **完全自动化** - 一条命令搞定
2. ✅ **零配置** - 无需手动安装依赖
3. ✅ **全新环境支持** - Python 刚装也能用
4. ✅ **智能错误处理** - 清晰的提示
5. ✅ **自动 ONNX 转换** - 立即可用

### 使用体验

**之前**：
```bash
pip install transformers optimum onnxruntime torch onnxscript
python scripts/download_qwen_onnx.py --model 1.5b
# 可能还需要手动转换...
```

**现在**：
```bash
python scripts/download_qwen_onnx.py --model 1.5b
# 完成！✨
```

---

**文档版本**: v1.0  
**最后更新**: 2025-12-05 01:30:00  
**状态**: ✅ **完全自动化，开箱即用！**

