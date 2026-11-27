@echo off
rem Double-click to build, start the server and open the app.
setlocal
cd /d "%~dp0src"

rem Find a JDK: IntelliJ downloads first, then JAVA_HOME, then PATH
set "JDK_BIN="
for /d %%d in ("%USERPROFILE%\.jdks\*") do if exist "%%d\bin\javac.exe" set "JDK_BIN=%%d\bin"
if not defined JDK_BIN if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_BIN=%JAVA_HOME%\bin"
if defined JDK_BIN set "PATH=%JDK_BIN%;%PATH%"
where javac >nul 2>nul || (
    echo JDK not found. Please install JDK 17 or newer.
    goto fail
)

if "%~1"=="server" goto server

call :is_running
if not errorlevel 1 (
    echo The server is already running. To use new code, close it first.
    goto open
)

echo Building...
call "%~dp0src\build.bat"
if errorlevel 1 (
    echo Build failed. See the messages above.
    goto fail
)
cd /d "%~dp0src"

echo Starting the server...
start "Course server - close this window to stop" /min cmd /k call "%~f0" server

for /l %%i in (1,1,20) do (
    call :is_running && goto open
    ping -n 2 127.0.0.1 >nul
)
echo The server did not start. Check the "Course server" window on the taskbar.
goto fail

:open
echo Opening the app...
start "" javaw -cp "out;lib/*" main.SwingApp
start "" http://127.0.0.1:8000
exit /b 0

:server
java "-Djdk.net.unixdomain.tmpdir=%CD%" --enable-native-access=ALL-UNNAMED -cp "out;lib/*" main.WebServer
exit /b

:is_running
curl -s -m 2 http://127.0.0.1:8000/ 2>nul | findstr /c:"loginForm" >nul
exit /b %errorlevel%

:fail
pause
exit /b 1
