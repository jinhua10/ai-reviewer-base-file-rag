# ✅ Maven 打包部署配置完成

## 🎯 解决方案总结

通过配置 Maven Assembly 插件的 `<attach>false</attach>`，实现了以下效果：

### 1️⃣ `mvn clean package` - 打包两个 JAR

**执行结果**：
```
target/
├── ai-reviewer-base-file-rag-1.0.jar                      ← thin JAR (约几百KB)
└── ai-reviewer-base-file-rag-1.0-jar-with-dependencies.jar ← FAT JAR (约200-300MB)
```

- **thin JAR**：只包含项目代码，会被 Maven attach 到项目
- **FAT JAR**：包含所有依赖，可直接运行，但不会被 attach

---

### 2️⃣ `mvn clean install` - 安装到本地仓库

**执行结果**：
```
~/.m2/repository/top/yumbo/ai/ai-reviewer-base-file-rag/1.0/
├── ai-reviewer-base-file-rag-1.0.jar        ← thin JAR (已安装)
├── ai-reviewer-base-file-rag-1.0.pom        ← POM 文件
├── ai-reviewer-base-file-rag-1.0-sources.jar
└── ai-reviewer-base-file-rag-1.0-javadoc.jar
```

✅ **只安装 thin JAR**，FAT JAR 不会被安装

---

### 3️⃣ `mvn clean deploy` - 部署到 Maven Central

**执行结果**：
```
部署到 Maven Central 的文件：
├── ai-reviewer-base-file-rag-1.0.jar         ← thin JAR (约几百KB)
├── ai-reviewer-base-file-rag-1.0.pom
├── ai-reviewer-base-file-rag-1.0-sources.jar
├── ai-reviewer-base-file-rag-1.0-javadoc.jar
├── ai-reviewer-base-file-rag-1.0.jar.asc     ← GPG 签名
├── ai-reviewer-base-file-rag-1.0.pom.asc
├── ai-reviewer-base-file-rag-1.0-sources.jar.asc
└── ai-reviewer-base-file-rag-1.0-javadoc.jar.asc
```

✅ **只部署 thin JAR**，FAT JAR 不会被部署  
✅ **上传速度快**（几百KB vs 几百MB）

---

## 🔑 核心配置

### Assembly 插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.7.1</version>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>top.yumbo.ai.rag.Application</mainClass>
                    </manifest>
                </archive>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <!-- 关键配置：不将 FAT JAR attach 到项目 -->
                <attach>false</attach>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 关键参数说明

**`<attach>false</attach>`**：
- **作用**：告诉 Maven 不要将 assembly 生成的 JAR attach 到项目
- **效果**：
  - `mvn install`：只安装 thin JAR 到本地仓库
  - `mvn deploy`：只部署 thin JAR 到远程仓库
  - FAT JAR 仍然会生成在 `target/` 目录，可以本地运行

---

## 📦 依赖传递机制

### Q: 第三方引入我的 JAR 后，能获取所有依赖吗？

**A: 是的！Maven 会自动处理传递依赖。**

#### 用户侧使用

```xml
<dependency>
    <groupId>top.yumbo.ai</groupId>
    <artifactId>ai-reviewer-base-file-rag</artifactId>
    <version>1.0</version>
</dependency>
```

#### Maven 自动下载

1. 下载 `ai-reviewer-base-file-rag-1.0.jar` (thin JAR)
2. 解析 JAR 内嵌的 `pom.xml`
3. 自动下载所有传递依赖：
   - ✅ `org.apache.lucene:lucene-core:9.9.1`
   - ✅ `org.apache.tika:tika-core:2.9.1`
   - ✅ `org.springframework.boot:spring-boot-starter:2.7.18`
   - ✅ ... 所有其他依赖

#### 依赖树示例

```
ai-reviewer-base-file-rag:1.0
├── lucene-core:9.9.1
├── lucene-queryparser:9.9.1
│   └── lucene-queries:9.9.1
├── tika-core:2.9.1
├── spring-boot-starter:2.7.18
│   ├── spring-boot:2.7.18
│   ├── spring-context:5.3.31
│   └── logback-classic:1.2.12
└── ... (所有依赖自动解析)
```

---

## 🚀 使用方式

### 方式 1：命令行

```bash
# 打包（生成 thin JAR 和 FAT JAR）
mvn clean package

# 本地运行 FAT JAR
java -jar target/ai-reviewer-base-file-rag-1.0-jar-with-dependencies.jar

# 安装到本地仓库（只安装 thin JAR）
mvn clean install

# 部署到 Maven Central（只部署 thin JAR）
mvn clean deploy
```

### 方式 2：IntelliJ IDEA

