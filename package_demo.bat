@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0package_demo.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo [FAILED] exit code: %ERRORLEVEL%
) else (
    echo [DONE]
)
pause
