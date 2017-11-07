#This script is just called manually for testing

[ -z ${SDK_HOME} ] && echo "SDK_HOME is null, export SDK_HOME=/opt/soft/android/sdk" && export SDK_HOME=/opt/soft/android/sdk
[ -z ${NDK_HOME} ] && echo "NDK_HOME is null, export NDK_HOME=/opt/android-ndk-r13b" && export NDK_HOME=/opt/android-ndk-r13b
[ -z ${NDK_TOOLCHAIN_HOME} ] && echo "NDK_TOOLCHAIN_HOME is null, export NDK_TOOLCHAIN_HOME=/opt" && export NDK_TOOLCHAIN_HOME=/opt

MPX_TOP_DIR=`pwd`
BUILD_DIR=BUILD
OUTPUT_DIR=/mnt/builds/develop/terminal/MPX
BUILD_TIME=`date +%Y%m%d%H%M`
GIT_REV_STRING=`git rev-parse HEAD`
GIT_REV_VERSION=${GIT_REV_STRING:0:8}

echo "rm -rf ${MPX_TOP_DIR}/app_mp100/build" && rm -rf ${MPX_TOP_DIR}/app/build
echo "rm -rf ${MPX_TOP_DIR}/module_mediacontrol/build" && rm -rf ${MPX_TOP_DIR}/module_mediacontrol/build
echo "rm -rf ${MPX_TOP_DIR}/module_multimedia/build" && rm -rf ${MPX_TOP_DIR}/module_multimedia/build
echo "rm -rf ${MPX_TOP_DIR}/module_multimedia/.externalNativeBuild" && rm -rf ${MPX_TOP_DIR}/module_multimedia/.externalNativeBuild
echo "rm -rf ${MPX_TOP_DIR}/module_multimedia/src/main/cpp/avalon" && rm -rf ${MPX_TOP_DIR}/module_multimedia/src/main/cpp/avalon
echo "rm -rf ${MPX_TOP_DIR}/module_multimedia/src/main/jniLibs" && rm -rf ${MPX_TOP_DIR}/module_multimedia/src/main/jniLibs
echo "rm -rf ${MPX_TOP_DIR}/local.properties" && rm -rf ${MPX_TOP_DIR}/local.properties
echo "rm -rf ${MPX_TOP_DIR}/BUILD" && rm -rf ${MPX_TOP_DIR}/BUILD

echo "-----------Gen local.properties" && \
sh ${MPX_TOP_DIR}/module_multimedia/scripts/gen_local_properties.sh "${MPX_TOP_DIR}" && \
echo '' && \
\
\
echo "-----------Build app" && \
cd ${MPX_TOP_DIR} && \
gradle makeJni && \
gradle build && \
mkdir -p ${BUILD_DIR} && \
cp app/build/outputs/apk/*.apk ${BUILD_DIR}/ && \
(([ -d module_multimedia/src/main/jniLibs ] && cp -a module_multimedia/src/main/jniLibs ${BUILD_DIR}/) || cp -a module_multimedia/build/intermediates/cmake/release/obj ${BUILD_DIR}/jniLibs) && \
echo '' && echo '' && echo '' && \
\
\
echo "-----------Copy to Output" && \
cd ${MPX_TOP_DIR} && \
mkdir -p ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} && \
echo "cp -r ${BUILD_DIR}/* ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION}" && \
cp -r ${BUILD_DIR}/* ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} && \
rm -f ${OUTPUT_DIR}/latest && \
echo "ln -s ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} ${OUTPUT_DIR}/latest" && \
ln -s ${OUTPUT_DIR}/${BUILD_TIME}-${GIT_REV_VERSION} ${OUTPUT_DIR}/latest