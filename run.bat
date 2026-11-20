@echo off
title Help Desk Ticketing System
cd /d "%~dp0"

set "JAVA_EXE=javaw"
if exist "C:\Program Files\Java\jdk-26.0.1\bin\javaw.exe" (
    set "JAVA_EXE=C:\Program Files\Java\jdk-26.0.1\bin\javaw.exe"
)

if exist "target\dist\HelpDeskTicketingSystem.jar" (
    start "" "%JAVA_EXE%" -jar "target\dist\HelpDeskTicketingSystem.jar"
) else (
    echo [ERROR] target\dist\HelpDeskTicketingSystem.jar not found. Please build the project first.
    pause
)
