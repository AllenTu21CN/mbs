#Refer to $NDK/build/cmake/android.toolchain.cmake

cmake_minimum_required(VERSION 3.6.0)


set(ANDROID TRUE)
set(CMAKE_SYSTEM_NAME Android)


# Touch toolchain variable to suppress "unused variable" warning.
# This happens if CMake is invoked with the same command line the second time.
if(ANDROID_NATIVE_API_LEVEL)
endif()
if(ANDROID_TOOLCHAIN)
endif()
if(CMAKE_TOOLCHAIN_FILE)
endif()
if(ANDROID_NDK)
endif()


# ABI
if(ANDROID_ABI STREQUAL "armeabi-v7a with NEON")
    set(ANDROID_ABI armeabi-v7a)
    set(ANDROID_ARM_NEON TRUE)
elseif(ANDROID_TOOLCHAIN_NAME AND NOT ANDROID_ABI)
    if(ANDROID_TOOLCHAIN_NAME MATCHES "^arm-linux-androideabi-")
        set(ANDROID_ABI armeabi-v7a)
    elseif(ANDROID_TOOLCHAIN_NAME MATCHES "^aarch64-linux-android-")
        set(ANDROID_ABI arm64-v8a)
    elseif(ANDROID_TOOLCHAIN_NAME MATCHES "^x86-")
        set(ANDROID_ABI x86)
    elseif(ANDROID_TOOLCHAIN_NAME MATCHES "^x86_64-")
        set(ANDROID_ABI x86_64)
    elseif(ANDROID_TOOLCHAIN_NAME MATCHES "^mipsel-linux-android-")
        set(ANDROID_ABI mips)
    elseif(ANDROID_TOOLCHAIN_NAME MATCHES "^mips64el-linux-android-")
        set(ANDROID_ABI mips64)
    endif()
endif()
if(NOT ANDROID_ABI)
    set(ANDROID_ABI armeabi-v7a)
endif()


# TOOLCHAIN
if(ANDROID_ABI STREQUAL armeabi-v7a)
    set(CMAKE_ANDROID_ARM_MODE ON)
    set(ANDROID_TOOLCHAIN_HOME "android-toolchain-arm-16")
    set(ANDROID_TOOLCHAIN_NAME "arm-linux-androideabi")
elseif(ANDROID_ABI STREQUAL arm64-v8a)
    set(CMAKE_ANDROID_ARM_MODE ON)
    set(ANDROID_TOOLCHAIN_HOME "android-toolchain-arm64-21")
    set(ANDROID_TOOLCHAIN_NAME "aarch64-linux-android")
elseif(ANDROID_ABI STREQUAL x86)
    set(ANDROID_TOOLCHAIN_HOME "android-toolchain-x86-16")
    set(ANDROID_TOOLCHAIN_NAME "i686-linux-android")
elseif(ANDROID_ABI STREQUAL x86_64)
    set(ANDROID_TOOLCHAIN_HOME "android-toolchain-x86_64-21")
    set(ANDROID_TOOLCHAIN_NAME "x86_64-linux-android")
else()
    message(FATAL_ERROR "Invalid Android ABI: '${ANDROID_ABI}'")
endif()


if(TOOLCHAIN_COMPILER STREQUAL "clang")
   set(C_COMPILER "clang")
   set(CXX_COMPILER "clang++")
else()
   set(C_COMPILER "gcc")
   set(CXX_COMPILER "g++")
endif()


if(CMAKE_HOST_SYSTEM_NAME STREQUAL Windows)
    set(ANDROID_TOOLCHAIN_SUFFIX .exe)
endif()


set(CMAKE_ANDROID_STANDALONE_TOOLCHAIN "${TOOLCHAIN_TOP_DIR}/${ANDROID_TOOLCHAIN_HOME}")
set(CMAKE_C_COMPILER "${CMAKE_ANDROID_STANDALONE_TOOLCHAIN}/bin/${ANDROID_TOOLCHAIN_NAME}-${C_COMPILER}${ANDROID_TOOLCHAIN_SUFFIX}")
set(CMAKE_CXX_COMPILER "${CMAKE_ANDROID_STANDALONE_TOOLCHAIN}/bin/${ANDROID_TOOLCHAIN_NAME}-${CXX_COMPILER}${ANDROID_TOOLCHAIN_SUFFIX}")


add_definitions(-DLINUX)
add_definitions(-DANDROID)


# Set or retrieve the cached flags.
# This is necessary in case the user sets/changes flags in subsequent
# configures. If we included the Android flags in here, they would get
# overwritten.
set(CMAKE_C_FLAGS ""
    CACHE STRING "Flags used by the compiler during all build types.")
set(CMAKE_CXX_FLAGS ""
    CACHE STRING "Flags used by the compiler during all build types.")
set(CMAKE_C_FLAGS_DEBUG ""
    CACHE STRING "Flags used by the compiler during debug builds.")
set(CMAKE_CXX_FLAGS_DEBUG ""
    CACHE STRING "Flags used by the compiler during debug builds.")
set(CMAKE_C_FLAGS_RELEASE ""
    CACHE STRING "Flags used by the compiler during release builds.")
set(CMAKE_CXX_FLAGS_RELEASE ""
    CACHE STRING "Flags used by the compiler during release builds.")
set(CMAKE_MODULE_LINKER_FLAGS ""
    CACHE STRING "Flags used by the linker during the creation of modules.")
set(CMAKE_SHARED_LINKER_FLAGS ""
    CACHE STRING "Flags used by the linker during the creation of dll's.")
set(CMAKE_EXE_LINKER_FLAGS ""
    CACHE STRING "Flags used by the linker.")
set(CMAKE_C_FLAGS             "${ANDROID_COMPILER_FLAGS} ${CMAKE_C_FLAGS}")
set(CMAKE_CXX_FLAGS           "${ANDROID_COMPILER_FLAGS} ${ANDROID_COMPILER_FLAGS_CXX} ${CMAKE_CXX_FLAGS} -std=c++11")
set(CMAKE_C_FLAGS_DEBUG       "${ANDROID_COMPILER_FLAGS_DEBUG} ${CMAKE_C_FLAGS_DEBUG}")
set(CMAKE_CXX_FLAGS_DEBUG     "${ANDROID_COMPILER_FLAGS_DEBUG} ${CMAKE_CXX_FLAGS_DEBUG}")
set(CMAKE_C_FLAGS_RELEASE     "${ANDROID_COMPILER_FLAGS_RELEASE} ${CMAKE_C_FLAGS_RELEASE}")
set(CMAKE_CXX_FLAGS_RELEASE   "${ANDROID_COMPILER_FLAGS_RELEASE} ${CMAKE_CXX_FLAGS_RELEASE}")
set(CMAKE_SHARED_LINKER_FLAGS "${ANDROID_LINKER_FLAGS} ${CMAKE_SHARED_LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${ANDROID_LINKER_FLAGS} ${CMAKE_MODULE_LINKER_FLAGS}")
set(CMAKE_EXE_LINKER_FLAGS    "${ANDROID_LINKER_FLAGS} ${ANDROID_LINKER_FLAGS_EXE} ${CMAKE_EXE_LINKER_FLAGS}")




