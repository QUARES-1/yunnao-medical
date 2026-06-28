@echo off
chcp 65001 >nul
title Coverage Preview Servers

cd /d "%~dp0"
node scripts\coverage-preview-server.mjs

echo.
echo Coverage preview server stopped.
pause
