@echo off
chcp 65001 >nul
echo ================================================================================
echo   下载 Paraphrase-Multilingual 模型
echo ================================================================================
echo.

REM 检查 paraphrase-multilingual 目录是否存在
if not exist "paraphrase-multilingual" (
    echo [信息] 创建目录: paraphrase-multilingual
    mkdir "paraphrase-multilingual"
    if errorlevel 1 (
        echo [错误] 无法创建目录
        pause
        exit /b 1
    )
    echo [成功] 目录已创建
    echo.
) else (
    echo [信息] 目录已存在: paraphrase-multilingual
    echo.
)

REM 检查模型文件是否已存在
if exist "paraphrase-multilingual\model.onnx" (
    echo [信息] 模型文件已存在
    choice /C YN /M "是否重新下载"
    if errorlevel 2 (
        echo [信息] 跳过下载
        pause
        exit /b 0
    )
    echo.
)

REM 下载模型文件
echo [信息] 开始下载模型文件...
echo [信息] 下载地址: https://hf-mirror.com/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
echo [信息] 保存路径: paraphrase-multilingual\model.onnx
echo.

curl -L --connect-timeout 30 --retry 3 -C - -# ^
     "https://hf-mirror.com/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx" ^
     -o "paraphrase-multilingual\model.onnx"

if errorlevel 1 (
    echo.
    echo [错误] 下载失败！
    echo.
    echo 可能的原因：
    echo   1. 网络连接问题
    echo   2. URL 地址已变更
    echo   3. curl 未安装或版本过低
    echo.
    echo 建议：
    echo   1. 检查网络连接
    echo   2. 手动下载: https://hf-mirror.com/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/tree/main/onnx
    echo   3. 将 model.onnx 放到 paraphrase-multilingual 目录
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================================================
echo   下载完成！
echo ================================================================================
echo.
echo [信息] 模型文件位置: paraphrase-multilingual\model.onnx

REM 显示文件大小
for %%F in ("paraphrase-multilingual\model.onnx") do (
    set size=%%~zF
    set /a sizeMB=!size! / 1048576
    echo [信息] 文件大小: !sizeMB! MB
)

echo.
echo [提示] 请在 application.yml 中配置模型路径：
echo   vector:
echo     model:
echo       path: ./models/paraphrase-multilingual/model.onnx
echo.
pause

