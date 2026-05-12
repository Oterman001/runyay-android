@echo off
chcp 65001 >nul
where pwsh.exe >nul 2>&1
if %errorlevel% == 0 (
    pwsh.exe -ExecutionPolicy Bypass -File "%~dp0generate_patches.ps1"
) else (
    echo [错误] 未找到 pwsh.exe，请安装 PowerShell 7：
    echo   https://aka.ms/install-powershell
    pause
    exit /b 1
)
echo.
pause
