@echo off

set OUT_DIR=..\src\main\cpp
set LOCAL_CLASS_PATH=..\build\intermediates\classes\release
set ANDROID_SDK_JAR=E:\Tools\AndroidSDK\platforms\android-25\android.jar

if exist "%ANDROID_SDK_JAR%" (echo Using android sdk jar: "%ANDROID_SDK_JAR%") else (goto no_sdk)
if exist "%LOCAL_CLASS_PATH%" (echo Using local class path: "%LOCAL_CLASS_PATH%") else (goto path_invalid)

echo Start path is "%CD%"

echo ----Gen head file of RTSPClient to %CD%\%OUT_DIR%\
javah -d %OUT_DIR% -classpath %LOCAL_CLASS_PATH% sanp.avalon.libs.network.protocol.RTSPClient

echo ----Gen head file of RTMPPushClient to %CD%\%OUT_DIR%\
javah -d %OUT_DIR% -classpath %ANDROID_SDK_JAR%;%LOCAL_CLASS_PATH% sanp.avalon.libs.network.protocol.RTMPPushClient

echo ----Gen head file of RTMPBandwidthTest to %CD%\%OUT_DIR%\
javah -d %OUT_DIR% -classpath %ANDROID_SDK_JAR%;%LOCAL_CLASS_PATH% sanp.avalon.libs.network.protocol.RTMPBandwidthTest

goto quit

:no_sdk
echo Error: "%ANDROID_SDK_JAR%" is not exist. It looks like "<SDK_HOME>\platforms\android-25\android.jar"
goto quit

:path_invalid
echo Error: "%LOCAL_CLASS_PATH%" is not exist. Maybe you need to compile application first.
goto quit

:quit
pause
@echo on