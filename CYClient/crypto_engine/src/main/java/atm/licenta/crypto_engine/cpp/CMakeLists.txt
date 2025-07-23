cmake_minimum_required(VERSION 3.10.2)
project("Liboqs_JNI")

include_directories(${CMAKE_SOURCE_DIR}/liboqs/include)

add_library(
        native-lib
        SHARED
        oqs_jni.cpp)

add_library(
        oqs
        SHARED
        IMPORTED)

set_target_properties(oqs PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/liboqs.so)

target_link_libraries(
        native-lib
        oqs
        log)