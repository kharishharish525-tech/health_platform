@echo off
setlocal
title Rural Health Network Platform - Build & Run

echo ============================================================
echo  Rural Health Network Platform - Build ^& Run (packaged jar)
echo ============================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on your PATH. Install JDK 17+ from https://adoptium.net/
    pause
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven was not found on your PATH. See README.md for install options.
    pause
    exit /b 1
)

echo Building the project (this downloads dependencies the first time)...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed - see the Maven output above for details.
    pause
    exit /b 1
)

echo.
echo Build succeeded. Starting the application...
echo Website:   http://localhost:8080
echo API login: admin / admin123
echo Press Ctrl+C to stop the server.
echo ============================================================
echo.

java -jar target\health-platform.jar

echo.
echo Server stopped.
pause
