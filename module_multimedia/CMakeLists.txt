# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

####################
include_directories(src/main/cpp/)

if (ANDROID_ABI STREQUAL "x86")
    message(SEND_ERROR "not support other ABI(${ANDROID_ABI}) building")
elseif (ANDROID_ABI STREQUAL "x86_64")
    set(target_name "x86_64.21")
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(target_name "arm-v7a.16")
elseif (ANDROID_ABI STREQUAL "arm64-v8a")
    set(target_name "arm64.21")
else ()
    message(SEND_ERROR "not support other ABI(${ANDROID_ABI}) building")
endif ()

include_directories(
    src/main/cpp/avalon/${target_name}/include/
    src/main/cpp/avalon/${target_name}/3rd-party/jansson/include/
)
link_directories(
    src/main/cpp/avalon/${target_name}/lib
    src/main/cpp/avalon/${target_name}/3rd-party/icu/lib
    src/main/cpp/avalon/${target_name}/3rd-party/jansson/lib
    src/main/cpp/avalon/${target_name}/3rd-party/ffmpeg/lib
    src/main/cpp/avalon/${target_name}/3rd-party/fdk-aac/lib
    src/main/cpp/avalon/${target_name}/3rd-party/x264/lib
)
set(3rd-party-libs
     jansson
     icuuc icudata
     avfilter swscale postproc avformat avcodec swresample avutil z
     fdk-aac x264)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             MPX_JNI

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             src/main/cpp/sanp_javalon_network_protocol_RTSPClient.cpp
             src/main/cpp/sanp_javalon_network_protocol_RTMPPushClient.cpp
             src/main/cpp/sanp_javalon_network_protocol_RTMPBandwidthTest.cpp)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(MPX_JNI
    network base media log ${3rd-party-libs})
