@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist lib mkdir lib
if not exist out mkdir out

call :download lib\sqlite.jar org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar || exit /b 1
call :download lib\gson.jar com/google/code/gson/gson/2.12.1/gson-2.12.1.jar || exit /b 1

rem javac does not expand wildcards in cmd, so build an argument file (src\*\*.java)
if exist sources.txt del sources.txt
for /d %%d in (src\*) do (
    for %%f in ("%%d\*.java") do (
        set "p=%%f"
        set "p=!p:\=/!"
        echo "!p!">>sources.txt
    )
)
if not exist sources.txt (
    echo No Java sources found in src\*\ 1>&2
    exit /b 1
)

javac --release 17 -encoding UTF-8 -cp "lib\*" -d out @sources.txt
set "rc=%errorlevel%"
del sources.txt
exit /b %rc%

:download
set "file=%~1"
set "size=0"
if exist "%file%" for %%A in ("%file%") do set "size=%%~zA"
if %size% gtr 0 exit /b 0
curl --fail --silent --show-error --location "https://repo.maven.apache.org/maven2/%~2" -o "%file%.tmp" || (
    if exist "%file%.tmp" del "%file%.tmp"
    exit /b 1
)
move /y "%file%.tmp" "%file%" >nul
exit /b 0
