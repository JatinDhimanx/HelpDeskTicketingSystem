@echo off
title Help Desk Ticketing System
cd /d "%~dp0"
if exist "target\dist\HelpDeskApp\HelpDesk.exe" (
    start "" "target\dist\HelpDeskApp\HelpDesk.exe"
) else (
    start "" javaw -jar "target\dist\HelpDeskTicketingSystem.jar"
)
