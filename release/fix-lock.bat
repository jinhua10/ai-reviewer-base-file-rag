@echo off
:: ========================================================================
:: Lucene 索引锁修复工具
:: Fix Lucene Index Lock
:: ========================================================================

chcp 65001 >nul 2>&1

echo ================================================================================
echo   Lucene 索引锁修复工具
echo ================================================================================
echo.

:: 检查应用是否正在运行
echo [1/3] 检查应用状态...
tasklist | findstr /i "java.exe" | findstr /i "ai-reviewer-base-file-rag" >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 应用正在运行！
    echo [提示] 请先运行 stop.bat 停止应用
    echo.
    pause
    exit /b 1
)
echo [信息] 应用未运行 ✓
echo.

:: 检查锁文件
echo [2/3] 检查锁文件...
set LOCK_FILE=data\knowledge-base\index\lucene-index\write.lock
if exist "%LOCK_FILE%" (
    echo [发现] 找到锁文件: %LOCK_FILE%

    :: 显示文件信息
    for %%F in ("%LOCK_FILE%") do (
        echo [信息] 文件大小: %%~zF 字节
        echo [信息] 修改时间: %%~tF
    )
    echo.

    :: 删除锁文件
    echo [3/3] 删除锁文件...
    del "%LOCK_FILE%" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [成功] 锁文件已删除 ✓
    ) else (
        echo [错误] 无法删除锁文件 ✗
        echo [提示] 请使用管理员权限运行此脚本
        echo.
        pause
        exit /b 1
    )
) else (
    echo [信息] 未发现锁文件 ✓
    echo [提示] 索引状态正常，无需修复
)

echo.
echo ================================================================================
echo   修复完成！
echo ================================================================================
echo.
echo [提示] 现在可以运行 start.bat 启动应用
echo.
pause