#### 使用 Maven 工具窗口

1. 打开右侧 `Maven` 工具窗口
2. 展开 `Lifecycle`
3. 双击相应命令：
   - **package**：打包
   - **install**：安装到本地
   - **deploy**：部署到远程

#### 创建 Run Configuration

**Package 配置**：
- Working directory: `$ProjectFileDir$`
- Command line: `clean package`

**Deploy 配置**：
- Working directory: `$ProjectFileDir$`
- Command line: `clean deploy`

---

## 📊 对比总结

| 特性 | thin JAR | FAT JAR |
|------|----------|---------|
| 大小 | ~几百KB | ~200-300MB |
| 内容 | 只包含项目代码 | 包含所有依赖 |
| 可直接运行 | ❌ 需要 classpath | ✅ 可直接运行 |
| 安装到本地 | ✅ | ❌ (attach=false) |
| 部署到远程 | ✅ | ❌ (attach=false) |
| 上传速度 | 快 ⚡ | 慢 |
| 依赖传递 | ✅ 自动 | N/A |
| 使用场景 | Maven 依赖 | 本地运行/分发 |

---

## ✨ 优势

### 1. 开发友好
- ✅ `package` 时同时生成两种 JAR
- ✅ FAT JAR 可直接运行测试
- ✅ 不需要额外的命令参数

### 2. 部署高效
- ✅ `deploy` 时自动只部署 thin JAR
- ✅ 上传速度快（几百KB vs 几百MB）
- ✅ 符合 Maven 最佳实践

### 3. 使用简单
- ✅ 第三方依赖时自动获取传递依赖
- ✅ 不需要手动管理依赖列表
- ✅ Maven 自动解决版本冲突

### 4. 配置清晰
- ✅ 只需一个配置：`<attach>false</attach>`
- ✅ 不需要额外的 profile
- ✅ 不需要命令行参数
- ✅ 不需要批处理脚本

---

## 🎓 原理说明

### Maven Attach 机制

1. **默认行为**（attach=true）：
   ```
   package → 生成 JAR → attach 到项目 → install/deploy 时处理
   ```

2. **配置 attach=false**：
   ```
   package → 生成 JAR → 不 attach → install/deploy 时忽略
   ```

3. **结果**：
   - thin JAR（主 artifact）：始终会被 install/deploy
   - FAT JAR（assembly artifact）：不被 attach，install/deploy 时忽略

### Maven 生命周期

```
compile → test → package → verify → install → deploy
                    ↓
                生成 JAR
                    ↓
        ┌───────────┴───────────┐
        ↓                       ↓
   thin JAR                FAT JAR
  (attach=true)         (attach=false)
        ↓                       ↓
   install/deploy            仅保留在 target/
```

---

## 📝 发布流程

### 步骤 1：配置 GPG 和 Maven settings.xml

确保 `~/.m2/settings.xml` 包含：

```xml
<servers>
    <server>
        <id>central</id>
        <username>YOUR_TOKEN</username>
        <password>YOUR_PASSWORD</password>
    </server>
</servers>
```

### 步骤 2：执行部署

```bash
mvn clean deploy
```

### 步骤 3：在 Central Portal 确认

1. 登录 https://central.sonatype.com
2. 进入 `Deployments` 页面
3. 查看上传的文件：
   - ✅ ai-reviewer-base-file-rag-1.0.jar (thin JAR)
   - ✅ ai-reviewer-base-file-rag-1.0-sources.jar
   - ✅ ai-reviewer-base-file-rag-1.0-javadoc.jar
   - ✅ 所有 .asc 签名文件
4. 点击 `Publish` 发布

### 步骤 4：等待同步

- 发布后 15-30 分钟同步到 Maven Central
- 搜索可用：https://search.maven.org

---

## 🎉 总结

### ✅ 配置完成

通过 `<attach>false</attach>` 配置，成功实现：

1. **mvn package**：生成 thin JAR + FAT JAR
2. **mvn install**：只安装 thin JAR
3. **mvn deploy**：只部署 thin JAR
4. **第三方使用**：自动获取传递依赖

### ✅ 无需额外操作

- ❌ 不需要命令行参数（如 `-DskipFatJar=true`）
- ❌ 不需要 profile 激活（如 `-P release`）
- ❌ 不需要批处理脚本
- ✅ 只需标准的 Maven 命令

### ✅ 符合最佳实践

- ✅ Maven 标准的 artifact attach 机制
- ✅ 符合 Maven Central 发布规范
- ✅ 依赖传递自动处理
- ✅ 配置简洁清晰

---

**🚀 现在可以直接使用 `mvn clean deploy` 发布项目了！**

