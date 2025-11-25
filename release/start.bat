@echo off
chcp 65001 >nul 2>&1
:: ========================================================================
:: 知识库问答系统 - 启动脚本
:: Knowledge QA System - Startup Script
:: ========================================================================

setlocal EnableDelayedExpansion

echo ================================================================================
echo   知识库问答系统 (Knowledge QA System)
echo   版本: 1.0.0
echo ================================================================================
echo.

:: ========================================================================
:: OCR图片文字识别配置
:: ========================================================================
:: 启用OCR功能（如果tessdata目录存在则自动启用）
:: 如果尚未下载语言包，请运行: download-tessdata.bat
:: 如需禁用OCR，请在下面三行前面添加 :: 注释
:: ========================================================================
set ENABLE_OCR=true
set TESSDATA_PREFIX=%~dp0tessdata
set OCR_LANGUAGE=chi_sim+eng
:: ========================================================================

:: 设置环境变量
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dspring.main.web-application-type=servlet
set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx2g
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=200

:: 查找JAR文件
set JAR_FILE=
for %%f in (*.jar) do (
    set JAR_FILE=%%f
    goto :found_jar
)

:found_jar
if "%JAR_FILE%"=="" (
    echo [错误] 未找到JAR文件
    echo.
    echo 请确保 ai-reviewer-base-file-rag-1.0-jar-with-dependencies.jar 文件在当前目录
    pause
    exit /b 1
)

echo [信息] 找到JAR文件: %JAR_FILE%
echo.

:: 检查Java环境
jdk21/bin/java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Java环境
    echo.
    echo 请安装 JDK 17 或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [信息] Java环境检查通过
echo.

:: 检查配置文件
if not exist "config\application.yml" (
    echo [警告] 未找到外置配置文件: config\application.yml
    echo [信息] 将使用jar包内的默认配置
    echo.
)

:: 检查文档目录
if not exist "data\documents" (
    echo [警告] 文档目录不存在: data\documents
    echo [信息] 正在创建文档目录...
    mkdir data\documents 2>nul
    echo.
    echo [提示] 请将要索引的文档放到 data\documents 目录
    echo        支持格式: Excel^(.xlsx, .xls^), Word^(.docx^), PDF^(.pdf^), TXT^(.txt^) 等
    echo.
)

:: 检查数据目录
if not exist "data" mkdir data 2>nul
if not exist "logs" mkdir logs 2>nul

:: 显示配置信息
echo ================================================================================
echo   启动配置
echo ================================================================================
echo   JAR文件:    %JAR_FILE%
echo   配置文件:   config\application.yml
echo   文档目录:   data\documents
echo   数据目录:   data
echo   日志目录:   logs
echo   JVM参数:    %JAVA_OPTS%
echo ================================================================================
echo.

:: 提示用户
echo [信息] 正在启动应用...
echo [提示] 按 Ctrl+C 停止应用
echo.
echo ================================================================================
echo.

:: 启动应用
jdk21/bin/java %JAVA_OPTS% ^
  -jar %JAR_FILE% ^
  --spring.config.location=file:./config/application.yml ^
  --logging.file.name=./logs/knowledge-qa-system.log

:: 检查退出状态
if errorlevel 1 (
    echo.
    echo ================================================================================
    echo   [错误] 应用启动失败
    echo ================================================================================
    echo.
    echo 常见问题排查:
    echo   1. 检查端口 8080 是否被占用
    echo   2. 查看日志文件: logs\knowledge-qa-system.log
    echo   3. 确认配置文件格式正确: config\application.yml
    echo   4. 确认文档目录存在且有权限: data\documents
    echo.
    pause
    exit /b 1
)

endlocal

