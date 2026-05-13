@echo off
pwsh.exe -ExecutionPolicy Bypass -File "%~dp0apply_patches.ps1"
echo.
pause
