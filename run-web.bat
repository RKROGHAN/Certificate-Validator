@echo off
echo Blockchain Certificate Validator - Web Interface
echo ==============================================
echo.

REM Change to the script's directory
cd /d "%~dp0"

REM Check if Java is available
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not found in PATH. Please install Java or add it to PATH.
    pause
    exit /b 1
)

REM Check if Maven dependencies are copied
if not exist "target\lib" (
    echo Copying Maven dependencies...
    call mvn dependency:copy-dependencies -q
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to copy dependencies. Please run 'mvn dependency:copy-dependencies' manually.
        pause
        exit /b 1
    )
)

REM Compile if needed
if not exist "target\classes" (
    echo Compiling project...
    call mvn compile -q
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Compilation failed.
        pause
        exit /b 1
    )
)

echo Starting the web application...
echo Server will be available at http://localhost:8080
echo Press Ctrl+C to stop the server.
echo.

java -cp "target\classes;target\lib\*" com.certificatevalidator.application.SimpleWebApp

echo.
echo Web application finished. Press any key to exit.
pause > nul
