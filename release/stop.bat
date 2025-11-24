@echo off
:: ========================================================================
:: 知识库问答系统 - 停止脚本
:: Knowledge QA System - Stop Script
:: ========================================================================

chcp 65001 >nul 2>&1

echo ================================================================================
echo   停止知识库问答系统
echo ================================================================================
echo.

:: 查找Java进程
for /f "tokens=2" %%a in ('tasklist ^| findstr /i "java.exe"') do (
    set PID=%%a

    :: 检查是否是我们的应用
    wmic process where processid=!PID! get commandline 2>nul | findstr /i "ai-reviewer-base-file-rag" >nul
    if !errorlevel! equ 0 (
        echo [信息] 找到应用进程: PID=!PID!
        echo [信息] 正在停止...
        taskkill /PID !PID! /F >nul 2>&1
        if !errorlevel! equ 0 (
            echo [成功] 应用已停止
        ) else (
            echo [错误] 停止失败
        )
        goto :done
    )
)

echo [信息] 未找到运行中的应用

:done
echo.

:: 清理 Lucene 锁文件（如果存在）
set LOCK_FILE=data\knowledge-base\index\lucene-index\write.lock
if exist "%LOCK_FILE%" (
    echo [信息] 检测到 Lucene 锁文件
    del "%LOCK_FILE%" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [成功] 已清理锁文件
    ) else (
        echo [警告] 无法删除锁文件，请手动删除: %LOCK_FILE%
    )
) else (
    echo [信息] 无需清理锁文件
)

echo.
echo [提示] 应用已安全停止
echo.
pause

