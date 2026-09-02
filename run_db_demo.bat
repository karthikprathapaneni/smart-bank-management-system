@echo off
title Smart Bank Management System - Live MySQL JDBC Demo
echo ===============================================================
echo   Starting Smart Bank Management System MySQL Live Demo...
echo ===============================================================
powershell -ExecutionPolicy Bypass -File .\build_and_run.ps1 -Action db-demo
pause
