#!/bin/bash

# set android sdk path
ANDROID_HOME=$1
#ANDROID_HOME=/opt/android-build/android-sdk

# gradle
#PATH=/opt/android-build/gradle/gradle-4.4/bin
PATH=$2:${PATH}

export ANDROID_HOME PATH

APPS=$3
[ -z "$APPS" ] && APPS=$APP_LIST

# jdk
[ ! -z "$4" ] && export JAVA_HOME=$4

# preprocess app
for APP in $APPS
do 
    # rm build cache
    rm -r -f ${APP}/build

    # copy jniLibs
    if [ -f "${APP}/src/main/jniLibs/copy_libs.sh" ]; then
        echo "Copy jniLibs for $APP with copy_libs.sh"
        cd "${APP}/src/main/jniLibs/"
        sh copy_libs.sh
        cd -
    else
        match=`grep :module_ep "${APP}/build.gradle" | awk '{gsub(/^\s+/, "");print}'`
        if [ ! -z "$match" ] && [[ "$match" != //* ]]; then
            echo "!!!Discarded: Copy all avalon jni libs to ${APP} with current version"
            # cp -r libs/avalon/jniLibs ${APP}/src/main/
        fi
    fi
done

# build
sed -i "s/^org.gradle.jvmargs=.*$/org.gradle.jvmargs=-Xmx16384M -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8/g" gradle.properties
sed -i "s/^org.gradle.parallel=.*$/org.gradle.parallel=true/g" gradle.properties

LC_ALL=en_US.UTF-8 gradle build
