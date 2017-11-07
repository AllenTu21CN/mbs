@echo off
::-----------------------------------------------------------------------
:: Fuction: make.bat
:: Version: 1.0
:: Created: Tuyj
:: Created date:2017/04/13
:: Parameters:
::   $1 -- Project cmake building parent directory
::   $2 -- Make program
::-----------------------------------------------------------------------

if "%1"==""  (goto invalid_params)
if "%2"==""  (goto invalid_params)

for /f "delims=" %%i in ('dir /b /ad "%1"') do (
    echo ---- %2 -C %1\%%i
    %2 -C %1\%%i || exit 1
)
goto quit


:invalid_params
echo Invalid params:
echo    $1 -- Project cmake building parent directory. (It looks like ^<ProjectDir^>\module_multimedia\.externalNativeBuild\cmake.custom\debug)
echo    $2 -- Make program. (It looks like ^<SDK_HOME^>\cmake\^<version^>\bin\ninja)

:quit
pause
@echo on