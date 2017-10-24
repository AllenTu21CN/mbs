#This script is just called manually for testing

[ -z ${SDK_HOME} ] && echo "SDK_HOME is null, export SDK_HOME=/opt/soft/android/sdk" && export SDK_HOME=/opt/soft/android/sdk
[ -z ${NDK_HOME} ] && echo "NDK_HOME is null, export NDK_HOME=/opt/android-ndk-r13b" && export NDK_HOME=/opt/android-ndk-r13b
[ -z ${NDK_TOOLCHAIN_HOME} ] && echo "NDK_TOOLCHAIN_HOME is null, export NDK_TOOLCHAIN_HOME=/opt" && export NDK_TOOLCHAIN_HOME=/opt

DAPANJI_TOP_DIR=`pwd`
BUILD_DIR=BUILD
OUTPUT_DIR=/mnt/builds/develop/terminal/DaPanJi
BUILD_TIME=`date +%Y%m%d%H%M`
GIT_REV_STRING=`git rev-parse HEAD`
GIT_REV_VERSION=${GIT_REV_STRING:0:8}

echo "rm -rf ${DAPANJI_TOP_DIR}/app_mp100/build" && rm -rf ${DAPANJI_TOP_DIR}/app/build
echo "rm -rf ${DAPANJI_TOP_DIR}/module_mediacontrol/build" && rm -rf ${DAPANJI_TOP_DIR}/module_mediacontrol/build
echo "rm -rf ${DAPANJI_TOP_DIR}/module_libraries/build" && rm -rf ${DAPANJI_TOP_DIR}/module_libraries/build
echo "rm -rf ${DAPANJI_TOP_DIR}/module_libraries/.externalNativeBuild" && rm -rf ${DAPANJI_TOP_DIR}/module_libraries/.externalNativeBuild
echo "rm -rf ${DAPANJI_TOP_DIR}/module_libraries/src/main/cpp/avalon" && rm -rf ${DAPANJI_TOP_DIR}/module_libraries/src/main/cpp/avalon
echo "rm -rf ${DAPANJI_TOP_DIR}/module_libraries/src/main/jniLibs" && rm -rf ${DAPANJI_TOP_DIR}/module_libraries/src/main/jniLibs
echo "rm -rf ${DAPANJI_TOP_DIR}/local.properties" && rm -rf ${DAPANJI_TOP_DIR}/local.properties
echo "rm -rf ${DAPANJI_TOP_DIR}/BUILD" && rm -rf ${DAPANJI_TOP_DIR}/BUILD

echo "-----------Gen local.properties" && \
sh ${DAPANJI_TOP_DIR}/module_libraries/scripts/gen_local_properties.sh "${DAPANJI_TOP_DIR}" && \
echo '' && \
\
\
echo "-----------Build app" && \
cd ${DAPANJI_TOP_DIR} && \
gradle makeJni && \
gradle build && \
mkdir -p ${BUILD_DIR} && \
cp app/build/outputs/apk/*.apk ${BUILD_DIR}/ && \
(([ -d module_libraries/src/main/jniLibs ] && cp -a module_libraries/src/main/jniLibs ${BUILD_DIR}/) || cp -a module_libraries/build/intermediates/cmake/release/obj ${BUILD_DIR}/jniLibs) && \
echo '' && echo '' && echo '' && \
\
\
echo "-----------Copy to Output" && \
cd ${DAPANJI_TOP_DIR} && \
mkdir -p ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} && \
echo "cp -r ${BUILD_DIR}/* ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION}" && \
cp -r ${BUILD_DIR}/* ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} && \
rm -f ${OUTPUT_DIR}/latest && \
echo "ln -s ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} ${OUTPUT_DIR}/latest" && \
ln -s ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} ${OUTPUT_DIR}/latest