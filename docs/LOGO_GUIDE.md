# 🎨 项目 Logo 使用指南

## 📁 Logo 文件

本项目提供了 3 种 Logo 格式，满足不同使用场景：

| 文件 | 尺寸 | 用途 |
|------|------|------|
| `logo.svg` | 200x200 | 主 Logo，用于文档、社交媒体 |
| `logo-banner.svg` | 400x100 | 横幅 Logo，用于 README 顶部 |
| `favicon.svg` | 64x64 | 网站图标，用于浏览器标签页 |

---

## 🎨 Logo 设计说明

### 设计元素

1. **文档图标** (蓝色 #2D5BFF)
   - 代表文档检索和知识库
   - 简洁的矩形设计，带有文本线条

2. **搜索放大镜** (橙色 #FF6B35)
   - 代表检索和查询功能
   - 位于文档图标上，象征对文档的搜索

3. **AI 脑图标** (绿色 #10B981)
   - 代表人工智能和智能问答
   - 神经网络连接，象征 RAG 技术

4. **闪电图标** (黄色 #FFC107)
   - 代表高性能和快速响应
   - 强调项目的速度优势

### 颜色方案

```
主色调 (Primary): #2D5BFF - 科技蓝
辅色1 (Secondary): #FF6B35 - 活力橙
辅色2 (Accent): #10B981 - 智能绿
辅色3 (Highlight): #FFC107 - 能量黄
```

---

## 📖 使用方法

### 在 README.md 中使用

#### 方式 1: 顶部横幅 Logo

```markdown
<div align="center">
  <img src="docs/images/logo-banner.svg" alt="AI Reviewer Base File RAG" width="400"/>
</div>
```

效果：
<div align="center">
  <img src="images/logo-banner.svg" alt="AI Reviewer Base File RAG" width="400"/>
</div>

#### 方式 2: 圆形主 Logo

```markdown
<div align="center">
  <img src="docs/images/logo.svg" alt="AI Reviewer Base File RAG" width="150"/>
  
  <h1>AI Reviewer Base File RAG</h1>
</div>
```

效果：
<div align="center">
  <img src="images/logo.svg" alt="AI Reviewer Base File RAG" width="150"/>
  
  <h1>AI Reviewer Base File RAG</h1>
</div>

### 在网站中使用

#### HTML 中引用 Favicon

```html
<link rel="icon" type="image/svg+xml" href="/docs/images/favicon.svg">
```

#### 作为页面图标

```html
<div class="logo">
  <img src="/docs/images/logo.svg" alt="Logo" width="100">
</div>
```

---

## 🎯 使用场景

### 1. GitHub 仓库

- **README 顶部**: 使用 `logo-banner.svg`
- **头像**: 使用 `logo.svg` (GitHub 会自动裁剪为圆形)
- **Social Preview**: 建议尺寸 1280x640，可基于 `logo-banner.svg` 扩展

### 2. 文档网站

- **导航栏**: 使用 `favicon.svg` 或 `logo.svg` (调整尺寸)
- **浏览器标签页**: 使用 `favicon.svg`
- **页脚**: 使用 `logo.svg` 或 `logo-banner.svg`

### 3. 社交媒体

- **Twitter/X**: 使用 `logo.svg` (400x400)
- **LinkedIn**: 使用 `logo-banner.svg` (1200x627)
- **知乎/掘金**: 使用 `logo.svg` (建议正方形)

### 4. 宣传材料

- **PPT/PDF**: 使用 `logo-banner.svg` (矢量图，可无损缩放)
- **海报**: 使用 `logo.svg` (大尺寸打印)
- **名片**: 使用 `favicon.svg` (简化版本)

---

## 🔧 自定义 Logo

### 修改颜色

编辑 SVG 文件中的颜色值：

```xml
<!-- 修改主色调 -->
<circle fill="#2D5BFF" />  <!-- 改为你想要的颜色 -->

<!-- 修改辅色 -->
<line stroke="#FF6B35" />  <!-- 改为你想要的颜色 -->
```

### 导出为 PNG

使用以下工具将 SVG 转换为 PNG：

1. **在线工具**
   - https://svgtopng.com/
   - https://cloudconvert.com/svg-to-png

2. **命令行工具**
   ```bash
   # 使用 Inkscape
   inkscape logo.svg -w 512 -h 512 -o logo.png
   
   # 使用 ImageMagick
   convert -background none logo.svg -resize 512x512 logo.png
   ```

3. **浏览器**
   - 在浏览器中打开 SVG
   - 右键 → 另存为图片

### 生成不同尺寸

常用尺寸：
- **16x16**: 浏览器标签页
- **32x32**: 网站图标
- **64x64**: App 图标
- **128x128**: 中等图标
- **256x256**: 大图标
- **512x512**: 高清图标
- **1024x1024**: 超高清图标

---

## 📋 品牌规范

### ✅ 可以做的

- ✅ 调整 Logo 大小
- ✅ 在白色或浅色背景上使用
- ✅ 在深色背景上使用（建议使用白色描边版本）
- ✅ 与项目名称一起使用
- ✅ 用于非商业目的

### ❌ 不应该做的

- ❌ 改变 Logo 的比例（拉伸或压缩）
- ❌ 改变 Logo 的颜色方案（除非有正当理由）
- ❌ 在 Logo 上添加额外元素
- ❌ 旋转或倾斜 Logo
- ❌ 用于误导性宣传
- ❌ 用于商业目的而不注明来源

---

## 📦 文件格式说明

### SVG (推荐)

**优点**:
- ✅ 矢量图，无损缩放
- ✅ 文件小，加载快
- ✅ 可直接在浏览器中显示
- ✅ 易于编辑和修改

**用途**:
- 网页显示
- 文档嵌入
- 打印材料

### PNG (备选)

**优点**:
- ✅ 通用性强
- ✅ 支持透明背景
- ✅ 兼容性好

**缺点**:
- ❌ 放大会失真
- ❌ 文件较大

**用途**:
- 社交媒体头像
- Email 签名
- 不支持 SVG 的平台

---

## 🎨 配色方案详情

### 主色调

```
科技蓝 (Primary Blue)
- HEX: #2D5BFF
- RGB: rgb(45, 91, 255)
- 用途: 主要元素、文档图标
```

### 辅色

```
活力橙 (Vibrant Orange)
- HEX: #FF6B35
- RGB: rgb(255, 107, 53)
- 用途: 强调元素、搜索图标

智能绿 (AI Green)
- HEX: #10B981
- RGB: rgb(16, 185, 129)
- 用途: AI 元素、成功状态

能量黄 (Energy Yellow)
- HEX: #FFC107
- RGB: rgb(255, 193, 7)
- 用途: 高亮元素、性能标识
```

---

## 📞 联系我们

如有 Logo 使用问题或需要定制，请联系：

- **Email**: 1015770492@qq.com
- **GitHub Issues**: https://github.com/jinhua10/ai-reviewer-base-file-rag/issues

---

## 📄 许可证

Logo 设计遵循项目的 [Apache License 2.0](../LICENSE.txt) 许可证。

---

<div align="center">

**感谢使用 AI Reviewer Base File RAG！**

Made with ❤️ by the AI Reviewer Team

</div>

