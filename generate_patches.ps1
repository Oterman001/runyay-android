# 生成 patch 文件并打包到 rundemo/src/main/assets/
# 使用方式：在项目根目录执行 .\generate_patches.ps1
#   若提示执行策略限制，运行：Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
param()
$ErrorActionPreference = "Stop"

$Remote     = "gitee"
$Branch     = "master"
$AssetsDir  = "rundemo\src\main\assets"
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$TmpDir     = Join-Path $ScriptDir ".patches_tmp"
$TargetDir  = Join-Path $ScriptDir $AssetsDir

Write-Host "========================================"
Write-Host "  Patch 生成脚本 (Windows PowerShell)"
Write-Host "========================================"

# ── [1/5] 拉取最新远端 ─────────────────────
Write-Host ""
Write-Host "[1/5] 从 $Remote 拉取最新代码..."
git fetch $Remote
if ($LASTEXITCODE -ne 0) { Write-Error "git fetch 失败"; exit 1 }

# ── [2/5] Rebase 到云端最新 ───────────────
Write-Host ""
Write-Host "[2/5] Rebase 到 $Remote/$Branch..."
git rebase "$Remote/$Branch"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "错误：Rebase 失败，可能存在冲突。"
    Write-Host "请手动解决冲突后执行："
    Write-Host "  git rebase --continue"
    Write-Host "或放弃本次 rebase："
    Write-Host "  git rebase --abort"
    git rebase --abort 2>$null
    exit 1
}

# ── [3/5] 统计未推送提交 ──────────────────
Write-Host ""
Write-Host "[3/5] 检查未推送的提交..."
$CommitCount = (git rev-list "$Remote/$Branch..HEAD" --count).Trim()
if ($CommitCount -eq "0") {
    Write-Host "  没有未推送的提交，无需生成 patch。"
    exit 0
}

Write-Host "  找到 $CommitCount 个未推送的提交："
git log "$Remote/$Branch..HEAD" --oneline

# ── [4/5] 生成 patch 文件 ─────────────────
Write-Host ""
Write-Host "[4/5] 生成 patch 文件..."

# 清理：删除 assets 目录下所有历史 patches_*.zip
$OldZips = Get-ChildItem -Path $TargetDir -Filter "patches_*.zip" -ErrorAction SilentlyContinue
if ($OldZips.Count -gt 0) {
    Write-Host "  清理 assets 下旧 patch 包："
    $OldZips | ForEach-Object {
        Write-Host "    删除 $($_.Name)"
        Remove-Item $_.FullName -Force
    }
}

# 清理：删除残留 tmp 目录
if (Test-Path $TmpDir) { Remove-Item -Recurse -Force $TmpDir }
New-Item -ItemType Directory -Path $TmpDir | Out-Null

git format-patch "$Remote/$Branch..HEAD" -o $TmpDir
if ($LASTEXITCODE -ne 0) { Write-Error "git format-patch 失败"; exit 1 }

# 将脚本（ps1 + bat）一起打包进去
$ScriptsToCopy = @(
    @{ Src = $MyInvocation.MyCommand.Path;               Dst = "generate_patches.ps1" },
    @{ Src = (Join-Path $ScriptDir "generate_patches.bat"); Dst = "generate_patches.bat" },
    @{ Src = (Join-Path $ScriptDir "apply_patches.ps1");  Dst = "apply_patches.ps1"  },
    @{ Src = (Join-Path $ScriptDir "apply_patches.bat");  Dst = "apply_patches.bat"  }
)
foreach ($s in $ScriptsToCopy) {
    if (-not (Test-Path $s.Src)) {
        Write-Error "找不到 $($s.Dst)，请确保它与本脚本在同一目录。"
        Remove-Item -Recurse -Force $TmpDir
        exit 1
    }
    Copy-Item $s.Src (Join-Path $TmpDir $s.Dst)
}

Write-Host "  Patch 目录内容："
Get-ChildItem $TmpDir | Select-Object -ExpandProperty Name | ForEach-Object { Write-Host "    $_" }

# ── [5/5] 打包并拷贝到 assets ─────────────
Write-Host ""
Write-Host "[5/5] 打包并复制到 $AssetsDir..."
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ZipName   = "patches_$Timestamp.zip"
$ZipPath   = Join-Path $ScriptDir $ZipName

Compress-Archive -Path "$TmpDir\*" -DestinationPath $ZipPath -Force
Move-Item $ZipPath (Join-Path $TargetDir $ZipName)
Remove-Item -Recurse -Force $TmpDir

Write-Host ""
Write-Host "========================================"
Write-Host "  完成！"
Write-Host "  ZIP 已保存到：$AssetsDir\$ZipName"
Write-Host "  包含 $CommitCount 个 patch 文件 + 两个脚本"
Write-Host "========================================"
