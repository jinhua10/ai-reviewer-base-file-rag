# 检查前端依赖的许可证
$packages = @(
    "antd",
    "@ant-design/icons",
    "react",
    "react-dom",
    "react-markdown",
    "axios",
    "highlight.js",
    "html2pdf.js",
    "marked",
    "remark-gfm",
    "rehype-raw",
    "vite",
    "@vitejs/plugin-react"
)

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "前端依赖许可证检查报告" -ForegroundColor Cyan
Write-Host "======================================`n" -ForegroundColor Cyan

foreach ($pkg in $packages) {
    $pkgPath = "UI/node_modules/$pkg/package.json"
    if (Test-Path $pkgPath) {
        try {
            $json = Get-Content $pkgPath -Raw | ConvertFrom-Json
            $license = if ($json.license) { $json.license } else { "未指定" }
            $version = if ($json.version) { $json.version } else { "未知" }

            $color = "Green"
            if ($license -match "GPL" -and $license -notmatch "Classpath") {
                $color = "Red"
            } elseif ($license -eq "未指定") {
                $color = "Yellow"
            }

            Write-Host "📦 $pkg" -ForegroundColor White
            Write-Host "   版本: $version" -ForegroundColor Gray
            Write-Host "   许可证: $license" -ForegroundColor $color
            Write-Host ""
        } catch {
            Write-Host "⚠️  无法读取 $pkg 的信息" -ForegroundColor Yellow
            Write-Host ""
        }
    } else {
        Write-Host "❌ $pkg - 未安装" -ForegroundColor Red
        Write-Host ""
    }
}

Write-Host "`n======================================" -ForegroundColor Cyan
Write-Host "检查完成" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

