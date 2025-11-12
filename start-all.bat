@echo off
REM TestSync Quick Start - starts all 4 servers in separate cmd windows
REM Run this batch file from the project root: D:\projectnn\TestSync---Quiz-App

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   TestSync - Exam Control System
echo   Starting all servers...
echo ========================================
echo.

REM Kill any old Java processes
echo Cleaning up old processes...
taskkill /F /IM java.exe >nul 2>&1

REM Delete old state file
del exam_state.txt >nul 2>&1

REM Wait a moment
timeout /T 1 /nobreak >nul

REM Compile first
echo Compiling Java files with UTF-8 encoding...
javac -encoding UTF-8 -cp . server\ExamState.java server\AdminHTTPServer.java server\ExamServer.java server\ResultManager.java broadcaster\TimerBroadcaster.java client\StudentClient.java
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting servers in separate windows...
echo.

REM Terminal 1: TimerBroadcaster
start "TimerBroadcaster" cmd /k "java -cp . broadcaster.TimerBroadcaster"

REM Terminal 2: Admin HTTP Server
start "Admin Dashboard - http://localhost:8080/dashboard.html" cmd /k "java -cp . server.AdminHTTPServer 8080"

REM Terminal 3: Exam Server
start "Exam Server - Type START here" cmd /k "java -cp . server.ExamServer"

REM Terminal 4: First StudentClient
start "Student Client 1" cmd /k "java -cp . client.StudentClient"

echo.
echo All servers have been started in separate windows!
echo.
echo NEXT STEPS:
echo ===========
echo 1. Open your browser and go to: http://localhost:8080/dashboard.html
echo 2. In the "Student Client 1" window, log in as:
echo    Username: student1
echo    Password: pass123
echo 3. You will see "Quiz Yet to Start" waiting screen
echo 4. In the "Exam Server" window, type START and press Enter
echo 5. The exam will begin and students will see questions
echo 6. Watch the dashboard update in real-time!
echo.
echo To test with more students:
echo   - Open more cmd windows and run: java -cp . client.StudentClient
echo   - Log in as student2, student3, etc. (password: pass123)
echo.
echo To stop all servers:
echo   - Close each window individually
echo.
pause
