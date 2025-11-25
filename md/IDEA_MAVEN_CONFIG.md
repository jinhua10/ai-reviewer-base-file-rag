# IntelliJ IDEA Maven 配置说明

## 配置方式

### 方法 1: 在 IntelliJ IDEA 中配置 Maven Deploy

1. 打开 **Run** → **Edit Configurations...**
2. 点击 **+** → **Maven**
3. 配置如下：
   - **Name**: `Maven Deploy (发布模式)`
   - **Command line**: `clean deploy -DskipFatJar=true`
   - **Working directory**: `$PROJECT_DIR$`

4. 再创建一个配置：
   - **Name**: `Maven Package (开发模式)`
   - **Command line**: `clean package -DskipTests`
   - **Working directory**: `$PROJECT_DIR$`

### 方法 2: 使用 Maven 生命周期

在 IntelliJ IDEA 右侧的 **Maven** 工具窗口中：

- **compile** → 编译，生成 fat JAR
- **package** → 打包，生成 fat JAR
- **install** → 安装到本地，生成 fat JAR
- **deploy** → 部署到远程仓库

如果要在 deploy 时跳过 fat JAR，可以：
1. 右键点击 **deploy**
2. 选择 **Modify Run Configuration...**
3. 在 **Command line** 末尾添加: `-DskipFatJar=true`

## 快速命令

```bash
# 开发模式 - 打包 fat JAR
mvn clean package

# 发布模式 - 不打 fat JAR
mvn clean deploy -DskipFatJar=true

# 快速模式 - 跳过签名
mvn clean install -P quick
```

## 配置说明

- `skipFatJar=false` (默认) - 打包 fat JAR
- `skipFatJar=true` - 跳过 fat JAR
- `-P quick` - 跳过 GPG 签名，快速构建

