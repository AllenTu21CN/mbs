#!/bin/sh 
#-----------------------------------------------------------------------
# Fuction: Gen jni head files (from .java to .h) 
# Version: 1.0
# Created: Tuyj
# Created date:2017/04/12
# Parameters: 
#    $1 -- Android SDK Home
#-----------------------------------------------------------------------

if [ ! $# -eq 1 ]
then
    echo "Input error. Usage:"
    echo "  \$1  -- Android SDK Home (It looks like /opt/soft/android/sdk)"
    exit 1
fi 

SDK_HOME=$1
OUT_DIR="../src/main/cpp"
LOCAL_CLASS_PATH="../build/intermediates/classes/release"
ANDROID_SDK_VERSION="android-25"


ANDROID_SDK_JAR="${SDK_HOME}/platforms/${ANDROID_SDK_VERSION}/android.jar"
[ ! -f ${ANDROID_SDK_JAR} ] && echo Error: \"${ANDROID_SDK_JAR}\" is not exist && exit -1
echo Using android sdk jar: \"${ANDROID_SDK_JAR}\"

[ ! -d ${LOCAL_CLASS_PATH} ] && echo Error: \"${LOCAL_CLASS_PATH}\" is not exist. Maybe you need to compile application first. && exit -1
echo Using local class path: \"${LOCAL_CLASS_PATH}\"

echo "Start path is \"${PWD}\""

echo "----Gen head file from RTMPPushClient.class to \"${PWD}/${OUT_DIR}/\""
javah -d ${OUT_DIR} -classpath ${ANDROID_SDK_JAR}:${LOCAL_CLASS_PATH} sanp.javalon.network.protocol.RTMPPushClient

echo "----Gen head file from RTSPClient.class to \"${PWD}/${OUT_DIR}/\""
javah -d ${OUT_DIR} -classpath ${LOCAL_CLASS_PATH} sanp.javalon.network.protocol.RTSPClient

echo "----Gen head file from RTMPBandwidthTest.class to \"${PWD}/${OUT_DIR}/\""
javah -d ${OUT_DIR} -classpath ${ANDROID_SDK_JAR}:${LOCAL_CLASS_PATH} sanp.javalon.network.protocol.RTMPBandwidthTest

cd ${START_PATH}

