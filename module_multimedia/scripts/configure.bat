@echo off
::-----------------------------------------------------------------------
:: Fuction: configure.bat
:: Version: 1.0
:: Created: Tuyj
:: Created date:2017/04/13
:: Parameters:
::   $1 -- Project cmake building parent directory
::-----------------------------------------------------------------------

if "%1"==""  (goto invalid_params)

echo Search cmake_build_command.bat in '%1'

for /f "delims=" %%i in ('dir /b /a-d /s "%1\cmake_build_command.bat"') do (
    echo ---- Call %%i
    call %%i
)
goto quit


:invalid_params
echo Invalid params:
echo    $1 -- Project cmake building parent directory. (It looks like ^<ProjectDir^>\module_multimedia\.externalNativeBuild\cmake.custom\debug)

:quit
pause
@echo on