# adb 日志抓取脚本 - Windows (PowerShell)
# 功能：通过 adb logcat 抓取 Android 日志并导出为 UTF-8 文件
# 使用：右键 -> 用 PowerShell 运行，或在 PowerShell 中执行 .\adb_logcat.ps1
# 参数示例：
#   .\adb_logcat.ps1                          # 抓取所有日志
#   .\adb_logcat.ps1 -Tag "rundemo"           # 按 Tag 过滤
#   .\adb_logcat.ps1 -Level W                 # 只抓 Warning 及以上
#   .\adb_logcat.ps1 -Tag "rundemo" -Level D  # 组合过滤

param(
    [string]$Tag   = "",       # 按 Tag 过滤，空表示不过滤
    [string]$Level = "V",      # 日志级别：V D I W E F（Verbose ~ Fatal）
    [string]$OutputDir = "",   # 输出目录，默认为脚本所在目录下的 log/
    [switch]$ClearFirst        # 抓取前先清空设备日志缓冲区
)

$ErrorActionPreference = "Stop"

# ── 输出目录 ──────────────────────────────────────────────
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
if ($OutputDir -eq "") {
    $OutputDir = Join-Path $ScriptDir "log"
}
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

# ── 文件名（时间戳） ──────────────────────────────────────
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$TagPart   = if ($Tag -ne "") { "_$Tag" } else { "" }
$LogFile   = Join-Path $OutputDir "logcat${TagPart}_${Timestamp}.log"

# ── 检查 adb ──────────────────────────────────────────────
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "[错误] 找不到 adb，请确认 Android SDK platform-tools 已加入 PATH" -ForegroundColor Red
    exit 1
}

# ── 检查设备连接 ───────────────────────────────────────────
$Devices = adb devices | Select-String -Pattern "^\S+\s+(device)$"
if ($Devices.Count -eq 0) {
    Write-Host "[错误] 未检测到已连接的 Android 设备，请通过 USB 或 WiFi 连接设备" -ForegroundColor Red
    exit 1
}
Write-Host "[设备] 检测到 $($Devices.Count) 台设备" -ForegroundColor Cyan
$Devices | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkCyan }

# ── 可选：清空日志缓冲区 ───────────────────────────────────
if ($ClearFirst) {
    Write-Host "[准备] 清空设备日志缓冲区..." -ForegroundColor Yellow
    adb logcat -c
    Start-Sleep -Milliseconds 300
}

# ── 构造 logcat 过滤参数 ───────────────────────────────────
# adb logcat 原生使用 UTF-8，Windows 控制台默认 GBK 会乱码
# 方案：让 adb 直接写文件流，PowerShell 以 UTF-8 读取并重新写出
$FilterArgs = @("-v", "threadtime")
if ($Tag -ne "") {
    # 指定 Tag:Level，其余静默
    $FilterArgs += "$Tag`:$Level"
    $FilterArgs += "*:S"
} else {
    # 全量日志，按级别过滤
    $FilterArgs += "*:$Level"
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Green
Write-Host "  ADB 日志抓取" -ForegroundColor Green
Write-Host "  时间: $Timestamp" -ForegroundColor Green
Write-Host "  过滤: Tag=$(if($Tag -eq ''){'全部'}else{$Tag})  Level=$Level" -ForegroundColor Green
Write-Host "  输出: $LogFile" -ForegroundColor Green
Write-Host "════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "按 Ctrl+C 停止抓取..." -ForegroundColor Yellow
Write-Host ""

# ── 写入文件头 ────────────────────────────────────────────
$Header = @"
# ================================================
# ADB Logcat 日志导出
# 时间: $Timestamp
# 设备: $(adb shell getprop ro.product.model 2>$null) / $(adb shell getprop ro.build.version.release 2>$null)
# 过滤: Tag=$(if($Tag -eq ''){'ALL'}else{$Tag})  Level=$Level
# ================================================

"@
# 以 UTF-8 with BOM 写文件头（Excel/记事本均可直接识别中文）
[System.IO.File]::WriteAllText($LogFile, $Header, [System.Text.Encoding]::UTF8)

# ── 启动 adb logcat 并实时写文件 ─────────────────────────
# 设置 adb 输出编码为 UTF-8
$env:PYTHONIOENCODING = "utf-8"   # 部分工具链用到

$ProcessInfo = New-Object System.Diagnostics.ProcessStartInfo
$ProcessInfo.FileName               = "adb"
$ProcessInfo.Arguments              = "logcat " + ($FilterArgs -join " ")
$ProcessInfo.RedirectStandardOutput = $true
$ProcessInfo.RedirectStandardError  = $true
$ProcessInfo.UseShellExecute        = $false
$ProcessInfo.CreateNoWindow         = $true
# 强制以 UTF-8 读取 adb 输出，避免中文乱码
$ProcessInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
$ProcessInfo.StandardErrorEncoding  = [System.Text.Encoding]::UTF8

$Process = New-Object System.Diagnostics.Process
$Process.StartInfo = $ProcessInfo

# 实时追加写文件
$Writer = New-Object System.IO.StreamWriter($LogFile, $true, [System.Text.Encoding]::UTF8)

$LineCount = 0

try {
    $Process.Start() | Out-Null

    while (-not $Process.StandardOutput.EndOfStream) {
        $Line = $Process.StandardOutput.ReadLine()
        if ($null -ne $Line) {
            $Writer.WriteLine($Line)
            $Writer.Flush()
            $LineCount++

            # 控制台实时显示（Windows 控制台若为 GBK 则中文可能乱码，文件本身无影响）
            Write-Host $Line
        }
    }
} finally {
    $Writer.Close()
    if (-not $Process.HasExited) {
        $Process.Kill()
    }
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Green
Write-Host "  抓取结束，共 $LineCount 行" -ForegroundColor Green
Write-Host "  文件: $LogFile" -ForegroundColor Green
Write-Host "════════════════════════════════════════" -ForegroundColor Green
