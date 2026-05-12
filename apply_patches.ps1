# 应用 patch 文件到当前 git 仓库
# 使用方式：
#   从项目根目录：.\apply_patches.ps1
#   从 zip 解压目录：.\apply_patches.ps1
# 脚本会自动查找与自身同目录下的所有 *.patch 文件
#   若提示执行策略限制，运行：Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
param()
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "========================================"
Write-Host "  Patch 应用脚本 (Windows PowerShell)"
Write-Host "========================================"

# ── 检查是否在 git 仓库内 ─────────────────
git rev-parse --git-dir 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误：当前目录不在 git 仓库中。"
    Write-Host "请在目标仓库根目录运行本脚本，或将本脚本复制到仓库根目录后运行。"
    exit 1
}

# ── 查找 patch 文件 ───────────────────────
Write-Host ""
Write-Host "[1/2] 查找 patch 文件（目录：$ScriptDir）..."
$Patches = Get-ChildItem -Path $ScriptDir -Filter "*.patch" | Sort-Object Name

if ($Patches.Count -eq 0) {
    Write-Host "错误：在 $ScriptDir 中未找到任何 *.patch 文件。"
    Write-Host "请确认已将 patch 文件放在与本脚本相同的目录下。"
    exit 1
}

Write-Host "  找到 $($Patches.Count) 个 patch 文件："
$Patches | ForEach-Object { Write-Host "    $($_.Name)" }

# ── 应用 patch ────────────────────────────
Write-Host ""
Write-Host "[2/2] 应用 patch（使用 git am --3way）..."
Write-Host ""

$PatchPaths = $Patches | ForEach-Object { $_.FullName }
git am --3way @PatchPaths

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "  错误：patch 应用过程中遇到冲突！"
    Write-Host ""
    Write-Host "  选项 A — 手动解决冲突后继续："
    Write-Host "    1. 编辑冲突文件"
    Write-Host "    2. git add <冲突文件>"
    Write-Host "    3. git am --continue"
    Write-Host ""
    Write-Host "  选项 B — 跳过当前 patch："
    Write-Host "    git am --skip"
    Write-Host ""
    Write-Host "  选项 C — 放弃全部应用："
    Write-Host "    git am --abort"
    Write-Host "========================================"
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "  完成！已成功应用 $($Patches.Count) 个 patch。"
Write-Host "  提交历史（最新 $($Patches.Count) 条）："
$n = $Patches.Count
git log --oneline -$n
Write-Host "========================================"
