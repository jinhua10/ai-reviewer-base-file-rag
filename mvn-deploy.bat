@echo off
mvn clean deploy -DskipFatJar=true %*
echo [INFO] 执行 deploy 模式（跳过 fat JAR 打包）

REM 自动设置 skipFatJar=true
REM Maven Deploy 包装脚本

