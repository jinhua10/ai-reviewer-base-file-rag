@echo off
chcp 65001 >nul 2>&1
REM 修复 Lucene 索引锁问题
REM Fix Lucene index lock issue

echo ========================================
echo 清理 Lucene 索引锁文件
echo Cleaning Lucene index lock files
echo ========================================
echo.

set LOCK_FILE=data\knowledge-base\index\lucene-index\write.lock

if exist "%LOCK_FILE%" (
    echo 发现锁文件: %LOCK_FILE%
    echo Found lock file: %LOCK_FILE%
    del /F "%LOCK_FILE%"
    if %ERRORLEVEL% EQU 0 (
        echo ✅ 锁文件已删除
        echo ✅ Lock file removed successfully
    ) else (
        echo ❌ 删除失败，请手动删除
        echo ❌ Failed to remove, please delete manually
    )
) else (
    echo ℹ️  未发现锁文件，无需清理
    echo ℹ️  No lock file found, nothing to clean
)

echo.
echo ========================================
echo 完成！现在可以重新启动应用
echo Done! You can now restart the application
echo ========================================
pause

