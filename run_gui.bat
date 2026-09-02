@echo off
title Smart Bank Management System (SBMS)
echo ===============================================================
echo   Launching Smart Bank Management System Desktop GUI...
echo ===============================================================
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Action run
pause
