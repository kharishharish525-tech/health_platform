@echo off
setlocal
title Rural Health Network Platform

echo ============================================================
echo  Rural Health Network Platform - Startup
echo ============================================================
echo.

REM ---- Check Java ----
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on your PATH.
    echo         Install JDK 17 or newer from https://adoptium.net/ and try again.
    echo.
    pause
    exit /b 1
)

REM ---- Check Maven ----
where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven was not found on your PATH.
    echo.
    echo         Install it one of these ways, then re-run this script:
    echo           1. winget install Apache.Maven
    echo           2. choco install maven
    echo           3. Download from https://maven.apache.org/download.cgi
    echo              and add its "bin" folder to your PATH.
    echo.
    pause
    exit /b 1
)

echo [OK] Java and Maven were found.
echo.
echo Make sure MySQL is running locally before continuing
echo   (default expected: localhost:3306, user "root", password "root" -
echo    edit src\main\resources\application.properties to change this).
echo.
pause

echo.
echo Starting the application...
echo Website:   http://localhost:8080
echo API login: admin / admin123  (see README.md to change)
echo.
echo Press Ctrl+C in this window to stop the server.
echo ============================================================
echo.

call mvn spring-boot:run

echo.
echo Server stopped.
pause
