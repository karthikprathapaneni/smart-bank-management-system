@echo off
title Smart Bank Management System (SBMS) - Tests
echo ===============================================================
echo   Executing SBMS Unit & Concurrency Stress Test Suites...
echo ===============================================================
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Action test
pause
