@echo off
REM 安装 Markdown 支持依赖
REM Install Markdown support dependencies

echo ========================================
echo 安装 Markdown 完整支持依赖
echo Installing Markdown Full Support Dependencies
echo ========================================
echo.

cd UI

echo 正在安装依赖...
echo Installing dependencies...
echo.

npm install react-markdown@9.0.1 remark-gfm@4.0.0 rehype-raw@7.0.0

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ 安装成功！
    echo ✅ Installation Successful!
    echo ========================================
    echo.
    echo 现在可以启动应用：
    echo Now you can start the application:
    echo   npm run dev
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ 安装失败！
    echo ❌ Installation Failed!
    echo ========================================
    echo.
    echo 请尝试：
    echo Please try:
    echo   1. npm cache clean --force
    echo   2. 删除 node_modules 和 package-lock.json
    echo   3. 重新运行此脚本
    echo.
)

cd ..
pause
