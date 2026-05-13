@echo off
pwsh.exe -ExecutionPolicy Bypass -File "%~dp0generate_patches.ps1"
echo.
pause
