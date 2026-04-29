# 打包脚本 - Windows (PowerShell)
# 步骤：clean -> 复制目录 -> 删除 zrun -> 压缩 -> 拷贝到 assets
# 使用方式：右键 -> 用 PowerShell 运行，或在 PowerShell 中执行 .\package_demo.ps1

$ErrorActionPreference = "Stop"

$ProjectDir  = Split-Path -Parent $MyInvocation.MyCommand.Definition
$ProjectName = "DemoHub"
$Date        = Get-Date -Format "yyyyMMddHHmm"
$CopyName    = "${ProjectName}_${Date}"
$ParentDir   = Split-Path -Parent $ProjectDir
$CopyDir     = Join-Path $ParentDir $CopyName
$ZipFile     = Join-Path $ParentDir "${CopyName}.zip"
$AssetsDir   = Join-Path $ProjectDir "rundemo\src\main\assets"

Write-Host "========================================"
Write-Host "  DemoHub 打包脚本"
Write-Host "  日期: $Date"
Write-Host "========================================"

# 步骤 0：清理 assets 中已有的 DemoHub 压缩包
Write-Host ""
Write-Host "[0/5] 检查并清理 assets 中的旧 DemoHub 压缩包..."
$OldZips = Get-ChildItem -Path $AssetsDir -Filter "DemoHub_*.zip" -ErrorAction SilentlyContinue
if ($OldZips) {
    foreach ($z in $OldZips) {
        Write-Host "  删除: $($z.FullName)"
        Remove-Item -Force $z.FullName
    }
    Write-Host "  清理完成"
} else {
    Write-Host "  无旧压缩包，跳过"
}

# 步骤 1：执行 Gradle clean
Write-Host ""
Write-Host "[1/5] 执行 Gradle clean..."
Set-Location $ProjectDir
& ".\gradlew.bat" :app:clean :fitdemo:clean :rundemo:clean :m3demo:clean
if ($LASTEXITCODE -ne 0) { throw "Gradle clean 失败，退出码: $LASTEXITCODE" }
Write-Host "  clean 完成"

# 步骤 2：复制整个 DemoHub 目录
Write-Host ""
Write-Host "[2/5] 复制目录 -> $CopyDir"
if (Test-Path $CopyDir) {
    Write-Host "  目标目录已存在，先删除旧的..."
    Remove-Item -Recurse -Force $CopyDir
}
robocopy $ProjectDir $CopyDir /E /XD ".cursor" ".gradle" ".idea" ".kotlin" /NP /NFL /NDL | Out-Null
if ($LASTEXITCODE -ge 8) { throw "目录复制失败，退出码: $LASTEXITCODE" }
Write-Host "  复制完成（已排除 .cursor .gradle .idea .kotlin）"

# 步骤 3：删除副本中的 zrun 目录
Write-Host ""
Write-Host "[3/5] 删除副本中的 zrun 目录..."
$ZrunDir = Join-Path $CopyDir "zrun"
if (Test-Path $ZrunDir) {
    Remove-Item -Recurse -Force $ZrunDir
    Write-Host "  zrun 目录已删除"
} else {
    Write-Host "  zrun 目录不存在，跳过"
}

# 步骤 4：压缩打包
Write-Host ""
Write-Host "[4/5] 压缩为 ${CopyName}.zip..."
if (Test-Path $ZipFile) {
    Write-Host "  压缩包已存在，先删除旧的..."
    Remove-Item -Force $ZipFile
}
Compress-Archive -Path $CopyDir -DestinationPath $ZipFile
Write-Host "  压缩完成: $ZipFile"

# 步骤 5：拷贝压缩包到 rundemo assets
Write-Host ""
Write-Host "[5/5] 拷贝压缩包到 rundemo\src\main\assets\..."
if (-not (Test-Path $AssetsDir)) {
    New-Item -ItemType Directory -Force -Path $AssetsDir | Out-Null
}
Copy-Item $ZipFile $AssetsDir
Write-Host "  拷贝完成: $AssetsDir\${CopyName}.zip"

Write-Host ""
Write-Host "========================================"
Write-Host "  打包完成！"
Write-Host "  压缩包: $ZipFile"
Write-Host "  assets: $AssetsDir\${CopyName}.zip"
Write-Host "========================================"
