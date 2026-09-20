@echo off
rem Builds the desktop app and opens the folder it lands in.
rem Double-click this instead of typing gradlew commands in a terminal.

cd /d "%~dp0"

echo Building the app. The first run downloads Java and takes a few minutes.
echo.
call gradlew.bat packageApp

if errorlevel 1 (
    echo.
    echo The build failed. The messages above say why.
    pause
    exit /b 1
)

echo.
echo Done. Opening the folder - run "Knights Tour Pro.exe" inside it.
start "" "build\dist\Knights Tour Pro"
